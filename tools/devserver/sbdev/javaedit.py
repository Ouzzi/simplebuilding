"""
Zahlenliterale im Java-Quelltext chirurgisch ersetzen - das Gegenstueck zu jsonedit für Java.

Die Leser (ex_loot, ex_constants, ex_config, ex_javadata) kennen für jeden Wert die genaue Stelle
im Quelltext (Zeichenbereich im Text mit LF-Zeilenenden, so wie ``Path.read_text`` ihn liefert).
Hier wird genau dieses Literal ersetzt:

* Vorab-Prüfung: an der Stelle muss noch das eingelesene Literal mit dem eingelesenen Wert stehen,
  sonst wird nichts geschrieben (ein paralleler Lauf hat die Datei geändert -> neu einlesen).
* Schreibweise bleibt: Suffix (``f``/``F``/``d``/``L``), Hex, Unterstriche, ``2.0f`` bleibt mit
  Nachkommastelle. Ein Rücksetzen auf den alten Wert ergibt wieder exakt das alte Literal - darum
  ist ``git diff`` nach einem Rollback leer (Test ``test_javaedit``).
* Zeilenenden (CRLF/LF) und alles andere in der Datei bleiben Byte für Byte.
* Mehrere Stellen derselben Datei werden in einem Durchgang von hinten nach vorn ersetzt, damit
  sich die Bereiche nicht verschieben.
"""

from __future__ import annotations

import json
import os
import re
import time
from decimal import Decimal
from pathlib import Path

from . import javasrc
from .values import same


class JavaEditError(Exception):
    pass


def read(path: Path) -> tuple[str, str]:
    """-> (Rohtext mit Original-Zeilenenden, Text mit LF - dessen Offsets die Leser benutzen)."""
    with open(path, encoding="utf-8", newline="") as handle:
        raw = handle.read()
    return raw, raw.replace("\r\n", "\n")


def _raw_offset(raw: str, crlf_before: list[int], offset: int) -> int:
    """LF-Offset -> Offset im Rohtext (jedes CRLF davor ist ein Zeichen länger)."""
    import bisect
    return offset + bisect.bisect_left(crlf_before, offset)


def _crlf_positions(raw: str) -> list[int]:
    """LF-Offsets der Zeilenumbrüche, die im Rohtext CRLF sind."""
    out = []
    removed = 0
    for m in re.finditer("\r\n", raw):
        out.append(m.start() - removed)
        removed += 1
    return out


def token_value(token: str):
    """Literal (Zahl auch mit Vorzeichen, true/false, "Text") -> Wert; None, wenn es kein reines Literal ist."""
    t = token.strip()
    if t in ("true", "false"):
        return t == "true"
    if len(t) >= 2 and t[0] == '"' and t[-1] == '"':
        try:
            return json.loads(t)
        except ValueError:
            return None
    sign = 1
    if t.startswith("-"):
        sign = -1
        t = t[1:].strip()
    if not javasrc.NUMBER.fullmatch(t):
        return None
    number, is_int = javasrc.parse_number(t)
    return javasrc.format_number(sign * number, is_int)


def format_literal(new, old_token: str, jtype: str) -> str:
    """
    Neues Literal in der Schreibweise des alten. jtype: int | long | float | double (der Typ, den
    Java an dieser Stelle erwartet; bestimmt, ob ein Suffix nötig ist).
    """
    old = old_token.strip()
    if jtype == "boolean":
        if not isinstance(new, bool):
            raise JavaEditError(f"{new!r} ist kein Wahrheitswert")
        return "true" if new else "false"
    if jtype == "String":
        if not isinstance(new, str):
            raise JavaEditError(f"{new!r} ist kein Text")
        return json.dumps(new, ensure_ascii=False)
    negative_old = old.startswith("-")
    body = old[1:].strip() if negative_old else old
    suffix = body[-1] if body and body[-1] in "fFdDlL" and not body.lower().startswith("0x") else ""
    if body.lower().startswith("0x") and body[-1] in "lL":
        suffix = body[-1]
    digits = body[:-1] if suffix else body
    if jtype in ("int", "long"):
        if isinstance(new, float):
            if not new.is_integer():
                raise JavaEditError(f"{new} ist keine ganze Zahl")
            new = int(new)
        sign = "-" if new < 0 else ""
        mag = abs(int(new))
        if digits.lower().startswith("0x"):
            hexdigits = digits[2:].replace("_", "")
            upper = any(c in "ABCDEF" for c in hexdigits) or not any(c in "abcdef" for c in hexdigits)
            text = format(mag, "X" if upper else "x").rjust(len(hexdigits), "0")
            return f"{sign}{digits[:2]}{text}{suffix}"
        text = str(mag)
        if "_" in digits:
            text = f"{mag:,}".replace(",", "_")
        return f"{sign}{text}{suffix}"
    # float / double
    value = float(new)
    sign = "-" if value < 0 else ""
    value = abs(value)
    text = repr(value)
    if "e" in text or "E" in text:
        text = format(Decimal(text), "f")
    had_point = "." in digits or "e" in digits.lower()
    if text.endswith(".0") and not had_point:
        text = text[:-2]            # 2f bleibt 2f, 5F bleibt 5F, 20 (int im float-Ausdruck) bleibt 20
    old_frac = digits.split(".", 1)[1] if "." in digits else ""
    if old_frac.endswith("0") and "." in text:
        # Schreibweise mit Nullen am Ende (0.10f): gleich viele Nachkommastellen behalten
        whole, frac = text.split(".", 1)
        if len(frac) < len(old_frac):
            text = f"{whole}.{frac.ljust(len(old_frac), '0')}"
    if "." in text and not suffix and jtype == "float":
        suffix = "f"                # 22.5 wäre in Java ein double - im float-Kontext ein Übersetzungsfehler
    return f"{sign}{text}{suffix}"


def forward(site: dict, literal_value):
    """Wert, den der Code aus dem Literal macht (Stellen mit "transform", z. B. 64*4 oder -3.0f - OFFSET)."""
    spec = site.get("transform")
    if not spec or literal_value is None or isinstance(literal_value, bool):
        return literal_value
    k, op = spec["k"], spec["op"]
    a, b = (literal_value, k) if spec["operand"] == "left" else (k, literal_value)
    is_int = site.get("jtype") in ("int", "long")
    if op == "*":
        out = a * b
    elif op == "+":
        out = a + b
    elif op == "-":
        out = a - b
    else:
        if b == 0:
            return None
        out = int(a / b) if is_int else a / b
    return int(out) if is_int else javasrc.format_number(out, False)


def inverse(site: dict, value):
    """Literal, das den gewünschten Wert ergibt; JavaEditError, wenn es keins gibt (z. B. kein Vielfaches)."""
    spec = site.get("transform")
    if not spec or isinstance(value, bool) or isinstance(value, str):
        return value
    k, op, left = spec["k"], spec["op"], spec["operand"] == "left"
    is_int = site.get("jtype") in ("int", "long")
    if op == "*":
        if k == 0:
            raise JavaEditError("Faktor 0 - nicht umkehrbar")
        out = value / k
    elif op == "+":
        out = value - k
    elif op == "-":
        out = value + k if left else k - value
    else:
        if left:
            out = value * k
        else:
            if value == 0:
                raise JavaEditError("0 ist hier nicht möglich (Division)")
            out = k / value
    if is_int:
        if abs(out - round(out)) > 1e-9:
            raise JavaEditError(f"{value} geht hier nicht: im Code steht {site.get('expr') or 'ein Ausdruck'} - "
                                f"der Wert muss sich als ganze Zahl {'mal ' + str(k) if op == '*' else ''} schreiben lassen")
        return int(round(out))
    return javasrc.format_number(out, False)


def check_site(text: str, site: dict, expected) -> str | None:
    """Fehlermeldung, wenn an der Stelle nicht mehr der eingelesene Wert steht (sonst None)."""
    a, b = site["span"]
    if b > len(text):
        return f"{site['file']}: Stelle liegt hinter dem Dateiende"
    token = text[a:b]
    if site.get("token") is not None and token.strip() != site["token"].strip():
        return (f"{site['file']}:{site.get('line', '?')} wurde seit dem Einlesen geändert (dort steht jetzt "
                f"'{token.strip()[:40]}', eingelesen '{site['token'][:40]}')")
    value = forward(site, token_value(token))
    if value is None or not same(value, expected):
        return f"{site['file']}:{site.get('line', '?')}: dort steht '{token.strip()[:40]}', erwartet {expected}"
    return None


def literal_for(site: dict, old_token: str, new, prefer: str | None = None) -> str:
    """
    Das Literal, das an die Stelle kommt. prefer: ein früheres Literal dieser Stelle (das Original aus
    der Ablage) - ergibt es denselben Wert, wird es genommen. So ist ein Rücksetzen Byte für Byte das
    alte Literal, auch bei ungewöhnlicher Schreibweise (1.5e-3f, .5, 1_000).
    """
    if prefer is not None and same(forward(site, token_value(prefer)), new):
        return prefer.strip()
    if same(forward(site, token_value(old_token)), new):
        return old_token.strip()
    return format_literal(inverse(site, new), old_token, site.get("jtype") or ("int" if isinstance(new, int) else "double"))


def apply_file(repo: Path, rel: str, edits: list[tuple]) -> list[dict]:
    """
    edits: [(site, expected, new[, prefer])] für EINE Datei. Prüft alle Stellen, ersetzt sie von
    hinten nach vorn und schreibt die Datei atomar. Wirft JavaEditError, ohne etwas zu schreiben,
    wenn eine Stelle nicht mehr passt.
    """
    path = repo / rel
    raw, text = read(path)
    edits = [tuple(e) + (None,) * (4 - len(e)) for e in edits]
    for site, expected, _new, _prefer in edits:
        err = check_site(text, site, expected)
        if err:
            raise JavaEditError(err)
    spans = sorted((tuple(e[0]["span"]) for e in edits))
    for (a1, b1), (a2, _b2) in zip(spans, spans[1:]):
        if a2 < b1:
            raise JavaEditError(f"{rel}: zwei Werte an derselben Stelle ({a1}-{b1})")
    crlf = _crlf_positions(raw)
    done = []
    for site, expected, new, prefer in sorted(edits, key=lambda e: -e[0]["span"][0]):
        a, b = site["span"]
        old_token = text[a:b]
        literal = literal_for(site, old_token, new, prefer)
        if not same(forward(site, token_value(literal)), new):
            raise JavaEditError(f"{rel}: {new} lässt sich nicht als Literal schreiben ({literal})")
        ra, rb = _raw_offset(raw, crlf, a), _raw_offset(raw, crlf, b)
        raw = raw[:ra] + literal + raw[rb:]
        done.append({"file": rel, "line": site.get("line"), "old": old_token.strip(), "new": literal, "mc": site.get("mc")})
    tmp = path.with_name(f".{path.name}.{os.getpid()}.{time.time_ns()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="") as handle:
        handle.write(raw)
        handle.flush()
        os.fsync(handle.fileno())
    for attempt in range(40):
        try:
            os.replace(tmp, path)
            break
        except PermissionError:
            if attempt == 39:
                raise
            time.sleep(0.05)
    return list(reversed(done))
