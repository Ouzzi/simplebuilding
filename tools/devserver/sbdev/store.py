"""
Die Balance-Ablage: balance/balance.json (aktueller Stand) und balance/versions/vNNNN.json (jede
bestaetigte Speicherung, unveränderlich, nie geloescht).

Regeln, damit nie ein Wert verloren geht oder still zurueckgesetzt wird:

* Commit-Punkt ist die Versionsdatei. Sie wird exklusiv angelegt (eine bestehende Version wird nie
  überschrieben), danach wird balance.json atomar ersetzt (temporaere Datei + fsync + os.replace).
  Stuerzt etwas dazwischen ab, stellt das nächste Laden balance.json aus der neuesten Version her.
* Gespeichert wird nur gegen die Version, die der Aufrufer gesehen hat (base_version). Hat
  inzwischen jemand anderes gespeichert, gibt es einen Konflikt statt eines Ueberschreibens.
* Eine Sperrdatei (balance/.lock) verhindert, dass zwei Prozesse gleichzeitig schreiben; innerhalb
  des Servers zusaetzlich ein Lock.
* Eine kaputte balance.json wird nie überschrieben, sondern als balance.json.corrupt-<zeit> beiseite
  gelegt; der Stand kommt aus der neuesten lesbaren Version. Kaputte Versionsdateien bleiben liegen
  und ihre Nummer wird nie wieder vergeben.
* Rückgängig machen (Rollback) ist eine neue Version mit dem Inhalt der alten - die Geschichte
  waechst nur.

Eintrag je Wert: {"value": geplanter Wert, "mod": Mod-Wert beim Speichern, "origin": Mod-Wert, als
der Wert zum ersten Mal geplant wurde, "applied": true, wenn Speichern ihn in die Mod geschrieben hat
(checkBalance meldet dann jede spätere Abweichung im Code)}.
"""

from __future__ import annotations

import copy
import json
import os
import re
import threading
import time
from pathlib import Path

VERSION_FILE = re.compile(r"^v(\d{4,})\.json$")
LOCK_STALE_SECONDS = 30.0


class StoreError(Exception):
    def __init__(self, status: int, message: str, details: list | None = None):
        super().__init__(message)
        self.status = status
        self.message = message
        self.details = details or []


def _now() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%S")


def _retry(action, attempts: int = 40):
    """Windows: eine Datei, die gerade jemand liest, laesst sich kurz nicht ersetzen - kurz warten."""
    for i in range(attempts):
        try:
            return action()
        except PermissionError:
            if i == attempts - 1:
                raise
            time.sleep(0.05)


def _atomic_write(path: Path, text: str) -> None:
    tmp = path.with_name(f".{path.name}.{os.getpid()}.{threading.get_ident()}.{time.time_ns()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
        handle.flush()
        os.fsync(handle.fileno())
    _retry(lambda: os.replace(tmp, path))


def _create_exclusive(path: Path, text: str) -> None:
    """Datei neu anlegen; gibt es sie schon, Fehler - nie überschreiben."""
    if path.exists():
        raise StoreError(409, f"{path.name} existiert schon - keine Version wird überschrieben.")
    tmp = path.with_name(f".{path.name}.{os.getpid()}.{threading.get_ident()}.{time.time_ns()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
        handle.flush()
        os.fsync(handle.fileno())
    try:
        try:
            os.link(tmp, path)
        except FileExistsError:
            raise StoreError(409, f"{path.name} existiert schon - keine Version wird überschrieben.") from None
        except (OSError, AttributeError):
            if path.exists():
                raise StoreError(409, f"{path.name} existiert schon - keine Version wird überschrieben.") from None
            os.rename(tmp, path)
            return
    finally:
        if tmp.exists():
            os.remove(tmp)


def dumps(data) -> str:
    return json.dumps(data, indent=1, ensure_ascii=False, sort_keys=True) + "\n"


class Store:
    def __init__(self, root: Path, lock_timeout: float = 10.0):
        self.root = Path(root)
        self.lock_timeout = lock_timeout
        self.main = self.root / "balance.json"
        self.versions = self.root / "versions"
        self.lockfile = self.root / ".lock"
        self._lock = threading.RLock()
        self.warnings: list[str] = []

    # ---- Lesen ---------------------------------------------------------------------------

    def _numbers(self) -> list[int]:
        if not self.versions.exists():
            return []
        out = []
        for path in self.versions.iterdir():
            m = VERSION_FILE.match(path.name)
            if m:
                out.append(int(m.group(1)))
        return sorted(out)

    def _path(self, number: int) -> Path:
        return self.versions / f"v{number:04d}.json"

    def _read_version(self, number: int) -> dict | None:
        try:
            data = json.loads(_retry(lambda: self._path(number).read_text(encoding="utf-8")))
        except (OSError, json.JSONDecodeError):
            return None
        if not isinstance(data, dict) or data.get("version") != number or not isinstance(data.get("entries"), dict):
            return None
        return data

    def _latest_valid(self) -> dict | None:
        for number in reversed(self._numbers()):
            data = self._read_version(number)
            if data is not None:
                return data
            self._warn(f"Versionsdatei v{number:04d}.json ist nicht lesbar - sie bleibt liegen, ihre Nummer wird nicht neu vergeben.")
        return None

    def _warn(self, text: str) -> None:
        if text not in self.warnings:
            self.warnings.append(text)

    @staticmethod
    def empty() -> dict:
        return {"schema": 1, "version": 0, "savedAt": None, "message": "Ausgangszustand (nur Mod-Werte)", "entries": {}}

    def _state_from_version(self, data: dict) -> dict:
        return {"schema": 1, "version": data["version"], "savedAt": data.get("savedAt"), "message": data.get("message", ""),
                "entries": copy.deepcopy(data["entries"])}

    def _load(self) -> dict:
        latest = self._latest_valid()
        if self.main.exists():
            try:
                raw = _retry(lambda: self.main.read_text(encoding="utf-8"))
            except OSError as err:
                raise StoreError(503, f"balance.json gerade nicht lesbar ({err}) - nichts wurde veraendert, bitte erneut versuchen.") from None
            try:
                state = json.loads(raw)
                if not isinstance(state, dict) or not isinstance(state.get("entries"), dict) or not isinstance(state.get("version"), int):
                    raise ValueError("Aufbau falsch")
            except ValueError as err:
                aside = self.main.with_name(f"balance.json.corrupt-{time.strftime('%Y%m%d-%H%M%S')}")
                os.replace(self.main, aside)
                self._warn(f"balance.json war nicht lesbar ({err}); beiseite gelegt als {aside.name}.")
                if latest:
                    state = self._state_from_version(latest)
                    _atomic_write(self.main, dumps(state))
                    self._warn(f"Stand aus Version v{latest['version']} wiederhergestellt.")
                    return state
                return self.empty()
            if latest and latest["version"] > state["version"]:
                recovered = self._state_from_version(latest)
                if self.lockfile.exists():
                    return recovered  # jemand speichert gerade: Version ist schon da, balance.json folgt gleich
                _atomic_write(self.main, dumps(recovered))
                self._warn(f"balance.json (v{state['version']}) war älter als die neueste Version v{latest['version']} "
                           "(Absturz beim Speichern?) - aus der Version wiederhergestellt.")
                return recovered
            return state
        if latest:
            state = self._state_from_version(latest)
            _atomic_write(self.main, dumps(state))
            self._warn(f"balance.json fehlte - aus Version v{latest['version']} wiederhergestellt.")
            return state
        return self.empty()

    def state(self) -> dict:
        with self._lock:
            return self._load()

    def history(self) -> list[dict]:
        with self._lock:
            out = [{"version": 0, "savedAt": None, "message": "Ausgangszustand (nur Mod-Werte)", "changes": 0, "entries": 0}]
            for number in self._numbers():
                data = self._read_version(number)
                if data is None:
                    out.append({"version": number, "corrupt": True, "message": "nicht lesbar"})
                    continue
                out.append({"version": number, "savedAt": data.get("savedAt"), "message": data.get("message", ""),
                            "changes": len(data.get("changes", [])), "entries": len(data["entries"]),
                            "rollbackOf": data.get("rollbackOf"), "applied": len(self.applied_for(number))})
            return out

    def version(self, number: int) -> dict:
        if number == 0:
            return dict(self.empty(), changes=[])
        data = self._read_version(number)
        if data is None:
            raise StoreError(404, f"Version v{number} gibt es nicht (oder sie ist nicht lesbar).")
        return data

    # ---- Schreiben -----------------------------------------------------------------------

    def _acquire(self) -> None:
        self.root.mkdir(parents=True, exist_ok=True)
        deadline = time.time() + self.lock_timeout
        while True:
            try:
                fd = os.open(self.lockfile, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                os.write(fd, f"{os.getpid()} {_now()}".encode())
                os.close(fd)
                return
            except FileExistsError:
                try:
                    age = time.time() - self.lockfile.stat().st_mtime
                except FileNotFoundError:
                    continue
                if age > LOCK_STALE_SECONDS:
                    try:
                        os.remove(self.lockfile)
                        self._warn("Eine alte Sperrdatei (balance/.lock) wurde entfernt.")
                    except FileNotFoundError:
                        pass
                    continue
                if time.time() > deadline:
                    raise StoreError(423, "Die Ablage ist gesperrt (ein anderer Prozess speichert gerade: balance/.lock). "
                                          "Nichts wurde gespeichert - bitte gleich noch einmal versuchen.")
                time.sleep(0.05)

    def _release(self) -> None:
        try:
            os.remove(self.lockfile)
        except FileNotFoundError:
            pass

    def commit(self, base_version: int, changes: list[dict], message: str, *, rollback_of: int | None = None,
               before_write=None, after_version=None) -> dict:
        """
        changes: [{"id", "new", "mod", "remove": bool}] - schon geprüft (Service). Legt Version N+1 an.
        before_write(record): darf mit StoreError abbrechen (nichts ist dann geschrieben).
        after_version(record) -> Liste angewendeter Dateiaenderungen (landet in balance.json-Historie).
        """
        if not isinstance(base_version, int):
            raise StoreError(400, "baseVersion fehlt oder ist keine Zahl.")
        if not changes:
            raise StoreError(400, "Keine Änderungen zum Speichern.")
        with self._lock:
            self._acquire()
            try:
                current = self._load()
                if current["version"] != base_version:
                    raise StoreError(409, f"Die Ablage ist inzwischen bei v{current['version']}, geladen war v{base_version}. "
                                          "Nichts wurde gespeichert. Bitte neu laden - deine ungespeicherten Änderungen bleiben erhalten.")
                entries = copy.deepcopy(current["entries"])
                log = []
                for change in changes:
                    vid = change["id"]
                    before = entries.get(vid)
                    if change.get("remove"):
                        if vid in entries:
                            del entries[vid]
                            log.append({"id": vid, "old": before["value"], "new": None, "removed": True, "mod": change.get("mod")})
                        continue
                    origin = before.get("origin", before.get("mod")) if before else change.get("mod")
                    entries[vid] = {"value": change["new"], "mod": change.get("mod"), "origin": origin}
                    if change.get("applied"):
                        entries[vid]["applied"] = True
                    origin_text = (before or {}).get("originText") or change.get("originText")
                    if origin_text:
                        entries[vid]["originText"] = origin_text
                    log.append({"id": vid, "old": before["value"] if before else change.get("old"), "new": change["new"],
                                "mod": change.get("mod"), "wasPlanned": before is not None})
                if entries == current["entries"]:
                    raise StoreError(400, "Die Änderungen ergeben denselben Stand - nichts zu speichern.")
                numbers = self._numbers()
                number = max(numbers + [current["version"]]) + 1
                record = {"schema": 1, "version": number, "parent": current["version"], "savedAt": _now(),
                          "message": (message or "").strip()[:500], "rollbackOf": rollback_of,
                          "changes": log, "entries": entries, "applied": []}
                if before_write:
                    before_write(record)
                self.versions.mkdir(parents=True, exist_ok=True)
                _create_exclusive(self._path(number), dumps(record))
                if after_version:
                    applied = after_version(record) or []
                    if applied:
                        record["applied"] = applied
                        # die Versionsdatei ist unveränderlich; die Liste steht zusaetzlich in einer Begleitdatei
                        _atomic_write(self.versions / f"v{number:04d}.applied.json", dumps(applied))
                state = {"schema": 1, "version": number, "savedAt": record["savedAt"], "message": record["message"],
                         "entries": entries}
                _atomic_write(self.main, dumps(state))
                return state
            finally:
                self._release()

    def applied_for(self, number: int) -> list:
        path = self.versions / f"v{number:04d}.applied.json"
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return []
