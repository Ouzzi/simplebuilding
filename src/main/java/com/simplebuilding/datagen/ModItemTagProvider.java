package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        valueLookupBuilder(ModTags.Items.CHISEL_TOOLS)
                .add(ModItems.STONE_CHISEL)
                .add(ModItems.COPPER_CHISEL)
                .add(ModItems.IRON_CHISEL)
                .add(ModItems.GOLD_CHISEL)
                .add(ModItems.DIAMOND_CHISEL)
                .add(ModItems.NETHERITE_CHISEL)
                .add(ModItems.STONE_SPATULA)
                .add(ModItems.COPPER_SPATULA)
                .add(ModItems.IRON_SPATULA)
                .add(ModItems.GOLD_SPATULA)
                .add(ModItems.DIAMOND_SPATULA)
                .add(ModItems.NETHERITE_SPATULA);

        valueLookupBuilder(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ItemTags.MINING_ENCHANTABLE);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        valueLookupBuilder(ItemTags.VANISHING_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        valueLookupBuilder(ModTags.Items.BUNDLE_ENCHANTABLE)
                .add(Items.BUNDLE);

        valueLookupBuilder(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .add(Items.BUNDLE)
                .add(Items.SHULKER_BOX);
    }
}
