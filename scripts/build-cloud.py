#!/usr/bin/env python3
"""Build in the managed cloud without changing HOME or weakening TLS verification."""
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit

root = Path(__file__).resolve().parents[1]
environment = os.environ.copy()
environment.setdefault("JAVA_HOME", "/workspace/tools/jdk-17.0.20+8")
environment.setdefault("ANDROID_HOME", "/workspace/android-sdk")
environment.setdefault("ANDROID_USER_HOME", "/workspace/.android")
environment.setdefault("GRADLE_USER_HOME", "/workspace/.gradle")
command = [str(root / "gradlew"), "--no-daemon", "--console=plain", "-Duser.home=/workspace"]
trust_store = Path("/etc/ssl/certs/java/cacerts")
if trust_store.exists():
    command.append(f"-Djavax.net.ssl.trustStore={trust_store}")
proxy = urlsplit(environment.get("HTTPS_PROXY") or environment.get("https_proxy") or "")
if proxy.hostname:
    if proxy.username or proxy.password:
        raise SystemExit("Authenticated proxy requires platform-supported Java configuration; credentials are not copied.")
    for scheme in ("http", "https"):
        command += [f"-D{scheme}.proxyHost={proxy.hostname}", f"-D{scheme}.proxyPort={proxy.port or 80}"]
command += sys.argv[1:] or ["assembleDebug", "testDebugUnitTest", "lintDebug"]
raise SystemExit(subprocess.call(command, cwd=root, env=environment))
