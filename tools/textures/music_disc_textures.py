"""Usage: python tools/textures/music_disc_textures.py [--preview]

Music discs and speakers (owner 2026-10-03, McVersion.MUSIC_DISCS).

Discs: vanilla's disc layout (item/music_disc_13.png, read from the 26.3 client jar: the 15 x 10 disc,
its outline, grooves and lower rim keep their exact pixels). Like vanilla's own discs (Pigstep's red body,
Creator's teal body) each disc gets a body tint of its dimension and a label of its own in the 5 x 3 label
field around the hole - not just recolored, every label has its own little motif:
  Voidline  (End)         purple-black body, an ender eye of end-stone yellow and purpur
  Driftwood (Overworld 1) blue-grey body, a rain drop in two blues
  Daybreak  (Overworld 2) grey body, a rising sun with rays (orange, yellow, pink)
  Brimstone (Nether)      blackstone-red body, a flame (lava orange, yellow, crimson)
The B-sides keep the disc and swap the label's two main colors (inverted label) and carry two light
scratches across the body.

Speakers: the wood of vanilla's note block (sides) and jukebox top (top/bottom), read from the jar. The
sides get a round speaker membrane - a dark wooden rim, a ring and a dome in the material's colors
(Astralit pink from the owner's astralit_dust.png, Nihilit teal-blue from nihilith_shard.png) with one
highlight pixel; the top keeps the jukebox frame and its disc slot is inlaid with the material.

Output (26.3-only): mc26_3/overlay/resources/assets/simplebuilding/textures/item/music_disc_<song>.png and
.../textures/block/jukebox_amplifier_side/_top.png and note_amplifier_side/_top.png (the amplifiers, formerly speakers).
--preview writes C:/Users/o_o/code/minecraft-mods/previews/schallplatten-vorschau.png and
lautsprecher-vorschau.png (16x, labeled).
"""
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

JAR = os.path.expanduser(r"~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar")
ITEM_OUT = "mc26_3/overlay/resources/assets/simplebuilding/textures/item"
BLOCK_OUT = "mc26_3/overlay/resources/assets/simplebuilding/textures/block"
PREVIEWS = r"C:/Users/o_o/code/minecraft-mods/previews"

# Roles of music_disc_13.png by color.
ROLE = {(33, 33, 33): "outline", (64, 64, 64): "body", (81, 81, 81): "groove", (38, 38, 38): "rim",
        (17, 17, 17): "bottom", (255, 216, 0): "label", (255, 255, 255): "label"}
LABEL_X, LABEL_Y = 5, 6  # top-left of the 5 x 3 label field; the hole sits at (7, 7)

# Body tints: outline, body, groove, rim, bottom.
BODIES = {
    "voidline": [(29, 20, 38), (52, 38, 66), (71, 53, 89), (37, 27, 49), (14, 9, 20)],
    "driftwood": [(32, 35, 41), (61, 66, 76), (79, 86, 98), (37, 40, 47), (16, 18, 22)],
    "daybreak": [(35, 33, 32), (66, 62, 60), (85, 80, 77), (40, 37, 36), (18, 16, 15)],
    "brimstone": [(58, 24, 22), (81, 33, 30), (101, 45, 39), (51, 20, 18), (25, 8, 8)],
}

# Labels: 5 x 3 grid, "." = body, "H" = hole, "1"/"2"/"3" = label colors.
LABELS = {
    # Ender eye: end-stone lids around a purpur iris.
    "voidline": (["2", "2", "2", "2", "2"], ["2", "1", "H", "1", "2"], [".", "2", "2", "2", "."],
                 {"1": (169, 102, 176), "2": (221, 223, 165), "3": (240, 240, 200)}),
    # Rain drop: a point on top, widening below.
    "driftwood": ([".", ".", "1", ".", "."], [".", "1", "H", "1", "."], ["2", "1", "3", "1", "2"],
                  {"1": (86, 128, 178), "2": (54, 82, 128), "3": (190, 214, 236)}),
    # Rising sun: rays on top, the disc of the sun below.
    "daybreak": (["3", ".", "3", ".", "3"], ["1", "2", "H", "2", "1"], [".", "1", "1", "1", "."],
                 {"1": (250, 146, 52), "2": (255, 222, 96), "3": (246, 120, 168)}),
    # Flame: two tips, a hot core, glowing base.
    "brimstone": ([".", "2", ".", "2", "."], ["2", "1", "H", "1", "2"], ["1", "3", "1", "3", "1"],
                  {"1": (244, 132, 30), "2": (255, 214, 76), "3": (178, 34, 30)}),
}
HOLE = (20, 16, 16)
SCRATCHES = [(2, 6), (3, 7), (4, 8), (11, 8), (12, 9)]
#: Track 3 and 4 (owner 2026-10-04): their own scratch patterns, all on body pixels.
SCRATCHES_3 = [(2, 8), (3, 9), (11, 7), (12, 6)]
SCRATCHES_4 = [(5, 10), (6, 10), (8, 4), (9, 4)]
SONGS = ["voidline", "driftwood", "daybreak", "brimstone"]

ASTRALIT = [(126, 57, 101), (160, 106, 142), (198, 144, 179), (226, 196, 214)]
NIHILITH = [(14, 40, 52), (38, 77, 98), (75, 150, 179), (150, 214, 222)]


def jar_image(path):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(path))).convert("RGBA")


def disc(base, song, track):
    """Track 1 = A-side, 2 = B-side (label inverted), 3/4 = label colors rotated one/the other way."""
    body = dict(zip(["outline", "body", "groove", "rim", "bottom"], BODIES[song]))
    img = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for y in range(base.height):
        for x in range(base.width):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue
            role = ROLE[(r, g, b)]
            img.putpixel((x, y), (body["body"] if role == "label" else body[role]) + (255,))
    rows = LABELS[song]
    colors = dict(rows[3])
    if track == 2:
        colors["1"], colors["2"] = colors["2"], colors["1"]
    elif track == 3:
        colors["1"], colors["2"], colors["3"] = colors["2"], colors["3"], colors["1"]
    elif track == 4:
        colors["1"], colors["2"], colors["3"] = colors["3"], colors["1"], colors["2"]
    for dy in range(3):
        for dx in range(5):
            cell = rows[dy][dx]
            pos = (LABEL_X + dx, LABEL_Y + dy)
            if cell == "H":
                img.putpixel(pos, HOLE + (255,))
            elif cell != ".":
                img.putpixel(pos, colors[cell] + (255,))
    scratches = {2: SCRATCHES, 3: SCRATCHES_3, 4: SCRATCHES_4}.get(track, [])
    light = tuple(min(255, c + 70) for c in body["groove"])
    for pos in scratches:
        img.putpixel(pos, light + (255,))
    return img


def speaker_side(note, pal):
    img = note.copy()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 0.8:
                c = pal[1]  # dust cap in the middle of the cone
            elif d <= 2.2:
                c = pal[2]
            elif d <= 3.6:
                c = pal[1]
            elif d <= 4.6:
                c = pal[0]
            elif d <= 5.6:
                c = (41, 40, 32)
            else:
                continue
            img.putpixel((x, y), c + (255,))
    img.putpixel((6, 6), pal[3] + (255,))
    return img


def speaker_end(top, pal):
    img = top.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[:3] == (24, 21, 20):
                img.putpixel((x, y), (pal[0] if (x + y) % 2 else pal[1]) + (255,))
    return img


def outputs():
    base = jar_image("assets/minecraft/textures/item/music_disc_13.png")
    note = jar_image("assets/minecraft/textures/block/note_block.png")
    top = jar_image("assets/minecraft/textures/block/jukebox_top.png")
    discs = {}
    for song in SONGS:
        discs["music_disc_" + song] = disc(base, song, 1)
        discs["music_disc_" + song + "_b_side"] = disc(base, song, 2)
        # Track 3/4 exist as items only once the owner imports music for them (tools/audio/import_discs.py);
        # the textures are ready beforehand.
        discs["music_disc_" + song + "_track_3"] = disc(base, song, 3)
        discs["music_disc_" + song + "_track_4"] = disc(base, song, 4)
    # Speakers settled 2026-10-04 (owner): note block with the material radially in its holes, one texture for all
    # six faces - drawn by speaker_settled_2026_10_04.py (speaker_side/speaker_end above are the earlier design).
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    saved_argv = sys.argv[:]          # the settled module reads/rewrites sys.argv on import
    try:
        import speaker_settled_2026_10_04 as settled
    finally:
        sys.argv = saved_argv
    blocks = settled.textures(note.convert("RGBA").crop((0, 0, 16, 16)))
    return discs, blocks


def label(draw, x, y, text):
    draw.rectangle([x, y, x + 9 * len(text) + 6, y + 16], fill=(0, 0, 0, 200))
    draw.text((x + 3, y + 2), text, fill=(255, 255, 255, 255))


def sheet(cells, path, title):
    scale, pad = 16, 24
    w = len(cells) * (16 * scale + pad) + pad
    img = Image.new("RGBA", (w, 16 * scale + 2 * pad + 40), (198, 198, 198, 255))
    draw = ImageDraw.Draw(img)
    draw.text((pad, 6), title, fill=(0, 0, 0, 255))
    for i, (tag, name, tex) in enumerate(cells):
        x = pad + i * (16 * scale + pad)
        img.alpha_composite(tex.resize((16 * scale, 16 * scale), Image.NEAREST), (x, pad + 16))
        label(draw, x, pad + 16, tag)
        draw.text((x, pad + 22 + 16 * scale), name, fill=(0, 0, 0, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def cube(side, top, scale=12):
    """Small isometric cube preview (top + two sides, sides shaded)."""
    s = 16 * scale
    out = Image.new("RGBA", (2 * s, 2 * s), (0, 0, 0, 0))
    big_top = top.resize((s, s), Image.NEAREST).rotate(45, expand=True, resample=Image.NEAREST)
    big_top = big_top.resize((big_top.width, big_top.height // 2), Image.NEAREST)
    out.alpha_composite(big_top, ((2 * s - big_top.width) // 2, 0))
    left = side.resize((s, s), Image.NEAREST)
    right = Image.eval(left, lambda v: v)
    from PIL import ImageEnhance
    left = ImageEnhance.Brightness(left).enhance(0.8)
    right = ImageEnhance.Brightness(right).enhance(0.6)
    h = big_top.height
    w = big_top.width // 2
    left = left.transform((w, s + h // 2), Image.AFFINE, (s / w, 0, 0, -(h / 2) / w, 1, 0), Image.NEAREST)
    right = right.transform((w, s + h // 2), Image.AFFINE, (s / w, 0, 0, (h / 2) / w, 1, -h / 2), Image.NEAREST)
    x0 = (2 * s - big_top.width) // 2
    out.alpha_composite(left, (x0, h // 2))
    out.alpha_composite(right, (x0 + w, h // 2))
    return out


def main():
    discs, blocks = outputs()
    os.makedirs(ITEM_OUT, exist_ok=True)
    os.makedirs(BLOCK_OUT, exist_ok=True)
    for name, tex in discs.items():
        tex.save(os.path.join(ITEM_OUT, name + ".png"))
    for name, tex in blocks.items():
        tex.save(os.path.join(BLOCK_OUT, name + ".png"))
    if "--preview" in sys.argv:
        vanilla = [("V1", "vanilla 13", jar_image("assets/minecraft/textures/item/music_disc_13.png")),
                   ("V2", "vanilla pigstep", jar_image("assets/minecraft/textures/item/music_disc_pigstep.png"))]
        tags = "ABCDEFGHIJKLMNOP"
        cells = [(tags[i], name.replace("music_disc_", ""), tex) for i, (name, tex) in enumerate(discs.items())]
        sheet(vanilla + cells, os.path.join(PREVIEWS, "schallplatten-vorschau.png"),
              "Schallplatten je 4 Tracks (A-D Voidline End, E-H Driftwood OW1, I-L Daybreak OW2, M-P Brimstone Nether; Track 1, B-Seite, Track 3, Track 4)")
        note = jar_image("assets/minecraft/textures/block/note_block.png")
        top = jar_image("assets/minecraft/textures/block/jukebox_top.png")
        cells = [("V1", "note_block", note), ("V2", "jukebox_top", top)]
        cells += [(t, n, blocks[n]) for t, n in zip("ABCD", blocks)]
        path = os.path.join(PREVIEWS, "lautsprecher-vorschau.png")
        sheet(cells, path, "Lautsprecher (A/B Astralit: Seite/oben, C/D Nihilit: Seite/oben) + Wuerfel E (Astralit), F (Nihilit)")
        img = Image.open(path)
        cubes = [cube(blocks["jukebox_amplifier_side"], blocks["jukebox_amplifier_top"]),
                 cube(blocks["note_amplifier_side"], blocks["note_amplifier_top"])]
        out = Image.new("RGBA", (max(img.width, 2 * cubes[0].width + 72), img.height + cubes[0].height + 40), (198, 198, 198, 255))
        out.alpha_composite(img.convert("RGBA"), (0, 0))
        draw = ImageDraw.Draw(out)
        for i, c in enumerate(cubes):
            x = 24 + i * (c.width + 24)
            out.alpha_composite(c, (x, img.height + 20))
            label(draw, x, img.height + 20, "EF"[i])
        out.save(path)
        print("previews written")
    print(f"{len(discs)} discs, {len(blocks)} speaker textures written")


if __name__ == "__main__":
    main()
