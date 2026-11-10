package com.simplebuilding.modules.simplelayers.backrooms.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One level of the stacked Backrooms (BACK22 core §4.1): a building block per level, each in its own package
 * {@code backrooms.level.<theme>} so the ten levels can be built in parallel without touching shared files. Its position in the
 * stack is only in {@code Bands.ORDER} (owner g1: R9). The core asks it for its generator, palette, door rule and look.
 */
public interface Band {
    /** Theme id ({@code lobby}, {@code pools}, …): biome {@code simplelayers:backrooms_<id>}, texts, tests {@code <id>_*}. */
    String id();

    BandLayout layout();

    /** The block of a piece (palette); {@link CorePiece#HULL} must be a solid block the Backrooms protect. */
    BlockState state(BandPiece piece);

    /** Rule of the door pair from this level down to the next (g3 a: the upper level decides). */
    DoorRule doorRule();

    /** The part that repairs/activates this level's doors (null: none yet - only the maintenance box mends). */
    default @Nullable Item repairPart() {
        return null;
    }

    /** Share of chunks with a door pair from this level down (start value 0.04). */
    default float doorDensity() {
        return 0.04F;
    }

    /**
     * Share of this level's door pairs to the level ABOVE that also have a door back up here (BACK22c §8.1: up-doors rarer the deeper
     * you are). 1 = every pair two-sided; below 1 the rest are one-way down (arrival on a free spot, no door back).
     */
    default float upDoorShare() {
        return 1.0F;
    }

    /** Where the maintenance box of this level's doors sits (BACK22c §8.2); the core builds {@link BoxPlacement#FRAME} so far. */
    default BoxPlacement boxPlacement() {
        return BoxPlacement.FRAME;
    }

    /** Share of chalk/sign pointers toward the next up-door (BACK22c §8.8, 0..1); drawn by the level. */
    default float pointerShare() {
        return 0.0F;
    }

    /** Loot table of this level's containers ({@link BandLayout#containers}); null = none. */
    default net.minecraft.resources.@Nullable ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot() {
        return null;
    }

    BandView view();

    /** Registers the level's own blocks/items (API freeze 10.11.; standard: none). */
    default void register(BandRegistrar registrar) {}

    /** Adds the level's gametests (names {@code <id>_*}) to the module catalogue; the adapter is {@code level/<Id>GameTest} (Fabric). */
    default void tests(java.util.function.BiConsumer<String, java.util.function.Consumer<net.minecraft.gametest.framework.GameTestHelper>> catalogue) {}
}
