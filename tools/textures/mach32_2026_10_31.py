"""Double Piston (IG31-26) and Signal Dampener (IG31-25), b-mach32 31.10.: textures, models, blockstates, item definitions, data.

Both live in the SimpleBuilding core now (IG31-12, Simple Machines tab, docs/ai/PLAN-B-MACH32-2026-10-31.md).

* Double Piston   Vanilla's piston textures by model reference (``minecraft:block/piston_*``, no Vanilla pixel is copied) plus an own
                  overlay layer drawn on top like Vanilla's grass overlay: ``double_piston_side_overlay`` (sides) and
                  ``double_piston_plate_overlay`` (plate). Stage 0 = Vanilla piston, stage 1 = Vanilla's extended base + head, stage 2 =
                  body moved half a block (``double_piston_moved``) + ``double_piston_arm`` (front of the body, rod) + head.
* Signal Dampener own pixel art in the Vanilla Repeater frame (slab = Vanilla smooth stone by reference):
                  ``signal_dampener_top_<stage>[_on]`` with input at the top edge (north), trace to the output, three stage lamps.

Endwood process (PRINZIPIEN A8/A8b): ramps of 8-12 tones, light top-left, soft mottling, ~7 % hard steps (measured: share of
adjacent opaque pixel pairs with a brightness step above HARD_STEP; for the overlays on the composite with Vanilla's texture).

Variants (owner choice on the preview page, codes MACH32-DAEMPFER-A|B|C and MACH32-KOLBEN-A|B): A is written into the repository.

  python3 tools/textures/mach32_2026_10_31.py                       # writes variant A into mc26_3/overlay/resources
  python3 tools/textures/mach32_2026_10_31.py --check               # exit 1 when a repository file is stale
  python3 tools/textures/mach32_2026_10_31.py --preview OUT.html --jar CLIENT.jar   # preview page (Vanilla images only there)
"""
import argparse
import base64
import io
import json
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402
from n41_proposals_2026_10_12 import H, hsh, vnoise  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
DATA = ROOT / "mc26_3/overlay/resources/data/simplebuilding"
NS = "simplebuilding"
HARD_STEP = 46
REPO_DAMPENER = "A"
REPO_PISTON = "A"


# ------------------------------------------------------------------ helpers
def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c[:3]) + (a,))


def get(img, x, y):
    return img.getpixel((x, y))[:3]


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def hard_share(img):
    """Share of adjacent opaque pixel pairs with a brightness step above HARD_STEP."""
    px = img.load()
    w, h = img.size
    pairs = hard = 0
    for y in range(h):
        for x in range(w):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx >= w or y + dy >= h:
                    continue
                a, b = px[x, y], px[x + dx, y + dy]
                if a[3] == 255 and b[3] == 255:
                    pairs += 1
                    hard += abs(lum(a) - lum(b)) > HARD_STEP
    return hard / pairs if pairs else 0.0


def ramp(*hexes):
    return [H(h)[:3] for h in hexes]


def pick(r, t):
    return r[max(0, min(len(r) - 1, int(t * len(r))))]


def tone(r, x, y, seed, level=0.5, rim=True, grain=1.0):
    """One pixel of a softly lit plate: ``level`` (0..1) base brightness, lit top-left rim, shaded bottom-right rim."""
    t = level + 0.12 * (1 - y / 15.0) - 0.04
    t += (vnoise(x, y, 8, seed) - 0.5) * 0.26 * grain + (vnoise(x, y, 4, seed + 5) - 0.5) * 0.12 * grain
    t += (hsh(x, y, seed + 13) - 0.5) * 0.05 * grain
    if rim:
        if x == 0 or y == 0:
            t += 0.12
        elif x == 15 or y == 15:
            t -= 0.20
        elif x == 14 or y == 14:
            t -= 0.06
        elif x == 1 or y == 1:
            t += 0.04
    return pick(r, t)


STONE = ramp("5b5c5e", "67686a", "737476", "7f8082", "8b8c8e", "97989a", "a3a4a6", "afb0b2", "bbbcbe", "c7c8ca")
QUARTZ = ramp("a39a92", "b0a79f", "bdb4ac", "c9c1b9", "d4cdc6", "ddd7d1", "e6e1dc", "eeeae6", "f5f2ef")
DEEP = ramp("26262b", "2e2e34", "37373e", "404048", "4a4a53", "54545e", "5f5f6a", "6a6a76")
RED_OFF = ramp("5e2621", "6e2e27", "7e362e", "8e4036", "9c4a40")
RED_ON = ramp("9c2a22", "b8352a", "d24434", "e85a45", "ff7a5c")
AMBER_OFF = ramp("3e3024", "4e3c2c", "5e4a36")
AMBER_ON = ramp("c8902c", "e2ae44", "f6cc62", "ffe08a")
COPPER = ramp("6a3c26", "844c30", "a0603c", "bc7650", "d48e64", "e8a87e")
STEEL = ramp("3d424b", "4c525c", "5d646e", "6f7782", "828a96", "949ca8", "a7afba", "bac2cc")


# ------------------------------------------------------------------ Signal Dampener tops (input at y = 0, output at y = 15)
DAMPENER_VARIANTS = {
    "A": dict(name="Stein und Quarz", base=STONE, level=0.56, seed=31, lamp="red", off=ramp("6e322b", "7a3a32", "86443a", "925044", "9e5a4e"), lamps=((2, 4), (2, 7), (2, 10)), knob=True,
              note="Vanilla-nah wie der Verstärker: helle Steinplatte, Leiterbahn mit Quarz-Knopf (3D), drei rote Stufen-Lämpchen links."),
    "B": dict(name="Quarzplatte", base=QUARTZ, level=0.55, seed=47, lamp="copper", off=ramp("a07068", "ac7c74", "b88880", "c4948c", "d0a098"), lamps=((12, 4), (12, 7), (12, 10)), knob=False,
              note="Helle Quarzplatte (Rezept-Quarz), dunkle Bahn, Kupfer-Stufenstifte rechts; flach ohne 3D-Teil."),
    "C": dict(name="Tiefenschiefer und Bernstein", base=DEEP, level=0.52, seed=59, lamp="amber", lamps=((4, 12), (7, 12), (10, 12)), knob=False,
              note="Dunkle Platte, Kupferspule in der Mitte, Bernstein-Lämpchen unten (Nachfolger der alten Textur)."),
}


def dampener_top(v, stage, on):
    spec = DAMPENER_VARIANTS[v]
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, tone(spec["base"], x, y, spec["seed"], spec["level"]))
    layers.snap(img, "Fläche")
    # output arrow, engraved two ramp steps darker (soft, no outline)
    for dy, half in ((12, 0), (13, 1), (14, 2)):
        for x in (7 - half, 8 + half):
            put(img, x, dy, mix(get(img, x, dy), spec["base"][0], 0.35))
    layers.snap(img, "Pfeil")
    lit = RED_ON if on else spec.get("off", RED_OFF)
    for y in range(1, 15):
        dampened = y >= 10  # after the quartz the glow is a step weaker
        for x in (7, 8):
            k = 0.62 if x == 7 else 0.42
            if dampened:
                k -= 0.18
            if y in (1, 14):
                k -= 0.12
            put(img, x, y, pick(lit, k + (0.08 if on else 0.0)))
        for x in (6, 9):  # soft edge of the trace
            put(img, x, y, mix(get(img, x, y), pick(lit, 0.3), 0.30 if on else 0.40))
    layers.snap(img, "Leiterbahn")
    if v == "A":
        for y in range(6, 10):
            for x in range(6, 10):
                t = 0.55 + 0.18 * (1 - (y - 6) / 3.0) - 0.12 * (x - 6) / 3.0
                put(img, x, y, pick(QUARTZ, t))
        layers.snap(img, "Quarz")
    elif v == "B":
        for y in range(6, 10):
            for x in (5, 10):
                put(img, x, y, mix(get(img, x, y), QUARTZ[2], 0.5))
        layers.snap(img, "Fassung")
    else:
        for y in range(5, 10):
            for x in range(4, 12):
                t = 0.62 - 0.1 * (y - 5) / 4.0 + (0.12 if (x + y) % 3 == 0 else 0.0) - (0.18 if x in (4, 11) else 0.0)
                put(img, x, y, pick(COPPER, t))
        layers.snap(img, "Spule")
    for i, (lx, ly) in enumerate(spec["lamps"]):
        on_lamp = i < stage
        if spec["lamp"] == "red":
            r = RED_ON if on_lamp else ramp("6a3a34", "7a463e", "8a5248")
        elif spec["lamp"] == "copper":
            r = COPPER if on_lamp else ramp("a89484", "b4a090", "c0ac9c")
        else:
            r = AMBER_ON if on_lamp else AMBER_OFF
        for dy in (0, 1):
            for dx in (0, 1):
                t = 0.78 - 0.22 * dy - 0.12 * dx
                put(img, lx + dx, ly + dy, pick(r, t))
        if on_lamp:  # soft halo, no ring
            for (hx, hy) in ((lx - 1, ly), (lx - 1, ly + 1), (lx + 2, ly), (lx + 2, ly + 1), (lx, ly - 1), (lx + 1, ly - 1), (lx, ly + 2), (lx + 1, ly + 2)):
                if 0 <= hx < 16 and 0 <= hy < 16:
                    put(img, hx, hy, mix(get(img, hx, hy), r[-1], 0.18))
    layers.snap(img, "Lämpchen")
    return img


def dampener_icon(v):
    """Item sprite: the lit stage-2 top seen from above and a little from the front, the slab edge below (own stone tones)."""
    top = dampener_top(v, 2, True)
    img = new()
    edge = DAMPENER_VARIANTS[v]["base"]
    for y in range(3, 13):
        for x in range(1, 15):
            put(img, x, y, get(top, x, 1 + (y - 3) * 14 // 9))
    for x in range(1, 15):
        put(img, x, 13, pick(edge, 0.45 - 0.02 * (x % 3)))
        put(img, x, 14, pick(edge, 0.22))
    for y in range(3, 15):
        put(img, 0, y, pick(edge, 0.30))
        put(img, 15, y, pick(edge, 0.18))
    for x in range(1, 15):
        put(img, x, 2, pick(edge, 0.62))
    if DAMPENER_VARIANTS[v]["knob"]:
        for x in range(6, 10):
            put(img, x, 7, pick(QUARTZ, 0.95))
            put(img, x, 8, pick(QUARTZ, 0.7))
            put(img, x, 9, pick(QUARTZ, 0.45))
    layers.snap(img, "Fläche")
    return img


# ------------------------------------------------------------------ Double Piston overlays (transparent except the marks)
PISTON_VARIANTS = {
    "A": dict(name="Doppelband", note="Vanilla-Kolben mit einem Eisenband mit zwei Redstone-Punkten (Zeichen für zwei Stufen) und einem Eisenring mit Redstone-Kern auf der Platte."),
    "B": dict(name="Eisenbeschlag", note="Vanilla-Kolben mit Eisen-Eckbeschlägen und Nieten am Körper und einem Eisenrahmen um die Platte."),
}


def side_overlay(v):
    img = new()
    if v == "A":
        for x in range(1, 15):
            put(img, x, 9, pick(STEEL, 0.78 - 0.04 * (x in (1, 14))))
            put(img, x, 10, pick(STEEL, 0.40 - 0.04 * (x in (1, 14))))
        layers.snap(img, "Band")
        for cx in (4, 11):
            put(img, cx, 9, RED_ON[2])
            put(img, cx + 1, 9, RED_ON[1])
            put(img, cx, 10, RED_ON[0])
            put(img, cx + 1, 10, RED_OFF[3])
        layers.snap(img, "Punkte")
    else:
        for (x0, y0, sx, sy) in ((1, 5, 1, 1), (14, 5, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
            for i in range(3):
                put(img, x0 + sx * i, y0, pick(STEEL, 0.75 - 0.12 * i))
                put(img, x0, y0 + sy * i, pick(STEEL, 0.62 - 0.12 * i))
            put(img, x0 + sx, y0 + sy, pick(STEEL, 0.9))
        layers.snap(img, "Beschlag")
    return img


def plate_overlay(v):
    img = new()
    if v == "A":
        for y in range(5, 11):
            for x in range(5, 11):
                ring = x in (5, 10) or y in (5, 10)
                if ring:
                    t = 0.74 if (x == 5 or y == 5) else 0.36
                    if (x, y) in ((5, 5), (10, 10)):
                        t = 0.55
                    put(img, x, y, pick(STEEL, t))
        layers.snap(img, "Ring")
        for (x, y), t in {(7, 7): 0.75, (8, 7): 0.55, (7, 8): 0.5, (8, 8): 0.3}.items():
            put(img, x, y, pick(RED_ON, t))
        layers.snap(img, "Kern")
    else:
        for i in range(1, 15):
            put(img, i, 1, pick(STEEL, 0.78))
            put(img, 1, i, pick(STEEL, 0.66))
            put(img, i, 14, pick(STEEL, 0.34))
            put(img, 14, i, pick(STEEL, 0.42))
        layers.snap(img, "Rahmen")
        for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            put(img, x, y, pick(STEEL, 0.9))
            put(img, x + (1 if x < 8 else -1), y + (1 if y < 8 else -1), pick(STEEL, 0.3))
        layers.snap(img, "Nieten")
    return img


# ------------------------------------------------------------------ models
V = "minecraft:block/"


def side_faces(tex, v1, v2, cull=True):
    """The four side faces of a north-facing piston element (Vanilla's template rotations), texture rows v1..v2."""
    def f(rot, cullface):
        d = {"uv": [0, v1, 16, v2], "texture": tex}
        if rot:
            d["rotation"] = rot
        if cull:
            d["cullface"] = cullface
        return d
    return {"down": f(180, "down"), "up": f(0, "up"), "west": f(270, "west"), "east": f(90, "east")}


def piston_models():
    side, plate, bottom, inner = V + "piston_side", V + "piston_top", V + "piston_bottom", V + "piston_inner"
    textures = {"particle": side, "side": side, "platform": plate, "bottom": bottom, "inside": inner,
                "side_overlay": f"{NS}:block/double_piston_side_overlay", "plate_overlay": f"{NS}:block/double_piston_plate_overlay"}
    full = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {**side_faces("#side", 0, 16),
            "north": {"uv": [0, 0, 16, 16], "texture": "#platform", "cullface": "north"},
            "south": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "south"}}}
    full_o = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {**side_faces("#side_overlay", 0, 16),
              "north": {"uv": [0, 0, 16, 16], "texture": "#plate_overlay", "cullface": "north"}}}
    ext = {"from": [0, 0, 4], "to": [16, 16, 16], "faces": {**side_faces("#side", 4, 16),
           "north": {"uv": [0, 0, 16, 16], "texture": "#inside"},
           "south": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "south"}}}
    ext_o = {"from": [0, 0, 4], "to": [16, 16, 16], "faces": side_faces("#side_overlay", 4, 16)}
    moved = {"from": [0, 0, 0], "to": [16, 16, 8], "faces": {**side_faces("#side", 8, 16),
             "south": {"uv": [0, 0, 16, 16], "texture": "#bottom"}}}
    moved_o = {"from": [0, 0, 0], "to": [16, 16, 8], "faces": side_faces("#side_overlay", 8, 16)}
    body = {"from": [0, 0, 12], "to": [16, 16, 16], "faces": {**side_faces("#side", 4, 8),
            "north": {"uv": [0, 0, 16, 16], "texture": "#inside"}}}
    body_o = {"from": [0, 0, 12], "to": [16, 16, 16], "faces": side_faces("#side_overlay", 4, 8)}
    rod = {"from": [6, 6, 4], "to": [10, 10, 12], "faces": {
        "down": {"uv": [8, 0, 16, 4], "texture": "#side", "rotation": 90}, "up": {"uv": [8, 0, 16, 4], "texture": "#side", "rotation": 270},
        "west": {"uv": [16, 4, 8, 0], "texture": "#side"}, "east": {"uv": [8, 0, 16, 4], "texture": "#side"}}}
    head_plate = {"from": [0, 0, 0], "to": [16, 16, 4], "faces": {
        "down": {"uv": [0, 0, 16, 4], "texture": "#side", "cullface": "down", "rotation": 180},
        "up": {"uv": [0, 0, 16, 4], "texture": "#side", "cullface": "up"},
        "north": {"uv": [0, 0, 16, 16], "texture": "#platform", "cullface": "north"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#platform"},
        "west": {"uv": [0, 0, 16, 4], "texture": "#side", "rotation": 270, "cullface": "west"},
        "east": {"uv": [0, 0, 16, 4], "texture": "#side", "rotation": 90, "cullface": "east"}}}
    head_plate_o = {"from": [0, 0, 0], "to": [16, 16, 4], "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#plate_overlay", "cullface": "north"}}}
    head_rod = {"from": [6, 6, 4], "to": [10, 10, 20], "faces": {
        "down": {"uv": [0, 0, 16, 4], "texture": "#side", "rotation": 90}, "up": {"uv": [0, 0, 16, 4], "texture": "#side", "rotation": 270},
        "west": {"uv": [16, 4, 0, 0], "texture": "#side"}, "east": {"uv": [0, 0, 16, 4], "texture": "#side"}}}

    def model(elements, parent=None):
        m = {}
        if parent:
            m["parent"] = parent
        m["textures"] = dict(textures)
        m["elements"] = elements
        return m
    return {
        "models/block/double_piston.json": model([full, full_o], "minecraft:block/block"),
        "models/block/double_piston_extended.json": model([ext, ext_o]),
        "models/block/double_piston_moved.json": model([moved, moved_o]),
        "models/block/double_piston_arm.json": model([body, body_o, rod]),
        "models/block/double_piston_head.json": model([head_plate, head_plate_o, head_rod]),
    }


FACINGS = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def piston_blockstates():
    base = {}
    for ext, name in ((0, "double_piston"), (1, "double_piston_extended"), (2, "double_piston_moved")):
        for f, rot in FACINGS.items():
            base[f"ext={ext},facing={f}"] = {"model": f"{NS}:block/{name}", **rot}
    part = lambda name: {"variants": {f"facing={f}": {"model": f"{NS}:block/{name}", **rot} for f, rot in FACINGS.items()}}
    return {"blockstates/double_piston.json": {"variants": base},
            "blockstates/double_piston_head.json": part("double_piston_head"),
            "blockstates/double_piston_arm.json": part("double_piston_arm")}


def dampener_models(v):
    out = {}
    for stage in range(4):
        for on in (False, True):
            suffix = f"{stage}{'_on' if on else ''}"
            elements = [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#slab", "cullface": "down"}, "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                **{s: {"uv": [0, 14, 16, 16], "texture": "#slab", "cullface": s} for s in ("north", "south", "west", "east")}}}]
            if DAMPENER_VARIANTS[v]["knob"]:
                elements.append({"from": [6, 2, 6], "to": [10, 3, 10], "faces": {
                    "up": {"uv": [6, 6, 10, 10], "texture": "#knob"},
                    **{s: {"uv": [6, 0, 10, 1], "texture": "#knob"} for s in ("north", "south", "west", "east")}}})
            textures = {"particle": V + "smooth_stone", "slab": V + "smooth_stone", "top": f"{NS}:block/signal_dampener_top_{suffix}"}
            if DAMPENER_VARIANTS[v]["knob"]:
                textures["knob"] = V + "quartz_block_top"
            out[f"models/block/signal_dampener_{suffix}.json"] = {"ambientocclusion": False, "textures": textures, "elements": elements}
    return out


def dampener_blockstate():
    variants = {}
    for f, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270})):
        for stage in range(4):
            for on in (False, True):
                variants[f"facing={f},powered={'true' if on else 'false'},stage={stage}"] = {
                    "model": f"{NS}:block/signal_dampener_{stage}{'_on' if on else ''}", **rot}
    return {"blockstates/signal_dampener.json": {"variants": variants}}


def items():
    return {"items/double_piston.json": {"model": {"type": "minecraft:model", "model": f"{NS}:block/double_piston"}},
            "items/signal_dampener.json": {"model": {"type": "minecraft:model", "model": f"{NS}:item/signal_dampener"}},
            "models/item/signal_dampener.json": {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/signal_dampener"}}}


# ------------------------------------------------------------------ data
def loot(item):
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": f"{NS}:{item}"}], "rolls": 1}],
            "random_sequence": f"{NS}:blocks/{item}"}


def advancement(recipe, items_):
    criteria = {f"has_{i}": {"conditions": {"items": [{"items": f"minecraft:{i}"}]}, "trigger": "minecraft:inventory_changed"} for i in items_}
    criteria["has_the_recipe"] = {"conditions": {"recipes": f"{NS}:{recipe}"}, "trigger": "minecraft:recipe_unlocked"}
    return {"parent": "minecraft:recipes/root", "criteria": criteria,
            "requirements": [["has_the_recipe", *[f"has_{i}" for i in items_]]], "rewards": {"recipes": [f"{NS}:{recipe}"]}}


def data_files():
    return {
        "recipe/double_piston.json": {"type": "minecraft:crafting_shaped", "category": "redstone",
                                      "key": {"Q": "minecraft:quartz", "R": "minecraft:redstone_block", "I": "minecraft:iron_ingot",
                                              "P": "minecraft:piston", "B": "minecraft:iron_block"},
                                      "pattern": ["QRQ", "IPI", "BP "], "result": {"count": 1, "id": f"{NS}:double_piston"}},
        "recipe/signal_dampener.json": {"type": "minecraft:crafting_shaped", "category": "redstone",
                                        "key": {"T": "minecraft:redstone_torch", "Q": "minecraft:quartz", "S": "minecraft:stone"},
                                        "pattern": ["TQT", "SSS"], "result": {"count": 1, "id": f"{NS}:signal_dampener"}},
        "advancement/recipes/redstone/double_piston.json": advancement("double_piston", ["quartz", "redstone_block", "iron_ingot", "piston", "iron_block"]),
        "advancement/recipes/redstone/signal_dampener.json": advancement("signal_dampener", ["redstone_torch", "quartz", "stone"]),
        "loot_table/blocks/double_piston.json": loot("double_piston"),
        "loot_table/blocks/signal_dampener.json": loot("signal_dampener"),
    }


def textures(dv=REPO_DAMPENER, pv=REPO_PISTON):
    out = {"textures/block/double_piston_side_overlay.png": side_overlay(pv),
           "textures/block/double_piston_plate_overlay.png": plate_overlay(pv),
           "textures/item/signal_dampener.png": dampener_icon(dv)}
    for stage in range(4):
        for on in (False, True):
            out[f"textures/block/signal_dampener_top_{stage}{'_on' if on else ''}.png"] = dampener_top(dv, stage, on)
    return out


def repository_jobs():
    jobs = [(RES / rel, img) for rel, img in textures().items()]
    for rel, data in {**piston_models(), **piston_blockstates(), **dampener_models(REPO_DAMPENER), **dampener_blockstate(), **items()}.items():
        jobs.append((RES / rel, data))
    for rel, data in data_files().items():
        jobs.append((DATA / rel, data))
    return jobs


# ------------------------------------------------------------------ preview (Vanilla images only here, never in the repository)
def over(base, top):
    out = base.convert("RGBA").copy()
    out.alpha_composite(top.convert("RGBA"))
    return out


def iso_cube(top, left, right, s=12):
    """Isometric cube: top face, left (south) and right (east) faces, each 16x16, texel size s."""
    w = int(32 * s * 0.866) + 4
    img = Image.new("RGBA", (w, 32 * s + 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = w / 2, 2
    ex, ey = (0.866 * s, 0.5 * s), (-0.866 * s, 0.5 * s)  # +x right-down, +z left-down
    def quad(o, a, b, tex, shade):
        px = tex.convert("RGBA").load()
        for v in range(16):
            for u in range(16):
                c = px[u, v]
                if c[3] == 0:
                    continue
                p0 = (o[0] + a[0] * u + b[0] * v, o[1] + a[1] * u + b[1] * v)
                pts = [p0, (p0[0] + a[0], p0[1] + a[1]), (p0[0] + a[0] + b[0], p0[1] + a[1] + b[1]), (p0[0] + b[0], p0[1] + b[1])]
                d.polygon(pts, fill=tuple(clamp(ch * shade) for ch in c[:3]) + (255,))
    # top: origin at the back corner, u along +x, v along +z
    quad((cx, cy), ex, ey, top, 1.0)
    # left face (south, z = 16): u along +x, v down
    o = (cx + ey[0] * 16, cy + ey[1] * 16)
    quad(o, ex, (0, s), left, 0.80)
    # right face (east, x = 16): u along -z (front to back reversed), v down
    o = (cx + ex[0] * 16 + ey[0] * 16, cy + ex[1] * 16 + ey[1] * 16)
    quad(o, (-ey[0], -ey[1]), (0, s), right, 0.64)
    return img


def iso_slab(top, side_strip, s=12):
    """Thin slab (2 px) in the same projection."""
    w = int(32 * s * 0.866) + 4
    img = Image.new("RGBA", (w, 18 * s + 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = w / 2, 2
    ex, ey = (0.866 * s, 0.5 * s), (-0.866 * s, 0.5 * s)
    def quad(o, a, b, tex, shade, rows):
        px = tex.convert("RGBA").load()
        for v in range(rows):
            for u in range(16):
                c = px[u, v]
                p0 = (o[0] + a[0] * u + b[0] * v, o[1] + a[1] * u + b[1] * v)
                pts = [p0, (p0[0] + a[0], p0[1] + a[1]), (p0[0] + a[0] + b[0], p0[1] + a[1] + b[1]), (p0[0] + b[0], p0[1] + b[1])]
                d.polygon(pts, fill=tuple(clamp(ch * shade) for ch in c[:3]) + (255,))
    quad((cx, cy), ex, ey, top, 1.0, 16)
    strip = side_strip.crop((0, 14, 16, 16)).resize((16, 2))
    quad((cx + ey[0] * 16, cy + ey[1] * 16), ex, (0, s), strip, 0.8, 2)
    quad((cx + ex[0] * 16 + ey[0] * 16, cy + ex[1] * 16 + ey[1] * 16), (-ey[0], -ey[1]), (0, s), strip, 0.64, 2)
    return img


def piston_profile(van, pv, ext, s=10):
    """Side view of the piston facing right (east) seen from the south, three cells wide: the stages of IG31-26."""
    side = over(van["piston_side"], side_overlay(pv)).rotate(-90)  # texture top (front) -> right
    strip = Image.new("RGBA", (48, 16), (0, 0, 0, 0))
    rod_row = van["piston_side"].crop((0, 0, 16, 4))  # the rod shows the wood band like Vanilla's head model
    if ext == 0:
        strip.alpha_composite(side, (0, 0))
    elif ext == 1:
        strip.alpha_composite(side.crop((0, 0, 12, 16)), (0, 0))
        strip.alpha_composite(rod_row, (12, 6))
        strip.alpha_composite(side.crop((12, 0, 16, 16)), (28, 0))
    else:
        strip.alpha_composite(side.crop((0, 0, 12, 16)), (8, 0))
        strip.alpha_composite(rod_row, (20, 6))
        strip.alpha_composite(rod_row.crop((0, 0, 8, 4)), (36, 6))
        strip.alpha_composite(side.crop((12, 0, 16, 16)), (44, 0))
    big = strip.resize((48 * s, 16 * s), Image.NEAREST)
    grid = ImageDraw.Draw(big)
    for c in (1, 2):
        grid.line([(16 * s * c, 0), (16 * s * c, 16 * s)], fill=(255, 255, 255, 70), width=1)
    return big


def data_uri(img, scale=1):
    if scale != 1:
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def vanilla(jar):
    names = ["piston_side", "piston_top", "piston_bottom", "piston_inner", "smooth_stone", "quartz_block_top", "repeater", "comparator"]
    out = {}
    with zipfile.ZipFile(jar) as z:
        for n in names:
            out[n] = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{n}.png"))).convert("RGBA")
    return out


def preview(path, jar):
    van = vanilla(jar)
    cards_d, cards_p = [], []
    for v, spec in DAMPENER_VARIANTS.items():
        tops = "".join(f'<figure><img src="{data_uri(dampener_top(v, st, on), 8)}" alt=""><figcaption>Stufe {st}{" an" if on else ""}</figcaption></figure>'
                       for st in range(4) for on in (False, True) if (st, on) in ((0, False), (1, True), (2, True), (3, True)))
        top2 = dampener_top(v, 2, True)
        iso = iso_slab(top2, van["smooth_stone"])
        share = max(hard_share(dampener_top(v, st, on)) for st in range(4) for on in (False, True))
        cards_d.append(f'''<button class="card" data-group="D" data-code="MACH32-DAEMPFER-{v}" aria-pressed="false">
<h3>Variante {v} – {spec["name"]}</h3><p>{spec["note"]}</p>
<div class="row"><img class="iso" src="{data_uri(iso)}" alt="Signaldämpfer {v} schräg"><figure><img src="{data_uri(dampener_icon(v), 8)}" alt=""><figcaption>Icon</figcaption></figure></div>
<div class="row">{tops}</div><p class="m">harte Übergänge (höchster Wert der 8 Texturen): {share * 100:.1f} %</p></button>''')
    for v, spec in PISTON_VARIANTS.items():
        side = over(van["piston_side"], side_overlay(v))
        plate = over(van["piston_top"], plate_overlay(v))
        cube = iso_cube(plate, side, side)
        prof = "".join(f'<figure><img src="{data_uri(piston_profile(van, v, e, 6))}" alt=""><figcaption>Stufe {e}{["", " (Signal 1-7)", " (Signal 8-15)"][e]}</figcaption></figure>' for e in range(3))
        share = max(hard_share(side), hard_share(plate))
        van_share = max(hard_share(van["piston_side"]), hard_share(van["piston_top"]))
        cards_p.append(f'''<button class="card" data-group="P" data-code="MACH32-KOLBEN-{v}" aria-pressed="false">
<h3>Variante {v} – {spec["name"]}</h3><p>{spec["note"]}</p>
<div class="row"><img class="iso" src="{data_uri(cube)}" alt="Doppelkolben {v}"><figure><img src="{data_uri(side, 8)}" alt=""><figcaption>Seite</figcaption></figure><figure><img src="{data_uri(plate, 8)}" alt=""><figcaption>Platte</figcaption></figure></div>
<div class="col">{prof}</div><p class="m">harte Übergänge: {share * 100:.1f} % (Vanilla-Kolben selbst: {van_share * 100:.1f} %)</p></button>''')
    vcube = iso_cube(van["piston_top"], van["piston_side"], van["piston_side"])
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dämpfer und Doppelkolben</title><style>
:root{{--bg:#f5f4f1;--fg:#1d1d1f;--muted:#5b5b60;--card:#fff;--line:#d9d6cf;--acc:#2f6fdb;}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]){{--bg:#18181b;--fg:#ececef;--muted:#a1a1aa;--card:#232327;--line:#3a3a40;--acc:#7aa7ff;}}}}
:root[data-theme="dark"]{{--bg:#18181b;--fg:#ececef;--muted:#a1a1aa;--card:#232327;--line:#3a3a40;--acc:#7aa7ff;}}
body{{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:16px;max-width:1100px;margin-inline:auto}}
h1{{font-size:1.4rem}} h2{{margin-top:2rem}} p{{color:var(--muted)}}
.grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px}}
.card{{text-align:left;background:var(--card);color:var(--fg);border:2px solid var(--line);border-radius:10px;padding:12px;cursor:pointer;font:inherit;min-width:0}}
.card[aria-pressed="true"]{{border-color:var(--acc);box-shadow:0 0 0 2px var(--acc) inset}}
.card h3{{margin:0 0 4px}} .row{{display:flex;flex-wrap:wrap;gap:8px;align-items:flex-end}} .col{{display:flex;flex-direction:column;gap:6px}}
img{{image-rendering:pixelated;max-width:100%;height:auto}} .iso{{width:150px}} figure{{margin:0}} figcaption{{font-size:12px;color:var(--muted)}}
.m{{font-size:12px}} #code{{position:sticky;bottom:0;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;margin-top:20px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}}
#code code{{font-size:16px;word-break:break-all}} #code button{{font:inherit;padding:6px 12px;border-radius:8px;border:1px solid var(--line);background:var(--bg);color:var(--fg);cursor:pointer}}
</style></head><body>
<h1>IG31-25/26: Signaldämpfer und Doppelkolben – Texturwahl</h1>
<p>Je Gruppe eine Variante antippen; unten steht der Antwortcode. Eingebaut ist vorläufig jeweils Variante A. Endholz-Prozess A8b (weiche Töne, ~7 % harte Übergänge).
Doppelkolben: Vanillas Kolben-Texturen werden im Spiel nur per Verweis genutzt, die Änderung ist eine eigene Overlay-Ebene.</p>
<h2>Signaldämpfer</h2><p>Eingang oben, Ausgang unten (Pfeil). Lämpchen = Stufe 0–3; die Bahn glüht, solange ein Signal herauskommt.</p>
<div class="grid">{"".join(cards_d)}</div>
<h2>Doppelkolben</h2><p>Ein Block. Stufe 1: Kopf +1 (wie Vanilla). Stufe 2: Kolbenkörper +0,5 und Kopf weitere +0,5 bis +1 → drei Blöcke lang (Seitenansicht, Raster = Blockgrenzen).
Zum Vergleich der Vanilla-Kolben: <img class="iso" style="width:70px;vertical-align:middle" src="{data_uri(vcube)}" alt="Vanilla-Kolben"></p>
<div class="grid">{"".join(cards_p)}</div>
<div id="code"><span>Antwortcode:</span> <code id="answer">bitte wählen</code> <button id="copy" type="button">Kopieren</button></div>
<script>
const pick={{}};
document.querySelectorAll('.card').forEach(c=>c.addEventListener('click',()=>{{
 document.querySelectorAll('.card[data-group="'+c.dataset.group+'"]').forEach(o=>o.setAttribute('aria-pressed','false'));
 c.setAttribute('aria-pressed','true'); pick[c.dataset.group]=c.dataset.code;
 document.getElementById('answer').textContent=[pick.D,pick.P].filter(Boolean).join(' ')||'bitte wählen';}}));
document.getElementById('copy').addEventListener('click',()=>{{try{{navigator.clipboard.writeText(document.getElementById('answer').textContent);}}catch(e){{}}}});
</script></body></html>'''
    Path(path).write_text(html, encoding="utf-8")
    print("preview", path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--preview")
    ap.add_argument("--jar")
    a = ap.parse_args()
    if a.preview:
        preview(a.preview, a.jar)
        return 0
    stale = []
    for path, data in repository_jobs():
        if isinstance(data, Image.Image):
            same = path.is_file() and Image.open(path).convert("RGBA").tobytes() == data.convert("RGBA").tobytes()
        else:
            same = path.is_file() and json.loads(path.read_text(encoding="utf-8")) == data
        if a.check:
            if not same:
                stale.append(path.name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(data, Image.Image):
            layers.save(data, path)
            print(f"{path.name:44s} hard steps {hard_share(data) * 100:4.1f} %")
        else:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if a.check:
        print("stale: " + ", ".join(stale[:8]) if stale else "OK mach32")
        return 1 if stale else 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
