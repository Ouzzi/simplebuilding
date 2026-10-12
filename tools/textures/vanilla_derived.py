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
}
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
    assert set(out) == set(OUTPUTS), 'OUTPUTS and images() disagree'
    return out


# Wiki 3D renders (wiki/assets/textures/render/<name>.png, 128x128) that show a Vanilla-derived texture.
WIKI_RENDERS = {'astral_vault': 128, 'nihil_vault': 128}


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


def placeholder(rel: str) -> bytes:
    (w, h), _ = OUTPUTS[rel]
    return placeholder_png(rel, w, h)


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
