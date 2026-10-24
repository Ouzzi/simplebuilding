#!/usr/bin/env python3.12
"""Module completeness audit (read-only, no Gradle): inventory + gaps of every module in modules/modules.json.

Per module: namespaces, blocks (blockstates), items (item definitions), counts (recipes, loot, advancements,
entities, configs, commands, tests), and findings:
  lang EN/DE parity and a name per block/item, loot table per block, mineable tag per block, an origin per
  item (recipe / loot / other data; "alone" = only this module + its hard requirements), JEI/REI/EMI plugin and
  categories, custom recipe types without an own JEI category, creative tab hook, wiki manual, config
  tooltips, standalone target, and a comparison with the state BEFORE the module splits (git BASELINE):
  every id that moved namespace keeps its recipes, block loot, extra lang keys (tooltips, JEI info),
  vanilla/c tags, advancements and its mention in JEI/REI code.

Usage:
  python3.12 tools/audit/module_completeness.py                 # summary per module on stdout
  python3.12 tools/audit/module_completeness.py --json out.json --md out.md [--baseline <rev>]
Exit code is always 0 (audit, not a gate). Heuristic codes are marked "check" in the text.
"""
import argparse, collections, glob, json, os, re, subprocess, sys

R = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
BASELINE = '134cb5a06^'   # last commit before "split stage 1" (2026-10-11)
FOREIGN_NS = {'minecraft', 'c', 'fabric', 'neoforge', 'forge', 'jei', 'trinkets', 'curios', 'accessories'}
CORE_ROOTS = ['src/main/generated', 'src/main/resources', 'mc26_3/generated', 'mc26_3/overlay/resources']
REMOVED = 'mc26_3/generated/removed-on-26.3.txt'
ID_RE = re.compile(r'^#?[a-z0-9_.-]+:[a-z0-9_./-]+$')
SEV = ['P1', 'P2', 'P3', 'P4']


def jl(path):
    try:
        with open(path, encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return None


def jls(text):
    try:
        return json.loads(text)
    except Exception:
        return None


def strings(o):
    """Every string value in a JSON tree."""
    if isinstance(o, str):
        yield o
    elif isinstance(o, dict):
        for v in o.values():
            yield from strings(v)
    elif isinstance(o, list):
        for v in o:
            yield from strings(v)


def result_ids(r):
    out = set()
    for k in ('result', 'output', 'results', 'result_item', 'outputs'):
        if k in r:
            out |= {s for s in strings(r[k]) if ID_RE.match(s) and not s.startswith('#')}
    return out


def conditions(r):
    """Mod ids a recipe/loot file needs (fabric load conditions)."""
    need = set()
    for c in r.get('fabric:load_conditions', []) if isinstance(r, dict) else []:
        if isinstance(c, dict) and c.get('condition') in ('fabric:all_mods_loaded', 'fabric:any_mods_loaded'):
            need |= set(c.get('values', []))
    return need


# ---------------------------------------------------------------- resource trees
class Tree:
    """Files of one module: rel path (data/... or assets/...) -> text loader."""

    def __init__(self, files):
        self.files = files  # rel -> absolute path or ('git', rev, path)
        self._cache = {}

    def json(self, rel):
        if rel not in self._cache:
            self._cache[rel] = jl(self.files[rel]) if isinstance(self.files[rel], str) else jls(self.files[rel][1])
        return self._cache[rel]

    def glob(self, regex):
        rx = re.compile(regex)
        return [(rel, rx.match(rel)) for rel in sorted(self.files) if rx.match(rel)]


def walk_roots(roots, removed=()):
    files = {}
    for root in roots:
        ab = os.path.join(R, root)
        for sub in ('data', 'assets'):
            for dp, _, fn in os.walk(os.path.join(ab, sub)):
                for f in fn:
                    full = os.path.join(dp, f)
                    files[os.path.relpath(full, ab).replace(os.sep, '/')] = full
    for r in removed:
        files.pop(r, None)
    return files


def module_roots(m):
    p = m['paths']
    roots = []
    for cand in (p.get('generated'), os.path.join(p['root'], 'shared/resources'), os.path.join(p['root'], 'generated')):
        if cand and os.path.isdir(os.path.join(R, cand)) and cand not in roots and \
                (os.path.isdir(os.path.join(R, cand, 'data')) or os.path.isdir(os.path.join(R, cand, 'assets'))):
            roots.append(cand)
    return roots


def read_removed(text):
    return {l.strip() for l in text.splitlines() if l.strip() and not l.startswith('#')}


def git(*args):
    return subprocess.run(['git', '-C', R, *args], capture_output=True, text=True, errors='replace').stdout


def git_files(rev, paths):
    """{repo path: content} for every blob under paths at rev (one cat-file batch)."""
    names = [n for n in git('ls-tree', '-r', '--name-only', rev, '--', *paths).splitlines()
             if n.endswith(('.json', '.java', '.txt')) and '/build/' not in n]
    if not names:
        return {}
    p = subprocess.run(['git', '-C', R, 'cat-file', '--batch'], input=('\n'.join(f'{rev}:{n}' for n in names) + '\n').encode(),
                       capture_output=True, text=False)
    data, out, i = p.stdout, {}, 0
    for n in names:
        nl = data.index(b'\n', i)
        header = data[i:nl].split()
        if len(header) < 3 or header[1] == b'missing':
            i = nl + 1
            continue
        size = int(header[2])
        out[n] = data[nl + 1:nl + 1 + size].decode('utf-8', 'replace')
        i = nl + 1 + size + 1
    return out


# ---------------------------------------------------------------- per-module facts
class Mod:
    def __init__(self, m, files, java_dirs):
        self.m, self.id = m, m['id']
        self.tree = Tree(files)
        lang_ns = os.path.basename(os.path.dirname(m['paths']['lang']))
        self.ns = lang_ns
        self.lang = {loc: self._lang(loc) for loc in ('en_us', 'de_de')}
        self.java = {}
        for d in java_dirs:
            for f in glob.glob(os.path.join(R, d, '**/*.java'), recursive=True):
                if '/build/' not in f and '/clienttest/' not in f:
                    try:
                        self.java[os.path.relpath(f, R)] = open(f, encoding='utf-8', errors='ignore').read()
                    except OSError:
                        pass
        self.blocks = sorted(mt.group(1) for _, mt in self.tree.glob(rf'assets/{self.ns}/blockstates/([a-z0-9_/]+)\.json$'))
        defs = sorted(mt.group(1) for _, mt in self.tree.glob(rf'assets/{self.ns}/items/([a-z0-9_/]+)\.json$'))
        en = self.lang['en_us']
        named = lambda i: f'item.{self.ns}.{i}' in en or f'block.{self.ns}.{i}' in en
        # stand-in item models for renderers (placed/held displays, book variants): no registry entry of their own
        self.render_only = [i for i in defs if not named(i) and ('/' in i or re.match(r'(placed|held|standing|enchanted_book)_', i))]
        self.items = [i for i in defs if i not in self.render_only]
        self.findings = []

    def _lang(self, loc):
        out = {}
        for rel, _ in self.tree.glob(rf'assets/{re.escape(self.ns)}/lang/{loc}\.json$'):
            out.update(self.tree.json(rel) or {})
        return out

    def add(self, sev, code, msg, ids=()):
        self.findings.append({'severity': sev, 'code': code, 'message': msg, 'ids': sorted(ids)[:60],
                              'count': len(ids) if ids else 1})


def build_index(mods):
    """Global data index over all modules: recipes, loot, tags, other data mentions, advancements."""
    idx = {'recipes': [], 'loot': [], 'tags': collections.defaultdict(set), 'mention': collections.defaultdict(set),
           'adv': collections.defaultdict(set)}
    for mod in mods.values():
        t = mod.tree
        for rel in t.files:
            if not rel.endswith('.json') or not rel.startswith('data/'):
                continue
            parts = rel.split('/')
            ns, kind = parts[1], parts[2] if len(parts) > 2 else ''
            if kind == 'recipe':
                r = t.json(rel)
                if isinstance(r, dict):
                    idx['recipes'].append({'mod': mod.id, 'rel': rel, 'type': r.get('type', ''), 'need': conditions(r),
                                           'results': result_ids(r)})
            elif kind == 'loot_table':
                r = t.json(rel)
                if r is not None:
                    idx['loot'].append({'mod': mod.id, 'rel': rel, 'need': conditions(r) if isinstance(r, dict) else set(),
                                        'items': {s for s in strings(r) if ID_RE.match(s) and not s.startswith('#')}})
            elif kind == 'tags' and len(parts) >= 5:
                r = t.json(rel) or {}
                reg = parts[3]
                name = f"{ns}:{'/'.join(parts[4:])[:-5]}"
                for v in r.get('values', []) if isinstance(r, dict) else []:
                    vid = v.get('id') if isinstance(v, dict) else v
                    if isinstance(vid, str):
                        idx['tags'][(reg, name)].add(vid)
            elif kind == 'advancement':
                r = t.json(rel)
                if r is not None and '/recipes/' not in rel:
                    for s in strings(r):
                        if ID_RE.match(s):
                            idx['adv'][s].add(f'{mod.id}:{rel}')
            else:
                r = t.json(rel)
                if r is not None:
                    for s in strings(r):
                        if ID_RE.match(s) and not s.startswith('#'):
                            idx['mention'][s].add(mod.id)
    return idx


def tag_closure(tags, reg, name, seen=None):
    seen = seen if seen is not None else set()
    if (reg, name) in seen:
        return set()
    seen.add((reg, name))
    out = set()
    for v in tags.get((reg, name), ()):
        if v.startswith('#'):
            out |= tag_closure(tags, reg, v[1:], seen)
        else:
            out.add(v)
    return out


def hard_closure(mid, modmap):
    out, todo = set(), [mid]
    while todo:
        x = todo.pop()
        if x in out or x not in modmap:
            continue
        out.add(x)
        todo += [r for r in modmap[x]['requires'] if r in modmap]
    return out


# ---------------------------------------------------------------- checks
NO_LOOT_HINT = re.compile(r'(^|_)wall_|_portal$|^placed_|fire$|^moving_|_cauldron$|lava$|water$|_air$|_piston_head$')
# Vanilla 26.3 mineable tags pull these vanilla block tags in (the vanilla jar is not read here).
VANILLA_MINEABLE_REFS = {
    'axe': ['logs', 'logs_that_burn', 'overworld_natural_logs', 'planks', 'wooden_buttons', 'wooden_doors', 'wooden_fences', 'fence_gates', 'wooden_pressure_plates',
            'wooden_slabs', 'wooden_stairs', 'wooden_trapdoors', 'all_signs', 'all_hanging_signs', 'signs',
            'standing_signs', 'wall_signs', 'ceiling_hanging_signs', 'wall_hanging_signs', 'banners', 'bamboo_blocks',
            'campfires', 'wooden_shelves', 'beds'],
    'hoe': ['leaves', 'wart_blocks'],
    'pickaxe': ['stone_buttons', 'rails', 'anvil', 'cauldrons', 'copper_chests', 'lightning_rods', 'bars', 'chains',
                'shulker_boxes'],
    'shovel': ['concrete_powder'],
}


def check_lang(mod):
    en, de = mod.lang['en_us'], mod.lang['de_de']
    if not en and not (mod.blocks or mod.items):
        return
    miss_de, miss_en = sorted(set(en) - set(de)), sorted(set(de) - set(en))
    if miss_de:
        mod.add('P2', 'LANG-MISSING-DE', f'{len(miss_de)} keys in en_us without de_de', miss_de)
    if miss_en:
        mod.add('P2', 'LANG-MISSING-EN', f'{len(miss_en)} keys in de_de without en_us', miss_en)
    ns = mod.ns
    nb = [b for b in mod.blocks if f'block.{ns}.{b}' not in en and not NO_LOOT_HINT.search(b)]
    if nb:
        mod.add('P2', 'LANG-NO-BLOCK-NAME', 'blocks without block.<ns>.<id> in en_us', nb)
    ni = [i for i in mod.items if f'item.{ns}.{i}' not in en and f'block.{ns}.{i}' not in en]
    if ni:
        mod.add('P2', 'LANG-NO-ITEM-NAME', 'item definitions without item./block. name in en_us', ni)
    orphan = sorted({k.split('.', 2)[2] for k in en if re.match(rf'^(item|block)\.{re.escape(ns)}\.[a-z0-9_]+$', k)
              and k.split('.', 2)[2] not in mod.items and k.split('.', 2)[2] not in mod.blocks})
    if orphan and mod.id == 'simplebuilding':
        mod.add('P4', 'LANG-NAME-WITHOUT-MODEL', 'name keys without a 26.3 item definition/blockstate: ids moved to '
                'sub-mods on 26.3 only; 26.2/1.21.11 still register them under simplebuilding - keep', orphan)
    elif orphan:
        mod.add('P4', 'LANG-NAME-WITHOUT-MODEL', 'name keys without item definition/blockstate (unused or registered '
                'without resources - check)', orphan)


def check_loot_tags(mod, idx):
    ns = mod.ns
    no_loot, hinted = [], []
    for b in mod.blocks:
        if f'data/{ns}/loot_table/blocks/{b}.json' in mod.tree.files:
            continue
        (hinted if NO_LOOT_HINT.search(b) else no_loot).append(b)
    overrides = any(re.search(r'List<ItemStack>\s+getDrops\(', j) for j in mod.java.values())
    if no_loot and overrides:
        mod.add('P4', 'LOOT-MISSING-GETDROPS', 'no loot table, but the module overrides Block#getDrops (check the '
                'block class)', no_loot)
    elif no_loot:
        mod.add('P2', 'LOOT-MISSING', 'blocks without data/<ns>/loot_table/blocks/<id>.json (check dropsLike/noLootTable '
                'in Java before adding)', no_loot)
    if hinted:
        mod.add('P4', 'LOOT-MISSING-EXPECTED', 'no loot table, name suggests dropsLike/technical block', hinted)
    mineable = set()
    for tool in ('pickaxe', 'axe', 'shovel', 'hoe', 'sword'):
        for tn in (f'minecraft:mineable/{tool}', f'fabric:mineable/{tool}', f'c:mineable/{tool}'):
            mineable |= tag_closure(idx['tags'], 'block', tn)
        for ref in VANILLA_MINEABLE_REFS.get(tool, ()):
            mineable |= tag_closure(idx['tags'], 'block', 'minecraft:' + ref)
    nom = [b for b in mod.blocks if f'{ns}:{b}' not in mineable and not NO_LOOT_HINT.search(b)]
    if nom:
        mod.add('P3', 'TAG-NO-MINEABLE', 'blocks in no mineable/* tag (fine for instant-break blocks - check hardness)', nom)


def check_origin(mod, idx, modmap):
    ns = mod.ns
    alone = hard_closure(mod.id, modmap)
    alone_ns = alone | {modmap[x]['ns'] for x in alone}
    produced, produced_alone, kinds = collections.defaultdict(int), collections.defaultdict(int), collections.Counter()
    for r in idx['recipes']:
        ok_alone = r['mod'] in alone and r['need'] <= alone_ns
        for res in r['results']:
            produced[res] += 1
            if ok_alone:
                produced_alone[res] += 1
        if r['mod'] == mod.id:
            kinds[r['type']] += 1
    looted, looted_alone = set(), set()
    for lt in idx['loot']:
        looted |= lt['items']
        if lt['mod'] in alone and lt['need'] <= alone_ns:
            looted_alone |= lt['items']
    java_all = '\n'.join(mod.java.values())
    none, only_with_partner, code_only = [], [], []
    for i in sorted(set(mod.items) | set(mod.blocks)):
        full = f'{ns}:{i}'
        if i in mod.blocks and i not in mod.items:
            continue  # block without an item (wall variants, technical blocks)
        src_any = produced[full] or full in looted or (idx['mention'][full] - {mod.id}) or idx['mention'][full]
        src_alone = produced_alone[full] or full in looted_alone or (idx['mention'][full] & alone)
        if not src_any:
            if i.endswith('_spawn_egg'):
                continue  # creative/spawner only, like Vanilla
            stem = re.sub(r'^(music_disc_|enchanted_book_)', '', i)
            (code_only if f'"{i}"' in java_all or f'"{stem}"' in java_all else none).append(i)
        elif not src_alone and f'"{i}"' not in java_all and not re.search(rf'\b{re.escape(i.upper())}\b', java_all):
            only_with_partner.append(i)
    if none:
        mod.add('P2', 'ITEM-NO-ORIGIN', 'items without recipe, loot, other data source or mention in Java', none)
    if code_only:
        mod.add('P3', 'ITEM-ORIGIN-CODE-ONLY', 'no recipe/loot/data source; the id is named in Java (loot injection, '
                'conversion, drop or creative-only - check)', code_only)
    if only_with_partner:
        mod.add('P3', 'ITEM-NO-ORIGIN-ALONE', 'items whose only recipe/loot needs a partner mod and that no module Java '
                'names (standalone rule 1 - fine if the item is only registered with the partner)',
                only_with_partner)
    # recipe types of a foreign namespace without a load condition break the standalone recipe manager
    bad = []
    for r in idx['recipes']:
        if r['mod'] != mod.id:
            continue
        tns = r['type'].split(':')[0] if ':' in r['type'] else 'minecraft'
        if tns not in alone_ns and tns not in FOREIGN_NS and tns not in r['need']:
            bad.append(r['rel'])
    if bad:
        mod.add('P1', 'RECIPE-FOREIGN-TYPE-UNGATED', 'recipes of another mod\'s type without load condition', bad)
    return kinds


JEI_PLUGIN = re.compile(r'implements\s+IModPlugin|@JeiPlugin')
REI_PLUGIN = re.compile(r'implements\s+REI(Client|Server|Common)Plugin')
EMI_PLUGIN = re.compile(r'implements\s+EmiPlugin|@EmiEntrypoint')


def jei_facts(java, fabric_mod_json):
    plug = [f for f, s in java.items() if JEI_PLUGIN.search(s)]
    rei = [f for f, s in java.items() if REI_PLUGIN.search(s)]
    emi = [f for f, s in java.items() if EMI_PLUGIN.search(s)]
    cats = sorted({os.path.basename(f)[:-5] for f, s in java.items()
                   if re.search(r'implements\s+IRecipeCategory|extends\s+AbstractRecipeCategory', s)})
    types = sorted({f'{a}:{b}' if a else b for s in java.values()
                    for a, b in re.findall(r'RecipeType\.create\(\s*(?:"([a-z0-9_]+)"|[A-Z_a-z.]+)\s*,\s*"([a-z0-9_/]+)"', s)})
    info = sum(len(re.findall(r'addIngredientInfo|addItemStackInfo', s)) for s in java.values())
    registered = None
    if plug:
        ent = (fabric_mod_json or {}).get('entrypoints', {}).get('jei_mod_plugin', [])
        names = {os.path.basename(f)[:-5] for f in plug}
        registered = all(any(e.endswith('.' + n) or (isinstance(e, dict) and str(e.get('value', '')).endswith(n))
                             for e in ent) for n in names)
    return {'jei_plugin': [os.path.basename(f) for f in plug], 'rei_plugin': [os.path.basename(f) for f in rei],
            'emi_plugin': [os.path.basename(f) for f in emi], 'categories': cats, 'recipe_types': types,
            'info_calls': info, 'fabric_entrypoint_ok': registered}


def check_jei(mod, kinds, core_jei_text):
    fmj = jl(os.path.join(R, mod.m['paths']['fabric'], 'src/main/resources/fabric.mod.json')) if mod.id != 'simplebuilding' \
        else jl(os.path.join(R, 'src/main/resources/fabric.mod.json'))
    facts = jei_facts(mod.java, fmj)
    if facts['jei_plugin'] and facts['fabric_entrypoint_ok'] is False:
        mod.add('P1', 'JEI-PLUGIN-NOT-REGISTERED', 'JEI plugin class exists but fabric.mod.json has no jei_mod_plugin '
                'entrypoint for it (check: loader-specific plugin may live in fabric/)', facts['jei_plugin'])
    own_types = [t for t in kinds if t and not t.startswith('minecraft:') and t.split(':')[0] not in ('fabric',)]
    jtext = core_jei_text  # JEI/REI code of every module: the owner of a recipe type draws its category
    missing, vanilla_cat = [], []
    for t in own_types:
        path = t.split(':', 1)[1]
        if path in jtext or t in jtext:
            continue
        cls = [s for s in mod.java.values() if f'"{path}"' in s or f'{{@code {t}}}' in s]
        (vanilla_cat if any(re.search(r'implements\s+(CraftingRecipe|SmithingRecipe)|extends\s+(Shapeless|Shaped|Custom)Recipe', c)
                            for c in cls) else missing).append(f'{t} ({kinds[t]} recipes)')
    if missing:
        mod.add('P2', 'JEI-RECIPE-TYPE-NO-CATEGORY', 'custom recipe types not named in any JEI plugin code (module or '
                'core) - recipes invisible in JEI [b-jei24]', missing)
    if vanilla_cat:
        mod.add('P3', 'JEI-RECIPE-TYPE-VANILLA-CLASS', 'custom serializer on a Vanilla crafting/smithing recipe class, no '
                'JEI code names it - check that JEI draws it (special/custom recipes stay hidden) [b-jei24]', vanilla_cat)
    if (mod.items or mod.blocks) and not facts['jei_plugin']:
        mod.add('P3', 'JEI-NO-PLUGIN', 'module has items/blocks but no own JEI plugin (no info pages / categories '
                'standalone) [b-jei24]')
    return facts


TAB_RE = re.compile(r'CreativeModeTab|ItemGroupEvents|BuildCreativeModeTabContentsEvent|CreativeTabs?\b|creativeTab|TabEntries|ModTabs')


def check_misc(mod, modmap):
    java_all = '\n'.join(mod.java.values())
    if (mod.items or mod.blocks) and not TAB_RE.search(java_all):
        mod.add('P2', 'TAB-NO-HOOK', 'items/blocks but no creative tab code found in module Java')
    man = jl(os.path.join(R, mod.m['paths']['wikiManual']))
    feats = len(man.get('features', [])) if isinstance(man, dict) else (len(man) if isinstance(man, list) else 0)
    if not feats:
        mod.add('P3', 'WIKI-NO-MANUAL', 'wiki manual missing or without features')
    wd = jl(os.path.join(R, 'wiki/data', mod.id + '.json'))
    undoc = len(wd.get('undocumented', [])) if isinstance(wd, dict) else 0
    if wd is None and mod.id not in ('wiringexample',):
        mod.add('P3', 'WIKI-NO-DATA', f'wiki/data/{mod.id}.json missing (wiki/generate.py --all)')
    elif undoc:
        mod.add('P3', 'WIKI-UNDOCUMENTED', f'{undoc} entries without wiki prose (wiki/data undocumented)',
                [u if isinstance(u, str) else u.get('id', '?') for u in wd['undocumented']])
    if mod.id not in ('simplebuilding', 'wiringexample', 'simplelib') and 'standalone' not in (mod.m.get('tests') or {}):
        mod.add('P2', 'TEST-NO-STANDALONE', 'no standalone test target (rule 8)')
    # config keys (listed only; b-cmdcfg24 owns commands/config)
    en = mod.lang['en_us']
    opts = [k for k in en if re.search(r'(\.config\.|^text\.autoconfig\.[^.]+\.option\.)', k)
            and not re.search(r'(\.tooltip|@Tooltip|\.desc|\.title|\.category|\.section|\.header)(\.\d+)?$', k)]
    tips = {re.sub(r'(\.tooltip|\.@Tooltip|\.desc)(\.\d+)?$', '', k) for k in en
            if re.search(r'(\.tooltip|@Tooltip|\.desc)(\.\d+)?$', k)}
    no_tip = [k for k in opts if k not in tips]
    if no_tip:
        mod.add('P3', 'CONFIG-NO-TOOLTIP', 'config labels without tooltip key [b-cmdcfg24]', no_tip)
    cmds = sorted({c for s in mod.java.values()
                   for c in re.findall(r'\.register\(\s*(?:Commands\.)?literal\(\s*"([a-z0-9_]+)"', s)})
    tests = 0
    for cat in (mod.m.get('tests') or {}).get('catalogues', []):
        try:
            s = open(os.path.join(R, cat), encoding='utf-8').read()
            tests += len(re.findall(r'@GameTest\b', s)) + len(re.findall(r'GameTestSpec\.named\(', s))
        except OSError:
            pass
    if mod.id == 'simplebuilding':
        s = open(os.path.join(R, 'common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java'),
                 encoding='utf-8').read()
        tests = len(re.findall(r'GameTestSpec\.named\(', s))
    return {'opts': len(opts), 'no_tip': len(no_tip), 'commands': cmds, 'tests': tests, 'wiki_features': feats,
            'tab': bool(TAB_RE.search(java_all)), 'undocumented': undoc,
            'config_classes': sorted({os.path.basename(f)[:-5] for f in mod.java if re.search(r'Config[A-Za-z]*\.java$', f)
                                      and '/test/' not in f})}


# ---------------------------------------------------------------- before the splits
def baseline_compare(mods, idx, rev, core_jei_now):
    """Ids that changed namespace since rev: what the old owner had that the new one lacks."""
    roots = list(CORE_ROOTS) + ['modules', 'common/src/jei', 'common/src/rei']
    old = git_files(rev, roots)
    if not old:
        return None
    removed = read_removed(old.get(REMOVED, ''))
    rootset = list(CORE_ROOTS) + sorted({'/'.join(p.split('/')[:3]) for p in old if p.startswith('modules/')
                                         and p.count('/') >= 3 and p.split('/')[2] in ('shared', 'generated')})
    # old effective files: rel -> content (core layers in order, removed hidden only for core roots)
    old_rel = {}
    for root in rootset:
        pre = root + '/'
        for p, c in old.items():
            if p.startswith(pre):
                rel = p[len(pre):]
                if rel.startswith('resources/'):
                    rel = rel[len('resources/'):]
                if not (rel.startswith('data/') or rel.startswith('assets/')):
                    continue
                if root in CORE_ROOTS and rel in removed:
                    continue
                old_rel.setdefault(rel, []).append(c)
    old_ids = collections.defaultdict(set)
    for rel in old_rel:
        mt = re.match(r'assets/([a-z0-9_]+)/(items|blockstates)/([a-z0-9_/]+)\.json$', rel)
        if mt:
            old_ids[mt.group(1)].add(mt.group(3))
    now = {}
    for mod in mods.values():
        for i in set(mod.items) | set(mod.blocks):
            now.setdefault(i, set()).add(mod.ns)
    moved = []   # (old_ns, path, new_ns, new_mod)
    ns2mod = {mod.ns: mod for mod in mods.values()}
    for ons, paths in old_ids.items():
        cur = ns2mod.get(ons)
        cur_ids = set(cur.items) | set(cur.blocks) if cur else set()
        for p in sorted(paths):
            if p in cur_ids:
                continue
            for nns in sorted(now.get(p, ())):
                if nns != ons:
                    moved.append((ons, p, nns, ns2mod[nns].id))
    # old lookup tables
    old_lang = collections.defaultdict(dict)
    old_recipes, old_tags, old_adv = collections.defaultdict(list), collections.defaultdict(set), collections.defaultdict(set)
    for rel, cs in old_rel.items():
        mt = re.match(r'assets/([a-z0-9_]+)/lang/en_us\.json$', rel)
        if mt:
            for c in cs:
                old_lang[mt.group(1)].update(jls(c) or {})
            continue
        if not rel.endswith('.json') or not rel.startswith('data/'):
            continue
        parts = rel.split('/')
        kind = parts[2] if len(parts) > 2 else ''
        r = jls(cs[-1])
        if r is None:
            continue
        if kind == 'recipe' and isinstance(r, dict):
            for res in result_ids(r):
                old_recipes[res].append(r.get('type', ''))
        elif kind == 'tags' and len(parts) >= 5 and parts[1] in FOREIGN_NS:
            name = f"{parts[1]}:{parts[3]}/{'/'.join(parts[4:])[:-5]}"
            for c in cs:
                for v in (jls(c) or {}).get('values', []):
                    vid = v.get('id') if isinstance(v, dict) else v
                    if isinstance(vid, str):
                        old_tags[vid].add(name)
        elif kind == 'advancement' and '/recipes/' not in rel:
            for s in strings(r):
                if ID_RE.match(s):
                    old_adv[s].add(rel)
    old_jei = '\n'.join(c for p, c in old.items() if p.endswith('.java') and
                        ('/jei/' in p or '/rei/' in p or 'Jei' in os.path.basename(p) or 'Rei' in os.path.basename(p)))
    new_tags = collections.defaultdict(set)
    for (reg, name), vals in idx['tags'].items():
        if name.split(':')[0] in FOREIGN_NS:
            for v in tag_closure(idx['tags'], reg, name):
                new_tags[v].add(f"{name.split(':')[0]}:{reg}/{name.split(':', 1)[1]}")
    new_res = collections.defaultdict(list)
    for r in idx['recipes']:
        for res in r['results']:
            new_res[res].append(r)
    report = collections.defaultdict(lambda: collections.defaultdict(list))
    java_now = '\n'.join(j for mod in mods.values() for j in mod.java.values())
    for ons, p, nns, nmod in moved:
        o, n = f'{ons}:{p}', f'{nns}:{p}'
        mod = mods[nmod]
        # recipes
        ot = collections.Counter(old_recipes.get(o, []))
        nt = collections.Counter(r['type'] for r in new_res.get(n, []))
        if ot and not nt:
            report[nmod]['RECIPE-LOST'].append(f"{p} ({', '.join(sorted(ot))})")
        elif ot:
            lost = sorted(set(t.split(':')[-1] for t in ot) - set(t.split(':')[-1] for t in nt))
            if lost:
                report[nmod]['RECIPE-KIND-LOST'].append(f"{p}: {', '.join(lost)}")
        # loot
        if f'data/{ons}/loot_table/blocks/{p}.json' in old_rel and \
                f'data/{nns}/loot_table/blocks/{p}.json' not in mod.tree.files:
            report[nmod]['LOOT-LOST'].append(p)
        # extra lang keys (tooltips, descriptions, JEI info)
        ol, nl = old_lang.get(ons, {}), mod.lang['en_us']
        core_now = mods[ns2mod[ons].id].lang['en_us'] if ons in ns2mod else {}
        for k in ol:
            mt = re.match(rf'^(item|block)\.{ons}\.{re.escape(p)}\.(.+)$', k)
            if not mt:
                continue
            nk = f'{mt.group(1)}.{nns}.{p}.{mt.group(2)}'
            if nk not in nl:
                where = ' (still only in old namespace)' if k in core_now else ''
                report[nmod]['LANG-EXTRA-LOST'].append(f'{nk}{where}')
        # tooltip/JEI/Jade keys that name the old id as a segment (tooltip.<ns>.<id>, jei.<ns>.info.<id>, ...)
        for cat in ('tooltip', 'jei', 'jade'):
            oks = [k for k in ol if k.startswith(f'{cat}.{ons}.') and p in k.split('.')[2:]]
            if not oks:
                continue
            nks = [k for k in nl if k.startswith(f'{cat}.') and p in k.split('.')[2:]]
            if not nks:
                still = any(k in core_now for k in oks)
                # used = the full key is named, or a generic "<prefix>." + "<id>" builder names both parts
                used = any(f'"{k}"' in java_now or (f'"{k.rsplit(".", 1)[0]}."' in java_now and f'"{k.rsplit(".", 1)[1]}"' in java_now)
                           for k in oks)
                report[nmod]['LANG-EXTRA-LOST' if used or not still else 'LANG-LEFTOVER'].append(
                    f"{p}: {cat} ({len(oks)} old keys, e.g. {oks[0]}{'; still in old namespace' if still else ''})")
        # vanilla/c tags
        lost_tags = sorted(old_tags.get(o, set()) - new_tags.get(n, set()))
        lost_tags = [t for t in lost_tags if not t.endswith('/craftable')]
        if lost_tags:
            report[nmod]['TAG-LOST'].append(f"{p}: {', '.join(lost_tags)}")
        # advancements
        if old_adv.get(o) and not idx['adv'].get(n) and not idx['adv'].get(o):
            report[nmod]['ADVANCEMENT-LOST'].append(f"{p} (was in {len(old_adv[o])})")
        # JEI/REI code mentions
        if (f'"{o}"' in old_jei or re.search(rf'\b{re.escape(p.upper())}\b', old_jei) or f'"{p}"' in old_jei) and \
                not (f'"{n}"' in core_jei_now or f'"{p}"' in core_jei_now or re.search(rf'\b{re.escape(p.upper())}\b', core_jei_now)):
            report[nmod]['JEI-MENTION-LOST'].append(p)
    sev = {'RECIPE-LOST': 'P1', 'LOOT-LOST': 'P1', 'RECIPE-KIND-LOST': 'P2', 'TAG-LOST': 'P2', 'LANG-EXTRA-LOST': 'P2',
           'ADVANCEMENT-LOST': 'P3', 'JEI-MENTION-LOST': 'P2', 'LANG-LEFTOVER': 'P4'}
    text = {'RECIPE-LOST': 'had a recipe before the split, now none produces it',
            'RECIPE-KIND-LOST': 'recipe kinds (type) the old id had and the new one lacks',
            'LOOT-LOST': 'old id had a block loot table, new namespace has none',
            'LANG-EXTRA-LOST': 'extra lang keys (tooltip/desc/info) of the old id missing under the new namespace',
            'TAG-LOST': 'vanilla/c/fabric tags the old id was in and the new id is not',
            'ADVANCEMENT-LOST': 'named in an advancement before, in none now (check)',
            'LANG-LEFTOVER': 'old tooltip/JEI/Jade keys of the moved id stay in the old namespace and no Java uses them '
                             '(cleanup; the new owner covers it otherwise, e.g. own JEI info pages)',
            'JEI-MENTION-LOST': 'named in old JEI/REI code, in no JEI/REI code now (in-world/info pages?) [b-jei24]'}
    for nmod, codes in report.items():
        for code, ids in codes.items():
            mods[nmod].add(sev[code], 'SPLIT-' + code, text[code], ids)
    per = collections.Counter(m for _, _, _, m in moved)
    return {'baseline': rev, 'moved_ids': len(moved), 'moved_per_module': dict(per)}


# ---------------------------------------------------------------- main
def load_purposes():
    return jl(os.path.join(R, 'tools/audit/module_purposes.json')) or {}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--json'), ap.add_argument('--md'), ap.add_argument('--baseline', default=BASELINE)
    ap.add_argument('--no-baseline', action='store_true')
    a = ap.parse_args()
    manifest = json.load(open(os.path.join(R, 'modules/modules.json'), encoding='utf-8'))['modules']
    removed = read_removed(open(os.path.join(R, REMOVED), encoding='utf-8').read())
    mods, modmap = {}, {}
    for m in manifest:
        if m['id'] == 'simplebuilding':
            files = walk_roots(CORE_ROOTS, removed)
            jd = ['common/src/shared/java', 'common/src/jei', 'common/src/rei', 'src/main/java', 'mc26_3/fabric/src']
        else:
            files = walk_roots(module_roots(m))
            jd = [os.path.join(m['paths']['root'], d) for d in ('shared', 'fabric')]
        mods[m['id']] = Mod(m, files, jd)
    for mid, mod in mods.items():
        reqs = [r for r in mod.m.get('requires', []) if r in mods]
        modmap[mid] = {'requires': reqs, 'ns': mod.ns}
    idx = build_index(mods)
    core_jei_now = '\n'.join(s for mod in mods.values() for f, s in mod.java.items()
                             if '/jei/' in f or '/rei/' in f or 'Jei' in os.path.basename(f) or 'Rei' in os.path.basename(f))
    required_by = collections.defaultdict(list)
    for m in manifest:
        for r in m.get('requires', []):
            required_by[r].append(m['id'])
    base = None if a.no_baseline else baseline_compare(mods, idx, a.baseline, core_jei_now)
    purposes = load_purposes()
    out = []
    for m in manifest:
        mod = mods[m['id']]
        check_lang(mod)
        check_loot_tags(mod, idx)
        kinds = check_origin(mod, idx, modmap)
        jei = check_jei(mod, kinds, core_jei_now)
        misc = check_misc(mod, modmap)
        ns = mod.ns
        t = mod.tree
        adv = [rel for rel, _ in t.glob(rf'data/{ns}/advancement/.+\.json$')]
        pur = purposes.get(m['id'], {})
        entities = sorted(k.split('.', 2)[2] for k in mod.lang['en_us'] if re.match(rf'^entity\.{ns}\.[a-z0-9_]+$', k))
        out.append({
            'id': m['id'], 'namespace': ns,
            'name_en': m.get('displayName') or m.get('name'), 'name_de': pur.get('name_de', m.get('displayName')),
            'purpose_en': pur.get('purpose_en', m.get('description', '')), 'purpose_de': pur.get('purpose_de', ''),
            'supermod': m.get('parent'), 'bundle': m.get('bundle'), 'required': m.get('bundle') == 'required',
            'super_of': sorted(x['id'] for x in manifest if x.get('parent') == m['id']),
            'required_by': sorted(required_by.get(m['id'], [])), 'requires': m.get('requires', []),
            'optional': m.get('optional', []), 'standalone': 'standalone' in (m.get('tests') or {}),
            'example': bool(m.get('example')),
            'counts': {'blocks': len(mod.blocks), 'items': len(mod.items), 'render_only_item_models': len(mod.render_only), 'entities': len(entities),
                       'recipes': sum(kinds.values()), 'recipe_types': dict(kinds),
                       'loot_tables': len(t.glob(rf'data/{ns}/loot_table/.+\.json$')),
                       'advancements': len([x for x in adv if '/recipes/' not in x]),
                       'recipe_advancements': len([x for x in adv if '/recipes/' in x]),
                       'lang_keys': len(mod.lang['en_us']), 'config_options': misc['opts'],
                       'config_without_tooltip': misc['no_tip'], 'commands': len(misc['commands']),
                       'tests': misc['tests'], 'wiki_features': misc['wiki_features'], 'java_files': len(mod.java)},
            'entities': entities, 'commands': misc['commands'], 'config_classes': misc['config_classes'],
            'creative_tab_hook': misc['tab'], 'jei': jei,
            'findings': sorted(mod.findings, key=lambda f: (SEV.index(f['severity']), f['code'])),
        })
    data = {'schema': 1, 'generated_by': 'tools/audit/module_completeness.py', 'baseline': base, 'modules': out}
    if a.json:
        with open(a.json, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=1)
            f.write('\n')
    if a.md:
        with open(a.md, 'w', encoding='utf-8') as f:
            f.write(render_md(data))
    for e in out:
        c = collections.Counter(x['severity'] for x in e['findings'])
        print(f"{e['id']:22} blocks={e['counts']['blocks']:4} items={e['counts']['items']:4} recipes={e['counts']['recipes']:4} "
              f"tests={e['counts']['tests']:4} " + ' '.join(f'{s}={c[s]}' for s in SEV if c[s]))
    if base:
        print(f"baseline {base['baseline']}: {base['moved_ids']} ids moved namespace")
    return 0


def render_md(data):
    L = []
    for e in data['modules']:
        c = e['counts']
        L.append(f"### {e['name_en']} (`{e['id']}`{', ns `' + e['namespace'] + '`' if e['namespace'] != e['id'] else ''})")
        L.append('')
        if e['purpose_de']:
            L.append(e['purpose_de'])
            L.append('')
        sup = e['supermod'] or '–'
        L.append(f"- Super-Mod: {sup} ({e['bundle'] or 'eigenständig'}); enthält: {', '.join(e['super_of']) or '–'}; "
                 f"Pflicht für: {', '.join(e['required_by']) or '–'}; Standalone-Target: {'ja' if e['standalone'] else 'nein'}")
        L.append(f"- Inhalt: {c['blocks']} Blöcke, {c['items']} Items, {c['entities']} Entities, {c['recipes']} Rezepte, "
                 f"{c['loot_tables']} Loot-Tables, {c['advancements']} Advancements (+{c['recipe_advancements']} Rezeptbuch), "
                 f"{c['config_options']} Config-Labels, {c['commands']} Befehle ({', '.join('/' + x for x in e['commands']) or '–'}), "
                 f"{c['tests']} Tests, {c['wiki_features']} Wiki-Abschnitte")
        j = e['jei']
        L.append(f"- JEI: {', '.join(j['jei_plugin']) or 'kein Plugin'}; Kategorien: {', '.join(j['categories']) or '–'}; "
                 f"Info-Aufrufe: {j['info_calls']}; REI: {', '.join(j['rei_plugin']) or '–'}; EMI: {', '.join(j['emi_plugin']) or '–'}")
        if not e['findings']:
            L.append('- Funde: keine')
        for f in e['findings']:
            ids = ', '.join(f'`{x}`' for x in f['ids'][:12])
            more = f" … (+{f['count'] - 12})" if f['count'] > 12 else ''
            L.append(f"- **{f['severity']} {f['code']}** ({f['count']}): {f['message']}{': ' + ids + more if ids else ''}")
        L.append('')
    return '\n'.join(L)


if __name__ == '__main__':
    sys.exit(main())
