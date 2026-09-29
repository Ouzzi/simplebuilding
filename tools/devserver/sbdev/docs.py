"""
Die Entwickler-Dokumente aus docs/ als HTML - live aus den Markdown-Dateien gerendert, damit es
nur eine Quelle gibt (die Datei im Repo). Wiki-Themen bleiben im Wiki und stehen hier nicht.

Kleiner Markdown-Leser für das, was docs/ benutzt: Ueberschriften, Absaetze, Listen (auch
verschachtelt per Einrueckung), Tabellen, Codebloecke, Zitate, `code`, **fett**, *kursiv*, Links.
Alles wird vorher HTML-escaped.
"""

from __future__ import annotations

import html
import re
from pathlib import Path

# Datei -> (Gruppe, Kurzname). Was hier fehlt, erscheint unter "Weitere" - ausser WIKI_ONLY.
DOCS = {
    "BALANCING-ZENTRALE.md": ("Balance", "Balancing-Zentrale: Phase 2"),
    "KERNE-SELTENHEIT.md": ("Balance", "Baukerne: Seltenheit"),
    "LOOT-BALANCE.md": ("Balance", "Loot-Balance"),
    "RARITAETEN.md": ("Balance", "Seltenheiten & Namen"),
    "SLEDGEHAMMER-BALANCE.md": ("Balance", "Vorschlaghammer-Balance"),
    "TRIM-BALANCE.md": ("Balance", "Besatz-Balance"),
    "CONFIG.md": ("Entwicklung", "Config-Referenz"),
    "TESTZENTRALE.md": ("Entwicklung", "Testzentrale"),
    "PERFORMANCE.md": ("Entwicklung", "Performance"),
    "AUDIT-2026-09-26.md": ("Entwicklung", "Audit 2026-09-26"),
    "UPGRADE-26.2-26.3.md": ("Entwicklung", "Welt-Upgrade 26.2 -> 26.3"),
    "PUBLISHING.md": ("Entwicklung", "Veröffentlichen"),
}
WIKI_ONLY = {"WIKI-HOSTING.md"}


def listing(repo: Path) -> list[dict]:
    out = []
    root = repo / "docs"
    if not root.exists():
        return out
    for path in sorted(root.glob("*.md")):
        if path.name in WIKI_ONLY:
            continue
        group, title = DOCS.get(path.name, ("Weitere", _title(path)))
        out.append({"name": path.name, "group": group, "title": title})
    order = {"Balance": 0, "Entwicklung": 1, "Weitere": 2}
    out.sort(key=lambda d: (order.get(d["group"], 3), d["title"]))
    return out


def _title(path: Path) -> str:
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return path.stem


def render_file(repo: Path, name: str) -> dict:
    if not re.fullmatch(r"[A-Za-z0-9._-]+\.md", name) or name in WIKI_ONLY:
        raise FileNotFoundError(name)
    path = repo / "docs" / name
    if not path.exists():
        raise FileNotFoundError(name)
    text = path.read_text(encoding="utf-8")
    return {"name": name, "title": _title(path), "html": render(text), "file": f"docs/{name}"}


def inline(text: str) -> str:
    parts = re.split(r"(`[^`]+`)", text)
    out = []
    for part in parts:
        if part.startswith("`") and part.endswith("`") and len(part) > 1:
            out.append("<code>" + html.escape(part[1:-1]) + "</code>")
            continue
        s = html.escape(part, quote=False)
        s = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", lambda m: _link(m.group(1), m.group(2)), s)
        s = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", s)
        s = re.sub(r"(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])", r"<em>\1</em>", s)
        out.append(s)
    return "".join(out)


def _link(label: str, target: str) -> str:
    if re.match(r"^(https?:|#)", target):
        return f'<a href="{html.escape(target)}" target="_blank" rel="noopener">{label}</a>'
    if target.endswith(".md") and "/" not in target:
        return f'<a href="#/docs/{html.escape(target)}">{label}</a>'
    return f"<code>{label}</code>"


def render(text: str) -> str:
    lines = text.replace("\r\n", "\n").split("\n")
    out: list[str] = []
    i = 0
    slugs: dict[str, int] = {}

    def slug(title: str) -> str:
        base = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-") or "abschnitt"
        n = slugs.get(base, 0)
        slugs[base] = n + 1
        return base if n == 0 else f"{base}-{n}"

    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("```"):
            j = i + 1
            code = []
            while j < len(lines) and not lines[j].startswith("```"):
                code.append(lines[j])
                j += 1
            out.append("<pre><code>" + html.escape("\n".join(code)) + "</code></pre>")
            i = j + 1
            continue
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if m:
            level = len(m.group(1))
            title = m.group(2).strip()
            out.append(f'<h{level} id="{slug(title)}">{inline(title)}</h{level}>')
            i += 1
            continue
        if line.lstrip().startswith("|") and i + 1 < len(lines) and re.match(r"^\s*\|?\s*:?-{2,}", lines[i + 1]):
            header = _cells(line)
            i += 2
            rows = []
            while i < len(lines) and lines[i].lstrip().startswith("|"):
                rows.append(_cells(lines[i]))
                i += 1
            head = "".join(f"<th>{inline(c)}</th>" for c in header)
            body = "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in row) + "</tr>" for row in rows)
            out.append(f'<div class="table-wrap"><table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table></div>')
            continue
        if line.startswith(">"):
            quote = []
            while i < len(lines) and lines[i].startswith(">"):
                quote.append(lines[i][1:].strip())
                i += 1
            out.append("<blockquote>" + inline(" ".join(quote)) + "</blockquote>")
            continue
        if re.match(r"^\s*([-*]|\d+\.)\s+", line):
            block = []
            while i < len(lines) and (re.match(r"^\s*([-*]|\d+\.)\s+", lines[i]) or (lines[i].startswith("  ") and lines[i].strip())):
                block.append(lines[i])
                i += 1
            out.append(_list(block))
            continue
        para = [line.strip()]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r"^(#{1,6}\s|```|\s*[-*]\s|\s*\d+\.\s|\s*\||>)", lines[i]):
            para.append(lines[i].strip())
            i += 1
        out.append("<p>" + inline(" ".join(para)) + "</p>")
    return "\n".join(out)


def _cells(line: str) -> list[str]:
    s = line.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|"):
        s = s[:-1]
    return [c.strip() for c in re.split(r"(?<!\\)\|", s)]


def _list(block: list[str]) -> str:
    items: list[tuple[int, str, bool]] = []
    for raw in block:
        m = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", raw)
        if m:
            items.append((len(m.group(1)), m.group(3), m.group(2)[0].isdigit()))
        elif items:
            indent, text, ordered = items[-1]
            items[-1] = (indent, text + " " + raw.strip(), ordered)
    html_out = []
    stack: list[tuple[int, str]] = []
    for indent, text, ordered in items:
        tag = "ol" if ordered else "ul"
        while stack and indent < stack[-1][0]:
            html_out.append(f"</li></{stack.pop()[1]}>")
        if not stack or indent > stack[-1][0]:
            html_out.append(f"<{tag}>")
            stack.append((indent, tag))
        else:
            html_out.append("</li>")
        html_out.append("<li>" + inline(text))
    while stack:
        html_out.append(f"</li></{stack.pop()[1]}>")
    return "".join(html_out)
