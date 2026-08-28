package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> CHISEL_TOOLS = createTag("chisel_tools");
        public static final TagKey<Item> CHISEL_AND_MINING_TOOLS = createTag("chisel_and_mining_tools");
        public static final TagKey<Item> BUNDLE_ENCHANTABLE = createTag("bundle_enchantable");
        public static final TagKey<Item> EXTRA_INVENTORY_ITEMS_ENCHANTABLE = createTag("extra_inventory_items");
        public static final TagKey<Item> CONSTRUCTORS_TOUCH_ENCHANTABLE = createTag("constructors_touch_enchantable");
        public static final TagKey<Item> OCTANTS_ENCHANTABLE = createTag("octants_enchantable");
        public static final TagKey<Item> SLEDGEHAMMER_ENCHANTABLE = createTag("sledgehammer_tools");
        public static final TagKey<Item> BUILDING_WAND_ENCHANTABLE = createTag("building_wand_enchantable");
        public static final TagKey<Item> VEINMINE_ENCHANTABLE = createTag("veinmine_enchantable");
        public static final TagKey<Item> TRIM_TEMPLATES = createTag("trim_templates");
        public static final TagKey<Item> TRIM_MATERIALS = createTag("trim_materials");

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

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
        }
    }
}
