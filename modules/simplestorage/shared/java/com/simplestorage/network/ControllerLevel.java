package com.simplestorage.network;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;

/**
 * The step of a controller chest (BS29b-3). It limits how many chest blocks a network may hold ({@link #maxChests}; one half of a
 * double chest is one chest block) and how good they may be ({@link #rank}): a chest above the controller's rank is ignored.
 * Ranks of chests: Vanilla chests and copper chests 0, Reinforced 1, Netherite 2, Enderite 3. The wooden controller therefore
 * takes up to two double chests of wood and copper, the Enderite controller everything.
 */
public enum ControllerLevel {
    WOODEN("wooden", 0, 4, ChestTier.REINFORCED),
    REINFORCED("reinforced", 1, 12, ChestTier.REINFORCED),
    NETHERITE("netherite", 2, 32, ChestTier.NETHERITE),
    ENDERITE("enderite", 3, 64, ChestTier.ENDERITE);

    private final String id;
    private final int rank;
    private final int maxChests;
    private final ChestTier slotTier;

    ControllerLevel(String id, int rank, int maxChests, ChestTier slotTier) {
        this.id = id;
        this.rank = rank;
        this.maxChests = maxChests;
        this.slotTier = slotTier;
    }

    public String id() {
        return this.id;
    }

    /** Block and item id of this step's controller chest. */
    public String controllerId() {
        return this.id + "_controller_chest";
    }

    public int rank() {
        return this.rank;
    }

    public int maxChests() {
        return this.maxChests;
    }

    /** The chest tier that gives the controller its own slots and stack size (the wooden one uses the Reinforced numbers). */
    public ChestTier slotTier() {
        return this.slotTier;
    }

    /** The plain chest this step turns into when the network is switched off. */
    public Block plainChest() {
        return switch (this) {
            case WOODEN -> Blocks.CHEST;
            case REINFORCED -> com.simplestorage.registry.StorageBlocks.REINFORCED_CHEST;
            case NETHERITE -> com.simplestorage.registry.StorageBlocks.NETHERITE_CHEST;
            case ENDERITE -> com.simplestorage.registry.StorageBlocks.ENDERITE_CHEST;
        };
    }

    /** Rank of a chest block a network may hold, or -1 for everything else (barrels, controllers, boxes). */
    public static int rankOf(Block block) {
        if (!(block instanceof ChestBlock) || block instanceof ControllerChestBlock) return -1;
        if (block instanceof TieredChestBlock tiered) return tiered.tier().ordinal() + 1;
        return 0;
    }
}
