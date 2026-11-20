"""
Dev-only texture assets of the wiki (owner 06.11.: Vanilla textures to work with, never public).

Two jobs, both writing only files that git ignores (.gitignore: wiki/assets/vanilla/, wiki/data/vanilla-textures.js
and the Vanilla-derived copies), so nothing of Mojang's ends up in the repository or in the production wiki:

  --vanilla  every texture of the 26.3 client jar -> wiki/assets/vanilla/<path in textures/> and the index
             wiki/data/vanilla-textures.js (window.WIKI_VANILLA_TEXTURES, same fields and cards as wiki/textures.py).
             The wiki page never loads that file itself; only the dev overlay of the Zentrale (behind the login,
             tools/zentrale-web/dev-overlay.js) does, which adds the category "Vanilla".
  --icons    id -> picture for the test centre editor (N72): wiki/data/vanilla-icons.json, names and wiki paths only,
             resolved like the game (items/<id>.json -> model -> parents -> textures) by tools/devserver/sbdev/icons.py:
             every Vanilla item of the jar, every id of the test centre layouts and every module item/block of the wiki
             data (N80: Guidebook/Questionary) without an own module picture
             (slabs, stairs, walls, bars, waxed blocks, lanterns, octets ... whose texture file has another name).
  --derived  the Vanilla-derived mod textures (tools/textures/vanilla_derived.py; placeholders without jar or Pillow)
             copied to the wiki paths that wiki/data/textures.js names for them. The wiki checkouts on the web host
             are plain git trees, so without this those cards had no image.

  --published-only  (N113, production checkout) only the Vanilla-derived textures found in the published mod jars
             (index.json of the green gates - the same pixels every player downloads), no client jar, no placeholders.
  --derived and --published-only also write wiki/data/derived-present.json (git-ignored): the pictures of
  wiki/data/local-only.js this host really has; the page asks only for those (assets/module-loader.js).

Run on the dev checkout (tools/zentrale-web/sb-web-sync), --published-only on the production checkout, or locally:
    python3 wiki/vanilla_textures.py [--jar client.jar] [--vanilla] [--icons] [--derived]     (default: all)
    python3 wiki/generate.py --with-vanilla                                         (same, after the normal run)
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import tempfile
import zipfile
from pathlib import Path

WIKI = Path(__file__).resolve().parent
REPO = WIKI.parent
sys.path.insert(0, str(WIKI))
sys.path.insert(0, str(REPO / "tools" / "textures"))
import textures as tx  # noqa: E402

VANILLA_DIR = "assets/vanilla"
DATA_FILE = "data/vanilla-textures.js"
ICONS_FILE = "data/vanilla-icons.json"
MARKER = "window.WIKI_VANILLA_TEXTURES = "
PREFIX = "assets/minecraft/textures/"


def default_jar() -> Path | None:
    try:
        import vanilla_derived  # tools/textures: SB_CLIENT_JAR, -PsbClientJar, the Gradle caches
        return vanilla_derived.find_jar()
    except Exception:  # noqa: BLE001 - any import problem: fall back to the loom cache
        jar = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
        return jar if jar.is_file() else None


def build_vanilla(jar: Path) -> tuple[dict, dict[str, bytes]]:
    """Index (schema of textures.js, module "minecraft", "vanilla": true) and the files (wiki path -> bytes)."""
    writes: dict[str, bytes] = {}
    textures = []
    with zipfile.ZipFile(jar) as archive, tempfile.TemporaryDirectory() as tmp:
        names = set(archive.namelist())
        # models/blockstates/items for the card grouping (textures.model_owners reads a resource layer)
        for name in names:
            if name.startswith(("assets/minecraft/models/", "assets/minecraft/blockstates/", "assets/minecraft/items/")) \
                    and name.endswith(".json"):
                target = Path(tmp) / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(name))
        owners = tx.model_owners([Path(tmp)])
        for name in sorted(names):
            if not name.startswith(PREFIX) or not name.endswith(".png"):
                continue
            payload = archive.read(name)
            size = tx.png_size(payload)
            if not size:
                continue
            inner = name[len(PREFIX):]
            file = f"{VANILLA_DIR}/{inner}"
            writes[file] = payload
            meta = None
            if name + ".mcmeta" in names:
                try:
                    meta = json.loads(archive.read(name + ".mcmeta").decode("utf-8"))
                except ValueError:
                    meta = {"invalid": True}
            stem = inner.rsplit("/", 1)[-1][:-4]
            entry = {"id": f"minecraft:{inner[:-4]}", "module": "minecraft", "vanilla": True,
                     "kind": inner.split("/", 1)[0] if "/" in inner else "other",
                     "name": {"en": tx.pretty(stem), "de": tx.pretty(stem)},
                     "file": file, "source": "jar:" + name, "w": size[0], "h": size[1]}
            anim = tx.animation(meta, *size)
            if anim:
                entry["anim"] = anim
            if meta is not None:
                entry["mcmeta"] = meta
            textures.append(entry)
    tx.assign_groups(textures, owners)
    textures.sort(key=lambda e: e["id"])
    return {"schema": 1, "jar": jar.name, "modules": ["minecraft"], "textures": textures}, writes


def write_vanilla(jar: Path, wiki: Path = WIKI) -> int:
    index, writes = build_vanilla(jar)
    root = wiki / VANILLA_DIR
    for file, payload in writes.items():
        path = wiki / file
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists() or path.read_bytes() != payload:
            path.write_bytes(payload)
    if root.is_dir():  # files of an older jar
        for path in sorted(root.rglob("*"), reverse=True):
            if path.is_file() and path.relative_to(wiki).as_posix() not in writes:
                path.unlink()
            elif path.is_dir() and not any(path.iterdir()):
                path.rmdir()
    text = tx.render_js(index, MARKER).replace("wiki/textures.py", "wiki/vanilla_textures.py - dev only, never committed")
    (wiki / DATA_FILE).write_text(text, encoding="utf-8", newline="\n")
    return len(index["textures"])


# extra asset roots of the Resolver: the 26.3 overlay (simplebuilding woods, cauldrons, hollow logs) and the
# Vanilla-derived textures --derived just wrote (build/vanilla-derived, never committed)
EXTRA_ROOTS = ("mc26_3/overlay/resources/assets", "mc26_3/generated/assets", "build/vanilla-derived/resources/assets")
DEVTEX_DIR = "assets/devtex"   # module textures the wiki folder has no copy of (git-ignored, dev only)


def _wiki_path(icon: str, repo: Path, wiki: Path, roots: list[str]) -> str | None:
    """Resolver path (wiki/..., vanilla/..., modtex/<ns>/...) -> path the wiki serves; module textures without a wiki
    copy are copied to assets/devtex/ (own pixels of the mod, never Mojang's)."""
    if icon.startswith("wiki/"):
        return icon[len("wiki/"):]
    if icon.startswith("vanilla/"):
        return f"{VANILLA_DIR}/{icon[len('vanilla/'):]}"
    if icon.startswith("modtex/"):
        ns, _, rest = icon[len("modtex/"):].partition("/")
        for rel in (f"assets/textures/{ns}/{rest}",) + ((f"assets/textures/{rest}",) if ns == "simplebuilding" else ()):
            if (wiki / rel).is_file():
                return rel
        for root in roots:
            src = repo / root / ns / "textures" / rest
            if src.is_file():
                rel = f"{DEVTEX_DIR}/{ns}/{rest}"
                dst = wiki / rel
                dst.parent.mkdir(parents=True, exist_ok=True)
                data = src.read_bytes()
                if not dst.exists() or dst.read_bytes() != data:
                    dst.write_bytes(data)
                return rel
    return None


def testcentre_ids(wiki: Path = WIKI) -> list[str]:
    """Ids of the test centre layouts (cells and octets on top) without a module picture the wiki folder really has
    (index.json names files of the generating machine; Vanilla-derived ones are missing in a plain checkout)."""
    base = wiki / "data" / "testcentre"
    try:
        own = json.loads((base / "index.json").read_text(encoding="utf-8")).get("textures", {})
    except (OSError, ValueError):
        own = {}
    ids: set[str] = set()
    for path in sorted(base.glob("layout_*.json")):
        try:
            layout = json.loads(path.read_text(encoding="utf-8"))
        except ValueError:
            continue
        for row in layout.get("rows", []):
            ids.update(c for c in row.get("cells", []) if isinstance(c, str))
            ids.update(c for c in (row.get("tops") or {}).values() if isinstance(c, str))
    return sorted(i for i in ids if i and not i.startswith("#") and not (own.get(i) and (wiki / own[i]).is_file()))


def _state_models(state: dict) -> list[str]:
    """Model ids of a blockstate file in file order (variants first, then multipart)."""
    out: list[str] = []

    def add(node):
        for one in node if isinstance(node, list) else [node]:
            if isinstance(one, dict) and isinstance(one.get("model"), str) and one["model"] not in out:
                out.append(one["model"])
    for variant in (state.get("variants") or {}).values() if isinstance(state.get("variants"), dict) else ():
        add(variant)
    for part in state.get("multipart") or [] if isinstance(state.get("multipart"), list) else ():
        if isinstance(part, dict):
            add(part.get("apply"))
    return out


HEAD_SUFFIXES = ("_head", "_skull")
# heads of mobs without a spawn egg: a related egg, or the face of a 64x64 humanoid skin ("file#x,y,w,h": the editor
# crops it in the browser from the dev copy - no pixels are written)
HEAD_ALIASES = {"giant": "zombie", "dragon": "ender_dragon", "giant_cow": "cow", "crowned_pig": "pig", "illusioner": "evoker", "abyssal_wither": "wither",
                "blighted_wither": "wither"}
HEAD_FACES = {"herobrine": "entity/player/wide/steve", "player": "entity/player/wide/steve", "giant": "entity/zombie/zombie"}


def build_icons(jar: Path, repo: Path = REPO, wiki: Path = WIKI) -> dict:
    """{"schema": 1, "icons": {id: wiki path}} - Vanilla items of the jar plus the test centre ids (see --icons).
    Order per id: the game's model walk; then fallbacks by name only: waxed_X -> X, the "base" model of
    special-rendered items (chests), mob heads/skulls -> the mob's spawn egg (heads have no flat texture),
    X_wall_Y -> X_Y (wall heads, wall torches: blocks without an item of their own)."""
    sys.path.insert(0, str(REPO / "tools" / "devserver"))
    from sbdev.icons import Resolver  # noqa: E402 - stdlib only, the Zentrale's own item -> model -> texture walk
    res = Resolver(repo, jar)
    res.asset_roots += [r for r in EXTRA_ROOTS if r not in res.asset_roots]
    for module in Path(repo, "modules").glob("*/build/vanilla-derived/resources/assets"):
        res.asset_roots.append(module.relative_to(repo).as_posix())
    roots = list(res.asset_roots)
    items = sorted(f"minecraft:{n[len('assets/minecraft/items/'):-5]}" for n in res.jar_names()
                   if n.startswith("assets/minecraft/items/") and n.endswith(".json") and n.count("/") == 3)
    # pictures the wiki already has for module items (all module data files): spawn eggs for the head fallback
    own: dict[str, str] = {}
    bare: set[str] = set()  # N80: module ids without a picture (Guidebook/Questionary showed "?") - walked like the game
    for path in sorted((wiki / "data").glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except ValueError:
            continue
        for section in ("items", "blocks") if isinstance(data, dict) else ():
            for entry in data.get(section) or []:
                pic = isinstance(entry, dict) and (entry.get("icon") or entry.get("texture"))
                if isinstance(pic, str) and isinstance(entry.get("id"), str) and (wiki / pic).is_file():
                    own.setdefault(entry["id"], pic)
                elif isinstance(entry, dict) and isinstance(entry.get("id"), str) and ":" in entry["id"]:
                    bare.add(entry["id"])

    def direct(ident: str) -> str | None:
        if own.get(ident):
            return own[ident]
        try:
            got = res.resolve(ident)
        except Exception:  # noqa: BLE001 - one broken model must not stop the others
            got = None
        path = got and _wiki_path(got["icon"], repo, wiki, roots)
        if path:
            return path
        ns, _, name = ident.partition(":")
        try:
            definition = res._read_json(ns, f"items/{name}.json") or {}
        except Exception:  # noqa: BLE001
            definition = {}
        base = (definition.get("model") or {}).get("base") if isinstance(definition.get("model"), dict) else None
        if isinstance(base, str) and "skull" not in base:
            for ref in res._model_textures(base):
                tex = res._texture(ref)
                path = tex and _wiki_path(tex, repo, wiki, roots)
                if path:
                    return path
        # N80: blocks without an item of their own (fire, placed eggs, carpets of a mod ...): the first model of the blockstate
        try:
            state = res._read_json(ns, f"blockstates/{name}.json") or {}
        except Exception:  # noqa: BLE001
            state = {}
        for ref in _state_models(state):
            if "skull" in ref or ref.rpartition("/")[2].endswith("_head"):  # heads: particle soul sand, see the head fallback
                continue
            for tex in res._model_textures(ref):
                got = res._texture(tex)
                path = got and _wiki_path(got, repo, wiki, roots)
                if path:
                    return path
        return None

    eggs: dict[str, list[str]] = {}
    for ident in sorted(set(items) | set(own)):
        if ident.endswith("_spawn_egg"):
            eggs.setdefault(ident.partition(":")[2][:-len("_spawn_egg")], []).append(ident)

    def pick(full: str) -> str | None:
        ns, _, name = full.partition(":")
        path = direct(full)
        if not path and name.startswith("waxed_"):
            path = direct(f"{ns}:{name[len('waxed_'):]}") or direct(f"minecraft:{name[len('waxed_'):]}")
        # N80: Vanilla skulls: the base model gives the whole skin (or the particle soul sand) - spawn egg first, else the face
        skin = path if path and "/entity/" in path and "#" not in path else None
        if (not path or skin or path.endswith("/soul_sand.png")) and name.endswith(HEAD_SUFFIXES):
            mob = name.rsplit("_", 1)[0]
            path = None
            for egg in sorted(eggs.get(mob, []) + eggs.get(HEAD_ALIASES.get(mob, ""), []),
                              key=lambda e: (not e.startswith("minecraft:"), e)):
                path = direct(egg)
                if path:
                    break
            if not path and skin:
                path = skin + "#8,8,8,8"  # the face of the skin
            face = HEAD_FACES.get(mob)
            if not path and face and f"assets/minecraft/textures/{face}.png" in res.jar_names():
                path = f"{VANILLA_DIR}/{face}.png#8,8,8,8"
        if not path and "_wall_" in name:  # N80: wall_head/wall_skull/wall_torch blocks show their standing item
            path = pick(f"{ns}:{name.replace('_wall_', '_', 1)}")
        return path

    icons: dict[str, str] = {}
    for ident in sorted(set(items) | set(testcentre_ids(wiki)) | (bare - set(own))):
        path = pick(ident if ":" in ident else "minecraft:" + ident)
        if path:
            icons[ident] = path
    return {"schema": 1, "jar": jar.name, "icons": icons}


def write_icons(jar: Path, repo: Path = REPO, wiki: Path = WIKI) -> int:
    data = build_icons(jar, repo, wiki)
    (wiki / ICONS_FILE).parent.mkdir(parents=True, exist_ok=True)
    (wiki / ICONS_FILE).write_text(json.dumps(data, sort_keys=True, separators=(",", ":")), encoding="utf-8", newline="\n")
    return len(data["icons"])


# N97: the jars of the green gates the web host already keeps (sb-web-sync: "sb-dist tar" -> /srv/zentrale/downloads).
DIST_INDEX = "/srv/zentrale/downloads/index.json"


def dist_textures(index_file: Path, wanted: set[str]) -> dict[str, bytes]:
    """{"assets/<ns>/textures/<rel>": png} for the wanted names, read from the published 26.3 mod jars of index.json
    (newest build first, nested META-INF/jars/*.jar too). The gate builds them with the client jar and Pillow, so they
    hold the real Vanilla-derived pixels; copied only on the dev host (git-ignored files), never into the repository."""
    import io
    try:
        index = json.loads(Path(index_file).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    builds = sorted((b for b in index.get("builds") or [] if isinstance(b, dict)),
                    key=lambda b: str(b.get("date", "")), reverse=True)
    found: dict[str, bytes] = {}

    def scan(archive: zipfile.ZipFile) -> None:
        for name in archive.namelist():
            if name in wanted and name not in found:
                found[name] = archive.read(name)
            elif name.startswith("META-INF/jars/") and name.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as inner:
                    scan(inner)
    for build in builds:
        files = [f for f in build.get("files") or [] if isinstance(f, dict) and f.get("minecraft") == "26.3" and f.get("url")]
        for f in sorted(files, key=lambda f: f.get("loader") != "fabric"):
            try:
                with zipfile.ZipFile(Path(index_file).parent / f["url"]) as archive:
                    scan(archive)
            except (OSError, zipfile.BadZipFile):
                continue
            if len(found) == len(wanted):
                return found
    return found


def fill_from_dist(out_root: Path, index_file: Path) -> tuple[int, list[str]]:
    """Replaces the placeholders vanilla_derived wrote under out_root (marker file per namespace: no Pillow/numpy or
    no jar) with the textures of the published jars; returns (replaced, names still placeholders)."""
    import vanilla_derived
    left: dict[str, Path] = {}
    for module in [None] + list(vanilla_derived.MODULE_OUTPUTS):
        ns = module or vanilla_derived.NS
        if not (out_root / "assets" / ns / vanilla_derived.MARKER).exists():
            continue  # drawn from the client jar: real pixels already
        for rel in (vanilla_derived.MODULE_OUTPUTS[module] if module else vanilla_derived.OUTPUTS):
            left[f"assets/{ns}/textures/{rel}"] = out_root / "assets" / ns / "textures" / rel
    if not left:
        return 0, []
    got = dist_textures(index_file, set(left))
    for name, data in got.items():
        path = left[name]
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.is_file() or path.read_bytes() != data:
            path.write_bytes(data)
    return len(got), sorted(set(left) - set(got))


PRESENT_FILE = "data/derived-present.json"


def write_present(files: set[str], mode: str, wiki: Path = WIKI) -> None:
    """N113: the git-ignored pictures this host really has (wiki/data/local-only.js names all of them); the page
    (assets/module-loader.js) asks only for these. Ignored by git (wiki/data/.gitignore)."""
    path = wiki / PRESENT_FILE
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps({"schema": 1, "mode": mode, "files": sorted(files)}, separators=(",", ":")) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != text:
        tmp = path.with_name(path.name + ".tmp")
        tmp.write_text(text, encoding="utf-8", newline="\n")
        os.replace(tmp, path)


def derived_entries(repo: Path = REPO, wiki: Path = WIKI) -> list[tuple[str, str]]:
    """(source below the derived layer, wiki file) of every Vanilla-derived texture textures.js names that git does
    not track (a tracked file is in every checkout and is never overwritten)."""
    index = tx.read_js(wiki / tx.DATA_FILE) or {}
    tracked = tx.claimed_copies(wiki, []) or set()  # what git tracks under assets/textures: never overwritten
    out = []
    for entry in index.get("textures", []):
        source = entry.get("source", "")
        if not source.startswith(tx.SB_LAYERS[-1] + "/"):
            continue
        if not str(entry["file"]).startswith("assets/textures/") or entry["file"] in tracked:
            continue
        out.append((source, entry["file"]))
    return out


def write_published(repo: Path = REPO, wiki: Path = WIKI, index_file: Path | None = None) -> tuple[int, int]:
    """N113, production host: only the Vanilla-derived textures of the published jars (index.json of the green gates,
    the same files every player downloads) - no client jar, no placeholders. Writes the present list; returns
    (written, not in a published jar)."""
    entries = derived_entries(repo, wiki)
    prefix = tx.SB_LAYERS[-1] + "/"
    wanted = {source[len(prefix):] for source, _ in entries}  # "assets/<ns>/textures/<rel>"
    got = dist_textures(Path(index_file or os.environ.get("SB_DIST_INDEX", DIST_INDEX)), wanted)
    present = set()
    for source, file in entries:
        data = got.get(source[len(prefix):])
        if data is None:
            continue
        dst = wiki / file
        dst.parent.mkdir(parents=True, exist_ok=True)
        if not dst.exists() or dst.read_bytes() != data:
            dst.write_bytes(data)
        present.add(file)
    write_present(present, "published", wiki)
    return len(present), len(entries) - len(present)


def write_derived(repo: Path = REPO, wiki: Path = WIKI) -> tuple[int, str]:
    """Copy the Vanilla-derived textures to the wiki files textures.js names for them; returns (count, jar|placeholder)."""
    import vanilla_derived
    try:
        _, mode = vanilla_derived.ensure_local()
    except Exception as e:  # noqa: BLE001 - a generator without numpy etc.: labelled placeholders, as without a jar
        print(f"vanilla_derived failed ({type(e).__name__}: {e}) - placeholders")
        mode = vanilla_derived.write(vanilla_derived.LOCAL_OUT, None, placeholders=True)
        for module in vanilla_derived.MODULE_OUTPUTS:
            vanilla_derived.write(vanilla_derived.LOCAL_OUT, None, placeholders=True, module=module)
    # N97: the web host has no Pillow/numpy -> placeholders (checkerboards in the TZ editor and on Blocks & items);
    # take the real textures from the jars of the green gates it already has.
    filled, left = fill_from_dist(vanilla_derived.LOCAL_OUT, Path(os.environ.get("SB_DIST_INDEX", DIST_INDEX)))
    if filled or left:
        mode += f", {filled} from the published jars"
        if left:
            print(f"Vanilla-derived placeholders left ({len(left)}, not in a published jar yet): " + ", ".join(left))
            mode += f", {len(left)} placeholders left"
    done = 0
    present = set()
    for source, file in derived_entries(repo, wiki):
        src, dst = repo / source, wiki / file
        if not src.is_file():
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        data = src.read_bytes()
        if not dst.exists() or dst.read_bytes() != data:
            dst.write_bytes(data)
        present.add(file)
        done += 1
    # N113: 3D renders (assets/textures/render/) need Pillow and the models - present only where generate.py drew them.
    present.update(p for p in local_only_pictures(wiki) if "/render/" in p and (wiki / p).is_file())
    write_present(present, "dev", wiki)
    return done, mode


def local_only_pictures(wiki: Path = WIKI) -> set[str]:
    import local_only
    return local_only.flat(local_only.read(wiki))


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--jar", type=Path, help="26.3 client jar (default: SB_CLIENT_JAR or the Gradle cache)")
    p.add_argument("--vanilla", action="store_true", help="only the Vanilla index and files")
    p.add_argument("--icons", action="store_true", help="only the id -> picture list of the test centre editor")
    p.add_argument("--derived", action="store_true", help="only the Vanilla-derived copies")
    p.add_argument("--published-only", action="store_true",
                   help="production host (N113): only the Vanilla-derived textures of the published jars, no client jar, "
                        "no placeholders, nothing else")
    a = p.parse_args(argv)
    if a.published_only:
        n, left = write_published()
        print(f"Vanilla-derived copies (published jars only): {n} wiki files, {left} not in a published jar -> "
              f"wiki/{PRESENT_FILE}")
        return 0
    both = not a.vanilla and not a.derived and not a.icons
    code = 0
    jar = None
    if a.vanilla or a.icons or both:
        jar = a.jar or default_jar()
        if jar is None or not jar.is_file():
            print("vanilla_textures: no 26.3 client jar - Vanilla category and id pictures skipped")
            code = 1 if (a.vanilla or a.icons) else 0
        elif a.vanilla or both:
            print(f"Vanilla textures (dev only): {write_vanilla(jar)} from {jar.name} -> wiki/{VANILLA_DIR}/, wiki/{DATA_FILE}")
    if a.derived or both:
        n, mode = write_derived()
        print(f"Vanilla-derived copies: {n} wiki files ({mode})")
    if (a.icons or both) and jar is not None and jar.is_file():  # after --derived: those copies are pictures too
        print(f"Id pictures (dev only): {write_icons(jar)} ids -> wiki/{ICONS_FILE}")
    return code


if __name__ == "__main__":
    sys.exit(main())
