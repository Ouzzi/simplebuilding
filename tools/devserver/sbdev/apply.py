"""
Werte, die in der Mod wirken (apply = "mod"), an ihre Stellen schreiben - alle Linien auf einmal.

Eine Stelle ist entweder ein JSON-Pfad (Handel, jsonedit) oder ein Java-Literal (javaedit); dazu
kommen die Zwillinge der anderen Linien (source.twins, siehe sites.py). Ablauf je Speicherung:

1. Vorab-Prüfung ALLER Stellen: überall muss noch der eingelesene Wert stehen. Hat jemand eine Datei
   seither geändert (ein paralleler Lauf, ein Editor), wird NICHTS geschrieben und der Besitzer muss
   neu einlesen - so überschreibt die Zentrale nie eine fremde Änderung.
2. Schreiben: je Datei ein Durchgang (Java von hinten nach vorn), atomar (temporäre Datei +
   os.replace). Scheitert eine spätere Datei, werden die schon geschriebenen aus dem Speicher
   zurückgesetzt - eine Speicherung ist ganz oder gar nicht in der Mod.
"""

from __future__ import annotations

import copy
import json
import os
import time
from pathlib import Path

from . import javaedit, jsonedit
from .values import same


class ApplyConflict(Exception):
    pass


def sites_of(record: dict) -> list[dict]:
    """Hauptstelle + Zwillinge. Die Hauptstelle eines JSON-Werts hat kein "kind"."""
    source = record.get("source") or {}
    main = dict(source)
    main.pop("twins", None)
    main.setdefault("kind", "json" if "path" in source and "span" not in source else "java")
    return [main] + [dict(t, kind=t.get("kind", "java")) for t in source.get("twins", [])]


def _json_current(repo: Path, site: dict):
    path = repo / site["file"]
    with open(path, encoding="utf-8", newline="") as handle:  # Zeilenenden (CRLF/LF) unveraendert lassen
        text = handle.read()
    data = json.loads(text)
    try:
        return text, jsonedit.get(data, site["path"]), True
    except (KeyError, IndexError, TypeError):
        return text, None, False


def _check_json(repo: Path, record: dict, site: dict) -> str | None:
    if not site.get("file") or not site.get("path"):
        return f"{record['label']}: keine Datei/kein Pfad bekannt"
    try:
        _, current, present = _json_current(repo, site)
    except (OSError, json.JSONDecodeError) as err:
        return f"{site['file']}: nicht lesbar ({err})"
    if not present:
        if site.get("insert"):
            return None
        return f"{site['file']}: {'/'.join(map(str, site['path']))} fehlt"
    if not same(current, record["value"]):
        return (f"{site['file']} wurde seit dem Einlesen geändert ({record['label']}: jetzt {current}, "
                f"eingelesen {record['value']}). Nichts geschrieben - bitte 'Neu einlesen' und erneut speichern.")
    return None


def precheck(repo: Path, record: dict, new=None) -> None:
    """Wirft ApplyConflict, wenn eine Stelle nicht mehr den eingelesenen Wert hat (oder new dort nicht geht)."""
    problems = check_all(repo, [(record, new)])
    if problems:
        raise ApplyConflict("; ".join(p["message"] for p in problems))


def origin_tokens(record: dict) -> list:
    """Die heutigen Literale aller Stellen (für die Ablage: damit ein Rücksetzen sie exakt wiederherstellt)."""
    return [s.get("token") if s["kind"] == "java" else None for s in sites_of(record)]


def check_all(repo: Path, items: list[tuple]) -> list[dict]:
    items = [(i[0], i[1]) for i in items]
    problems = []
    texts: dict[str, str] = {}
    for record, new in items:
        for site in sites_of(record):
            if site["kind"] == "json":
                err = _check_json(repo, record, site)
            else:
                try:
                    if site["file"] not in texts:
                        texts[site["file"]] = javaedit.read(repo / site["file"])[1]
                    err = javaedit.check_site(texts[site["file"]], site, record["value"])
                    if err is None and new is not None:
                        javaedit.inverse(site, new)
                except OSError as err_:
                    err = f"{site['file']}: nicht lesbar ({err_})"
                except javaedit.JavaEditError as err_:
                    err = f"{record['label']}: {err_}"
            if err:
                problems.append({"id": record["id"], "message": (f"[{site['mc']}] " if site.get("mc") else "") + err})
    return problems


def _write_json(repo: Path, record: dict, site: dict, new) -> dict:
    path = repo / site["file"]
    text, current, present = _json_current(repo, site)
    if present:
        result = jsonedit.replace(text, tuple(site["path"]), new)
    else:
        spec = site.get("insert")
        if not spec:
            raise ApplyConflict(f"{site['file']}: Pfad fehlt und kann nicht eingefügt werden")
        template = copy.deepcopy(spec["template"])
        # der Wert gehört an den letzten Pfadteil innerhalb der Vorlage
        leaf = site["path"][len(spec["parent"]) + 1:]
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
    return {"id": record["id"], "file": site["file"], "path": site["path"], "old": current, "new": new, "mc": site.get("mc")}


def write(repo: Path, record: dict, new) -> list[dict]:
    """Ein Wert (alle Stellen)."""
    return write_all(repo, [(record, new)])


def write_all(repo: Path, items: list[tuple]) -> list[dict]:
    """
    items: [(record, new)] oder [(record, new, prefer)] - prefer: Literale je Stelle (Reihenfolge wie
    sites_of), die genommen werden, wenn sie den neuen Wert ergeben (Original-Schreibweise beim
    Rücksetzen). Prüft vorher alles (ApplyConflict, nichts geschrieben) und setzt bei einem Fehler
    mitten im Schreiben die schon geschriebenen Dateien zurück.
    """
    items = [tuple(i) + (None,) * (3 - len(i)) for i in items]
    problems = check_all(repo, [(r, n) for r, n, _p in items])
    if problems:
        raise ApplyConflict("; ".join(p["message"] for p in problems))
    java_edits: dict[str, list] = {}
    json_edits: list = []
    for record, new, prefer in items:
        for index, site in enumerate(sites_of(record)):
            if site["kind"] == "json":
                json_edits.append((record, site, new))
            else:
                token = prefer[index] if prefer and index < len(prefer) else None
                java_edits.setdefault(site["file"], []).append((site, record["value"], new, record["id"], token))
    backups: dict[str, str] = {}

    def backup(rel):
        if rel not in backups:
            with open(repo / rel, encoding="utf-8", newline="") as handle:
                backups[rel] = handle.read()
    done: list[dict] = []
    try:
        for record, site, new in json_edits:
            backup(site["file"])
            done.append(_write_json(repo, record, site, new))
        for rel, edits in java_edits.items():
            backup(rel)
            written = javaedit.apply_file(repo, rel, [(s, e, n, t) for s, e, n, _vid, t in edits])
            ordered = sorted(edits, key=lambda e: e[0]["span"][0])
            for entry, (site, _e, new, vid, _t) in zip(written, ordered):
                done.append(dict(entry, id=vid, value=new))
    except Exception:
        for rel, text in backups.items():
            try:
                with open(repo / rel, "w", encoding="utf-8", newline="") as handle:
                    handle.write(text)
            except OSError:
                pass
        raise
    return done
