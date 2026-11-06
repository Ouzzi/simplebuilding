"""
Dev-only texture assets of the wiki (owner 06.11.: Vanilla textures to work with, never public).

Two jobs, both writing only files that git ignores (.gitignore: wiki/assets/vanilla/, wiki/data/vanilla-textures.js
and the Vanilla-derived copies), so nothing of Mojang's ends up in the repository or in the production wiki:

  --vanilla  every texture of the 26.3 client jar -> wiki/assets/vanilla/<path in textures/> and the index
             wiki/data/vanilla-textures.js (window.WIKI_VANILLA_TEXTURES, same fields and cards as wiki/textures.py).
             The wiki page never loads that file itself; only the dev overlay of the Zentrale (behind the login,
             tools/zentrale-web/dev-overlay.js) does, which adds the category "Vanilla".
  --derived  the Vanilla-derived mod textures (tools/textures/vanilla_derived.py; placeholders without jar or Pillow)
             copied to the wiki paths that wiki/data/textures.js names for them. The wiki checkouts on the web host
             are plain git trees, so without this those cards had no image.

Run on the dev checkout only (tools/zentrale-web/sb-web-sync), or locally:
    python3 wiki/vanilla_textures.py [--jar client.jar] [--vanilla] [--derived]     (default: both)
    python3 wiki/generate.py --with-vanilla                                         (same, after the normal run)
"""
from __future__ import annotations

import argparse
import json
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
    index = tx.read_js(wiki / tx.DATA_FILE) or {}
    tracked = tx.claimed_copies(wiki, []) or set()  # what git tracks under assets/textures: never overwritten
    done = 0
    for entry in index.get("textures", []):
        source = entry.get("source", "")
        if not source.startswith(tx.SB_LAYERS[-1] + "/"):
            continue
        src, dst = repo / source, wiki / entry["file"]
        if not src.is_file() or not str(entry["file"]).startswith("assets/textures/") or entry["file"] in tracked:
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        data = src.read_bytes()
        if not dst.exists() or dst.read_bytes() != data:
            dst.write_bytes(data)
        done += 1
    return done, mode


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--jar", type=Path, help="26.3 client jar (default: SB_CLIENT_JAR or the Gradle cache)")
    p.add_argument("--vanilla", action="store_true", help="only the Vanilla index and files")
    p.add_argument("--derived", action="store_true", help="only the Vanilla-derived copies")
    a = p.parse_args(argv)
    both = not a.vanilla and not a.derived
    code = 0
    if a.vanilla or both:
        jar = a.jar or default_jar()
        if jar is None or not jar.is_file():
            print("vanilla_textures: no 26.3 client jar - Vanilla category skipped")
            code = 1 if a.vanilla else 0
        else:
            print(f"Vanilla textures (dev only): {write_vanilla(jar)} from {jar.name} -> wiki/{VANILLA_DIR}/, wiki/{DATA_FILE}")
    if a.derived or both:
        n, mode = write_derived()
        print(f"Vanilla-derived copies: {n} wiki files ({mode})")
    return code


if __name__ == "__main__":
    sys.exit(main())
