"""Simple Redstone (WP-17, docs/ai/KONZEPT-A-WP17-2026-10-29.md): textures, models, blockstates, item definitions.

Own pixel art in the Vanilla frame (soft gradients, a dark contour, ~7 % hard steps, A8/A8b), nothing from Vanilla:

* Double Piston (1x2)   base ``double_piston_side``/``_back``, barrel ``double_piston_barrel_side``/``_front_open``, head ``double_piston_head``
                        (plate), ``double_piston_head_side``, ``double_piston_rod``. Steel body with a redstone band, iron plate.
* Signal Dampener       ``signal_dampener_top_<stage>[_on]``: a flat diode, input track at the back (front of the model is the
                        input side), a coil in the middle, three lamps: as many lamps lit as the stage; the track glows with a signal.

Every texture gets its layer source (tools/textures/layers.py). The hard-step share (adjacent opaque pixels whose brightness differs by more
than HARD_STEP) is printed per texture.

  python3 tools/textures/redstone29_2026_10_29.py          # writes everything
  python3 tools/textures/redstone29_2026_10_29.py --check  # exit 1 when a file is stale
"""
import argparse
import json
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "modules/simpleredstone/shared/resources/assets/simpleredstone"
NS = "simpleredstone"
HARD_STEP = 46


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(v * f) for v in c)


def noise(x, y, seed):
    """Deterministic +-1 jitter, so flat areas are not dead."""
    h = (x * 73856093 ^ y * 19349663 ^ seed * 83492791) & 0xFFFF
    return (h % 5) - 2


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (a,))


def hard_share(img):
    """Share of adjacent opaque pixel pairs with a brightness step above HARD_STEP."""
    px = img.load()
    lum = lambda p: 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]
    pairs = hard = 0
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx > 15 or y + dy > 15:
                    continue
                a, b = px[x, y], px[x + dx, y + dy]
                if a[3] == 255 and b[3] == 255:
                    pairs += 1
                    hard += abs(lum(a) - lum(b)) > HARD_STEP
    return hard / pairs if pairs else 0.0


# ------------------------------------------------------------------ palette
STEEL = (128, 133, 140)
STEEL_DARK = (78, 82, 90)
STEEL_LIGHT = (168, 173, 181)
RED = (168, 38, 34)
RED_DARK = (104, 26, 26)
RED_LIT = (232, 56, 40)
AMBER_LIT = (255, 214, 92)
AMBER_OFF = (84, 66, 44)
COIL = (176, 112, 70)


def panel(base, seed, edge=0.78, top=1.08, bottom=0.9):
    """Soft vertical gradient with a darker contour; used for every body face."""
    img = new()
    for y in range(16):
        k = top + (bottom - top) * y / 15.0
        for x in range(16):
            c = shade(base, k)
            j = noise(x, y, seed)
            c = tuple(clamp(v + j) for v in c)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, edge)
            put(img, x, y, c)
    return img


def side():
    img = panel(STEEL, 11)
    layers.snap(img, "Fläche")
    for y in range(16):
        # redstone band across the middle (rows 6-9), softened at its rims
        t = {6: 0.45, 7: 1.0, 8: 1.0, 9: 0.45}.get(y)
        if t is None:
            continue
        for x in range(16):
            base = img.getpixel((x, y))[:3]
            band = shade(RED, 1.0 - 0.18 * abs(y - 7.5) / 1.5)
            put(img, x, y, mix(base, band, t))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):  # rivets
        put(img, x, y, shade(STEEL_LIGHT, 1.0))
        put(img, x + (1 if x < 8 else -1), y + 1, shade(STEEL_DARK, 1.0))
    return layers.snap(img, "Details")


def back():
    img = panel(shade(STEEL, 0.86), 21, edge=0.74)
    layers.snap(img, "Fläche")
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.4:
                f = 1.0 - d / 3.4
                put(img, x, y, mix(img.getpixel((x, y))[:3], RED, 0.35 + 0.55 * f))
            elif d < 4.4:
                put(img, x, y, shade(img.getpixel((x, y))[:3], 0.8))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, shade(STEEL_LIGHT, 0.95))
    return layers.snap(img, "Details")


def barrel_side():
    img = new()
    for y in range(16):
        for x in range(16):
            k = 1.14 - 0.24 * y / 15.0
            c = shade(STEEL_LIGHT, k * 0.92)
            c = tuple(clamp(v + noise(x, y, 31)) for v in c)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.78)
            put(img, x, y, c)
    layers.snap(img, "Fläche")
    for x in (4, 8, 12):  # three soft ring grooves
        for y in range(16):
            for dx, f in ((-1, 0.94), (0, 0.8), (1, 1.06)):
                px = img.getpixel((x + dx, y))
                put(img, x + dx, y, shade(px[:3], f))
    return layers.snap(img, "Details")


def head():
    """The plate seen from the front: iron plate, lighter centre cross, four rivets."""
    img = new()
    for y in range(16):
        for x in range(16):
            dist = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            c = shade(STEEL_LIGHT, 1.04 - 0.2 * dist)
            c = tuple(clamp(v + noise(x, y, 41)) for v in c)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.84)
            elif x in (1, 14) or y in (1, 14):
                c = shade(c, 1.04)
            put(img, x, y, c)
    layers.snap(img, "Fläche")
    for i in range(5, 11):  # a soft cross
        for x, y in ((i, 7), (i, 8), (7, i), (8, i)):
            put(img, x, y, shade(img.getpixel((x, y))[:3], 1.1))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(img, x, y, shade(STEEL, 0.98))
        put(img, x + 1, y + 1, shade(STEEL, 0.9))
    return layers.snap(img, "Details")


def head_side():
    img = panel(shade(STEEL, 0.84), 51, edge=0.8, top=1.04, bottom=0.92)
    return layers.snap(img, "Fläche")


def rod():
    img = new()
    for y in range(16):
        for x in range(16):
            k = 0.78 + 0.5 * (1 - abs(x - 5.5) / 5.5) ** 0.8 if x < 12 else 0.78
            c = tuple(clamp(v * k + noise(x, y, 61)) for v in STEEL)
            if x in (0, 15):
                c = shade(c, 0.84)
            put(img, x, y, c)
    layers.snap(img, "Fläche")
    for y in (3, 4, 11, 12):  # two soft collars
        for x in range(16):
            put(img, x, y, shade(img.getpixel((x, y))[:3], 0.9 if y in (3, 11) else 1.06))
    return layers.snap(img, "Details")


def front_open():
    """Barrel seen from the front while the rod is out: a dark opening in the iron frame."""
    img = head()
    layers.snap(img, "Platte")
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d < 1.8:
                put(img, x, y, (46, 48, 54))
            elif d < 2.8:
                put(img, x, y, (84, 87, 94))
            elif d < 3.8:
                put(img, x, y, shade(img.getpixel((x, y))[:3], 0.8))
    return layers.snap(img, "Öffnung")


# ------------------------------------------------------------------ dampener
BASE_TOP = (122, 126, 132)


def dampener_base():
    img = panel(shade(STEEL, 0.9), 71, edge=0.76, top=1.0, bottom=0.92)
    return layers.snap(img, "Fläche")


def dampener_top(stage, on):
    """Top view; the model's north is the input side. Track (back to front), coil, three lamps (stage lit)."""
    img = new()
    for y in range(16):
        for x in range(16):
            c = shade(BASE_TOP, 1.08 - 0.16 * y / 15.0)
            c = tuple(clamp(v + noise(x, y, 81)) for v in c)
            if x in (0, 15) or y in (0, 15):
                c = shade(c, 0.78)
            put(img, x, y, c)
    layers.snap(img, "Fläche")
    track = RED_LIT if on else RED_DARK
    for y in range(0, 16):
        for x in (7, 8):
            put(img, x, y, track if not (y in (0, 15)) else shade(track, 0.8))
        for x in (6, 9):
            f = 0.55 if on else 0.9
            put(img, x, y, mix(img.getpixel((x, y))[:3], track, 0.25 if on else 0.0) if on else shade(img.getpixel((x, y))[:3], f))
    layers.snap(img, "Leiterbahn")
    for y in range(5, 9):  # coil block over the track
        for x in range(4, 12):
            k = 1.1 - 0.1 * (y - 5) / 3.0
            put(img, x, y, shade(COIL, k * (0.9 if x in (4, 11) else 1.0)))
    for y in (6, 7):
        for x in range(5, 11):
            put(img, x, y, shade(COIL, 0.8 + 0.1 * ((x + y) % 2)))
    layers.snap(img, "Spule")
    for i, cx in enumerate((3, 7, 11)):
        lit = i < stage
        c = AMBER_LIT if lit else AMBER_OFF
        for y in (11, 12):
            for x in (cx, cx + 1):
                put(img, x, y, shade(c, 1.0 if y == 11 else 0.86))
        if lit:  # a soft halo, no hard ring
            for x in range(cx - 1, cx + 3):
                for y in (10, 13):
                    put(img, x, y, mix(img.getpixel((x, y))[:3], AMBER_LIT, 0.22))
    layers.snap(img, "Lampen")
    return img


def dampener_icon():
    """Flat item sprite: the lit variant (stage 2) seen from above and a little from the front."""
    top = dampener_top(2, True)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, (0, 0, 0), 0)
    for y in range(3, 13):
        for x in range(1, 15):
            src = top.getpixel((x, 1 + (y - 3) * 14 // 9))
            put(img, x, y, src[:3])
    for x in range(1, 15):  # front edge
        put(img, x, 13, shade(STEEL, 0.78))
        put(img, x, 14, shade(STEEL, 0.62))
    for y in range(3, 13):
        put(img, 0, y, shade(STEEL, 0.68))
        put(img, 15, y, shade(STEEL, 0.68))
    for x in range(1, 15):
        put(img, x, 2, shade(STEEL, 0.78))
    layers.snap(img, "Fläche")
    return img


# ------------------------------------------------------------------ models
def cube(textures_by_face, frm=(0, 0, 0), to=(16, 16, 16), uv=None):
    faces = {}
    for face, tex in textures_by_face.items():
        f = {"texture": tex}
        if uv and face in uv:
            f["uv"] = uv[face]
        faces[face] = f
    return {"from": list(frm), "to": list(to), "faces": faces}


def block_model(elements, textures, display=None):
    m = {"textures": textures, "elements": elements}
    if display:
        m["display"] = display
    return m


def models():
    out = {}
    out["models/block/double_piston.json"] = block_model(
        [cube({"north": "#side", "south": "#back", "east": "#side", "west": "#side", "up": "#side", "down": "#side"})],
        {"side": f"{NS}:block/double_piston_side", "back": f"{NS}:block/double_piston_back", "particle": f"{NS}:block/double_piston_side"})
    out["models/block/double_piston_barrel.json"] = block_model(
        [cube({"north": "#plate", "south": "#sleeve", "east": "#sleeve", "west": "#sleeve", "up": "#sleeve", "down": "#sleeve"})],
        {"sleeve": f"{NS}:block/double_piston_barrel_side", "plate": f"{NS}:block/double_piston_head", "particle": f"{NS}:block/double_piston_barrel_side"})
    out["models/block/double_piston_barrel_open.json"] = block_model(
        [cube({"north": "#open", "south": "#sleeve", "east": "#sleeve", "west": "#sleeve", "up": "#sleeve", "down": "#sleeve"})],
        {"sleeve": f"{NS}:block/double_piston_barrel_side", "open": f"{NS}:block/double_piston_front_open", "particle": f"{NS}:block/double_piston_barrel_side"})
    plate_uv = {"east": [0, 0, 4, 16], "west": [0, 0, 4, 16], "up": [0, 0, 16, 4], "down": [0, 0, 16, 4]}
    rod_uv = {"east": [0, 0, 16, 4], "west": [0, 0, 16, 4], "up": [0, 0, 16, 4], "down": [0, 0, 16, 4], "north": [0, 0, 4, 4], "south": [0, 0, 4, 4]}
    plate = cube({"north": "#plate", "south": "#plate_side", "east": "#plate_side", "west": "#plate_side", "up": "#plate_side", "down": "#plate_side"},
                 (0, 0, 0), (16, 16, 4), {"east": [0, 0, 4, 16], "west": [0, 0, 4, 16], "up": [0, 0, 16, 4], "down": [0, 0, 16, 4]})
    rod_head = cube({f: "#rod" for f in ("north", "south", "east", "west", "up", "down")}, (6, 6, 4), (10, 10, 16), rod_uv)
    rod_arm = cube({f: "#rod" for f in ("north", "south", "east", "west", "up", "down")}, (6, 6, 0), (10, 10, 16), rod_uv)
    tex = {"plate": f"{NS}:block/double_piston_head", "plate_side": f"{NS}:block/double_piston_head_side", "rod": f"{NS}:block/double_piston_rod",
           "particle": f"{NS}:block/double_piston_head_side"}
    out["models/block/double_piston_head.json"] = block_model([plate, rod_head], tex)
    out["models/block/double_piston_arm.json"] = block_model([rod_arm], tex)
    # Item: both cells squeezed into one cube (back half the base, front half the barrel).
    item = block_model(
        [cube({"north": "#side", "south": "#back", "east": "#side", "west": "#side", "up": "#side", "down": "#side"}, (0, 0, 8), (16, 16, 16)),
         cube({"north": "#plate", "south": "#sleeve", "east": "#sleeve", "west": "#sleeve", "up": "#sleeve", "down": "#sleeve"}, (0, 0, 0), (16, 16, 8))],
        {"side": f"{NS}:block/double_piston_side", "back": f"{NS}:block/double_piston_back", "sleeve": f"{NS}:block/double_piston_barrel_side",
         "plate": f"{NS}:block/double_piston_head", "particle": f"{NS}:block/double_piston_side"})
    item["parent"] = "minecraft:block/block"
    out["models/item/double_piston.json"] = item
    out["items/double_piston.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/double_piston"}}
    # Dampener: a 2 px slab, one model per (stage, powered).
    for stage in range(4):
        for on in (False, True):
            name = f"signal_dampener_{stage}{'_on' if on else ''}"
            out[f"models/block/{name}.json"] = block_model(
                [cube({"up": "#top", "down": "#base", "north": "#base", "south": "#base", "east": "#base", "west": "#base"}, (0, 0, 0), (16, 2, 16),
                      {"north": [0, 14, 16, 16], "south": [0, 14, 16, 16], "east": [0, 14, 16, 16], "west": [0, 14, 16, 16]})],
                {"top": f"{NS}:block/signal_dampener_top_{stage}{'_on' if on else ''}", "base": f"{NS}:block/signal_dampener_base",
                 "particle": f"{NS}:block/signal_dampener_base"})
    out["models/item/signal_dampener.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/signal_dampener"}}
    out["items/signal_dampener.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/signal_dampener"}}
    return out


ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def blockstates():
    out = {}
    out["blockstates/double_piston.json"] = {"variants": {f"facing={f},ext={e}": {"model": f"{NS}:block/double_piston", **ROT[f]}
                                                          for f in ROT for e in range(3)}}
    variants = {}
    for f in ROT:
        for e in range(3):
            variants[f"facing={f},ext={e}"] = {"model": f"{NS}:block/double_piston_barrel{'' if e == 0 else '_open'}", **ROT[f]}
    out["blockstates/double_piston_barrel.json"] = {"variants": variants}
    out["blockstates/double_piston_head.json"] = {"variants": {f"facing={f}": {"model": f"{NS}:block/double_piston_head", **ROT[f]} for f in ROT}}
    out["blockstates/double_piston_arm.json"] = {"variants": {f"facing={f}": {"model": f"{NS}:block/double_piston_arm", **ROT[f]} for f in ROT}}
    # The model's north is the input side, and the block's FACING points at the input: facing=north needs no rotation.
    variants = {}
    for f, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270})):
        for stage in range(4):
            for on in (False, True):
                variants[f"facing={f},stage={stage},powered={'true' if on else 'false'}"] = {
                    "model": f"{NS}:block/signal_dampener_{stage}{'_on' if on else ''}", **rot}
    out["blockstates/signal_dampener.json"] = {"variants": variants}
    return out


def loot(item):
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": f"{NS}:{item}"}], "rolls": 1}]}


def data_files():
    d = ROOT / "modules/simpleredstone/shared/resources/data"
    out = {}
    out[str(d / NS / "loot_table/blocks/double_piston.json")] = loot("double_piston")
    out[str(d / NS / "loot_table/blocks/signal_dampener.json")] = loot("signal_dampener")
    for part in ("double_piston_barrel", "double_piston_head", "double_piston_arm"):
        out[str(d / NS / f"loot_table/blocks/{part}.json")] = {"type": "minecraft:block", "pools": []}
    out[str(d / "minecraft/tags/block/mineable/pickaxe.json")] = {
        "values": [f"{NS}:double_piston", f"{NS}:double_piston_barrel", f"{NS}:double_piston_head", f"{NS}:double_piston_arm"]}
    out[str(d / NS / "recipe/double_piston.json")] = {
        "type": "minecraft:crafting_shaped", "category": "redstone",
        "key": {"Q": "minecraft:quartz", "R": "minecraft:redstone_block", "I": "minecraft:iron_ingot", "P": "minecraft:piston", "B": "minecraft:iron_block"},
        "pattern": ["QRQ", "IPI", "BP "], "result": {"count": 1, "id": f"{NS}:double_piston"}}
    out[str(d / NS / "recipe/signal_dampener.json")] = {
        "type": "minecraft:crafting_shaped", "category": "redstone",
        "key": {"T": "minecraft:redstone_torch", "Q": "minecraft:quartz", "S": "minecraft:stone"},
        "pattern": ["TQT", "SSS"], "result": {"count": 1, "id": f"{NS}:signal_dampener"}}
    return out


def textures():
    t = {"double_piston_side": side(), "double_piston_back": back(), "double_piston_barrel_side": barrel_side(), "double_piston_head": head(),
         "double_piston_head_side": head_side(), "double_piston_rod": rod(), "double_piston_front_open": front_open(),
         "signal_dampener_base": dampener_base()}
    out = {f"textures/block/{k}.png": v for k, v in t.items()}
    for stage in range(4):
        for on in (False, True):
            out[f"textures/block/signal_dampener_top_{stage}{'_on' if on else ''}.png"] = dampener_top(stage, on)
    out["textures/item/signal_dampener.png"] = dampener_icon()
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    stale = []
    jobs = []
    for rel, img in textures().items():
        jobs.append((RES / rel, img))
    for rel, data in {**models(), **blockstates()}.items():
        jobs.append((RES / rel, data))
    for path, data in data_files().items():
        jobs.append((Path(path), data))
    for path, data in jobs:
        if isinstance(data, Image.Image):
            same = path.is_file() and Image.open(path).convert("RGBA").tobytes() == data.tobytes()
        else:
            same = path.is_file() and json.loads(path.read_text()) == data
        if a.check:
            if not same:
                stale.append(path.name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(data, Image.Image):
            layers.save(data, path)
            print(f"{path.name:44s} hard steps {hard_share(data) * 100:4.1f} %")
        else:
            path.write_text(json.dumps(data, indent=2) + "\n")
    if a.check:
        print("stale: " + ", ".join(stale[:8]) if stale else "OK redstone29")
        return 1 if stale else 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
