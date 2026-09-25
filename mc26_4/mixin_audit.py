"""Mixin audit for the 26.4 snapshot line - run from the repository root BEFORE booting a client:

    python mc26_4/mixin_audit.py [OLD_MC] [NEW_MC]     (default: 26.3 and mc264_minecraft_version)

Checks every mixin method = "...", @At target = "L...;...", @Shadow member and @Accessor/@Invoker
name of the sources the 26.4 line compiles (shared trees + 26.3 overlay with 26.4 twins) against the
Loom-cached Minecraft jars (~/.gradle/caches/fabric-loom/<version>/minecraft-merged.jar, present
after one compile of each line). Prints only references missing on NEW_MC; "OLD=MISSING" as well
usually means an inherited member (false positive). A mixin that fails to apply costs a client boot:
on 26.4-snapshot-1 the broken EquipmentLayerRenderer mixin stopped every chunk from building.
"""
import os, re, subprocess, functools, sys
R = os.getcwd()
_props = dict(l.split('=', 1) for l in open(os.path.join(R, 'gradle.properties'), encoding='utf-8').read().splitlines()
              if '=' in l and not l.startswith('#'))
OLD = sys.argv[1] if len(sys.argv) > 1 else '26.3'
NEW = sys.argv[2] if len(sys.argv) > 2 else _props['mc264_minecraft_version'].strip()
J = {v: os.path.expanduser('~/.gradle/caches/fabric-loom/%s/minecraft-merged.jar' % v) for v in (OLD, NEW)}
trees = ['common/src/shared/java', 'src/main/java']
over263, over264 = 'mc26_3/overlay/java', 'mc26_4/overlay/java'
files = {}
for t in trees:
    for dp, dn, fn in os.walk(os.path.join(R, t)):
        for f in fn:
            if f.endswith('.java'):
                files[os.path.relpath(os.path.join(dp, f), os.path.join(R, t))] = os.path.join(dp, f)
for t in (over263, over264):
    for dp, dn, fn in os.walk(os.path.join(R, t)):
        for f in fn:
            if f.endswith('.java'):
                files[os.path.relpath(os.path.join(dp, f), os.path.join(R, t))] = os.path.join(dp, f)

@functools.lru_cache(None)
def members(ver, cls):
    p = subprocess.run(['javap', '-p', '-s', '-cp', J[ver], cls], capture_output=True, text=True)
    if p.returncode:
        return None
    out = set(); names = set(); last = None
    for line in p.stdout.splitlines():
        line = line.strip()
        if line.startswith('descriptor:') and last:
            out.add(last + line.split(':', 1)[1].strip()); last = None
        else:
            m = re.search(r'([\w$<>]+)\(', line)
            if m and not line.startswith('descriptor'):
                last = m.group(1); names.add(last)
                if ' ' + cls + '(' in ' ' + line or line.split('(')[0].endswith(cls.split('.')[-1]):
                    last = '<init>'; names.add('<init>')
            else:
                fm = re.match(r'.*?\s([\w$]+);$', line)
                last = fm.group(1) + ':' if fm else None
                if fm: names.add(fm.group(1))
    return out, names

def check(ver, cls, ref):
    m = members(ver, cls)
    if m is None:
        return 'NO CLASS'
    full, names = m
    if '(' in ref or ':' in ref:
        return 'ok' if ref.replace(':', ':', 1) in full else 'MISSING'
    return 'ok' if ref in names else 'MISSING'

for rel, path in sorted(files.items()):
    src = open(path, encoding='utf-8', errors='replace').read()
    mm = re.search(r'@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class', src)
    tm = re.search(r'@Mixin\(\s*targets\s*=\s*\{?\s*"([^"]+)"', src)
    if not (mm or tm):
        continue
    if mm:
        simple = mm.group(1)
        imp = re.search(r'import\s+([\w.]+\.%s);' % re.escape(simple.split('.')[0]), src)
        cls = (imp.group(1) + simple[len(simple.split('.')[0]):]) if imp else simple
        parts = cls.split('.')
        # inner classes: Outer.Inner -> Outer$Inner
        while len(parts) > 1 and parts[-2][0].isupper():
            parts[-2:] = [parts[-2] + '$' + parts[-1]]
        cls = '.'.join(parts)
    else:
        cls = tm.group(1).replace('/', '.')
    refs = []
    for grp in re.findall(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', src):
        for s in re.findall(r'"([^"]+)"', grp):
            refs.append((cls, s))
    for s in re.findall(r'target\s*=\s*"L([\w/$]+);([^"]+)"', src):
        refs.append((s[0].replace('/', '.'), s[1]))
    for m in re.finditer(r'@Shadow(?:\s*@\w+(?:\([^)]*\))?)*\s+([^;{=]*?)([\w$]+)\s*(\(|;|=)', src):
        refs.append((cls, m.group(2)))
    for m in re.finditer(r'@(?:Accessor|Invoker)\(\s*(?:value\s*=\s*)?"([^"]+)"', src):
        refs.append((cls, m.group(1)))
    for c, r in refs:
        a, b = check(OLD, c, r), check(NEW, c, r)
        if b != 'ok':
            print(f'{rel}: {c} {r}  {OLD}={a} {NEW}={b}')
