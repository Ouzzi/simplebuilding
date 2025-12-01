package com.simplebuilding.datagen;

import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagProvider extends FabricTagProvider<Enchantment> {

    public static final TagKey<Enchantment> QUIVER_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/quiver_group"));
    public static final TagKey<Enchantment> BUILDER_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/builder_group"));
    public static final TagKey<Enchantment> RADIUS_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/radius_group"));
    public static final TagKey<Enchantment> BREAK_THROUGH_EXCLUSIVE_SET = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("simplebuilding", "exclusive_set/break_through_group"));

    public ModEnchantmentTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.ENCHANTMENT, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        // KORREKTUR: Nutze builder() statt getOrCreateTagBuilder()

        // Da Enchantments in 1.21 dynamisch sind, fügen wir sie per RegistryKey hinzu.
        // Wenn ModEnchantments.MASTER_BUILDER ein RegistryKey<Enchantment> ist:
        builder(BUILDER_EXCLUSIVE_SET)
                .add(ModEnchantments.MASTER_BUILDER)
                .add(ModEnchantments.COLOR_PALETTE);

        builder(QUIVER_EXCLUSIVE_SET)
                .add(ModEnchantments.QUIVER);

        builder(RADIUS_EXCLUSIVE_SET)
                .add(ModEnchantments.RADIUS);

        builder(BREAK_THROUGH_EXCLUSIVE_SET)
                .add(ModEnchantments.BREAK_THROUGH);
    }
}