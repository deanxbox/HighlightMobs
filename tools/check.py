"""Run from any directory: python tools/check.py [evidence-directory]. Uses a disposable config, never the player's config."""
from pathlib import Path
import json
import os
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
out = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(tempfile.mkdtemp(prefix="highlightmobs-check-"))
out.mkdir(parents=True, exist_ok=True)
classpath_file = out / "classpath.txt"
init = out / "classpath.gradle"
init.write_text('allprojects { afterEvaluate { tasks.register("iconTestClasspath") { doLast { new File(System.getenv("HM_CP_OUT")).text = sourceSets.client.runtimeClasspath.asPath } } } }\n')
env = dict(os.environ, HM_CP_OUT=str(classpath_file))
results = {}

def run(name, command):
    result = subprocess.run(command, cwd=root if name == "build" else out, env=env, capture_output=True, text=True)
    (out / (name + ".log")).write_text(result.stdout + result.stderr, encoding="utf-8")
    results[name] = "verified" if result.returncode == 0 else "failed"
    print(name + ": " + results[name], flush=True)
    return result.returncode == 0

def java(name, executable, arguments):
    # JDK argument files avoid Windows' command-line length limit on the full Fabric classpath.
    args = out / (name + ".args")
    args.write_text("\n".join(json.dumps(str(arg)) for arg in arguments), encoding="utf-8")
    return run(name, [executable, "@" + str(args)])

wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
if run("build", [str(wrapper), "build", "iconTestClasspath", "--init-script", str(init), "--console=plain", "--no-daemon"]):
    cp = classpath_file.read_text()
    client_jar = next(Path(p) for p in cp.split(os.pathsep) if Path(p).name.lower().startswith("minecraft-clientonly-") and p.endswith(".jar"))
    classes = out / "classes"
    classes.mkdir(exist_ok=True)
    sources = [root / "tools" / (name + ".java") for name in ["FaceIconCheck", "ConfigSelectionCheck", "VariantIconCheck"]]
    if java("compile-checks", "javac", ["--release", "21", "-cp", cp, "-d", classes, *sources]):
        for name, args in [("FaceIconCheck", [client_jar, out / "faces.png"]), ("ConfigSelectionCheck", []), ("VariantIconCheck", [client_jar])]:
            java(name, "java", ["-Xmx1G", "-Djava.awt.headless=true", "-cp", str(classes) + os.pathsep + cp, "dev.deanm.highlightmobs.client." + name, *args])
(out / "checks.json").write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
print("Evidence: " + str(out))
sys.exit(0 if all(result == "verified" for result in results.values()) else 1)
