package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantmentTags {
    public static final TagKey<Enchantment> BUILDER_EXCLUSIVE_SET = tag("exclusive_set/builder_group");
    public static final TagKey<Enchantment> COVER_EXCLUSIVE_SET = tag("exclusive_set/cover_group");
    public static final TagKey<Enchantment> WAND_MODIFIER_EXCLUSIVE_SET = tag("exclusive_set/wand_modifier_group");
    public static final TagKey<Enchantment> MINING_EXCLUSIVE_SET = tag("exclusive_set/mining");

    private ModEnchantmentTags() {
    }

    private static TagKey<Enchantment> tag(String path) {
        return TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path));
    }
}
