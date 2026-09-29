"""
checkBalance: passen Ablage, Code und erzeugte Dateien zusammen?

Aufruf: ``python tools/devserver/serve.py --check`` (Gradle: ``gradlew checkBalance``, hängt an ``check``).
Fehler (Exit-Code 1):

1. **Ablage gegen Code**: ein gespeicherter Wert, den die Zentrale in die Mod geschrieben hat
   ("angewendet"), steht dort nicht mehr - jemand hat die Zahl im Code geändert, ohne die Zentrale.
   Lösung: in der Zentrale übernehmen (neu speichern) oder den Code zurücksetzen.
2. **Linien**: bei einem angewendeten Wert steht in einer anderen Linie (1.21.11, 26.3-Overlay) eine
   andere Zahl.
3. **Erzeugte Dateien gegen Code**: Beute-Inject-Tabellen, Verzauberungen, Rezepte, Erz-Generierung
   und der Item-Export (src/main/generated, mc1_21_11/.../generated, mc26_3/generated) passen nicht zu
   den Java-Zahlen, aus denen runDatagen sie macht - Datagen fehlt.

Warnungen (Exit-Code 0): geplante, noch nicht angewendete Werte; verwaiste Werte; Werte, deren Mod-Wert
sich seit dem Speichern bewegt hat, ohne dass sie angewendet waren.
"""

from __future__ import annotations

import json
from pathlib import Path

from . import jsonedit
from .values import same

INJECT = "data/simplebuilding/loot_table/inject/"
GENERATED_ROOTS = [("26.2", "src/main/generated/"), ("1.21.11", "mc1_21_11/fabric/src/main/generated/"), ("26.3", "mc26_3/generated/")]


class _Json:
    def __init__(self, repo: Path):
        self.repo = repo
        self.cache: dict[str, object] = {}

    def get(self, rel: str):
        if rel not in self.cache:
            path = self.repo / rel
            try:
                self.cache[rel] = json.loads(path.read_text(encoding="utf-8")) if path.is_file() else None
            except (OSError, json.JSONDecodeError):
                self.cache[rel] = None
        return self.cache[rel]


def applied_log(store_root: Path) -> dict[str, object]:
    """Id -> zuletzt per "Jetzt anwenden" geschriebener Wert."""
    out = {}
    path = Path(store_root) / "applied-log.jsonl"
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(entry, dict) and "id" in entry:
                out[entry["id"]] = entry.get("new")
    return out


def was_applied(entry: dict, log: dict, vid: str) -> bool:
    if entry.get("applied"):
        return True
    return vid in log and same(log[vid], entry.get("value"))


def run(snapshot: dict, state: dict, repo: Path, store_root: Path, record_for=None) -> dict:
    errors: list[dict] = []
    warnings: list[dict] = []
    values = snapshot["values"]
    record_for = record_for or values.get
    log = applied_log(store_root)
    stats = {"entries": len(state.get("entries", {})), "applied": 0, "planned": 0, "generatedChecked": 0}

    # 1 + 2: Ablage gegen Code und Linien
    for vid, entry in sorted(state.get("entries", {}).items()):
        record = record_for(vid)
        if record is None:
            warnings.append({"id": vid, "kind": "orphan", "message": f"{vid}: gibt es in der Mod nicht mehr (verwaist)"})
            continue
        if record.get("apply") != "mod":
            continue
        where = _where(record)
        if same(entry["value"], record["value"]):
            stats["applied"] += 1
            for twin in (record.get("source") or {}).get("twinsDiffer", []):
                errors.append({"id": vid, "kind": "line", "message":
                               f"{record['label']} ({record.get('group', '')}): Ablage und 26.2 = {entry['value']}, "
                               f"aber {twin['mc']} ({twin['file']}:{twin.get('line')}) = {twin.get('value')}"})
            continue
        if was_applied(entry, log, vid):
            errors.append({"id": vid, "kind": "drift", "message":
                           f"{record['label']} ({record.get('group', '')}): in der Zentrale gespeichert und angewendet = "
                           f"{entry['value']}, im Code steht jetzt {record['value']} ({where}). Im Code zurücksetzen oder "
                           "in der Zentrale den neuen Wert speichern."})
        else:
            stats["planned"] += 1
            warnings.append({"id": vid, "kind": "planned", "message":
                             f"{record['label']} ({record.get('group', '')}): geplant {entry['value']}, Mod {record['value']} "
                             f"({where}) - noch nicht angewendet"})

    # 3: erzeugte Dateien gegen Code
    js = _Json(repo)
    for vid, record in values.items():
        source = record.get("source") or {}
        gens = source.get("generated")
        for gen in gens if isinstance(gens, list) else []:
            expected = _value_for_line(record, gen.get("mc", "26.2"))
            if expected is None:
                continue
            data = js.get(gen["file"])
            if data is None:
                continue
            try:
                actual = jsonedit.get(data, gen["path"])
            except (KeyError, IndexError, TypeError):
                actual = None
            stats["generatedChecked"] += 1
            if actual is None or not same(actual, expected):
                errors.append({"id": vid, "kind": "datagen", "message":
                               f"{gen['file']} {'/'.join(map(str, gen['path']))} = {actual}, Code ({_where(record)}) = "
                               f"{expected} - runDatagen fehlt"})
        alias = record.get("alias")
        if alias and record.get("generatedStale") and record.get("apply") == "mod":
            target = values.get(alias["id"])
            if target is not None:
                stats["generatedChecked"] += 1
                errors.append({"id": vid, "kind": "datagen", "message":
                               f"src/main/generated/wiki/items.json {record['refs'].get('item')} {vid.rsplit(':', 1)[1]} = "
                               f"{record['value']}, Code {record.get('derived')} = {target['value'] * alias['factor']} - "
                               "runDatagen fehlt"})
    errors += _loot_generated(snapshot, js, stats)
    return {"ok": not errors, "errors": errors, "warnings": warnings, "stats": stats}


def _where(record: dict) -> str:
    source = record.get("source") or {}
    return f"{source.get('file', '?').split('/')[-1]}:{source.get('line', '?')}"


def _value_for_line(record: dict, mc: str):
    if mc == "26.2":
        return record.get("value")
    source = record.get("source") or {}
    if any(t.get("mc") == mc for t in source.get("twins", [])):
        return record.get("value")
    differ = next((t for t in source.get("twinsDiffer", []) if t.get("mc") == mc), None)
    return differ.get("value") if differ else None


# ---- Beute: Inject-Tabellen gegen ModLootTableModifications ------------------------------------

def _rolls(raw):
    """JSON-Würfe -> {"type", ...} (alle drei Linien: Zahl, {"type": uniform|binomial|constant})."""
    if isinstance(raw, (int, float)):
        return {"type": "exactly", "n": raw}
    if isinstance(raw, dict):
        kind = raw.get("type", "").split(":")[-1]
        if kind == "uniform":
            return {"type": "uniform", "min": raw.get("min"), "max": raw.get("max")}
        if kind == "binomial":
            return {"type": "binomial", "n": raw.get("n"), "p": raw.get("p")}
        if kind == "constant":
            return {"type": "exactly", "n": raw.get("value")}
    return {"type": "?"}


def _core_chance(pool: dict):
    conditions = pool.get("conditions") or ([pool["condition"]] if isinstance(pool.get("condition"), dict) else [])
    for cond in conditions:
        if str(cond.get("condition") or cond.get("type") or "").endswith("core_chance"):
            return cond.get("chance")
    return None


def _count(entry: dict):
    mods = entry.get("functions") or entry.get("modifier") or entry.get("modifiers") or []
    if isinstance(mods, dict):
        mods = [mods]
    for fn in mods:
        if str(fn.get("function") or fn.get("type") or "").endswith("set_count"):
            count = fn.get("count")
            if isinstance(count, (int, float)):
                return [count, count]
            if isinstance(count, dict):
                return [count.get("min"), count.get("max")]
    return None


def _loot_generated(snapshot: dict, js: _Json, stats: dict) -> list[dict]:
    errors = []
    values = snapshot["values"]

    def val(vid, fallback):
        record = values.get(vid) if vid else None
        return record["value"] if record else fallback

    for table in snapshot.get("loot", {}).get("tables", []):
        ns, path = table["id"].split(":", 1)
        if ns != "minecraft":
            continue
        for mc, root in GENERATED_ROOTS:
            rel = f"{root}{INJECT}{path}.json"
            data = js.get(rel)
            if data is None:
                continue
            stats["generatedChecked"] += 1
            problems = []
            pools = data.get("pools") or []
            if len(pools) != len(table["pools"]):
                problems.append(f"{len(pools)} Pools, Code hat {len(table['pools'])}")
            for pi, (jpool, cpool) in enumerate(zip(pools, table["pools"])):
                rolls = cpool["rolls"]
                if cpool.get("rareCore"):
                    chance = _core_chance(jpool)
                    want = val(rolls["ids"].get("p"), rolls.get("p"))
                    if chance is None or not same(chance, want):
                        problems.append(f"Pool {pi + 1}: Kern-Chance {chance}, Code {want}")
                    continue
                jr = _rolls(jpool.get("rolls"))
                for key in ("n", "min", "max", "p"):
                    if key in rolls and key != "type":
                        want = val(rolls["ids"].get(key), rolls.get(key))
                        if want is not None and (jr.get(key) is None or not same(jr.get(key), want)):
                            problems.append(f"Pool {pi + 1}: Würfe {key} {jr.get(key)}, Code {want}")
                jentries = jpool.get("entries") or []
                if len(jentries) != len(cpool["entries"]):
                    problems.append(f"Pool {pi + 1}: {len(jentries)} Einträge, Code {len(cpool['entries'])}")
                    continue
                for je, ce in zip(jentries, cpool["entries"]):
                    want_w = val(ce["ids"].get("weight"), ce.get("weight", 1))
                    if not same(je.get("weight", 1), want_w):
                        problems.append(f"Pool {pi + 1} {ce.get('key')}: Gewicht {je.get('weight', 1)}, Code {want_w}")
                    if ce["ids"].get("min"):
                        jc = _count(je) or [None, None]
                        for idx, key in ((0, "min"), (1, "max")):
                            want_c = val(ce["ids"].get(key), None)
                            if want_c is not None and (jc[idx] is None or not same(jc[idx], want_c)):
                                problems.append(f"Pool {pi + 1} {ce.get('key')}: Anzahl {key} {jc[idx]}, Code {want_c}")
            if problems:
                errors.append({"id": f"loot:{table['id']}", "kind": "datagen", "message":
                               f"{rel} passt nicht zu ModLootTableModifications ({mc}): " + "; ".join(problems[:6])
                               + (" ..." if len(problems) > 6 else "") + " - runDatagen fehlt"})
    return errors


def format_report(result: dict) -> str:
    lines = []
    for err in result["errors"]:
        lines.append(f"FEHLER  {err['message']}")
    for warn in result["warnings"]:
        lines.append(f"Hinweis {warn['message']}")
    s = result["stats"]
    lines.append(f"checkBalance: {'ok' if result['ok'] else 'FEHLER'} - {s['entries']} gespeicherte Werte "
                 f"({s['applied']} in der Mod, {s['planned']} nur geplant), {s['generatedChecked']} erzeugte Stellen geprüft, "
                 f"{len(result['errors'])} Fehler, {len(result['warnings'])} Hinweise")
    if not result["ok"]:
        lines.append("Lösung: Datagen fehlt -> gradlew runDatagen :mc26_3:fabric:runDatagen :mc1_21_11:fabric:runDatagen "
                     "(oder in der Balancing-Zentrale 'Datagen starten'); Ablage/Code weichen ab -> Zentrale öffnen "
                     "(python tools/devserver/serve.py), Seite Übersicht.")
    return "\n".join(lines)
