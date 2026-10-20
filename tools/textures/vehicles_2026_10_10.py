"""Tiered vehicles (Queue N19/N23): item sprites and item models of the tiered carts and chest boats.

docs/ai/PLAN-FAHRZEUG-STUFEN-2026-10-10.md. The sprites are Vanilla-derived (their shape and shading are masks of
Mojang sprites) and therefore never committed: tools/textures/vanilla_derived.py builds them through derived(jar);
this script writes and checks the models only. No vanilla pixel colours are written: every sprite is an overlay that the item
model puts as layer1 over vanilla's own sprite (layer0 = minecraft:item/minecart or the chest boat of the wood). The
overlay covers exactly the pixels where vanilla's chest/furnace/hopper cart (chest boat) differs from the empty cart
(plain boat). Inside that shape the shading follows vanilla's light (dark outline, dark front, lit top), the colours
come from the tier's own textures (tier chest atlas, tier furnace top, tier hopper), and each tier adds its mark:
Reinforced a light metal band across the lid with a diamond lock, Netherite a gold lock, Enderite a pink lock and
end sparkles.

  python3.12 tools/textures/vehicles_2026_10_10.py                 # write sprites, models, preview
  python3.12 tools/textures/vehicles_2026_10_10.py --check         # sprites and models are current
  python3.12 tools/textures/vehicles_2026_10_10.py --preview DIR   # preview to DIR (default ~/previews/vehicles)

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
# Split stage 8b-2 (docs/ai/PLAN-B-S8C-STORAGE-2026-10-20.md): the chest carts and chest boats are Simple Storage's.
STORAGE_ASSETS = ROOT / "modules/simplestorage/shared/resources/assets/simplestorage"
OWN_TEX = ROOT / "src/main/resources/assets/simplebuilding/textures"
CLIENT_JAR = Path(os.environ.get("SB_CLIENT_JAR") or Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar")
TIERS = ["reinforced", "netherite", "enderite"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "pale_oak", "mangrove", "poplar", "bamboo"]
# Simple End boat woods (BoatWoods.END, docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md): Simple End's own chest boat sprite under
# the tier overlay; the case only matters while Simple End is loaded.
END_WOODS = ["ashwood", "duskwood"]
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


def mask(with_content, empty, palette=None):
    """Pixels where with_content differs from empty. With a palette only pixels in those colours count: the boat
    hull's lit edge next to the chest also differs from the plain boat, and painting it in the chest colours left
    a stray diagonal line (owner 2026-10-11)."""
    a, b = with_content.load(), empty.load()
    return {(x, y) for x in range(16) for y in range(16) if a[x, y] != b[x, y] and a[x, y][3] > 0
            and (palette is None or a[x, y][:3] in palette)}


def chest_palette():
    """Colours of vanilla's chest (on the chest minecart) plus the two dark tones of its slanted boat front."""
    src = vanilla("chest_minecart")
    cols = {src.getpixel(q)[:3] for q in mask(src, vanilla("minecart"))}
    cart = {c[:3] for c in vanilla("minecart").get_flattened_data() if c[3]}
    return (cols - cart) | {(0x42, 0x2F, 0x0B), (0x47, 0x37, 0x25)}


def interior(region):
    return {(x, y) for (x, y) in region if all(q in region for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))}


def paint(src, region, shades, lock=None):
    """The region of vanilla sprite src in the tier shades, by vanilla's light; near-white grey pixels become the lock."""
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p, o = src.load(), out.load()
    grey = lambda c: max(c[:3]) - min(c[:3]) < 20
    lums = [lum(p[x, y]) for x, y in region if not (lock and grey(p[x, y]))]
    lo, hi = min(lums), max(lums)
    for x, y in region:
        c = p[x, y]
        if lock and grey(c):
            # vanilla's latch: only its brightest pixel becomes the lock, the other latch greys are front wood
            o[x, y] = lock + (255,) if lum(c) > 190 else shades[1] + (255,)
            continue
        t = (lum(c) - lo) / max(1.0, hi - lo)
        o[x, y] = shades[min(len(shades) - 1, int(t * len(shades)))] + (255,)
    return out


def tier_marks(img, region, tier, kind):
    o = img.load()
    if kind in ("chest", "boat"):
        # a two-pixel latch like vanilla's chest: lock over its shadow (the boats' chest has no lit latch pixel,
        # it gets one on the front face where the minecart's latch sits)
        lock_pixels = [(x, y) for (x, y) in region if o[x, y][:3] == LOCK[tier]] or [(3, 6)]
        for (x, y) in lock_pixels:
            o[x, y] = LOCK[tier] + (255,)
            if (x, y + 1) in region and (x, y + 1) not in lock_pixels:
                o[x, y + 1] = LOCK_DARK[tier] + (255,)
    # Owner 2026-10-11: no more lines across the shape. The reinforced metal band ran straight over the round lid
    # and its outline, so the tiers keep only their latch; marks sit on inner lid pixels, never on the outline.
    inner = sorted(interior(region), key=lambda q: (q[1], q[0]))
    if tier == "enderite":
        # two end sparkles on the lit top
        ys = sorted({y for _, y in inner})
        for y, pick in ((ys[1], 0.35), (ys[3], 0.7)):
            xs = sorted(x for x, yy in inner if yy == y)
            o[xs[int(pick * (len(xs) - 1))], y] = (240, 170, 250, 255)
    if tier == "netherite" and kind in ("furnace", "hopper"):
        y = sorted({y for _, y in inner})[2]
        xs = sorted(x for x, yy in inner if yy == y)
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
            ns = "simplestorage" if kind == "chest" else "simplebuilding"
            key = f"simplestorage:{name}" if kind == "chest" else name
            models[key] = {"parent": "minecraft:item/generated",
                           "textures": {"layer0": "minecraft:item/minecart", "layer1": f"{ns}:item/{name}"}}
            items[key] = {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}}
    chest = chest_palette()
    # the chest is the same on every wood, the hull (and its edge next to the chest) is not: only pixels in the
    # chest colours on all chest boats that differ from the plain boat belong to the chest
    boats = [vanilla(f"{wood}_chest_boat") for wood in WOODS[:-1]]
    boat_region = set()
    for wood, img in zip(WOODS[:-1], boats):
        boat_region |= mask(img, vanilla(f"{wood}_boat"), chest)
    boat_region = {q for q in boat_region if all(img.getpixel(q)[:3] in chest for img in boats)}
    raft_region = mask(vanilla("bamboo_chest_raft"), vanilla("bamboo_raft"), chest)
    for tier in TIERS:
        shades = ramp(OWN_TEX / f"entity/chest/{tier}.png", (14, 0, 42, 14))
        for shape, region, src in (("boat", boat_region, vanilla("oak_chest_boat")),
                                   ("raft", raft_region, vanilla("bamboo_chest_raft"))):
            img = paint(src, region, shades, LOCK[tier])
            tex[f"{tier}_chest_{shape}_overlay"] = tier_marks(img, region, tier, "boat")
        cases = []
        for wood in WOODS + END_WOODS:
            raft = wood == "bamboo"
            model = f"{tier}_chest_boat_{wood}"
            ns = "simpleend" if wood in END_WOODS else "minecraft"
            # End-wood models live in simpleend (Nachtrag 66): without that module only this case shows the missing
            # model, instead of the whole item failing to bake over a sprite from a foreign atlas.
            mkey = f"simpleend:{model}" if wood in END_WOODS else f"simplestorage:{model}"
            models[mkey] = {"parent": "minecraft:item/generated", "textures": {
                "layer0": f"{ns}:item/" + ("bamboo_chest_raft" if raft else f"{wood}_chest_boat"),
                "layer1": f"simplestorage:item/{tier}_chest_{'raft' if raft else 'boat'}_overlay"}}
            cases.append({"when": wood, "model": {"type": "minecraft:model", "model": f"{'simpleend' if wood in END_WOODS else 'simplestorage'}:item/{model}"}})
        items[f"simplestorage:{tier}_chest_boat"] = {"model": {
            "type": "minecraft:select", "property": "minecraft:component", "component": "simplestorage:boat_wood",
            "cases": cases,
            "fallback": {"type": "minecraft:model", "model": f"simplestorage:item/{tier}_chest_boat_oak"}}}
    return tex, models, items


def is_storage(name):
    """Split stage 8b-2: the chest cart and chest boat sprites are Simple Storage's (simplestorage:item/...)."""
    return "_chest_" in name


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image (SimpleBuilding's carts)."""
    global CLIENT_JAR
    CLIENT_JAR = Path(jar)
    return {f"item/{name}.png": img for name, img in build()[0].items() if not is_storage(name)}


def derived_storage(jar):
    """The same for Simple Storage (vanilla_derived.py --module simplestorage): chest carts, chest boat/raft overlays."""
    global CLIENT_JAR
    CLIENT_JAR = Path(jar)
    return {f"item/{name}.png": img for name, img in build()[0].items() if is_storage(name)}


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
    ap.add_argument("--preview", default=os.path.expanduser("~/previews/vehicles"))
    args = ap.parse_args()
    tex, models, items = build()
    stale = []
    files = {}
    roots = {"simpleend": ROOT / "modules/simpleend/shared/resources/assets/simpleend", "simplestorage": STORAGE_ASSETS}
    for name, data in models.items():
        root = roots.get(name.split(":")[0], ASSETS) if ":" in name else ASSETS
        files[root / f"models/item/{name.split(':')[-1]}.json"] = json.dumps(data, indent=2) + "\n"
    for name, data in items.items():
        root = roots.get(name.split(":")[0], ASSETS) if ":" in name else ASSETS
        files[root / f"items/{name.split(':')[-1]}.json"] = json.dumps(data, indent=2) + "\n"
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
