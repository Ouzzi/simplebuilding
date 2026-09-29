"""
Java-Quelltext als Text lesen - ohne Parser-Bibliothek, aber mit exakten Positionen.

Alles hier arbeitet auf einer Kopie des Quelltexts, in der Kommentare durch Leerzeichen ersetzt
sind (Zeilenumbrüche bleiben). So stimmen Offsets und Zeilennummern mit der echten Datei ueberein,
und ein auskommentierter Pool oder eine alte Konstante im Kommentar wird nie als Wert gelesen.
"""

from __future__ import annotations

import bisect
import re


_TOKENS = re.compile(r'"""[\s\S]*?(?:"""|$)|"(?:\\.|[^"\\\n])*"?|\'(?:\\.|[^\'\\\n])*\'?|//[^\n]*|/\*[\s\S]*?(?:\*/|$)')
_NOT_NEWLINE = re.compile(r"[^\n]")


def blank_comments(text: str) -> str:
    """Kommentare durch Leerzeichen ersetzen; Strings, Zeichen und Zeilenumbrüche bleiben."""
    def repl(m):
        s = m.group(0)
        return _NOT_NEWLINE.sub(" ", s) if s.startswith("/") else s
    return _TOKENS.sub(repl, text)


class Lines:
    """Offset -> Zeilennummer (1-basiert) für einen Text."""

    def __init__(self, text: str):
        self.starts = [0] + [m.end() for m in re.finditer("\n", text)]

    def line(self, offset: int) -> int:
        return bisect.bisect_right(self.starts, offset)


def closing(text: str, start: int, open_ch: str = "(", close_ch: str = ")") -> int:
    """Index direkt hinter der Klammer, die die Klammer bei text[start] schliesst."""
    depth = 0
    i = start
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c == open_ch:
            depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError(f"unbalanced {open_ch}{close_ch} from offset {start}")


def split_args(text: str, start: int, end: int) -> list[tuple[str, int, int]]:
    """Argumente zwischen start und end (ohne die Klammern) als (Text, Anfang, Ende), Positionen absolut."""
    parts = []
    depth = 0
    seg_start = start
    i = start
    while i < end:
        c = text[i]
        if c == '"':
            j = i + 1
            while j < end and text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            parts.append(seg_start)
            seg_start = i + 1
        i += 1
    parts.append(seg_start)
    out = []
    bounds = parts + [end + 1]
    for a, b in zip(bounds, bounds[1:]):
        raw = text[a:b - 1]
        lead = len(raw) - len(raw.lstrip())
        trail = len(raw.rstrip())
        out.append((raw.strip(), a + lead, a + trail))
    if len(out) == 1 and not out[0][0]:
        return []
    return out


def doc_before(raw: str, offset: int) -> str:
    """Javadoc oder //-Kommentarzeilen direkt über der Zeile, in der offset liegt (bereinigt)."""
    line_start = raw.rfind("\n", 0, offset) + 1
    before = raw[:line_start].rstrip()
    if before.endswith("*/"):
        begin = before.rfind("/*")
        if begin >= 0:
            body = before[begin + 2:-2]
            lines = [re.sub(r"^\s*\*\s?", "", l).strip() for l in body.splitlines()]
            text = " ".join(l for l in lines if l and not l.startswith("@"))
            return _clean_doc(text.lstrip("*").strip())
    comment_lines = []
    for line in reversed(before.splitlines()):
        stripped = line.strip()
        if stripped.startswith("//"):
            comment_lines.append(stripped[2:].strip())
        elif stripped.startswith("@"):
            continue
        else:
            break
    return _clean_doc(" ".join(reversed(comment_lines)))


def trailing_comment(raw: str, offset: int) -> str:
    """//-Kommentar am Ende der Zeile, in der offset liegt."""
    end = raw.find("\n", offset)
    line = raw[offset:end if end >= 0 else len(raw)]
    m = re.search(r"//(.*)$", line)
    return m.group(1).strip() if m else ""


def _clean_doc(text: str) -> str:
    text = re.sub(r"\{@(?:code|link|linkplain)\s+([^}]*)\}", r"\1", text)
    text = re.sub(r"</?\w+[^>]*>", "", text)
    return re.sub(r"\s+", " ", text).strip()


# ---------------------------------------------------------------------------
# Konstanten-Ausdruecke: 190 * BASE_DURABILITY_MULTIPLIER, -3.0f - OFFSET, 64 * 64, 131_072
# ---------------------------------------------------------------------------

NUMBER = re.compile(r"(?:0[xX][0-9a-fA-F_]+|(?:\d[\d_]*\.?[\d_]*|\.\d[\d_]*)(?:[eE][+-]?\d+)?)[fFdDlL]?")


class ExprError(ValueError):
    pass


def parse_number(token: str) -> tuple[float | int, bool]:
    """Java-Zahlenliteral -> (Wert, ist_ganzzahlig)."""
    t = token.replace("_", "")
    suffix = t[-1] if t and t[-1] in "fFdDlL" else ""
    if suffix and not t.lower().startswith("0x"):
        t = t[:-1]
    elif suffix in ("l", "L"):
        t = t[:-1]
    if t.lower().startswith("0x"):
        return int(t, 16), True
    if suffix in ("f", "F", "d", "D") or "." in t or "e" in t.lower():
        return float(t), False
    return int(t), True


def evaluate(expr: str, env: dict[str, tuple[float | int, bool]]) -> tuple[float | int, bool]:
    """
    Wertet einen Konstanten-Ausdruck aus: Zahlen, Namen aus env (auch Klasse.NAME), + - * / %,
    Klammern und Casts (int)/(float)/(double)/(long). Ganzzahl-Division wie in Java (Richtung 0).
    Alles andere (Methodenaufrufe, Felder anderer Klassen, die env nicht kennt) -> ExprError.
    """
    tokens = _tokenize(expr)
    pos = 0

    def peek():
        return tokens[pos] if pos < len(tokens) else None

    def take():
        nonlocal pos
        tok = tokens[pos]
        pos += 1
        return tok

    def atom():
        tok = peek()
        if tok is None:
            raise ExprError("unexpected end")
        if tok == "(":
            # Cast?
            if pos + 2 < len(tokens) and tokens[pos + 1] in ("int", "float", "double", "long") and tokens[pos + 2] == ")":
                take(); kind = take(); take()
                value, is_int = unary()
                if kind in ("int", "long"):
                    return int(value), True
                return float(value), False
            take()
            value = additive()
            if take() != ")":
                raise ExprError("missing )")
            return value
        take()
        if NUMBER.fullmatch(tok):
            return parse_number(tok)
        if re.fullmatch(r"[A-Za-z_][\w.]*", tok):
            if tok in env:
                return env[tok]
            last = tok.split(".")[-1]
            if last in env and "." in tok:
                return env[last]
            raise ExprError(f"unknown name {tok}")
        raise ExprError(f"unexpected token {tok}")

    def unary():
        tok = peek()
        if tok == "-":
            take()
            value, is_int = unary()
            return -value, is_int
        if tok == "+":
            take()
            return unary()
        return atom()

    def multiplicative():
        left = unary()
        while peek() in ("*", "/", "%"):
            op = take()
            right = unary()
            left = _apply(op, left, right)
        return left

    def additive():
        left = multiplicative()
        while peek() in ("+", "-"):
            op = take()
            right = multiplicative()
            left = _apply(op, left, right)
        return left

    value = additive()
    if pos != len(tokens):
        raise ExprError(f"trailing tokens: {' '.join(tokens[pos:])}")
    return value


def _apply(op, left, right):
    (a, ai), (b, bi) = left, right
    both_int = ai and bi
    if op == "+":
        return a + b, both_int
    if op == "-":
        return a - b, both_int
    if op == "*":
        return a * b, both_int
    if b == 0:
        raise ExprError("division by zero")
    if op == "/":
        if both_int:
            q = abs(a) // abs(b)
            return (q if (a >= 0) == (b >= 0) else -q), True
        return a / b, False
    if both_int:
        return int(a - b * int(a / b)), True
    import math
    return math.fmod(a, b), False


def _tokenize(expr: str) -> list[str]:
    tokens = []
    i = 0
    while i < len(expr):
        c = expr[i]
        if c.isspace():
            i += 1
            continue
        m = NUMBER.match(expr, i)
        if m and (c.isdigit() or (c == "." and i + 1 < len(expr) and expr[i + 1].isdigit())):
            tokens.append(m.group(0))
            i = m.end()
            continue
        m = re.compile(r"[A-Za-z_][\w]*(?:\.[A-Za-z_]\w*)*").match(expr, i)
        if m:
            tokens.append(m.group(0))
            i = m.end()
            continue
        if c in "+-*/%()":
            tokens.append(c)
            i += 1
            continue
        raise ExprError(f"unsupported character {c!r}")
    return tokens


def format_number(value: float | int, is_int: bool) -> float | int:
    """Floats aus Java-float-Literalen sauber runden (0.015f bleibt 0.015)."""
    if is_int:
        return int(value)
    return float(f"{float(value):.10g}")
