"""
Datagen aus der Zentrale: nach dem Speichern von Java-Werten die erzeugten Dateien neu bauen.

Ein Auftrag = Schritte nacheinander (je ein eigener Gradle-Aufruf, weil syncGenerated263 die
26.3-Ausgabe mit dem frisch erzeugten src/main/generated vergleicht):

    26.2   gradlew runDatagen                 (danach generateWiki - Wiki-Daten)
    26.3   gradlew :mc26_3:fabric:runDatagen  (behält in mc26_3/generated nur die Abweichungen)
    1.21.11 gradlew :mc1_21_11:fabric:runDatagen
    (26.4  gradlew -Pmc264=true :mc26_4:fabric:runDatagen - nur auf Wunsch)
    Wiki   python wiki/generate.py

Danach liest die Zentrale neu ein, prüft (checkBalance) und zeigt ``git diff --stat`` - so sieht der
Besitzer, welche Dateien sich geändert haben. Es läuft immer nur ein Auftrag; er lässt sich abbrechen.

Schutz: laufen Dev-Clients/-Server (runClient, runServer), würde ein Gradle-Aufruf im selben Checkout
deren Klassen neu übersetzen und das Spiel mit NoClassDefFoundError abstürzen lassen. Dann startet
der Auftrag nur mit ausdrücklichem "trotzdem".
"""

from __future__ import annotations

import os
import subprocess
import sys
import threading
import time
from pathlib import Path


def default_steps(repo: Path, include_264: bool = False) -> list[dict]:
    gradle = str(repo / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    steps = [
        {"key": "26.2", "label": "Datagen 26.2 (+ Wiki)", "cmd": [gradle, "runDatagen", "--console=plain"]},
        {"key": "26.3", "label": "Datagen 26.3", "cmd": [gradle, ":mc26_3:fabric:runDatagen", "--console=plain"]},
        {"key": "1.21.11", "label": "Datagen 1.21.11", "cmd": [gradle, ":mc1_21_11:fabric:runDatagen", "--console=plain"]},
    ]
    if include_264:
        steps.append({"key": "26.4", "label": "Datagen 26.4 (Snapshot)",
                      "cmd": [gradle, "-Pmc264=true", ":mc26_4:fabric:runDatagen", "--console=plain"]})
    steps.append({"key": "wiki", "label": "Wiki-Daten", "cmd": [sys.executable, "wiki/generate.py"]})
    return steps


def module_steps(repo, module):
    """Main-line only; never launch a deferred port from the module UI."""
    gradle = str(repo / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))
    if module['id'] == 'simplebuilding':
        return [{'key': '26.3', 'label': 'Datagen 26.3', 'cmd':
                 [gradle, ':mc26_3:fabric:runDatagen', '--console=plain']},
                {'key': 'wiki', 'label': 'Wiki', 'cmd': [sys.executable, 'wiki/generate.py']}]
    # Scaffold projects have no datagen task. Producers opt in with an explicit task.
    task = module.get('datagenTask')
    if not task:
        raise ValueError('Dieses Modul hat noch keinen datagenTask im Manifest.')
    import re
    if not re.fullmatch(r'(?::[a-zA-Z][a-zA-Z0-9_]*)+', task):
        raise ValueError('Ungueltiger datagenTask')
    return [{'key': module['id'], 'label': 'Datagen 26.3', 'cmd': [gradle, task, '--console=plain']}]


def running_dev_games(repo: Path | None = None) -> list[str]:
    """Kommandozeilen laufender Dev-Clients/-Server dieses Checkouts (nur Bordmittel, im Zweifel leer).
    Spiele aus anderen Checkouts (Haupt-Repo, andere Worktrees) stoeren nicht und zaehlen nicht."""
    markers = ("runClient", "runServer", "devlaunchinjector", "forge_userdev", "net.fabricmc.devlaunchinjector")
    try:
        if os.name == "nt":
            out = subprocess.run(["powershell", "-NoProfile", "-Command",
                                  "Get-CimInstance Win32_Process -Filter \"Name like 'java%'\" | "
                                  "ForEach-Object { $_.CommandLine }"],
                                 capture_output=True, text=True, timeout=20).stdout
        else:
            out = subprocess.run(["ps", "-eo", "args"], capture_output=True, text=True, timeout=20).stdout
    except (OSError, subprocess.SubprocessError):
        return []
    found = []
    root = str(Path(repo).resolve()).replace("\\", "/").lower().rstrip("/") if repo else None
    in_worktree = bool(root and "/.claude/worktrees/" in root)
    for line in out.splitlines():
        if not any(m in line for m in markers) or "GradleDaemon" in line:
            continue
        norm = line.replace("\\", "/").lower()
        if root and (root not in norm or (not in_worktree and "/.claude/worktrees/" in norm)):
            continue
        found.append(line.strip()[:200])
    return found


class Job:
    def __init__(self, steps: list[dict], cwd: Path, on_done=None):
        self.steps = [dict(s, status="waiting", seconds=None, returncode=None) for s in steps]
        self.cwd = Path(cwd)
        self.on_done = on_done
        self.status = "running"
        self.started = time.time()
        self.finished = None
        self.log: list[str] = []
        self.result: dict | None = None
        self._proc: subprocess.Popen | None = None
        self._cancel = False
        self._lock = threading.Lock()
        self.thread = threading.Thread(target=self._run, daemon=True)

    def start(self):
        self.thread.start()
        return self

    def _append(self, line: str):
        with self._lock:
            self.log.append(line.rstrip("\r\n"))
            if len(self.log) > 4000:
                del self.log[:1000]

    def _run(self):
        for step in self.steps:
            if self._cancel:
                step["status"] = "skipped"
                continue
            step["status"] = "running"
            begin = time.time()
            self._append(f"=== {step['label']}: {' '.join(Path(c).name if i == 0 else c for i, c in enumerate(step['cmd']))}")
            try:
                self._proc = subprocess.Popen(step["cmd"], cwd=str(self.cwd), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                              text=True, encoding="utf-8", errors="replace", bufsize=1,
                                              env=dict(os.environ, PYTHONIOENCODING="utf-8"))
                for line in self._proc.stdout:
                    self._append(line)
                code = self._proc.wait()
            except OSError as err:
                self._append(f"nicht startbar: {err}")
                code = -1
            step["seconds"] = round(time.time() - begin, 1)
            step["returncode"] = code
            step["status"] = "ok" if code == 0 else ("cancelled" if self._cancel else "failed")
            if code != 0:
                self._append(f"=== {step['label']}: Fehler (Exit-Code {code}) - weitere Schritte übersprungen")
                for rest in self.steps:
                    if rest["status"] == "waiting":
                        rest["status"] = "skipped"
                break
        final = "cancelled" if self._cancel else ("ok" if all(s["status"] == "ok" for s in self.steps) else "failed")
        self._append("=== Auswertung: neu einlesen, checkBalance, git diff")
        if self.on_done:
            try:
                self.result = self.on_done(self)
            except Exception as err:  # Auswertung darf den Auftrag nicht haengen lassen
                self.result = {"error": str(err)}
        self.finished = time.time()
        self.status = final  # erst jetzt: wer "fertig" sieht, sieht auch das Ergebnis

    def cancel(self):
        self._cancel = True
        proc = self._proc
        if proc and proc.poll() is None:
            try:
                if os.name == "nt":
                    subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True, timeout=20)
                else:
                    proc.terminate()
            except (OSError, subprocess.SubprocessError):
                pass

    def payload(self, tail: int = 200) -> dict:
        with self._lock:
            log = self.log[-tail:]
        return {"status": self.status, "steps": self.steps, "started": self.started, "finished": self.finished,
                "seconds": round((self.finished or time.time()) - self.started, 1), "log": log, "result": self.result}


def git_diff_stat(repo: Path) -> dict:
    """Welche Dateien sich im Arbeitsbaum geändert haben (nach dem Datagen)."""
    try:
        stat = subprocess.run(["git", "diff", "--stat", "--", "."], cwd=str(repo), capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=60).stdout
        names = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all", "--", "."], cwd=str(repo),
                               capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60).stdout
    except (OSError, subprocess.SubprocessError) as err:
        return {"error": str(err), "files": [], "stat": ""}
    files = [line[3:] for line in names.splitlines() if len(line) > 3]
    return {"files": files[:400], "count": len(files), "stat": stat[-6000:]}
