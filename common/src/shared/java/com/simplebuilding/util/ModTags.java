package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> REPAIRS_RESONANCE_ROD = createTag("repairs_resonance_rod");
        public static final TagKey<Item> CHISEL_TOOLS = createTag("chisel_tools");
        public static final TagKey<Item> CHISEL_AND_MINING_TOOLS = createTag("chisel_and_mining_tools");
        /**
         * Was Reichweite am Amboss annimmt: die Werkzeuge oben plus Attractor (Zugradius) und Messuhr
         * (Hoehenmesser) - bei diesen beiden ohne Blockreichweite ({@code RangeReach}).
         */
        public static final TagKey<Item> RANGE_ENCHANTABLE = createTag("range_enchantable");
        public static final TagKey<Item> BUNDLE_ENCHANTABLE = createTag("bundle_enchantable");
        public static final TagKey<Item> EXTRA_INVENTORY_ITEMS_ENCHANTABLE = createTag("extra_inventory_items");
        public static final TagKey<Item> CONSTRUCTORS_TOUCH_ENCHANTABLE = createTag("constructors_touch_enchantable");
        /** Die vier Rucksaecke. */
        public static final TagKey<Item> BACKPACKS = createTag("backpacks");
        /** Die acht Vanilla-Kupfertruhen - Zutat der Verstaerkten Truhe (Vanilla hat keinen Item-Tag dafuer). */
        public static final TagKey<Item> COPPER_CHESTS = createTag("copper_chests");
        /** Vanillas drei und die Mod-Froschlichter: Zutat der Faerberezepte der Naturvarianten (N24). */
        public static final TagKey<Item> FROGLIGHTS = createTag("froglights");
        /**
         * supported_items von Tiefe Taschen und Trichter: {@code #bundle_enchantable} plus
         * {@code #backpacks}. Eigene Tags statt die Rucksaecke in {@code bundle_enchantable} zu legen,
         * weil dort auch die Schublade haengt - und die soll auf keinen Rucksack.
         */
        public static final TagKey<Item> DEEP_POCKETS_ENCHANTABLE = createTag("deep_pockets_enchantable");
        public static final TagKey<Item> FUNNEL_ENCHANTABLE = createTag("funnel_enchantable");
        /**
         * supported_items von Meisterbauer: {@code #extra_inventory_items} plus {@code #backpacks}.
         * Eigener Tag, weil an {@code extra_inventory_items} auch die Farbpalette haengt.
         */
        public static final TagKey<Item> MASTER_BUILDER_ENCHANTABLE = createTag("master_builder_enchantable");
        /**
         * Items, auf die keine Erfahrungs-Reparatur (Mending) darf, obwohl sie in
         * {@code #minecraft:enchantable/durability} stehen und Unbreaking nehmen: der Rotator, dessen
         * Haltbarkeit eine Ladung ist. Ausgewertet von {@code EnchantmentMixin}.
         */
        public static final TagKey<Item> XP_REPAIR_INCOMPATIBLE = createTag("xp_repair_incompatible");
        public static final TagKey<Item> OCTANTS_ENCHANTABLE = createTag("octants_enchantable");
        public static final TagKey<Item> SLEDGEHAMMER_ENCHANTABLE = createTag("sledgehammer_tools");
        /**
         * supported_items von Radius: die Vorschlaghaemmer plus der Erzdetektor (dort mehr Reichweite
         * fuer Gold und seltene Erze). Am Verzauberungstisch bleibt Radius Vorschlaghaemmern vorbehalten.
         */
        public static final TagKey<Item> RADIUS_ENCHANTABLE = createTag("radius_enchantable");
        public static final TagKey<Item> BUILDING_WAND_ENCHANTABLE = createTag("building_wand_enchantable");
        public static final TagKey<Item> VEINMINE_ENCHANTABLE = createTag("veinmine_enchantable");
        public static final TagKey<Item> TRIM_TEMPLATES = createTag("trim_templates");
        public static final TagKey<Item> TRIM_MATERIALS = createTag("trim_materials");

        /**
         * Zutaten, die keine doppelte Ofen-Erfahrung erhalten, weil sie sich verlustfrei im Kreis fuehren liessen: der rissige Diamant.
         */
        public static final TagKey<Item> FURNACE_BONUS_EXCLUDED = createTag("furnace_bonus_excluded");

        /**
         * Jeder Enderit-Gegenstand der Mod (Besitzer-Entscheidung 2026-09-28): Werkzeuge, Ruestung,
         * Pferde-/Nautilusruestung, Bloecke als Items, Pads, Aepfel samt verzaubertem Enderit-Apfel,
         * Rohstoff, Schrott, Vorlage. Aus diesem einen Tag speisen sich der Void-Schutz
         * ({@link #VOID_PROTECTED}) und die doppelte Liegezeit ({@link #DOUBLE_DESPAWN_TIME}); beide
         * Tags enthalten nur {@code #simplebuilding:enderite_items}, Modpacks koennen jeden einzeln
         * erweitern. Befuellt per Datagen ueber {@link #isEnderiteItemByRule(Identifier)}.
         */
        public static final TagKey<Item> ENDERITE_ITEMS = createTag("enderite_items");

        /**
         * Enderit-Stufen, deren Registry-Pfad das Wort "enderite" nicht enthaelt: die drei Flypads
         * (alle aus der Enderit-Druckplatte), das Fine Elytra Pad V (ueber dem Enderit-Pad III; seit
         * 2026-10-07 Legacy, Migrationsziel Enderit-Elytra-Pad III) und
         * das Infused Potion Pad III (Enderit-Aufwertung) und das Echolot (Enderit-Kern + Enderit-Klumpen,
         * Besitzer 2026-09-29).
         */
        public static final Set<String> ENDERITE_ITEMS_EXTRA_PATHS = Set.of(
                "flypad", "reinforced_flypad", "stellar_flypad", "fine_elytra_pad", "infused_potion_pad",
                "echo_sounder");

        /**
         * Die Regel fuer {@link #ENDERITE_ITEMS}: jedes {@code simplebuilding}-Item, dessen Pfad
         * "enderite" enthaelt (auch {@code raw_enderite}, {@code enchanted_enderite_apple}), plus
         * {@link #ENDERITE_ITEMS_EXTRA_PATHS}. Datagen und Gametest teilen sie.
         */
        public static boolean isEnderiteItemByRule(Identifier id) {
            return Simplebuilding.MOD_ID.equals(id.getNamespace())
                    && (id.getPath().contains("enderite") || ENDERITE_ITEMS_EXTRA_PATHS.contains(id.getPath()));
        }

        /**
         * Items, die im Void nicht verloren gehen duerfen; ausgewertet von
         * {@code com.simplebuilding.mixin.EnderiteItemMixin}. Enthaelt {@link #ENDERITE_ITEMS}.
         *
         * <p>Frueher hat der Mixin die geschuetzten Items am Anzeigenamen erkannt
         * ({@code getHoverName().getString().contains("Enderite")}). Das war sprachabhaengig: in
         * jeder nicht-englischen Lokalisierung griff der Schutz nicht, und umgekehrt war jedes im
         * Amboss auf "Enderite" umbenannte Fremditem geschuetzt. Der Tag wird stattdessen per
         * Datagen deterministisch aus der Item-Registry befuellt.
         */
        public static final TagKey<Item> VOID_PROTECTED = createTag("void_protected");

        /** Die Regel hinter {@link #VOID_PROTECTED}: dieselbe wie {@link #ENDERITE_ITEMS}. */
        public static boolean isVoidProtectedByRule(Identifier id) {
            return isEnderiteItemByRule(id) || isDimensionalScrap(id);
        }

        /** Dimensions-Schrott (2026-10-01): unzerstoerbar, im Void geschuetzt, viermal so lange liegend. */
        public static boolean isDimensionalScrap(Identifier id) {
            return com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP && Simplebuilding.MOD_ID.equals(id.getNamespace())
                    && id.getPath().endsWith("dimensional_scrap");
        }

        /**
         * Items, die als Item-Entity nach {@link #DOUBLE_DESPAWN_LIFETIME} statt nach den 6000
         * Vanilla-Ticks verschwinden; ausgewertet von {@code EnderiteLifetime}. Enthaelt
         * {@link #ENDERITE_ITEMS}.
         */
        public static final TagKey<Item> DOUBLE_DESPAWN_TIME = createTag("double_despawn_time");
        /** Liegt als Item-Entity viermal so lange wie Vanilla ({@link #QUADRUPLE_DESPAWN_LIFETIME}); Dimensions-Schrott. */
        public static final TagKey<Item> QUADRUPLE_DESPAWN_TIME = createTag("quadruple_despawn_time");
        /** Kleinteile, die sich mit Schleichen + Rechtsklick ablegen lassen (2026-10-02, {@code PlacedTemplates}). */
        public static final TagKey<Item> PLACEABLE_SMALL = createTag("placeable_small");
        /** Faerbbar wie Vanilla-Buendel (2026-10-02, {@code StorageDyes}): Rucksaecke, Buendel, Koecher. */
        public static final TagKey<Item> DYEABLE_STORAGE = createTag("dyeable_storage");
        public static final int QUADRUPLE_DESPAWN_LIFETIME = 24000;
        /** Item-Entity nimmt keinerlei Schaden (Feuer, Lava, Explosion, Kaktus, Amboss ...); nur der Despawn entfernt es. */
        public static final TagKey<Item> INDESTRUCTIBLE = createTag("indestructible");

        /** Doppelte Vanilla-Lebensdauer eines Item-Entities (6000 Ticks = 5 Minuten). */
        public static final int DOUBLE_DESPAWN_LIFETIME = 12000;

        /**
         * Alles ab dem Enderit-Barren aufwaerts (Barren, Nugget, Block, Werkzeuge, Ruestung,
         * aufgewertete Gegenstaende und Maschinen) - was Netherit enthaelt und deshalb feuerfest
         * ist. Steuert seit 2026-09-28 nicht mehr die Liegezeit (die gilt fuer alle
         * {@link #ENDERITE_ITEMS}); bleibt als Daten-Tag fuer Modpacks. Befuellt per Datagen ueber
         * {@link #isEnderiteIngotTierByRule(Identifier)}.
         */
        public static final TagKey<Item> ENDERITE_INGOT_TIER = createTag("enderite_ingot_tier");

        /**
         * Enderit-Pfade vor dem Barren (Rohstoff und Schrott) und die Schmiedevorlage, die nicht
         * aus dem Barren entsteht, sondern aus Diamanten und Endstein kopiert wird.
         */
        public static final Set<String> ENDERITE_INGOT_TIER_EXCLUDED_PATHS =
                Set.of("raw_enderite", "enderite_scrap", "enderite_upgrade_template");

        /** Die Regel fuer {@link #ENDERITE_INGOT_TIER}; Datagen und Gametest teilen sie. */
        public static boolean isEnderiteIngotTierByRule(Identifier id) {
            return Simplebuilding.MOD_ID.equals(id.getNamespace())
                    && (id.getPath().startsWith("enderite_") || "raw_enderite".equals(id.getPath()))
                    && !ENDERITE_INGOT_TIER_EXCLUDED_PATHS.contains(id.getPath());
        }

        /**
         * Items the attractor (magnet) never pulls - for modpacks: display items, markers or
         * quest items of other mods that lie around as item entities. By default only the
         * creative-only structure void (it shows the format and keeps the check testable); the
         * attractor also leaves items alone that can never be picked up, that belong to another
         * player, and death drops of other players ({@code com.simplebuilding.util.AttractorFilter}).
         */
        public static final TagKey<Item> ATTRACTOR_IGNORE = createTag("attractor_ignore");

        /**
         * Items that may not go into a backpack slot, on top of the built-in rule (no backpacks, no
         * shulker boxes or anything else that refuses to sit inside a container item). By default
         * only the creative-only structure void.
         */
        public static final TagKey<Item> NOT_ALLOWED_IN_BACKPACK = createTag("not_allowed_in_backpack");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
        }
    }

    public static class Blocks {
        /** Optional opt-in tag for modded colour families supported by the brush. */
        public static final TagKey<Block> DYEABLE_FAMILIES = createTag("dyeable_families");
        /**
         * Bloecke, die kein Kolben der Mod durchbricht, obwohl sie unzerstoerbar sind (Zerstoerungs-
         * geschwindigkeit unter 0): Barriere, Lichtblock, Portale, Befehls-, Struktur-, Verbund- und
         * Testbloecke, der bewegte Kolben. Ausgewertet von {@link PistonBreach#isBreachable}; dort
         * sind Bloecke mit Block-Entity zusaetzlich unabhaengig von diesem Tag ausgenommen.
         */
        public static final TagKey<Block> PISTON_BREACH_IMMUNE = createTag("piston_breach_immune");

        /**
         * Bloecke, die ein Kolben der Mod durchbricht, obwohl sie abbaubar sind (Vanilla:
         * verstaerkter Tiefenschiefer, den {@code PistonBaseBlock#isPushable} beim Namen verweigert).
         */
        public static final TagKey<Block> PISTON_BREACHABLE_EXTRA = createTag("piston_breachable_extra");

        /**
         * Blocks the building wand never places - neither on a plane nor from a blueprint or an
         * octant fill. By default only the creative-only structure void; for modpacks that want to
         * keep e.g. a mod's machine or a valuable block out of mass placement.
         */
        /**
         * Blocks an Astral/Nihil piston never moves, on top of its built-in rules (unbreakable, block
         * entities, pistons, two-part blocks, push reaction BLOCK/DESTROY): end portal frames, reinforced
         * deepslate, the mod's breachable extras and anything a modpack adds.
         */
        public static final TagKey<Block> END_PISTON_IMMOVABLE = createTag("end_piston_immovable");

        public static final TagKey<Block> BUILDING_WAND_BLACKLIST = createTag("building_wand_blacklist");

        /**
         * What Vein Miner counts as an ore with a pickaxe (and what the crack preview outlines):
         * {@code #c:ores} of the loader (every mod ore that follows the convention), the vanilla ore
         * tags, nether quartz ore, nether gold ore, ancient debris and the mod's own ores.
         */
        public static final TagKey<Block> VEIN_MINER_ORES = createTag("vein_miner_ores");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
        }
    }
}
