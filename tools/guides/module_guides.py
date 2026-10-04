#!/usr/bin/env python3
"""
Guide books for the SimpleBuilding modules - the single source of truth (plan: docs/ai/PLAN-MODUL-GUIDES-2026-10-05.md).

Every module gets a guide that works without SimpleBuilding:

  item modules    <ns>:guide_book, a Vanilla WrittenBookItem whose default written_book_content holds translatable
                  pages (opened by Vanilla on use, in the reader's language), a shapeless recipe "book + one
                  typical Vanilla item", the recipe-book unlock, an item model, and an FTB Quests start chapter whose
                  first quest gives the book (copied into config/ftbquests only when FTB Quests is installed).
  client modules  no item (they must stay client-only installable); the same pages open locally in the Vanilla
                  book screen with the client command /<ns> guide.

Pages come from modules/<id>/wiki/manual.json (code-backed texts): an intro page, then one bold title plus the
summary per feature (config/trade/link/loader rows skipped), wrapped to the Vanilla book page by pixel width and
split over continuation pages. English and German always get the same page count.

Written per module:
  <shared java>/<package>/guide/<Class>.java   from tools/guides/ModuleGuide.java.in or ClientGuide.java.in
  lang en_us/de_de                             item name, <ns>.guide.page.<n>(.title), quests.<ns>.start.*
  item modules additionally                    recipe, recipe advancement, item model, data/<ns>/ftbquests/*

Later (simplelib merged): the template becomes com.simplelib.guide.ModuleGuideBook; only this script changes.

Usage:
  python tools/guides/module_guides.py          write all files
  python tools/guides/module_guides.py --check  fail if anything is stale (gradlew check: checkModuleGuides)
"""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
DATA_VERSION = 1
FTB_FILE_VERSION = 13

# Vanilla book page: BookViewScreen draws 114 px wide, 14 lines. Kept a little smaller so estimated glyph widths
# never overflow in the real font.
PAGE_WIDTH = 112
PAGE_LINES = 14
MAX_PAGES = 100
SKIP_PREFIXES = ("config_", "config-", "trade_", "money_links_")
SKIP_IDS = {"forge_263", "forge_support"}

MODULES = [
    dict(module="simpleriding", ns="simpleriding", package="com.simpleriding.guide", cls="RidingGuide", kind="item",
         author="Simple Riding", recipe="minecraft:hay_block",
         name=("Simple Riding Guide", "Simple-Riding-Handbuch"),
         intro=("Saddle speed, horse jumps, nautilus dashes, mount armor utilities, loot and trades.",
                "Sattel-Tempo, Pferdesprünge, Nautilus-Sprints, Reittier-Rüstungshilfen, Beute und Handel.")),
    dict(module="simplefun", ns="simplefun", package="com.simplefun.guide", cls="FunGuide", kind="item",
         author="Simple Fun", recipe="minecraft:brick",
         name=("Simple Fun Guide", "Simple-Fun-Handbuch"),
         intro=("Bounded playful mechanics, throwable bricks, cosmetic transformations and farm-animal heads.",
                "Begrenzte verspielte Mechaniken, Wurfziegel, kosmetische Verwandlungen und Nutztierköpfe.")),
    dict(module="simplemoney", ns="simplemoney", package="com.simplemoney.guide", cls="MoneyGuide", kind="item",
         author="Simple Money", recipe="minecraft:gold_nugget",
         name=("Simple Money Guide", "Simple-Money-Handbuch"),
         intro=("Currency production, weighted villager trades and treasure loot.",
                "Geldherstellung, gewichtete Dorfbewohner-Angebote und Schatzbeute.")),
    dict(module="simplesandwiches", ns="simplesandwiches", package="com.simplesandwiches.guide", cls="SandwichGuide",
         kind="item", author="Simple Sandwiches", recipe="minecraft:bread",
         name=("Simple Sandwiches Guide", "Simple-Sandwiches-Handbuch"),
         intro=("Sandwiches on a cutting board, a kitchen knife, cheese and butter from the cauldron, and eating "
                "straight out of bundles.",
                "Sandwiches auf dem Schneidebrett, ein Küchenmesser, Käse und Butter aus dem Kessel und Essen direkt "
                "aus Bündeln.")),
    dict(module="simplequalityoflife", ns="simplequalityoflife", package="com.simplequalityoflife.guide",
         cls="QolGuide", kind="item", author="Simple Quality of Life", recipe="minecraft:chest",
         name=("Simple Quality of Life Guide", "Simple-Quality-of-Life-Handbuch"),
         intro=("Server-controlled movement, farming, mob, weather and vault utilities.",
                "Servergesteuerte Hilfen für Bewegung, Landwirtschaft, Mobs, Wetter und Tresore.")),
    dict(module="simplemodels", ns="simplemodels", package="com.simplebuilding.modules.simplemodels.guide",
         cls="ModelsGuide", kind="item", author="Simple Models", recipe="minecraft:item_frame",
         name=("Simple Models Guide", "Simple-Models-Handbuch"),
         intro=("Server-approved custom item models, anvil assignment and a searchable model browser.",
                "Vom Server freigegebene eigene Itemmodelle, Zuweisung am Amboss und ein durchsuchbarer Modell-Browser.")),
    dict(module="simpledimensions", ns="simpledimension", package="com.simplebuilding.modules.simpledimensions.guide",
         cls="DimensionsGuide", kind="item", author="Simple Dimensions", recipe="minecraft:flint_and_steel",
         name=("Simple Dimensions Guide", "Simple-Dimensions-Handbuch"),
         intro=("Configurable dimensions, light arches and safe linked portals.",
                "Konfigurierbare Dimensionen, Lichtbögen und sicher verknüpfte Portale.")),
    dict(module="simpletweaks", ns="simpletweaks", package="com.simplebuilding.modules.simpletweaks.guide",
         cls="TweaksGuide", kind="item", author="Simple Tweaks", recipe="minecraft:paper",
         name=("Simple Tweaks Guide", "Simple-Tweaks-Handbuch"),
         intro=("Legacy compatibility and staged server-authoritative claims, disabled by default.",
                "Kompatibilität mit alten Ständen und stufenweise, serverseitige Claims, standardmäßig aus.")),
    dict(module="simplevisuals", ns="simplevisuals", package="com.simplevisuals.guide", cls="VisualsGuide",
         kind="client", author="Simple Visuals",
         intro=("Bounded client HUD, tooltips, anvil formatting and Vanilla immersion particles. "
                "Open this guide again with /simplevisuals guide.",
                "Begrenzte Client-Anzeigen, Tooltips, Amboss-Formatierung und Vanilla-Immersionspartikel. "
                "Dieses Handbuch öffnet /simplevisuals guide.")),
    dict(module="simplesounds", ns="simplesounds", package="com.simplebuilding.modules.simplesounds.guide",
         cls="SoundsGuide", kind="client", author="Simple Sounds",
         intro=("Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects. "
                "Open this guide again with /simplesounds guide.",
                "Begrenzte lokale Vanilla-Klänge zu den zwölf Immersionseffekten von Simple Visuals. "
                "Dieses Handbuch öffnet /simplesounds guide.")),
]

SETTINGS = ("Settings", "Einstellungen",
            "More settings are in the mod's config screen.",
            "Weitere Einstellungen findest du im Konfigurationsbildschirm der Mod.")
QUEST = {
    "title": ("Welcome to %s", "Willkommen bei %s"),
    "subtitle": ("Your free guide", "Dein Handbuch gratis"),
    "welcome_title": ("A free guide", "Ein Handbuch gratis"),
    "welcome_desc": ("Craft or pick up a crafting table and claim the %s as a reward. You can also craft it from a "
                     "book and %s.",
                     "Stelle eine Werkbank her oder hebe eine auf und hol dir das %s als Belohnung. Du kannst es auch "
                     "aus einem Buch und %s herstellen."),
}
RECIPE_ITEM_NAMES = {  # (English with article, German with article) for the quest text
    "minecraft:hay_block": ("a hay bale", "einem Heuballen"),
    "minecraft:brick": ("a brick", "einem Ziegel"),
    "minecraft:gold_nugget": ("a gold nugget", "einem Goldklumpen"),
    "minecraft:bread": ("bread", "Brot"),
    "minecraft:chest": ("a chest", "einer Truhe"),
    "minecraft:item_frame": ("an item frame", "einem Rahmen"),
    "minecraft:flint_and_steel": ("flint and steel", "einem Feuerzeug"),
    "minecraft:paper": ("paper", "Papier"),
}


# --------------------------------------------------------------------------------------------
# Page layout
# --------------------------------------------------------------------------------------------

NARROW = {"i": 2, "!": 2, ".": 2, ",": 2, ":": 2, ";": 2, "'": 2, "|": 2, "l": 3, "`": 3, "t": 4, "I": 4, " ": 4,
          "[": 4, "]": 4, "(": 4, ")": 4, "{": 4, "}": 4, "\"": 4, "*": 4, "f": 5, "k": 5, "<": 5, ">": 5}


def text_width(text: str, bold: bool = False) -> int:
    return sum(NARROW.get(ch, 6) + (1 if bold else 0) for ch in text)


def wrap(text: str, bold: bool = False) -> list[str]:
    lines, line = [], ""
    for word in text.split():
        candidate = word if not line else line + " " + word
        if text_width(candidate, bold) <= PAGE_WIDTH:
            line = candidate
            continue
        if line:
            lines.append(line)
        while text_width(word, bold) > PAGE_WIDTH:  # very long token: hard break
            cut = len(word)
            while text_width(word[:cut], bold) > PAGE_WIDTH:
                cut -= 1
            lines.append(word[:cut])
            word = word[cut:]
        line = word
    if line:
        lines.append(line)
    return lines


def body_room(title: str) -> int:
    return PAGE_LINES - len(wrap(title, True)) - 1  # one blank line under the title


def chunks(title: str, body: str) -> list[str]:
    """Split body into page bodies that fit under the title: whole sentences where possible, never inside a word."""
    room = body_room(title)
    if room < 2:
        raise SystemExit(f"title too long for a page: {title!r}")
    fits = lambda words: len(wrap(" ".join(words))) <= room  # noqa: E731
    sentences, current = [], []
    for word in body.split():
        current.append(word)
        if word.endswith((".", ";", "!", "?", ":")):
            sentences.append(current)
            current = []
    if current:
        sentences.append(current)
    out, page = [], []
    for sentence in sentences:
        if fits(page + sentence):
            page += sentence
            continue
        if page:
            out.append(page)
            page = []
        for word in sentence:  # a sentence longer than a page: fill word by word
            if page and not fits(page + [word]):
                out.append(page)
                page = []
            page.append(word)
    out.append(page)
    return [" ".join(words) for words in out]


def split_to(title: str, body: str, count: int) -> list[str]:
    parts = chunks(title, body)
    while len(parts) < count:  # the shorter language: split its longest part until both have the same pages
        i = max(range(len(parts)), key=lambda k: len(parts[k].split()))
        words = parts[i].split()
        if len(words) < 2:
            parts.append("")
            continue
        half = len(words) // 2
        parts[i:i + 1] = [" ".join(words[:half]), " ".join(words[half:])]
    return parts


def build_pages(cfg: dict) -> list[tuple[tuple[str, str], tuple[str, str]]]:
    """[((title_en, body_en), (title_de, body_de)), ...]"""
    manual = json.loads((REPO / "modules" / cfg["module"] / "wiki/manual.json").read_text(encoding="utf-8"))
    entries = [((cfg["author"], cfg["intro"][0]), (cfg["author"], cfg["intro"][1]))]
    has_config = False
    for feature in manual["features"]:
        fid = feature["id"]
        if fid.startswith(SKIP_PREFIXES) or fid in SKIP_IDS:
            has_config |= fid.startswith(("config_", "config-")) or fid == "configuration"
            continue
        en, de = feature["en"], feature.get("de")
        if not de or not de.get("summary"):
            raise SystemExit(f"{cfg['module']}: feature {fid} has no German summary in manual.json")
        titles = feature.get("title") or {"en": en["title"], "de": de["title"]}  # simplemodels keeps titles apart
        entries.append(((titles["en"], en["summary"]), (titles["de"], de["summary"])))
    if has_config:
        entries.append(((SETTINGS[0], SETTINGS[2]), (SETTINGS[1], SETTINGS[3])))
    pages = []
    for (te, be), (td, bd) in entries:
        count = max(len(chunks(te, be)), len(chunks(td, bd)))
        pages += list(zip(((te, b) for b in split_to(te, be, count)), ((td, b) for b in split_to(td, bd, count))))
    if len(pages) > MAX_PAGES:
        raise SystemExit(f"{cfg['module']}: {len(pages)} pages, Vanilla books hold {MAX_PAGES}")
    return pages


# --------------------------------------------------------------------------------------------
# Files
# --------------------------------------------------------------------------------------------

def hid(ns: str, kind: str, key: str) -> str:
    digest = hashlib.sha256(f"{ns}/{kind}/{key}".encode()).hexdigest()
    value = int(digest[:16], 16) & 0x7FFFFFFFFFFFFFFF
    return f"{max(value, 2):016X}"


def to_json(obj, indent="\t") -> str:
    return json.dumps(obj, indent=indent, ensure_ascii=False) + "\n"


def translate(key: str) -> str:
    return json.dumps({"translate": key}, ensure_ascii=False)


def module_paths(cfg: dict) -> dict:
    root = REPO / "modules" / cfg["module"]
    res = root / "shared/resources"
    return dict(root=root, res=res, java=root / "shared/java" / cfg["package"].replace(".", "/") / f"{cfg['cls']}.java",
                lang=res / "assets" / cfg["ns"] / "lang", data=res / "data" / cfg["ns"], assets=res / "assets" / cfg["ns"])


def lang_entries(cfg: dict, pages) -> tuple[dict, dict]:
    ns = cfg["ns"]
    en, de = {}, {}
    if cfg["kind"] == "item":
        en[f"item.{ns}.guide_book"], de[f"item.{ns}.guide_book"] = cfg["name"]
    for i, ((te, be), (td, bd)) in enumerate(pages, start=1):
        key = f"{ns}.guide.page.{i}"
        en[key + ".title"], de[key + ".title"] = te, td
        en[key], de[key] = be, bd
    if cfg["kind"] == "item":
        q = f"quests.{ns}.start"
        thing = RECIPE_ITEM_NAMES[cfg["recipe"]]
        en[q + ".title"], de[q + ".title"] = QUEST["title"][0] % cfg["author"], QUEST["title"][1] % cfg["author"]
        en[q + ".subtitle"], de[q + ".subtitle"] = QUEST["subtitle"]
        en[q + ".welcome.title"], de[q + ".welcome.title"] = QUEST["welcome_title"]
        en[q + ".welcome.description"] = QUEST["welcome_desc"][0] % (cfg["name"][0], thing[0])
        de[q + ".welcome.description"] = QUEST["welcome_desc"][1] % (cfg["name"][1], thing[1])
    return en, de


def updated_lang(path: Path, cfg: dict, entries: dict) -> str:
    ns = cfg["ns"]
    owned = (f"item.{ns}.guide_book", f"{ns}.guide.page.", f"quests.{ns}.start.")
    data = json.loads(path.read_text(encoding="utf-8"))
    items = {k: v for k, v in data.items() if not (k == owned[0] or k.startswith(owned[1:]))}
    items.update(entries)
    return json.dumps(items, indent=2, ensure_ascii=False) + "\n"


def java_source(cfg: dict, pages) -> str:
    template = (HERE / ("ModuleGuide.java.in" if cfg["kind"] == "item" else "ClientGuide.java.in")).read_text(encoding="utf-8")
    values = dict(PACKAGE=cfg["package"], CLASS=cfg["cls"], NAMESPACE=cfg["ns"], AUTHOR=cfg["author"], PAGES=str(len(pages)),
                  RECIPE_ITEM=cfg.get("recipe", "").removeprefix("minecraft:").upper())
    for key, value in values.items():
        template = template.replace("@" + key + "@", value)
    return template


def item_files(cfg: dict) -> dict[str, str]:
    ns, p = cfg["ns"], module_paths(cfg)
    out = {}
    out[p["data"] / "recipe/guide_book.json"] = to_json({
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ["minecraft:book", cfg["recipe"]],
        "result": {"count": 1, "id": f"{ns}:guide_book"},
    }, indent=2)
    out[p["data"] / "advancement/recipes/misc/guide_book.json"] = to_json({
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_book": {"conditions": {"items": [{"items": "minecraft:book"}]}, "trigger": "minecraft:inventory_changed"},
            "has_item": {"conditions": {"items": [{"items": cfg["recipe"]}]}, "trigger": "minecraft:inventory_changed"},
            "has_the_recipe": {"conditions": {"recipes": f"{ns}:guide_book"}, "trigger": "minecraft:recipe_unlocked"},
        },
        "requirements": [["has_the_recipe", "has_book", "has_item"]],
        "rewards": {"recipes": [f"{ns}:guide_book"]},
    }, indent=2)
    out[p["assets"] / "items/guide_book.json"] = to_json(
        {"model": {"type": "minecraft:model", "model": f"{ns}:item/guide_book"}}, indent=2)
    out[p["assets"] / "models/item/guide_book.json"] = to_json(
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/guide_book"}}, indent=2)
    # FTB Quests: one chapter, one quest (crafting table, like SimpleBuilding's first quest), reward = the guide.
    qdir = p["data"] / "ftbquests"
    filename = f"{ns}_start"
    chapter_id, quest_id = hid(ns, "chapter", "start"), hid(ns, "quest", "start.welcome")
    out[qdir / f"chapters/{filename}.json5"] = to_json({
        "id": chapter_id,
        "order_index": 0,
        "filename": filename,
        "icon": {"id": f"{ns}:guide_book"},
        "default_quest_shape": "",
        "default_hide_dependency_lines": False,
        "progression_mode": "flexible",
        "quests": [{
            "id": quest_id, "x": 0.0, "y": 0.0, "dependencies": [],
            "tasks": [{"id": hid(ns, "task", "start.welcome"), "type": "item",
                       "item": {"id": "minecraft:crafting_table", "count": 1}}],
            "rewards": [{"id": hid(ns, "reward", "start.welcome"), "type": "item",
                         "item": {"id": f"{ns}:guide_book", "count": 1}}],
        }],
        "quest_links": [],
        "images": [],
    })
    q = f"quests.{ns}.start"
    out[qdir / f"lang/en_us/chapters/{filename}.json5"] = to_json({
        f"chapter.{chapter_id}.title": translate(q + ".title"),
        f"chapter.{chapter_id}.chapter_subtitle": [translate(q + ".subtitle")],
        f"quest.{quest_id}.title": translate(q + ".welcome.title"),
        f"quest.{quest_id}.quest_desc": [translate(q + ".welcome.description")],
    })
    out[qdir / "install.txt"] = "\n".join([
        f"# {cfg['author']} default quests for FTB Quests - generated by tools/guides/module_guides.py, do not edit.",
        "# Read by com.simplebuilding.framework.api.ModuleQuestDefaults; paths are relative to config/ftbquests/quests.",
        "format json5", f"version {DATA_VERSION}",
        f"file chapters/{filename}.json5", f"file lang/en_us/chapters/{filename}.json5"]) + "\n"
    return out


def outputs() -> dict[Path, str]:
    out: dict[Path, str] = {}
    for cfg in MODULES:
        pages = build_pages(cfg)
        p = module_paths(cfg)
        out[p["java"]] = java_source(cfg, pages)
        en, de = lang_entries(cfg, pages)
        out[p["lang"] / "en_us.json"] = updated_lang(p["lang"] / "en_us.json", cfg, en)
        out[p["lang"] / "de_de.json"] = updated_lang(p["lang"] / "de_de.json", cfg, de)
        if cfg["kind"] == "item":
            out.update(item_files(cfg))
    return out


def main(argv: list[str]) -> int:
    check = "--check" in argv
    stale = []
    for path, text in outputs().items():
        old = path.read_text(encoding="utf-8").replace("\r\n", "\n") if path.exists() else None
        if old == text:
            continue
        if check:
            stale.append(str(path.relative_to(REPO)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            crlf = path.exists() and b"\r\n" in path.read_bytes()
            path.write_bytes(text.replace("\n", "\r\n").encode() if crlf else text.encode())
            print("wrote", path.relative_to(REPO))
    if stale:
        print("Module guides are stale - run: python tools/guides/module_guides.py")
        for s in stale:
            print("  " + s)
        return 1
    if check:
        print("module guides: up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
