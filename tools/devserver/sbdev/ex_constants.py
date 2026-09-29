"""
Balance-relevante Code-Konstanten: Ladungen, Abklingzeiten, Reichweiten, Tempo, Haltbarkeiten ...

Gelesen wird jede `static final int|float|double|long NAME = Ausdruck;` im gemeinsamen Code,
deren Name nach Balance klingt (Positivliste) und nicht nach Oberfläche/Technik (Negativliste).
Ausdruecke aus Zahlen und Konstanten derselben Klasse werden ausgerechnet; solche Werte sind
"abgeleitet" und nur lesbar - ändern soll man die Bestandteile. Was sich nicht ausrechnen laesst
(Methodenaufrufe, fremde Klassen), steht im Auslese-Bericht.
"""

from __future__ import annotations

import re
from pathlib import Path

from . import javasrc
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
            decls = list(DECL.finditer(text))
            if not decls:
                continue
            cls = path.stem
            if cls in seen_classes and seen_classes[cls] != rel:
                cls = f"{path.parent.name}.{cls}"
            seen_classes[cls] = rel
            env: dict[str, tuple] = {}
            pending = [(m.group(1), m.group(2), m.group(3).strip(), m.start(3), m.start(3) + len(m.group(3).rstrip())) for m in decls]
            results: dict[str, tuple] = {}
            # Bis zum Fixpunkt: Konstanten duerfen auf später deklarierte verweisen.
            progress = True
            while progress:
                progress = False
                for jtype, name, expr, a, b in pending:
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
            records: dict[str, dict] = {}
            for jtype, name, expr, a, b in pending:
                relevant = BALANCE.search(name) and not NOT_BALANCE.search(name)
                if name not in results:
                    if relevant:
                        problems.append(problem("constant", f"{cls}.{name} = {' '.join(expr.split())[:70]}",
                                                file=rel, line=lines.line(a),
                                                why="Ausdruck nicht statisch ausrechenbar (Methodenaufruf oder fremde Klasse)"))
                    continue
                if not relevant:
                    continue
                number, is_int = results[name]
                literal = bool(javasrc.NUMBER.fullmatch(expr.replace(" ", "")) or re.fullmatch(r"-\s*" + javasrc.NUMBER.pattern, expr))
                kind = "int" if jtype in ("int", "long") else "float"
                note = javasrc.doc_before(raw, a) or javasrc.trailing_comment(raw, a)
                record = value(f"const:{cls}.{name}", "constant", name, kind, javasrc.format_number(number, is_int),
                               group=cls, min=0 if number >= 0 else None,
                               source={"file": rel, "line": lines.line(a), "span": [a, b], "expr": " ".join(expr.split())},
                               refs={"class": cls, "javaType": jtype}, note=note[:400])
                if not literal:
                    record["readonly"] = True
                    record["derived"] = " ".join(expr.split())
                if name.endswith("_CHANCE") and kind == "float" and 0 <= number <= 1:
                    record["type"] = "prob"
                records[name] = record
                values.append(record)
            if records:
                by_class[cls] = records
    return by_class, values, problems
