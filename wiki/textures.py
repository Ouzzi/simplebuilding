"""
Texture index of the wiki page "Textures" (?tab=textures).

Walks every texture of every module (assets/<ns>/textures/**/*.png), reads size and
.mcmeta animation from the files themselves and writes wiki/data/textures.js. Nothing is
kept by hand: generate.py calls sync() on its default run and on --all.

Images: a byte-identical copy that the wiki already holds under assets/textures/ (item and
block icons) is reused; everything else is copied to assets/textures/sheets/<module>/<ns>/.
Copies nothing references any more are removed.

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
SB_LAYERS = ("src/main/resources", "mc26_3/overlay/resources")


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
    return re.sub(r"[_\-]+", " ", stem).strip().title() or stem


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


def existing_copies(wiki: Path) -> dict[bytes, str]:
    """Byte content -> first wiki path (sorted) of the own textures outside sheets/ and minecraft/."""
    copies: dict[bytes, str] = {}
    base = wiki / "assets" / "textures"
    if not base.is_dir():
        return copies
    for png in sorted(base.rglob("*.png")):
        relative = png.relative_to(wiki).as_posix()
        if relative.startswith((SHEETS + "/", "assets/textures/minecraft/")):
            continue
        copies.setdefault(png.read_bytes(), relative)
    return copies


def build(repo: Path, wiki: Path, entries: list[dict]) -> tuple[dict, dict[str, bytes]]:
    """The index and the sheet copies it needs (wiki path -> bytes)."""
    copies = existing_copies(wiki)
    by_texture, by_id = wiki_entries(wiki, entries)
    dates = git_dates(repo)
    writes: dict[str, bytes] = {}
    textures = []
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
        if link:
            entry["name"] = {"en": link["name"].get("en_us") or pretty(stem),
                             "de": link["name"].get("de_de") or link["name"].get("en_us") or pretty(stem)}
            entry["item"] = link["id"]
            entry["itemMod"] = link["mod"]
            if link["icon"] and link["icon"] != file:
                entry["icon"] = link["icon"]
        if anim:
            entry["anim"] = anim
        if meta is not None:
            entry["mcmeta"] = meta
        if source in dates:
            entry["modified"] = dates[source]
        textures.append(entry)
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
        for file, payload in writes.items():
            path = wiki / file
            if not path.exists() or path.read_bytes() != payload:
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
