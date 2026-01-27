package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        // 1. Der Block soll mit einer Spitzhacke SCHNELLER abbaubar sein
        // Das behalten wir bei.
        valueLookupBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.CRACKED_DIAMOND_BLOCK)
                .add(ModBlocks.REINFORCED_HOPPER)
                .add(ModBlocks.NETHERITE_HOPPER)
                .add(ModBlocks.REINFORCED_BLAST_FURNACE)
                .add(ModBlocks.NETHERITE_BLAST_FURNACE)
                .add(ModBlocks.REINFORCED_PISTON)
                .add(ModBlocks.NETHERITE_PISTON)
                .add(ModBlocks.NETHERITE_PISTON_HEAD)
                .add(ModBlocks.REINFORCED_FURNACE)
                .add(ModBlocks.NETHERITE_FURNACE)
                .add(ModBlocks.REINFORCED_SMOKER)
                .add(ModBlocks.NETHERITE_SMOKER);


        // 2. Er benötigt mindestens ein Eisenwerkzeug (wie Diamantblock)
        valueLookupBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.CRACKED_DIAMOND_BLOCK);


        // Pickaxe Mineable
        valueLookupBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.NIHILITH_ORE)
                .add(ModBlocks.ASTRALIT_ORE)
                .add(ModBlocks.ENDERITE_BLOCK);

        // Needs Diamond Tool (oder Netherite)
        valueLookupBuilder(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.NIHILITH_ORE)
                .add(ModBlocks.ASTRALIT_ORE)
                .add(ModBlocks.ENDERITE_BLOCK);

        // BEACON BASE (Wichtig für dein Feature)
        valueLookupBuilder(BlockTags.BEACON_BASE_BLOCKS)
                .add(ModBlocks.ENDERITE_BLOCK);
    }
}