"""
Texture index of the wiki page "Textures" (?tab=textures).

Walks every texture of every module (assets/<ns>/textures/**/*.png), reads size and
.mcmeta animation from the files themselves and writes wiki/data/textures.js. Nothing is
kept by hand: generate.py calls sync() on its default run and on --all.

Images: a byte-identical copy that the wiki already holds under assets/textures/ (item and
block icons) is reused; everything else is copied to assets/textures/sheets/<module>/<ns>/.
Copies nothing references any more are removed. A layer source next to a PNG (<name>.layers.json,
tools/textures/layers.py) is copied beside the sheets and named in the entry as "layers" (wiki pixel editor).

UI screenshots (--ui-shots DIR) land in assets/ui/ and data/ui-shots.js. Both are ignored
by git and not published: they are local client-test output.
"""
from __future__ import annotations

import json
import re
import shutil
import struct
import subprocess
from pathlib import Path

SHEETS = "assets/textures/sheets"
UI_DIR = "assets/ui"
DATA_FILE = "data/textures.js"
UI_DATA_FILE = "data/ui-shots.js"
MARKER = "window.WIKI_TEXTURES = "
UI_MARKER = "window.WIKI_UI_SHOTS = "
# Layers of SimpleBuilding on the 26.3 line, later wins per path (mc26_3/resources.gradle).
# build/vanilla-derived/resources: the Vanilla-derived textures (never committed, tools/textures/vanilla_derived.py;
# generate.py writes them, or placeholders without the client jar, before this runs).
SB_LAYERS = ("src/main/resources", "mc26_3/overlay/resources", "build/vanilla-derived/resources")


def png_size(data: bytes) -> tuple[int, int] | None:
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", data[16:24])


def animation(meta: dict | None, width: int, height: int) -> dict | None:
    """Frame grid of an animated texture as the game cuts it, or None when it is a still."""
    anim = meta.get("animation") if isinstance(meta, dict) else None
    if not isinstance(anim, dict):
        return None
    side = min(width, height)
    fw = int(anim.get("width") or anim.get("height") or side)
    fh = int(anim.get("height") or anim.get("width") or side)
    if fw <= 0 or fh <= 0 or width % fw or height % fh:
        return None
    cols, count = width // fw, (width // fw) * (height // fh)
    default = int(anim.get("frametime", 1) or 1)
    order = []
    for frame in anim.get("frames") or range(count):
        if isinstance(frame, dict):
            order.append([int(frame.get("index", 0)), int(frame.get("time", default))])
        else:
            order.append([int(frame), default])
    result = {"w": fw, "h": fh, "cols": cols, "count": count, "frametime": default}
    if order != [[i, default] for i in range(count)]:
        result["order"] = order
    if anim.get("interpolate"):
        result["interpolate"] = True
    return result


def texture_roots(repo: Path, entries: list[dict]) -> list[tuple[str, list[Path]]]:
    """(module id, resource layers, earliest first) for every manifest module."""
    roots = []
    for entry in entries:
        mid, paths = entry["id"], entry["paths"]
        if mid == "simplebuilding":
            layers = [repo / layer for layer in SB_LAYERS]
        else:
            layers = [repo / paths["shared"] / "resources"]
            layers += [repo / paths[loader] / "src/main/resources" for loader in entry["loaders"] if paths.get(loader)]
            layers += [repo / paths["generated"]] if paths.get("generated") else []
            layers.reverse()  # wiki/modules.py: the first layer that has a file wins
        roots.append((mid, layers))
    return roots


def collect_sources(repo: Path, entries: list[dict]) -> list[dict]:
    """Every texture file once per module and id: {module, ns, path (in textures/), source}."""
    found = []
    for mid, layers in texture_roots(repo, entries):
        chosen: dict[tuple[str, str], Path] = {}
        for layer in layers:
            assets = layer / "assets"
            if not assets.is_dir():
                continue
            for png in sorted(assets.glob("*/textures/**/*.png")):
                ns = png.relative_to(assets).parts[0]
                inner = png.relative_to(assets / ns / "textures").as_posix()
                chosen[(ns, inner)] = png
        for (ns, inner), png in sorted(chosen.items()):
            found.append({"module": mid, "ns": ns, "path": inner, "source": png})
    return found


def git_dates(repo: Path) -> dict[str, str]:
    """Repo path -> date of the last commit that touched it (one git log over the textures)."""
    try:
        out = subprocess.run(["git", "-C", str(repo), "log", "--format=%x00%cs", "--name-only", "--",
                              "*.png", "*.png.mcmeta"], capture_output=True, text=True, timeout=60).stdout
    except (OSError, subprocess.SubprocessError):
        return {}
    dates: dict[str, str] = {}
    current = ""
    for line in out.splitlines():
        if line.startswith("\x00"):
            current = line[1:]
        elif line and line not in dates:
            dates[line] = current
    return dates


def pretty(stem: str) -> str:
    return re.sub(r"[_\-]+", " ", stem.replace("light_pane", "pane")).strip().title() or stem


def wiki_entries(wiki: Path, entries: list[dict]) -> tuple[dict, dict]:
    """Items/blocks of the generated wiki data: by texture file and by id."""
    by_texture, by_id = {}, {}
    for entry in entries:
        path = wiki / "data" / (entry["id"] + ".json")
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for section in ("items", "blocks"):
            for item in data.get(section, []):
                if not isinstance(item, dict) or "id" not in item:
                    continue
                record = {"id": item["id"], "mod": entry["id"], "section": section,
                          "name": item.get("name") or {}, "icon": item.get("icon") or item.get("texture")}
                by_id.setdefault(item["id"], record)
                if item.get("texture"):
                    by_texture.setdefault(item["texture"], record)
    return by_texture, by_id


def claimed_copies(wiki: Path, entries: list[dict]) -> set[str] | None:
    """
    Wiki paths of the copies the index may reuse: what git tracks plus what the generated wiki data
    names (texture/icon of an item or block; generate.py writes those on every run). Anything else
    under assets/textures is a leftover of an older checkout (ignored by git, e.g. a Vanilla-derived
    copy of a texture that has since been renamed or merged) and must not change the index, or the same
    commit would give a different textures.js in a long-lived gate worktree than in a fresh one.
    None when git is unavailable (no repository): then every file counts.
    """
    try:
        done = subprocess.run(["git", "-C", str(wiki), "ls-files", "-z", "--", "assets/textures"],
                              capture_output=True, timeout=60)
    except (OSError, subprocess.SubprocessError):
        return None
    if done.returncode != 0:
        return None
    claimed = {p for p in done.stdout.decode("utf-8", "replace").split("\0") if p}
    for entry in entries:
        path = wiki / "data" / (entry["id"] + ".json")
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for section in ("items", "blocks"):
            for item in data.get(section, []):
                if isinstance(item, dict):
                    claimed.update(v for v in (item.get("texture"), item.get("icon")) if isinstance(v, str))
    return claimed


def existing_copies(wiki: Path, claimed: set[str] | None = None) -> dict[bytes, str]:
    """Byte content -> first wiki path (sorted) of the own textures outside sheets/ and minecraft/ (only claimed ones)."""
    copies: dict[bytes, str] = {}
    base = wiki / "assets" / "textures"
    if not base.is_dir():
        return copies
    for png in sorted(base.rglob("*.png")):
        relative = png.relative_to(wiki).as_posix()
        if relative.startswith((SHEETS + "/", "assets/textures/minecraft/")):
            continue
        if claimed is not None and relative not in claimed:
            continue
        copies.setdefault(png.read_bytes(), relative)
    return copies


# --- Grouping (one card per block/item) and categories --------------------------------------
# Suffixes that only name a variant (side, state, stage, overlay) of the same object.
VARIANT_RE = re.compile(
    r"(?:_(?:top|bottom|side|sides|front|back|left|right|inside|outside|inner|outer|end|on|off|open|closed|lit|unlit|"
    r"active|inactive|powered|unpowered|full|empty|half|base|overlay|inventory|gui|ground|item|block|particle|"
    r"stage\d+|frame\d*|layer\d*|phase\d+|age\d+|level\d+|state\d+|\d+|[a-z]\d|north|south|east|west|up|down|"
    r"emissive|glow|mask|small|large|cracked|wet|dry|filled|plus|minus|mid|middle|head|foot|post|tip|bud|flower))+$")


def stem_of(path: str) -> str:
    return path.rsplit("/", 1)[-1].removesuffix(".png")


def base_stem(stem: str) -> str:
    """Name stem without variant suffixes (oven_front_on -> oven); never empty."""
    return VARIANT_RE.sub("", stem) or stem


def _refs(value, out: list) -> None:
    if isinstance(value, dict):
        for k, v in value.items():
            if k == "model" and isinstance(v, str):
                out.append(v)
            else:
                _refs(v, out)
    elif isinstance(value, list):
        for v in value:
            _refs(v, out)


def _rid(ns: str, ref: str) -> tuple[str, str]:
    if ":" in ref:
        a, b = ref.split(":", 1)
        return a, b
    return ns, ref


def model_owners(layers: list[Path]) -> dict[str, set[str]]:
    """texture id (ns:path) -> owner names (ns:name) taken from blockstates, item definitions and models."""
    models: dict[tuple[str, str], dict] = {}
    owners_of: dict[tuple[str, str], set[str]] = {}
    for layer in layers:
        assets = layer / "assets"
        if not assets.is_dir():
            continue
        for js in assets.glob("*/models/**/*.json"):
            ns = js.relative_to(assets).parts[0]
            rel = js.relative_to(assets / ns / "models").with_suffix("").as_posix()
            try:
                models[(ns, rel)] = json.loads(js.read_text(encoding="utf-8"))
            except ValueError:
                continue
        for folder in ("blockstates", "items"):
            for js in assets.glob(f"*/{folder}/**/*.json"):
                ns = js.relative_to(assets).parts[0]
                name = js.relative_to(assets / ns / folder).with_suffix("").as_posix()
                try:
                    data = json.loads(js.read_text(encoding="utf-8"))
                except ValueError:
                    continue
                refs: list[str] = []
                _refs(data, refs)
                for ref in refs:
                    owners_of.setdefault(_rid(ns, ref), set()).add(f"{ns}:{name}")
    result: dict[str, set[str]] = {}

    def textures_of(key, seen=()):
        model = models.get(key)
        if not model or key in seen:
            return {}
        found = {}
        parent = model.get("parent")
        if isinstance(parent, str):
            found.update(textures_of(_rid(key[0], parent), seen + (key,)))
        for k, v in (model.get("textures") or {}).items():
            if isinstance(v, str) and not v.startswith("#"):
                found[k] = v
        return found

    for key in models:
        ns, rel = key
        own = owners_of.get(key) or ({f"{ns}:{rel.split('/', 1)[-1]}"} if rel.split("/", 1)[0] in ("block", "item") else set())
        for ref in textures_of(key).values():
            tns, tpath = _rid(ns, ref)
            for o in own:
                result.setdefault(f"{tns}:{tpath}", set()).add(o)
    return result


def group_key(ns: str, path: str, kind: str, owners: set[str]) -> str:
    """Card key of a texture: the owning block/item of the models, else the name stem."""
    stem = stem_of(path)
    if kind in ("block", "item"):
        names = {o.split(":", 1)[1] for o in owners if o.split(":", 1)[0] == ns}
        base = base_stem(stem)
        for cand in (stem, base):
            if cand in names:
                return f"{ns}:{cand}"
        if len(names) == 1:
            return f"{ns}:{next(iter(names))}"
        return f"{ns}:{base}"
    parts = path.split("/")
    if kind == "entity" and len(parts) > 2:
        return f"{ns}:entity/{parts[1]}"
    if kind in ("entity", "particle"):
        return f"{ns}:{kind}/{base_stem(stem)}"
    return f"{ns}:{path[:-4]}"


def role_of(group: str, path: str, kind: str) -> str:
    stem = stem_of(path)
    name = group.split(":", 1)[1].rsplit("/", 1)[-1]
    if stem == name:
        return ""
    if stem.startswith(name + "_"):
        return stem[len(name) + 1:]
    if stem.endswith("_" + name):
        return stem[:-len(name) - 1]
    return stem


CATEGORY_RULES = [
    ("block_wood", re.compile(r"plank|(?:^|_)logs?(?:_|$)|wood|stem|hyphae|bamboo|sign|door|trapdoor|fence|barrel|bookshelf|ladder|crafting|cauldron|chest|boat|beam")),
    ("block_ore", re.compile(r"(?:^|_)ore(?:s|_|$)|(?:^|_)deepslate_|raw_|ingot|nugget|_block$|ancient|netherite|diamond|emerald|gold|iron|copper|lapis|amethyst|crystal|obsidian|enderit")),
    ("block_machine", re.compile(r"redstone|piston|lever|button|pressure|lamp|hopper|dispenser|dropper|furnace|oven|repeater|comparator|observer|rail|machine|crucible|smither|anvil|press|grinder|pad|wire|sensor|detector|beacon|light|torch|lantern|jukebox|speaker|container|storage|shop|trade|spawner|portal|teleport")),
    ("block_plant", re.compile(r"sapling|flower|leaves|grass|vine|crop|mushroom|fern|bush|seed|wheat|carrot|potato|beet|berry|moss|lily|kelp|coral|bud|plant|cactus|dripleaf|azalea|rose|tulip|stage")),
    ("block_deco", re.compile(r"carpet|bed|banner|glass|pane|candle|pot|painting|frame|statue|head|skull|rug|cushion|chair|table|shelf|sofa|vase|flag|trophy|clock|mirror|curtain|hammock|cage|lattice|plaque")),
]
ITEM_RULES = [
    ("item_armor", re.compile(r"helmet|chestplate|leggings|boots|armor|shield|trim|cape|elytra|backpack")),
    ("item_tool", re.compile(r"pickaxe|axe|shovel|hoe|sword|hammer|sickle|saw|shears|bow|crossbow|arrow|trident|spear|rod|wand|staff|bucket|compass|map|tool|wrench|key|flint|brush|lead|saddle|sledge")),
    ("item_food", re.compile(r"apple|bread|sandwich|cake|cookie|pie|stew|soup|meat|beef|pork|chicken|mutton|fish|salmon|cod|carrot|potato|berry|berries|food|steak|cheese|honey|milk|juice|potion|bottle|melon|pumpkin|egg|drink|candy|jam|burger")),
    ("item_material", re.compile(r"ingot|nugget|dust|powder|shard|fragment|scrap|raw_|gem|pearl|core|essence|dye|string|paper|fabric|fiber|fibre|plate|rod$|stick|coal|clay|brick|pressling|ash|salt|slime|leather|feather|bone|flake|heart|crystal|amber")),
]


def categorize(kind: str, key: str, is_block: bool) -> str:
    """Category of a card from the pack path (kind) and the name; no hand-kept list."""
    name = key.split(":", 1)[1].rsplit("/", 1)[-1]
    if kind == "gui":
        return "gui"
    if kind in ("entity", "models", "painting"):
        return "entity"
    if kind in ("particle", "mob_effect"):
        return "particle"
    if kind in ("block", "item"):
        if is_block:
            for cat, rx in CATEGORY_RULES:
                if rx.search(name):
                    return cat
            return "block_build"
        for cat, rx in ITEM_RULES:
            if rx.search(name):
                return cat
        return "item_misc"
    return "other"


def assign_groups(textures: list[dict], owners: dict[str, set[str]]) -> None:
    """Adds "group", "role" and "cat" to every entry (in place); one card per group in the wiki."""
    groups: dict[tuple[str, str], list[dict]] = {}
    for e in textures:
        ns, path = e["id"].split(":", 1)
        key = group_key(ns, path + ".png", e["kind"], owners.get(e["id"], set()))
        e["group"] = key
        groups.setdefault((e["module"], key), []).append(e)
    for (module, key), members in groups.items():
        is_block = any(m["kind"] == "block" for m in members) or any(
            m.get("itemSection") == "blocks" for m in members)
        kinds = [m["kind"] for m in members]
        kind = "block" if "block" in kinds else "item" if "item" in kinds else kinds[0]
        cat = categorize(kind, key, is_block)
        for m in members:
            ns, path = m["id"].split(":", 1)
            m["role"] = role_of(key, path + ".png", m["kind"])
            m["cat"] = cat
            m.pop("itemSection", None)


def build(repo: Path, wiki: Path, entries: list[dict]) -> tuple[dict, dict[str, bytes]]:
    """The index and the sheet copies it needs (wiki path -> bytes)."""
    copies = existing_copies(wiki, claimed_copies(wiki, entries))
    by_texture, by_id = wiki_entries(wiki, entries)
    dates = git_dates(repo)
    writes: dict[str, bytes] = {}
    textures = []
    owners: dict[str, set[str]] = {}
    for mid, layers in texture_roots(repo, entries):
        for tid, names in model_owners(layers).items():
            owners.setdefault(tid, set()).update(names)
    for src in collect_sources(repo, entries):
        payload = src["source"].read_bytes()
        size = png_size(payload)
        if not size:
            continue
        width, height = size
        if payload in copies:
            file = copies[payload]
        else:
            file = f"{SHEETS}/{src['module']}/{src['ns']}/{src['path']}"
            writes[file] = payload
        meta_path = src["source"].with_name(src["source"].name + ".mcmeta")
        meta = None
        if meta_path.exists():
            try:
                meta = json.loads(meta_path.read_text(encoding="utf-8"))
            except ValueError:
                meta = {"invalid": True}
        anim = animation(meta, width, height)
        stem = src["path"].rsplit("/", 1)[-1][:-4]
        kind = src["path"].split("/", 1)[0] if "/" in src["path"] else "other"
        texture_id = f"{src['ns']}:{src['path'][:-4]}"
        source = src["source"].relative_to(repo).as_posix()
        link = by_texture.get(file) or (by_id.get(f"{src['ns']}:{stem}") if kind in ("item", "block") else None)
        entry = {"id": texture_id, "module": src["module"], "kind": kind,
                 "name": {"en": pretty(stem), "de": pretty(stem)},
                 "file": file, "source": source, "w": width, "h": height, "bytes": len(payload)}
        if source.startswith(SB_LAYERS[-1] + "/"):
            del entry["bytes"]  # built from the client jar or a placeholder: the size is not the same everywhere
        if link:
            entry["name"] = {"en": link["name"].get("en_us") or pretty(stem),
                             "de": link["name"].get("de_de") or link["name"].get("en_us") or pretty(stem)}
            entry["item"] = link["id"]
            entry["itemMod"] = link["mod"]
            entry["itemSection"] = link["section"]
            if link["icon"] and link["icon"] != file:
                entry["icon"] = link["icon"]
        layer_src = src["source"].with_name(stem + ".layers.json")
        if layer_src.is_file():
            # layer source of the pixel editor (tools/textures/layers.py, PRINZIPIEN A8 b): copied beside the sheets
            entry["layers"] = f"{SHEETS}/{src['module']}/{src['ns']}/{src['path'][:-4]}.layers.json"
            writes[entry["layers"]] = layer_src.read_bytes()
        if anim:
            entry["anim"] = anim
        if meta is not None:
            entry["mcmeta"] = meta
        if source in dates:
            entry["modified"] = dates[source]
        textures.append(entry)
    assign_groups(textures, owners)
    order = {e["id"]: i for i, e in enumerate(entries)}
    textures.sort(key=lambda e: (order.get(e["module"], 99), e["id"]))
    return {"schema": 1, "modules": [e["id"] for e in entries], "textures": textures}, writes


def render_js(index: dict, marker: str = MARKER) -> str:
    """One texture per line, so parallel branches touch different lines."""
    head = {k: v for k, v in index.items() if k != "textures"}
    lines = [json.dumps(t, ensure_ascii=False, sort_keys=True) for t in index.get("textures", [])]
    body = json.dumps(head, ensure_ascii=False)[:-1] + ', "textures": [\n' + ",\n".join(lines) + "\n]}"
    return ("// Generated by wiki/generate.py (wiki/textures.py) - do not edit.\n"
            + marker + body + ";\n")


def read_js(path: Path, marker: str = MARKER) -> dict | None:
    try:
        return json.loads(path.read_text(encoding="utf-8").split(marker, 1)[1].rstrip().rstrip(";"))
    except (OSError, ValueError, IndexError):
        return None


def without_dates(index: dict | None) -> dict | None:
    if index is None:
        return None
    return dict(index, textures=[{k: v for k, v in t.items() if k != "modified"} for t in index.get("textures", [])])


def sync(repo: Path, wiki: Path, entries: list[dict], check: bool = False) -> list[str]:
    """Write (or with check: compare) the index and the sheet copies. Returns problems."""
    index, writes = build(repo, wiki, entries)
    target = wiki / DATA_FILE
    if check:
        problems = []
        if without_dates(read_js(target)) != without_dates(index):
            problems.append(f"wiki/{DATA_FILE} is out of date with the textures - run python wiki/generate.py")
        derived_root = repo / SB_LAYERS[-1] / "assets"
        derived = {f"{SHEETS}/simplebuilding/{png.relative_to(derived_root).parts[0]}/"
                   + png.relative_to(derived_root / png.relative_to(derived_root).parts[0] / "textures").as_posix()
                   for png in derived_root.glob("*/textures/**/*.png")} if derived_root.is_dir() else set()
        for file, payload in writes.items():
            path = wiki / file
            if not path.exists() or path.read_bytes() != payload:
                if file in derived:
                    # Vanilla-derived (ignored by git, jar or placeholder): written, not compared.
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(payload)
                else:
                    problems.append(f"wiki/{file}: missing or stale texture copy")
        return problems
    for file, payload in writes.items():
        path = wiki / file
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists() or path.read_bytes() != payload:
            path.write_bytes(payload)
    sheets = wiki / SHEETS
    if sheets.is_dir():
        for path in sorted(sheets.rglob("*"), reverse=True):
            relative = path.relative_to(wiki).as_posix()
            if path.is_file() and relative not in writes:
                path.unlink()
            elif path.is_dir() and not any(path.iterdir()):
                path.rmdir()
    text = render_js(index)
    if not target.exists() or target.read_text(encoding="utf-8") != text:
        target.write_text(text, encoding="utf-8", newline="\n")
    return []


def import_ui_shots(source: Path, wiki: Path, module_ids: list[str]) -> dict:
    """Copy the PNG screenshots under source to assets/ui/ and index them in data/ui-shots.js."""
    target = wiki / UI_DIR
    if target.exists():
        shutil.rmtree(target)
    shots = []
    for png in sorted(source.rglob("*.png")):
        relative = png.relative_to(source)
        flat = "__".join(relative.parts)
        flat = re.sub(r"[^A-Za-z0-9._-]+", "_", flat)
        payload = png.read_bytes()
        size = png_size(payload)
        if not size:
            continue
        target.mkdir(parents=True, exist_ok=True)
        (target / flat).write_bytes(payload)
        words = re.split(r"[^a-z0-9]+", relative.as_posix().lower())
        module = next((m for m in sorted(module_ids, key=len, reverse=True)
                       if m in words or any(w.startswith(m) for w in words)), "")
        shots.append({"file": f"{UI_DIR}/{flat}", "name": pretty(png.stem), "module": module,
                      "w": size[0], "h": size[1], "source": relative.as_posix()})
    index = {"schema": 1, "shots": shots}
    (wiki / UI_DATA_FILE).write_text("// Generated by wiki/generate.py --ui-shots - local only, not committed.\n"
                                     + UI_MARKER + json.dumps(index, ensure_ascii=False, indent=1) + ";\n",
                                     encoding="utf-8", newline="\n")
    return index
