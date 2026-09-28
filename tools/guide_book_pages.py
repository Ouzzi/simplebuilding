#!/usr/bin/env python3
"""Checks that every page of the guide books (com.simplebuilding.guide.GuideBooks) fits the vanilla
book screen, in English and German, on both Minecraft lines.

The vanilla BookViewScreen draws at most 14 lines of 114 px and silently cuts off the rest, so an
overlong page is not an error anywhere - it just loses its last lines. This script rebuilds every
page the way GuideBooks.pages() does (bold title, blank line, text, "« Contents" link; contents page
with one link per chapter; the guide's two topic pages) from the lang files, wraps it with the
glyph widths of the vanilla default font (read from the 26.2 client jar in the Gradle cache) and
fails if a page needs more than 13 lines - one line of margin for wrap differences.

    python tools/guide_book_pages.py          # check, print the fullest pages
    python tools/guide_book_pages.py -v       # print every page as it wraps

Keep the page layout here in step with GuideBooks.pages().
"""
import io
import json
import os
import re
import sys
import zipfile

from PIL import Image

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar")
LANG_DIRS = ["src/main/resources/assets/simplebuilding/lang",
             "mc1_21_11/fabric/src/main/resources/assets/simplebuilding/lang"]
MAX_LINES = 13
LINE_WIDTH = 114
BOOKS = ["guide", "tools", "building", "storage", "machines", "end", "tweaks", "trims", "admin"]
TOPICS_ON_FIRST_PAGE = 2
TOPICS_PER_PAGE = 4
# GuideBooks.CONTENTS_FIRST_TOPIC / CONTENTS_FIRST_GUIDE / CONTENTS_MORE: links on contents page 1 and each further one.
CONTENTS_FIRST_TOPIC, CONTENTS_FIRST_GUIDE, CONTENTS_MORE = 6, 11, 11
# What the keybind arguments and key items render as (vanilla names, en/de).
KEYBINDS = {"%1$s": "G", "%2$s": "B"}
KEY_ITEMS = {
    "tools": ("Stone Chisel", "Steinmeißel"), "building": ("Brick", "Ziegel"), "storage": ("Chest", "Truhe"),
    "machines": ("Piston", "Kolben"), "end": ("Ender Pearl", "Enderperle"),
    "tweaks": ("Stone Pressure Plate", "Steindruckplatte"), "trims": ("Amethyst Shard", "Amethystscherbe"),
    "admin": ("Redstone Comparator", "Redstone-Komparator"),
}

_WIDTHS = {}


def _load_widths():
    z = zipfile.ZipFile(JAR)
    providers = json.loads(z.read("assets/minecraft/font/include/default.json"))["providers"]
    for p in providers:
        if p.get("type") != "bitmap":
            continue
        img = Image.open(io.BytesIO(z.read("assets/minecraft/textures/" + p["file"].split(":")[1]))).convert("RGBA")
        rows = p["chars"]
        cw, ch = img.width // len(rows[0]), img.height // len(rows)
        scale = p.get("height", 8) / ch
        for r, row in enumerate(rows):
            for c, glyph in enumerate(row):
                if glyph in _WIDTHS or glyph == "\u0000":
                    continue
                w = 0
                for x in range(cw - 1, -1, -1):
                    if any(img.getpixel((c * cw + x, r * ch + y))[3] for y in range(ch)):
                        w = x + 1
                        break
                _WIDTHS[glyph] = int(0.5 + w * scale) + 1
    _WIDTHS[" "] = 4


def width(glyph, bold):
    if not _WIDTHS:
        _load_widths()
    return _WIDTHS.get(glyph, 6) + (1 if bold and glyph != " " else 0)


def wrap(segments):
    """segments: [(text, bold)] -> wrapped lines (greedy, break at the last space that fits)."""
    lines, para = [], []
    paras = []
    for text, bold in segments:
        for glyph in text:
            if glyph == "\n":
                paras.append(para)
                para = []
            else:
                para.append((glyph, bold))
    paras.append(para)
    for p in paras:
        if not p:
            lines.append("")
            continue
        start = 0
        while start < len(p):
            w, i, space = 0, start, -1
            while i < len(p) and w + width(*p[i]) <= LINE_WIDTH:
                if p[i][0] == " ":
                    space = i
                w += width(*p[i])
                i += 1
            if i >= len(p):
                lines.append("".join(g for g, _ in p[start:]))
                break
            end = space if space > start else i
            lines.append("".join(g for g, _ in p[start:end]))
            start = end + 1 if end == space else end
    return lines


def pages(lang, locale):
    t = lambda key: lang[key]
    sub = lambda text: re.sub(r"%\d\$s", lambda m: KEYBINDS[m.group(0)], text)
    back = [("\n", False), (t("book.simplebuilding.back"), False)]
    out = []
    for book in BOOKS:
        base = "book.simplebuilding." + book
        chapters = max(int(m.group(1)) for k in lang for m in [re.fullmatch(re.escape(base) + r"\.(\d+)\.title", k)] if m)
        segs = [(t(base + ".title"), True), ("\n\n", False)]
        if book != "guide":
            segs += [(t(base + ".intro"), False), ("\n\n", False)]
        links = [t("%s.%d.title" % (base, i)) for i in range(1, chapters + 1)]
        if book == "guide":
            links.append(t("book.simplebuilding.guide.topics.title"))
        first = CONTENTS_FIRST_GUIDE if book == "guide" else CONTENTS_FIRST_TOPIC
        out.append(("%s contents" % book, segs + [("\n".join(links[:first]), False)]))
        rest = links[first:]
        for n in range(0, len(rest), CONTENTS_MORE):
            out.append(("%s contents %d" % (book, 2 + n // CONTENTS_MORE), [("\n".join(rest[n:n + CONTENTS_MORE]), False)]))
        for i in range(1, chapters + 1):
            out.append(("%s %d" % (book, i), [(t("%s.%d.title" % (base, i)), True), ("\n\n", False),
                                              (sub(t("%s.%d.text" % (base, i))), False)] + back))
        if book == "guide":
            def entry(topic):
                recipe = t("book.simplebuilding.guide.topics.recipe").replace("%s", KEY_ITEMS[topic][locale == "de_de"])
                return [(t("book.simplebuilding.%s.title" % topic), False), ("\n", False), (recipe, False)]

            def listing(topics):
                segs = []
                for j, topic in enumerate(topics):
                    segs += ([("\n", False)] if j else []) + entry(topic)
                return segs

            first = [(t("book.simplebuilding.guide.topics.title"), True), ("\n\n", False),
                     (t("book.simplebuilding.guide.topics.text"), False), ("\n\n", False)]
            out.append(("guide topics 1", first + listing(BOOKS[1:1 + TOPICS_ON_FIRST_PAGE]) + back))
            rest = BOOKS[1 + TOPICS_ON_FIRST_PAGE:]
            for n in range(0, len(rest), TOPICS_PER_PAGE):
                out.append(("guide topics %d" % (2 + n // TOPICS_PER_PAGE), listing(rest[n:n + TOPICS_PER_PAGE]) + back))
    return out


def main():
    verbose = "-v" in sys.argv
    bad = 0
    for lang_dir in LANG_DIRS:
        for locale in ("en_us", "de_de"):
            path = os.path.join(REPO, lang_dir, locale + ".json")
            with open(path, encoding="utf-8") as f:
                lang = json.load(f)
            for name, segs in pages(lang, locale):
                lines = wrap(segs)
                over = len(lines) > MAX_LINES
                bad += over
                if over or verbose or len(lines) == MAX_LINES:
                    print("%-55s %-6s %-18s %2d lines%s" % (lang_dir, locale, name, len(lines), "  <-- too long" if over else ""))
                    if over or verbose:
                        for line in lines:
                            print("        |" + line)
    print("pages over %d lines: %d" % (MAX_LINES, bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
