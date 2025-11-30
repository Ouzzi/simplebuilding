package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
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

        var rangefinderBuilder = valueLookupBuilder(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(ModItems.RANGEFINDER_ITEM);

        // Alle gefärbten Varianten hinzufügen
        for (Item coloredRangefinder : ModItems.COLORED_RANGEFINDERS.values()) {
            rangefinderBuilder.add(coloredRangefinder);
        }

        valueLookupBuilder(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ItemTags.MINING_ENCHANTABLE);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.MINING_LOOT_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.VANISHING_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        valueLookupBuilder(ModTags.Items.BUNDLE_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE);

        valueLookupBuilder(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE)
                .add(Items.SHULKER_BOX);

        valueLookupBuilder(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE)
                .add(Items.SHULKER_BOX)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE);

        valueLookupBuilder(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .add(ModItems.STONE_SLEDGEHAMMER)
                .add(ModItems.COPPER_SLEDGEHAMMER)
                .add(ModItems.IRON_SLEDGEHAMMER)
                .add(ModItems.GOLD_SLEDGEHAMMER)
                .add(ModItems.DIAMOND_SLEDGEHAMMER)
                .add(ModItems.NETHERITE_SLEDGEHAMMER);
    }
}
