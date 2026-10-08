"""Trigger SELECT-only workloads on Render; all reported timings come from its JVM, not this PC."""
from pathlib import Path
import argparse
import datetime
import json
import math
import re
import statistics
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
WORKLOADS = ("select1", "clubs", "players", "fantasy", "minigame")
TIMINGS = ("apiMs", "connectionMs", "sqlExecuteMs", "sqlFetchMs", "jdbcControlMs")
COUNTERS = ("acquisitions", "reusedConnections", "statements", "sessionStatements", "executeCalls")


def run():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--label", choices=("before", "after"), required=True)
    parser.add_argument("--samples", type=int, choices=range(3, 11), default=5)
    options = parser.parse_args()
    credentials = ROOT / "backend/secrets/read-latency.json"
    ignored = subprocess.run(["git", "check-ignore", "--quiet", str(credentials)], cwd=ROOT, capture_output=True)
    if ignored.returncode != 0:
        raise RuntimeError("Diagnostic credentials must be Git ignored")
    secrets = json.loads(credentials.read_text(encoding="utf-8-sig"))
    if any(not re.fullmatch("[a-fA-F0-9]{64}", secrets.get(key, "")) for key in ("operator_key", "proxy_secret")):
        raise RuntimeError("Configure two distinct hexadecimal keys in the ignored file")
    if secrets["operator_key"] == secrets["proxy_secret"]:
        raise RuntimeError("Operator key must be separate from the proxy secret")

    def request(workload):
        req = urllib.request.Request(
            "https://epl-personal.onrender.com/api/diagnostics/read-latency?workload=" + workload,
            headers={"Accept": "application/json", "X-PrismaXI-Read-Latency-Key": secrets["operator_key"],
                     "X-PrismaXI-Proxy-Secret": secrets["proxy_secret"]})
        with urllib.request.urlopen(req, timeout=60) as response:
            data = json.load(response)
        if not data.get("available"):
            raise RuntimeError("Workload unavailable; do not create sample accounts or games")
        measurement = data["measurement"]
        if measurement.get("route") != "probe/" + workload or measurement.get("status") != 200:
            raise RuntimeError("Unexpected diagnostic response")
        safe = {name: measurement[name] for name in TIMINGS + COUNTERS}
        if any(not isinstance(value, (int, float)) or not math.isfinite(value) or value < 0 for value in safe.values()):
            raise RuntimeError("Invalid numeric measurement")
        queries = measurement["queries"]
        safe["queries"] = {}
        for digest, query in queries.items():
            if not re.fullmatch("[a-f0-9]{16}", digest) or query.get("kind") not in ("SELECT", "SET", "SHOW"):
                raise RuntimeError("Unexpected statement in SELECT-only workload")
            safe["queries"][digest] = {"kind": query["kind"], "count": int(query["count"]),
                                      "executeMs": float(query["executeMs"])}
        return safe

    report = {"label": options.label, "timing_source": "Render JVM (not client HTTP elapsed time)",
              "captured_at": datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=7))).isoformat(),
              "samples_per_workload": options.samples, "workloads": {}}
    destination = ROOT / "backend/target/read-latency" / (options.label + ".json")
    destination.parent.mkdir(parents=True, exist_ok=True)
    for workload in WORKLOADS:
        warm = request(workload)
        samples = [request(workload) for _ in range(options.samples)]
        summary = {}
        for name in TIMINGS:
            values = sorted(sample[name] for sample in samples)
            summary[name] = {"median": statistics.median(values),
                             "p95": values[math.ceil(len(values) * .95) - 1]}
        summary["statement_counts"] = [sample["statements"] for sample in samples]
        report["workloads"][workload] = {"warmup": warm, "summary": summary, "samples": samples}
        destination.write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(json.dumps({"workload": workload, "summary": summary}), flush=True)
    print("Saved ignored measurement report: backend/target/read-latency/" + options.label + ".json")


if __name__ == "__main__":
    try:
        run()
    except urllib.error.HTTPError as failure:
        print("Diagnostic request failed: HTTP " + str(failure.code) + "; response/credentials suppressed", file=sys.stderr)
        sys.exit(1)
    except Exception as failure:
        print("Measurement stopped: " + type(failure).__name__ + "; private details suppressed", file=sys.stderr)
        sys.exit(1)
