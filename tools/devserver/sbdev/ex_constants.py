"""
Balance-relevante Code-Konstanten: Ladungen, Abklingzeiten, Reichweiten, Tempo, Haltbarkeiten ...

Gelesen wird jede `static final int|float|double|long NAME = Ausdruck;` im gemeinsamen Code,
deren Name nach Balance klingt (Positivliste) und nicht nach Oberfläche/Technik (Negativliste).
Ausdruecke aus Zahlen und Konstanten derselben Klasse werden ausgerechnet; solche Werte sind
"abgeleitet" und nur lesbar - ändern soll man die Bestandteile. Was sich nicht ausrechnen laesst
(Methodenaufrufe, fremde Klassen), steht im Auslese-Bericht.

Eine Konstante mit reinem Zahlenliteral wirkt in der Mod: Speichern ersetzt das Literal
(javaedit) - in der Datei der Linie 26.2 und in den Zwillingen der anderen Linien (sites.py), wenn
dort dieselbe Konstante mit demselben Wert steht.
"""

from __future__ import annotations

import re
from pathlib import Path

from . import javasrc, sites
from .values import problem, value

ROOTS = ("common/src/shared/java/com/simplebuilding", "common/src/mc26_2/java/com/simplebuilding")
SKIP_DIRS = ("/gametest/", "/client/", "/datagen/", "/dev/", "/screen/", "/network/", "/compat/")

DECL = re.compile(r"\b(?:(?:public|private|protected)\s+)?static\s+final\s+(int|float|double|long)\s+([A-Z][A-Z0-9_]*)\s*=\s*([^;,]+);")

BALANCE = re.compile(
    r"CHANCE|COOLDOWN|DURABILITY|DAMAGE|ATTACK|SPEED|RANGE|RADIUS|CHARGE|COST|TICKS|DELAY|BONUS|PERIOD|"
    r"DEPTH|BOOST|FACTOR|CAPACITY|SQUARE|ENCHANTABILITY|WEAR|PEBBLES|WARMUP|STRENGTH|DISTANCE|DURATION|"
    r"MULTIPLIER|INTERVAL|PER_|LIMIT|BUDGET|LEVEL|AMOUNT|DRAIN|HUNGER|EXHAUSTION|REWARD|PEARLS|SECONDS|"
    r"LOSS|HARDNESS|GRAVITY|DRAG|LIFETIME|FUEL|BURN|COOK|SMELT|HEAL|SATURATION|NUTRITION|REACH|PULL|PUSH|"
    r"^COPPER$|^IRON$|^GOLD$|^DIAMOND$|^NETHERITE$|^ENDERITE$|MAX_ROWS|MAX_COUNT|MAX_SCANS|BREACH")
NOT_BALANCE = re.compile(
    r"SLOT|MODE_|VOLUME|PITCH|PARTICLE|BITS|MASK|GRID|CODEC|ROW_|TOPIC|CODE_LENGTH|TITLE|WRAP|COLOR|ARGB|RGB|"
    r"TEXTURE|PIXEL|WIDTH|HEIGHT|_X$|_Y$|_U$|_V$|PREVIEW|VISITS|EXPRESSION|REPEAT|NUMBER|CELLS|CONTENTS|"
    r"TOOLTIP|SYNC|VERSION|HASH|_ID$|INDEX|ENTRIES|PAGE|ICON|FRAME|ANIM|ALPHA|FONT|SCALE|OFFSET_X|OFFSET_Y")

LABEL_WORDS = {
    "CHANCE": "Chance", "COOLDOWN": "Abklingzeit", "DURABILITY": "Haltbarkeit", "DAMAGE": "Schaden",
    "SPEED": "Tempo", "RANGE": "Reichweite", "RADIUS": "Radius", "CHARGE": "Ladung", "COST": "Kosten",
    "TICKS": "Ticks", "DELAY": "Verzögerung",
}

_LITERAL = re.compile(r"-?\s*" + javasrc.NUMBER.pattern)


def _declarations(text: str):
    """[(java-Typ, NAME, Ausdruck, Anfang, Ende)] aller static-final-Zahlen einer Datei."""
    return [(m.group(1), m.group(2), m.group(3).strip(), m.start(3), m.start(3) + len(m.group(3).rstrip()))
            for m in DECL.finditer(text)]


def _evaluate_all(pending) -> dict[str, tuple]:
    env: dict[str, tuple] = {}
    results: dict[str, tuple] = {}
    progress = True
    # Bis zum Fixpunkt: Konstanten duerfen auf später deklarierte verweisen.
    while progress:
        progress = False
        for jtype, name, expr, _a, _b in pending:
            if name in results:
                continue
            try:
                number, is_int = javasrc.evaluate(expr, env)
            except javasrc.ExprError:
                continue
            if jtype in ("int", "long"):
                number, is_int = int(number), True
            else:
                is_int = False
            env[name] = (number, is_int)
            results[name] = (number, is_int)
            progress = True
    return results


def is_literal(expr: str) -> bool:
    return bool(_LITERAL.fullmatch(expr.strip()))


def read_sites(repo: Path, rel: str, mc: str | None = None) -> dict[str, dict]:
    """NAME -> Stelle (mit "value") jeder Literal-Konstante der Datei - für Zwillinge."""
    raw = (repo / rel).read_text(encoding="utf-8", errors="replace")
    text = javasrc.blank_comments(raw)
    lines = javasrc.Lines(text)
    pending = _declarations(text)
    results = _evaluate_all(pending)
    out = {}
    for jtype, name, expr, a, b in pending:
        if name not in results:
            continue
        site = sites.java_site(rel, text, lines, a, b, jtype, mc)
        number, is_int = results[name]
        site["value"] = javasrc.format_number(number, is_int) if is_literal(expr) else None
        if not is_literal(expr):
            spec = _transform(text, a, expr, jtype, results, rel, lines)
            if spec and "site" in spec:
                site = dict(spec["site"], value=javasrc.format_number(number, is_int))
                if mc:
                    site["mc"] = mc
            else:
                site["token"] = " ".join(expr.split())
        out[name] = site
    return out


def scan(repo: Path) -> tuple[dict[str, dict[str, dict]], list[dict], list[dict]]:
    """
    -> (je Klasse: NAME -> Wertdatensatz, alle Werte, Probleme)

    Die Werte einer Klasse sind auch per NAME greifbar, damit andere Leser (Beute) auf dieselben
    Datensaetze verweisen können.
    """
    by_class: dict[str, dict[str, dict]] = {}
    values: list[dict] = []
    problems: list[dict] = []
    seen_classes: dict[str, str] = {}
    for root in ROOTS:
        base = repo / root
        if not base.exists():
            continue
        for path in sorted(base.rglob("*.java")):
            rel = path.relative_to(repo).as_posix()
            if any(skip in "/" + rel for skip in SKIP_DIRS):
                continue
            raw = path.read_text(encoding="utf-8", errors="replace")
            if "static final" not in raw:
                continue
            text = javasrc.blank_comments(raw)
            lines = javasrc.Lines(text)
            pending = _declarations(text)
            if not pending:
                continue
            cls = path.stem
            if cls in seen_classes and seen_classes[cls] != rel:
                cls = f"{path.parent.name}.{cls}"
            seen_classes[cls] = rel
            results = _evaluate_all(pending)
            records: dict[str, dict] = {}
            for jtype, name, expr, a, b in pending:
                relevant = BALANCE.search(name) and not NOT_BALANCE.search(name)
                if name not in results:
                    if relevant:
                        problems.append(problem("constant", f"{cls}.{name} = {' '.join(expr.split())[:70]}",
                                                file=rel, line=lines.line(a),
                                                why=_why_not(expr)))
                    continue
                if not relevant:
                    continue
                number, is_int = results[name]
                literal = is_literal(expr)
                kind = "int" if jtype in ("int", "long") else "float"
                note = javasrc.doc_before(raw, a) or javasrc.trailing_comment(raw, a)
                source = sites.java_site(rel, text, lines, a, b, jtype)
                source["expr"] = " ".join(expr.split())
                source["lines"] = sites.main_lines(rel)
                record = value(f"const:{cls}.{name}", "constant", name, kind, javasrc.format_number(number, is_int),
                               group=cls, min=0 if number >= 0 else None, apply="mod" if literal else "plan",
                               source=source, refs={"class": cls, "javaType": jtype}, note=note[:400])
                if not literal:
                    record["derived"] = " ".join(expr.split())
                    spec = _transform(text, a, expr, jtype, results, rel, lines)
                    if spec is None:
                        record["readonly"] = True
                        record["apply"] = "plan"
                        source.pop("token", None)
                    elif "alias" in spec:
                        record["readonly"] = True
                        record["alias"] = spec["alias"]
                        record["apply"] = "mod"
                        source.pop("token", None)
                    else:
                        record["apply"] = "mod"
                        record["source"] = source = dict(spec["site"], expr=" ".join(expr.split()), lines=sites.main_lines(rel))
                if name.endswith("_CHANCE") and kind == "float" and 0 <= number <= 1:
                    record["type"] = "prob"
                records[name] = record
                values.append(record)
            if records:
                for rec in records.values():
                    alias = rec.get("alias")
                    if alias and isinstance(alias.get("name"), str):
                        target = records.get(alias.pop("name"))
                        if target is None or target.get("readonly"):
                            rec.pop("alias")
                            rec["apply"] = "plan"
                        else:
                            alias["id"] = target["id"]
                by_class[cls] = records
                writable = {f"const:{cls}.{n}": r for n, r in records.items() if not r.get("readonly")}
                if writable:
                    def read_twin(repo_, twin_rel, _cls=cls):
                        return {f"const:{_cls}.{n}": s for n, s in read_sites(repo_, twin_rel).items()}
                    sites.attach_twins(repo, writable, rel, read_twin)
    return by_class, values, problems


_BINARY_LEFT = re.compile(r"^\s*(-?\s*" + javasrc.NUMBER.pattern + r")\s*([*/+-])\s*(.+?)\s*$")
_BINARY_RIGHT = re.compile(r"^\s*(.+?)\s*([*/+-])\s*(" + javasrc.NUMBER.pattern + r")\s*$")


def _transform(text: str, a: int, expr: str, jtype: str, results: dict, rel: str, lines) -> dict | None:
    """
    Ein abgeleiteter Ausdruck mit genau einem schreibbaren Literal:

        48*4, 190 * BASE_DURABILITY_MULTIPLIER, -3.0f - ATTACK_SPEED_OFFSET  -> das linke Literal,
            mit "transform" (Wert = Literal op k); Speichern schreibt das Literal zurückgerechnet
        1.0f / 1200.0f                                                      -> das rechte Literal (Wert = k / Literal)
        CHARGE_TICKS * 2                                                    -> Verweis ("alias") auf CHARGE_TICKS x 2

    Anderes (mehrere Operatoren auf oberster Ebene, fremde Klassen) -> None (nur lesbar).
    """
    env = {k: v for k, v in results.items()}

    def value_of(sub: str):
        try:
            return javasrc.evaluate(sub, env)
        except javasrc.ExprError:
            return None
    left = _BINARY_LEFT.match(expr)
    right = _BINARY_RIGHT.match(expr)
    if right and re.fullmatch(r"[A-Z][A-Z0-9_]*", right.group(1)) and right.group(2) == "*" and right.group(1) in results:
        number, _ = javasrc.parse_number(right.group(3))
        return {"alias": {"name": right.group(1), "factor": number}}
    if left:
        lit, op, rest = left.group(1), left.group(2), left.group(3)
        if op == "/" and javasrc.NUMBER.fullmatch(rest.strip()) and jtype in ("float", "double"):
            k = value_of(lit)
            at = a + expr.index(rest.strip(), len(lit))
            site = sites.java_site(rel, text, lines, at, at + len(rest.strip()), jtype)
            site["transform"] = {"op": "/", "k": k[0], "operand": "right"}
            return {"site": site}
        if re.search(r"[+-]", rest.strip().lstrip("-")) and op in "*/":
            return None  # 2 * A + B: nicht eindeutig
        k = value_of(rest)
        if k is None or (op == "/" and jtype not in ("float", "double")):
            return None
        stripped = lit.replace(" ", "")
        at = a + (len(expr) - len(expr.lstrip()))
        site = sites.java_site(rel, text, lines, at, at + len(lit.rstrip()), jtype)
        site["token"] = text[at:at + len(lit.rstrip())].strip()
        if stripped != site["token"].replace(" ", ""):
            return None
        site["transform"] = {"op": op, "k": k[0], "operand": "left"}
        return {"site": site}
    return None


def _why_not(expr: str) -> str:
    if "ServerTuning." in expr or "getConfig()" in expr:
        return "kommt aus der Config (Standard auf der Seite Config-Standardwerte änderbar)"
    return "Ausdruck nicht statisch ausrechenbar (Methodenaufruf oder fremde Klasse)"
