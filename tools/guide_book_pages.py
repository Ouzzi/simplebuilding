#!/usr/bin/env python3
"""Checks that every page of the guide books (com.simplebuilding.guide.GuideBooks) fits the vanilla
book screen, in English and German, and that the custom book screen gets clean text.

The vanilla BookViewScreen draws at most 14 lines of 114 px and silently cuts off the rest, so an
overlong page is not an error anywhere - it just loses its last lines. This script rebuilds every
page the way GuideBooks.pages() does (bold title, blank line, text, "« Contents" link; contents
pages with one link per chapter; each shelf hub's topic pages) from the lang files, wraps it with
the glyph widths of the vanilla default font (read from the 26.3 client jar in the Gradle cache) and
fails if a page needs more than 13 lines - one line of margin for wrap differences.

Everything else is read from the sources, so the script never needs editing when a book changes:
  - the books, their shelves and key items from GuideBooks.java (enum Book, keyItem),
  - the chapters and their icons (the %3$s argument of every chapter text) from GuideContent.java,
  - vanilla item names (key items, icons) from the 26.3 client jar (en) and the asset index (de).

Formatting rules checked as well (both languages): no double spaces, no space before a line break,
no empty paragraphs, bullets written as "- ", titles on at most two lines, and every chapter text
has the same placeholders in English and German.

    python tools/guide_book_pages.py          # check, print the fullest pages
    python tools/guide_book_pages.py -v       # print every page as it wraps
    python tools/guide_book_pages.py --stats  # pages and chapters per book
"""
import io
import json
import os
import re
import sys
import zipfile

from PIL import Image

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
GRADLE = os.path.expanduser("~/.gradle/caches/fabric-loom")
JAR = os.path.join(GRADLE, "26.3", "minecraft-client.jar")
ASSET_INDEX = os.path.join(GRADLE, "assets", "indexes", "26.3-34.json")
LANG_DIR = "src/main/resources/assets/simplebuilding/lang"
GUIDE_BOOKS = "common/src/shared/java/com/simplebuilding/guide/GuideBooks.java"
GUIDE_CONTENT = "common/src/shared/java/com/simplebuilding/guide/GuideContent.java"
MAX_LINES = 13
MAX_TITLE_LINES = 2
LINE_WIDTH = 114
TOPICS_ON_FIRST_PAGE = 2
TOPICS_PER_PAGE = 4
# GuideBooks.CONTENTS_FIRST_TOPIC / CONTENTS_FIRST_GUIDE / CONTENTS_MORE: links on contents page 1 and each further one.
CONTENTS_FIRST_TOPIC, CONTENTS_FIRST_GUIDE, CONTENTS_MORE = 6, 11, 11
# What the keybind arguments render as.
KEYBINDS = {"%1$s": "G", "%2$s": "B"}

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


def cut_words(segments, line_width=LINE_WIDTH):
    """Words that do not fit on one line and get cut in the middle (vanilla breaks them hard)."""
    out = []
    for text, bold in segments:
        for word in re.split(r"[ \n]", text):
            # Config paths and commands (tweaks.pads.enableFlypads, giveGuideBookOnFirstJoin) must be
            # written out in full so admins can type them; only ordinary words must never be cut.
            if re.search(r"[a-z][A-Z]|[a-z]\.[a-z]|^/", word):
                continue
            if word and sum(width(g, bold) for g in word) > line_width:
                out.append(word)
    return out


def wrap(segments, line_width=LINE_WIDTH):
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
            while i < len(p) and w + width(*p[i]) <= line_width:
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


# ---------------------------------------------------------------------------------------------
# Sources
# ---------------------------------------------------------------------------------------------

def read(path):
    with open(os.path.join(REPO, path), encoding="utf-8") as f:
        return f.read()


def books():
    """[(enum, id, shelf, key item id)] in enum order."""
    src = read(GUIDE_BOOKS)
    enum = re.findall(r'^\s+([A-Z_]+)\("([a-z_]+)", "([a-z_]+)", Shelf\.(MOD|VANILLA)\)', src, re.M)
    keys = dict(re.findall(r'case ([A-Z_]+) -> (Items\.[A-Z_]+|ModItems\.[A-Z_]+);', src[src.index("keyItem(Book"):]))
    out = []
    for name, book_id, _item, shelf in enum:
        ref = keys[name]
        ns = "minecraft" if ref.startswith("Items.") else "simplebuilding"
        out.append((name, book_id, shelf, ns + ":" + ref.split(".")[1].lower()))
    return out


def chapter_icons():
    """{enum: [icon id per chapter]} from the STYLES.put(...) blocks of GuideContent."""
    src = read(GUIDE_CONTENT)
    out = {}
    for m in re.finditer(r'STYLES\.put\(GuideBooks\.Book\.([A-Z_]+),(.*?)\)\)\);', src, re.S):
        out[m.group(1)] = re.findall(r'\bch\("([a-z0-9_:]+)"', m.group(2))
    return out


def vanilla_names():
    z = zipfile.ZipFile(JAR)
    en = json.loads(z.read("assets/minecraft/lang/en_us.json"))
    index = json.load(open(ASSET_INDEX))["objects"]
    h = index["minecraft/lang/de_de.json"]["hash"]
    de = json.load(open(os.path.join(GRADLE, "assets", "objects", h[:2], h), encoding="utf-8"))
    return {"en_us": en, "de_de": de}


def item_name(item_id, lang, vanilla):
    ns, path = item_id.split(":")
    table = lang if ns == "simplebuilding" else vanilla
    for kind in ("item", "block"):
        key = "%s.%s.%s" % (kind, ns, path)
        if key in table:
            return table[key]
    return path.replace("_", " ").title()


# ---------------------------------------------------------------------------------------------
# Pages
# ---------------------------------------------------------------------------------------------

def pages(lang, vanilla, all_books, icons):
    t = lambda key: lang[key]
    back = [("\n", False), (t("book.simplebuilding.back"), False)]
    out = []
    for name, book, shelf, _key in all_books:
        base = "book.simplebuilding." + book
        hub = [b for b in all_books if b[2] == shelf][0][0] == name
        chapters = len(icons.get(name, []))
        segs = [(t(base + ".title"), True), ("\n\n", False)]
        if not hub:
            segs += [(t(base + ".intro"), False), ("\n\n", False)]
        links = [t("%s.%d.title" % (base, i)) for i in range(1, chapters + 1)]
        if hub:
            links.append(t("book.simplebuilding.guide.topics.title"))
        first = CONTENTS_FIRST_GUIDE if hub else CONTENTS_FIRST_TOPIC
        out.append((book, "contents", segs + [("\n".join(links[:first]), False)]))
        rest = links[first:]
        for n in range(0, len(rest), CONTENTS_MORE):
            out.append((book, "contents %d" % (2 + n // CONTENTS_MORE), [("\n".join(rest[n:n + CONTENTS_MORE]), False)]))
        for i in range(1, chapters + 1):
            icon = item_name(icons[name][i - 1], lang, vanilla)
            text = t("%s.%d.text" % (base, i))
            text = re.sub(r"%\d\$s", lambda m: KEYBINDS.get(m.group(0), icon), text)
            out.append((book, str(i), [(t("%s.%d.title" % (base, i)), True), ("\n", False), (text, False)] + back))
        if hub:
            topics = [b for b in all_books if b[2] == shelf and b[0] != name]

            def entry(topic):
                recipe = t("book.simplebuilding.guide.topics.recipe").replace("%s", item_name(topic[3], lang, vanilla))
                return [(t("book.simplebuilding.%s.title" % topic[1]), False), ("\n", False), (recipe, False)]

            def listing(ts):
                segs = []
                for j, topic in enumerate(ts):
                    segs += ([("\n", False)] if j else []) + entry(topic)
                return segs

            head = [(t("book.simplebuilding.guide.topics.title"), True), ("\n\n", False),
                    (t("book.simplebuilding.guide.topics.text"), False), ("\n\n", False)]
            out.append((book, "topics 1", head + listing(topics[:TOPICS_ON_FIRST_PAGE]) + back))
            more = topics[TOPICS_ON_FIRST_PAGE:]
            for n in range(0, len(more), TOPICS_PER_PAGE):
                out.append((book, "topics %d" % (2 + n // TOPICS_PER_PAGE), listing(more[n:n + TOPICS_PER_PAGE]) + back))
    return out


def style_problems(lang, other, locale, all_books, icons):
    problems = []
    for name, book, _shelf, _key in all_books:
        base = "book.simplebuilding." + book
        for i in range(1, len(icons.get(name, [])) + 1):
            for part in ("title", "text"):
                key = "%s.%d.%s" % (base, i, part)
                text = lang.get(key)
                if text is None:
                    problems.append("%s %s: missing" % (locale, key))
                    continue
                if "  " in text or " \n" in text or "\n " in text or "\n\n" in text or text != text.strip():
                    problems.append("%s %s: stray spaces or an empty paragraph" % (locale, key))
                if re.search(r"(^|\n)[*•-](?! )", text):
                    problems.append("%s %s: bullets are written as '- '" % (locale, key))
                if sorted(re.findall(r"%\d\$s", text)) != sorted(re.findall(r"%\d\$s", other.get(key, ""))):
                    problems.append("%s %s: placeholders differ from the other language" % (locale, key))
            title = lang.get("%s.%d.title" % (base, i), "")
            if len(wrap([(title, True)])) > MAX_TITLE_LINES:
                problems.append("%s %s.%d.title: more than %d lines" % (locale, base, i, MAX_TITLE_LINES))
    return problems


def main():
    verbose = "-v" in sys.argv
    stats = "--stats" in sys.argv
    all_books = books()
    icons = chapter_icons()
    vanilla = vanilla_names()
    langs = {}
    for locale in ("en_us", "de_de"):
        with open(os.path.join(REPO, LANG_DIR, locale + ".json"), encoding="utf-8") as f:
            langs[locale] = json.load(f)
    bad = 0
    for locale in ("en_us", "de_de"):
        lang = langs[locale]
        other = langs["de_de" if locale == "en_us" else "en_us"]
        for problem in style_problems(lang, other, locale, all_books, icons):
            print(problem)
            bad += 1
        per_book = {}
        for book, name, segs in pages(lang, vanilla[locale], all_books, icons):
            for word in cut_words(segs):
                print("%-6s %-18s %-12s cut word: %s" % (locale, book, name, word))
                bad += 1
            lines = wrap(segs)
            per_book[book] = per_book.get(book, 0) + 1
            over = len(lines) > MAX_LINES
            bad += over
            if over or verbose or len(lines) == MAX_LINES:
                print("%-6s %-18s %-12s %2d lines%s" % (locale, book, name, len(lines), "  <-- too long" if over else ""))
                if over or verbose:
                    for line in lines:
                        print("        |" + line)
        if stats:
            for name, book, shelf, _ in all_books:
                print("%-6s %-8s %-18s %2d chapters %3d pages" % (locale, shelf, book, len(icons.get(name, [])), per_book.get(book, 0)))
    print("problems: %d" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
