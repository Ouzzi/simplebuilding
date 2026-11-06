"""Entwurf A unverändert lassen, nur die Icon-Leiste „Alle N Module“ leicht nach Mod-Familien gruppieren."""
import re, sys
sys.path.insert(0, sys.argv[3] + '/tools/modtree'); import modtree as t
src, out = sys.argv[1], sys.argv[2]
s = open(src, encoding='utf-8').read()
i = s.find('<div class="ic">'); j = s.find('</div>', i) + len('</div>')
imgs = {re.search(r'title="([^"]+)"', m).group(1): m for m in re.findall(r'<img [^>]*>', s[i:j])}
mods = t.load(t.ROOT if len(sys.argv) < 4 else __import__('pathlib').Path(sys.argv[3]))
def fam(pid):
    ids = [pid]
    for c in t.children(mods, pid): ids += fam(c['id'])
    return ids
sup, rest = t.roots(mods)
groups, used = [], set()
for r in sup:
    ids = [x for x in fam(r['id']) if x in imgs]; used |= set(ids)
    if ids: groups.append((r['name'], ids))
left = [x for x in imgs if x not in used]
if left: groups.append(('Weitere Mods', left))
html = '<div class="icg">' + ''.join(
    f'<div class="icf"><span>{n}</span><div class="ic">{"".join(imgs[x] for x in ids)}</div></div>' for n, ids in groups) + '</div>'
css = '.icg{display:flex;flex-wrap:wrap;gap:14px 22px}.icf span{display:block;font-size:12px;color:#cbbf9f;margin:0 0 4px}'
s = s[:i] + html + s[j:]
s = s.replace('</style>', css + '</style>', 1)
open(out, 'w', encoding='utf-8').write(s)
print(len(groups), 'Gruppen,', sum(len(g[1]) for g in groups), 'Icons von', len(imgs))
