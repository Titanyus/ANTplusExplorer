#!/usr/bin/env python3
"""Prepare verified, pinned Android build tools under /workspace; no repository changes."""
from pathlib import Path
import hashlib
import shutil
import subprocess
import tarfile
import zipfile

workspace = Path("/workspace")
downloads = workspace / "setup-downloads"
downloads.mkdir(exist_ok=True)
tools = workspace / "tools"
tools.mkdir(exist_ok=True)
sdk = workspace / "android-sdk"


def download(name, url, algorithm, expected):
    target = downloads / name
    if not target.exists() or hashlib.new(algorithm, target.read_bytes()).hexdigest() != expected:
        subprocess.run(["curl", "--fail", "--location", "--retry", "2", url, "--output", str(target)], check=True)
    if hashlib.new(algorithm, target.read_bytes()).hexdigest() != expected:
        raise SystemExit(f"Checksum verification failed: {name}")
    print(f"Verified {name}", flush=True)
    return target


jdk = tools / "jdk-17.0.20+8"
if not (jdk / "bin/javac").exists():
    archive = download("jdk.tar.gz",
        "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20_8.tar.gz",
        "sha256", "be7668bc030d578b83d6d5ef9221d6d6729bbbca8cf94a7d52e16ac68b5a5a35")
    with tarfile.open(archive) as package:
        package.extractall(tools, filter="data")

if not (sdk / "platforms/android-35/android.jar").exists():
    archive = download("platform.zip", "https://dl.google.com/android/repository/platform-35_r02.zip",
        "sha1", "0bb560a90a7a2cbd0dd8348224d518b638fe7949")
    with zipfile.ZipFile(archive) as package:
        package.extractall(sdk / "platforms")

if not (sdk / "build-tools/35.0.0/source.properties").exists():
    archive = download("build-tools.zip", "https://dl.google.com/android/repository/build-tools_r35_linux.zip",
        "sha1", "2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc")
    unpack = downloads / "build-tools-unpack"
    if unpack.exists():
        shutil.rmtree(unpack)
    with zipfile.ZipFile(archive) as package:
        package.extractall(unpack)
    roots = [p for p in unpack.iterdir() if p.is_dir()]
    if len(roots) != 1:
        raise SystemExit("Unexpected build-tools archive structure")
    target = sdk / "build-tools/35.0.0"
    target.parent.mkdir(parents=True, exist_ok=True)
    # Only our tool directory is touched; the checkout and other SDK versions are preserved.
    if target.exists():
        shutil.rmtree(target)
    shutil.move(str(roots[0]), str(target))

for file in (sdk / "build-tools/35.0.0").iterdir():
    if file.is_file() and not file.suffix:
        file.chmod(file.stat().st_mode | 0o111)
if not (sdk / "platform-tools/adb").exists():
    archive = download("platform-tools.zip", "https://dl.google.com/android/repository/platform-tools_r37.0.1-linux.zip",
        "sha1", "477254aa5f903c15cf51001717bdf347fb6b53e0")
    with zipfile.ZipFile(archive) as package:
        package.extractall(sdk)
for name in ("adb", "fastboot"):
    file = sdk / "platform-tools" / name
    file.chmod(file.stat().st_mode | 0o111)
(workspace / ".android").mkdir(exist_ok=True)
subprocess.run([str(jdk / "bin/javac"), "-version"], check=True)
print("Android SDK Platform 35 and Build Tools 35.0.0 ready. Run python scripts/build-cloud.py.")
