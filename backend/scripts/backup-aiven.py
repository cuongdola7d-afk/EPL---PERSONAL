import argparse
import glob
import hashlib
import os
import re
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse


def fail(reason: str) -> None:
    print(f"BACKUP_FAILED {reason}")
    sys.exit(1)


def redact(text: str, values: list[str]) -> str:
    result = text
    for val in values:
        if val:
            result = result.replace(val, "<REDACTED>")
    return result


def find_mysqldump() -> Path | None:
    on_path = shutil.which("mysqldump")
    if on_path:
        return Path(on_path)
    candidates = [
        Path(r"C:\Program Files\MySQL\MySQL Server 9.6\bin\mysqldump.exe"),
        Path(r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe"),
        Path(r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"),
    ]
    for pattern in (
        r"C:\Program Files\MySQL\*\bin\mysqldump.exe",
        r"C:\Program Files (x86)\MySQL\*\bin\mysqldump.exe",
    ):
        for match in sorted(glob.glob(pattern), reverse=True):
            candidates.append(Path(match))
    for path in candidates:
        if path.is_file():
            return path
    return None


def load_secrets(env_path: Path) -> dict[str, str]:
    if not env_path.is_file():
        fail(f"secrets file not found: {env_path}")
    raw: dict[str, str] = {}
    for line in env_path.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)\s*$", line)
        if m:
            key = m.group(1).strip()
            val = m.group(2).strip().strip('"').strip("'")
            raw[key] = val
    for required in ("PREMIERHUB_JDBC_URL", "PREMIERHUB_DB_USER", "PREMIERHUB_DB_PASSWORD"):
        if not raw.get(required):
            fail(f"missing required key {required} in secrets file")
    jdbc_url = raw["PREMIERHUB_JDBC_URL"]
    parsed = urlparse(jdbc_url.replace("jdbc:", "", 1))
    host = parsed.hostname or ""
    port = str(parsed.port or 3306)
    database = parsed.path.lstrip("/").split("?")[0]
    if not host or not database:
        fail("invalid PREMIERHUB_JDBC_URL in secrets file")
    return {
        "jdbc_url": jdbc_url,
        "host": host,
        "port": port,
        "database": database,
        "user": raw["PREMIERHUB_DB_USER"],
        "password": raw["PREMIERHUB_DB_PASSWORD"],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Create verified Aiven MySQL backup in backend/local-backups.")
    parser.add_argument("--label", required=True, help="Short folder/file label, e.g. don-du-lieu")
    parser.add_argument("--secrets-file", default=r"D:\File Jva\PrismaXI-secrets\aiven-production.env")
    parser.add_argument("--ca-file", default="")
    args = parser.parse_args()

    label = args.label.strip()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}", label):
        fail("invalid label; use 1-64 characters from [A-Za-z0-9._-]")

    backend_dir = Path(__file__).resolve().parent.parent
    ca_path = Path(args.ca_file) if args.ca_file.strip() else backend_dir / "secrets" / "aiven-ca.pem"
    if not ca_path.is_file():
        fail(f"CA certificate file not found: {ca_path}")

    mysqldump = find_mysqldump()
    if not mysqldump:
        fail("mysqldump executable not found in PATH or C:\\Program Files\\MySQL")

    cfg = load_secrets(Path(args.secrets_file))
    secret_values = [cfg["host"], cfg["user"], cfg["password"], cfg["jdbc_url"]]

    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    out_dir = backend_dir / "local-backups" / f"{label}-{ts}"
    out_dir.mkdir(parents=True, exist_ok=True)
    dump_file = out_dir / f"aiven-{label}-{ts}.sql"

    env = os.environ.copy()
    env["MYSQL_PWD"] = cfg["password"]
    cmd = [
        str(mysqldump),
        "-h", cfg["host"],
        "-P", cfg["port"],
        "-u", cfg["user"],
        "--ssl-mode=VERIFY_IDENTITY",
        f"--ssl-ca={ca_path}",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--set-gtid-purged=OFF",
        "--column-statistics=0",
        "--default-character-set=utf8mb4",
        "--skip-lock-tables",
        cfg["database"],
    ]

    try:
        with dump_file.open("wb") as handle:
            proc = subprocess.run(cmd, env=env, stdout=handle, stderr=subprocess.PIPE)
    except OSError as exc:
        fail(redact(f"failed to launch mysqldump: {exc}", secret_values))

    if proc.returncode != 0:
        err_text = redact(proc.stderr.decode("utf-8", "replace").strip(), secret_values)
        if dump_file.exists() and dump_file.stat().st_size == 0:
            dump_file.unlink(missing_ok=True)
        first_line = err_text.splitlines()[0] if err_text else f"mysqldump exit {proc.returncode}"
        fail(first_line)

    data = dump_file.read_bytes()
    size_bytes = len(data)
    if size_bytes == 0:
        dump_file.unlink(missing_ok=True)
        fail("dump file is empty")

    tail = data[-512:].decode("utf-8", "replace")
    if "Dump completed" not in tail:
        fail("dump missing 'Dump completed' footer")

    table_count = data.count(b"CREATE TABLE `")
    if table_count == 0:
        fail("dump contains 0 CREATE TABLE statements")

    sha256 = hashlib.sha256(data).hexdigest()
    print(f"BACKUP_OK path={dump_file} bytes={size_bytes} tables={table_count} sha256={sha256}")


if __name__ == "__main__":
    main()
