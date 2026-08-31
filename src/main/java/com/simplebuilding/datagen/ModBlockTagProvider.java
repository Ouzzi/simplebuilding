package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * MC 26.2: {@code valueLookupBuilder(...)} ist entfallen (Ersatz: {@code builder(...)}) und
     * {@link net.minecraft.data.tags.TagAppender} nimmt nur noch {@link ResourceKey}s statt Block-Instanzen
     * entgegen (Vanilla nutzt dafuer die Konstanten aus {@code net.minecraft.references.BlockIds}).
     * Dieser Helfer liefert den Registry-Key zu einer Block-Instanz, damit die Tag-Inhalte
     * unveraendert bleiben.
     */
    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        // 1. Der Block soll mit einer Spitzhacke SCHNELLER abbaubar sein
        // Das behalten wir bei.
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.CRACKED_DIAMOND_BLOCK))
                .add(key(ModBlocks.REINFORCED_HOPPER))
                .add(key(ModBlocks.NETHERITE_HOPPER))
                .add(key(ModBlocks.REINFORCED_BLAST_FURNACE))
                .add(key(ModBlocks.NETHERITE_BLAST_FURNACE))
                .add(key(ModBlocks.REINFORCED_PISTON))
                .add(key(ModBlocks.NETHERITE_PISTON))
                .add(key(ModBlocks.NETHERITE_PISTON_HEAD))
                .add(key(ModBlocks.REINFORCED_FURNACE))
                .add(key(ModBlocks.NETHERITE_FURNACE))
                .add(key(ModBlocks.REINFORCED_SMOKER))
                .add(key(ModBlocks.NETHERITE_SMOKER));


        // 2. Er benötigt mindestens ein Eisenwerkzeug (wie Diamantblock)
        builder(BlockTags.NEEDS_IRON_TOOL)
                .add(key(ModBlocks.CRACKED_DIAMOND_BLOCK));


        // Pickaxe Mineable
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(key(ModBlocks.NIHILITH_ORE))
                .add(key(ModBlocks.ASTRALIT_ORE))
                .add(key(ModBlocks.ENDERITE_BLOCK));

        // Needs Diamond Tool (oder Netherite)
        builder(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(key(ModBlocks.NIHILITH_ORE))
                .add(key(ModBlocks.ASTRALIT_ORE))
                .add(key(ModBlocks.ENDERITE_BLOCK));

        // BEACON BASE (Wichtig für dein Feature)
        builder(BlockTags.BEACON_BASE_BLOCKS)
                .add(key(ModBlocks.ENDERITE_BLOCK));
    }
}