#!/usr/bin/env python3
"""Simple Livemap: static copy of the map for any web space (self-hosting guide docs/deploy/LIVEMAP-SELF-HOST.md).

Copies the tile pictures of a world (<world>/simplelivemap/tiles) into a folder that any static web host can serve:
  <dest>/index.html   the viewer page (from the mod jar, --jar, or an existing file, --index)
  <dest>/live.json    the dimension list (never players: a static copy has no live data)
  <dest>/tiles/...    only <dimension>/<zoom 0-4>/<x>/<y>.png, at most 1 MiB each, no links followed

Then upload <dest> with rsync, scp, SFTP or FTP (or run this on the web host itself, with the world folder mounted or
copied there). Works on Linux, macOS and Windows: Python 3.8+ standard library only.

  python3 livemap_static_sync.py --world /path/to/server/world --dest ./livemap-site --jar mods/simplelivemap-1.0.jar
  python3 livemap_static_sync.py --world "C:\\Users\\me\\AppData\\Roaming\\.minecraft\\saves\\My World" --dest site --every 300
  python3 livemap_static_sync.py ... --dry-run          only report

Idempotent: unchanged tiles are not copied again (size + mtime), tiles that are gone in the world are removed, the two
small files are rewritten only when they differ. No key is needed or written: a static copy is as public as the web
space it lies on; protect it with the web host's own login if it should not be.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
import time
import zipfile
from pathlib import Path

DIM = re.compile(r"^[a-z0-9_.-]{1,96}$")
NUM = re.compile(r"^-?\d{1,7}$")
MAX_FILE = 1 << 20
INDEX_IN_JAR = "assets/simplelivemap/web/index.html"


def tile_files(tiles: Path):
    """Relative paths of valid tile files (the same pattern the mod serves), real files only, no links."""
    if not tiles.is_dir():
        return
    for dim in sorted(tiles.iterdir()):
        if dim.is_symlink() or not dim.is_dir() or not DIM.match(dim.name):
            continue
        for zoom in sorted(dim.iterdir()):
            if zoom.is_symlink() or not zoom.is_dir() or zoom.name not in {"0", "1", "2", "3", "4"}:
                continue
            for x in sorted(zoom.iterdir()):
                if x.is_symlink() or not x.is_dir() or not NUM.match(x.name):
                    continue
                for f in sorted(x.iterdir()):
                    if f.is_symlink() or not f.is_file() or not f.name.endswith(".png") or not NUM.match(f.name[:-4]):
                        continue
                    if f.stat().st_size > MAX_FILE:
                        continue
                    yield Path(dim.name, zoom.name, x.name, f.name)


def live_json(tiles: Path) -> str:
    """Like the mod's live.json without players; the folder name turns back into the dimension id at its first "_"."""
    dims = []
    if tiles.is_dir():
        for d in sorted(tiles.iterdir()):
            if d.is_dir() and not d.is_symlink() and DIM.match(d.name):
                dims.append({"id": d.name.replace("_", ":", 1), "folder": d.name})
    return json.dumps({"players": [], "dimensions": dims}, separators=(",", ":"))


def viewer(jar: Path | None, index: Path | None) -> bytes | None:
    if index is not None:
        return index.read_bytes()
    if jar is not None:
        with zipfile.ZipFile(jar) as z:
            return z.read(INDEX_IN_JAR)
    return None


def write_if_changed(path: Path, data: bytes, dry: bool, log) -> bool:
    if path.is_file() and path.read_bytes() == data:
        return False
    log(f"write {path.name}")
    if not dry:
        tmp = path.with_name(path.name + ".new")
        tmp.write_bytes(data)
        os.replace(tmp, path)
    return True


def sync(world: Path, dest: Path, jar: Path | None, index: Path | None, dry: bool, log=print) -> dict:
    src = world / "simplelivemap" / "tiles"
    if not src.is_dir():
        raise SystemExit(f"no tiles in {src} (is Simple Livemap installed and has someone explored yet?)")
    out = dest / "tiles"
    if not dry:
        out.mkdir(parents=True, exist_ok=True)
    wanted = set()
    copied = 0
    for rel in tile_files(src):
        wanted.add(rel)
        a, b = src / rel, out / rel
        sa = a.stat()
        if b.is_file():
            sb = b.stat()
            if sb.st_size == sa.st_size and int(sb.st_mtime) == int(sa.st_mtime):
                continue
        copied += 1
        if not dry:
            b.parent.mkdir(parents=True, exist_ok=True)
            tmp = b.with_name(b.name + ".new")
            shutil.copy2(a, tmp)
            os.replace(tmp, b)
    removed = 0
    if out.is_dir():
        for f in sorted(out.rglob("*"), reverse=True):
            rel = f.relative_to(out)
            if f.is_symlink() or (f.is_file() and rel not in wanted):
                removed += 1
                if not dry:
                    f.unlink()
            elif f.is_dir() and not dry and not any(f.iterdir()):
                f.rmdir()
    write_if_changed(dest / "live.json", live_json(src).encode("utf-8"), dry, log)
    page = viewer(jar, index)
    if page is None:
        if not (dest / "index.html").is_file():
            log("warning: no viewer page yet (give --jar <simplelivemap jar> or --index <index.html>)")
    else:
        write_if_changed(dest / "index.html", page, dry, log)
    result = {"tiles": len(wanted), "copied": copied, "removed": removed}
    log(("dry run: " if dry else "") + f"{len(wanted)} tiles, {copied} copied, {removed} removed -> {dest}")
    return result


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--world", required=True, type=Path, help="world folder (contains simplelivemap/tiles)")
    p.add_argument("--dest", required=True, type=Path, help="output folder to upload")
    p.add_argument("--jar", type=Path, help="simplelivemap mod jar (viewer page)")
    p.add_argument("--index", type=Path, help="viewer page file instead of --jar")
    p.add_argument("--every", type=int, default=0, help="repeat every N seconds (0 = once)")
    p.add_argument("--dry-run", action="store_true", help="only report")
    a = p.parse_args(argv)
    if a.every and a.every < 30:
        p.error("--every: at least 30 seconds")
    while True:
        sync(a.world, a.dest, a.jar, a.index, a.dry_run)
        if not a.every:
            return 0
        time.sleep(a.every)


if __name__ == "__main__":
    sys.exit(main())
