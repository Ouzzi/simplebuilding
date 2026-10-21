"""
Layer sources of textures (docs/ai/PRINZIPIEN.md A8 b, docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md).

Every texture may carry its layers next to the PNG as <name>.layers.json; the PNG is their composition.
The wiki pixel editor (wiki/assets/pixeleditor*.js, dev mirror) reads and writes the same format and composes
with the same integer rules, so a pair exported from the editor passes `check` unchanged.

    python tools/textures/layers.py check [files...]      every tracked *.layers.json (or the given ones)
    python tools/textures/layers.py compose X.layers.json [--out X.png]
    python tools/textures/layers.py import X.png [--name Base] [--force]

Pure Python (zlib only): runs wherever `gradlew check` runs, Pillow is not needed.
"""
from __future__ import annotations

import argparse
import json
import re
import struct
import subprocess
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FORMAT = "sb-layers"
SUFFIX = ".layers.json"
COLOR = re.compile(r"^#[0-9a-f]{8}$")


# ---------------------------------------------------------------- PNG

def png_read(data: bytes) -> tuple[int, int, bytearray]:
    """(width, height, RGBA bytes) of a non-interlaced PNG (colour types 0/2/3/4/6, bit depth 1-16)."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, palette, trns = 8, bytearray(), b"", b""
    width = height = depth = ctype = 0
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if interlace:
                raise ValueError("interlaced PNG is not supported")
        elif kind == b"PLTE":
            palette = body
        elif kind == b"tRNS":
            trns = body
        elif kind == b"IDAT":
            idat += body
        elif kind == b"IEND":
            break
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bpp = max(1, channels * depth // 8)
    stride = (width * channels * depth + 7) // 8
    raw = zlib.decompress(bytes(idat))
    rows, prev, p = [], bytearray(stride), 0
    for _ in range(height):
        f = raw[p]
        line = bytearray(raw[p + 1:p + 1 + stride])
        p += 1 + stride
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + b) & 255
            elif f == 3:
                line[i] = (line[i] + ((a + b) >> 1)) & 255
            elif f == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    out = bytearray(width * height * 4)
    maxv = (1 << depth) - 1
    for y, line in enumerate(rows):
        for x in range(width):
            if depth == 16:
                vals = [line[(x * channels + k) * 2] for k in range(channels)]
                raw16 = [line[(x * channels + k) * 2] << 8 | line[(x * channels + k) * 2 + 1] for k in range(channels)]
            elif depth == 8:
                vals = [line[x * channels + k] for k in range(channels)]
                raw16 = vals
            else:  # 1/2/4 bit: grey or palette, one channel
                bit = x * depth
                v = (line[bit >> 3] >> (8 - depth - (bit & 7))) & maxv
                vals = [v if ctype == 3 else v * 255 // maxv]
                raw16 = [v]
            o = (y * width + x) * 4
            if ctype == 3:
                i = raw16[0]
                r, g, b = palette[i * 3:i * 3 + 3]
                a = trns[i] if i < len(trns) else 255
            elif ctype in (0, 4):
                r = g = b = vals[0]
                a = vals[1] if ctype == 4 else 255
                if ctype == 0 and len(trns) >= 2 and raw16[0] == struct.unpack(">H", trns[:2])[0]:
                    a = 0
            else:
                r, g, b = vals[:3]
                a = vals[3] if ctype == 6 else 255
                if ctype == 2 and len(trns) >= 6 and tuple(raw16) == struct.unpack(">HHH", trns[:6]):
                    a = 0
            out[o:o + 4] = bytes((r, g, b, a))
    return width, height, out


def png_write(width: int, height: int, rgba: bytes) -> bytes:
    """RGBA 8-bit PNG, filter 0 per row."""
    rows = b"".join(b"\x00" + bytes(rgba[y * width * 4:(y + 1) * width * 4]) for y in range(height))

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b""))


# ---------------------------------------------------------------- format

def digits_for(count: int) -> int:
    return 2 if count <= 256 else 3 if count <= 4096 else 4


def validate(doc: dict) -> list[str]:
    """Problems of a layers document (empty when it is well-formed)."""
    if not isinstance(doc, dict) or doc.get("format") != FORMAT or doc.get("version") != 1:
        return [f'format must be "{FORMAT}", version 1']
    problems = []
    w, h, d = doc.get("width"), doc.get("height"), doc.get("digits")
    palette = doc.get("palette")
    if not (isinstance(w, int) and isinstance(h, int) and w > 0 and h > 0):
        return ["width/height must be positive integers"]
    if d not in (2, 3, 4):
        return ["digits must be 2, 3 or 4"]
    if not isinstance(palette, list) or not all(isinstance(c, str) and COLOR.match(c) for c in palette):
        return ['palette must be a list of "#rrggbbaa" (lower case)']
    if len(palette) > 16 ** d:
        problems.append(f"palette has {len(palette)} colours, more than {d} digits can address")
    layers = doc.get("layers")
    if not isinstance(layers, list) or not layers:
        return problems + ["layers must be a non-empty list"]
    for n, layer in enumerate(layers):
        tag = f"layer {n} ({layer.get('name') if isinstance(layer, dict) else '?'})"
        if not isinstance(layer, dict):
            problems.append(f"{tag}: not an object")
            continue
        if not isinstance(layer.get("name"), str):
            problems.append(f"{tag}: name missing")
        if not isinstance(layer.get("visible"), bool):
            problems.append(f"{tag}: visible must be true/false")
        op = layer.get("opacity")
        if not (isinstance(op, int) and not isinstance(op, bool) and 0 <= op <= 100):
            problems.append(f"{tag}: opacity must be an integer 0-100")
        if layer.get("blend", "normal") != "normal":
            problems.append(f"{tag}: only blend \"normal\" is supported")
        rows = layer.get("rows")
        if not isinstance(rows, list) or len(rows) != h:
            problems.append(f"{tag}: needs {h} rows")
            continue
        for y, row in enumerate(rows):
            if not isinstance(row, str) or len(row) != w * d:
                problems.append(f"{tag}: row {y} must have {w * d} characters")
                break
            for x in range(w):
                cell = row[x * d:(x + 1) * d]
                if cell == "." * d:
                    continue
                if not re.fullmatch(r"[0-9a-f]+", cell) or int(cell, 16) >= len(palette):
                    problems.append(f"{tag}: row {y} column {x}: {cell!r} is no palette index")
                    break
    return problems


def color_bytes(color: str) -> tuple[int, int, int, int]:
    v = int(color[1:], 16)
    return (v >> 24) & 255, (v >> 16) & 255, (v >> 8) & 255, v & 255


def layer_pixels(doc: dict, layer: dict) -> list[tuple[int, int, int, int] | None]:
    d, w = doc["digits"], doc["width"]
    pal = [color_bytes(c) for c in doc["palette"]]
    out = []
    for row in layer["rows"]:
        for x in range(w):
            cell = row[x * d:(x + 1) * d]
            out.append(None if cell == "." * d else pal[int(cell, 16)])
    return out


def rnd(x: float) -> int:
    """floor(x + 0.5): the same rounding as Math.floor(x + 0.5) in pixeleditor-core.js."""
    return int(x + 0.5) if x >= 0 else -int(-x + 0.5)


def compose(doc: dict) -> bytearray:
    """RGBA bytes of the visible layers, bottom first, source-over (see the plan)."""
    w, h = doc["width"], doc["height"]
    out = bytearray(w * h * 4)
    for layer in doc["layers"]:
        if not layer["visible"] or layer["opacity"] <= 0:
            continue
        op = layer["opacity"]
        for i, px in enumerate(layer_pixels(doc, layer)):
            if px is None:
                continue
            sa = rnd(px[3] * op / 100)
            if sa <= 0:
                continue
            o = i * 4
            if sa >= 255:
                out[o:o + 4] = bytes((px[0], px[1], px[2], 255))
                continue
            da = out[o + 3]
            rest = da * (255 - sa) / 255
            oa = sa + rest
            for k in range(3):
                out[o + k] = rnd((px[k] * sa + out[o + k] * rest) / oa)
            out[o + 3] = rnd(oa)
    return out


def from_rgba(width: int, height: int, rgba: bytes, name: str = "Base") -> dict:
    """A one-layer document of an image; palette in order of first appearance, fully transparent pixels empty."""
    palette: dict[str, int] = {}
    cells = []
    for i in range(width * height):
        r, g, b, a = rgba[i * 4:i * 4 + 4]
        if a == 0:
            cells.append(None)
            continue
        key = f"#{r:02x}{g:02x}{b:02x}{a:02x}"
        cells.append(palette.setdefault(key, len(palette)))
    d = digits_for(len(palette))
    rows = ["".join("." * d if c is None else format(c, f"0{d}x") for c in cells[y * width:(y + 1) * width])
            for y in range(height)]
    return {"format": FORMAT, "version": 1, "width": width, "height": height, "digits": d,
            "palette": list(palette), "layers": [{"name": name, "visible": True, "opacity": 100,
                                                  "blend": "normal", "rows": rows}]}


def dumps(doc: dict) -> str:
    """Stable text: header fields on one line each, one row per line."""
    head = {k: doc[k] for k in ("format", "version", "width", "height", "digits")}
    lines = ["{"] + [f'  {json.dumps(k)}: {json.dumps(v)},' for k, v in head.items()]
    lines.append('  "palette": [' + ", ".join(json.dumps(c) for c in doc["palette"]) + "],")
    lines.append('  "layers": [')
    for n, layer in enumerate(doc["layers"]):
        meta = ", ".join(f"{json.dumps(k)}: {json.dumps(layer[k], ensure_ascii=False)}"
                         for k in ("name", "visible", "opacity", "blend") if k in layer)
        rows = ",\n".join("      " + json.dumps(r) for r in layer["rows"])
        lines.append("    {" + meta + ', "rows": [\n' + rows + "\n    ]}" + ("," if n < len(doc["layers"]) - 1 else ""))
    lines += ["  ]", "}"]
    return "\n".join(lines) + "\n"


def png_path(layers_path: Path) -> Path:
    return layers_path.with_name(layers_path.name[:-len(SUFFIX)] + ".png")


def layers_path(png: Path) -> Path:
    return png.with_name(png.name[:-4] + SUFFIX)


# ---------------------------------------------------------------- commands

def ignored(path: Path) -> bool:
    try:
        return subprocess.run(["git", "-C", str(ROOT), "check-ignore", "-q", str(path)],
                              capture_output=True, timeout=30).returncode == 0
    except (OSError, subprocess.SubprocessError):
        return False


def tracked() -> list[Path]:
    try:
        out = subprocess.run(["git", "-C", str(ROOT), "ls-files", "-z", "--", "*" + SUFFIX],
                             capture_output=True, timeout=60).stdout.decode("utf-8", "replace")
    except (OSError, subprocess.SubprocessError):
        return []
    # wiki/assets/textures/sheets holds copies for the editor (wiki/textures.py), their PNG may sit elsewhere
    return [ROOT / p for p in out.split("\0") if p and not p.startswith("wiki/")]


def check_file(path: Path) -> list[str]:
    try:
        doc = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as e:
        return [f"{path}: unreadable ({e})"]
    problems = [f"{path}: {p}" for p in validate(doc)]
    if problems:
        return problems
    png = png_path(path)
    if ignored(png):
        return [f"{path}: {png.name} is Vanilla-derived/ignored - no layer source there (A8)"]
    if not png.exists():
        return [f"{path}: {png.name} missing next to it"]
    try:
        w, h, rgba = png_read(png.read_bytes())
    except (ValueError, KeyError, zlib.error) as e:
        return [f"{png}: unreadable ({e})"]
    if (w, h) != (doc["width"], doc["height"]):
        return [f"{path}: size {doc['width']}x{doc['height']} but {png.name} is {w}x{h}"]
    want = compose(doc)
    # fully transparent pixels compare as transparent, whatever colour the PNG stores in them
    bad = [i for i in range(w * h) if (rgba[i * 4 + 3] or want[i * 4 + 3]) and rgba[i * 4:i * 4 + 4] != want[i * 4:i * 4 + 4]]
    if bad:
        x, y = bad[0] % w, bad[0] // w
        return [f"{path}: {len(bad)} pixel(s) of {png.name} differ from the layers (first at {x},{y}) - "
                f"run: python tools/textures/layers.py compose {path.relative_to(ROOT) if path.is_relative_to(ROOT) else path}"]
    return []


def cmd_check(files: list[str]) -> int:
    paths = [Path(f).resolve() for f in files] if files else tracked()
    problems = [p for path in paths for p in check_file(path)]
    for p in problems:
        print(p, file=sys.stderr)
    print(f"layers: {len(paths)} source(s) checked, {len(problems)} problem(s)")
    return 1 if problems else 0


def cmd_compose(src: str, out: str | None) -> int:
    path = Path(src)
    doc = json.loads(path.read_text(encoding="utf-8"))
    problems = validate(doc)
    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    target = Path(out) if out else png_path(path)
    target.write_bytes(png_write(doc["width"], doc["height"], compose(doc)))
    print(f"wrote {target}")
    return 0


def cmd_import(src: str, name: str, force: bool) -> int:
    png = Path(src)
    target = layers_path(png)
    if target.exists() and not force:
        print(f"{target} exists (use --force)", file=sys.stderr)
        return 1
    if ignored(png.resolve()):
        print(f"{png} is Vanilla-derived/ignored - no layer source (A8)", file=sys.stderr)
        return 1
    w, h, rgba = png_read(png.read_bytes())
    target.write_text(dumps(from_rgba(w, h, rgba, name)), encoding="utf-8", newline="\n")
    print(f"wrote {target}")
    return 0


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("check")
    c.add_argument("files", nargs="*")
    p = sub.add_parser("compose")
    p.add_argument("layers")
    p.add_argument("--out")
    i = sub.add_parser("import")
    i.add_argument("png")
    i.add_argument("--name", default="Base")
    i.add_argument("--force", action="store_true")
    a = ap.parse_args(argv)
    if a.cmd == "check":
        return cmd_check(a.files)
    if a.cmd == "compose":
        return cmd_compose(a.layers, a.out)
    return cmd_import(a.png, a.name, a.force)


if __name__ == "__main__":
    sys.exit(main())
