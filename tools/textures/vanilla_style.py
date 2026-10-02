"""Vanilla-Stil fuer zu bunte Texturen (Textur-Audit Q1, 2026-10-02).

Werkzeuge, mit denen generate_textures.py und vanilla_style_2026_10_02.py verrauschte Bilder
(Verlaeufe, Alpha-Mischungen, herunterskalierte Fotos) in Pixelkunst nach Vanilla-Regeln ueberfuehren:

- Grundmaterial auf die Rampe der Vanilla-Textur desselben Materials (RAMPS, aus dem 26.3-Client-Jar
  abgelesen; der Generator braucht das Jar nicht).
- Was nicht zum Grundmaterial gehoert (Schleier, Spirale, Sterne, Druck), wird zu wenigen festen
  Toenen zusammengefasst (k-Mittel im Lab-Raum, deterministisch).
- Items: deckend (Alpha 0 oder 255), 1 px Kontur im dunkelsten Ton der Rampe, nie reines Schwarz.

Alle Funktionen sind deterministisch: gleiche Vorlage -> gleiches Bild (fuer --check).
"""
import numpy as np
from PIL import Image


def hexrgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


# Vanilla-Rampen (dunkel -> hell), abgelesen aus assets/minecraft/textures im 26.3-Client.
RAMPS = {
    "paper": ["#878787", "#aeaeae", "#c1c1c1", "#d6d6d6", "#e9eaeb", "#fcfcf2"],
    "map": ["#736041", "#877251", "#947f5d", "#baa57f", "#d4c09d", "#ebd7b2", "#f2e2c2", "#fff0d1"],
    "emerald": ["#002d00", "#005300", "#007b18", "#009529", "#00aa2c", "#17dd62", "#41f384", "#82f6ad"],
    "green_dye": ["#223505", "#2c420b", "#36510d", "#4a6b18", "#668a16"],
    "snowball": ["#7ba6a6", "#afcaca", "#c2dada", "#d0f1f1", "#e8f8f8", "#ffffff"],
    "brick": ["#2d1610", "#492319", "#612f22", "#7f3e2c", "#8e4631", "#b75a40", "#c76245"],
    "elytra": ["#353535", "#4b4b4b", "#696969", "#737373", "#8c8c8c"],
    "elytra_lilac": ["#706e8d", "#7f7f98", "#8f8fb3"],
    "resin": ["#802a1a", "#9b3300", "#b54006", "#e05504", "#f3791b", "#fab326", "#ffdbb1"],
    "amethyst": ["#54398a", "#6f4fab", "#8d6acc", "#b38ef3", "#cfa0f3", "#fecbe6"],
    "lapis": ["#052463", "#12408b", "#1c53a8", "#345ec3", "#5a82e2", "#7497ea"],
    "honeycomb": ["#d56b2b", "#e57b24", "#ea8e16", "#fabf29", "#fade29"],
    "book_leather": ["#161005", "#312104", "#44250a", "#522e10", "#654b17"],
    "copper": ["#904931", "#9a5038", "#a75a40", "#b26247", "#c26b4c", "#c87456", "#d67b5b", "#e3826c"],
    "exposed_copper": ["#796454", "#7f7257", "#947661", "#a87762", "#988c69", "#ba8277", "#ce8d83"],
    "weathered_copper": ["#497164", "#6a7147", "#748d57", "#6c975c", "#8d8770", "#64a077", "#66a977", "#7ab799"],
    "oxidized_copper": ["#3b6655", "#396e59", "#3e816b", "#4c9484", "#53a178", "#4fab90", "#59b292", "#6ec59f"],
    "gold": ["#cc8e27", "#d39632", "#f9bd23", "#f5cc27", "#ffd83e", "#fee048", "#ffec4f", "#fffd90", "#feffbd"],
    "iron": ["#b1b0b0", "#b9b9b9", "#c1c1c1", "#d1cfcf", "#d6d6d6", "#dcdcdc", "#e6e6e6", "#ececec", "#f2f2f2"],
    "diamond": ["#0ebabd", "#15c2c6", "#3de0e5", "#4bede6", "#65f5e3", "#70fbf0", "#9efeeb", "#d5fff6", "#ffffff"],
    "netherite": ["#241e1f", "#31292a", "#3c3232", "#434043", "#4d494d", "#5a575a"],
    # Enderit des Mods (Enderitbarren/-block, end_palette_textures)
    "enderite": ["#1c0a33", "#2d1656", "#3e2173", "#55309a", "#6d45b8", "#7b51c9", "#8e63dc", "#a57de9", "#cfb2fb",
                 "#f4d2ff"],
}


# ---------------------------------------------------------------------------
# Farbraum
# ---------------------------------------------------------------------------

def lab(rgb):
    """sRGB (N x 3, 0..255) -> CIE-Lab (D65)."""
    c = np.asarray(rgb, dtype=np.float64) / 255.0
    c = np.where(c > 0.04045, ((c + 0.055) / 1.055) ** 2.4, c / 12.92)
    m = np.array([[0.4124, 0.3576, 0.1805], [0.2126, 0.7152, 0.0722], [0.0193, 0.1192, 0.9505]])
    xyz = c @ m.T / np.array([0.95047, 1.0, 1.08883])
    f = np.where(xyz > 0.008856, np.cbrt(xyz), 7.787 * xyz + 16.0 / 116.0)
    return np.stack([116.0 * f[:, 1] - 16.0, 500.0 * (f[:, 0] - f[:, 1]), 200.0 * (f[:, 1] - f[:, 2])], axis=1)


def _dist(a, b):
    return np.sqrt(((a[:, None, :] - b[None, :, :]) ** 2).sum(axis=2))


def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def kmeans(colors, weights, k, iters=30):
    """Gewichtetes k-Mittel im Lab-Raum, deterministisch (Start: gewichtete Helligkeits-Quantile).
    colors: N x 3 (RGB), weights: N. Gibt k RGB-Zentren (gerundet, nach Helligkeit sortiert) zurueck."""
    colors = np.asarray(colors, dtype=np.float64)
    weights = np.asarray(weights, dtype=np.float64)
    if len(colors) <= k:
        return [tuple(int(v) for v in c) for c in sorted(colors.tolist(), key=luma)]
    L = lab(colors)
    order = np.argsort(L[:, 0], kind="stable")
    cum = np.cumsum(weights[order]) / weights.sum()
    centers = np.array([L[order[min(np.searchsorted(cum, (i + 0.5) / k), len(order) - 1)]] for i in range(k)])
    for _ in range(iters):
        lab_assign = _dist(L, centers).argmin(axis=1)
        new = centers.copy()
        for j in range(k):
            m = lab_assign == j
            if m.any():
                new[j] = (L[m] * weights[m, None]).sum(axis=0) / weights[m].sum()
        if np.allclose(new, centers):
            break
        centers = new
    assign = _dist(L, centers).argmin(axis=1)
    out = []
    for j in range(k):
        m = assign == j
        if not m.any():
            continue
        # Zentrum = gewichtetes Mittel der echten Farben in RGB (bleibt im Farbumfang des Bildes)
        rgbc = (colors[m] * weights[m, None]).sum(axis=0) / weights[m].sum()
        out.append(tuple(int(round(v)) for v in rgbc))
    return sorted(set(out), key=luma)


def _counts(pixels):
    cols, inv, cnt = np.unique(np.asarray(pixels).reshape(-1, 3), axis=0, return_inverse=True, return_counts=True)
    return cols, cnt


def map_to_palette(pixels, palette):
    """Jeder Pixel auf die naechste Palettenfarbe (Lab). pixels N x 3 -> N x 3 (uint8)."""
    pal = np.asarray(palette, dtype=np.float64)
    idx = _dist(lab(pixels), lab(pal)).argmin(axis=1)
    return pal[idx].astype(np.uint8), idx


def build_palette(pixels, base, extra, thresh=9.0):
    """Palette = Vanilla-Grundrampe (base, hex) + bis zu `extra` Toene fuer das, was weiter als `thresh`
    (Delta-E) von der Grundrampe entfernt liegt."""
    base_rgb = [hexrgb(h) for h in base]
    cols, cnt = _counts(pixels)
    pal = list(base_rgb)
    if extra > 0:
        d = _dist(lab(cols), lab(np.array(base_rgb, dtype=np.float64))).min(axis=1) if base_rgb else np.full(len(cols), 1e9)
        foreign = d > thresh
        if foreign.any():
            for c in kmeans(cols[foreign], cnt[foreign], extra):
                if not base_rgb or _dist(lab([c]), lab(base_rgb)).min() > thresh / 2:
                    pal.append(c)
    return pal


def drop_unused(pixels, palette):
    used = {tuple(int(v) for v in p) for p in np.asarray(pixels).reshape(-1, 3)}
    return [c for c in palette if tuple(c) in used]


# ---------------------------------------------------------------------------
# Bloecke
# ---------------------------------------------------------------------------

def restyle_block(img, base, extra=14, thresh=9.0, palette=None):
    """Deckende 16x16-Blockflaeche (RGB) auf Vanilla-Grundrampe + `extra` feste Toene.
    Mit `palette` (Liste RGB) wird statt dessen genau diese Palette benutzt (Familien mit gleicher Palette)."""
    rgb = np.asarray(img.convert("RGB"), dtype=np.uint8)
    flat = rgb.reshape(-1, 3)
    pal = palette if palette is not None else build_palette(flat, base, extra, thresh)
    out, _ = map_to_palette(flat, pal)
    return Image.fromarray(out.reshape(rgb.shape), "RGB")


def palette_of(img):
    a = np.asarray(img.convert("RGBA"))
    m = a[..., 3] > 0
    return sorted({tuple(int(v) for v in p) for p in a[m][:, :3]}, key=luma)


# ---------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------

def restyle_item(img, ramps, extra=0, alpha_cut=128, outline=True, thresh=9.0, outline_color=None):
    """Item (RGBA): deckend machen (Alpha >= alpha_cut bleibt), Farben auf die Rampen (Liste von Hex-Listen,
    je dunkel -> hell) plus `extra` freie Toene, dann 1 px Kontur im dunkelsten Ton der Rampe, zu der der
    Randpixel gehoert (oder `outline_color`)."""
    a = np.asarray(img.convert("RGBA"), dtype=np.uint8).copy()
    h, w = a.shape[:2]
    op = a[..., 3] >= alpha_cut
    flat = a[..., :3][op]
    base = [c for r in ramps for c in r]
    pal = build_palette(flat, base, extra, thresh)
    mapped, idx = map_to_palette(flat, pal)
    rgb = a[..., :3].copy()
    rgb[op] = mapped
    # zu welcher Rampe gehoert jede Palettenfarbe (freie Toene: naechste Rampe)
    ramp_of = []
    ramp_lab = [lab([hexrgb(c) for c in r]) for r in ramps]
    for c in pal:
        ramp_of.append(int(np.argmin([_dist(lab([c]), rl).min() for rl in ramp_lab])))
    darkest = [hexrgb(r[0]) for r in ramps]
    if outline:
        pad = np.pad(op, 1, constant_values=False)
        edge = op & (~pad[:-2, 1:-1] | ~pad[2:, 1:-1] | ~pad[1:-1, :-2] | ~pad[1:-1, 2:])
        pal_arr = np.asarray(pal)
        idx_full = np.full((h, w), -1)
        idx_full[op] = idx
        for y, x in zip(*np.nonzero(edge)):
            rgb[y, x] = hexrgb(outline_color) if outline_color else darkest[ramp_of[idx_full[y, x]]]
    out = np.zeros((h, w, 4), dtype=np.uint8)
    out[..., :3] = np.where(op[..., None], rgb, 0)
    out[..., 3] = np.where(op, 255, 0)
    return Image.fromarray(out, "RGBA")


def set_pixels(img, pixels):
    """Handkorrektur: {(x, y): '#rrggbb' | None (frei)}."""
    img = img.copy()
    for (x, y), c in pixels.items():
        img.putpixel((x, y), (0, 0, 0, 0) if c is None else hexrgb(c) + ((255,) if img.mode == "RGBA" else ()))
    return img


def opaque_items_fix(img, outline_dark=None):
    """Mechanische Korrektur fuer Items: halbtransparente Pixel deckend (Alpha >= 128) oder frei, reines
    Schwarz (0,0,0) auf den dunkelsten Nachbarton (sonst `outline_dark`)."""
    a = np.asarray(img.convert("RGBA"), dtype=np.uint8).copy()
    h, w = a.shape[:2]
    semi = (a[..., 3] > 0) & (a[..., 3] < 255)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    a[a[..., 3] == 0] = 0
    blacks = list(zip(*np.nonzero((a[..., 3] == 255) & (a[..., :3].sum(axis=2) == 0))))
    for y, x in blacks:
        cands = []
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                ny, nx = y + dy, x + dx
                if 0 <= ny < h and 0 <= nx < w and a[ny, nx, 3] == 255 and a[ny, nx, :3].sum() > 0:
                    cands.append(tuple(int(v) for v in a[ny, nx, :3]))
        c = min(cands, key=luma) if cands else (hexrgb(outline_dark) if outline_dark else (20, 16, 18))
        a[y, x, :3] = c
    return Image.fromarray(a, "RGBA"), int(semi.sum()), len(blacks)
