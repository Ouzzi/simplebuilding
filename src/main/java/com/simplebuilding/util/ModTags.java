package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

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


        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(Simplebuilding.MOD_ID, name));
        }
    }
}
