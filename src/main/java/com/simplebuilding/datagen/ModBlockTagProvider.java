package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        // 1. Der Block soll mit einer Spitzhacke SCHNELLER abbaubar sein
        // Das behalten wir bei.
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
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
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
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