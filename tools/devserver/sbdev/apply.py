"""
Werte, die schon in Phase 1 in der Mod wirken (apply = "mod", heute: Handel), in ihre Datei
schreiben - chirurgisch per jsonedit, atomar (temporaere Datei + os.replace), mit Vorab-Prüfung.

Vorab-Prüfung: in der Datei muss noch der Wert stehen, den der Schnappschuss eingelesen hat.
Hat jemand die Datei seither geändert (ein paralleler Lauf, ein Editor), wird NICHTS geschrieben
und der Besitzer muss neu einlesen - so ueberschreibt die Zentrale nie eine fremde Änderung.
"""

from __future__ import annotations

import copy
import json
import os
import time
from pathlib import Path

from . import jsonedit
from .values import same


class ApplyConflict(Exception):
    pass


def _current(repo: Path, record: dict):
    source = record["source"]
    path = repo / source["file"]
    with open(path, encoding="utf-8", newline="") as handle:  # Zeilenenden (CRLF/LF) unveraendert lassen
        text = handle.read()
    data = json.loads(text)
    try:
        return text, jsonedit.get(data, source["path"]), True
    except (KeyError, IndexError, TypeError):
        return text, None, False


def precheck(repo: Path, record: dict) -> None:
    """Wirft ApplyConflict, wenn die Datei nicht mehr den eingelesenen Wert hat."""
    source = record["source"]
    if not source.get("file") or not source.get("path"):
        raise ApplyConflict(f"{record['label']}: keine Datei/kein Pfad bekannt")
    try:
        _, current, present = _current(repo, record)
    except (OSError, json.JSONDecodeError) as err:
        raise ApplyConflict(f"{source['file']}: nicht lesbar ({err})") from None
    if not present:
        if source.get("insert"):
            return
        raise ApplyConflict(f"{source['file']}: {'/'.join(map(str, source['path']))} fehlt")
    if not same(current, record["value"]):
        raise ApplyConflict(f"{source['file']} wurde seit dem Einlesen geändert ({record['label']}: jetzt {current}, "
                            f"eingelesen {record['value']}). Nichts geschrieben - bitte 'Neu einlesen' und erneut speichern.")


def write(repo: Path, record: dict, new) -> dict:
    source = record["source"]
    path = repo / source["file"]
    text, current, present = _current(repo, record)
    if present:
        result = jsonedit.replace(text, tuple(source["path"]), new)
    else:
        spec = source.get("insert")
        if not spec:
            raise ApplyConflict(f"{source['file']}: Pfad fehlt und kann nicht eingefügt werden")
        template = copy.deepcopy(spec["template"])
        # der Wert gehört an den letzten Pfadteil innerhalb der Vorlage
        leaf = source["path"][len(spec["parent"]) + 1:]
        target = template
        for part in leaf[:-1]:
            target = target[part]
        target[leaf[-1]] = new
        result = jsonedit.insert_before(text, tuple(spec["parent"]), spec["key"], template, spec["before"])
    tmp = path.with_name(f".{path.name}.{os.getpid()}.{time.time_ns()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="") as handle:
        handle.write(result)
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(tmp, path)
    return {"id": record["id"], "file": source["file"], "path": source["path"], "old": current, "new": new}
