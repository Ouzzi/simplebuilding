package com.simplebuilding.woodwork;

import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The wood types of the woodwork family (docs/ai/PLAN-HOLZWERK-2026-10-09.md): the Vanilla log, its stripped log
 * and its planks. {@code log} is the Vanilla path stem used in block ids ({@code oak_log}, {@code crimson_stem},
 * {@code bamboo_block}); Nether woods do not burn.
 */
public enum WoodKind {
    OAK("oak", "oak_log", () -> Blocks.OAK_LOG, () -> Blocks.STRIPPED_OAK_LOG, () -> Blocks.OAK_PLANKS, false),
    SPRUCE("spruce", "spruce_log", () -> Blocks.SPRUCE_LOG, () -> Blocks.STRIPPED_SPRUCE_LOG, () -> Blocks.SPRUCE_PLANKS, false),
    BIRCH("birch", "birch_log", () -> Blocks.BIRCH_LOG, () -> Blocks.STRIPPED_BIRCH_LOG, () -> Blocks.BIRCH_PLANKS, false),
    JUNGLE("jungle", "jungle_log", () -> Blocks.JUNGLE_LOG, () -> Blocks.STRIPPED_JUNGLE_LOG, () -> Blocks.JUNGLE_PLANKS, false),
    ACACIA("acacia", "acacia_log", () -> Blocks.ACACIA_LOG, () -> Blocks.STRIPPED_ACACIA_LOG, () -> Blocks.ACACIA_PLANKS, false),
    DARK_OAK("dark_oak", "dark_oak_log", () -> Blocks.DARK_OAK_LOG, () -> Blocks.STRIPPED_DARK_OAK_LOG, () -> Blocks.DARK_OAK_PLANKS, false),
    MANGROVE("mangrove", "mangrove_log", () -> Blocks.MANGROVE_LOG, () -> Blocks.STRIPPED_MANGROVE_LOG, () -> Blocks.MANGROVE_PLANKS, false),
    CHERRY("cherry", "cherry_log", () -> Blocks.CHERRY_LOG, () -> Blocks.STRIPPED_CHERRY_LOG, () -> Blocks.CHERRY_PLANKS, false),
    PALE_OAK("pale_oak", "pale_oak_log", () -> Blocks.PALE_OAK_LOG, () -> Blocks.STRIPPED_PALE_OAK_LOG, () -> Blocks.PALE_OAK_PLANKS, false),
    CRIMSON("crimson", "crimson_stem", () -> Blocks.CRIMSON_STEM, () -> Blocks.STRIPPED_CRIMSON_STEM, () -> Blocks.CRIMSON_PLANKS, true),
    WARPED("warped", "warped_stem", () -> Blocks.WARPED_STEM, () -> Blocks.STRIPPED_WARPED_STEM, () -> Blocks.WARPED_PLANKS, true),
    BAMBOO("bamboo", "bamboo_block", () -> Blocks.BAMBOO_BLOCK, () -> Blocks.STRIPPED_BAMBOO_BLOCK, () -> Blocks.BAMBOO_PLANKS, false);

    private final String id;
    private final String log;
    private final Supplier<Block> logBlock;
    private final Supplier<Block> strippedBlock;
    private final Supplier<Block> planks;
    private final boolean nether;

    WoodKind(String id, String log, Supplier<Block> logBlock, Supplier<Block> strippedBlock, Supplier<Block> planks, boolean nether) {
        this.id = id;
        this.log = log;
        this.logBlock = logBlock;
        this.strippedBlock = strippedBlock;
        this.planks = planks;
        this.nether = nether;
    }

    /** {@code oak}, {@code dark_oak}, {@code crimson}, {@code bamboo}. */
    public String id() {
        return this.id;
    }

    /** The Vanilla log id path: {@code oak_log}, {@code crimson_stem}, {@code bamboo_block}. */
    public String log() {
        return this.log;
    }

    /** The stripped log id path: {@code stripped_oak_log}. */
    public String strippedLog() {
        return "stripped_" + this.log;
    }

    public Block logBlock() {
        return this.logBlock.get();
    }

    public Block strippedBlock() {
        return this.strippedBlock.get();
    }

    public Block planks() {
        return this.planks.get();
    }

    /** Crimson and warped: no fire, no furnace fuel, a cauldron of it holds lava. */
    public boolean nether() {
        return this.nether;
    }
}
