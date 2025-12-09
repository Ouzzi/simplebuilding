package com.simplebuilding.datagen;

import com.google.gson.JsonObject;
import com.simplebuilding.block.ModBlocks;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.client.data.*;
import net.minecraft.item.Item;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;


public class ModModelProvider extends FabricModelProvider {


    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {

        // --- 1. RANGEFINDER (Generated / Flach) ---
        itemModelGenerator.register(ModItems.OCTANT, Models.GENERATED);

        for (DyeColor color : DyeColor.values()) {
            Item item = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (item != null) {
                itemModelGenerator.register(item, Models.GENERATED);
            }
        }

        // --- 2. CHISELS (Handheld / Werkzeug) ---
        itemModelGenerator.register(ModItems.STONE_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_CHISEL, Models.HANDHELD);

        // --- 3. SPATULAS (Handheld / Werkzeug) ---
        itemModelGenerator.register(ModItems.STONE_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_SPATULA, Models.HANDHELD);

        // --- 4. BUILDING CORES (Generated / Flach) ---
        itemModelGenerator.register(ModItems.COPPER_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.IRON_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.GOLD_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.DIAMOND_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_CORE, Models.GENERATED);

        // --- 5. BUILDING WANDS (Handheld / Werkzeug) ---
        itemModelGenerator.register(ModItems.COPPER_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_BUILDING_WAND, Models.HANDHELD);

        // SLEDGEHAMMER
        itemModelGenerator.register(ModItems.STONE_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_SLEDGEHAMMER, Models.HANDHELD);

        // --- 6. REINFORCED BUNDLES (Generated / Flach) ---
        itemModelGenerator.register(ModItems.REINFORCED_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_BUNDLE, Models.GENERATED);

        // --- 7. VELOCITY_GAUGES (Generated / Flach) ---
        itemModelGenerator.register(ModItems.VELOCITY_GAUGE, Models.GENERATED);

    }
}
