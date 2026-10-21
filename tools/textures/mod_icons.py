"""
Mod icons: one 16x16 pixel-art icon per mod of modules/modules.json (docs/ai/PLAN-B-MODICONS-2026-10-21.md,
docs/ai/PRINZIPIEN.md A8 c).

Every icon is drawn here as ASCII art with a palette; the characters are grouped into named layers, so the
layer source <assets>/<modid>/icon.layers.json opens in the wiki pixel editor (A8 b). Written per mod:

    icon.layers.json   layer source (tools/textures/layers.py format)
    icon.png           16x16 composition: Fabric `icon`, NeoForge `iconFile` (both draw it unfiltered)
    icon_128.png       8x nearest-neighbour copy for Forge `logoFile` (Forge stretches the logo to 50 px and
                       cannot switch off filtering, so it gets enough pixels to only ever shrink)

Owner icons (OWNER) were converted once from the owner's upscaled images (`--import-owner`, cell centres sampled);
their layer source is the original from then on and is only composed here.

    python tools/textures/mod_icons.py            write all icons
    python tools/textures/mod_icons.py --check    icons match this script / their layer source
    python tools/textures/mod_icons.py --sheet X  contact sheet PNG (8x) for previews
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers as L  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SCALE = 8
OWNER = ("simplebuilding", "simplemoney", "simpleriding", "simplevisuals")


def asset_dir(entry: dict) -> Path:
    mod_id = entry.get("modId", entry["id"])
    if entry["paths"]["root"] == ".":
        return ROOT / "src/main/resources/assets" / mod_id
    return ROOT / entry["paths"]["root"] / "shared/resources/assets" / mod_id


def modules() -> list[dict]:
    return json.loads((ROOT / "modules/modules.json").read_text(encoding="utf-8"))["modules"]


# ---------------------------------------------------------------- drawing helpers

def icon(motif: str, pal: dict[str, str], groups: list[tuple[str, str]], *rows: str) -> dict:
    return {"motif": motif, "pal": pal, "groups": groups, "art": list(rows)}


def blank() -> list[list[str]]:
    return [["."] * 16 for _ in range(16)]


def cube(grid, x0: int, y0: int, w: int, side: int, top, left, right, edge: str = "o"):
    """Isometric block (2:1) at (x0, y0), width w, side height `side`; face callables (s, t) -> char.
    Drawn over the grid (painter's order: call back cubes first); its silhouette gets `edge`."""
    hw, hh = w / 2, w / 4
    cx, cy = x0 + hw, y0 + hh
    mask = {}
    for r in range(16):
        for c in range(16):
            px, py = c + 0.5, r + 0.5
            if abs(px - cx) / hw + abs(py - cy) / hh <= 1:
                s = (px - cx) / w + (py - cy) / (2 * hh) + 0.5
                t = -(px - cx) / w + (py - cy) / (2 * hh) + 0.5
                mask[r, c] = top(s, t)
            elif x0 <= px <= cx:
                yt = cy + (px - x0) / 2
                if yt <= py <= yt + side:
                    mask[r, c] = left((px - x0) / hw, (py - yt) / side)
            elif cx <= px <= x0 + w:
                yt = cy + hh - (px - cx) / 2
                if yt <= py <= yt + side:
                    mask[r, c] = right((px - cx) / hw, (py - yt) / side)
    for (r, c), ch in mask.items():
        outer = any((r + dr, c + dc) not in mask for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        grid[r][c] = edge if outer else ch


def rows_of(grid) -> list[str]:
    return ["".join(r) for r in grid]


def flat(ch):
    return lambda s, t: ch


# ---------------------------------------------------------------- procedural icons

def blocks_icon():
    """Simple Blocks: a block of hushed deepslate tiles."""
    g = blank()

    def tiles(light, mid, line):
        return lambda s, t: line if abs(s - 0.5) < 0.07 or abs(t - 0.5) < 0.07 else (light if (s < 0.5) == (t < 0.5) else mid)
    cube(g, 1, 1, 14, 7, tiles("T", "t", "k"), tiles("L", "l", "k"), tiles("R", "r", "k"))
    return icon("Block aus Deepslate-Fliesen", {"o": "#141418", "k": "#26262c", "T": "#7a7a84", "t": "#6a6a74",
                                               "L": "#55555e", "l": "#4a4a52", "R": "#3c3c44", "r": "#34343b"},
                [("Kontur", "o"), ("Oberseite", "Tt"), ("Linke Seite", "Ll"), ("Rechte Seite", "Rr"), ("Fugen", "k")],
                *rows_of(g))


def octets_icon():
    """Simple Octets: three half-size cubes stacked."""
    g = blank()
    planks_top = lambda s, t: "p" if int(t * 3) % 2 else "P"  # noqa: E731
    cube(g, 4, 1, 8, 4, planks_top, flat("l"), flat("r"))
    cube(g, 0, 6, 8, 4, planks_top, flat("l"), flat("r"))
    cube(g, 8, 6, 8, 4, planks_top, flat("l"), flat("r"))
    # keep the 1 px border free (AGENTS 7): the lower cubes touch column 0/15 only with their outline corner
    for r in range(16):
        g[r][0] = g[r][15] = "."
    return icon("Drei Achtel-Würfel aus Eichenholz", {"o": "#3a2810", "P": "#c8a064", "p": "#b48a50", "l": "#9a7240",
                                                      "r": "#7a5630"},
                [("Kontur", "o"), ("Oberseiten", "Pp"), ("Linke Seiten", "l"), ("Rechte Seiten", "r")], *rows_of(g))


def models_icon():
    """Simple Models: a wireframe model cube (silhouette plus the three front edges)."""
    g = blank()
    cube(g, 1, 1, 14, 7, flat("T"), flat("L"), flat("R"), edge="e")
    out = blank()
    for r in range(16):
        for c in range(16):
            ch = g[r][c]
            if ch == "e":
                out[r][c] = "e"
            elif ch == "L" and c + 1 < 16 and g[r][c + 1] == "R":
                out[r][c] = out[r][c + 1] = "v"
            elif ch == "T" and r + 1 < 16 and g[r + 1][c] in "LR":
                out[r][c] = "v"
    return icon("Drahtgitter-Würfel (Modell)", {"e": "#2a9ac0", "v": "#9ef0ff"},
                [("Umriss", "e"), ("Vorderkanten", "v")], *rows_of(out))


def beacon_icon():
    """Simple Beacons: a beacon block (glass shell, cyan core, obsidian base); drawn on the cube silhouette."""
    return icon("Leuchtfeuer-Block", {"o": "#1e3a44", "W": "#f4ffff", "w": "#c8eef2", "g": "#a8e4ec", "h": "#7cc8d4",
                                      "d": "#5aa8b4", "C": "#7ffcff", "c": "#2a2238"},
                [("Kontur", "o"), ("Glas", "Wwghd"), ("Kern", "C"), ("Obsidian", "c")],
                "................",
                ".......oo.......",
                ".....ooWWoo.....",
                "...ooWggggWoo...",
                ".ooWggCCCCggWoo.",
                ".owWWggCCggWWwo.",
                ".ohwwWWggWWwwdo.",
                ".ohhhwwWWwwdddo.",
                ".ohhhhhwwdddddo.",
                ".ohCChhwwddCCdo.",
                ".ohCCChwwdCCCdo.",
                ".occcccwwccccco.",
                "...ooccwwccoo...",
                ".....oowwoo.....",
                ".......oo.......",
                "................")


# ---------------------------------------------------------------- ASCII icons

ICONS: dict[str, dict] = {
    "wiringexample": icon("Redstone-Knoten (Verdrahtung)", {"o": "#3a0a08", "R": "#d82818", "r": "#9a1a10",
                                                            "H": "#ff6a4a", "L": "#ffd0b0"},
                          [("Kontur", "o"), ("Leitung", "Rr"), ("Knoten", "HL")],
                          "................",
                          "................",
                          "......oooo......",
                          "......oRro......",
                          "......orRo......",
                          "......oRro......",
                          ".ooooooHHoooooo.",
                          ".oRrRrHLLHrRrRo.",
                          ".orRrRHLLHRrRro.",
                          ".ooooooHHoooooo.",
                          "......oRro......",
                          "......orRo......",
                          "......oRro......",
                          "......oooo......",
                          "................",
                          "................"),
    "simplefun": icon("Wurf-Ziegel", {"o": "#4a1f14", "h": "#c97a5a", "R": "#a8523a", "r": "#8a4030",
                                     "d": "#6e3020"},
                      [("Kontur", "o"), ("Ziegel", "hRrd")],
                      "................",
                      "................",
                      "................",
                      "................",
                      "....ooooooooo...",
                      "...ohhhhhhhhRo..",
                      "..ohhhhhhhhRdo..",
                      "..oRRRRRRRRddo..",
                      "..oRrRRRRrRddo..",
                      "..oRRRRrRRRddo..",
                      "..odddddddddo...",
                      "...ooooooooo....",
                      "................",
                      "................",
                      "................",
                      "................"),
    "simplesounds": icon("Lautsprecher mit Schallwellen", {"o": "#2a2a2a", "g": "#6b6b6b", "G": "#9a9a9a",
                                                           "w": "#3aa0c8"},
                         [("Kontur", "o"), ("Lautsprecher", "gG"), ("Wellen", "w")],
                         "................",
                         "................",
                         "................",
                         "......oo........",
                         ".....oGo...w....",
                         "..ooogGo.w..w...",
                         "..oggggo..w..w..",
                         "..oggggo..w..w..",
                         "..oggggo..w..w..",
                         "..ooogGo.w..w...",
                         ".....oGo...w....",
                         "......oo........",
                         "................",
                         "................",
                         "................",
                         "................"),
    "simplequalityoflife": icon("Bündel mit Herz (Sammel-Mod)", {"o": "#3a2410", "b": "#8b5a2b", "h": "#a8743c",
                                                                 "d": "#6a4220", "L": "#c9a46a", "H": "#d6303a"},
                                [("Kontur", "o"), ("Beutel", "bhd"), ("Schnur", "L"), ("Herz", "H")],
                                "................",
                                "................",
                                ".......oo.......",
                                "......oLLo......",
                                ".......oo.......",
                                ".....oooooo.....",
                                "....ohhhhhbo....",
                                "...ohhbbbbbdo...",
                                "..ohhbbHbHbbdo..",
                                "..ohbbHHHHHbdo..",
                                "..obbbbHHHbbdo..",
                                "..obbbbbHbbddo..",
                                "...obbbbbbddo...",
                                "....oooooooo....",
                                "................",
                                "................"),
    "simpletweaks": icon("Claim-Fahne", {"o": "#1e2430", "W": "#9c7440", "R": "#2f7fd0", "h": "#6ab0f0",
                                        "d": "#1f5a98"},
                         [("Kontur", "o"), ("Stange", "W"), ("Fahne", "Rhd")],
                         "................",
                         "................",
                         "...ooo..........",
                         "...oWooooooo....",
                         "...oWRRRRRRRo...",
                         "...oWRhRRRRRRo..",
                         "...oWRRRRRRRRo..",
                         "...oWRRRRRRRdo..",
                         "...oWRRRRRRdo...",
                         "...oWoooooooo...",
                         "...oWo..........",
                         "...oWo..........",
                         "...oWo..........",
                         "..ooooo.........",
                         "................",
                         "................"),
    "simpledimensions": icon("Portal im Obsidianrahmen", {"o": "#0a0612", "K": "#1a1028", "k": "#3b2a5c",
                                                          "p": "#7a2fd0", "P": "#b070f8", "q": "#4e1a98"},
                             [("Kontur", "o"), ("Rahmen", "Kk"), ("Portal", "pPq")],
                             "................",
                             "...oooooooooo...",
                             "...okKKKKKKko...",
                             "...oKppqpPpKo...",
                             "...oKpPpqppKo...",
                             "...oKqppPpqKo...",
                             "...oKppqppPKo...",
                             "...oKPpppqpKo...",
                             "...oKpqPpppKo...",
                             "...oKppqpPpKo...",
                             "...oKpPpqppKo...",
                             "...oKqppPpqKo...",
                             "...okKKKKKKko...",
                             "...oooooooooo...",
                             "................",
                             "................"),
    "simplesandwiches": icon("Sandwich", {"o": "#4a2a10", "b": "#9c5a22", "B": "#e2b871", "G": "#5cbc32",
                                          "g": "#2f7d1c", "y": "#f2c832", "r": "#d0646a", "R": "#e88a8f"},
                             [("Kontur", "o"), ("Brot", "bB"), ("Salat", "Gg"), ("Käse", "y"), ("Schinken", "rR")],
                             "................",
                             "................",
                             "................",
                             "....oooooooo....",
                             "...obbbbbbbbo...",
                             "..obBBBBBBBBbo..",
                             "..oBBBBBBBBBBo..",
                             ".oGgGGgGgGGgGgo.",
                             "..oyyyyyyyyyyo..",
                             "..orrRrrrrRrro..",
                             "..oBBBBBBBBBBo..",
                             "..obbbbbbbbbbo..",
                             "...oooooooooo...",
                             "................",
                             "................",
                             "................"),
    "simplelib": icon("Buch mit Zahnrad (Bibliothek)", {"o": "#2b1a0e", "d": "#5e311b", "c": "#7a4024",
                                                        "C": "#9a5630", "g": "#d8d8d8", "G": "#9a9a9a", "p": "#e8e1c8",
                                                        "P": "#c4b99a"},
                      [("Kontur", "o"), ("Einband", "dcC"), ("Seiten", "pP"), ("Zahnrad", "gG")],
                      "................",
                      "...ooooooooooo..",
                      "..odCCCCCCCCCo..",
                      "..odcCCgCgCCco..",
                      "..odcCgggGgCco..",
                      "..odcggCCCGgco..",
                      "..odcCgCCCGCco..",
                      "..odcggCCCGGco..",
                      "..odcCgGGGGCco..",
                      "..odcCCGCGCCco..",
                      "..odcccccccccoo.",
                      "..odpppppppppPo.",
                      "..odPPPPPPPPPPo.",
                      "...oooooooooooo.",
                      "................",
                      "................"),
    "simpleinterfaces": icon("Fenster mit Inventarplätzen", {"o": "#000000", "W": "#ffffff", "c": "#c6c6c6",
                                                             "S": "#555555", "D": "#373737", "s": "#8b8b8b",
                                                             "x": "#4ad8e0"},
                             [("Kontur", "o"), ("Fenster", "WcS"), ("Plätze", "Ds"), ("Inhalt", "x")],
                             "................",
                             "................",
                             "..oooooooooooo..",
                             ".oWWWWWWWWWWWco.",
                             ".oWccccccccccSo.",
                             ".oWcDDDccDDDcSo.",
                             ".oWcDxWccDsWcSo.",
                             ".oWcDWWccDWWcSo.",
                             ".oWccccccccccSo.",
                             ".oWcDDDccDDDcSo.",
                             ".oWcDsWccDsWcSo.",
                             ".oWcDWWccDWWcSo.",
                             ".oWccccccccccSo.",
                             ".ocSSSSSSSSSSSo.",
                             "..oooooooooooo..",
                             "................"),
    "simplemaps": icon("Karte mit Markierung", {"o": "#6b5a3a", "p": "#e3d3a5", "l": "#6fa046", "L": "#8cbd5a",
                                               "w": "#3f76c4", "X": "#c42a2a"},
                       [("Kontur", "o"), ("Papier", "p"), ("Land", "lL"), ("Wasser", "w"), ("Markierung", "X")],
                       "................",
                       "................",
                       "..oooooooooooo..",
                       "..oppppppppppo..",
                       "..opllllpwwwpo..",
                       "..oplLlwXwXwpo..",
                       "..opllwwwXwwpo..",
                       "..oplwwwXwXlpo..",
                       "..opwwwllllLpo..",
                       "..opwwlllLllpo..",
                       "..opwlllllllpo..",
                       "..oppppppppppo..",
                       "..oooooooooooo..",
                       "................",
                       "................",
                       "................"),
    "simpletrims": icon("Brustpanzer mit Goldbesatz", {"o": "#2e2e2e", "m": "#c8c8c8", "M": "#ececec",
                                                       "d": "#8a8a8a", "T": "#f2c12e"},
                        [("Kontur", "o"), ("Panzer", "mMd"), ("Besatz", "T")],
                        "................",
                        "................",
                        "..ooo......ooo..",
                        ".oMmmo....oMmmo.",
                        ".oMmmmooooMmmmo.",
                        ".omTmmmmmmmmTmo.",
                        ".oomTmmmmmmTmoo.",
                        "..omdTmmmmTmdo..",
                        "..ommdTmmTmmdo..",
                        "..ommmdTTmmmdo..",
                        "..ommmmddmmmdo..",
                        "..ommmmmmmmmdo..",
                        "..oddddddddddo..",
                        "...oooooooooo...",
                        "................",
                        "................"),
    "simplemobs": icon("Kopf eines Hohlwächters", {"o": "#0c0a10", "k": "#2e2a3a", "K": "#4a4458", "E": "#e080ff",
                                                   "e": "#8a3ac8", "d": "#120e18"},
                       [("Kontur", "o"), ("Kopf", "kK"), ("Augen", "Ee"), ("Maul", "d")],
                       "................",
                       "................",
                       "..oooooooooooo..",
                       "..okKkkkkkkKko..",
                       "..okkkkkkkkkko..",
                       "..okkkkkkkkkko..",
                       "..okEEEkkEEEko..",
                       "..okeEekkeEeko..",
                       "..okkkkkkkkkko..",
                       "..okkkkddkkkko..",
                       "..okkddddddkko..",
                       "..okkdkddkdkko..",
                       "..okkkkkkkkkko..",
                       "..oooooooooooo..",
                       "................",
                       "................"),
    "simpleweather": icon("Regenwolke", {"o": "#6a7380", "W": "#f2f2f2", "g": "#b8c0cc", "b": "#3f7fe0"},
                          [("Kontur", "o"), ("Wolke", "Wg"), ("Regen", "b")],
                          "................",
                          "................",
                          "......oooo......",
                          ".....oWWWWo.....",
                          "...ooWWWWWWooo..",
                          "..oWWWWWWWWWWWo.",
                          ".oWWWWWWWWWWWWo.",
                          ".oWWWWWWWWWWWgo.",
                          ".oggggggggggggo.",
                          "..oooooooooooo..",
                          "...b...b...b....",
                          "..b...b...b.....",
                          "................",
                          "....b...b...b...",
                          "...b...b...b....",
                          "................"),
    "simplemovement": icon("Stiefel mit Tempolinien", {"o": "#2a170b", "L": "#a8683a", "l": "#8b4f2a",
                                                       "d": "#4a2a15", "w": "#4aa8e0"},
                           [("Kontur", "o"), ("Stiefel", "Ll"), ("Sohle", "d"), ("Tempo", "w")],
                           "................",
                           "................",
                           "................",
                           "......oooo......",
                           "......oLlo......",
                           "..www.oLlo......",
                           "......oLlo......",
                           ".wwww.oLlo......",
                           "......oLlooo....",
                           "..www.oLllllo...",
                           "......oLlllllo..",
                           "......odddddddo.",
                           ".......ooooooo..",
                           "................",
                           "................",
                           "................"),
    "simpletools": icon("Eisenspitzhacke", {"o": "#2d2d2d", "H": "#dadada", "h": "#a0a0a0", "W": "#8a6232"},
                        [("Kontur", "o"), ("Kopf", "Hh"), ("Stiel", "W")],
                        "................",
                        "................",
                        "...ooooooo......",
                        "..oHHHHHHhoo....",
                        "...ooooooHhho...",
                        ".........oHhho..",
                        ".......oWooHho..",
                        "......oWo.oHho..",
                        ".....oWo..oHho..",
                        "....oWo...oHo...",
                        "...oWo....oHo...",
                        "..oWo......o....",
                        ".oWo............",
                        ".oo.............",
                        "................",
                        "................"),
    "simplehammers": icon("Vorschlaghammer", {"o": "#2d2d2d", "H": "#d4d4d4", "h": "#a8a8a8", "d": "#6e6e6e",
                                              "W": "#8a6232"},
                          [("Kontur", "o"), ("Kopf", "Hhd"), ("Stiel", "W")],
                          "................",
                          "........oooooo..",
                          ".......oHHHHHho.",
                          ".......oHhhhhdo.",
                          ".......oHhhhhdo.",
                          ".......oHhhhhdo.",
                          ".......oddddddo.",
                          "......oWoooooo..",
                          ".....oWo........",
                          "....oWo.........",
                          "...oWo..........",
                          "..oWo...........",
                          ".oWo............",
                          ".oo.............",
                          "................",
                          "................"),
    "simplefarming": icon("Weizengarbe", {"o": "#3a2810", "Y": "#e8c050", "y": "#b8862c", "s": "#8a9a30",
                                          "T": "#6a4a20"},
                          [("Ähren", "Yy"), ("Halme", "s"), ("Band", "oT")],
                          "................",
                          "................",
                          "....Y..Y..Y.....",
                          "...YyYYyYYyY....",
                          "...yYyyYyyYy....",
                          "...YyYYyYYyY....",
                          "...yYyyYyyYy....",
                          "....s..s..s.....",
                          ".....s.s.s......",
                          "....oTTTTTo.....",
                          "....oTTTTTo.....",
                          ".....s.s.s......",
                          "....s..s..s.....",
                          "...s...s...s....",
                          "................",
                          "................"),
    "simplemobmanners": icon("Namensschild", {"o": "#5a4220", "T": "#dcc89e", "t": "#b8a070", "k": "#3a2a14",
                                              "S": "#8a8a8a", "L": "#8a6a3a"},
                             [("Kontur", "o"), ("Schild", "Ttk"), ("Schnur", "S"), ("Schrift", "L")],
                             "................",
                             "................",
                             "..S.............",
                             "..S.............",
                             "...S............",
                             "....Sooooooooo..",
                             "....oSTTTTTTTTo.",
                             "...oTkTTTTTTTTo.",
                             "...oTTTLLLLLLTo.",
                             "....oTTTTTTTTto.",
                             ".....ooooooooo..",
                             "................",
                             "................",
                             "................",
                             "................",
                             "................"),
    "simpleloot": icon("Schatztruhe", {"o": "#2a1a0c", "w": "#a8743c", "W": "#c48a4a", "d": "#6a4220",
                                       "G": "#f2d040", "g": "#b89020"},
                       [("Kontur", "o"), ("Holz", "wWd"), ("Schloss", "Gg")],
                       "................",
                       "................",
                       "................",
                       "..oooooooooooo..",
                       "..owWwwwwwwwwo..",
                       "..owwwwwwwwwwo..",
                       "..owwwwwwwwwwo..",
                       "..oooooGGooooo..",
                       "..owwwwggwwwwo..",
                       "..owwwwwwwwwwo..",
                       "..owwwwwwwwwwo..",
                       "..owwwwwwwwwwo..",
                       "..oddddddddddo..",
                       "..oooooooooooo..",
                       "................",
                       "................"),
    "simplequestionary": icon("Aufgeschlagenes Buch", {"o": "#3a2414", "P": "#f0e8d0", "g": "#c8b890",
                                                       "L": "#6a6a7a", "c": "#7a3a2a"},
                              [("Kontur", "o"), ("Seiten", "Pg"), ("Schrift", "L"), ("Einband", "c")],
                              "................",
                              "................",
                              "................",
                              "................",
                              "..ooooo..ooooo..",
                              ".oPPPPPggPPPPPo.",
                              ".oPLLLPggPLLLPo.",
                              ".oPPPPPggPPPPPo.",
                              ".oPLLLPggPLLPPo.",
                              ".oPPPPPggPPPPPo.",
                              ".oPLLPPggPLLLPo.",
                              ".oPPPPPggPPPPPo.",
                              ".occcccooccccco.",
                              "..oooooooooooo..",
                              "................",
                              "................"),
    "simpleaesthetics": icon("Malerpalette (Sammel-Mod)", {"o": "#4a2e14", "W": "#c8975a", "R": "#d83030",
                                                           "B": "#3060d8", "Y": "#f0d030", "G": "#30a040"},
                             [("Kontur", "o"), ("Palette", "W"), ("Farben", "RBYG")],
                             "................",
                             "................",
                             "................",
                             "....ooooooo.....",
                             "...oWWWWWWWoo...",
                             "..oWRRWWBBWWWo..",
                             ".oWWRRWWBBWWWWo.",
                             ".oWWWWWWWWWYYWo.",
                             ".oWGGWWooWWYYWo.",
                             ".oWGGWo..oWWWo..",
                             "..oWWWWooWWWo...",
                             "...ooWWWWWWWo...",
                             ".....ooooooo....",
                             "................",
                             "................",
                             "................"),
    "simpleenddragonfight": icon("Drachenei", {"o": "#2a1840", "k": "#1e1428", "K": "#3a2a50", "p": "#b040e0"},
                                 [("Kontur", "o"), ("Schale", "kK"), ("Funken", "p")],
                                 "................",
                                 "................",
                                 "......oooo......",
                                 ".....oKkkko.....",
                                 "....oKkkkkko....",
                                 "....okkpkkko....",
                                 "...oKkkkkpkko...",
                                 "...okpkkkkkko...",
                                 "...okkkkpkkko...",
                                 "..oKkkkkkkkkko..",
                                 "..okkpkkkkpkko..",
                                 "..okkkkkkkkkko..",
                                 "...okkkpkkkko...",
                                 "....oooooooo....",
                                 "................",
                                 "................"),
    "simpleend": icon("Aschholz-Baum auf Endstein", {"o": "#2a2030", "L": "#8a7a9a", "l": "#5e5070",
                                                     "t": "#4a3a2a", "T": "#6a5440", "E": "#e8e2a8",
                                                     "e": "#c8c088"},
                      [("Kontur", "o"), ("Krone", "Ll"), ("Stamm", "tT"), ("Endstein", "Ee")],
                      "................",
                      "......oooo......",
                      ".....oLLLlo.....",
                      "....oLLlLLlo....",
                      "...oLlLLLlLlo...",
                      "...oLLLlLLLlo...",
                      "....olLLLllo....",
                      ".....oottoo.....",
                      ".......tT.......",
                      ".......tT.......",
                      "..ooooottooooo..",
                      "..oEEeEEEEeEEo..",
                      "..oeEEEeEEEEeo..",
                      "..oooooooooooo..",
                      "................",
                      "................"),
    "simplerespawn": icon("Herz mit Plus (Wiederbelebung)", {"o": "#3a0808", "R": "#d42a2a", "H": "#f07070",
                                                             "d": "#8a1414", "W": "#ffffff"},
                          [("Kontur", "o"), ("Herz", "RHd"), ("Plus", "W")],
                          "................",
                          "................",
                          "...oooo..oooo...",
                          "..oHRRRooRRRRo..",
                          ".oHHRRRRRRRRRRo.",
                          ".oHRRRRWWRRRRRo.",
                          ".oRRRWWWWWWRRRo.",
                          ".oRRRWWWWWWRRdo.",
                          "..oRRRRWWRRRdo..",
                          "...oRRRWWRRdo...",
                          "....oRRRRRdo....",
                          ".....oRRRdo.....",
                          "......oRdo......",
                          ".......oo.......",
                          "................",
                          "................"),
    "simpleovens": icon("Ofen mit Feuer", {"o": "#2a2a2a", "S": "#8a8a8a", "s": "#6e6e6e", "d": "#3a3a3a",
                                           "K": "#1e1e1e", "f": "#c84a10", "F": "#f08a20", "Y": "#ffd84a"},
                        [("Kontur", "o"), ("Stein", "Ssd"), ("Öffnung", "K"), ("Feuer", "fFY")],
                        "................",
                        "................",
                        "..oooooooooooo..",
                        "..oSsSSSsSSSSo..",
                        "..oSddddddddSo..",
                        "..oSSSsSSSSsSo..",
                        "..oSSSSSSsSSSo..",
                        "..oSKKKKKKKKSo..",
                        "..oSKKKKfKKKSo..",
                        "..oSKKfKFfKKSo..",
                        "..oSKfFfYFfKSo..",
                        "..oSKFYYYYFKSo..",
                        "..oSsSSSSSSsSo..",
                        "..oooooooooooo..",
                        "................",
                        "................"),
    "simplemobheads": icon("Mob-Kopf", {"o": "#1e3a18", "G": "#5ab04a", "g": "#3e8a34", "K": "#121a10"},
                           [("Kontur", "o"), ("Kopf", "Gg"), ("Gesicht", "K")],
                           "................",
                           "................",
                           "..oooooooooooo..",
                           "..oGgGGgGGGgGo..",
                           "..oGGgGGGgGGgo..",
                           "..oGgKKGGKKgGo..",
                           "..oGGKKgGKKGGo..",
                           "..ogGGGKKGGGgo..",
                           "..oGGGKKKKGgGo..",
                           "..oGgGKKKKGGGo..",
                           "..oGGGKGgKGgGo..",
                           "..ogGGGGGGGGGo..",
                           "..oGGgGGgGGGgo..",
                           "..oooooooooooo..",
                           "................",
                           "................"),
    "simpleshops": icon("Marktstand", {"o": "#3a2410", "R": "#c83030", "W": "#f0f0f0", "P": "#8a6232",
                                       "e": "#30c060", "g": "#f0c030", "C": "#a8743c", "c": "#7a5228"},
                        [("Kontur", "o"), ("Markise", "RW"), ("Stand", "PCc"), ("Ware", "eg")],
                        "................",
                        "................",
                        "..oooooooooooo..",
                        ".oRRWWRRWWRRWWo.",
                        ".oRRWWRRWWRRWWo.",
                        "..oooooooooooo..",
                        "...P........P...",
                        "...P.ee..gg.P...",
                        "...P.ee..gg.P...",
                        "..oooooooooooo..",
                        "..oCCCCCCCCCCo..",
                        "..oCcCCCCCCcCo..",
                        "..oCCCCCCCCCCo..",
                        "..oooooooooooo..",
                        "................",
                        "................"),
    "simplewanderers": icon("Fahrender Händler", {"o": "#142446", "B": "#2e5ca8", "b": "#1f4280", "s": "#c89a6a",
                                                  "N": "#a8784a", "W": "#f0f0f0", "K": "#2a8a3a"},
                            [("Kontur", "o"), ("Kapuze", "Bb"), ("Gesicht", "sN"), ("Augen", "WK")],
                            "................",
                            "................",
                            "..oooooooooooo..",
                            "..oBBBBBBBBBBo..",
                            "..oBbBBBBBBbBo..",
                            "..oBbssssssbBo..",
                            "..oBsWKssKWsBo..",
                            "..oBsssNNsssBo..",
                            "..oBsssNNsssBo..",
                            "..oBsssNNsssBo..",
                            "..oBbssssssbBo..",
                            "..oBBbbbbbbBBo..",
                            "..oBBBBBBBBBBo..",
                            "..oooooooooooo..",
                            "................",
                            "................"),
    "simplestats": icon("Balkendiagramm", {"A": "#3a3a3a", "R": "#d84040", "r": "#9a2828", "Y": "#e8c030",
                                           "y": "#a88a20", "G": "#40b050", "g": "#2a7a38"},
                        [("Achsen", "A"), ("Balken", "RrYyGg")],
                        "................",
                        "................",
                        ".A..............",
                        ".A.........GGg..",
                        ".A.........GGg..",
                        ".A.....YYy.GGg..",
                        ".A.....YYy.GGg..",
                        ".A.....YYy.GGg..",
                        ".A.RRr.YYy.GGg..",
                        ".A.RRr.YYy.GGg..",
                        ".A.RRr.YYy.GGg..",
                        ".A.RRr.YYy.GGg..",
                        ".AAAAAAAAAAAAAA.",
                        "................",
                        "................",
                        "................"),
    "simplecrucibles": icon("Tiegel mit Glut", {"o": "#2a2a2a", "I": "#b8b8b8", "i": "#8a8a8a", "d": "#5a5a5a",
                                                "L": "#f08020", "l": "#ffd040"},
                            [("Kontur", "o"), ("Tiegel", "Iid"), ("Glut", "Ll")],
                            "................",
                            "................",
                            "................",
                            ".oooooooooooooo.",
                            ".oILLlLLLLlLLIo.",
                            "..oIIIIIIIIIIo..",
                            "..oIiiiiiiiiio..",
                            "..oIiiiiiiiiio..",
                            "..oIiiiiiiiido..",
                            "..oIiiiiiiiido..",
                            "..oIiiiiiiiddo..",
                            "...oiiiiiiddo...",
                            "....oooooooo....",
                            "................",
                            "................",
                            "................"),
    "simpleoptimization": icon("Blitz (Sammel-Mod)", {"o": "#6a4a08", "Y": "#ffe040", "y": "#e0a020"},
                               [("Kontur", "o"), ("Blitz", "Yy")],
                               "................",
                               ".........oooo...",
                               "........oYYyo...",
                               ".......oYYyo....",
                               "......oYYyo.....",
                               ".....oYYyooooo..",
                               "....oYYYYYYYyo..",
                               "....oooooYYyo...",
                               "........oYyo....",
                               ".......oYyo.....",
                               "......oYyo......",
                               ".....oYyo.......",
                               "....oYo.........",
                               "....oo..........",
                               "................",
                               "................"),
    "simpleoptrender": icon("Auge (Darstellung)", {"o": "#2a2a3a", "W": "#f0f0f0", "B": "#3a8ad8",
                                                   "K": "#101820", "w": "#ffffff"},
                            [("Kontur", "o"), ("Auge", "W"), ("Iris", "BKw")],
                            "................",
                            "................",
                            "................",
                            "................",
                            ".....oooooo.....",
                            "...ooWWWWWWoo...",
                            "..oWWWBBBBWWWo..",
                            ".oWWWBBwKBBWWWo.",
                            ".oWWWBBKKBBWWWo.",
                            "..oWWWBBBBWWWo..",
                            "...ooWWWWWWoo...",
                            ".....oooooo.....",
                            "................",
                            "................",
                            "................",
                            "................"),
    "simpleoptmemory": icon("Speicherriegel", {"o": "#0e3a14", "G": "#2a8a3a", "g": "#1e6a2a", "K": "#1a1a1a",
                                               "Y": "#e8c040"},
                            [("Kontur", "o"), ("Platine", "Gg"), ("Chips", "K"), ("Kontakte", "Y")],
                            "................",
                            "................",
                            "................",
                            "................",
                            ".oooooooooooooo.",
                            ".oGGGGGGGGGGGGo.",
                            ".oGKKGGKKGGKKGo.",
                            ".oGKKGGKKGGKKGo.",
                            ".oGGGGGGGGGGGGo.",
                            ".oYgYgYgYgYgYgo.",
                            ".oooooooooooooo.",
                            "................",
                            "................",
                            "................",
                            "................",
                            "................"),
    "simpleoptserver": icon("Server-Turm", {"o": "#1a1c20", "S": "#9aa0a8", "d": "#5a6068", "K": "#2a2e34",
                                            "G": "#40e060", "Y": "#f0c030"},
                            [("Kontur", "o"), ("Gehäuse", "Sd"), ("Einschübe", "K"), ("Lampen", "GY")],
                            "................",
                            "................",
                            "...oooooooooo...",
                            "...oSSSSSSSSo...",
                            "...oSKKKKSGSo...",
                            "...oSSSSSSSSo...",
                            "...oSKKKKSGSo...",
                            "...oSSSSSSSSo...",
                            "...oSKKKKSYSo...",
                            "...oSSSSSSSSo...",
                            "...oSKKKKSGSo...",
                            "...oSSSSSSSSo...",
                            "...oddddddddo...",
                            "...oooooooooo...",
                            "................",
                            "................"),
    "simplestorage": icon("Holzkiste", {"o": "#3a2410", "W": "#b88a50", "w": "#8a6232", "B": "#c8a060"},
                          [("Kontur", "o"), ("Rahmen", "W"), ("Bretter", "w"), ("Strebe", "B")],
                          "................",
                          "................",
                          "..oooooooooooo..",
                          "..oWWWWWWWWWWo..",
                          "..oWBBwwwwwwWo..",
                          "..oWwBBwwwwwWo..",
                          "..oWwwBBwwwwWo..",
                          "..oWwwwBBwwwWo..",
                          "..oWwwwwBBwwWo..",
                          "..oWwwwwwBBwWo..",
                          "..oWwwwwwwBBWo..",
                          "..oWwwwwwwwBWo..",
                          "..oWWWWWWWWWWo..",
                          "..oooooooooooo..",
                          "................",
                          "................"),
    "simplemusic": icon("Notenblatt", {"o": "#6a5a3a", "P": "#f0e8d0", "L": "#b8b0a0", "K": "#1a1a1a"},
                        [("Kontur", "o"), ("Papier", "P"), ("Linien", "L"), ("Noten", "K")],
                        "................",
                        "..oooooooooooo..",
                        "..oPPPPPPPPPPo..",
                        "..oPPPPPKKKKPo..",
                        "..oLLLLLKLLKLo..",
                        "..oPPPPPKPPKPo..",
                        "..oLLLLLKLLKLo..",
                        "..oPPPPPKPPKPo..",
                        "..oLLLLKKLKKLo..",
                        "..oPPPPKKPKKPo..",
                        "..oLLLLLLLLLLo..",
                        "..oPPPPPPPPPPo..",
                        "..oLLLLLLLLLLo..",
                        "..oPPPPPPPPPPo..",
                        "..oooooooooooo..",
                        "................"),
}
PROCEDURAL = {"simpleblocks": blocks_icon, "simpleoctets": octets_icon, "simplemodels": models_icon,
              "simplebeacons": beacon_icon}


def spec(mod_id: str) -> dict | None:
    if mod_id in PROCEDURAL:
        return PROCEDURAL[mod_id]()
    return ICONS.get(mod_id)


def to_doc(sp: dict, mod_id: str) -> dict:
    art, pal, groups = sp["art"], sp["pal"], sp["groups"]
    if len(art) != 16 or any(len(r) != 16 for r in art):
        raise ValueError(f"{mod_id}: art must be 16 rows of 16 characters")
    used = {ch for r in art for ch in r} - {"."}
    grouped = [ch for _, chars in groups for ch in chars]
    if used - set(grouped) or len(grouped) != len(set(grouped)) or set(grouped) - set(pal):
        raise ValueError(f"{mod_id}: characters {sorted(used - set(grouped))} without layer, or layer/palette mismatch")
    if any(art[0][c] != "." or art[15][c] != "." or art[c][0] != "." or art[c][15] != "." for c in range(16)):
        raise ValueError(f"{mod_id}: keep the 1 px border free (AGENTS.md rule 7)")
    order = [ch for ch in grouped if ch in used]
    index = {ch: i for i, ch in enumerate(order)}
    out = []
    for name, chars in groups:
        rows = ["".join(format(index[ch], "02x") if ch in chars else ".." for ch in r) for r in art]
        if any(ch in used for ch in chars):
            out.append({"name": name, "visible": True, "opacity": 100, "blend": "normal", "rows": rows})
    return {"format": L.FORMAT, "version": 1, "width": 16, "height": 16, "digits": 2,
            "palette": [pal[ch].lower() + "ff" for ch in order], "layers": out}


def upscale(rgba: bytes, w: int, h: int, k: int) -> bytes:
    out = bytearray()
    for y in range(h * k):
        row = rgba[(y // k) * w * 4:(y // k + 1) * w * 4]
        out += b"".join(bytes(row[x * 4:x * 4 + 4]) * k for x in range(w))
    return bytes(out)


def outputs(entry: dict) -> dict[Path, bytes]:
    """Files of one mod (path -> content)."""
    mod_id = entry.get("modId", entry["id"])
    base = asset_dir(entry)
    files: dict[Path, bytes] = {}
    if entry["id"] in OWNER:
        doc = json.loads((base / "icon.layers.json").read_text(encoding="utf-8"))
    else:
        sp = spec(entry["id"])
        if sp is None:
            raise ValueError(f"no icon drawn for {entry['id']}")
        doc = to_doc(sp, mod_id)
        files[base / "icon.layers.json"] = L.dumps(doc).encode()
    rgba = bytes(L.compose(doc))
    files[base / "icon.png"] = L.png_write(16, 16, rgba)
    files[base / "icon_128.png"] = L.png_write(16 * SCALE, 16 * SCALE, upscale(rgba, 16, 16, SCALE))
    return files


def same_pixels(path: Path, data: bytes) -> bool:
    if not path.is_file():
        return False
    return L.png_read(path.read_bytes()) == L.png_read(data) if path.suffix == ".png" else path.read_bytes() == data


def import_owner() -> None:
    """One-time: sample the owner's upscaled icons at the cell centres into a 16x16 layer source."""
    for entry in modules():
        if entry["id"] not in OWNER:
            continue
        base = asset_dir(entry)
        w, h, px = L.png_read((base / "icon.png").read_bytes())
        step = w / 16
        rgba = bytearray()
        for y in range(16):
            for x in range(16):
                i = (int((y + 0.5) * step) * w + int((x + 0.5) * step)) * 4
                rgba += px[i:i + 4]
        doc = L.from_rgba(16, 16, bytes(rgba), "Besitzer-Bild")
        (base / "icon.layers.json").write_text(L.dumps(doc), encoding="utf-8")
        print("imported", base / "icon.layers.json", f"({w}x{h}, step {step})")


def sheet(out: Path, scale: int = 8) -> None:
    entries = modules()
    cols = 8
    cell = 16 * scale + 8
    rows = (len(entries) + cols - 1) // cols
    W, H = cols * cell, rows * cell
    img = bytearray()
    for y in range(H):
        for x in range(W):
            v = 0x30 if ((x // 8) + (y // 8)) % 2 else 0xd8 if (y // cell) % 2 == 0 else 0x50
            img += bytes((v, v, v, 255))
    for n, entry in enumerate(entries):
        data = outputs(entry)[asset_dir(entry) / "icon.png"]
        _, _, rgba = L.png_read(data)
        ox, oy = (n % cols) * cell + 4, (n // cols) * cell + 4
        for y in range(16 * scale):
            for x in range(16 * scale):
                s = ((y // scale) * 16 + x // scale) * 4
                a = rgba[s + 3]
                if a:
                    d = ((oy + y) * W + ox + x) * 4
                    img[d:d + 4] = bytes(rgba[s:s + 3]) + b"\xff"
    out.write_bytes(L.png_write(W, H, bytes(img)))
    print("sheet", out, [e["id"] for e in entries])


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--import-owner", action="store_true")
    ap.add_argument("--sheet")
    args = ap.parse_args(argv)
    if args.import_owner:
        import_owner()
        return 0
    if args.sheet:
        sheet(Path(args.sheet))
        return 0
    bad = []
    for entry in modules():
        for path, data in outputs(entry).items():
            if args.check:
                if not same_pixels(path, data):
                    bad.append(path.relative_to(ROOT).as_posix())
            elif not same_pixels(path, data):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
                print("wrote", path.relative_to(ROOT).as_posix())
    if bad:
        print("mod icons out of date (run python tools/textures/mod_icons.py):", *bad, sep="\n  ")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
