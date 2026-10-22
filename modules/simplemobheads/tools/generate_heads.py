#!/usr/bin/env python3
"""Simple Mobheads generator (KONZEPT-SIMPLE-MOBHEADS-2026-10-13 §3.6): data/mobs.json is the only source of truth.

Writes
  shared/java/com/simplemobheads/data/HeadTable.java       registration + render table (Java, no client class)
  generated/resources/assets/simplemobheads/blockstates/*  standing and wall head (particles of block/skull)
  generated/resources/assets/simplemobheads/items/*        minecraft:head special model, variants via select
  generated/resources/data/simplemobheads/loot_table/...   block loot, copies name/sound/variant like Vanilla skulls
  generated/resources/data/simplemobheads/advancement/...  one hidden collection advancement per head
  generated/resources/data/*/tags/...                      skulls, mineable/axe, trial chamber heads, freeze immune
  shared/resources/assets/simplemobheads/lang/{en_us,de_de}.json   generated keys merged into the hand-written file
  shared/resources/assets/simplemobheads/questionary/{en_us,de_de}.json
  ../simplemoney/.../money/simplemobheads/prices.json      Simple Money price rows

Mojang pixels are never written: every head references the Vanilla (or mod) texture at runtime.

Usage: python modules/simplemobheads/tools/generate_heads.py [--check]
"""
from __future__ import annotations

import json
import sys
import textwrap
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = "simplemobheads"
DATA = json.loads((MODULE / "data/mobs.json").read_text(encoding="utf-8"))
HEADS = DATA["heads"]
GEN = MODULE / "generated/resources"
SHARED = MODULE / "shared/resources"
JAVA = MODULE / "shared/java/com/simplemobheads/data/HeadTable.java"
PRICES = ROOT / "modules/simplemoney/shared/resources/data/simplemoney/money/simplemobheads/prices.json"

# What each ability does (questionary secret, wiki). Keys = mobs.json "ability".
ABILITIES = {
    "blaze": ("Magma blocks and campfires do not burn its wearer.", "Magmablöcke und Lagerfeuer verbrennen den Träger nicht."),
    "enderman": ("Ender pearl teleports cost its wearer no health.", "Enderperlen-Teleports kosten den Träger keine Herzen."),
    "husk": ("Food never gives its wearer Hunger.", "Essen macht den Träger nie hungrig (kein Hunger-Effekt)."),
    "spider": ("Cobwebs do not slow its wearer down.", "Spinnennetze bremsen den Träger nicht."),
    "cave_spider": ("Bare hands cut cobwebs as fast as a sword.", "Die bloße Hand schneidet Spinnennetze so schnell wie ein Schwert."),
    "stray": ("Powder snow does not freeze its wearer.", "Pulverschnee lässt den Träger nicht erfrieren."),
    "bogged": ("Food never poisons its wearer.", "Essen vergiftet den Träger nie."),
    "slime": ("Its wearer falls one block further without damage.", "Der Träger fällt einen Block tiefer ohne Schaden."),
    "silverfish": ("Its wearer shrinks to half size.", "Der Träger schrumpft auf die halbe Größe."),
    "breeze": ("Its wearer, light as the wind, never tramples farmland.", "Leicht wie der Wind: der Träger zertrampelt kein Ackerland."),
    "shulker": ("Its wearer opens shulker boxes even when the lid is blocked.", "Der Träger öffnet Shulkerkisten auch mit versperrtem Deckel."),
    "drowned": ("Magma bubble columns do not drag its wearer down.", "Magma-Blasensäulen ziehen den Träger nicht nach unten."),
    "cow": ("Another player can milk its wearer with a bucket.", "Ein anderer Spieler kann den Träger mit einem Eimer melken."),
    "sheep": ("Another player can shear its wearer (1-3 wool, then it grows back).", "Ein anderer Spieler kann den Träger scheren (1-3 Wolle, dann wächst sie nach)."),
    "chicken": ("Its wearer lays an egg now and then, like a chicken.", "Der Träger legt ab und zu ein Ei, wie ein Huhn."),
    "pig": ("Sneak three seconds on grass, dirt or moss: the truffle nose digs something up.", "Drei Sekunden Schleichen auf Gras, Erde oder Moos: die Trüffelnase scharrt etwas aus."),
    "bee": ("Harvesting a hive or nest does not anger the bees at its wearer.", "Beim Ernten von Stock oder Nest werden die Bienen nicht wütend auf den Träger."),
    "fox": ("Sweet berry bushes do not prick its wearer.", "Süßbeerensträucher stechen den Träger nicht."),
    "frog": ("Its wearer jumps higher from lily pads and out of water.", "Der Träger springt von Seerosen und aus dem Wasser höher."),
    "goat": ("A ramming goat only pushes its wearer, without damage.", "Eine rammende Ziege schubst den Träger nur, ohne Schaden."),
    "cat": ("Creepers keep their distance from its wearer, as from a cat.", "Creeper halten Abstand zum Träger, wie zu einer Katze."),
    "mooshroom": ("Another player can fill a bowl with mushroom stew from its wearer.", "Ein anderer Spieler füllt beim Träger eine Schüssel mit Pilzsuppe."),
    "sniffer": ("Sneak five seconds on moss, grass or dirt: it sniffs up seeds.", "Fünf Sekunden Schleichen auf Moos, Gras oder Erde: er erschnüffelt Samen."),
    "allay": ("Dropped items near its wearer float up a little.", "Liegende Items in der Nähe des Trägers schweben leicht."),
    "turtle": ("Its wearer never tramples turtle eggs.", "Der Träger zertrampelt keine Schildkröteneier."),
    "polar_bear": ("Ice is less slippery for its wearer.", "Eis ist für den Träger weniger rutschig."),
    "wolf": ("Wild wolves never attack its wearer.", "Wilde Wölfe greifen den Träger nie an."),
    "warden": ("Half of its wearer's footsteps never reach sculk.", "Die Hälfte der Schritte des Trägers erreicht kein Sculk."),
    "herobrine": ("Torches near its wearer flicker; in the dark, hostile mobs hesitate before they attack.",
                  "Fackeln beim Träger flackern; im Dunkeln zögern feindliche Mobs vor dem Angriff."),
    # Easter eggs EH21 (docs/ai/PLAN-B-EH21-2026-10-22.md).
    "crowned_pig": ("Pigs nearby turn their heads to its wearer, and the crown glints.",
                    "Schweine in der Nähe drehen den Kopf zum Träger, und die Krone glänzt."),
    "giant": ("Its wearer grows to 1.5x size, steps up whole blocks and walks a little faster; zombies see it only half as far, "
              "and it works like the head of its zombie kind (husk, drowned).",
              "Der Träger wächst auf 1,5-fache Größe, steigt ganze Blöcke hoch und läuft etwas schneller; Zombies sehen ihn nur halb "
              "so weit, und er wirkt wie der Kopf seiner Zombie-Art (Wüstenzombie, Ertrunkener)."),
    "giant_cow": ("Its wearer grows to 1.5x size and steps up whole blocks; another player can milk it like a cow.",
                  "Der Träger wächst auf 1,5-fache Größe und steigt ganze Blöcke hoch; ein anderer Spieler kann ihn wie eine Kuh melken."),
}

TRADE_EXCLUDED = {"wither": "Boss head: only the charged creeper (server switch, default off).",
                  "warden": "Boss head: only the charged creeper.",
                  "blighted_wither": "Boss head of Simple Mobs.", "abyssal_wither": "Boss head of Simple Mobs.",
                  "herobrine": "Secret head: only the Herobrine ritual (KONZEPT-HEROBRINE-KOPF-2026-10-19), never sold.",
                  "crowned_pig": "Secret head: only the pig named Technoblade (EH21 TO-1), never sold.",
                  "giant": "Secret head: only a giant killed by a charged creeper (EH21 ZO-1), never sold.",
                  "giant_cow": "Secret head: only a giant cow killed by a charged creeper (EH21 CO-2), never sold."}
# Price rows that moved in from SimpleBuilding / Simple Fun keep their values.
LEGACY_TRADE = {"bogged": ("mason", 0, 0.5, 1, 2, 1.0), "stray": ("mason", 0, 0.5, 1, 2, 1.0)}


def item_id(h):
    return h.get("item", h["id"] + "_head")


def wall_id(h):
    item = item_id(h)
    return item[:-len("_skull")] + "_wall_skull" if item.endswith("_skull") else item[:-len("_head")] + "_wall_head"


def mob_id(h):
    return h.get("mob", "minecraft:" + h["id"])


def tex(t):
    """mobs.json texture short form -> identifier of the texture file."""
    ns, path = t.split(":", 1) if ":" in t else ("minecraft", t)
    return f"{ns}:textures/entity/{path}.png"


def short(t):
    """Texture short form for the item model's head 'texture' field (namespace:path under textures/entity)."""
    return t if ":" in t else "minecraft:" + t


def looks(h):
    """(variant id or '', variant dict or None) for the base look and every variant."""
    variants = h.get("variants")
    if not variants:
        return [("", None)]
    out = [("", variants["list"][0])]
    out += [(v["id"], v) for v in variants["list"][1:]]
    return out


def kind(h, variant):
    return f"{NS}:{h['id']}" + (f"/{variant}" if variant else "")


def resolve(h, v):
    """The fully resolved render spec of one look: custom geometry or layer+part, texture, tints, passes.

    v is the variant of the look (the base look carries the first variant) or None for heads without variants.
    'variant' in a texture/tint means: from the variant (tex, tint); 'variant2' the second tint, 'variant_eyes' its eyes.
    """
    model = dict(v["model"]) if v and "model" in v else dict(h["model"])
    variant_layer = h.get("variant_layer", False)

    def pick(value, key):
        if value == "variant":
            return v.get(key, "") if v else ""
        if value == "variant2":
            return v.get(key + "2", "") if v else ""
        if value == "variant_eyes":
            return v.get("eyes", "") if v else ""
        return value

    base_tex = model["tex"]
    if v and not variant_layer and "tex" in v:
        base_tex = v["tex"]
    passes = []
    for p in model.get("layers", []):
        passes.append({"layer": p.get("layer", model.get("layer", "")), "part": p.get("part", model.get("part", "")),
                       "tex": tex(pick(p["tex"], "tex")), "render": p.get("render", "cutout"), "tint": pick(p.get("tint", ""), "tint")})
    return {"custom": model.get("custom", ""), "layer": model.get("layer", ""), "part": model.get("part", ""),
            "tex": tex(base_tex), "short": short(base_tex), "render": model.get("render", "cutout"),
            "tint": pick(model.get("tint", ""), "tint"), "hide": model.get("hide", []), "rot": model.get("rot"),
            "scale": model.get("scale", 0), "passes": passes}


def jstr(s):
    return json.dumps(s, ensure_ascii=False)


def java():
    lines = ["package com.simplemobheads.data;", "",
             "import java.util.List;", "",
             "/**",
             " * GENERATED by modules/simplemobheads/tools/generate_heads.py from data/mobs.json - do not edit by hand.",
             " * Every head of Simple Mobheads: ids, mob, note block sound, rules and the resolved look of each variant.",
             " */",
             "public final class HeadTable {",
             "    private HeadTable() {",
             "    }", "",
             "    public static final List<HeadDef> HEADS = List.of("]
    entries = []
    for h in HEADS:
        vs = h.get("variants") or {}
        look_lines = []
        for vid, v in looks(h):
            r = resolve(h, v)
            passes = ", ".join(f"new HeadDef.Pass({jstr(p['layer'])}, {jstr(p['part'])}, {jstr(p['tex'])}, {jstr(p['render'])}, {jstr(p['tint'])})"
                               for p in r["passes"])
            rot = "null" if r["rot"] is None else "new float[] {" + ", ".join(f"{float(x)}F" for x in r["rot"]) + "}"
            hide = ", ".join(jstr(x) for x in r["hide"])
            look_lines.append(
                f"                new HeadDef.Look({jstr(vid)}, {jstr(kind(h, vid))}, {jstr(r['custom'])}, {jstr(r['layer'])}, {jstr(r['part'])}, "
                f"{jstr(r['tex'])}, {jstr(r['render'])}, {jstr(r['tint'])}, List.of({hide}), {rot}, {float(r['scale'])}F, List.of({passes}))")
        entries.append(
            f"        new HeadDef({jstr(h['id'])}, {jstr(item_id(h))}, {jstr(wall_id(h))}, {jstr(mob_id(h))}, {jstr(h['group'])}, "
            f"List.of({', '.join(jstr(s) for s in h['sound'])}), {jstr(h.get('from', ''))}, {jstr(h.get('ability', ''))}, "
            f"{str(h.get('trial', False)).lower()}, {str(h.get('freeze_immune', False)).lower()}, {jstr(h.get('drop', 'default'))}, "
            f"{jstr(vs.get('component') or '')}, {jstr(h.get('variant_source', ''))}, {jstr(vs['list'][0]['id'] if vs else '')}, List.of(\n" + ",\n".join(look_lines) + "), "
            f"{jstr(h.get('counts_as', ''))}, List.of({', '.join(jstr(m) for m in h.get('camouflage', []))}))")
    lines.append(",\n".join(entries) + ");")
    lines.append("")
    excl = ", ".join(f"java.util.Map.entry({jstr(k)}, {jstr(v)})" for k, v in DATA["exclude"].items())
    lines.append(f"    /** Living mobs without a Mobheads head and why (Vanilla heads, unused mobs); the completeness test reads it. */")
    lines.append(f"    public static final java.util.Map<String, String> EXCLUDED = java.util.Map.ofEntries({excl});")
    lines.append("}")
    return "\n".join(lines) + "\n"


def skull_transform_model(kind_name, texture=None, tall=False, scale=1.0):
    """scale: the look's own model scale; a giant head (scale 3) is drawn at 1/3 so it fits a slot like any head."""
    head = {"type": "minecraft:head", "kind": kind_name}
    if texture:
        head["texture"] = texture
    s = round(1.0 / scale, 4) if scale > 1 else 1.0
    return {"type": "minecraft:special", "base": f"{NS}:item/template_tall_head" if tall else "minecraft:item/template_skull",
            "model": head,
            "transformation": {"left_rotation": [1.0, 0.0, 0.0, -0.0], "right_rotation": [0.0, 0.0, 0.0, 1.0],
                               "scale": [s, s, s], "translation": [0.5, 0.0, 0.5]}}


def resources():
    out = {}
    skull = {"variants": {"": {"model": "minecraft:block/skull"}}}
    for h in HEADS:
        item, wall = item_id(h), wall_id(h)
        out[GEN / f"assets/{NS}/blockstates/{item}.json"] = skull
        out[GEN / f"assets/{NS}/blockstates/{wall}.json"] = skull
        tall = h.get("tall", False)
        lk = looks(h)
        size = {vid: resolve(h, v)["scale"] for vid, v in lk}
        if len(lk) == 1:
            model = skull_transform_model(kind(h, ""), tall=tall, scale=size[""])
        else:
            model = {"type": "minecraft:select", "property": "minecraft:component", "component": f"{NS}:variant",
                     "cases": [{"when": vid, "model": skull_transform_model(kind(h, vid), tall=tall, scale=size[vid])} for vid, _ in lk[1:]]
                              + [{"when": lk[0][1]["id"], "model": skull_transform_model(kind(h, ""), tall=tall, scale=size[""])}],
                     "fallback": skull_transform_model(kind(h, ""), tall=tall, scale=size[""])}
        out[GEN / f"assets/{NS}/items/{item}.json"] = {"model": model}
        out[GEN / f"data/{NS}/loot_table/blocks/{item}.json"] = {
            "type": "minecraft:block",
            "pools": [{"entries": [{"type": "minecraft:item",
                                    "modifier": {"type": "minecraft:copy_components",
                                                 "include": ["minecraft:custom_name", "minecraft:note_block_sound", f"{NS}:variant"],
                                                 "source": "block_entity"},
                                    "name": f"{NS}:{item}"}],
                       "rolls": 1}],
            "random_sequence": f"{NS}:blocks/{item}"}
        out[GEN / f"data/{NS}/advancement/heads/{h['id']}.json"] = {
            "parent": f"{NS}:heads/root",
            "criteria": {"has_head": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"{NS}:{item}"}]}}},
            "requirements": [["has_head"]],
            # A head with "advancement": true (the secret Herobrine head) has its own hand-written title and description.
            "display": {"icon": {"id": f"{NS}:{item}"},
                        "title": {"translate": f"advancements.{NS}.heads.{h['id']}.title" if h.get("advancement") else f"block.{NS}.{item}"},
                        "description": {"translate": f"advancements.{NS}.heads.{h['id']}.description" if h.get("advancement")
                                        else f"advancements.{NS}.heads.head.description"},
                        "show_toast": False, "announce_to_chat": False, "hidden": True}}
    out[GEN / f"data/{NS}/advancement/heads/root.json"] = {
        "criteria": {"any_head": {"trigger": "minecraft:inventory_changed",
                                  "conditions": {"items": [{"items": "#" + NS + ":heads"}]}}},
        "requirements": [["any_head"]],
        "display": {"icon": {"id": f"{NS}:{item_id(HEADS[0])}"}, "title": {"translate": f"advancements.{NS}.heads.root.title"},
                    "description": {"translate": f"advancements.{NS}.heads.root.description"},
                    "background": "minecraft:block/soul_sand", "show_toast": True, "announce_to_chat": False}}
    items = [f"{NS}:{item_id(h)}" for h in HEADS]
    blocks = []
    for h in HEADS:
        blocks += [f"{NS}:{item_id(h)}", f"{NS}:{wall_id(h)}"]
    out[GEN / f"data/{NS}/tags/item/heads.json"] = {"values": items}
    out[GEN / "data/minecraft/tags/item/skulls.json"] = {"values": items}
    out[GEN / "data/minecraft/tags/block/mineable/axe.json"] = {"values": blocks}
    out[GEN / f"data/{NS}/tags/item/trial_chamber_heads.json"] = {
        "values": ["minecraft:zombie_head", "minecraft:skeleton_skull"] + [f"{NS}:{item_id(h)}" for h in HEADS if h.get("trial")]}
    out[GEN / "data/minecraft/tags/item/freeze_immune_wearables.json"] = {
        "values": [f"{NS}:{item_id(h)}" for h in HEADS if h.get("freeze_immune")]}
    out[GEN / f"assets/{NS}/models/item/template_tall_head.json"] = {
        "parent": "minecraft:item/template_skull",
        "display": {"gui": {"rotation": [30, 45, 0], "translation": [0, 1.5, 0], "scale": [0.8, 0.8, 0.8]}}}
    return out


def lang(locale):
    i = 0 if locale == "en_us" else 1
    keys = {}
    for h in HEADS:
        name = h["en"] if i == 0 else h["de"]
        keys[f"block.{NS}.{item_id(h)}"] = name
        keys[f"block.{NS}.{wall_id(h)}"] = name
        for v in (h.get("variants") or {}).get("list", []):
            keys[f"variant.{NS}.{h['id']}.{v['id']}"] = v["en"] if i == 0 else v["de"]
        if h.get("ability"):
            keys[f"ability.{NS}.{h['ability']}"] = name
            # Tooltip lines stay within 48 characters (tools/check_tooltip_text.py); the secret/wiki text keeps one line.
            keys[f"ability.{NS}.{h['ability']}.tooltip"] = "\n".join(textwrap.wrap(ABILITIES[h["ability"]][i], 48))
    return keys


# Riddles of the EH21 easter-egg heads (rule A6: hinted in the game, never spelled out): (use EN, lore EN, use DE, lore DE).
EASTER = {
    "tribute": ("No mob drops it. A pig with a certain name never dies - strike it down, and its crown falls instead; "
                "the title screen remembers him. Place it, wear it or put it on a note block.",
                "{name}: Technoblade never dies.",
                "Kein Mob lässt ihn fallen. Ein Schwein mit einem bestimmten Namen stirbt nie - schlägst du es nieder, fällt "
                "stattdessen seine Krone; der Startbildschirm erinnert an ihn. Aufstellen, tragen oder auf einen Notenblock setzen.",
                "{name}: Technoblade never dies."),
    "giant": ("No ordinary zombie drops it. When lightning strikes a zombie under a full moon, something big gets up - "
              "only a charged creeper brings its head down. Place it, wear it or put it on a note block.",
              "{name}: the biggest trouble a full moon ever brought.",
              "Kein gewöhnlicher Zombie lässt ihn fallen. Trifft bei Vollmond ein Blitz einen Zombie, steht etwas Großes auf - "
              "nur ein geladener Creeper holt seinen Kopf. Aufstellen, tragen oder auf einen Notenblock setzen.",
              "{name}: der größte Ärger, den ein Vollmond je brachte."),
    "giant_cow": ("No ordinary cow drops it. A cow fed hay bales, one by one, may - very rarely - grow into a giant; "
                  "only a charged creeper brings its head down. Place it, wear it or put it on a note block.",
                  "{name}: it really did eat all that hay.",
                  "Keine gewöhnliche Kuh lässt ihn fallen. Eine Kuh, die Heuballen frisst, einen nach dem anderen, wird - sehr selten - "
                  "zur Riesin; nur ein geladener Creeper holt ihren Kopf. Aufstellen, tragen oder auf einen Notenblock setzen.",
                  "{name}: sie hat das ganze Heu wirklich gefressen."),
}


def questionary(locale):
    en = locale == "en_us"
    out = {}
    for h in HEADS:
        name = h["en"] if en else h["de"]
        mob = h["en"].replace(" Head", "").replace(" Skull", "")
        if en:
            use = (f"Dropped when a charged creeper's explosion kills the mob, one head per explosion; no recipe. "
                   f"Place it on the floor or a wall, wear it (that mob sees you only half as far), or put it on a note block.")
            if h.get("drop") == "wither":
                use = "Only with the server switch for boss heads: dropped when a charged creeper's explosion kills it. Place it, wear it or put it on a note block."
            secret = ABILITIES[h["ability"]][0] if h.get("ability") else "Sneak while wearing it: a quiet greeting in the mob's own voice."
            lore = f"{{name}}: proof that a charged creeper met a {mob.lower()}."
            if h.get("drop") == "ritual":
                use = ("No mob drops it. Gold carries moss, moss carries netherrack, four red lights keep watch - "
                       "light it at night. Place it, wear it or put it on a note block.")
                lore = "{name}: he was never removed."
        else:
            use = ("Fällt, wenn die Explosion eines geladenen Creepers den Mob tötet, ein Kopf je Explosion; kein Rezept. "
                   "Auf Boden oder Wand stellen, tragen (der Mob sieht dich nur halb so weit) oder auf einen Notenblock setzen.")
            if h.get("drop") == "wither":
                use = "Nur mit dem Server-Schalter für Boss-Köpfe: fällt, wenn ein geladener Creeper ihn sprengt. Aufstellen, tragen oder auf einen Notenblock setzen."
            secret = ABILITIES[h["ability"]][1] if h.get("ability") else "Beim Schleichen mit dem Kopf: ein leiser Gruß mit der Stimme des Mobs."
            lore = "{name}: der Beweis, dass ein geladener Creeper hier war."
            if h.get("drop") == "ritual":
                use = ("Kein Mob lässt ihn fallen. Gold trägt Moos, Moos trägt Netherrack, vier rote Lichter wachen - "
                       "zünde es nachts an. Aufstellen, tragen oder auf einen Notenblock setzen.")
                lore = "{name}: er wurde nie entfernt."
        if h.get("drop") in EASTER:
            riddle = EASTER[h["drop"]]
            use, lore = (riddle[0], riddle[1]) if en else (riddle[2], riddle[3])
        entry = {"lore": [lore], "use": [use], "secret": [secret]}
        out[item_id(h)] = entry
        out[wall_id(h)] = entry
    out["guide_book"] = {
        "lore": ["A book about every mob's head." if en else "Ein Buch über den Kopf jedes Mobs."],
        "use": ["The Simple Mobheads Guide explains drops, variants and head abilities. Craft it shapeless from a book and a skeleton skull."
                if en else "Das Simple-Mobheads-Handbuch erklärt Beute, Varianten und Kopf-Fähigkeiten. Formlos aus einem Buch und einem Skelettschädel."],
        "secret": ["Its pages follow the wiki, so it never tells more than the heads really do."
                   if en else "Seine Seiten folgen dem Wiki und verraten nie mehr, als die Köpfe wirklich tun."]}
    return out


def prices():
    rows = []
    for h in HEADS:
        if h["id"] in TRADE_EXCLUDED:
            continue
        # Village wanderer concept (W8, 2026-10-15) and a-small22: the 14 heads that moved in (chance 0.1) are the
        # Village Wanderer's (Simple Wanderers' pool wanderer/rare), no longer the wandering trader's.
        merchant, tier, hours, level, stock, chance = LEGACY_TRADE.get(h["id"], (
            "village_wanderer" if h.get("from") else "wandering_trader", 3, 15.0, 4, 1, 0.1 if h.get("from") else 0.05))
        rows.append({"item": f"{NS}:{item_id(h)}", "tier": tier, "hours": hours, "craft": 0, "safetyFloor": 8,
                     "merchant": merchant, "level": level, "stock": stock, "chance": chance})
    return {"module": NS,
            "derivation": "Scarce heads tier 3 like SimpleBuilding's and Simple Fun's (KERNE-SELTENHEIT 5.3); heads that moved in keep their rows, "
                          "new heads are rarer (chance 0.05) so the wandering trader's rare pool is not flooded. Boss heads are not sold. "
                          "The 14 heads that moved in are sold by the Village Wanderer (Simple Wanderers), not by the wandering trader.",
            "excluded": {f"{NS}:{item_id(h)}": TRADE_EXCLUDED[h["id"]] for h in HEADS if h["id"] in TRADE_EXCLUDED},
            "prices": rows}


def validate():
    errors = []
    seen = set()
    abilities_used = set()
    for h in HEADS:
        if h["id"] in seen:
            errors.append("duplicate head " + h["id"])
        seen.add(h["id"])
        for key in ("en", "de", "group", "sound", "model"):
            if key not in h:
                errors.append(f"{h['id']}: missing {key}")
        if h.get("ability"):
            if h["ability"] not in ABILITIES:
                errors.append(f"{h['id']}: unknown ability {h['ability']}")
            abilities_used.add(h["ability"])
        if h.get("variants"):
            ids = [v["id"] for v in h["variants"]["list"]]
            if len(ids) != len(set(ids)):
                errors.append(f"{h['id']}: duplicate variant ids")
            if not h["variants"].get("component") and not h.get("variant_source"):
                errors.append(f"{h['id']}: variants need a component or a variant_source")
        counts = h.get("counts_as", "")
        if counts and counts != "variant" and counts not in {x["id"] for x in HEADS}:
            errors.append(f"{h['id']}: counts_as {counts} is no head")
        if counts == "variant" and not h.get("variants"):
            errors.append(f"{h['id']}: counts_as variant needs variants")
    errors += [f"ability {a} has no head" for a in sorted(set(ABILITIES) - abilities_used)]
    return errors


def write(path, text, check, stale):
    if check:
        if not path.is_file() or path.read_text(encoding="utf-8") != text:
            stale.append(str(path.relative_to(ROOT)))
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")


def main():
    check = "--check" in sys.argv
    errors = validate()
    if errors:
        print("\n".join(errors))
        return 1
    stale = []
    expected = set()
    for path, value in resources().items():
        expected.add(path)
        write(path, json.dumps(value, indent=2, ensure_ascii=False) + "\n", check, stale)
    # Files of removed heads must not stay behind in the generated tree.
    for old in GEN.rglob("*.json"):
        if old not in expected:
            if check:
                stale.append(f"leftover {old.relative_to(ROOT)}")
            else:
                old.unlink()
    write(JAVA, java(), check, stale)
    for locale in ("en_us", "de_de"):
        path = SHARED / f"assets/{NS}/lang/{locale}.json"
        current = json.loads(path.read_text(encoding="utf-8")) if path.is_file() else {}
        generated = lang(locale)
        # Keep the file's order (other tools, e.g. tools/guides/module_guides.py, own keys too): generated keys are
        # replaced in place, new ones appended, stale generated ones dropped.
        owned = (f"block.{NS}.", f"variant.{NS}.", f"ability.{NS}.")
        merged = {k: generated.get(k, v) for k, v in current.items() if not k.startswith(owned) or k in generated}
        merged.update({k: v for k, v in generated.items() if k not in merged})
        write(path, json.dumps(merged, indent=2, ensure_ascii=False) + "\n", check, stale)
        write(SHARED / f"assets/{NS}/questionary/{locale}.json", json.dumps(questionary(locale), indent=1, ensure_ascii=False) + "\n", check, stale)
    write(PRICES, json.dumps(prices(), indent=2, ensure_ascii=False) + "\n", check, stale)
    if stale:
        print("Simple Mobheads: stale generated files (run tools/generate_heads.py):\n  " + "\n  ".join(stale))
        return 1
    print(f"Simple Mobheads: {len(HEADS)} heads, {sum(len(looks(h)) for h in HEADS)} looks " + ("current" if check else "written"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
