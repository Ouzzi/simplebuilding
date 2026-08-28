package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * MC 1.21.11: {@code FabricTagProvider.builder(...)} liefert einen
     * {@link net.minecraft.data.tags.TagAppender}, der {@link ResourceKey}s entgegennimmt
     * (das instanzbasierte {@code valueLookupBuilder(...)} gibt es hier zwar noch, ab 26.2
     * aber nicht mehr). Damit die 1.21.11- und die 26.2-Linie denselben Quelltext und
     * dieselben Tag-Inhalte haben, wird durchgaengig {@code builder(...)} benutzt; dieser
     * Helfer liefert den Registry-Key zu einer Block-Instanz.
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