package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagProvider extends FabricTagProvider<Enchantment> {

    public static final TagKey<Enchantment> BUILDER_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/builder_group"));
    public static final TagKey<Enchantment> RADIUS_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/radius_group"));
    public static final TagKey<Enchantment> BREAK_THROUGH_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/break_through_group"));

    public static final TagKey<Enchantment> COVER_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/cover_group"));
    public static final TagKey<Enchantment> WAND_MODIFIER_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/wand_modifier_group"));

    public static final TagKey<Enchantment> MINING_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "exclusive_set/mining"));

    public ModEnchantmentTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.ENCHANTMENT, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {

        builder(BUILDER_EXCLUSIVE_SET)
                .add(ModEnchantments.MASTER_BUILDER)
                .add(ModEnchantments.COLOR_PALETTE);

        builder(RADIUS_EXCLUSIVE_SET)
                .add(ModEnchantments.RADIUS);

        builder(BREAK_THROUGH_EXCLUSIVE_SET)
                .add(ModEnchantments.BREAK_THROUGH);

        // 1. Cover darf nicht mit Bridge oder Linear kombiniert werden.
        // Dieses Set wird dem COVER Enchantment zugewiesen.
        builder(COVER_EXCLUSIVE_SET)
                .add(ModEnchantments.BRIDGE)
                .add(ModEnchantments.LINEAR);

        // 2. Bridge und Linear dürfen nicht mit Cover kombiniert werden.
        // Dieses Set wird den BRIDGE und LINEAR Enchantments zugewiesen.
        // Da sie sich gegenseitig hier NICHT auflisten, bleiben sie kompatibel.
        builder(WAND_MODIFIER_EXCLUSIVE_SET)
                .add(ModEnchantments.COVER);

        builder(MINING_EXCLUSIVE_SET)
                .add(ModEnchantments.STRIP_MINER)
                .add(ModEnchantments.VEIN_MINER);

        builder(EnchantmentTags.ARMOR_EXCLUSIVE_SET)
                .add(Enchantments.FEATHER_FALLING);
    }
}