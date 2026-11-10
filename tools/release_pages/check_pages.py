#!/usr/bin/env python3
"""Checks store-page drafts: summary <=150 chars, no internal codes, one page per mod in modules.json."""
import json, re, sys, pathlib
FORBIDDEN = re.compile(r"\b[A-Z]{1,4}\d{2,3}[A-Za-z0-9-]*\b|\b[A-Z]-[A-Z]\d+\b|\bN\d{1,3}[a-z]?\b|\b[Bb]-[a-z]+\d+\b|\bStufe\b|\bstage \d\b|\bGate\b|\bQueue\b|wave1|B[uü]ndel|claude-|\bsb-|\b\d\d\.\d\d\.(\d{4})?|\b\d[a-c]-\d\b")
def main():
    R = pathlib.Path(__file__).resolve().parents[2]
    bad = []
    for m in json.load(open(R / "modules/modules.json"))["modules"]:
        p = R / f"docs/release/pages/{m['id']}.md"
        if not p.exists(): bad.append(f"missing {p.name}"); continue
        t = p.read_text()
        s = re.search(r"## Summary[^\n]*\n\n(.+)\n", t)
        if not s or len(s.group(1)) > 150: bad.append(f"{p.name}: summary {len(s.group(1)) if s else 'missing'} chars")
        for h in FORBIDDEN.finditer(t): bad.append(f"{p.name}: forbidden '{h.group(0)}'")
    print("\n".join(bad) or "OK: all pages present, summaries <=150, no internal codes")
    return 1 if bad else 0
if __name__ == "__main__": sys.exit(main())
