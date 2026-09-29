"""
Ein Balance-Wert, wie ihn Auslesen, Ablage, API und Oberfläche teilen - als einfaches dict.

    id        stabiler Schlüssel, z. B. "trade:simplebuilding:wandering_trader/emerald_iron_cores:price"
    category  loot | trade | mobdrop | blockdrop | item | enchant | worldgen | recipe | constant | config | param | source
    group     Ueberschrift innerhalb der Kategorie (Tabelle, Händler, Klasse, ...)
    label     kurzer Name der Spalte/Zeile
    type      int | float | prob | bool | string | json
    value     aktueller Wert in der Mod (bzw. Standard der Annahme)
    min, max  erlaubter Bereich (None = offen)
    apply     "mod"    - Speichern schreibt den Wert direkt in die Mod-Datei (Phase 1: Handel)
              "phase2" - nur Planung; wirkt erst nach Phase 2 (docs/BALANCING-ZENTRALE.md)
              "tool"   - nur für die Rechner (Annahmen), wirkt nie in der Mod
    source    {"file": repo-relativ, "line": n, "path": [...] (JSON) oder "span": [a, b] (Java)}
    refs      Querverweise für die Oberfläche und die Rechner (item, table, trade, ...)
"""

from __future__ import annotations

import math

CATEGORIES = {
    "loot": "Beute (Truhen, Tresore, Angeln)",
    "trade": "Handel",
    "mobdrop": "Mob-Drops",
    "blockdrop": "Block-Drops",
    "item": "Werkzeuge & Rüstung",
    "enchant": "Verzauberungen",
    "worldgen": "Welt (Erze)",
    "recipe": "Rezepte",
    "constant": "Code-Konstanten",
    "config": "Config-Standardwerte",
    "param": "Rechner-Annahmen",
    "source": "Geplante Quellen",
}

TYPES = ("int", "float", "prob", "bool", "string", "json")


def value(id: str, category: str, label: str, type: str, current, *, group: str = "",
          min=None, max=None, apply: str = "phase2", source: dict | None = None,
          refs: dict | None = None, unit: str = "", note: str = "", nullable: bool = False) -> dict:
    assert category in CATEGORIES, category
    assert type in TYPES, type
    record = {
        "id": id, "category": category, "group": group, "label": label, "type": type,
        "value": current, "min": min, "max": max, "apply": apply,
        "source": source or {}, "refs": refs or {},
    }
    if unit:
        record["unit"] = unit
    if note:
        record["note"] = note
    if nullable:
        record["nullable"] = True
    return record


def problem(area: str, message: str, *, file: str = "", line: int | None = None, why: str = "") -> dict:
    out = {"area": area, "message": message}
    if file:
        out["file"] = file
    if line:
        out["line"] = line
    if why:
        out["why"] = why
    return out


class Invalid(ValueError):
    """Ein Wert, der nicht gespeichert werden darf - mit einer Meldung für den Besitzer."""


def validate(record: dict, raw):
    """
    Prueft und normalisiert einen neuen Wert gegen seinen Datensatz. Wirft Invalid mit einer
    deutschen, konkreten Meldung. Strings wie "1,5" werden für Zahlen akzeptiert.
    """
    kind = record["type"]
    label = record.get("label") or record["id"]
    if raw is None:
        if record.get("nullable"):
            return None
        raise Invalid(f"{label}: ein Wert ist nötig.")
    if kind == "bool":
        if isinstance(raw, bool):
            return raw
        if isinstance(raw, str) and raw.strip().lower() in ("true", "false", "an", "aus", "on", "off", "ja", "nein"):
            return raw.strip().lower() in ("true", "an", "on", "ja")
        raise Invalid(f"{label}: erwartet an/aus, bekommen {raw!r}.")
    if kind == "string":
        if not isinstance(raw, str):
            raise Invalid(f"{label}: erwartet Text, bekommen {raw!r}.")
        if len(raw) > 2000:
            raise Invalid(f"{label}: Text zu lang (höchstens 2000 Zeichen).")
        return raw
    if kind == "json":
        return raw
    number = _to_number(raw, label)
    if kind == "int":
        if isinstance(number, float) and not number.is_integer():
            raise Invalid(f"{label}: erwartet eine ganze Zahl, bekommen {raw!r}.")
        number = int(number)
    else:
        number = float(number)
    lo, hi = record.get("min"), record.get("max")
    if kind == "prob":
        lo = 0.0 if lo is None else max(lo, 0.0)
        hi = 1.0 if hi is None else min(hi, 1.0)
    if lo is not None and number < lo:
        raise Invalid(f"{label}: {_fmt(number, kind)} ist kleiner als das Minimum {_fmt(lo, kind)}.")
    if hi is not None and number > hi:
        raise Invalid(f"{label}: {_fmt(number, kind)} ist größer als das Maximum {_fmt(hi, kind)}.")
    return number


def _to_number(raw, label):
    if isinstance(raw, bool):
        raise Invalid(f"{label}: erwartet eine Zahl, bekommen {raw!r}.")
    if isinstance(raw, (int, float)):
        number = raw
    elif isinstance(raw, str):
        text = raw.strip().replace(" ", "").replace(" ", "")
        percent = text.endswith("%")
        if percent:
            raise Invalid(f"{label}: bitte ohne %-Zeichen senden (die Oberfläche rechnet Prozent um).")
        if text.count(",") == 1 and "." not in text:
            text = text.replace(",", ".")
        try:
            number = float(text) if any(ch in text for ch in ".eE") else int(text)
        except ValueError:
            raise Invalid(f"{label}: {raw!r} ist keine Zahl.") from None
    else:
        raise Invalid(f"{label}: erwartet eine Zahl, bekommen {type(raw).__name__}.")
    if isinstance(number, float) and not math.isfinite(number):
        raise Invalid(f"{label}: {raw!r} ist keine endliche Zahl.")
    return number


def _fmt(number, kind):
    if kind == "prob":
        return f"{number * 100:.4g} %"
    return f"{number:g}" if isinstance(number, float) else str(number)


def same(a, b) -> bool:
    """Gleichheit für Werte: Zahlen tolerant (float-Rundung), sonst exakt."""
    if isinstance(a, bool) or isinstance(b, bool):
        return a is b or a == b and type(a) is type(b)
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return math.isclose(float(a), float(b), rel_tol=1e-9, abs_tol=1e-12)
    return a == b
