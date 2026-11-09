#!/usr/bin/env python3
"""Public, read-only download index for the landing page simplemods.de (W12, IG31-54).

Derived from the Zentrale collection (collect_downloads.py, ``<dir>/index.json`` + ``files/``): the newest COMPLETE
gate-green build (contains SimpleLib for every public loader) is copied as hard links into ``<dir>/public/``:

    public/index.json                     manifest for tools/landing/paket.html
    public/files/<loader>/<mc>/<jar>      only the released loaders (default: Fabric 26.3), no dev/sources jars

nginx serves ``<dir>/public/`` as https://simplemods.de/downloads/ (tools/zentrale-web/nginx/domains.conf); the rest of
the collection stays behind the login. External mods (Fabric API, Cloth Config, ...) are only linked, never shipped.

    python3 tools/zentrale-web/public_downloads.py --out ~/zentrale-downloads [--loaders fabric] [--minecraft 26.3]

collect_downloads.py calls build() after every collection, so dist-publish refreshes it with every green gate.
"""
import argparse
import datetime as dt
import json
import os
import shutil
import sys
import tomllib
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import modselect  # noqa: E402

PUBLIC_LOADERS = ("fabric",)  # owner 22.10.: until everything is done only 26.3 Fabric is released
PUBLIC_MC = "26.3"
# ids from the loader metadata that are no mod a player downloads
PLATFORM = {"minecraft", "java", "fabricloader", "fabric-loader", "neoforge", "forge", "framework"}
EXTERNAL = {  # id -> (name, link); links only, the jars are never redistributed
    "fabric-api": ("Fabric API", "https://modrinth.com/mod/fabric-api"),
    "cloth-config": ("Cloth Config API", "https://modrinth.com/mod/cloth-config"),
    "jei": ("Just Enough Items", "https://modrinth.com/mod/jei"),
    "roughlyenoughitems": ("Roughly Enough Items", "https://modrinth.com/mod/rei"),
    "modmenu": ("Mod Menu", "https://modrinth.com/mod/modmenu"),
    "jade": ("Jade", "https://modrinth.com/mod/jade"),
    "appleskin": ("AppleSkin", "https://modrinth.com/mod/appleskin"),
    "mousetweaks": ("Mouse Tweaks", "https://modrinth.com/mod/mouse-tweaks"),
    "trinkets_updated": ("Trinkets", "https://modrinth.com/mod/trinkets"),
    "curios": ("Curios API", "https://modrinth.com/mod/curios"),
}
LOADER_LINKS = {"fabric": ("Fabric Loader", "https://fabricmc.net/use/installer/"),
                "neoforge": ("NeoForge", "https://neoforged.net/"),
                "forge": ("Forge", "https://files.minecraftforge.net/")}


ALIAS = {"cloth_config": "cloth-config", "fabric": "fabric-api"}


def own(mod_id, nodes):
    """A Simple mod (also misspelt/planned ids like simpledimension) is never an external download."""
    return mod_id in nodes or mod_id.startswith("simple") or mod_id == "wiringexample"


def jar_info(path):
    """depends / suggests / nested jar names from the loader metadata ({} when unreadable)."""
    try:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            if "fabric.mod.json" in names:
                d = json.loads(z.read("fabric.mod.json").decode("utf-8"))
                return {"depends": sorted(d.get("depends") or {}),
                        "suggests": sorted(set(d.get("recommends") or {}) | set(d.get("suggests") or {})),
                        "nested": sorted(Path(j["file"]).name for j in d.get("jars") or [] if j.get("file"))}
            for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if name in names:
                    d = tomllib.loads(z.read(name).decode("utf-8"))
                    dep, opt = set(), set()
                    for lst in (d.get("dependencies") or {}).values():
                        for x in lst:
                            req = x.get("type", "required" if x.get("mandatory", True) else "optional") == "required"
                            (dep if req else opt).add(x.get("modId"))
                    nested = []
                    if "META-INF/jarjar/metadata.json" in names:
                        meta = json.loads(z.read("META-INF/jarjar/metadata.json").decode("utf-8"))
                        nested = [Path(j.get("path", "")).name for j in meta.get("jars", [])]
                    return {"depends": sorted(dep - {None}), "suggests": sorted(opt - {None}), "nested": sorted(nested)}
    except Exception:
        pass
    return {"depends": [], "suggests": [], "nested": []}


def pick_build(index, loaders, mc):
    """Newest build that holds SimpleLib for every public loader (partial gate builds are skipped)."""
    for b in index.get("builds", []):
        have = {(f["module"], f["loader"], f["minecraft"]) for f in b.get("files", [])}
        if all(("simplelib", lo, mc) in have for lo in loaders):
            return b
    return None


def _link(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    try:
        os.link(src, dst)
    except OSError:
        shutil.copy2(src, dst)


def build(out, repo=modselect.REPO, loaders=PUBLIC_LOADERS, mc=PUBLIC_MC, now=None):
    """Writes <out>/public/. Returns the public index (dict) or None when no complete build exists."""
    out = Path(out)
    index = json.loads((out / "index.json").read_text(encoding="utf-8"))
    b = pick_build(index, loaders, mc)
    if not b:
        return None
    nodes = {n["id"]: n for n in modselect.catalog(repo)}
    pub = out / "public.new"
    shutil.rmtree(pub, ignore_errors=True)
    by_file = {}
    for f in b["files"]:
        if f["loader"] in loaders and f["minecraft"] == mc:
            by_file[(f["loader"], f["file"])] = f
    mods, used_ext = [], set()
    for (loader, _), f in sorted(by_file.items(), key=lambda kv: (kv[0][0], kv[1]["module"])):
        src = out / f["url"]
        if not src.is_file():
            continue
        rel = f"files/{loader}/{mc}/{f['file']}"
        _link(src, pub / rel)
        info = jar_info(src)
        n = nodes.get(f["module"], {})
        here = {g["file"]: g["module"] for (lo, _), g in by_file.items() if lo == loader}
        contains = sorted({here[j] for j in info["nested"] if j in here})
        dep = [d for d in info["depends"] if d not in PLATFORM]
        ext_req = sorted({ALIAS.get(d, d) for d in dep if not own(d, nodes)}
                         | {ALIAS.get(d, d) for d in n.get("external", [])})
        # optional: only well-known mods with a link (compat hooks such as claim mods are no download hint)
        ext_opt = sorted({ALIAS.get(d, d) for d in info["suggests"] if d in EXTERNAL} - set(ext_req))
        used_ext |= set(ext_req) | set(ext_opt)
        mods.append({
            "module": f["module"], "name": f.get("name") or n.get("name", f["module"]), "kind": n.get("kind", f.get("kind")),
            "parent": n.get("parent"), "loader": loader, "minecraft": mc, "version": f.get("version", ""),
            "file": f["file"], "size": f["size"], "sha256": f["sha256"], "url": rel,
            "requires": sorted(set(n.get("requires", [])) | {d for d in dep if d in nodes}),
            "contains": contains, "external": ext_req, "externalOptional": ext_opt,
        })
    if not mods:
        shutil.rmtree(pub, ignore_errors=True)
        return None
    stamp = (now or dt.datetime.now(dt.timezone.utc)).isoformat(timespec="seconds")
    sb = next((m["version"] for m in mods if m["module"] == "simplebuilding"), "")
    pindex = {
        "schemaVersion": 1, "updatedAt": stamp,
        "release": {"version": sb, "commit": b["short"], "date": b.get("date", ""), "verdict": b.get("verdict", "")},
        "minecraft": mc, "loaders": [lo for lo in loaders if any(m["loader"] == lo for m in mods)],
        "loaderLinks": {lo: dict(zip(("name", "url"), LOADER_LINKS[lo])) for lo in loaders if lo in LOADER_LINKS},
        "externalMods": {e: {"name": EXTERNAL.get(e, (e, ""))[0], "url": EXTERNAL.get(e, ("", ""))[1]}
                         for e in sorted(used_ext)},
        "note": "Release builds for testing. External mods are linked, not redistributed.",
        "mods": mods,
    }
    (pub / "index.json").write_text(json.dumps(pindex, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    old = out / "public.old"
    shutil.rmtree(old, ignore_errors=True)
    if (out / "public").exists():
        os.replace(out / "public", old)
    os.replace(pub, out / "public")
    shutil.rmtree(old, ignore_errors=True)
    return pindex


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--repo", default=str(modselect.REPO))
    ap.add_argument("--out", required=True, help="collection folder of collect_downloads.py")
    ap.add_argument("--loaders", default=",".join(PUBLIC_LOADERS))
    ap.add_argument("--minecraft", default=PUBLIC_MC)
    a = ap.parse_args(argv)
    p = build(a.out, a.repo, tuple(x for x in a.loaders.split(",") if x), a.minecraft)
    if not p:
        print("public_downloads: no complete build for " + a.loaders, file=sys.stderr)
        return 2
    print(f"public_downloads: {len(p['mods'])} jars of {p['release']['commit']} ({','.join(p['loaders'])} {p['minecraft']})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
