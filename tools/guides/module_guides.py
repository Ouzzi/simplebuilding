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
         intro=("Bounded playful mechanics, throwable bricks and cosmetic transformations. The animal heads moved to "
                "Simple Mobheads.",
                "Begrenzte verspielte Mechaniken, Wurfziegel und kosmetische Verwandlungen. Die Tierköpfe liegen jetzt in "
                "Simple Mobheads.")),
    dict(module="simplemobheads", ns="simplemobheads", package="com.simplemobheads.guide", cls="MobHeadsGuide",
         kind="item", author="Simple Mobheads", recipe="minecraft:skeleton_skull",
         name=("Simple Mobheads Guide", "Simple-Mobheads-Handbuch"),
         intro=("A head for every mob: a charged creeper's explosion drops it, worn it keeps that mob calm and hides a "
                "small secret, placed it decorates, on a note block it speaks.",
                "Ein Kopf für jeden Mob: die Explosion eines geladenen Creepers lässt ihn fallen, getragen beruhigt er "
                "seinen Mob und verbirgt ein kleines Geheimnis, aufgestellt schmückt er, auf dem Notenblock spricht er.")),
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
         submods=("simpleweather","simpleloot","simplemobmanners","simplefarming","simpletools","simplemovement","simpleinterfaces"),
         name=("Simple Quality of Life Guide", "Simple-Quality-of-Life-Handbuch"),
         intro=("Bundles seven sub-mods with one switch each: weather, interfaces, loot, mob manners, farming, tools and movement.",
                "Bündelt sieben Sub-Mods mit je einem Schalter: Wetter, Oberflächen, Beute, Mob-Manieren, Landwirtschaft, Werkzeuge und Bewegung.")),
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
         intro=("Legacy compatibility and server-authoritative claims, on by default.",
                "Kompatibilität mit alten Ständen und serverseitige Claims, standardmäßig an.")),
    dict(module="simplerespawn", ns="simplerespawn", package="com.simplebuilding.modules.simplerespawn.guide",
         cls="RespawnGuide", kind="item", author="Simple Respawn", recipe="minecraft:glistering_melon_slice",
         name=("Simple Respawn Guide", "Simple-Respawn-Handbuch"),
         intro=("Downed instead of dead: teammates revive you before you bleed out.",
                "Niedergeschlagen statt tot: Mitspieler beleben dich wieder, bevor du verblutest.")),
    dict(module="simplebeacons", ns="simplebeacons", package="com.simplebeacons.guide", cls="BeaconsGuide", kind="item",
         author="Simple Beacons", recipe="minecraft:prismarine_shard",
         name=("Simple Beacons Guide", "Simple-Beacons-Handbuch"),
         intro=("The Strong Upgrade turns a beacon into a Strong Beacon with six pyramid levels and six more powers, and a "
                "conduit into a Strong Conduit with a 7x7x7 frame.",
                "Die Starke Aufwertungsvorlage macht aus einem Leuchtfeuer ein Starkes Leuchtfeuer mit sechs Pyramidenebenen "
                "und sechs weiteren Kräften und aus einem Aquisator einen Starken Aquisator mit 7x7x7-Rahmen.")),
    dict(module="simpleshops", ns="simpleshops", package="com.simpleshops.guide", cls="ShopsGuide", kind="item",
         author="Simple Shops", recipe="minecraft:emerald",
         name=("Simple Shops Guide", "Simple-Shops-Handbuch"),
         intro=("Player shops: a Sales Block with a price and a Display Case with up to five goods, protected from strangers and "
                "explosions. Buyers swap price items for one piece per trade.",
                "Spieler-Shops: ein Verkaufsblock mit Preis und eine Vitrine mit bis zu fünf Waren, geschützt vor Fremden und "
                "Explosionen. Käufer tauschen Preis-Items gegen ein Stück je Tausch.")),
    dict(module="simplewanderers", ns="simplewanderers", package="com.simplewanderers.guide", cls="WanderersGuide", kind="item",
         author="Simple Wanderers", recipe="minecraft:lead",
         name=("Simple Wanderers Guide", "Simple-Wanderers-Handbuch"),
         intro=("One Village Wanderer per village: a travel-cloaked trader with a leashed horse, a new stock every 3 days, "
                "a restock every 1.5 days and goods that follow the village biome. Kill him and a new one comes after 7 days.",
                "Ein Dorf-Wanderer je Dorf: ein Händler im Reisemantel mit Pferd an der Leine, alle 3 Tage neues Angebot, "
                "alle 1,5 Tage Nachschub und Waren passend zum Dorfbiom. Wird er getötet, kommt nach 7 Tagen ein neuer.")),
    dict(module="simpleblocks", ns="simpleblocks", package="com.simpleblocks.guide", cls="BlocksGuide", kind="item",
         author="Simple Blocks", recipe="minecraft:clay_ball",
         name=("Simple Blocks Guide", "Simple-Blocks-Handbuch"),
         intro=("The missing building blocks: Hushed Deepslate Tiles that swallow vibrations like wool, Sculk Deepslate, "
                "the missing Vanilla stairs, slabs and walls, cracked and mossy bricks and the Shaping Saw, which cuts stone and wood.",
                "Die fehlenden Baublöcke: Gedämpfte Tiefenschieferfliesen, die Vibrationen wie Wolle schlucken, "
                "Sculk-Tiefenschiefer, die fehlenden Vanilla-Treppen, -Stufen und -Mauern, rissige und bemooste Ziegel "
                "und die Formsäge, die Stein und Holz schneidet.")),
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
    dict(module="simpleend", ns="simpleend", package="com.simplebuilding.modules.simpleend.guide", cls="EndGuide",
         kind="item", author="Simple End", recipe="minecraft:chorus_fruit",
         name=("Simple End Guide", "Simple-End-Handbuch"),
         intro=("Ashwood and Duskwood, buds that grow End trees or stick scrub, End grass and the Ash Valley and "
                "Dawnlands biomes on the outer End islands.",
                "Aschholz und Dämmerholz, Knospen für End-Bäume oder Stöcke-Gestrüpp, End-Gras und die Biome "
                "Aschental und Dämmerlande auf den äußeren End-Inseln.")),
    dict(module="simpleovens", ns="simpleovens", package="com.simpleovens.guide", cls="OvensGuide", kind="item",
         author="Simple Ovens", recipe="minecraft:charcoal",
         name=("Simple Ovens Guide", "Simple-Ovens-Handbuch"),
         intro=("Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces that cook 2, 4 and 8 times as fast, "
                "and furnace minecarts of the same tiers.",
                "Verstärkte, Netherit- und Enderit-Öfen, -Räucheröfen und -Schmelzöfen, die 2-, 4- und 8-mal so schnell "
                "garen, und Antriebsloren derselben Stufen.")),
    dict(module="simplecrucibles", ns="simplecrucibles", package="com.simplecrucibles.guide", cls="CruciblesGuide", kind="item",
         author="Simple Crucibles", recipe="minecraft:cauldron",
         name=("Simple Crucibles Guide", "Simple-Crucibles-Handbuch"),
         intro=("Iron, Reinforced, Netherite and Enderite crucibles that cook every slot at once without fuel, barrels that dock "
                "onto them, the reinforced cauldron and village field kitchens.",
                "Eisen-, verstärkte, Netherit- und Enderit-Schmelztiegel, die ohne Brennstoff alle Plätze zugleich garen, Fässer, "
                "die an sie andocken, der verstärkte Kessel und Dorf-Feldküchen.")),
    dict(module="simpletrims", ns="simpletrims", package="com.simpletrims.guide", cls="TrimsGuide", kind="item",
         author="Simple Trims", recipe="minecraft:glow_ink_sac",
         name=("Simple Trims Guide", "Simple-Trims-Handbuch"),
         intro=("Glowing, Emitting and Pulsating armor trims, placeable trim templates and forging them with an axe "
                "(with SimpleBuilding: the sledgehammer).",
                "Leuchtender, strahlender und pulsierender Rüstungsbesatz, ablegbare Besatzvorlagen und ihr Schmieden "
                "mit der Axt (mit SimpleBuilding: dem Vorschlaghammer).")),
    dict(module="simplemobs", ns="simplemobs", package="com.simplebuilding.modules.simplemobs.guide", cls="MobsGuide",
         kind="item", author="Simple Mobs", recipe="minecraft:bone",
         name=("Simple Mobs Guide", "Simple-Mobs-Handbuch"),
         intro=("New bosses and mobs with fair, announced attacks, their drops, the Blighted Wither and the Biome Brush.",
                "Neue Bosse und Mobs mit fairen, angekündigten Angriffen, ihre Beute, der Verdorrte Wither und der "
                "Biom-Pinsel.")),
    dict(module="simplestats", ns="simplestats", package="com.simplebuilding.modules.simplestats.guide", cls="StatsGuide",
         kind="item", author="Simple Stats", recipe="minecraft:flint",
         name=("Simple Stats Guide", "Simple-Stats-Handbuch"),
         intro=("Every tool, weapon and piece of armor counts its own blocks, kills, absorbed damage, distances and catches, "
                "with an item-specific statistics screen.",
                "Jedes Werkzeug, jede Waffe und jedes Rüstungsteil zählt eigene Blöcke, Kills, abgefangenen Schaden, Strecken "
                "und Fänge, mit einem Statistik-Bildschirm je Gegenstand.")),
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
    "minecraft:lead": ("a lead", "einer Leine"),
    "minecraft:brick": ("a brick", "einem Ziegel"),
    "minecraft:clay_ball": ("a clay ball", "einem Tonklumpen"),
    "minecraft:gold_nugget": ("a gold nugget", "einem Goldklumpen"),
    "minecraft:bread": ("bread", "Brot"),
    "minecraft:chest": ("a chest", "einer Truhe"),
    "minecraft:item_frame": ("an item frame", "einem Rahmen"),
    "minecraft:flint_and_steel": ("flint and steel", "einem Feuerzeug"),
    "minecraft:paper": ("paper", "Papier"),
    "minecraft:chorus_fruit": ("a chorus fruit", "einer Chorusfrucht"),
    "minecraft:glistering_melon_slice": ("a glistering melon slice", "einer glitzernden Melonenscheibe"),
    "minecraft:bone": ("a bone", "einem Knochen"),
    "minecraft:skeleton_skull": ("a skeleton skull", "einem Skelettschädel"),
    "minecraft:glow_ink_sac": ("a glow ink sac", "einem Leuchttintenbeutel"),
    "minecraft:prismarine_shard": ("a prismarine shard", "einer Prismarinscherbe"),
    "minecraft:charcoal": ("charcoal", "Holzkohle"),
    "minecraft:cauldron": ("a cauldron", "einem Kessel"),
    "minecraft:emerald": ("an emerald", "einem Smaragd"),
    "minecraft:flint": ("a piece of flint", "einem Feuerstein"),
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
    features = list(manual["features"])
    # A super-mod's guide also covers its bundled sub-mods (their own wiki pages, without the sub-mod note).
    for sub in cfg.get("submods", ()):
        sub_manual = json.loads((REPO / "modules" / sub / "wiki/manual.json").read_text(encoding="utf-8"))
        features += [f for f in sub_manual["features"] if f["id"] != "submod"]
    entries = [((cfg["author"], cfg["intro"][0]), (cfg["author"], cfg["intro"][1]))]
    has_config = False
    for feature in features:
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


NOTE = ("Guide to this mod: %d pages taken from this wiki, shown in your language. Shapeless recipe: book + %s. "
        "Use it to read. With FTB Quests installed, the first quest of the chapter \"Welcome to %s\" gives one for free.",
        "Handbuch zu dieser Mod: %d Seiten aus diesem Wiki, in deiner Sprache. Formloses Rezept: Buch + %s. "
        "Benutzen zum Lesen. Mit FTB Quests schenkt die erste Quest im Kapitel \"Willkommen bei %s\" eins.")
RECIPE_ITEM_SHORT = {  # (English, German) item names for the wiki note
    "minecraft:clay_ball": ("clay ball", "Tonklumpen"),
    "minecraft:emerald": ("emerald", "Smaragd"),
    "minecraft:lead": ("lead", "Leine"),
    "minecraft:hay_block": ("hay bale", "Heuballen"), "minecraft:brick": ("brick", "Ziegel"),
    "minecraft:gold_nugget": ("gold nugget", "Goldklumpen"), "minecraft:bread": ("bread", "Brot"),
    "minecraft:chest": ("chest", "Truhe"), "minecraft:item_frame": ("item frame", "Rahmen"),
    "minecraft:flint_and_steel": ("flint and steel", "Feuerzeug"), "minecraft:paper": ("paper", "Papier"),
    "minecraft:chorus_fruit": ("chorus fruit", "Chorusfrucht"),
    "minecraft:glistering_melon_slice": ("glistering melon slice", "glitzernde Melonenscheibe"),
    "minecraft:bone": ("bone", "Knochen"),
    "minecraft:skeleton_skull": ("skeleton skull", "Skelettschädel"),
    "minecraft:glow_ink_sac": ("glow ink sac", "Leuchttintenbeutel"),
    "minecraft:prismarine_shard": ("prismarine shard", "Prismarinscherbe"),
    "minecraft:charcoal": ("charcoal", "Holzkohle"),
    "minecraft:cauldron": ("cauldron", "Kessel"),
    "minecraft:flint": ("flint", "Feuerstein"),
}


def updated_manual(cfg: dict, pages) -> str:
    """The guide's wiki note in modules/<id>/wiki/manual.json (owned key <ns>:guide_book), keeping the file's style."""
    nl = "\n"
    path = REPO / "modules" / cfg["module"] / "wiki/manual.json"
    raw = path.read_text(encoding="utf-8").replace("\r" + nl, nl)
    data = json.loads(raw)
    key = f"{cfg['ns']}:guide_book"
    names = RECIPE_ITEM_SHORT[cfg["recipe"]]
    rel = module_paths(cfg)["java"].relative_to(REPO).as_posix()
    note = {"sources": [rel, f"modules/{cfg['module']}/shared/resources/data/{cfg['ns']}/recipe/guide_book.json"],
            "en": {"summary": NOTE[0] % (len(pages), names[0], cfg["author"])},
            "de": {"summary": NOTE[1] % (len(pages), names[1], cfg["author"])}}
    if json.dumps(data, indent=2, ensure_ascii=False) + nl == raw:
        notes = {k: v for k, v in data["notes"].items() if k != key}
        data["notes"] = {key: note, **notes}
        return json.dumps(data, indent=2, ensure_ascii=False) + nl
    # Hand-formatted file (compact note objects): replace or insert the entry as text.
    dump = lambda value: json.dumps(value, ensure_ascii=False)  # noqa: E731
    entry = (f'    "{key}": {{{nl}      "sources": {dump(note["sources"])},{nl}'
             f'      "en": {dump(note["en"])},{nl}      "de": {dump(note["de"])}{nl}    }},{nl}')
    start = raw.find(f'    "{key}": {{{nl}')
    if start >= 0:
        close = f"{nl}    }},{nl}"
        raw = raw[:start] + raw[raw.index(close, start) + len(close):]
    head = f'  "notes": {{{nl}'
    anchor = raw.index(head) + len(head)
    out = raw[:anchor] + entry + raw[anchor:]
    json.loads(out)
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
            out[REPO / "modules" / cfg["module"] / "wiki/manual.json"] = updated_manual(cfg, pages)
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
