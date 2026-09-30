#!/usr/bin/env python3
"""
FTB Quests chapters for SimpleBuilding - the single source of truth.

The quest book is written here, in Python, and this script turns it into the files the mod ships:

  26.x lines (26.2, 26.3, 26.4)   src/main/resources/data/simplebuilding/ftbquests/        JSON5 layout
  1.21.11                         mc1_21_11/fabric/src/main/resources/data/simplebuilding/ftbquests/  SNBT layout
  quest texts (en + de)           quests.simplebuilding.* in both lang files of both lines

FTB Quests has no data-pack loading: its quest book is the folder config/ftbquests/quests. The mod
copies these files there on start when FTB Quests is installed (com.simplebuilding.compat.
FtbQuestsDefaults, see docs/QUESTS.md). FTB Quests 2111.x (MC 1.21.11) reads SNBT, FTB Quests
26.1+ reads JSON5 - so each line ships its own format, generated from the same data.

Titles and descriptions are not stored as text: FTB Quests parses a translation entry that looks
like a JSON text component, so every entry is {"translate": "<key>"} and the text lives in the
mod's lang files (English and German, resolved on the client in the player's language). Quests
whose task is an advancement reuse that advancement's title and description.

Usage:
  python tools/quests/generate_quests.py          write all files
  python tools/quests/generate_quests.py --check  fail if anything is stale or the book is invalid
                                                  (gradlew check runs this: task checkQuests)
"""
from __future__ import annotations

import hashlib
import json
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
NS = "simplebuilding"
# Bumped whenever the shipped chapters change in a way an existing install should pick up (new files).
DATA_VERSION = 1
FTB_FILE_VERSION = 13

LINES = {
    "26.x": {
        "format": "json5",
        "resources": REPO / "src/main/resources",
        "items": REPO / "src/main/generated/assets/simplebuilding/items",
        "advancements": REPO / "src/main/generated/data/simplebuilding/advancement",
    },
    "1.21.11": {
        "format": "snbt",
        "resources": REPO / "mc1_21_11/fabric/src/main/resources",
        "items": REPO / "mc1_21_11/fabric/src/main/generated/assets/simplebuilding/items",
        "advancements": REPO / "mc1_21_11/fabric/src/main/generated/data/simplebuilding/advancement",
    },
}
QUEST_DIR = "data/simplebuilding/ftbquests"
LANG_KEY = "quests." + NS
FILE_PREFIX = NS + "_"


# --------------------------------------------------------------------------------------------
# The book
# --------------------------------------------------------------------------------------------

@dataclass
class Q:
    key: str
    task: str                       # "item:<id>" or "adv:<advancement id>"
    deps: tuple = ()                # quest keys; "<chapter>.<quest>" for another chapter
    title: tuple | None = None      # (en, de); None: the advancement's title (adv tasks only)
    desc: tuple | None = None       # (en, de); None: the advancement's description (adv tasks only)
    hint: tuple | None = None       # (en, de) extra description line under an advancement's text
    optional: bool = False
    capstone: bool = False          # the stage's last quest: depends on every required quest of it


@dataclass
class Chapter:
    key: str
    icon: str
    title: tuple
    subtitle: tuple
    stage: int | None               # 1..4 for the stage chapters, None for side chapters
    quests: list = field(default_factory=list)


GROUP_TITLE = ("SimpleBuilding", "SimpleBuilding")
GROUP_ICON = "simplebuilding:iron_building_wand"

CHAPTERS = [
    Chapter("stage_1", "simplebuilding:stone_chisel",
            ("Stage 1: First Steps", "Stufe 1: Erste Schritte"),
            ("Vanilla basics and your first SimpleBuilding tools. Finish this chapter to unlock Stage 2.",
             "Vanilla-Grundlagen und deine ersten SimpleBuilding-Werkzeuge. Schließ dieses Kapitel ab, um Stufe 2 freizuschalten."),
            1, [
        Q("welcome", "item:minecraft:crafting_table",
          title=("Welcome to SimpleBuilding", "Willkommen bei SimpleBuilding"),
          desc=("This book leads you through SimpleBuilding in four stages; each one unlocks when the previous one is done, the side chapters open along the way. Items you already carry or find by luck count too.",
                "Dieses Buch führt dich in vier Stufen durch SimpleBuilding; jede wird frei, sobald die vorige geschafft ist, die Nebenkapitel öffnen sich unterwegs. Gegenstände, die du schon trägst oder zufällig findest, zählen auch.")),
        Q("guide_book", "item:simplebuilding:guide_book", ("welcome",), optional=True,
          title=("A Book for Beginners", "Ein Buch für Einsteiger"),
          desc=("A book and a crafting table make the Beginner's Guide. Crafted with a matching item it becomes one of seven topic guides.",
                "Ein Buch und eine Werkbank ergeben das Einsteiger-Handbuch. Mit dem passenden Gegenstand wird daraus eines von sieben Themen-Handbüchern.")),
        Q("stone_age", "adv:minecraft:story/mine_stone", ("welcome",)),
        Q("stone_chisel", "item:simplebuilding:stone_chisel", ("stone_age",),
          title=("Stone Chisel", "Steinmeißel"),
          desc=("Cobblestone, a copper nugget and a stick make a Stone Chisel.",
                "Bruchstein, ein Kupferklumpen und ein Stock ergeben einen Steinmeißel.")),
        Q("chisel", "adv:simplebuilding:chisel/chip_off_the_old_block", ("stone_chisel",)),
        Q("iron", "adv:minecraft:story/smelt_iron", ("stone_age",)),
        Q("stone_sledgehammer", "item:simplebuilding:stone_sledgehammer", ("iron",),
          title=("Stone Sledgehammer", "Stein-Vorschlaghammer"),
          desc=("Cobblestone, an iron ingot and sticks make a Stone Sledgehammer: it mines 3x3 at once.",
                "Bruchstein, ein Eisenbarren und Stöcke ergeben einen Stein-Vorschlaghammer: er baut 3×3 auf einmal ab.")),
        Q("reshape", "adv:simplebuilding:hammer/stair_master", ("stone_sledgehammer",)),
        Q("leather_sheet", "item:simplebuilding:leather_sheet", ("welcome",),
          title=("Leather Sheet", "Lederplatte"),
          desc=("Nine leather make a Leather Sheet - backpacks, bundles and quivers are cut from it.",
                "Neun Leder ergeben eine Lederplatte - daraus werden Rucksäcke, Bündel und Köcher geschnitten.")),
        Q("backpack", "adv:simplebuilding:storage/pack_mule", ("leather_sheet", "iron")),
        Q("copper_tool", "adv:simplebuilding:tiers/copper_crafter", ("stone_chisel",)),
        Q("trade", "adv:minecraft:adventure/trade", ("welcome",),
          hint=("Wandering traders now and then sell a Copper Core - the only merchant that does.",
                "Fahrende Händler verkaufen ab und zu einen Kupferkern - als einzige Händler.")),
        Q("copper_core", "item:simplebuilding:copper_core", ("trade",),
          title=("A Core of Copper", "Ein Kern aus Kupfer"),
          desc=("Buy a Copper Core from a wandering trader. Every building wand is built around a core.",
                "Kauf einem fahrenden Händler einen Kupferkern ab. Jeder Baustab ist um einen Kern gebaut.")),
        Q("copper_wand", "item:simplebuilding:copper_building_wand", ("copper_core",),
          title=("Copper Building Wand", "Kupfer-Baustab"),
          desc=("A Copper Core on two sticks is a Copper Building Wand.",
                "Ein Kupferkern auf zwei Stöcken ist ein Kupfer-Baustab.")),
        Q("wand", "adv:simplebuilding:wand/one_click_wonder", ("copper_wand",)),
        Q("done", "adv:minecraft:story/iron_tools", capstone=True,
          title=("Stage 1 Complete", "Stufe 1 geschafft"),
          hint=("Finish every quest of this chapter and craft an iron pickaxe to unlock Stage 2.",
                "Schließ jede Quest dieses Kapitels ab und bau eine Eisenspitzhacke, um Stufe 2 freizuschalten.")),
    ]),
    Chapter("stage_2", "simplebuilding:iron_sledgehammer",
            ("Stage 2: Iron & Diamonds", "Stufe 2: Eisen & Diamanten"),
            ("Better tiers, diamond pebbles and the first reinforced machines. Finish this chapter to unlock Stage 3.",
             "Bessere Stufen, Diamantkiesel und die ersten verstärkten Maschinen. Schließ dieses Kapitel ab, um Stufe 3 freizuschalten."),
            2, [
        Q("ironclad", "adv:simplebuilding:tiers/ironclad", ("stage_1.done",)),
        Q("template", "adv:simplebuilding:tiers/step_by_step", ("stage_1.done",), optional=True),
        Q("golden", "adv:simplebuilding:tiers/golden_touch", ("ironclad",), optional=True),
        Q("diamonds", "adv:minecraft:story/mine_diamond", ("ironclad",)),
        Q("diamond_block", "item:minecraft:diamond_block", ("diamonds",),
          title=("A Block to Break", "Ein Block zum Zerschlagen"),
          desc=("Nine diamonds make a Block of Diamond - a sledgehammer crushes it into Diamond Pebbles.",
                "Neun Diamanten ergeben einen Diamantblock - ein Vorschlaghammer zerschlägt ihn zu Diamantkieseln.")),
        Q("pebbles", "adv:simplebuilding:hammer/pebble_dash", ("diamond_block",)),
        Q("cracked", "adv:simplebuilding:hammer/cracked_up", ("pebbles",)),
        Q("reinforced", "adv:simplebuilding:machines/reinforcements", ("cracked",)),
        Q("diamond_tool", "adv:simplebuilding:tiers/diamond_standard", ("diamonds",)),
        Q("enchanter", "adv:minecraft:story/enchant_item", ("diamonds",),
          hint=("SimpleBuilding adds its own enchantments for chisels, hammers, wands and storage - look for them in chests and villager trades.",
                "SimpleBuilding bringt eigene Verzauberungen für Meißel, Hämmer, Baustäbe und Lager mit - such sie in Truhen und beim Handel.")),
        Q("done", "adv:minecraft:story/enter_the_nether", capstone=True,
          title=("Stage 2 Complete", "Stufe 2 geschafft"),
          hint=("Finish every quest of this chapter and step through a Nether portal to unlock Stage 3.",
                "Schließ jede Quest dieses Kapitels ab und geh durch ein Netherportal, um Stufe 3 freizuschalten.")),
    ]),
    Chapter("stage_3", "simplebuilding:netherite_sledgehammer",
            ("Stage 3: The Nether", "Stufe 3: Der Nether"),
            ("Netherite tools, machines hammered in place and the first pads. Finish this chapter to unlock Stage 4.",
             "Netherit-Werkzeuge, vor Ort gehämmerte Maschinen und die ersten Pads. Schließ dieses Kapitel ab, um Stufe 4 freizuschalten."),
            3, [
        Q("fortress", "adv:minecraft:nether/find_fortress", ("stage_2.done",)),
        Q("bastion", "adv:minecraft:nether/find_bastion", ("stage_2.done",)),
        Q("nugget", "adv:simplebuilding:nether/nugget_of_wisdom", ("stage_2.done",)),
        Q("debris", "adv:minecraft:nether/obtain_ancient_debris", ("stage_2.done",)),
        Q("netherite", "item:minecraft:netherite_ingot", ("debris",),
          title=("Netherite", "Netherit"),
          desc=("Four netherite scrap and four gold ingots make a Netherite Ingot.",
                "Vier Netheritplatten und vier Goldbarren ergeben einen Netheritbarren.")),
        Q("netherite_template", "item:minecraft:netherite_upgrade_smithing_template", ("bastion",),
          title=("Netherite Upgrade", "Netherit-Aufwertung"),
          desc=("Bastion treasure rooms always hold a Netherite Upgrade; diamonds and netherrack copy it. Every netherite tier of SimpleBuilding is smithed with it.",
                "Der Schatzraum einer Bastion enthält immer eine Netherit-Aufwertung; Diamanten und Netherrack vervielfältigen sie. Jede Netherit-Stufe von SimpleBuilding wird damit geschmiedet.")),
        Q("heavy_metal", "adv:simplebuilding:hammer/heavy_metal", ("netherite", "netherite_template")),
        Q("nether_forged", "adv:simplebuilding:tiers/nether_forged", ("netherite", "netherite_template"), optional=True),
        Q("forged_in_place", "adv:simplebuilding:machines/forged_in_place", ("nugget",)),
        Q("diamond_plate", "adv:simplebuilding:pads/under_pressure", ("stage_2.done",)),
        # Launchpad I = schwere Waegeplatte + Eisenkern (seit 2026-09-28, keine Diamant-Druckplatte mehr)
        Q("launchpad", "adv:simplebuilding:pads/liftoff", ("stage_2.done",),
          hint=("Smith a heavy weighted pressure plate with an Iron Core and any template.",
                "Schmiede eine schwere Wägeplatte mit einem Eisenkern und einer beliebigen Vorlage.")),
        Q("netherite_plate", "item:simplebuilding:netherite_pressure_plate", ("diamond_plate", "netherite", "netherite_template"),
          title=("Netherite Pressure Plate", "Netherit-Druckplatte"),
          desc=("Smith a Diamond Pressure Plate into Netherite: the base of the Potion Pad and of every netherite pad tier.",
                "Schmiede eine Diamant-Druckplatte zu Netherit: die Basis des Trank-Pads und jeder Netherit-Pad-Stufe.")),
        Q("blaze_rod", "adv:minecraft:nether/obtain_blaze_rod", ("fortress",)),
        Q("blaze_head", "adv:simplebuilding:pads/hot_head", ("fortress",)),
        Q("potion_pad", "adv:simplebuilding:pads/bottoms_up", ("blaze_head", "netherite_plate")),
        Q("done", "adv:minecraft:story/follow_ender_eye", capstone=True,
          title=("Stage 3 Complete", "Stufe 3 geschafft"),
          hint=("Finish every quest of this chapter and follow an Eye of Ender to a stronghold to unlock Stage 4.",
                "Schließ jede Quest dieses Kapitels ab und folge einem Enderauge zu einer Festung, um Stufe 4 freizuschalten.")),
    ]),
    Chapter("stage_4", "simplebuilding:enderite_ingot",
            ("Stage 4: The End and Beyond", "Stufe 4: Das Ende und darüber hinaus"),
            ("New minerals, enderite and the strongest tier of every tool.",
             "Neue Minerale, Enderit und die stärkste Stufe jedes Werkzeugs."),
            4, [
        Q("enter_end", "adv:minecraft:story/enter_the_end", ("stage_3.done",)),
        Q("dragon", "adv:minecraft:end/kill_dragon", ("enter_end",)),
        Q("end_city", "adv:minecraft:end/find_end_city", ("dragon",)),
        Q("stardust", "adv:simplebuilding:end/stardust", ("enter_end",)),
        Q("ender_quartz", "adv:simplebuilding:end/quartz_fusion", ("stardust",)),
        Q("raw_enderite", "adv:simplebuilding:enderite/raw_deal", ("stardust",)),
        Q("scrap", "adv:simplebuilding:enderite/patience_is_a_virtue", ("raw_enderite",)),
        Q("ingot", "adv:simplebuilding:enderite/beyond_netherite", ("scrap",)),
        Q("template", "adv:simplebuilding:enderite/template_of_the_end", ("end_city",)),
        Q("armor", "adv:simplebuilding:enderite/cover_me_in_enderite", ("ingot", "template")),
        Q("tool", "adv:simplebuilding:enderite/cutting_edge", ("ingot", "template")),
        Q("hammer", "adv:simplebuilding:enderite/hammer_of_the_end", ("ingot", "template")),
        Q("machine", "adv:simplebuilding:machines/end_of_the_line", ("ingot",)),
        Q("wither", "adv:minecraft:nether/summon_wither", ("stage_3.done",), optional=True,
          hint=("Iron, gold and diamond cores are crafted around a Nether Star, and the netherite and enderite cores are smithed from the diamond one.",
                "Eisen-, Gold- und Diamantkerne entstehen um einen Netherstern, und aus dem Diamantkern werden Netherit- und Enderitkern geschmiedet.")),
        Q("core", "adv:simplebuilding:enderite/heart_of_the_end", ("template", "wither"), optional=True),
        Q("echo", "adv:simplebuilding:enderite/echolocation", ("core",), optional=True),
        Q("done", "adv:simplebuilding:enderite/void_walker", capstone=True,
          title=("Void Walker", "Leerenwandler"),
          hint=("The last quest of the SimpleBuilding stages. Congratulations!",
                "Die letzte Quest der SimpleBuilding-Stufen. Glückwunsch!")),
    ]),
    Chapter("building", "simplebuilding:octant",
            ("Building & Blueprints", "Bauen & Blaupausen"),
            ("Side chapter: chisels, wands, octants and blueprints. Opens with your first chisel.",
             "Nebenkapitel: Meißel, Baustäbe, Oktanten und Blaupausen. Öffnet sich mit deinem ersten Meißel."),
            None, [
        Q("toolkit", "adv:simplebuilding:building/root", ("stage_1.stone_chisel",)),
        Q("guide", "item:simplebuilding:guide_book_building", ("toolkit", "stage_1.guide_book"), optional=True,
          title=("Guide: Building", "Handbuch: Bauen"),
          desc=("The Beginner's Guide crafted with a brick is the building guide.",
                "Das Einsteiger-Handbuch mit einem Ziegel ergibt das Bau-Handbuch.")),
        Q("octant", "item:simplebuilding:octant", ("toolkit",),
          title=("Octant", "Oktant"),
          desc=("A compass, lightning rods, a lead, gold nuggets and a gold core make an Octant.",
                "Kompass, Blitzableiter, Leine, Goldnuggets und ein Goldkern ergeben einen Oktanten.")),
        Q("measure", "adv:simplebuilding:octant/measure_twice", ("octant",)),
        Q("colour", "adv:simplebuilding:octant/colour_coded", ("octant",), optional=True),
        Q("full_spectrum", "adv:simplebuilding:octant/full_spectrum", ("colour",), optional=True),
        Q("light", "adv:simplebuilding:building/let_there_be_light", ("toolkit",)),
        Q("checker", "adv:simplebuilding:building/checkmate", ("toolkit",)),
        Q("glow", "adv:simplebuilding:hammer/glow_up", ("toolkit", "stage_1.stone_sledgehammer")),
        Q("emitting", "adv:simplebuilding:hammer/bright_idea", ("glow",), optional=True),
        Q("fine_detail", "adv:simplebuilding:chisel/fine_detail", ("stage_2.diamond_tool",)),
        Q("sculptor", "adv:simplebuilding:chisel/sculptor", ("toolkit",), optional=True),
        Q("master_mason", "adv:simplebuilding:wand/master_mason", ("toolkit",), optional=True),
        Q("rotator", "adv:simplebuilding:gadgets/spin_doctor", ("toolkit", "stage_1.done"), optional=True,
          hint=("The Rotator needs an Iron Core - woodland mansions hide them.",
                "Der Rotator braucht einen Eisenkern - Waldanwesen verstecken welche.")),
        Q("wand_erful", "adv:simplebuilding:wand/wand_erful", ("stage_3.done",), optional=True),
        Q("palette", "adv:simplebuilding:end/palette_cleanser", ("stage_4.stardust",)),
        Q("astral", "adv:simplebuilding:end/astral_projection", ("stage_4.stardust",), optional=True),
        Q("gravity", "adv:simplebuilding:end/defying_gravity", ("stage_4.stardust",)),
        Q("blueprint", "item:simplebuilding:blueprint", ("measure", "stage_4.ender_quartz"),
          title=("Blueprint", "Blaupause"),
          desc=("Paper, an ink sac and Ender Quartz make a blank Blueprint.",
                "Papier, ein Tintenbeutel und Enderquarz ergeben eine leere Blaupause.")),
        Q("scan", "adv:simplebuilding:blueprint/copy_that", ("blueprint",)),
        Q("copy", "adv:simplebuilding:blueprint/carbon_copy", ("scan",)),
        Q("architect", "adv:simplebuilding:blueprint/instant_architect", ("scan",)),
    ]),
    Chapter("storage", "simplebuilding:backpack",
            ("Storage", "Lagerung"),
            ("Side chapter: backpacks, bundles and quivers, from leather to enderite. Opens with your first backpack.",
             "Nebenkapitel: Rucksäcke, Bündel und Köcher, von Leder bis Enderit. Öffnet sich mit deinem ersten Rucksack."),
            None, [
        Q("bundle", "item:minecraft:bundle", ("stage_1.backpack",),
          title=("Bundle Up", "Gut verschnürt"),
          desc=("Leather and string make a bundle - the base of the Quiver and the Reinforced Bundle.",
                "Leder und Faden ergeben ein Bündel - die Basis von Köcher und Verstärktem Bündel.")),
        Q("guide", "item:simplebuilding:guide_book_storage", ("stage_1.backpack", "stage_1.guide_book"), optional=True,
          title=("Guide: Storage", "Handbuch: Lagerung"),
          desc=("Everything about backpacks, bundles and quivers in one topic guide.",
                "Alles über Rucksäcke, Bündel und Köcher in einem Themen-Handbuch.")),
        Q("quiver", "adv:simplebuilding:storage/quiver_in_fear", ("bundle",)),
        Q("pitching_camp", "adv:simplebuilding:storage/pitching_camp", ("stage_1.backpack",), optional=True),
        Q("splash_of_color", "adv:simplebuilding:storage/splash_of_color", ("stage_1.backpack",), optional=True),
        Q("reinforced_bundle", "adv:simplebuilding:storage/bundle_of_joy", ("bundle", "stage_2.pebbles")),
        Q("reinforced_backpack", "item:simplebuilding:reinforced_backpack", ("stage_2.pebbles",),
          title=("Reinforced Backpack", "Verstärkter Rucksack"),
          desc=("Diamond Pebbles, a Leather Sheet and string reinforce a backpack: more room for your things.",
                "Diamantkiesel, eine Lederplatte und Faden verstärken einen Rucksack: mehr Platz für deine Sachen.")),
        Q("reinforced_quiver", "item:simplebuilding:reinforced_quiver", ("quiver", "stage_2.pebbles"),
          title=("Reinforced Quiver", "Verstärkter Köcher"),
          desc=("Diamond Pebbles and a Leather Sheet reinforce a quiver, just like a bundle.",
                "Diamantkiesel und eine Lederplatte verstärken einen Köcher, genau wie ein Bündel.")),
        Q("netherite_backpack", "adv:simplebuilding:storage/heavy_luggage", ("reinforced_backpack", "stage_3.netherite_template")),
        Q("netherite_bundle", "adv:simplebuilding:storage/deeper_pockets", ("reinforced_bundle", "stage_3.netherite_template")),
        Q("netherite_quiver", "adv:simplebuilding:storage/sharpshooter", ("reinforced_quiver", "stage_3.netherite_template")),
        Q("enderite", "adv:simplebuilding:enderite/endless_pockets", ("stage_4.template",)),
    ]),
    Chapter("gadgets", "simplebuilding:magnet",
            ("Gadgets & Tweaks", "Gadgets & Kniffe"),
            ("Side chapter: gadgets, pressure plates and pads. Opens right at the start.",
             "Nebenkapitel: Gadgets, Druckplatten und Pads. Öffnet sich gleich zu Beginn."),
            None, [
        Q("intro", "adv:simplebuilding:tweaks/root", ("stage_1.welcome",)),
        Q("guide", "item:simplebuilding:guide_book_tweaks", ("intro", "stage_1.guide_book"), optional=True,
          title=("Guide: Pads & Gadgets", "Handbuch: Pads & Geräte"),
          desc=("Every pad and gadget explained in one topic guide.",
                "Jedes Pad und jedes Gerät in einem Themen-Handbuch erklärt.")),
        Q("copper_plate", "adv:simplebuilding:pads/copper_plated", ("intro",)),
        Q("velocity", "adv:simplebuilding:gadgets/speed_reader", ("intro", "stage_1.copper_core")),
        Q("magnet", "adv:simplebuilding:gadgets/attractive_personality", ("intro",), optional=True),
        Q("laser", "adv:simplebuilding:gadgets/burning_focus", ("intro",), optional=True),
        Q("remote_detonation", "adv:simplebuilding:gadgets/remote_detonation", ("laser",), optional=True),
        Q("leap_of_faith", "adv:simplebuilding:tweaks/leap_of_faith", ("intro",), optional=True,
          hint=("Air Jump is an enchanted book found in chests; it goes on boots.",
                "Luftsprung ist ein verzaubertes Buch aus Truhen; es kommt auf Stiefel.")),
        Q("detector", "adv:simplebuilding:gadgets/ping", ("intro",), optional=True,
          hint=("The Ore Detector needs a Gold Core - bastions and fortresses hide them.",
                "Der Erzdetektor braucht einen Goldkern - Bastionen und Festungen verstecken welche.")),
        Q("netherite_food", "adv:simplebuilding:tweaks/forbidden_fruit", ("stage_3.nugget",)),
        Q("crown_jewel", "adv:simplebuilding:tweaks/crown_jewel", ("netherite_food",), optional=True),
        # Chunk-Loader I = Kupfer-Druckplatte + Kupferkern, Spawn-Teleporter I = leichte Waegeplatte + Endermankopf (2026-09-28)
        Q("chunk_loader", "adv:simplebuilding:pads/always_loaded", ("copper_plate", "stage_1.copper_core")),
        Q("spawn", "adv:simplebuilding:pads/home_sweet_spawn", ("intro",),
          hint=("Smith a light weighted pressure plate with an Enderman Head - a charged creeper's explosion knocks one off an enderman.",
                "Schmiede eine leichte Wägeplatte mit einem Endermankopf - die Explosion eines geladenen Creepers reißt einem Enderman einen ab.")),
        Q("traveller", "adv:simplebuilding:pads/frequent_traveller", ("spawn",), optional=True),
        Q("higher_ground", "adv:simplebuilding:pads/higher_ground", ("stage_3.launchpad", "stage_3.netherite_plate")),
        Q("elytra_pad", "adv:simplebuilding:pads/wings_on_loan", ("stage_3.launchpad", "stage_4.end_city")),
        Q("fine_feathers", "adv:simplebuilding:pads/fine_feathers", ("elytra_pad",), optional=True),
        Q("crumple_zone", "adv:simplebuilding:tweaks/crumple_zone", ("elytra_pad",), optional=True),
        Q("flypad", "adv:simplebuilding:pads/fly_me_to_the_moon", ("stage_4.core",), optional=True),
        Q("stellar", "adv:simplebuilding:pads/stellar", ("flypad",), optional=True),
        Q("infusion", "adv:simplebuilding:pads/infusion", ("stage_3.potion_pad", "stage_4.core"), optional=True),
        Q("enderite_food", "adv:simplebuilding:enderite/void_feast", ("stage_4.ingot",)),
        Q("last_bite", "adv:simplebuilding:enderite/the_last_bite", ("enderite_food",), optional=True),
    ]),
]


# --------------------------------------------------------------------------------------------
# Model: ids, dependencies, layout, texts
# --------------------------------------------------------------------------------------------

def hid(kind: str, key: str) -> str:
    """A stable FTB Quests id: 16 hex digits, positive as a Java long, never 0 or 1."""
    digest = hashlib.sha256(f"{NS}/{kind}/{key}".encode()).hexdigest()
    value = int(digest[:16], 16) & 0x7FFFFFFFFFFFFFFF
    if value < 2:
        value += 2
    return f"{value:016X}"


def translate(key: str) -> str:
    return json.dumps({"translate": key}, ensure_ascii=False)


def adv_key(adv: str, part: str) -> str:
    ns, path = adv.split(":", 1)
    return f"advancements.{path.replace('/', '.')}.{part}" if ns == "minecraft" else f"advancements.{ns}.{path.replace('/', '.')}.{part}"


class Book:
    def __init__(self):
        self.errors: list[str] = []
        self.quests: dict[str, tuple[Chapter, Q]] = {}
        for chapter in CHAPTERS:
            for q in chapter.quests:
                full = f"{chapter.key}.{q.key}"
                if full in self.quests:
                    self.errors.append(f"duplicate quest {full}")
                self.quests[full] = (chapter, q)
        self.deps: dict[str, list[str]] = {}
        for full, (chapter, q) in self.quests.items():
            resolved = []
            for dep in q.deps:
                target = dep if "." in dep else f"{chapter.key}.{dep}"
                if target not in self.quests:
                    self.errors.append(f"{full} depends on the unknown quest {dep}")
                    continue
                resolved.append(target)
            if q.capstone:
                if q.deps:
                    self.errors.append(f"{full} is a capstone; its dependencies are computed")
                resolved = [f"{chapter.key}.{o.key}" for o in chapter.quests if o is not q and not o.optional]
            self.deps[full] = resolved
        self.lang_en: dict[str, str] = {}
        self.lang_de: dict[str, str] = {}
        self.validate()

    # ---- validation ------------------------------------------------------------------

    def validate(self):
        # No cycles.
        state: dict[str, int] = {}

        def visit(node, path):
            if state.get(node) == 1:
                self.errors.append("dependency cycle: " + " -> ".join(path + [node]))
                return
            if state.get(node) == 2:
                return
            state[node] = 1
            for dep in self.deps.get(node, []):
                visit(dep, path + [node])
            state[node] = 2

        for node in self.quests:
            visit(node, [])
        # Stages: exactly one capstone per stage chapter, none elsewhere; every quest of stage n+1
        # needs the capstone of stage n; the side chapters never gate a stage.
        stages = sorted((c for c in CHAPTERS if c.stage), key=lambda c: c.stage)
        if [c.stage for c in stages] != list(range(1, len(stages) + 1)):
            self.errors.append("stage numbers must run 1..n")
        capstones = {}
        for chapter in CHAPTERS:
            caps = [q for q in chapter.quests if q.capstone]
            if chapter.stage and len(caps) != 1:
                self.errors.append(f"stage chapter {chapter.key} needs exactly one capstone, has {len(caps)}")
            if not chapter.stage and caps:
                self.errors.append(f"side chapter {chapter.key} must not have a capstone")
            if caps:
                capstones[chapter.stage] = f"{chapter.key}.{caps[0].key}"
        for chapter in stages:
            for q in chapter.quests:
                full = f"{chapter.key}.{q.key}"
                ancestors = self.ancestors(full)
                for other in ancestors:
                    other_chapter = self.quests[other][0]
                    if other_chapter.stage is None:
                        self.errors.append(f"{full} (stage {chapter.stage}) depends on the side quest {other}")
                    elif other_chapter.stage > chapter.stage:
                        self.errors.append(f"{full} (stage {chapter.stage}) depends on the later stage quest {other}")
                if chapter.stage > 1 and capstones.get(chapter.stage - 1) not in ancestors:
                    self.errors.append(f"{full} does not wait for the end of stage {chapter.stage - 1}")
            if chapter.stage == 1:
                roots = [q.key for q in chapter.quests if not self.deps[f"{chapter.key}.{q.key}"]]
                if roots != ["welcome"]:
                    self.errors.append(f"stage 1 must start with exactly one quest, starts with {roots}")
        for full, (chapter, q) in self.quests.items():
            kind, _, ident = q.task.partition(":")
            if kind not in ("item", "adv") or ":" not in ident:
                self.errors.append(f"{full} has the malformed task {q.task}")
            if kind == "item" and (q.title is None or q.desc is None):
                self.errors.append(f"{full} is an item quest and needs its own title and description")
            for text in (q.title, q.desc, q.hint):
                if text is not None and (len(text) != 2 or not all(isinstance(t, str) and t.strip() for t in text)):
                    self.errors.append(f"{full} has an incomplete text {text}")
            if not chapter.stage and not self.deps[full]:
                self.errors.append(f"side quest {full} has no dependency - side chapters open from a stage quest")
        ids = [hid("quest", k) for k in self.quests] + [hid("task", k) for k in self.quests] + \
              [hid("chapter", c.key) for c in CHAPTERS] + [hid("group", NS)]
        if len(set(ids)) != len(ids):
            self.errors.append("id collision")

    def ancestors(self, full: str) -> set[str]:
        seen: set[str] = set()
        stack = list(self.deps.get(full, []))
        while stack:
            node = stack.pop()
            if node not in seen:
                seen.add(node)
                stack.extend(self.deps.get(node, []))
        return seen

    def check_line(self, line: str):
        roots = LINES[line]
        items = {p.stem for p in roots["items"].glob("*.json")}
        for full, (chapter, q) in self.quests.items():
            kind, _, ident = q.task.partition(":")
            ns, path = ident.split(":", 1)
            if ns != NS:
                continue
            if kind == "item" and path not in items:
                self.errors.append(f"[{line}] {full}: no item {ident}")
            if kind == "adv" and not (roots["advancements"] / (path + ".json")).exists():
                self.errors.append(f"[{line}] {full}: no advancement {ident} in the datagen output (run datagen)")
        for chapter in CHAPTERS:
            ns, path = chapter.icon.split(":", 1)
            if ns == NS and path not in items:
                self.errors.append(f"[{line}] chapter {chapter.key}: no icon item {chapter.icon}")

    # ---- layout ----------------------------------------------------------------------

    def layout(self, chapter: Chapter) -> dict[str, tuple[float, float]]:
        depth: dict[str, int] = {}

        def d(full):
            if full not in depth:
                own = [x for x in self.deps[full] if x.startswith(chapter.key + ".")]
                depth[full] = 0 if not own else 1 + max(d(x) for x in own)
            return depth[full]

        columns: dict[int, list[str]] = {}
        for q in chapter.quests:
            full = f"{chapter.key}.{q.key}"
            columns.setdefault(d(full), []).append(full)
        pos = {}
        for col, members in columns.items():
            for i, full in enumerate(members):
                pos[full] = (col * 2.0, (i - (len(members) - 1) / 2.0) * 1.5)
        return pos

    # ---- texts -----------------------------------------------------------------------

    def text(self, key: str, pair: tuple) -> str:
        self.lang_en[key] = pair[0]
        self.lang_de[key] = pair[1]
        return translate(key)

    def quest_texts(self, chapter: Chapter, q: Q) -> tuple[str, list[str]]:
        stem = f"{LANG_KEY}.{chapter.key}.{q.key}"
        kind, _, ident = q.task.partition(":")
        title = self.text(stem + ".title", q.title) if q.title else translate(adv_key(ident, "title"))
        desc = [self.text(stem + ".description", q.desc) if q.desc else translate(adv_key(ident, "description"))]
        if q.hint:
            desc.append(self.text(stem + ".hint", q.hint))
        return title, desc


# --------------------------------------------------------------------------------------------
# Output: one neutral model, written as JSON5 (strict JSON) or SNBT
# --------------------------------------------------------------------------------------------

class Dbl(float):
    """A number FTB Quests reads as a double (x, y, size)."""


def build_files(book: Book, fmt: str, mega: bool = False) -> dict[str, str]:
    ext = "." + fmt
    files: dict[str, object] = {}
    group_id = hid("group", NS)
    group_entry = {"id": group_id, "icon": {"id": GROUP_ICON}}
    lang_tables: dict[str, dict] = {}
    for index, chapter in enumerate(CHAPTERS):
        chapter_id = hid("chapter", chapter.key)
        filename = FILE_PREFIX + chapter.key
        positions = book.layout(chapter)
        table: dict[str, object] = {}
        if index == 0:
            table[f"chapter_group.{group_id}.title"] = book.text(f"{LANG_KEY}.group.title", GROUP_TITLE)
        table[f"chapter.{chapter_id}.title"] = book.text(f"{LANG_KEY}.{chapter.key}.title", chapter.title)
        table[f"chapter.{chapter_id}.chapter_subtitle"] = [book.text(f"{LANG_KEY}.{chapter.key}.subtitle", chapter.subtitle)]
        quests = []
        for q in chapter.quests:
            full = f"{chapter.key}.{q.key}"
            quest_id = hid("quest", full)
            task_id = hid("task", full)
            kind, _, ident = q.task.partition(":")
            if mega and ident in ("simplebuilding:guide_book_building", "simplebuilding:guide_book_storage", "simplebuilding:guide_book_tweaks"):
                kind, ident = "adv", "simplebuilding:guides/" + ident.removeprefix("simplebuilding:guide_book_")
            if kind == "item":
                task = {"id": task_id, "type": "item", "item": {"id": ident, "count": 1}}
            else:
                task = {"id": task_id, "type": "advancement", "advancement": ident, "criterion": ""}
            x, y = positions[full]
            quest = {"id": quest_id, "x": Dbl(x), "y": Dbl(y)}
            if q.capstone:
                quest["shape"] = "gear"
                quest["size"] = Dbl(1.5)
            if q.optional:
                quest["optional"] = True
            quest["dependencies"] = [hid("quest", dep) for dep in book.deps[full]]
            quest["tasks"] = [task]
            quests.append(quest)
            title, desc = book.quest_texts(chapter, q)
            table[f"quest.{quest_id}.title"] = title
            table[f"quest.{quest_id}.quest_desc"] = desc
        files[f"chapters/{filename}{ext}"] = {
            "id": chapter_id,
            "group": group_id,
            "order_index": index,
            "filename": filename,
            "icon": {"id": chapter.icon},
            "default_quest_shape": "",
            "default_hide_dependency_lines": False,
            "progression_mode": "flexible",
            "quests": quests,
            "quest_links": [],
            "images": [],
        }
        lang_tables[filename] = table
    files[f"chapter_groups{ext}"] = {"chapter_groups": [group_entry]}
    files[f"chapter_group_entry{ext}"] = group_entry
    files[f"data{ext}"] = {
        "version": FTB_FILE_VERSION,
        "default_reward_team": False,
        "default_consume_items": False,
        "default_autoclaim_rewards": "disabled",
        "default_quest_shape": "circle",
        "default_quest_disable_jei": False,
        "emergency_items_cooldown": 300,
        "drop_loot_crates": False,
        "disable_gui": False,
        "grid_scale": Dbl(0.5),
        "pause_game": False,
        "lock_message": "",
        "progression_mode": "linear",
        "detection_delay": 20,
        "show_lock_icons": True,
        "drop_book_on_death": False,
        "hide_excluded_quests": False,
        "fallback_locale": "en_us",
        "verify_on_load": False,
    }
    first_lang_key = None
    install = [f"# SimpleBuilding default quests for FTB Quests - generated by tools/quests/generate_quests.py, do not edit.",
               f"# Read by com.simplebuilding.compat.FtbQuestsDefaults; paths are relative to config/ftbquests/quests.",
               f"format {fmt}", f"version {DATA_VERSION}", f"file data{ext}",
               f"list chapter_groups{ext} chapter_groups chapter_group_entry{ext} {group_id}"]
    for chapter in CHAPTERS:
        install.append(f"file chapters/{FILE_PREFIX}{chapter.key}{ext}")
    if fmt == "json5":
        # FTB Quests 26.1+: lang/<locale>/**/*.json5, merged; a chapter's quests live in
        # lang/<locale>/chapters/<chapter file>.json5, exactly where the mod saves them itself.
        for filename, table in lang_tables.items():
            path = f"lang/en_us/chapters/{filename}{ext}"
            files[path] = table
            install.append(f"file {path}")
    else:
        # FTB Quests 2111.x: one lang/<locale>.snbt per language, shared with the rest of the book.
        merged: dict[str, object] = {}
        for table in lang_tables.values():
            merged.update(table)
        first_lang_key = next(iter(merged))
        files[f"lang/en_us{ext}"] = merged
        files[f"lang_entries{ext}"] = merged
        install.append(f"map lang/en_us{ext} lang_entries{ext} {first_lang_key}")
    out = {path: (to_json(obj) if fmt == "json5" else to_snbt(obj)) for path, obj in files.items()}
    if fmt == "snbt":
        # The entries file is inserted into an existing compound: its lines without the braces.
        body = out[f"lang_entries{ext}"].strip()
        out[f"lang_entries{ext}"] = body[1:-1].strip("\n") + "\n"
        body = out[f"chapter_group_entry{ext}"]
        out[f"chapter_group_entry{ext}"] = body
    out["install.txt"] = "\n".join(install) + "\n"
    return out


def to_json(obj) -> str:
    return json.dumps(obj, indent="\t", ensure_ascii=False) + "\n"


def snbt_string(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def snbt_key(k: str) -> str:
    return k if k.replace("_", "").isalnum() and k.isascii() else snbt_string(k)


def to_snbt(obj, indent: int = 0) -> str:
    """FTB-style SNBT that FTB Library and vanilla's TagParser both read (commas between entries)."""
    pad = "\t" * (indent + 1)
    end = "\t" * indent
    if isinstance(obj, dict):
        if not obj:
            return "{ }"
        inner = ",\n".join(f"{pad}{snbt_key(k)}: {to_snbt(v, indent + 1)}" for k, v in obj.items())
        return "{\n" + inner + "\n" + end + "}" + ("\n" if indent == 0 else "")
    if isinstance(obj, list):
        if not obj:
            return "[ ]"
        if all(isinstance(v, str) for v in obj):
            return "[" + ", ".join(snbt_string(v) for v in obj) + "]"
        inner = ",\n".join(f"{pad}{to_snbt(v, indent + 1)}" for v in obj)
        return "[\n" + inner + "\n" + end + "]"
    if isinstance(obj, bool):
        return "true" if obj else "false"
    if isinstance(obj, Dbl):
        return repr(float(obj)) + "d"
    if isinstance(obj, int):
        return str(obj)
    if isinstance(obj, str):
        return snbt_string(obj)
    raise TypeError(obj)


# --------------------------------------------------------------------------------------------
# Lang files
# --------------------------------------------------------------------------------------------

def lang_paths() -> list[tuple[Path, str]]:
    out = []
    for roots in LINES.values():
        for loc in ("en_us", "de_de"):
            out.append((roots["resources"] / "assets/simplebuilding/lang" / f"{loc}.json", loc))
    return out


def updated_lang(path: Path, entries: dict[str, str]) -> str:
    data = json.loads(path.read_text(encoding="utf-8"))
    # Main-line facts; the 1.21.11 copy follows in the separate port run.
    if path.is_relative_to(REPO / "src/main/resources"):
        entries = dict(entries)
        german = path.stem == "de_de"
        entries[LANG_KEY + ".stage_1.stone_sledgehammer.description"] += (
            " Auf 26.3 trägt Schleichen ohne Berührung des Konstrukteurs eine Zielecke mit 1,5-fachem Tempo ab: Innenecke, gerade Treppe, Außenecke, Stufe."
            if german else
            " On 26.3, sneak without Constructor's Touch to remove one aimed corner at 1.5x speed: inner corner, straight stair, outer corner, slab.")
        entries[LANG_KEY + ".stage_1.guide_book.description"] = (
            "Ein Buch und eine Werkbank ergeben das Einsteiger-Handbuch. Mit dem passenden Gegenstand wird daraus ein Themen-Handbuch."
            if german else
            "A book and a crafting table make the Beginner's Guide. Craft it with a matching item to make a topic guide.")
        entries[LANG_KEY + ".stage_3.launchpad.hint"] = (
            "Schmiede eine schwere Wägeplatte mit einem Eisenkern im Vorlagenfeld und einem Trial-Chamber-Mobkopf als Zusatz."
            if german else
            "Smith a heavy weighted pressure plate with an Iron Core in the template slot and a Trial Chamber mob head as the addition.")
    items = [(k, v) for k, v in data.items() if not k.startswith(LANG_KEY + ".")]
    anchor = max((i for i, (k, _) in enumerate(items) if k.startswith("advancements." + NS + ".")), default=len(items) - 1)
    items = items[:anchor + 1] + list(entries.items()) + items[anchor + 1:]
    return json.dumps(dict(items), indent=2, ensure_ascii=False) + "\n"


# --------------------------------------------------------------------------------------------

def main(argv: list[str]) -> int:
    check = "--check" in argv
    book = Book()
    for line in LINES:
        book.check_line(line)
    outputs: dict[Path, str] = {}
    for line, roots in LINES.items():
        base = roots["resources"] / QUEST_DIR
        for rel, text in build_files(book, roots["format"]).items():
            outputs[base / rel] = text
    mega_base = REPO / "mc26_3/overlay/resources" / QUEST_DIR
    for rel, text in build_files(book, "json5", mega=True).items():
        shared = REPO / "src/main/resources" / QUEST_DIR / rel
        if shared.exists() and shared.read_text(encoding="utf-8") == text:
            continue
        outputs[mega_base / rel] = text
    for path, loc in lang_paths():
        outputs[path] = updated_lang(path, book.lang_en if loc == "en_us" else book.lang_de)
    if book.errors:
        print("FTB Quests book is invalid:")
        for e in book.errors:
            print("  - " + e)
        return 1
    stale = []
    expected_dirs = {LINES[l]["resources"] / QUEST_DIR for l in LINES} | {mega_base}
    for d in expected_dirs:
        if d.exists():
            for f in d.rglob("*"):
                if f.is_file() and f not in outputs:
                    stale.append(f)
                    if not check:
                        f.unlink()
    for path, text in outputs.items():
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current != text:
            stale.append(path)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8", newline="\n")
    quests = len(book.quests)
    if check:
        if stale:
            print("FTB Quests files are out of date - run: python tools/quests/generate_quests.py")
            for p in stale[:20]:
                print("  " + str(p.relative_to(REPO)))
            return 1
        print(f"FTB Quests book OK: {len(CHAPTERS)} chapters, {quests} quests")
        return 0
    print(f"wrote {len(stale)} file(s); {len(CHAPTERS)} chapters, {quests} quests")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
