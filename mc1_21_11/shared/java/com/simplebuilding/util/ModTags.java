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
        public static final TagKey<Item> CHISEL_TOOLS = createTag("chisel_tools");
        public static final TagKey<Item> CHISEL_AND_MINING_TOOLS = createTag("chisel_and_mining_tools");
        public static final TagKey<Item> BUNDLE_ENCHANTABLE = createTag("bundle_enchantable");
        public static final TagKey<Item> EXTRA_INVENTORY_ITEMS_ENCHANTABLE = createTag("extra_inventory_items");
        public static final TagKey<Item> CONSTRUCTORS_TOUCH_ENCHANTABLE = createTag("constructors_touch_enchantable");
        /** Die vier Rucksaecke. */
        public static final TagKey<Item> BACKPACKS = createTag("backpacks");
        /** Die acht Vanilla-Kupfertruhen - Zutat der Verstaerkten Truhe (Vanilla hat keinen Item-Tag dafuer). */
        public static final TagKey<Item> COPPER_CHESTS = createTag("copper_chests");
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
         * Zutaten, fuer die der Netherit- und der Enderit-Schmelzofen mehr Ausbeute geben (jeder
         * vierte bzw. zweite Vorgang +1): die Rohmetalle. Ein Rezept bekommt den Bonus nur, wenn
         * jede Zutat, die es annimmt, hier drin steht; siehe
         * {@code com.simplebuilding.blocks.entity.custom.FurnaceTierPerks}.
         */
        public static final TagKey<Item> BLAST_FURNACE_BONUS = createTag("blast_furnace_bonus");

        /**
         * Zutaten, die von keinem Ofen-Bonus profitieren - weder doppelte Erfahrung noch mehr
         * Ausbeute -, weil sie sich verlustfrei im Kreis fuehren liessen: der rissige Diamant.
         */
        public static final TagKey<Item> FURNACE_BONUS_EXCLUDED = createTag("furnace_bonus_excluded");

        /**
         * Items, die im Void nicht verloren gehen duerfen; ausgewertet von
         * {@code com.simplebuilding.mixin.EnderiteItemMixin}.
         *
         * <p>Frueher hat der Mixin die geschuetzten Items am Anzeigenamen erkannt
         * ({@code getHoverName().getString().contains("Enderite")}). Das war sprachabhaengig: in
         * jeder nicht-englischen Lokalisierung griff der Schutz nicht, und umgekehrt war jedes im
         * Amboss auf "Enderite" umbenannte Fremditem geschuetzt. Der Tag wird stattdessen per
         * Datagen deterministisch aus der Item-Registry befuellt, siehe
         * {@link #isVoidProtectedByRule(Identifier)}.
         */
        public static final TagKey<Item> VOID_PROTECTED = createTag("void_protected");

        /** Registry-Pfad-Praefix, aus dem {@link #VOID_PROTECTED} befuellt wird. */
        public static final String VOID_PROTECTED_PATH_PREFIX = "enderite_";

        /**
         * Registry-Pfade, die dem Praefix nicht folgen, aber trotzdem in den Tag gehoeren:
         * {@code raw_enderite} heisst im Englischen "Raw Enderite" und war damit vom alten
         * Namens-Check erfasst.
         */
        public static final Set<String> VOID_PROTECTED_EXTRA_PATHS = Set.of("raw_enderite");

        /**
         * Die Regel, nach der der Datagen-Provider {@link #VOID_PROTECTED} befuellt. Der
         * Gametest berechnet den Sollzustand ueber dieselbe Methode, damit Tag-Inhalt und Regel
         * nicht auseinanderlaufen koennen.
         */
        public static boolean isVoidProtectedByRule(Identifier id) {
            return Simplebuilding.MOD_ID.equals(id.getNamespace())
                    && (id.getPath().startsWith(VOID_PROTECTED_PATH_PREFIX)
                            || VOID_PROTECTED_EXTRA_PATHS.contains(id.getPath()));
        }

        /**
         * Alles ab dem Enderit-Barren aufwaerts (Barren, Nugget, Block, Werkzeuge, Ruestung,
         * aufgewertete Gegenstaende und Maschinen). Als liegengelassenes Item verschwindet es erst
         * nach {@link #ENDERITE_INGOT_TIER_LIFETIME} statt nach den 6000 Vanilla-Ticks;
         * ausgewertet von {@code com.simplebuilding.mixin.EnderiteItemMixin}. Befuellt wird der
         * Tag per Datagen ueber {@link #isEnderiteIngotTierByRule(Identifier)}.
         */
        public static final TagKey<Item> ENDERITE_INGOT_TIER = createTag("enderite_ingot_tier");

        /** Doppelte Vanilla-Lebensdauer eines Item-Entities (6000 Ticks = 5 Minuten). */
        public static final int ENDERITE_INGOT_TIER_LIFETIME = 12000;

        /**
         * Enderit-Pfade vor dem Barren (Rohstoff und Schrott) und die Schmiedevorlage, die nicht
         * aus dem Barren entsteht, sondern aus Diamanten und Endstein kopiert wird.
         */
        public static final Set<String> ENDERITE_INGOT_TIER_EXCLUDED_PATHS =
                Set.of("raw_enderite", "enderite_scrap", "enderite_upgrade_template");

        /** Die Regel fuer {@link #ENDERITE_INGOT_TIER}; Datagen und Gametest teilen sie. */
        public static boolean isEnderiteIngotTierByRule(Identifier id) {
            return isVoidProtectedByRule(id) && !ENDERITE_INGOT_TIER_EXCLUDED_PATHS.contains(id.getPath());
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
