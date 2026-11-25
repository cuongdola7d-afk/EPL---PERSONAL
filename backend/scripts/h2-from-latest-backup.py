import argparse
import shutil
import subprocess
import sys
from pathlib import Path


def fail(reason: str) -> None:
    print(f"H2_FAILED {reason}")
    sys.exit(1)


def find_latest_backup(backups_dir: Path) -> Path | None:
    if not backups_dir.is_dir():
        return None
    candidates = [p for p in backups_dir.rglob("*.sql") if p.is_file()]
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def find_h2_jar() -> Path | None:
    m2_h2 = Path.home() / ".m2" / "repository" / "com" / "h2database" / "h2"
    if not m2_h2.is_dir():
        return None
    jars = sorted(m2_h2.rglob("h2-*.jar"))
    jars = [j for j in jars if not j.name.endswith(("-sources.jar", "-javadoc.jar"))]
    return jars[-1] if jars else None


def find_java_tool(tool_name: str) -> str | None:
    direct = shutil.which(tool_name)
    if direct:
        return direct
    java_exe = shutil.which("java")
    if java_exe:
        sibling = Path(java_exe).resolve().with_name(f"{tool_name}.exe")
        if sibling.is_file():
            return str(sibling)
    return None


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a fresh local H2 database from a MySQL backup dump.")
    parser.add_argument("--backup", default="", help="Optional path to a .sql backup dump; defaults to latest.")
    args = parser.parse_args()

    script_dir = Path(__file__).resolve().parent
    backend_dir = script_dir.parent
    backups_dir = backend_dir / "local-backups"

    if args.backup.strip():
        dump_file = Path(args.backup.strip()).resolve()
        if not dump_file.is_file():
            fail(f"backup file not found: {dump_file}")
    else:
        dump_file = find_latest_backup(backups_dir)
        if not dump_file:
            fail(f"no .sql backup found in {backups_dir}")

    h2_jar = find_h2_jar()
    if not h2_jar:
        fail("H2 jar not found in local Maven repository (~/.m2/repository/com/h2database/h2); run mvnw test-compile first")

    java_bin = find_java_tool("java")
    javac_bin = find_java_tool("javac")
    if not java_bin or not javac_bin:
        fail("java/javac not found in PATH")

    seeder_src = script_dir / "H2FromDumpSeeder.java"
    if not seeder_src.is_file():
        fail(f"missing seeder source: {seeder_src}")

    out_dir = backend_dir / "target" / "h2-from-backup"
    classes_dir = out_dir / ".classes"
    classes_dir.mkdir(parents=True, exist_ok=True)
    class_file = classes_dir / "H2FromDumpSeeder.class"

    if (not class_file.exists()) or (class_file.stat().st_mtime < seeder_src.stat().st_mtime):
        comp = subprocess.run(
            [javac_bin, "--release", "21", "-encoding", "UTF-8", "-d", str(classes_dir), str(seeder_src)],
            capture_output=True, text=True, encoding="utf-8", errors="replace",
        )
        if comp.returncode != 0:
            err = (comp.stdout + "\n" + comp.stderr).strip().splitlines()
            fail(f"failed to compile H2FromDumpSeeder: {err[0] if err else comp.returncode}")

    h2_base = out_dir / "premierhub"
    resources_dir = backend_dir / "src" / "main" / "resources"
    run = subprocess.run(
        [
            java_bin, "-Dfile.encoding=UTF-8",
            "-cp", f"{classes_dir};{h2_jar}",
            "H2FromDumpSeeder",
            str(dump_file),
            str(h2_base),
            str(resources_dir),
        ],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if run.returncode != 0:
        err_lines = [l for l in (run.stdout + "\n" + run.stderr).splitlines() if l.strip()]
        fail(err_lines[-1] if err_lines else f"seeder exit {run.returncode}")

    output = run.stdout.strip().splitlines()
    if not output:
        fail("seeder produced no output")
    print(output[-1])


if __name__ == "__main__":
    main()
