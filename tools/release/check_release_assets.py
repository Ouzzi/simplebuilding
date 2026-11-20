#!/usr/bin/env python3
"""N117: every asset the release jar set references resolves to a file of the set (static, no game run).

    python3 tools/release/check_release_assets.py <jar dir> [--vanilla <minecraft client jar>] [--src <repo root>]
                                                  [--list-checked]          exit 1 on any missing reference

Why: the 26.3 release jar of SimpleBuilding drops 300+ paths (mc26_3/not-shipped-on-26.3.txt, written by
tools/release/dead_assets_263.py). A wrongly excluded texture only shows in a real install as a missing-texture square,
so this check reads the built jars (every *.jar in the dir plus the jars nested in META-INF/jars/) as one resource set.

References resolved across ALL jars of the set (the minecraft namespace is allowed and not checked):
- items/*.json            -> models ("minecraft:model" model, special "base"), chest/shulker_box special textures
- blockstates/*.json      -> models (variants, multipart)
- models/**.json          -> parent chain; every concrete texture; for models used by an item/blockstate every #var an
                             element face needs resolves; model textures lie in the blocks/items atlas
- atlases/*.json          -> directory sources (at least one file), single resources, paletted permutations
- equipment/*.json        -> textures/entity/equipment/<layer>/<texture>.png
- particles/*.json        -> textures/particle/<texture>.png
- code (sources of the classes the set ships, --src, default this repo):
    full "…textures/….png" literals, GUI sprite ids (Identifier literals used by blitSprite/WidgetSprites or *SPRITE*
    fields -> textures/gui/sprites/<id>.png), and run-time built paths: a "textures/…/" or "…_" prefix literal must be
    listed in RUNTIME_TEXTURES (expected files, or "at least one") - an unlisted prefix fails too.
Namespace of a code literal: explicit "ns:", fromNamespaceAndPath("ns", …), withDefaultNamespace (minecraft, allowed),
fromNamespaceAndPath(<expr>, …)/X.id(…) = the namespaces of the class's own jar; anything else is resolved in any
namespace of the set or (with --vanilla) in Minecraft; without --vanilla such a miss is only listed as unverified.
"""
from __future__ import annotations

import io
import json
import re
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import dead_assets_263  # noqa: E402  (RUNTIME_ITEM_TEXTURES, FORCED)

MC = 'minecraft'
SRC_ROOTS = ('common/src/shared/java', 'common/src/main/java', 'common/src/jei/java', 'common/src/jade/java',
             'src/main/java', 'mc26_3/overlay/java', 'mc26_3/framework/java', 'framework/src/main/java')
MODULE_SRC = ('shared/java', 'fabric/src/main/java')
STR = re.compile(r'"((?:[^"\\\n]|\\.)*)"')


def _guide_books(src: Path) -> list[str]:
    """simplebuilding item textures GuideTooltip builds per inserted chapter (GuideBooks.Book item names)."""
    f = src / 'common/src/shared/java/com/simplebuilding/guide/GuideBooks.java'
    if not f.is_file():
        return []
    names = re.findall(r'\("[a-z_]+",\s*"([a-z_]+)",\s*Shelf\.', f.read_text(encoding='utf-8'))
    return [n for n in names if n == 'guide_book' or any(n.startswith(p) for p in dead_assets_263.RUNTIME_ITEM_TEXTURES)]


# Run-time built texture paths (prefix literal + name at run time). Key 'ns:prefix' ('*' = the class's own namespaces).
# Value (reason, expected) - expected: list of names (path = prefix + name + '.png'), a callable(src) giving that list,
# None = at least one file below the prefix, [] = no load / never reached on 26.3 (not checked).
RUNTIME_TEXTURES = {
    'simplebuilding:textures/item/': (
        'GuideTooltip: one miniature book per inserted chapter (dead_assets_263.RUNTIME_ITEM_TEXTURES)', _guide_books),
    'simplebuilding:textures/entity/backpack/': ('BackpackLayer: worn backpack per tier, plain/dyed/dyed overlay', [
        f'{t}backpack{s}' for t in ('', 'reinforced_', 'netherite_', 'enderite_') for s in ('', '_dyed', '_dyed_overlay')]),
    'simplebuilding:textures/entity/training_dummy/': ('TrainingDummyRenderer', ['straw_armor_stand', 'training_dummy']),
    'simplebuilding:textures/jukebox_264/jukebox_': ('JukeboxSnapshotSpritesMixin faces', ['side', 'top', 'bottom']),
    'simplebuilding:textures/gui/container/backpack/': (
        '26.2 BackpackScreen; never opened on 26.3 (dead_assets_263.FORCED)', []),
    'simplestorage:textures/entity/backpack/': ('Simple Storage BackpackLayer', None),
    'simplestorage:textures/gui/container/backpack/': ('Simple Storage BackpackScreen per tier',
                                                      ['basic', 'reinforced', 'netherite', 'enderite']),
    'simplestorage:textures/entity/rare_shulker/': ('RareShulkers tiers', None),
    '**:textures/entity/boat/': ('boat renderers (Simple Storage: BoatWoods.namespace(wood), any namespace)', None),
    '*:textures/block': ('TexturePickerScreen: listResources scan of the blocks atlas, no fixed file', []),
    'simpleend:textures/entity/': ('EndBoatRenderer chest_boat/ or boat/ + wood', None),
    'simpleanimals:textures/entity/hedgehog/': ('HedgehogRenderer variants', None),
    'simpleanimals:textures/entity/deer/': ('DeerRenderer variants', None),
    'simpleanimals:textures/entity/boar/': ('BoarRenderer variants', None),
    'simpleanimals:textures/entity/crab/': ('CrabRenderer variants', None),
    'simplearrows:textures/entity/projectiles/': ('ArrowsFabricClient arrow renderers', None),
    'simpledinos:textures/entity/trex/': ('TrexRenderer colour/tier', None),
    'simpledinos:textures/entity/triceratops/': ('TriceratopsRenderer colour/tier', None),
    'simpledinos:textures/entity/utahraptor/': ('UtahraptorRenderer colour/tier', None),
    'simpledinos:textures/entity/parasaurolophus/': ('ParasaurolophusRenderer colour/tier', None),
    'simpledinos:textures/entity/stegosaurus/': ('StegosaurusRenderer colour/tier', None),
    'simpleriding:textures/entity/horseshoe/': ('HorseshoeLayer tiers', None),
    '*:textures/entity/equipment/humanoid_leggings/': ('LeggingsFix: a startsWith test, no load', []),
    '*:textures/palettes/': ('trim palette id withPath (namespace of the palette id; vanilla trims)', []),
    '*:textures/': ('generic prefix', []),
}


class JarSet:
    """All files of the jars in a directory (nested jars included) as one resource set."""

    def __init__(self, jar_dir: Path, vanilla: Path | None = None):
        self.files: dict[str, str] = {}          # path -> first jar label
        self.classes: dict[str, str] = {}        # com/x/Y.class -> jar label
        self.jar_ns: dict[str, set[str]] = {}    # jar label -> own namespaces
        self.namespaces: set[str] = set()         # every namespace the set ships assets for
        self.json: dict[str, list] = {}          # path -> [parsed json of every jar that ships it]
        self.jars = sorted(Path(jar_dir).glob('*.jar'))
        for j in self.jars:
            with zipfile.ZipFile(j) as z:
                self._load(z, j.name)
        self.vanilla: dict[str, bytes | None] = {}
        self.vanilla_zip = zipfile.ZipFile(vanilla) if vanilla else None
        if self.vanilla_zip:
            self.vanilla = {n: None for n in self.vanilla_zip.namelist() if n.startswith('assets/')}

    def _load(self, z: zipfile.ZipFile, label: str):
        own = self.jar_ns.setdefault(label, set())
        for name in z.namelist():
            if name.endswith('/'):
                continue
            if name.startswith('META-INF/jars/') and name.endswith('.jar'):
                self._load(zipfile.ZipFile(io.BytesIO(z.read(name))), f'{label}!{name.rsplit("/", 1)[1]}')
                continue
            if name.endswith('.class'):
                self.classes.setdefault(name, label)
                continue
            self.files.setdefault(name, label)
            parts = name.split('/')
            if len(parts) > 2 and parts[0] == 'assets' and parts[1] != MC:
                own.add(parts[1])
                self.namespaces.add(parts[1])
            if name == 'fabric.mod.json':
                try:
                    own.add(json.loads(z.read(name)).get('id'))
                except ValueError:
                    pass
            if name.endswith('.json') and name.startswith('assets/') and '/' + parts[2] + '/' in (
                    '/models/', '/items/', '/blockstates/', '/atlases/', '/equipment/', '/particles/'):
                try:
                    self.json.setdefault(name, []).append(json.loads(z.read(name)))
                except ValueError as e:
                    self.json.setdefault(name, []).append(e)

    def items(self, kind: str):
        """(path, json) of every copy of assets/*/<kind>/** in the set."""
        for p, copies in self.json.items():
            if p.split('/')[2] == kind:
                for d in copies:
                    yield p, d

    def vanilla_json(self, path: str):
        if not self.vanilla_zip or path not in self.vanilla:
            return None
        if self.vanilla[path] is None:
            self.vanilla[path] = self.vanilla_zip.read(path)
        try:
            return json.loads(self.vanilla[path])
        except ValueError:
            return None


def split_id(ref: str, default: str = MC) -> tuple[str, str]:
    ns, _, path = ref.rpartition(':')
    return (ns or default), path


class Checker:
    def __init__(self, js: JarSet, src: Path | None):
        self.js = js
        self.src = src
        self.checked = 0
        self.allowed = 0
        self.missing: list[tuple[str, str, str]] = []     # (kind, target, referrer)
        self.unverified: list[tuple[str, str, str]] = []
        self.foreign: set[str] = set()
        self.seen: set[tuple[str, str, str]] = set()
        self.log: list[str] = []
        self.atlas_dirs: dict[str, set[str]] = {}  # atlas id -> directory sources
        self.atlas_singles: dict[str, set[str]] = {}
        self.virtual: set[str] = set()   # 'ns:path' sprites from paletted permutations
        self._atlases()

    # -- helpers -------------------------------------------------------------------------------------------------
    def need(self, kind: str, target: str, referrer: str) -> bool:
        """target = jar path 'assets/ns/...'. minecraft = allowed. Returns True when present (or allowed)."""
        ns = target.split('/')[1]
        if ns == MC:
            self.allowed += 1
            return True
        if ns not in self.js.namespaces:
            # Content for an optional partner mod outside the set (simpleslabs' half slabs of Simple Chess blocks are
            # only registered when that mod is loaded): nothing of that namespace ships, so nothing can be excluded.
            self.foreign.add(ns)
            return True
        key = (kind, target, referrer)
        if key in self.seen:
            return target in self.js.files
        self.seen.add(key)
        self.checked += 1
        self.log.append(f'{kind}\t{target}\t{referrer}')
        if target in self.js.files:
            return True
        self.missing.append(key)
        return False

    def texture(self, kind: str, ref: str, referrer: str, atlas: tuple[str, ...] = ()) -> None:
        ns, path = split_id(ref)
        if ns != MC and f'{ns}:{path}' in self.virtual:
            self.checked += 1
            return
        if self.need(kind, f'assets/{ns}/textures/{path}.png', referrer) and atlas and ns != MC:
            if not any(self.in_atlas(a, path) for a in atlas):
                self.miss('model texture outside the blocks/items atlas', f'assets/{ns}/textures/{path}.png', referrer)

    def miss(self, kind, target, referrer):
        key = (kind, target, referrer)
        if key not in self.seen:
            self.seen.add(key)
            self.missing.append(key)

    def in_atlas(self, atlas: str, path: str) -> bool:
        return (any(path.startswith(d + '/') for d in self.atlas_dirs.get(atlas, ()))
                or any(s.split(':')[-1] == path for s in self.atlas_singles.get(atlas, ())))

    def any_ns(self, rel: str) -> bool:
        """rel = path below the namespace ('textures/x.png'): in any namespace of the set or of Minecraft."""
        suffix = '/' + rel
        return any(f.startswith('assets/') and f.endswith(suffix) and f.count('/') == rel.count('/') + 2
                   for f in self.js.files) or f'assets/{MC}/{rel}' in self.js.vanilla

    # -- atlases ---------------------------------------------------------------------------------------------------
    def _atlas_files(self):
        out = list(self.js.items('atlases'))
        if self.js.vanilla_zip:
            out += [(p, self.js.vanilla_json(p)) for p in self.js.vanilla if p.startswith(f'assets/{MC}/atlases/')]
        else:  # Minecraft's own sources the model check relies on
            out += [(f'assets/{MC}/atlases/blocks.json', {'sources': [{'type': 'directory', 'source': 'block'}]}),
                    (f'assets/{MC}/atlases/items.json', {'sources': [{'type': 'directory', 'source': 'item'}]})]
        return out

    def _atlases(self):
        for path, data in self._atlas_files():
            if not isinstance(data, dict):
                continue
            atlas = path.split('/')[1] + ':' + path.split('/', 3)[3][:-5]
            for s in data.get('sources', []):
                t = s.get('type', '').split(':')[-1]
                if t == 'directory':
                    self.atlas_dirs.setdefault(atlas, set()).add(s.get('source', ''))
                elif t == 'single':
                    self.atlas_singles.setdefault(atlas, set()).add(s.get('resource', ''))
                elif t == 'paletted_permutations':
                    for tex in s.get('textures', []):
                        for k in s.get('permutations', {}):
                            ns, p = split_id(tex)
                            self.virtual.add(f'{ns}:{p}_{k}')

    def check_atlases(self):
        for path, data in self.js.items('atlases'):
            if not isinstance(data, dict):
                continue
            for s in data.get('sources', []):
                t = s.get('type', '').split(':')[-1]
                if t == 'directory':
                    self.checked += 1
                    src = s.get('source', '')
                    if not any(f.startswith('assets/') and f.split('/', 3)[2:3] == ['textures']
                               and f.split('/', 3)[3].startswith(src + '/') for f in self.js.files) \
                            and not any(p.startswith(f'assets/{MC}/textures/{src}/') for p in self.js.vanilla):
                        self.miss('atlas directory without files', f'textures/{src}/', path)
                elif t in ('single', 'unstitch'):
                    self.texture('atlas single', s.get('resource', ''), path)
                elif t == 'paletted_permutations':
                    for tex in s.get('textures', []):
                        self.texture('atlas permutation texture', tex, path)
                    if 'palette_key' in s:
                        ns, p = split_id(s['palette_key'])
                        self.texture('atlas palette key', f'{ns}:palettes/{p}', path)
                    for v in s.get('permutations', {}).values():
                        ns, p = split_id(v)
                        self.texture('atlas palette', f'{ns}:palettes/{p}', path)

    # -- models ----------------------------------------------------------------------------------------------------
    def model(self, ref: str):
        ns, path = split_id(ref)
        p = f'assets/{ns}/models/{path}.json'
        d = (self.js.json.get(p) or [None])[0]
        if d is None and ns == MC:
            d = self.js.vanilla_json(p)
        return p, d if isinstance(d, dict) else None

    def chain(self, ref: str, referrer: str):
        """[(path, model)] leaf first; None when a link is unknown (minecraft without --vanilla) - missing ones report."""
        out, seen = [], set()
        while ref and ref not in seen:
            seen.add(ref)
            ns, path = split_id(ref)
            if path.startswith('builtin/'):
                out.append((ref, {}))
                return out
            p, d = self.model(ref)
            if ns != MC and not self.need('model', p, referrer):
                return None
            if d is None:
                return None
            out.append((p, d))
            referrer = p
            ref = d.get('parent')
        return out

    def check_models(self):
        for p, d in self.js.items('models'):
            if not isinstance(d, dict):
                continue
            if 'parent' in d:
                pns, ppath = split_id(d['parent'])
                if pns != MC and not ppath.startswith('builtin/'):
                    self.need('model parent', f'assets/{pns}/models/{ppath}.json', p)
            for v in (d.get('textures') or {}).values():
                if isinstance(v, dict):
                    v = v.get('sprite', '')
                if isinstance(v, str) and v and not v.startswith('#'):
                    self.texture('model texture', v, p, (f'{MC}:blocks', f'{MC}:items'))

    def check_leaf(self, ref: str, referrer: str):
        ch = self.chain(ref, referrer)
        if not ch:
            return
        textures: dict[str, str] = {}
        for _, d in reversed(ch):
            for k, v in (d.get('textures') or {}).items():
                textures[k] = v.get('sprite', '') if isinstance(v, dict) else v
        elements = next((d['elements'] for _, d in ch if 'elements' in d), None)
        if elements is None:
            return
        needed = {f['texture'] for e in elements if isinstance(e, dict)
                  for f in (e.get('faces') or {}).values() if isinstance(f, dict) and 'texture' in f}
        for var in sorted(needed):
            v, hops = var, 0
            while isinstance(v, str) and v.startswith('#') and hops < 16:
                v, hops = textures.get(v[1:]), hops + 1
            self.checked += 1
            if not v or v.startswith('#'):
                self.miss(f'unresolved texture variable {var}', ch[0][0], referrer)

    def walk_models(self, o, referrer: str, kind: str):
        if isinstance(o, dict):
            t = str(o.get('type', '')).split(':')[-1]
            if kind == 'items' and t == 'model' and isinstance(o.get('model'), str):
                self.check_leaf(o['model'], referrer)
            if kind == 'items' and t == 'special' and isinstance(o.get('base'), str):
                self.check_leaf(o['base'], referrer)
            if kind == 'items' and t == 'chest' and isinstance(o.get('texture'), str):
                ns, p = split_id(o['texture'])
                self.texture('chest special texture', f'{ns}:entity/chest/{p}', referrer)
            if kind == 'items' and t == 'shulker_box' and isinstance(o.get('texture'), str):
                ns, p = split_id(o['texture'])
                self.texture('shulker special texture', f'{ns}:entity/shulker/{p}', referrer)
            if kind == 'blockstates' and isinstance(o.get('model'), str):
                self.check_leaf(o['model'], referrer)
            for k, v in o.items():
                if not (kind == 'items' and k in ('model', 'base') and isinstance(v, str)):
                    self.walk_models(v, referrer, kind)
        elif isinstance(o, list):
            for v in o:
                self.walk_models(v, referrer, kind)

    def check_items_blockstates(self):
        for p, copies in self.js.json.items():
            for d in copies:
                if isinstance(d, Exception):
                    self.miss(f'unreadable json ({d})', p, p)
                elif p.split('/')[2] in ('items', 'blockstates'):
                    self.walk_models(d, p, p.split('/')[2])

    # -- equipment / particles -------------------------------------------------------------------------------------
    def check_equipment_particles(self):
        for p, d in [*self.js.items('equipment'), *self.js.items('particles')]:
            kind = p.split('/')[2]
            if kind == 'equipment' and isinstance(d, dict):
                for layer, entries in (d.get('layers') or {}).items():
                    for e in entries or []:
                        if isinstance(e, dict) and isinstance(e.get('texture'), str) and not e.get('use_player_texture'):
                            ns, t = split_id(e['texture'])
                            self.texture('equipment layer', f'{ns}:entity/equipment/{layer}/{t}', p)
            elif kind == 'particles' and isinstance(d, dict):
                for t in d.get('textures', []):
                    ns, t2 = split_id(t)
                    self.texture('particle texture', f'{ns}:particle/{t2}', p)

    # -- code ------------------------------------------------------------------------------------------------------
    def sources(self):
        """(java file, shipped class label) of every source whose top-level class the set ships."""
        if not self.src:
            return
        roots = [self.src / r for r in SRC_ROOTS]
        mods = self.src / 'modules'
        if mods.is_dir():
            roots += [m / s for m in sorted(mods.iterdir()) for s in MODULE_SRC]
        for r in roots:
            if not r.is_dir():
                continue
            for f in sorted(r.rglob('*.java')):
                label = self.js.classes.get(f.relative_to(r).with_suffix('.class').as_posix())
                if label:
                    yield f, label

    @staticmethod
    def context(text: str, pos: int) -> str:
        """Namespace context of the literal starting at pos: 'mc', 'ns:<x>', 'own' or 'any'."""
        before = text[max(0, pos - 160):pos].rstrip()
        if re.search(r'withDefaultNamespace\(\s*$', before):
            return 'mc'
        m = re.search(r'fromNamespaceAndPath\(\s*"([a-z0-9_.-]+)"\s*,\s*$', before)
        if m:
            return 'ns:' + m.group(1)
        if re.search(r'(fromNamespaceAndPath\([^,()"]+(\([^()]*\))?\s*,|\bid\()\s*$', before):
            return 'own'
        return 'any'

    def candidates(self, ctx: str, label: str) -> set[str] | None:
        if ctx.startswith('ns:'):
            return {ctx[3:]}
        if ctx == 'own':
            return self.js.jar_ns.get(label, set()) - {MC}
        return None

    def code_texture(self, kind: str, rel: str, ctx: str, label: str, where: str):
        """rel = path below the namespace."""
        if ctx == 'mc':
            self.allowed += 1
            return
        cands = self.candidates(ctx, label)
        self.checked += 1
        self.log.append(f'{kind}\t{ctx}:{rel}\t{where}')
        if cands is not None:
            if not any(f'assets/{ns}/{rel}' in self.js.files for ns in cands):
                if MC in cands:
                    return
                self.miss(kind, f'assets/{"|".join(sorted(cands)) or "?"}/{rel}', where)
            return
        if self.any_ns(rel):
            return
        if self.js.vanilla_zip:
            self.miss(kind, f'assets/*/{rel}', where)
        else:
            self.unverified.append((kind, f'assets/*/{rel}', where))

    def check_code(self):
        for f, label in self.sources():
            text = f.read_text(encoding='utf-8', errors='replace')
            where = f.relative_to(self.src).as_posix()
            for m in STR.finditer(text):
                s = m.group(1)
                ns_lit, _, path = s.rpartition(':')
                if not path.startswith('textures/') or ' ' in s:
                    continue
                ctx = 'ns:' + ns_lit if ns_lit else self.context(text, m.start())
                if ctx == 'ns:' + MC:
                    ctx = 'mc'
                if path.endswith('.png'):
                    self.code_texture('code texture literal', path, ctx, label, where)
                else:
                    self.runtime_prefix(path, ctx, label, where)
            # GUI sprites: Identifier literals in statements that draw sprites or fields such statements use.
            stmts = text.split(';')
            used = set()
            for st in stmts:
                if re.search(r'blitSprite|WidgetSprites', st):
                    used.update(re.findall(r'\b([A-Z][A-Z0-9_]+)\b', st))
            for st in stmts:
                decl = re.search(r'\b(Identifier|WidgetSprites|Identifier\[\]|List<Identifier>)\s+([A-Z][A-Z0-9_]+)\s*=', st)
                mapper = re.search(r'Sheets\.([A-Z_]+)_MAPPER', st)
                if mapper:  # a SpriteId of another atlas: Sheets.CHEST_MAPPER -> textures/entity/chest/<id>.png
                    sub = {'CHEST': 'entity/chest/', 'SHULKER': 'entity/shulker/'}.get(mapper.group(1))
                    for m in re.finditer(r'fromNamespaceAndPath\(\s*"([a-z0-9_.-]+)"\s*,\s*"([a-z0-9_./-]+)"\s*\)', st):
                        if sub:
                            self.code_texture('code sprite (chest/shulker atlas)', f'textures/{sub}{m.group(2)}.png',
                                              'ns:' + m.group(1), label, where)
                    continue
                if not (re.search(r'blitSprite|WidgetSprites', st) or (decl and (decl.group(2) in used or 'SPRITE' in decl.group(2)))):
                    continue
                base = text.find(st)
                for m in re.finditer(r'(withDefaultNamespace|fromNamespaceAndPath|\bid)\(\s*(?:"([a-z0-9_.-]+)"\s*,\s*|[^,()"]+(?:\([^()]*\))?\s*,\s*)?"([^"]+)"\s*\)', st):
                    fn, ns, path = m.groups()
                    if path.startswith('textures/') or ':' in path or not re.fullmatch(r'[a-z0-9_./-]+', path):
                        continue
                    ctx = 'mc' if fn == 'withDefaultNamespace' else ('ns:' + ns if ns else self.context(text, base + m.start(3)))
                    if ctx == 'any':
                        ctx = 'own'
                    self.code_texture('gui sprite', f'textures/gui/sprites/{path}.png', ctx, label, where)

    def runtime_prefix(self, prefix: str, ctx: str, label: str, where: str):
        if ctx == 'mc':
            self.allowed += 1
            return
        cands = self.candidates(ctx, label)
        entry = None
        for key, val in RUNTIME_TEXTURES.items():
            ns, _, p = key.partition(':')
            if p == prefix and (ns in ('*', '**') or cands is None or ns in cands):
                entry = (key, val)
                break
        self.checked += 1
        if entry is None:
            self.miss('run-time texture prefix not listed in RUNTIME_TEXTURES', f'{ctx}:{prefix}', where)
            return
        key, (_, expected) = entry
        ns = key.split(':')[0]
        nss = ([ns] if ns not in ('*', '**') else sorted(self.js.namespaces) if ns == '**'
               else sorted(cands or self.js.jar_ns.get(label, set()) - {MC}))
        if callable(expected):
            expected = expected(self.src)
        if expected is None:
            if not any(f.startswith(f'assets/{n}/{prefix}') for n in nss for f in self.js.files):
                self.miss('run-time texture prefix without files', f'assets/{"|".join(nss)}/{prefix}', where)
            return
        for name in expected:
            target = f'assets/{nss[0]}/{prefix}{name}.png'
            if target in dead_assets_263.FORCED:
                continue
            self.need('run-time texture', target, where)

    def run(self):
        self.check_atlases()
        self.check_models()
        self.check_items_blockstates()
        self.check_equipment_particles()
        self.check_code()
        return self


def main(argv: list[str]) -> int:
    args, vanilla, src, listing = [], None, Path(__file__).resolve().parents[2], False
    it = iter(argv)
    for a in it:
        if a == '--vanilla':
            vanilla = Path(next(it))
        elif a == '--src':
            src = Path(next(it))
        elif a == '--list-checked':
            listing = True
        else:
            args.append(a)
    if len(args) != 1:
        print(__doc__)
        return 2
    js = JarSet(Path(args[0]), vanilla)
    if not js.jars:
        print(f'no jars in {args[0]}')
        return 2
    c = Checker(js, src).run()
    if listing:
        print('\n'.join(sorted(c.log)))
    for kind, target, ref in sorted(c.unverified):
        print(f'UNVERIFIED {kind}: {target}  <- {ref}')
    for kind, target, ref in sorted(c.missing):
        print(f'MISSING {kind}: {target}  <- {ref}')
    if c.foreign:
        print(f'skipped references into namespaces no jar of the set ships (optional partner mods): {sorted(c.foreign)}')
    print(f'{len(js.jars)} jars, {len(js.files)} files, {len(js.classes)} classes; {c.checked} references checked, '
          f'{c.allowed} minecraft references allowed, {len(c.unverified)} unverified, {len(c.missing)} missing')
    return 1 if c.missing else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
