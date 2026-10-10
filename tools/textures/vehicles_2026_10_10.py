"""Tiered vehicles (Queue N19/N23): item sprites and item models of the tiered carts and chest boats.

docs/ai/PLAN-FAHRZEUG-STUFEN-2026-10-10.md. No vanilla pixels are written: every sprite is an overlay that the item
model puts as layer1 over vanilla's own sprite (layer0 = minecraft:item/minecart or the chest boat of the wood). The
overlay covers exactly the pixels where vanilla's chest/furnace/hopper cart (chest boat) differs from the empty cart
(plain boat). Inside that shape the shading follows vanilla's light (dark outline, dark front, lit top), the colours
come from the tier's own textures (tier chest atlas, tier furnace top, tier hopper), and each tier adds its mark:
Reinforced a light metal band across the lid with a diamond lock, Netherite a gold lock, Enderite a pink lock and
end sparkles.

  python3.12 tools/textures/vehicles_2026_10_10.py                 # write sprites, models, preview
  python3.12 tools/textures/vehicles_2026_10_10.py --check         # sprites and models are current
  python3.12 tools/textures/vehicles_2026_10_10.py --preview DIR   # preview to DIR (default /root/previews/vehicles)

Vanilla sprites are read from SB_VANILLA (an extracted client jar), else from the Loom client jar.
"""
import argparse
import io
import json
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
OWN_TEX = ROOT / "src/main/resources/assets/simplebuilding/textures"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
TIERS = ["reinforced", "netherite", "enderite"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "pale_oak", "mangrove", "poplar", "bamboo"]
LOCK = {"reinforced": (90, 220, 210), "netherite": (230, 170, 60), "enderite": (250, 180, 255)}
LOCK_DARK = {"reinforced": (40, 160, 160), "netherite": (150, 96, 30), "enderite": (200, 90, 210)}


def vanilla(name):
    base = os.environ.get("SB_VANILLA")
    rel = f"assets/minecraft/textures/item/{name}.png"
    if base:
        return Image.open(Path(base) / rel).convert("RGBA")
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        return Image.open(io.BytesIO(jar.read(rel))).convert("RGBA")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp(path, box=None, steps=5):
    """Five shades (dark..light) of an own texture: luminance quantiles of its opaque pixels."""
    im = Image.open(path).convert("RGBA")
    if box:
        im = im.crop(box)
    px = sorted((c[:3] for c in im.get_flattened_data() if c[3] > 0), key=lum)
    return [px[min(len(px) - 1, int(q * (len(px) - 1)))] for q in (0.05, 0.3, 0.55, 0.75, 0.9)][:steps]


def mask(with_content, empty):
    a, b = with_content.load(), empty.load()
    return {(x, y) for x in range(16) for y in range(16) if a[x, y] != b[x, y] and a[x, y][3] > 0}


def paint(src, region, shades, lock=None):
    """The region of vanilla sprite src in the tier shades, by vanilla's light; near-white grey pixels become the lock."""
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p, o = src.load(), out.load()
    lums = [lum(p[x, y]) for x, y in region
            if not (lock and max(p[x, y][:3]) - min(p[x, y][:3]) < 20 and lum(p[x, y]) > 150)]
    lo, hi = min(lums), max(lums)
    for x, y in region:
        c = p[x, y]
        grey = max(c[:3]) - min(c[:3]) < 20
        if lock and grey and lum(c) > 150:
            o[x, y] = lock + (255,)
            continue
        t = (lum(c) - lo) / max(1.0, hi - lo)
        o[x, y] = shades[min(len(shades) - 1, int(t * len(shades)))] + (255,)
    return out


def tier_marks(img, region, tier, kind):
    o = img.load()
    top = sorted(region, key=lambda q: (q[1], q[0]))
    rows = {}
    for x, y in region:
        rows.setdefault(y, []).append(x)
    if kind in ("chest", "boat"):
        lock_pixels = [(x, y) for (x, y) in region if o[x, y][:3] == LOCK[tier]]
        if not lock_pixels:
            # no lit lock pixel in vanilla's shape (boats): put one at the front edge of the lid
            y = sorted(rows)[len(rows) // 2]
            lock_pixels = [(min(rows[y]) + 1, y)]
            o[lock_pixels[0]] = LOCK[tier] + (255,)
        for (x, y) in lock_pixels:
            if (x, y + 1) in region:
                o[x, y + 1] = LOCK_DARK[tier] + (255,)
    if tier == "reinforced" and kind in ("chest", "boat"):
        # metal band across the lid: a row in the middle of the lid, lit
        y = sorted(rows)[4]
        for x in rows[y]:
            if o[x, y][:3] not in (LOCK[tier], LOCK_DARK[tier]):
                o[x, y] = (205, 210, 214, 255)
    if tier == "enderite":
        # two end sparkles on the lit top
        ys = sorted(rows)
        for y, pick in ((ys[1], 0.35), (ys[3], 0.7)):
            xs = sorted(rows[y])
            o[xs[int(pick * (len(xs) - 1))], y] = (240, 170, 250, 255)
    if tier == "netherite" and kind in ("furnace", "hopper"):
        y = sorted(rows)[1]
        xs = sorted(rows[y])
        o[xs[len(xs) // 2], y] = (230, 170, 60, 255)
    return img


def build():
    tex, models, items = {}, {}, {}
    cart = vanilla("minecart")
    for kind, src_name in (("chest", "chest_minecart"), ("furnace", "furnace_minecart"), ("hopper", "hopper_minecart")):
        src = vanilla(src_name)
        region = mask(src, cart)
        for tier in TIERS:
            if kind == "chest":
                shades = ramp(OWN_TEX / f"entity/chest/{tier}.png", (14, 0, 42, 14))
            elif kind == "furnace":
                shades = ramp(OWN_TEX / f"block/{tier}_furnace_top.png", (0, 0, 16, 16))
            else:
                shades = ramp(OWN_TEX / f"block/{tier}_hopper_outside.png", (0, 0, 16, 16))
            img = paint(src, region, shades, LOCK[tier] if kind == "chest" else None)
            name = f"{tier}_{kind}_minecart"
            tex[name] = tier_marks(img, region, tier, "chest" if kind == "chest" else kind)
            models[name] = {"parent": "minecraft:item/generated",
                            "textures": {"layer0": "minecraft:item/minecart", "layer1": f"simplebuilding:item/{name}"}}
            items[name] = {"model": {"type": "minecraft:model", "model": f"simplebuilding:item/{name}"}}
    boat_region = set()
    for wood in WOODS[:-1]:
        boat_region |= mask(vanilla(f"{wood}_chest_boat"), vanilla(f"{wood}_boat"))
    raft_region = mask(vanilla("bamboo_chest_raft"), vanilla("bamboo_raft"))
    for tier in TIERS:
        shades = ramp(OWN_TEX / f"entity/chest/{tier}.png", (14, 0, 42, 14))
        for shape, region, src in (("boat", boat_region, vanilla("oak_chest_boat")),
                                   ("raft", raft_region, vanilla("bamboo_chest_raft"))):
            img = paint(src, region, shades, LOCK[tier])
            tex[f"{tier}_chest_{shape}_overlay"] = tier_marks(img, region, tier, "boat")
        cases = []
        for wood in WOODS:
            raft = wood == "bamboo"
            model = f"{tier}_chest_boat_{wood}"
            models[model] = {"parent": "minecraft:item/generated", "textures": {
                "layer0": "minecraft:item/" + ("bamboo_chest_raft" if raft else f"{wood}_chest_boat"),
                "layer1": f"simplebuilding:item/{tier}_chest_{'raft' if raft else 'boat'}_overlay"}}
            cases.append({"when": wood, "model": {"type": "minecraft:model", "model": f"simplebuilding:item/{model}"}})
        items[f"{tier}_chest_boat"] = {"model": {
            "type": "minecraft:select", "property": "minecraft:component", "component": "simplebuilding:boat_wood",
            "cases": cases,
            "fallback": {"type": "minecraft:model", "model": f"simplebuilding:item/{tier}_chest_boat_oak"}}}
    return tex, models, items


def preview(tex, out_dir):
    names = []
    for kind in ("chest_minecart", "furnace_minecart", "hopper_minecart"):
        names.append([("minecart", f"minecraft:{kind}")] + [("minecart", t + "_" + kind) for t in TIERS])
    names.append([("oak_chest_boat", "minecraft:oak_chest_boat")] + [("oak_chest_boat", t + "_chest_boat_overlay") for t in TIERS])
    names.append([("bamboo_chest_raft", "minecraft:bamboo_chest_raft")] + [("bamboo_chest_raft", t + "_chest_raft_overlay") for t in TIERS])
    s = 12
    sheet = Image.new("RGBA", (4 * 18 * s, len(names) * 18 * s), (139, 139, 139, 255))
    for r, row in enumerate(names):
        for c, (base, name) in enumerate(row):
            if name.startswith("minecraft:"):
                img = vanilla(name.split(":")[1])
            else:
                img = vanilla(base).copy()
                img.alpha_composite(tex[name])
            sheet.alpha_composite(img.resize((16 * s, 16 * s), Image.NEAREST), (c * 18 * s + s, r * 18 * s + s))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "items-16x.png")
    sheet.save(path)
    return path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--preview", default="/root/previews/vehicles")
    args = ap.parse_args()
    tex, models, items = build()
    stale = []
    files = {}
    for name, img in tex.items():
        files[ASSETS / f"textures/item/{name}.png"] = img
    for name, data in models.items():
        files[ASSETS / f"models/item/{name}.json"] = json.dumps(data, indent=2) + "\n"
    for name, data in items.items():
        files[ASSETS / f"items/{name}.json"] = json.dumps(data, indent=2) + "\n"
    for path, content in files.items():
        if args.check:
            try:
                if isinstance(content, str):
                    same = path.read_text(encoding="utf-8") == content
                else:
                    cur = Image.open(path)
                    same = cur.mode == content.mode and cur.tobytes() == content.tobytes()
            except OSError:
                same = False
            if not same:
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(content, str):
                path.write_text(content, encoding="utf-8", newline="\n")
            else:
                content.save(path)
    if args.check:
        if stale:
            print("Veraltet oder fehlend:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(files)} Dateien aktuell")
        return 0
    print(f"{len(files)} Dateien geschrieben, Vorschau: {preview(tex, args.preview)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
