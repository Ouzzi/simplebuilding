"""Contrast lint for module UI colour constants (ST-35, docs/ai/AUDIT-STIL-MODULE-2026-10-17.md; no client).

Per client Java file under modules/*/shared: every opaque `0xAARRGGBB` int constant whose name reads as text
(TEXT, LABEL, DIM, SOFT, VALUE, INK ...) must reach 4.5:1 (WCAG, same maths as UiContrast) on every constant that reads
as a face (BG, PANEL, FILL, FACE ...) in the same file. Backgrounds that are textures are listed in TEXTURE_FACES with the
colour measured from the Vanilla-derived texture. A line with `// contrast-ok` is skipped (text on a different face).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIN = 4.5
CONST = re.compile(r"\b([A-Z][A-Z0-9_]*)\s*=\s*0x([0-9A-Fa-f]{8})\b")
TEXT_NAME = re.compile(r"(^|_)(TEXT|LABEL|DIM|SOFT|VALUE|INK|SECTION|MUTED|WARN)(_|$)")
FACE_NAME = re.compile(r"(^|_)(BG|PANEL|FILL|FACE|BACK|BACKGROUND)(_|$)")
# File -> face colours of texture backgrounds (RGB, measured on the Vanilla-derived texture).
TEXTURE_FACES = {
    # beacon.png right field (rows 80-124, x 120-215) is flat 0x212121
    "modules/simplebeacons/shared/java/com/simplebeacons/client/StrongBeaconScreen.java": [0x212121],
}


def lum(c):
    def f(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(c >> 16 & 255) + 0.7152 * f(c >> 8 & 255) + 0.0722 * f(c & 255)


def ratio(a, b):
    hi, lo = sorted((lum(a), lum(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def check(rel, text):
    texts, faces = [], [(f"texture#{i}", c) for i, c in enumerate(TEXTURE_FACES.get(rel, []))]
    for line in text.splitlines():
        if "contrast-ok" in line:
            continue
        for name, hexv in CONST.findall(line):
            argb = int(hexv, 16)
            if argb >> 24 != 0xFF:
                continue  # translucent overlays have no fixed face
            if FACE_NAME.search(name):
                faces.append((name, argb & 0xFFFFFF))
            elif TEXT_NAME.search(name):
                texts.append((name, argb & 0xFFFFFF))
    return [f"{rel}: {t} #{tc:06X} on {f} #{fc:06X} = {ratio(tc, fc):.2f}:1 (< {MIN})"
            for t, tc in texts for f, fc in faces if ratio(tc, fc) < MIN]


def main():
    errors, files = [], 0
    for path in sorted((ROOT / "modules").glob("*/shared/java/**/client/*.java")):
        rel = path.relative_to(ROOT).as_posix()
        text = path.read_text(encoding="utf-8")
        if CONST.search(text) or rel in TEXTURE_FACES:
            files += 1
            errors += check(rel, text)
    for rel in TEXTURE_FACES:
        assert (ROOT / rel).exists(), f"TEXTURE_FACES names a missing file: {rel}"
    if errors:
        print("\n".join(errors))
        return 1
    print(f"ui contrast ok: {files} files with colour constants")
    return 0


if __name__ == "__main__":
    sys.exit(main())
