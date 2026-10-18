#!/usr/bin/env python3
"""Finds items/blocks that would show the purple/black "missing texture" checkerboard (MC 26.3).

No Gradle, no Minecraft: it rebuilds the shipped resource tree from the repository layers and resolves
  assets/<ns>/items/*.json  -> models (model / composite / select / condition / range_dispatch / special base)
  assets/<ns>/blockstates/*.json -> models
  models -> parent chain -> textures (layerN / #variables used by faces) -> assets/<ns>/textures/<path>.png
  plus the sprites named by special models (chest / shulker_box / bed / banner / head textures are checked loosely).
`minecraft:*` references count as present (optionally checked against a client jar: --jar).
Also reported: lang keys item.<ns>.<id> / block.<ns>.<id> whose item has no assets/<ns>/items/<id>.json
(heuristic, shown as WARN unless the id is registered as a block without item, see ALLOW_NO_ITEM).

Layers, highest priority first (same precedence as mc26_3/resources.gradle mergeResources263 and the module projects):
  vanilla-derived outputs (never committed; vanilla_derived.OUTPUTS/MODULE_OUTPUTS count as present)
  mc26_3/overlay/resources, mc26_3/generated (minus removed-on-26.3.txt), src/main/resources, src/main/generated,
  modules/<id>/shared/resources, modules/<id>/generated/resources
(modules/simplemodels/examples is an example resource pack, not shipped, and is skipped.)

Usage: python3 tools/check_missing_textures.py [--ns simplebuilding] [--jar client.jar] [-v]
Exit 1 on any ERROR. Documented in AGENTS.md section 4.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Registered block ids that intentionally have no inventory item (wall signs, technical blocks ...): lang key
# block.<ns>.<id> without items/<id>.json is only a WARN anyway; this list silences known cases.
# The 11 lang-only ids found 2026-10-18: guide_book_* are legacy aliases (datafix/LegacyItemIds -> guide_book; the books of
# GuideBooks share one item model) and home_teleporter is only a tooltip name (SpawnSetup), no registered item. The runtime
# audit (testrunner target audit-models-263, ModelAuditClientTest) walks the real item registry and confirms 0 faults.
ALLOW_NO_ITEM: set[str] = {'guide_book_admin', 'guide_book_building', 'guide_book_enchantments', 'guide_book_end',
                           'guide_book_gadgets', 'guide_book_machines', 'guide_book_storage', 'guide_book_tools',
                           'guide_book_trims', 'guide_book_tweaks', 'home_teleporter'}


def layer_roots() -> list[tuple[str, Path]]:
    roots = [('overlay', ROOT / 'mc26_3/overlay/resources'), ('gen263', ROOT / 'mc26_3/generated'),
             ('main', ROOT / 'src/main/resources'), ('gen262', ROOT / 'src/main/generated')]
    for m in sorted((ROOT / 'modules').iterdir()):
        for sub in ('shared/resources', 'generated/resources'):
            if (m / sub / 'assets').is_dir():
                roots.append((f'{m.name}/{sub.split("/")[0]}', m / sub))
    return roots


def derived_paths() -> set[str]:
    """Texture paths (assets/<ns>/textures/..) that the build draws from the client jar (not in the repo)."""
    out: set[str] = set()
    try:
        sys.path.insert(0, str(ROOT / 'tools/textures'))
        import vanilla_derived as vd  # imports only stdlib at module level
        out |= {f'assets/{vd.NS}/textures/{k}' for k in vd.OUTPUTS}
        for mod, table in vd.MODULE_OUTPUTS.items():
            out |= {f'assets/{mod}/textures/{k}' for k in table}
    except Exception as e:  # pragma: no cover
        print(f'WARN could not read tools/textures/vanilla_derived.py ({e}); derived textures not recognised')
    return out


class Pool:
    """Union of all shipped asset files: relative path (assets/...) -> absolute Path, precedence applied."""

    def __init__(self) -> None:
        self.files: dict[str, Path] = {}
        self.derived = derived_paths()
        removed: set[str] = set()
        rm = ROOT / 'mc26_3/generated/removed-on-26.3.txt'
        if rm.is_file():
            removed = {l.strip() for l in rm.read_text('utf-8').splitlines() if l.strip() and not l.startswith('#')}
        overlay_files: set[str] = set()
        for name, root in layer_roots():
            for f in (root / 'assets').rglob('*'):
                if not f.is_file():
                    continue
                rel = f.relative_to(root).as_posix()
                if name == 'gen262' and rel in removed:
                    continue
                if name == 'main' and rel in overlay_files:
                    continue
                if name == 'overlay':
                    overlay_files.add(rel)
                self.files.setdefault(rel, f)
        for d in self.derived:
            self.files.setdefault(d, ROOT / 'build/vanilla-derived' / d)
        self.atlas_sprites: set[str] = set()
        # sprite id prefixes/singles of the blocks + items atlases (Vanilla 26.3 defaults + the mod atlas files)
        self.atlas_prefixes: set[str] = {'block/', 'item/', 'entity/conduit/', 'trims/items/'}
        self.atlas_singles: set[str] = set()
        self.atlas_errors: list[str] = []
        self._atlas_permutations()

    def _atlas_permutations(self) -> None:
        """Sprites made by atlas paletted_permutations (<texture>_<key>, e.g. the trim item overlays)."""
        for rel in sorted(self.files):
            if not re.fullmatch(r'assets/[^/]+/atlases/[a-z_]+\.json', rel):
                continue
            for src in self.json(rel).get('sources', []):
                kind = src.get('type', '').split(':')[-1]
                if kind == 'directory' and rel.endswith(('/blocks.json', '/items.json')):
                    self.atlas_prefixes.add(src.get('prefix', ''))
                elif kind == 'single' and rel.endswith(('/blocks.json', '/items.json')):
                    self.atlas_singles.add(split(src['resource'])[1] if split(src['resource'])[0] == 'minecraft' else src['resource'])
                if kind != 'paletted_permutations':
                    continue
                for tex in src.get('textures', []):
                    ns, path = split(tex)
                    if ns != 'minecraft' and f'assets/{ns}/textures/{path}.png' not in self.files:
                        self.atlas_errors.append(f'atlas {rel}: permutation base texture {ns}:{path} missing')
                    for key in src.get('permutations', {}):
                        self.atlas_sprites.add(f'assets/{ns}/textures/{path}_{key}.png')

    def has(self, rel: str) -> bool:
        return rel in self.files or rel in self.atlas_sprites

    def json(self, rel: str):
        return json.loads(self.files[rel].read_text('utf-8'))


def split(ref: str, default_ns: str = 'minecraft') -> tuple[str, str]:
    if ':' in ref:
        ns, p = ref.split(':', 1)
        return ns, p
    return default_ns, ref


class Checker:
    def __init__(self, pool: Pool, jar: set[str] | None, only_ns: str | None) -> None:
        self.pool, self.jar, self.only_ns = pool, jar, only_ns
        self.errors: list[str] = []
        self.warns: list[str] = []
        self.model_cache: dict[str, tuple[list[str], dict[str, str], set[str], bool] | None] = {}
        self.checked_models = 0

    # -- existence ----------------------------------------------------------------------------------------
    def exists(self, ns: str, kind: str, path: str, ext: str) -> bool:
        rel = f'assets/{ns}/{kind}/{path}.{ext}'
        if self.pool.has(rel):
            return True
        if ns == 'minecraft':
            return self.jar is None or rel in self.jar
        return False

    def in_atlas(self, ns: str, tpath: str) -> bool:
        if f'assets/{ns}/textures/{tpath}.png' in self.pool.atlas_sprites:
            return True
        if any(tpath.startswith(p) for p in self.pool.atlas_prefixes):
            return True
        return tpath in self.pool.atlas_singles or f'{ns}:{tpath}' in self.pool.atlas_singles

    def err(self, who: str, msg: str) -> None:
        self.errors.append(f'{who}: {msg}')

    # -- models -------------------------------------------------------------------------------------------
    def model(self, ref: str, who: str, tex_ctx: bool = True) -> None:
        """Check a model reference (model ids as written in item/blockstate files, relative to models/)."""
        ns, path = split(ref)
        key = f'{ns}:{path}'
        if key in self.model_cache:
            return
        self.model_cache[key] = None
        if path.startswith('builtin/'):
            return
        if not self.exists(ns, 'models', path, 'json'):
            self.err(who, f'model {key} missing (models/{path}.json)')
            return
        if ns == 'minecraft' and not self.pool.has(f'assets/minecraft/models/{path}.json'):
            return  # vanilla model, assumed fine (and its own textures)
        self.checked_models += 1
        chain, textures, used, generated = self.resolve(ns, path, who, [])
        if generated and 'layer0' not in textures and not any(k.startswith('layer') for k in textures):
            self.err(who, f'model {key} (builtin/generated) has no layer0 texture')
        for var in sorted(used):
            if var not in textures:
                self.err(who, f'model {key} uses #{var} but no texture is defined for it in the parent chain')
        for var, val in sorted(textures.items()):
            if val.startswith('#'):
                tgt = val[1:]
                if tgt not in textures:
                    self.err(who, f'model {key}: texture "{var}" -> {val} is never defined')
                continue
            tns, tpath = split(val)
            if self.exists(tns, 'textures', tpath, 'png') and not self.in_atlas(tns, tpath):
                self.err(who, f'model {key}: texture {tns}:{tpath} exists but is in no block/item atlas source '
                              f'(shows as missing; add it under block/ or item/, or to an atlas file)')
            if not self.exists(tns, 'textures', tpath, 'png'):
                self.err(who, f'model {key}: texture "{var}" -> {tns}:{tpath} missing '
                              f'(textures/{tpath}.png in assets/{tns})')

    def resolve(self, ns: str, path: str, who: str, seen: list[str]):
        """(chain, merged textures, used #vars, is builtin/generated) over the parent chain."""
        key = f'{ns}:{path}'
        if key in seen:
            self.err(who, f'model parent loop at {key}')
            return [], {}, set(), False
        rel = f'assets/{ns}/models/{path}.json'
        if not self.pool.has(rel):
            return [key], {}, set(), False
        try:
            data = self.pool.json(rel)
        except Exception as e:
            self.err(who, f'model {key} is not valid JSON ({e})')
            return [key], {}, set(), False
        textures: dict[str, str] = {}
        used: set[str] = set()
        generated = False
        parent = data.get('parent')
        if parent:
            pns, ppath = split(parent)
            if ppath in ('builtin/generated', 'item/generated'):
                generated = True
            elif ppath.startswith('builtin/'):
                pass
            elif not self.exists(pns, 'models', ppath, 'json'):
                self.err(who, f'model {key}: parent {pns}:{ppath} missing')
            else:
                _, ptex, pused, pgen = self.resolve(pns, ppath, who, seen + [key])
                textures.update(ptex)
                used |= pused
                generated = pgen
        for k, v in (data.get('textures') or {}).items():
            if isinstance(v, dict):  # 26.x: {"sprite": "...", "force_translucent": true}
                v = v.get('sprite')
            if isinstance(v, str):
                textures[k] = v
        for el in data.get('elements') or []:
            for face in (el.get('faces') or {}).values():
                t = face.get('texture')
                if isinstance(t, str) and t.startswith('#'):
                    used.add(t[1:])
        return [key], textures, used, generated

    # -- item definitions ---------------------------------------------------------------------------------
    def item_node(self, node, who: str, ns: str) -> None:
        if not isinstance(node, dict):
            return
        t = node.get('type', '')
        t = t if ':' in t else f'minecraft:{t}'
        if t == 'minecraft:model':
            m = node.get('model')
            if not m:
                self.err(who, 'minecraft:model without "model"')
            else:
                self.model(m, who)
        elif t == 'minecraft:composite':
            for sub in node.get('models', []):
                self.item_node(sub, who, ns)
        elif t == 'minecraft:condition':
            self.item_node(node.get('on_true'), who, ns)
            self.item_node(node.get('on_false'), who, ns)
        elif t == 'minecraft:select':
            for case in node.get('cases', []):
                self.item_node(case.get('model'), who, ns)
            self.item_node(node.get('fallback'), who, ns)
        elif t == 'minecraft:range_dispatch':
            for e in node.get('entries', []):
                self.item_node(e.get('model'), who, ns)
            self.item_node(node.get('fallback'), who, ns)
        elif t == 'minecraft:special':
            if node.get('base'):
                self.model(node['base'], who)
            self.special(node.get('model') or {}, who)
        elif t in ('minecraft:empty', 'minecraft:bundle/selected_item'):
            pass
        else:
            self.warns.append(f'{who}: item model type {t} not checked')

    def special(self, m: dict, who: str) -> None:
        t = m.get('type', '')
        tex = m.get('texture')
        folder = {'minecraft:chest': 'entity/chest', 'minecraft:shulker_box': 'entity/shulker',
                  'minecraft:bed': 'entity/bed', 'minecraft:banner': 'entity/banner',
                  'minecraft:decorated_pot': None}.get(t)
        if tex and folder:
            tns, tp = split(tex)
            if tns != 'minecraft' and not self.exists(tns, 'textures', f'{folder}/{tp}', 'png'):
                self.err(who, f'special {t} texture {tns}:{tp} missing (textures/{folder}/{tp}.png)')

    def check_items(self) -> int:
        n = 0
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/items/(.+)\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            n += 1
            who = f'item {m.group(1)}:{m.group(2)}'
            try:
                data = self.pool.json(rel)
            except Exception as e:
                self.err(who, f'invalid JSON ({e}) [{self.pool.files[rel].relative_to(ROOT)}]')
                continue
            if 'model' not in data:
                self.err(who, 'item definition has no "model"')
                continue
            self.item_node(data['model'], who, m.group(1))
        return n

    # -- blockstates --------------------------------------------------------------------------------------
    def check_blockstates(self) -> int:
        n = 0
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/blockstates/(.+)\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            n += 1
            who = f'blockstate {m.group(1)}:{m.group(2)}'
            try:
                data = self.pool.json(rel)
            except Exception as e:
                self.err(who, f'invalid JSON ({e})')
                continue

            def apply(v):
                if isinstance(v, list):
                    for x in v:
                        apply(x)
                elif isinstance(v, dict) and v.get('model'):
                    self.model(v['model'], who)

            for v in (data.get('variants') or {}).values():
                apply(v)
            for part in data.get('multipart') or []:
                apply(part.get('apply'))
        return n

    # -- particles, equipment, sounds, textures named in Java code (Nachtrag 66) --------------------------
    def check_particles(self) -> int:
        """assets/<ns>/particles/*.json -> textures/particle/<name>.png (particle atlas)."""
        n = 0
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/particles/(.+)\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            n += 1
            who = f'particle {m.group(1)}:{m.group(2)}'
            try:
                data = self.pool.json(rel)
            except Exception as e:
                self.err(who, f'invalid JSON ({e})')
                continue
            for tex in data.get('textures') or []:
                tns, tp = split(tex)
                if not self.exists(tns, 'textures', f'particle/{tp}', 'png'):
                    self.err(who, f'texture {tns}:{tp} missing (textures/particle/{tp}.png in assets/{tns})')
        return n

    def check_equipment(self) -> int:
        """assets/<ns>/equipment/*.json layers -> textures/entity/equipment/<layer type>/<name>.png."""
        n = 0
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/equipment/(.+)\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            n += 1
            who = f'equipment {m.group(1)}:{m.group(2)}'
            try:
                data = self.pool.json(rel)
            except Exception as e:
                self.err(who, f'invalid JSON ({e})')
                continue
            for layer, entries in (data.get('layers') or {}).items():
                for e in entries:
                    tex = e.get('texture')
                    if not tex or e.get('use_player_texture'):
                        continue
                    tns, tp = split(tex)
                    if not self.exists(tns, 'textures', f'entity/equipment/{layer}/{tp}', 'png'):
                        self.err(who, f'layer {layer} texture {tns}:{tp} missing '
                                      f'(textures/entity/equipment/{layer}/{tp}.png in assets/{tns})')
        return n

    def check_sounds(self) -> int:
        """assets/<ns>/sounds.json -> sounds/<name>.ogg (a missing file is silent, not purple, but just as broken)."""
        n = 0
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/sounds\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            ns = m.group(1)
            try:
                data = self.pool.json(rel)
            except Exception as e:
                self.err(f'sounds {ns}', f'invalid JSON ({e})')
                continue
            for event, spec in data.items():
                for snd in spec.get('sounds', []):
                    name = snd if isinstance(snd, str) else snd.get('name', '')
                    if isinstance(snd, dict) and snd.get('type') == 'event':
                        continue
                    n += 1
                    sns, sp = split(name, ns)
                    if sns != 'minecraft' and not self.pool.has(f'assets/{sns}/sounds/{sp}.ogg'):
                        self.err(f'sound {ns}:{event}', f'file {sns}:{sp} missing (sounds/{sp}.ogg)')
        return n

    def check_java_textures(self) -> int:
        """String literals "textures/..." in the Java sources. Complete paths (.png) must exist; a path built
        at run time ("textures/entity/x/" + name + ".png") needs at least its folder to exist and be non-empty."""
        lit = re.compile(r'"(textures/[A-Za-z0-9_/.\-]*)"')
        n = 0
        mod_ns = {m.name: m.name for m in (ROOT / 'modules').iterdir()}
        for base in ('common/src', 'src/main/java', 'mc26_3', 'neoforge/src', 'forge/src', 'modules'):
            for f in sorted((ROOT / base).rglob('*.java')):
                posix = f.as_posix()
                if '/build/' in posix or '/gametest' in posix or 'clienttest' in posix or 'clientgametest' in posix \
                        or '/mc26_2/' in posix or '/test/' in posix:
                    continue
                text = f.read_text('utf-8', 'replace')
                file_ns = 'simplebuilding'
                mm = re.search(r'/modules/([^/]+)/', posix)
                if mm:
                    file_ns = mm.group(1)
                for m in lit.finditer(text):
                    path = m.group(1)
                    before = text[max(0, m.start() - 90):m.start()]
                    line_no = text.count('\n', 0, m.start()) + 1
                    who = f'java {f.relative_to(ROOT)}:{line_no}'
                    if re.search(r'withDefaultNamespace\(\s*$', before):
                        ns = 'minecraft'
                    else:
                        fm = re.search(r'fromNamespaceAndPath\(\s*([^,()]+?)\s*,\s*$', before)
                        if fm:
                            arg = fm.group(1)
                            if arg.startswith('"'):
                                ns = arg.strip('"')
                            elif arg.endswith('MOD_ID'):
                                ns = 'simplebuilding' if arg.startswith('Simplebuilding') else file_ns
                            else:
                                continue  # namespace from a variable: not decidable here
                        elif re.search(r'(\.id|Id|withPath)\(\s*(\w+\s*\+\s*)?$', before) or re.search(r'\bid\(\s*$', before):
                            ns = file_ns
                        elif re.search(r'(name|location|id|path|ref)\w*\.withPath\(\s*$', before) or before.rstrip().endswith('withPath('):
                            continue  # path of an arbitrary identifier
                        else:
                            continue
                    if self.only_ns and ns != self.only_ns:
                        continue
                    if path.endswith('.png'):
                        n += 1
                        tp = path[len('textures/'):-len('.png')]
                        if not self.exists(ns, 'textures', tp, 'png'):
                            self.err(who, f'texture {ns}:{path} is named in code but missing')
                    elif path.endswith('/'):
                        n += 1
                        folder = f'assets/{ns}/{path}'
                        if ns == 'minecraft':
                            ok = self.jar is None or any(j.startswith(folder) for j in self.jar)
                        else:
                            ok = any(k.startswith(folder) for k in self.pool.files)
                        if not ok:
                            self.err(who, f'texture folder {ns}:{path} (names built at run time) is empty or missing')
        return n

    # -- items without definition -------------------------------------------------------------------------
    def all_ns(self) -> set[str]:
        return {rel.split('/')[1] for rel in self.pool.files}

    def check_lang(self) -> None:
        for rel in sorted(self.pool.files):
            m = re.fullmatch(r'assets/([^/]+)/lang/en_us\.json', rel)
            if not m or (self.only_ns and m.group(1) != self.only_ns):
                continue
            ns = m.group(1)
            try:
                lang = self.pool.json(rel)
            except Exception:
                continue
            for key in lang:
                km = re.fullmatch(rf'(item)\.{re.escape(ns)}\.([a-z0-9_/.]+)', key)
                if not km:
                    continue
                ident = km.group(2)
                if '.' in ident or ident in ALLOW_NO_ITEM:
                    continue
                if not self.pool.has(f'assets/{ns}/items/{ident}.json'):
                    if any(self.pool.has(f'assets/{o}/items/{ident}.json') for o in self.all_ns()):
                        continue  # moved to another namespace (e.g. simplelib materials), lang key is a leftover
                    self.warns.append(f'item {ns}:{ident}: lang key but no assets/{ns}/items/{ident}.json '
                                      f'(if that item is registered it renders as the missing model)')


def check_registered(c: 'Checker') -> None:
    """registerItem("name"...) in the Java sources: some namespace must have assets/*/items/name.json."""
    have = {m.group(1) for rel in c.pool.files if (m := re.fullmatch(r'assets/[^/]+/items/(.+)\.json', rel))}
    pat = re.compile(r'registerItem\(\s*"([a-z0-9_]+)"')
    have_bs = {m.group(1) for rel in c.pool.files if (m := re.fullmatch(r'assets/[^/]+/blockstates/(.+)\.json', rel))}
    bpat = re.compile(r'registerBlock\(\s*"([a-z0-9_]+)"')
    for base in ('common/src/shared/java', 'modules'):
        for f in (ROOT / base).rglob('*.java'):
            if '/build/' in f.as_posix():
                continue
            text = f.read_text('utf-8', 'replace')
            for name in bpat.findall(text):
                if name not in have_bs and not name.endswith('_'):
                    c.warns.append(f'registered block "{name}" ({f.relative_to(ROOT)}): no blockstates/{name}.json in any namespace')
            for name in pat.findall(text):
                if name not in have and not name.endswith('_'):
                    c.warns.append(f'registered item "{name}" ({f.relative_to(ROOT)}): no items/{name}.json in any namespace')


def jar_names(path: str) -> set[str]:
    with zipfile.ZipFile(path) as z:
        return set(z.namelist())


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--ns', help='only check definitions of this namespace')
    ap.add_argument('--jar', help='Minecraft client jar: also verify minecraft:* references')
    ap.add_argument('--no-warn', action='store_true', help='hide WARN lines')
    ap.add_argument('-v', '--verbose', action='store_true')
    a = ap.parse_args()
    pool = Pool()
    c = Checker(pool, jar_names(a.jar) if a.jar else None, a.ns)
    c.errors += pool.atlas_errors
    ni = c.check_items()
    nb = c.check_blockstates()
    np_, ne, ns_, nj = c.check_particles(), c.check_equipment(), c.check_sounds(), c.check_java_textures()
    c.check_lang()
    if not a.ns:
        check_registered(c)
    errs = sorted(set(c.errors))
    for e in errs:
        print('ERROR', e)
    if not a.no_warn:
        for w in sorted(set(c.warns)):
            print('WARN ', w)
    print(f'check_missing_textures: {ni} item definitions, {nb} blockstates, {c.checked_models} own models checked, '
          f'{np_} particles, {ne} equipment assets, {ns_} sounds, {nj} Java texture paths, {len(errs)} errors, {len(set(c.warns))} warnings')
    print('alles gruen' if not errs else 'NICHT gruen')
    return 1 if errs else 0


if __name__ == '__main__':
    sys.exit(main())
