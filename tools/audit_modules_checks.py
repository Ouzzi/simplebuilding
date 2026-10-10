#!/usr/bin/env python3.12
"""Audit evidence for docs/ai/AUDIT-MODULE-2026-10-11.md. Exit 1 while findings exist.
Checks: (M-02) umlauts replaced by '?' in DE texts, (M-04) raw GLFW key codes in module code,
(M-01) shared bundled libs without a module version, (M-05) modules without a Forge test target."""
import glob, json, os, re, sys
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(root)
bad = 0
def report(tag, msg):
    global bad; bad += 1; print(f"{tag}: {msg}")
# M-02
for f in glob.glob('modules/*/**/*.json', recursive=True):
    if '/build/' in f or not ('de_de' in f or f.endswith('manual.json')): continue
    s = open(f, encoding='utf-8').read()
    n = len(re.findall(r'(?<![\w/=])[A-Za-z]*[a-zäöüß]\?[a-zäöüß][A-Za-z]*', s))
    if n: report('M-02', f"{f}: {n} words with '?' inside letters (lost umlaut)")
# M-04
for f in glob.glob('modules/*/**/*.java', recursive=True):
    if '/build/' in f or '/clienttest/' in f: continue
    for i, l in enumerate(open(f, encoding='utf-8', errors='ignore'), 1):
        if re.search(r'(key|Key)\w*\s*==\s*3\d\d\b|GLFW_KEY', l): report('M-04', f"{f}:{i}: {l.strip()[:90]}")
# M-01
for m in sorted(os.listdir('modules')):
    if not m.startswith('simple') or not os.path.isdir(f'modules/{m}/fabric'): continue
    txt = ''.join(open(p).read() for p in glob.glob(f'modules/{m}/*/build.gradle'))
    if 'moduleVersion' not in txt and m in ('simplelib',):
        report('M-01', f"{m}: bundled library has no moduleVersion (stays 0.1.0 while its API grows)")
# M-05
for e in json.load(open('modules/modules.json'))['modules']:
    t = e.get('tests')
    if t and 'forge' in e.get('loaders', []) and 'forge' not in t['loaders']:
        report('M-05', f"{e['id']}: declares loader forge but has no Forge test target")
sys.exit(1 if bad else 0)
