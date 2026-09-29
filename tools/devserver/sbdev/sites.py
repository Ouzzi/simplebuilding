"""
Wo ein Wert im Repo steht - in allen Minecraft-Linien.

Die Zentrale liest die Linie 26.2. Dieselbe Zahl steht aber oft noch an anderen Stellen:

    common/src/shared/java/X      26.2, 26.3, 26.4 (eine Datei für alle drei)  + 1.21.11: mc1_21_11/shared/java/X
    common/src/mc26_2/java/X      nur 26.2  + 26.3: mc26_3/overlay/java/X  + 26.4: mc26_4/overlay/java/X  + 1.21.11
    src/main/java/X               Fabric 26.2 und 26.3 (Datagen)  + 1.21.11: mc1_21_11/fabric/src/main/java/X
    src/mc26_2/java/X             nur Fabric 26.2  + 26.3/26.4-Overlays  + 1.21.11
    src/main/resources/data/...   26.2 und 26.3 (Handel)  - 1.21.11 baut Handel in Java (ex_javadata)

Ein Wert "wirkt in der Mod", wenn die Zentrale ihn an seiner Stelle schreiben kann. Die Stellen der
anderen Linien heissen hier Zwillinge: gleiche Id, im Zwilling mit demselben Leser gefunden. Steht
dort derselbe Wert, schreibt Speichern beide; weicht er ab oder fehlt er, sagt das der Wert
(``twinNotes``) und die Linie bleibt, wie sie ist.
"""

from __future__ import annotations

from pathlib import Path

LINES_SHARED = ["26.2", "26.3", "26.4"]

_TWIN_ROOTS = [
    ("common/src/shared/java/", LINES_SHARED, [("1.21.11", "mc1_21_11/shared/java/")]),
    ("common/src/mc26_2/java/", ["26.2"], [("26.3", "mc26_3/overlay/java/"), ("26.4", "mc26_4/overlay/java/"),
                                           ("1.21.11", "mc1_21_11/shared/java/")]),
    ("src/main/java/", ["26.2", "26.3"], [("1.21.11", "mc1_21_11/fabric/src/main/java/")]),
    ("src/mc26_2/java/", ["26.2"], [("26.3", "mc26_3/fabric/src/main/java/"), ("26.4", "mc26_4/fabric/src/main/java/"),
                                    ("1.21.11", "mc1_21_11/fabric/src/main/java/")]),
    ("src/main/resources/", ["26.2", "26.3"], []),
]


def main_lines(rel: str) -> list[str]:
    """Die Linien, für die die Datei der Linie 26.2 selbst gilt."""
    for prefix, lines, _twins in _TWIN_ROOTS:
        if rel.startswith(prefix):
            return list(lines)
    return ["26.2"]


def twin_paths(repo: Path, rel: str) -> list[tuple[str, str]]:
    """Vorhandene Zwillingsdateien: [(Linie, repo-relativer Pfad)]."""
    out = []
    for prefix, _lines, twins in _TWIN_ROOTS:
        if rel.startswith(prefix):
            rest = rel[len(prefix):]
            for mc, root in twins:
                path = root + rest
                if (repo / path).is_file():
                    out.append((mc, path))
            break
    return out


def java_site(rel: str, text: str, lines, start: int, end: int, jtype: str, mc: str | None = None) -> dict:
    """Eine schreibbare Stelle im Java-Quelltext (Offsets im LF-Text)."""
    site = {"kind": "java", "file": rel, "line": lines.line(start), "span": [start, end], "token": text[start:end].strip(),
            "jtype": jtype}
    if mc:
        site["mc"] = mc
    return site


def attach_twins(repo: Path, records_by_id: dict[str, dict], main_file: str, read_twin) -> None:
    """
    Sucht in jeder Zwillingsdatei von main_file dieselben Ids.

    read_twin(repo, twin_rel) -> {id: site-dict mit "value"} (derselbe Leser wie für die Hauptdatei,
    nur auf die andere Datei). Gleicher Wert -> source.twins (wird mitgeschrieben); anderer Wert
    oder fehlend -> source.twinNotes.
    """
    for mc, twin_rel in twin_paths(repo, main_file):
        try:
            found = read_twin(repo, twin_rel)
        except Exception as err:  # ein kaputter Zwilling darf die Auslese nicht stoppen
            for record in records_by_id.values():
                record["source"].setdefault("twinNotes", []).append(f"{mc}: {twin_rel} nicht lesbar ({err})")
            continue
        add_found(records_by_id, found, mc, twin_rel)


def add_found(records_by_id: dict[str, dict], found: dict, mc: str, twin_rel: str) -> None:
    from .values import same
    for vid, record in records_by_id.items():
        site = found.get(vid)
        source = record["source"]
        if site is None:
            source.setdefault("twinNotes", []).append(f"{mc}: in {twin_rel.split('/')[-1]} nicht gefunden - bleibt, wie es ist")
            continue
        if site.get("value") is None or not same(site["value"], record["value"]):
            shown = site.get("value") if site.get("value") is not None else site.get("token", "?")
            source.setdefault("twinNotes", []).append(
                f"{mc}: {twin_rel.split('/')[-1]}:{site.get('line')} steht auf {shown} (weicht ab) - wird nicht mitgeschrieben")
            source.setdefault("twinsDiffer", []).append({"mc": mc, "file": twin_rel, "line": site.get("line"), "value": site.get("value")})
            continue
        twin = {k: v for k, v in site.items() if k != "value"}
        twin["mc"] = mc
        source.setdefault("twins", []).append(twin)


def lines_of(record: dict) -> list[str]:
    """Alle Linien, in denen Speichern den Wert ändert."""
    source = record.get("source") or {}
    out = list(source.get("lines") or main_lines(source.get("file", "")))
    for twin in source.get("twins", []):
        if twin.get("mc") and twin["mc"] not in out:
            out.append(twin["mc"])
    if "26.3" in out and "26.4" not in out and any(t.get("mc") == "26.3" and "/overlay/" in t.get("file", "")
                                                   for t in source.get("twins", [])):
        out.append("26.4")  # die 26.4-Linie übernimmt die 26.3-Overlays, solange sie keine eigene Datei hat
    order = {"26.2": 0, "26.3": 1, "26.4": 2, "1.21.11": 3}
    return sorted(out, key=lambda m: order.get(m, 9))
