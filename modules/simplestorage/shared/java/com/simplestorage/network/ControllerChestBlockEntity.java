package com.simplestorage.network;

import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a controller chest: a tier chest (own slots, lid, hoppers) that remembers its last network search. The search
 * ({@link ChestNetwork#scan}) runs again only when {@link ChestNetwork#version} moved on, and only when a window asks for it.
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
