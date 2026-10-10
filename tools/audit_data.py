#!/usr/bin/env python3.12
"""Data audit (read-only): builds the effective 26.3 data/asset tree per mod and checks
lang EN/DE parity, recipe reachability, tags, loot refs. Usage: audit_data.py [lang|recipes|names|loot|tags|adv|all] | cycles <dir with extracted vanilla data/>"""
import json, os, re, sys, glob, collections
R = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

def layers():
    """(namespace-agnostic) list of resource roots in precedence order (later wins), SimpleBuilding + modules."""
    roots = [os.path.join(R, 'src/main/generated'), os.path.join(R, 'src/main/resources'),
             os.path.join(R, 'mc26_3/generated'), os.path.join(R, 'mc26_3/overlay/resources')]
    for m in sorted(os.listdir(os.path.join(R, 'modules'))):
        for sub in ('generated/resources', 'shared/resources', 'generated'):
            p = os.path.join(R, 'modules', m, sub)
            if os.path.isdir(p) and os.path.isdir(os.path.join(p, 'data')) or os.path.isdir(os.path.join(p, 'assets')):
                roots.append(p)
    return roots

def tree():
    files = {}
    removed = set()
    rf = os.path.join(R, 'mc26_3/generated/removed-on-26.3.txt')
    if os.path.isfile(rf):
        removed = {l.strip() for l in open(rf) if l.strip() and not l.startswith('#')}
    seen = set()
    for root in layers():
        if root in seen: continue
        seen.add(root)
        for sub in ('data', 'assets'):
            for dp, _, fn in os.walk(os.path.join(root, sub)):
                for f in fn:
                    full = os.path.join(dp, f)
                    rel = os.path.relpath(full, root).replace(os.sep, '/')
                    files[rel] = full
    for r in removed:
        files.pop(r, None)
    return files

def jl(p):
    with open(p, encoding='utf-8') as f:
        return json.load(f)

def dupkeys(p):
    c = collections.Counter(); 
    def hook(pairs):
        for k, _ in pairs: c[k] += 1
        return dict(pairs)
    json.load(open(p, encoding='utf-8'), object_pairs_hook=hook)
    return [k for k, n in c.items() if n > 1 and False]

def lang_audit(files):
    out = []
    nss = collections.defaultdict(dict)
    for rel, full in files.items():
        m = re.match(r'assets/([^/]+)/lang/(en_us|de_de)\.json$', rel)
        if m: nss[m.group(1)][m.group(2)] = full
    for ns, d in sorted(nss.items()):
        en = jl(d['en_us']) if 'en_us' in d else {}
        de = jl(d['de_de']) if 'de_de' in d else {}
        for k in sorted(set(en) - set(de)): out.append(('LANG-MISSING-DE', ns, k, en[k]))
        for k in sorted(set(de) - set(en)): out.append(('LANG-MISSING-EN', ns, k, de[k]))
        for lg, dd in (('en', en), ('de', de)):
            for k, v in dd.items():
                if not isinstance(v, str): continue
                if v.strip() == '' : out.append(('LANG-EMPTY-' + lg, ns, k, v))
                if re.search(r'\b(TODO|FIXME|XXX|lorem|PLACEHOLDER)\b', v, re.I): out.append(('LANG-PLACEHOLDER-' + lg, ns, k, v))
                if v == k: out.append(('LANG-KEYASVALUE-' + lg, ns, k, v))
        for lg, dd in (('en', en), ('de', de)):
            for k, v in dd.items():
                if isinstance(v, str) and (re.search(r'[A-Za-z\u00e4\u00f6\u00fc]\?[a-z\u00e4\u00f6\u00fc]', v) or re.search('\u00c3|\u00e2\u20ac|\ufffd', v)) and not re.search(r'https?://', v):
                    out.append(('LANG-MOJIBAKE-' + lg, ns, k, v[:80]))
        for k in set(en) & set(de):
            if not (isinstance(en[k], str) and isinstance(de[k], str)): continue
            pe = sorted(re.findall(r'%(?:\d+\$)?[sdf]', en[k])); pd = sorted(re.findall(r'%(?:\d+\$)?[sdf]', de[k]))
            if len(pe) != len(pd): out.append(('LANG-FORMAT-MISMATCH', ns, k, f'{en[k]!r} | {de[k]!r}'))
            if en[k] == de[k] and len(en[k]) > 25 and re.search(r'[a-z]{4}', en[k]): out.append(('LANG-DE-UNTRANSLATED', ns, k, en[k]))
            if re.search(r'\b(colour|armour|grey|favourite|neighbour|behaviour|traveller|catalogue)\w*', en[k], re.I): out.append(('LANG-BRITISH-EN', ns, k, en[k][:80]))
    return out


# ---------------------------------------------------------------- recipes
def ing_alts(x):
    """ingredient json -> list of alternatives ('item:id' or '#tag')."""
    if x is None: return []
    if isinstance(x, str): return [x]
    if isinstance(x, list):
        out = []
        for y in x: out += ing_alts(y)
        return out
    if isinstance(x, dict):
        if 'item' in x: return [x['item']]
        if 'tag' in x: return ['#' + x['tag']]
    return []

def recipes(files):
    out = []
    for rel, full in files.items():
        m = re.match(r'data/([^/]+)/recipe/(.+)\.json$', rel)
        if not m: continue
        d = jl(full)
        t = d.get('type', '')
        rid = f'{m.group(1)}:{m.group(2)}'
        res = d.get('result')
        if isinstance(res, dict): res = res.get('id')
        if t == 'minecraft:brewing': res = (d.get('output') or {}).get('id')
        ins = []  # list of slots, each slot = alternatives
        if t == 'minecraft:crafting_shaped' or t == 'simplebuilding:backpack_upgrade' or t == 'simplebuilding:reinforced_bundle':
            key = d.get('key', {})
            for row in d.get('pattern', []):
                for ch in row:
                    if ch != ' ': ins.append(ing_alts(key.get(ch)))
        elif t in ('minecraft:crafting_shapeless', 'simplebuilding:enchanted_shapeless'):
            ins = [ing_alts(i) for i in d.get('ingredients', [])]
        elif t in ('minecraft:stonecutting', 'minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking'):
            ins = [ing_alts(d.get('ingredient'))]
        elif t in ('minecraft:smithing_transform', 'simplebuilding:count_based_smithing', 'simplebuilding:easter_smithing'):
            ins = [ing_alts(d.get(k)) for k in ('template', 'base', 'addition')]
        elif t == 'minecraft:crafting_transmute':
            ins = [ing_alts(d.get('input')), ing_alts(d.get('material'))]
        elif t == 'minecraft:brewing':
            ins = [ing_alts(d.get('input')), ing_alts(d.get('reagent'))]
        out.append(dict(id=rid, type=t, result=res, ins=ins, d=d, rel=rel))
    return out

def tags(files):
    tg = {}
    for rel, full in files.items():
        m = re.match(r'data/([^/]+)/tags/item/(.+)\.json$', rel)
        if not m: continue
        d = jl(full)
        tg.setdefault(f'{m.group(1)}:{m.group(2)}', []).append(d)
    return tg

def tag_members(tg, name, seen=None):
    seen = seen or set()
    if name in seen: return set()
    seen.add(name)
    out = set()
    for d in tg.get(name, []):
        for v in d.get('values', []):
            v = v['id'] if isinstance(v, dict) else v
            if v.startswith('#'): out |= tag_members(tg, v[1:], seen)
            else: out.add(v)
    return out

def mod_items(files, nss):
    its = set()
    for rel in files:
        m = re.match(r'assets/([^/]+)/items/(.+)\.json$', rel)
        if m and m.group(1) in nss: its.add(f'{m.group(1)}:{m.group(2)}')
    return its

def loot_items(files):
    out = collections.defaultdict(set)
    def walk(o, acc):
        if isinstance(o, dict):
            if o.get('type') in ('minecraft:item', 'item') and isinstance(o.get('name'), str): acc.add(o['name'])
            for v in o.values(): walk(v, acc)
        elif isinstance(o, list):
            for v in o: walk(v, acc)
    for rel, full in files.items():
        m = re.match(r'data/([^/]+)/loot_table/(.+)\.json$', rel)
        if m:
            acc = set(); walk(jl(full), acc)
            for a in acc: out[a].add(f'{m.group(1)}:{m.group(2)}')
    return out

def recipe_audit(files):
    out = []
    rs = recipes(files)
    tg = tags(files)
    nss = {re.match(r'assets/([^/]+)/', r).group(1) for r in files if r.startswith('assets/')}
    nss = {n for n in nss if n != 'minecraft'}
    items = mod_items(files, nss)
    loot = loot_items(files)
    # other sources (non-recipe)
    other = collections.defaultdict(set)
    for it, tabs in loot.items():
        for t in tabs:
            if '/blocks/' in t or t.split(':')[1].startswith('blocks/'): continue
            other[it].add('loot:' + t)
    for rel, full in files.items():
        m = re.match(r'data/([^/]+)/(villager_trade|chisel_transformations|sledgehammer_upgrades|trial_spawner|enchantment|advancement)/', rel)
        if not m: continue
        txt = open(full, encoding='utf-8').read()
        for it in set(re.findall(r'"((?:%s):[a-z0-9_/]+)"' % '|'.join(nss), txt)):
            other[it].add(m.group(2))
    # wiki obtain
    for fn in glob.glob(os.path.join(R, 'wiki/data/*.json')):
        try: w = jl(fn)
        except Exception: continue
        for s in (w.get('obtain') or {}).get('sources', []): other[s['item']].add('wiki-obtain')
    # reachable fixpoint
    def alt_ok(a, reach):
        if a.startswith('#'):
            mem = tag_members(tg, a[1:])
            if a[1:].startswith('minecraft:') and not mem: return True
            return any(alt_ok(x[1:] if x.startswith('#') else x, reach) if not x.startswith('#') else alt_ok(x, reach) for x in mem) or not mem and a[1:].split(':')[0] == 'minecraft'
        ns = a.split(':')[0]
        return ns not in nss or a in reach
    reach = set(other)
    changed = True
    while changed:
        changed = False
        for r in rs:
            if r['result'] and r['result'] not in reach and all(any(alt_ok(a, reach) for a in slot) if slot else True for slot in r['ins']):
                if r['type'].startswith('simplebuilding:fletching'): continue
                reach.add(r['result']); changed = True
    for it in sorted(items - reach):
        out.append(('ITEM-UNREACHABLE', it, 'no recipe with obtainable inputs, no loot/trade/wiki-obtain source', ''))
    # unknown tags used by recipes
    used = collections.defaultdict(list)
    for r in rs:
        for slot in r['ins']:
            for a in slot:
                if a.startswith('#'): used[a[1:]].append(r['id'])
    for t, rl in sorted(used.items()):
        if t.split(':')[0] in nss and not tag_members(tg, t): out.append(('RECIPE-TAG-EMPTY', t, 'tag undefined or empty', rl[0]))
    # missing ingredient / result items of this mod
    for r in rs:
        if r['result'] and r['result'].split(':')[0] in nss and r['result'] not in items and not r['result'].startswith('minecraft'):
            out.append(('RECIPE-RESULT-UNKNOWN', r['id'], r['result'], ''))
        for slot in r['ins']:
            for a in slot:
                if not a.startswith('#') and a.split(':')[0] in nss and a not in items:
                    out.append(('RECIPE-INGREDIENT-UNKNOWN', r['id'], a, ''))
    # conflicts: identical input signature -> different results
    sig = collections.defaultdict(list)
    for r in rs:
        if r['type'] == 'minecraft:crafting_shapeless' or r['type'] == 'simplebuilding:enchanted_shapeless':
            s = ('shapeless', tuple(sorted('|'.join(sorted(x)) for x in r['ins'])))
        elif r['type'] in ('minecraft:crafting_shaped',):
            rows = r['d']['pattern']; key = r['d']['key']
            s = ('shaped', tuple(tuple('|'.join(sorted(ing_alts(key.get(c)))) if c != ' ' else '' for c in row) for row in rows))
        elif r['type'] in ('minecraft:stonecutting', 'minecraft:smelting', 'minecraft:blasting'):
            s = (r['type'], tuple('|'.join(sorted(x)) for x in r['ins']))
        elif r['type'].endswith('smithing') or 'smithing' in r['type']:
            s = ('smithing', tuple('|'.join(sorted(x)) for x in r['ins']))
        else: continue
        sig[s].append(r)
    for s, rl in sig.items():
        if len(rl) < 2: continue
        res = {(x['result'], json.dumps(x['d'].get('result', {}).get('count', 1) if isinstance(x['d'].get('result'), dict) else 1)) for x in rl}
        if len({x['result'] for x in rl}) > 1 and s[0] != 'stonecutting' and s[0] != 'minecraft:stonecutting':
            out.append(('RECIPE-CONFLICT', ' + '.join(x['id'] for x in rl), 'same inputs, different results: ' + ', '.join(sorted({str(x['result']) for x in rl})), ''))
        elif len({x['result'] for x in rl}) == 1:
            out.append(('RECIPE-DUPLICATE', ' + '.join(x['id'] for x in rl), 'same inputs and result: ' + str(rl[0]['result']), ''))
    return out


# ---------------------------------------------------------------- names, loot, tags, advancements
def ns_lang(files):
    L = collections.defaultdict(lambda: {'en_us': {}, 'de_de': {}})
    for rel, full in files.items():
        m = re.match(r'assets/([^/]+)/lang/(en_us|de_de)\.json$', rel)
        if m: L[m.group(1)][m.group(2)] = jl(full)
    return L

def names_audit(files):
    out = []
    L = ns_lang(files)
    nss = set(L) - {'minecraft'}
    for ns in sorted(nss):
        en, de = L[ns]['en_us'], L[ns]['de_de']
        keys = set(en) | set(de)
        items = {rel for rel in files if re.match(rf'assets/{ns}/items/.+\.json$', rel)}
        for rel in sorted(items):
            n = re.match(rf'assets/{ns}/items/(.+)\.json$', rel).group(1).replace('/', '.')
            if f'item.{ns}.{n}' not in keys and f'block.{ns}.{n}' not in keys:
                out.append(('LANG-ITEM-NAME-MISSING', ns, n, 'no item.%s.%s / block.%s.%s in en/de' % (ns, n, ns, n)))
            else:
                for lg, d in (('en', en), ('de', de)):
                    if f'item.{ns}.{n}' not in d and f'block.{ns}.{n}' not in d:
                        out.append(('LANG-ITEM-NAME-MISSING-' + lg.upper(), ns, n, ''))
        # blocks (blockstates) without a name
        for rel in files:
            m = re.match(rf'assets/{ns}/blockstates/(.+)\.json$', rel)
            if m:
                n = m.group(1).replace('/', '.')
                if f'block.{ns}.{n}' not in keys:
                    out.append(('LANG-BLOCK-NAME-MISSING', ns, n, ''))
    # translate keys used in data json
    allk = {}
    for ns in L:
        for lg in ('en_us', 'de_de'):
            for k in L[ns][lg]: allk.setdefault(k, set()).add(lg)
    for rel, full in files.items():
        if not rel.startswith('data/') or not rel.endswith('.json'): continue
        try: txt = open(full, encoding='utf-8').read()
        except Exception: continue
        for k in set(re.findall(r'"translate"\s*:\s*"([^"]+)"', txt)):
            if k not in allk and not k.startswith(('block.minecraft', 'item.minecraft', 'enchantment.minecraft', 'effect.minecraft')):
                out.append(('LANG-DATA-KEY-MISSING', rel, k, ''))
            elif k in allk and len(allk[k]) < 2: out.append(('LANG-DATA-KEY-ONE-LANG', rel, k, str(allk[k])))
    return out

def loot_audit(files):
    out = []
    nss = {re.match(r'assets/([^/]+)/', r).group(1) for r in files if r.startswith('assets/')} - {'minecraft'}
    for rel in sorted(files):
        m = re.match(r'assets/([^/]+)/blockstates/(.+)\.json$', rel)
        if m and m.group(1) in nss:
            ns, n = m.groups()
            if f'data/{ns}/loot_table/blocks/{n}.json' not in files:
                out.append(('LOOT-BLOCK-NO-TABLE', f'{ns}:{n}', 'blockstate without loot_table/blocks (drops nothing unless Java drops / noLootTable)', ''))
    items = set()
    for rel in files:
        m = re.match(r'assets/([^/]+)/items/(.+)\.json$', rel)
        if m: items.add(f'{m.group(1)}:{m.group(2)}')
    for rel, full in sorted(files.items()):
        m = re.match(r'data/([^/]+)/loot_table/(.+)\.json$', rel)
        if not m: continue
        d = jl(full)
        names = set()
        def walk(o):
            if isinstance(o, dict):
                if o.get('type') in ('minecraft:item', 'item') and isinstance(o.get('name'), str): names.add(o['name'])
                if o.get('type') in ('minecraft:loot_table',) and isinstance(o.get('value'), str):
                    t = o['value']; a, b = (t.split(':') + [''])[:2] if ':' in t else ('minecraft', t)
                    if a in nss and f'data/{a}/loot_table/{b}.json' not in files: out.append(('LOOT-REF-MISSING', rel, t, ''))
                for v in o.values(): walk(v)
            elif isinstance(o, list):
                for v in o: walk(v)
        walk(d)
        for n in names:
            if n.split(':')[0] in nss and n not in items: out.append(('LOOT-ITEM-UNKNOWN', rel, n, ''))
    return out

def tags_audit(files):
    out = []
    nss = {re.match(r'assets/([^/]+)/', r).group(1) for r in files if r.startswith('assets/')} - {'minecraft'}
    items = set(); blocks = set()
    for rel in files:
        m = re.match(r'assets/([^/]+)/items/(.+)\.json$', rel)
        if m: items.add(f'{m.group(1)}:{m.group(2)}')
        m = re.match(r'assets/([^/]+)/blockstates/(.+)\.json$', rel)
        if m: blocks.add(f'{m.group(1)}:{m.group(2)}')
    alltags = collections.defaultdict(set)
    for rel in files:
        m = re.match(r'data/([^/]+)/tags/([a-z_/]+?)/(.+)\.json$', rel)
        if m: alltags[(m.group(2), f'{m.group(1)}:{m.group(3)}')].add(rel)
    for rel, full in sorted(files.items()):
        m = re.match(r'data/([^/]+)/tags/([a-z_/]+?)/(.+)\.json$', rel)
        if not m: continue
        kind = m.group(2)
        d = jl(full)
        seen = collections.Counter()
        for v in d.get('values', []):
            req = True
            if isinstance(v, dict): req = v.get('required', True); v = v['id']
            seen[v] += 1
            if v.startswith('#'):
                if (kind, v[1:]) not in alltags and v[1:].split(':')[0] in nss and req: out.append(('TAG-REF-TAG-MISSING', rel, v, ''))
                continue
            ns = v.split(':')[0]
            if ns in nss and req:
                if kind == 'item' and v not in items: out.append(('TAG-ITEM-UNKNOWN', rel, v, ''))
                if kind == 'block' and v not in blocks: out.append(('TAG-BLOCK-UNKNOWN', rel, v, ''))
        for v, n in seen.items():
            if n > 1: out.append(('TAG-DUPLICATE-ENTRY', rel, v, f'{n}x'))
    return out

def advancement_audit(files):
    out = []
    advs = {}
    for rel, full in files.items():
        m = re.match(r'data/([^/]+)/advancement/(.+)\.json$', rel)
        if m: advs[f'{m.group(1)}:{m.group(2)}'] = (rel, jl(full))
    nss = {a.split(':')[0] for a in advs}
    L = ns_lang(files)
    for aid, (rel, d) in sorted(advs.items()):
        p = d.get('parent')
        if p and p.split(':')[0] in nss and p not in advs: out.append(('ADV-PARENT-MISSING', rel, p, ''))
        if not d.get('criteria'): out.append(('ADV-NO-CRITERIA', rel, '', ''))
        disp = d.get('display') or {}
        for fld in ('title', 'description'):
            v = disp.get(fld)
            if isinstance(v, dict) and 'translate' in v:
                k = v['translate']
                ns = aid.split(':')[0]
                has = any(k in L[n][lg] for n in L for lg in L[n])
                if not has: out.append(('ADV-KEY-MISSING', rel, k, ''))
        for crit_name, crit in (d.get('criteria') or {}).items():
            txt = json.dumps(crit)
            for it in set(re.findall(r'"((?:%s):[a-z0-9_/]+)"' % '|'.join(map(re.escape, nss)), txt)):
                pass
    return out


def vanilla_clashes(files, vanilla_dir):
    """Mod crafting recipes with the same shape/inputs as a vanilla recipe but another result."""
    vf = {}
    for dp, _, fn in os.walk(os.path.join(vanilla_dir, 'data')):
        for f in fn: vf[os.path.relpath(os.path.join(dp, f), vanilla_dir).replace(os.sep, '/')] = os.path.join(dp, f)
    def sig(r):
        if r['type'] == 'minecraft:crafting_shapeless':
            return ('sl', tuple(sorted('|'.join(sorted(x)) for x in r['ins'])))
        if r['type'] == 'minecraft:crafting_shaped':
            key = r['d']['key']
            g = [['|'.join(sorted(ing_alts(key.get(c)))) if c != ' ' else '' for c in row] for row in r['d']['pattern']]
            while g and not any(g[0]): g.pop(0)
            while g and not any(g[-1]): g.pop()
            if not g: return None
            cols = [j for j in range(max(map(len, g))) if any(j < len(x) and x[j] for x in g)]
            return ('sh', tuple(tuple((x + [''] * 9)[min(cols):max(cols) + 1]) for x in g))
    vs = collections.defaultdict(list)
    for r in recipes(vf):
        k = sig(r)
        if k: vs[k].append(r)
    out = []
    for r in recipes(files):
        k = sig(r)
        for v in vs.get(k, []) if k else []:
            if v['result'] != r['result']:
                out.append(('RECIPE-VANILLA-CLASH', r['id'], f"{r['result']} vs vanilla {v['id']} -> {v['result']}", ''))
    return out

def main():
    what = sys.argv[1] if len(sys.argv) > 1 else 'all'
    files = tree()
    res = []
    if what in ('lang', 'all'): res += lang_audit(files)
    if what in ('recipes', 'all'): res += recipe_audit(files)
    if what in ('cycles', 'vanilla') and len(sys.argv) > 2:
        cyc, n = conversion_cycles(files, sys.argv[2]); res += cyc + vanilla_clashes(files, sys.argv[2])
        print(f'# {n} single-item conversion edges checked', file=sys.stderr)
    if what in ('names', 'all'): res += names_audit(files)
    if what in ('loot', 'all'): res += loot_audit(files)
    if what in ('tags', 'all'): res += tags_audit(files)
    if what in ('adv', 'all'): res += advancement_audit(files)
    for r in res: print('\t'.join(str(x).replace('\n', ' ') for x in r))
    print(f'# {len(res)} findings, {len(files)} files', file=sys.stderr)

# ---------------------------------------------------------------- conversion cycles
def conversion_cycles(files, vanilla_dir=None):
    """Single-item conversions (stonecutting, shapeless/shaped with one item kind): find cycles whose
    product of ratios is > 1 (item duplication by crafting back and forth)."""
    import math
    fl = dict(files)
    if vanilla_dir:
        for dp, _, fn in os.walk(os.path.join(vanilla_dir, 'data')):
            for f in fn:
                full = os.path.join(dp, f); fl.setdefault(os.path.relpath(full, vanilla_dir).replace(os.sep, '/'), full)
    rs = recipes(fl)
    tg = tags(fl)
    edges = []
    for r in rs:
        res = r['result']
        if not res or r['type'] not in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'minecraft:stonecutting'): continue
        slots = [tuple(sorted(s)) for s in r['ins']]
        if not slots or len(set(slots)) != 1 or not slots[0]: continue
        cnt = r['d'].get('result', {}).get('count', 1) if isinstance(r['d'].get('result'), dict) else 1
        n = len(slots)
        for a in slots[0]:
            srcs = tag_members(tg, a[1:]) if a.startswith('#') else {a}
            for s in srcs:
                if s != res: edges.append((s, res, cnt / n, r['id']))
    nodes = {e[0] for e in edges} | {e[1] for e in edges}
    idx = {n: i for i, n in enumerate(sorted(nodes))}
    # Bellman-Ford on -log(ratio): negative cycle = gain cycle
    dist = {n: 0.0 for n in nodes}; pred = {}
    w = [(a, b, -math.log(r), rid) for a, b, r, rid in edges if r > 0]
    last = None
    for _ in range(len(nodes) + 1):
        last = None
        for a, b, wt, rid in w:
            if dist[a] + wt < dist[b] - 1e-9:
                dist[b] = dist[a] + wt; pred[b] = (a, rid); last = b
        if last is None: break
    out = []
    if last is not None:
        x = last
        for _ in range(len(nodes)): x = pred[x][0]
        cyc = [x]; y = pred[x][0]
        while y != x: cyc.append(y); y = pred[y][0]
        cyc.reverse()
        steps = []
        prod = 1.0
        for i, n in enumerate(cyc):
            nxt = cyc[(i + 1) % len(cyc)]
            e = max((e for e in edges if e[0] == n and e[1] == nxt), key=lambda e: e[2])
            prod *= e[2]; steps.append(f'{n} -[{e[3]} x{e[2]:g}]-> {nxt}')
        out.append(('RECIPE-GAIN-CYCLE', ' ; '.join(steps), f'product {prod:g}', ''))
    return out, len(edges)


if __name__ == '__main__':
    main()
