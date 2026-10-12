"""Vanilla-derived textures: built, never committed (owner rule 2026-10-12).

A texture whose pixels come from a Mojang texture (a recolour or mask of a sprite read from the Minecraft client
jar) must not live in the public repository. It is generated from the local 26.3 client jar at build time:

  - Gradle (mc26_3/resources.gradle, task generateVanillaDerived263) runs this script before processResources of
    every 26.3 loader and ships build/generated-resources/vanillaDerived as an extra resource root;
  - wiki/generate.py writes them into build/vanilla-derived/resources (or placeholders without a jar) and reads
    them from there like a resource layer;
  - the repository paths are in .gitignore and the --check modes of the texture generators skip them.

The generators stay where the art was made; this file only lists the outputs and calls them.

Run: python3.12 tools/textures/vanilla_derived.py --out DIR [--jar client.jar] [--placeholder-if-missing]
     python3.12 tools/textures/vanilla_derived.py --list
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import importlib
import os
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
NS = 'simplebuilding'
# Where wiki/generate.py and the local tools keep the generated set (build/ is ignored by git).
LOCAL_OUT = ROOT / 'build/vanilla-derived/resources'

# Texture path (under assets/simplebuilding/textures/) -> (size, generator description).
OUTPUTS = {
    'item/blaze_book.png': ((16, 16), 'n41_round3_2026_10_12.blaze_book(2) - Vanilla written_book layout (D9)'),
    'entity/chest/astral_vault.png': ((64, 64), 'n46_round3_2026_10_12.game_chests() - Vanilla ender chest (A2)'),
    'entity/chest/nihil_vault.png': ((64, 64), 'n46_round3_2026_10_12.game_chests() - Vanilla ender chest (N2)'),
    # Older generators (owner 2026-10-12: build them too; Git history is not rewritten):
    'gui/container/auto_smither.png': ((256, 256), 'auto_smither_gui.derived()'),
    'item/color_brush.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'item/color_brush_tip.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'item/color_brush_tip_palette.png': ((16, 16), 'color_brush_2026_10_09.derived()'),
    'block/astral_piston.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/astral_piston_active.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/nihil_piston.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/nihil_piston_active.png': ((16, 16), 'end_pistons_2026_10_03.derived()'),
    'block/astral_rail.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/astral_rail_on.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/nihil_rail.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/nihil_rail_on.png': ((16, 16), 'end_rails_2026_10_04.derived()'),
    'block/sulfur_fire_0.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/sulfur_fire_1.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/copper_fire_0.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/copper_fire_1.png': ((16, 512), 'fires_2026_10_11.derived()'),
    'block/silent_dandelion.png': ((16, 16), 'silent_dandelion_2026_10_03.derived()'),
    'block/soul_lava_still.png': ((16, 320), 'soul_lava_2026_10_09.derived()'),
    'block/soul_lava_flow.png': ((32, 512), 'soul_lava_2026_10_09.derived()'),
    'item/soul_lava_bucket.png': ((16, 16), 'soul_lava_2026_10_09.derived()'),
    'item/reinforced_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived()'),
    'item/netherite_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived()'),
    'item/enderite_shulker_shell.png': ((16, 16), 'shulker_shell_textures.derived()'),
    'entity/rare_shulker/reinforced.png': ((64, 64), 'shulker_shell_textures.derived()'),
    'entity/rare_shulker/netherite.png': ((64, 64), 'shulker_shell_textures.derived()'),
    'entity/rare_shulker/enderite.png': ((64, 64), 'shulker_shell_textures.derived()'),
    'item/music_disc_voidline.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_voidline_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_driftwood_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_daybreak_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_b_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_track_3.png': ((16, 16), 'music_disc_textures.derived()'),
    'item/music_disc_brimstone_track_4.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/jukebox_amplifier_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/jukebox_amplifier_top.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/note_amplifier_side.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/note_amplifier_top.png': ((16, 16), 'music_disc_textures.derived()'),
    'block/placed_egg_white.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/placed_egg_blue.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/placed_egg_brown.png': ((16, 16), 'placed_egg_textures.derived()'),
    'block/standing_stick.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_bone.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_blaze_rod.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'block/standing_breeze_rod.png': ((16, 16), 'standing_rods_2026_10_04.derived()'),
    'item/reinforced_chest_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_chest_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_chest_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/reinforced_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_furnace_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/reinforced_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_hopper_minecart.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/reinforced_chest_boat_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/reinforced_chest_raft_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_chest_boat_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/netherite_chest_raft_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_chest_boat_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'item/enderite_chest_raft_overlay.png': ((16, 16), 'vehicles_2026_10_10.derived()'),
    'block/enderite_crucible_side.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_crucible_top.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_barrel_side.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_barrel_top.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_barrel_top_open.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_barrel_bottom.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'block/enderite_barrel_flange.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_water_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_lava_bucket_0.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_water_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_lava_bucket_1.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_water_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_lava_bucket_2.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_water_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/copper_lava_bucket_3.png': ((16, 16), 'crucible_art_v2_2026_10_05.derived()'),
    'item/enderite_bucket.png': ((16, 320), 'crucible_art_v2_2026_10_05.derived()'),
}
# Older generators with a derived(jar) -> {texture path: Image or PNG bytes} function.
OLDER_GENERATORS = ('auto_smither_gui', 'color_brush_2026_10_09', 'end_pistons_2026_10_03', 'end_rails_2026_10_04',
                    'fires_2026_10_11', 'silent_dandelion_2026_10_03', 'soul_lava_2026_10_09', 'shulker_shell_textures',
                    'music_disc_textures', 'placed_egg_textures', 'standing_rods_2026_10_04', 'vehicles_2026_10_10',
                    'crucible_art_v2_2026_10_05')
# Probe file every usable jar contains.
PROBE = 'assets/minecraft/textures/entity/chest/ender.png'


def find_jar(explicit: str | None = None) -> Path | None:
    """The 26.3 client jar: explicit, SB_CLIENT_JAR, or the Loom / NeoForm cache under GRADLE_USER_HOME."""
    home = Path(os.environ.get('GRADLE_USER_HOME') or Path.home() / '.gradle')
    candidates = [explicit, os.environ.get('SB_CLIENT_JAR')]
    candidates += sorted(glob.glob(str(home / 'caches/fabric-loom/26.3/minecraft-client.jar')))
    candidates += sorted(glob.glob(str(home / 'caches/neoformruntime/artifacts/minecraft_26.3_client.jar')))
    for c in candidates:
        if not c or not Path(c).is_file():
            continue
        try:
            with zipfile.ZipFile(c) as z:
                z.getinfo(PROBE)
            return Path(c)
        except (OSError, KeyError, zipfile.BadZipFile):
            continue
    return None


def extract_vanilla_textures(jar: Path) -> None:
    """build/vanilla-textures: the Vanilla textures as files, which the crucible/barrel/bucket generators read at
    import time (build/ is not committed). Extracted once."""
    base = ROOT / 'build/vanilla-textures'
    if (base / 'block/iron_block.png').exists():
        return
    prefix = 'assets/minecraft/textures/'
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith(prefix) and name.endswith('.png'):
                target = base / name[len(prefix):]
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(name))


def images(jar: Path) -> dict:
    """Texture path -> RGBA image, drawn from the given jar."""
    os.environ['SB_CLIENT_JAR'] = str(jar)
    if str(HERE) not in sys.path:
        sys.path.append(str(HERE))
    import n41_round3_2026_10_12 as n41
    import n46_round3_2026_10_12 as n46
    chests = n46.game_chests()
    out = {'item/blaze_book.png': n41.blaze_book(2),
           'entity/chest/astral_vault.png': chests['astral'],
           'entity/chest/nihil_vault.png': chests['nihil']}
    extract_vanilla_textures(jar)
    for name in OLDER_GENERATORS:
        out.update(importlib.import_module(name).derived(jar))
    assert set(out) == set(OUTPUTS), 'OUTPUTS and images() disagree'
    return out


# Wiki 3D renders (wiki/assets/textures/render/<name>.png, block renders as block/<name>, 128x128) that show a
# Vanilla-derived texture (or draw over a Mojang sprite): not committed, placeholders without the renderer.
WIKI_RENDERS = {'astral_vault': 128, 'nihil_vault': 128, 'enderite_barrel': 128, 'enderite_crucible': 128,
                'astral_piston': 128, 'nihil_piston': 128, 'jukebox_amplifier': 128, 'note_amplifier': 128,
                'enderite_chest_boat': 128, 'netherite_chest_boat': 128, 'reinforced_chest_boat': 128,
                'enderite_chest_minecart': 128, 'enderite_furnace_minecart': 128, 'enderite_hopper_minecart': 128,
                'netherite_chest_minecart': 128, 'netherite_furnace_minecart': 128, 'netherite_hopper_minecart': 128,
                'reinforced_chest_minecart': 128, 'reinforced_furnace_minecart': 128,
                'reinforced_hopper_minecart': 128, 'block/astral_rail': 128, 'block/nihil_rail': 128,
                'block/copper_fire': 128, 'block/sulfur_fire': 128, 'block/placed_egg': 128,
                'block/potted_silent_dandelion': 128, 'block/silent_dandelion': 128, 'block/standing_rod': 128}


def placeholder_png(key: str, w: int, h: int) -> bytes:
    """A deterministic stand-in (no Mojang pixels, no Pillow needed): grey checker, one tint per key."""
    import struct
    import zlib
    tint = hashlib.sha256(key.encode()).digest()
    a = bytes((96 + tint[0] % 64, 96 + tint[1] % 64, 96 + tint[2] % 64, 255))
    b = bytes((a[0] // 2, a[1] // 2, a[2] // 2, 255))
    step = max(1, w // 8)
    raw = b''.join(b'\0' + b''.join(a if ((x // step) + (y // step)) % 2 else b for x in range(w)) for y in range(h))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# Outputs that are byte-identical in the real set (one texture for all six faces): their placeholders must be
# identical too, or the wiki texture index (which merges equal copies) would differ between jar and no-jar runs.
SAME_AS = {'block/jukebox_amplifier_top.png': 'block/jukebox_amplifier_side.png',
           'block/note_amplifier_top.png': 'block/note_amplifier_side.png'}


def placeholder(rel: str) -> bytes:
    (w, h), _ = OUTPUTS[rel]
    return placeholder_png(SAME_AS.get(rel, rel), w, h)


def write(out_root: Path, jar: Path | None, placeholders: bool = False) -> str:
    """Writes assets/simplebuilding/textures/<rel> under out_root; returns 'jar' or 'placeholder'."""
    if jar is None and not placeholders:
        raise SystemExit('vanilla_derived: 26.3 client jar not found (Gradle cache fabric-loom/26.3 or '
                         'neoformruntime, SB_CLIENT_JAR or -PsbClientJar)')
    art = {rel: _png(im) for rel, im in images(jar).items()} if jar is not None else \
        {rel: placeholder(rel) for rel in OUTPUTS}
    base = out_root / 'assets' / NS / 'textures'
    for rel, data in art.items():
        p = base / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        if not p.exists() or p.read_bytes() != data:
            p.write_bytes(data)
    return 'jar' if jar is not None else 'placeholder'


def _png(im) -> bytes:
    if isinstance(im, bytes):  # a generator that encodes its own PNG bytes (e.g. optimized)
        return im
    import io
    buf = io.BytesIO()
    im.save(buf, format='PNG')
    return buf.getvalue()


def ensure_local() -> tuple[Path, str]:
    """For local tools (wiki): the set under build/vanilla-derived/resources; placeholders without a jar or Pillow."""
    try:
        import PIL  # noqa: F401
        jar = find_jar()
    except ImportError:
        jar = None
    return LOCAL_OUT, write(LOCAL_OUT, jar, placeholders=True)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path)
    ap.add_argument('--jar')
    ap.add_argument('--placeholder-if-missing', action='store_true')
    ap.add_argument('--list', action='store_true')
    a = ap.parse_args()
    if a.list or not a.out:
        for rel, (size, how) in OUTPUTS.items():
            print(f'assets/{NS}/textures/{rel}  {size[0]}x{size[1]}  {how}')
        return
    jar = find_jar(a.jar)
    mode = write(a.out, jar, a.placeholder_if_missing)
    print(f'vanilla_derived: {len(OUTPUTS)} textures ({mode}{", " + str(jar) if jar else ""}) -> {a.out}')


if __name__ == '__main__':
    main()
