package com.simplestorage.network;

import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a controller chest: a tier chest (lid, sounds) that remembers its last network search. The search
 * ({@link ChestNetwork#scan}) runs again only when {@link ChestNetwork#version} moved on, and only when a window asks for it.
 *
 * <p>DW04C-2 (owner 04.11.): the controller has no inventory of its own. Everything stored through it goes into the network's
 * chests ({@link ChestNetwork.Storage#insert}, hoppers and pipes through {@link ControllerChestBlock#getContainer}). The slots of the
 * tier chest stay only for items an older version stored there: the window lists them, they can be taken, and
 * {@link #moveOwnItemsIntoNetwork} moves them into the chests; what does not fit stays (and drops when the block is broken).
 */
public class ControllerChestBlockEntity extends TieredChestBlockEntity {
    private @Nullable ChestNetwork.Scan scan;
    /** How many searches this controller ran (tests: the cache works). */
    private int scans;

    public ControllerChestBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.CONTROLLER_CHEST, pos, state);
    }

    public ControllerLevel step() {
        return getBlockState().getBlock() instanceof ControllerChestBlock block ? block.step() : ControllerLevel.WOODEN;
    }

    /** The cached search, renewed when cables or interfaces changed since. */
    public ChestNetwork.Scan scan() {
        ChestNetwork.Scan current = this.scan;
        if (current == null || current.version() != ChestNetwork.version()) {
            current = ChestNetwork.scan(this.level, this.worldPosition);
            this.scan = current;
            this.scans++;
        }
        return current;
    }

    /** The network's chests to store into and take from, or null while the network is broken. {@code player} null: automation. */
    public @Nullable ChestNetwork.Storage storage(@Nullable Player player) {
        if (this.level == null) return null;
        ChestNetwork.Scan scan = scan();
        ChestNetwork.Members members = ChestNetwork.members(this.level, step().rank(), scan);
        return status(scan, members) == ChestNetwork.Status.OK ? new ChestNetwork.Storage(this.level, this, members, player) : null;
    }

    /** Old worlds (DW04C-2): moves items in the controller's own slots into the network's chests; the rest stays. */
    public void moveOwnItemsIntoNetwork() {
        if (this.level == null || this.level.isClientSide() || isEmpty()) return;
        ChestNetwork.Storage storage = storage(null);
        if (storage == null) return;
        boolean moved = false;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack rest = storage.insert(stack);
            if (rest.getCount() != stack.getCount()) {
                getItems().set(slot, rest.isEmpty() ? ItemStack.EMPTY : rest);
                moved = true;
            }
        }
        if (moved) setChanged();
    }

    /** Nothing is put into the controller itself (DW04C-2); hoppers and pipes go through the block's network intake. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    public int scanCount() {
        return this.scans;
    }

    /** The network state a window shows: OK, or why the controller is broken. */
    public ChestNetwork.Status status(ChestNetwork.Scan scan, ChestNetwork.Members members) {
        if (scan.status() != ChestNetwork.Status.OK) return scan.status();
        return members.chests().size() > step().maxChests() ? ChestNetwork.Status.TOO_MANY : ChestNetwork.Status.OK;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ControllerMenu.server(containerId, inventory, this);
    }

    @Override
    protected boolean ownsMenu(Player player) {
        return player.containerMenu instanceof ControllerMenu menu && menu.controller() == this;
    }
}
