package com.simplestorage.network;

import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.config.StorageConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Switching the chest network off ({@code chestNetwork} in {@code config/simplestorage.json}) deletes nothing: the blocks that are
 * already in a world turn into an equal Vanilla block as soon as they are touched - a neighbour changes, somebody right-clicks
 * them, or at the latest their random tick comes: the cable into copper bars, the interface into a glass pane, the controller
 * chest into the plain chest of its step (contents, name, lock and facing stay; what does not fit the smaller chest pops out).
 * The items cannot be placed any more.
 */
public final class NetworkSubstitutes {
    private NetworkSubstitutes() {
    }

    public static boolean enabled() {
        return StorageConfig.chestNetwork;
    }

    /** Replaces the network block at {@code pos} when the network is off; true if it did (server only). */
    public static boolean replaceIfDisabled(Level level, BlockPos pos, BlockState state) {
        if (enabled() || !(level instanceof ServerLevel server)) return false;
        Block block = state.getBlock();
        if (block instanceof CableBlock) {
            server.setBlock(pos, Blocks.COPPER_BARS.weathering().unaffected().defaultBlockState(), Block.UPDATE_ALL);
        } else if (block instanceof InterfaceBlock) {
            server.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (block instanceof ControllerChestBlock controller) {
            toPlainChest(server, pos, state, controller);
        } else {
            return false;
        }
        ChestNetwork.changed();
        return true;
    }

    /** The controller chest as the plain chest of its step, keeping everything it holds. */
    static void toPlainChest(ServerLevel level, BlockPos pos, BlockState state, ControllerChestBlock controller) {
        List<ItemStack> items = new ArrayList<>();
        DataComponentMap.Builder kept = DataComponentMap.builder();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BaseContainerBlockEntity container) {
            DataComponentMap components = container.collectComponents();
            kept.set(DataComponents.CUSTOM_NAME, components.get(DataComponents.CUSTOM_NAME));
            if (components.has(DataComponents.LOCK)) kept.set(DataComponents.LOCK, components.get(DataComponents.LOCK));
            for (int i = 0; i < container.getContainerSize(); i++) items.add(container.getItem(i));
            container.clearContent();
        }
        level.setBlock(pos, controller.step().plainChest().withPropertiesOf(state), Block.UPDATE_ALL);
        BlockEntity plain = level.getBlockEntity(pos);
        if (plain instanceof BaseContainerBlockEntity container) container.applyComponents(kept.build(), DataComponentPatch.EMPTY);
        if (plain instanceof TieredChestBlockEntity tiered) {
            tiered.receiveUpgradedContents(items);
        } else if (plain instanceof Container container) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) continue;
                if (i < container.getContainerSize() && container.getItem(i).isEmpty() && stack.getCount() <= container.getMaxStackSize(stack)) {
                    container.setItem(i, stack);
                } else {
                    Block.popResource(level, pos, stack);
                }
            }
        } else {
            items.forEach(stack -> Block.popResource(level, pos, stack));
        }
    }
}
