"""
JSON-Dateien chirurgisch ändern: genau einen Wert ersetzen (oder einen Schlüssel einfügen) und
den Rest der Datei Byte für Byte stehen lassen - Einrueckung, Reihenfolge, Leerzeichen, alles.

Warum nicht json.load + json.dump? Die Handelsdateien sind von Hand formatiert
({ "id": "minecraft:emerald", "count": 32 } in einer Zeile); ein Neuschreiben machte aus jeder
Preisaenderung einen Diff über die ganze Datei und Merge-Konflikte mit parallelen Laeufen.

Jede Änderung wird danach geprüft: die neue Datei muss gültiges JSON sein und sich vom alten
Inhalt an genau dem einen Pfad unterscheiden - sonst Fehler und keine Änderung.
"""

from __future__ import annotations

import copy
import json
import math
import re

_WS = re.compile(r"[ \t\r\n]*")
_NUM = re.compile(r"-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?")
_STR = re.compile(r'"(?:[^"\\]|\\.)*"')


class JsonEditError(ValueError):
    pass


def scan(text: str) -> dict[tuple, dict]:
    """
    Pfad -> {"start", "end", "kind", "keyStart"} für jeden Wert der Datei (auch Objekte/Listen).
    Der Wurzelpfad ist (). keyStart = Anfang des Schlüssel-Strings (für Einfügen davor).
    """
    spans: dict[tuple, dict] = {}

    def ws(i):
        return _WS.match(text, i).end()

    def parse(i, path, key_start=None):
        i = ws(i)
        if i >= len(text):
            raise JsonEditError("unerwartetes Dateiende")
        c = text[i]
        if c == "{":
            start = i
            i = ws(i + 1)
            if text[i] == "}":
                spans[path] = {"start": start, "end": i + 1, "kind": "object", "keyStart": key_start}
                return i + 1
            while True:
                i = ws(i)
                m = _STR.match(text, i)
                if not m:
                    raise JsonEditError(f"Schlüssel erwartet bei {i}")
                k_start = i
                key = json.loads(m.group(0))
                i = ws(m.end())
                if text[i] != ":":
                    raise JsonEditError(f"':' erwartet bei {i}")
                i = parse(i + 1, path + (key,), k_start)
                i = ws(i)
                if text[i] == ",":
                    i += 1
                    continue
                if text[i] == "}":
                    spans[path] = {"start": start, "end": i + 1, "kind": "object", "keyStart": key_start}
                    return i + 1
                raise JsonEditError(f"',' oder '}}' erwartet bei {i}")
        if c == "[":
            start = i
            i = ws(i + 1)
            index = 0
            if text[i] == "]":
                spans[path] = {"start": start, "end": i + 1, "kind": "array", "keyStart": key_start}
                return i + 1
            while True:
                i = parse(i, path + (index,))
                index += 1
                i = ws(i)
                if text[i] == ",":
                    i += 1
                    continue
                if text[i] == "]":
                    spans[path] = {"start": start, "end": i + 1, "kind": "array", "keyStart": key_start}
                    return i + 1
                raise JsonEditError(f"',' oder ']' erwartet bei {i}")
        m = _STR.match(text, i)
        if m:
            spans[path] = {"start": i, "end": m.end(), "kind": "string", "keyStart": key_start}
            return m.end()
        m = _NUM.match(text, i)
        if m and m.end() > i:
            spans[path] = {"start": i, "end": m.end(), "kind": "number", "keyStart": key_start}
            return m.end()
        for word, kind in (("true", "bool"), ("false", "bool"), ("null", "null")):
            if text.startswith(word, i):
                spans[path] = {"start": i, "end": i + len(word), "kind": kind, "keyStart": key_start}
                return i + len(word)
        raise JsonEditError(f"unerwartetes Zeichen {c!r} bei {i}")

    end = parse(0, ())
    if text[ws(end):].strip():
        raise JsonEditError("Text nach dem JSON-Wert")
    return spans


def line_of(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def get(data, path):
    for part in path:
        data = data[part]
    return data


def _set(data, path, new):
    target = data
    for part in path[:-1]:
        target = target[part]
    target[path[-1]] = new


def dump_scalar(value) -> str:
    if isinstance(value, bool) or value is None:
        return json.dumps(value)
    if isinstance(value, float):
        if not math.isfinite(value):
            raise JsonEditError("nicht endliche Zahl")
        if value.is_integer() and abs(value) < 1e15:
            return repr(value)  # 2.0 bleibt 2.0 (Typ im JSON erhalten)
        return format(value, ".10g") if "e" not in format(value, ".10g") else repr(value)
    if isinstance(value, (int, str)):
        return json.dumps(value)
    raise JsonEditError(f"kein einfacher Wert: {value!r}")


def replace(text: str, path: tuple, new) -> str:
    """Den Wert an path ersetzen. Der Pfad muss existieren und ein einfacher Wert sein."""
    spans = scan(text)
    if path not in spans:
        raise JsonEditError(f"Pfad {'/'.join(map(str, path))} nicht in der Datei")
    span = spans[path]
    if span["kind"] in ("object", "array"):
        raise JsonEditError("nur einfache Werte können ersetzt werden")
    result = text[:span["start"]] + dump_scalar(new) + text[span["end"]:]
    _verify(text, result, path, new)
    return result


def insert_before(text: str, parent: tuple, key: str, new, before_key: str) -> str:
    """
    Schlüssel key mit Wert new (auch ein kleines Objekt) direkt vor before_key im Objekt parent
    einfügen, in derselben Zeile/Einrueckung wie before_key.
    """
    spans = scan(text)
    anchor = spans.get(parent + (before_key,))
    if anchor is None or anchor["keyStart"] is None:
        raise JsonEditError(f"Anker {before_key} nicht gefunden")
    if parent + (key,) in spans:
        raise JsonEditError(f"{key} gibt es schon")
    k = anchor["keyStart"]
    line_start = text.rfind("\n", 0, k) + 1
    indent = text[line_start:k]
    if indent.strip():
        indent = " "
        sep = ", "
    else:
        sep = ("\r\n" if "\r\n" in text else "\n")
        sep = "," + sep + indent
    rendered = json.dumps(new, separators=(", ", ": ")) if isinstance(new, (dict, list)) else dump_scalar(new)
    if isinstance(new, dict):
        rendered = "{ " + rendered[1:-1] + " }"
    result = text[:k] + json.dumps(key) + ": " + rendered + sep + text[k:]
    _verify(text, result, parent + (key,), new)
    return result


def _verify(old_text: str, new_text: str, path: tuple, new) -> None:
    old = json.loads(old_text)
    try:
        parsed = json.loads(new_text)
    except json.JSONDecodeError as err:  # pragma: no cover - dump_scalar liefert gültiges JSON
        raise JsonEditError(f"Ergebnis ist kein JSON: {err}") from None
    expected = copy.deepcopy(old)
    _set(expected, path, new)
    if parsed != expected:
        raise JsonEditError("Prüfung fehlgeschlagen: Datei würde sich an mehr als einer Stelle ändern")
