package com.simplebuilding.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The loader's item transfer API as pipes and other mods see a block: NeoForge's
 * {@code Capabilities.Item.BLOCK}, Fabric's {@code ItemStorage.SIDED}. Vanilla hoppers do not go
 * through it (they talk to the {@code Container} directly), which is why a machine can work with a
 * hopper and still be invisible to every pipe mod. Each loader installs its implementation at start
 * through {@link PlatformServices#setItemAutomation}; only the game tests read it.
 */
public interface ItemAutomation {

    /** Returned when the loader finds no item handler on that face at all. */
    int NO_HANDLER = -1;

    /** Moves as much of {@code stack} as the face accepts into the block; the count moved. */
    int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack);

    /** Takes up to {@code amount} of {@code item} out of the block through the face; the count taken. */
    int extract(ServerLevel level, BlockPos pos, Direction side, Item item, int amount);

    ItemAutomation NOT_INSTALLED = new ItemAutomation() {
        @Override
        public int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
            throw new IllegalStateException("this loader installed no ItemAutomation");
        }

        @Override
        public int extract(ServerLevel level, BlockPos pos, Direction side, Item item, int amount) {
            throw new IllegalStateException("this loader installed no ItemAutomation");
        }
    };
}
