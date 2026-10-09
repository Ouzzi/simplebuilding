package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.screen.StorageCraftingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Storage Crafting Table's grid (owner 2026-10-09, queue N26): nine stacks that stay when the menu closes. The
 * grid is one shared {@link CraftingContainer} ({@link #grid()}) that every open {@link StorageCraftingMenu} uses
 * directly, like the slots of a chest - two players see and take the same items, nothing is copied, so nothing can
 * be duplicated. Every change recomputes the result of each open menu and is sent to the clients, which draw the
 * stacks flat on the top ({@code StorageCraftingTableRenderer}).
 *
 * <p>Deliberately not a {@code Container}: hoppers neither fill nor empty the grid (it is a workbench, the Autonomous
 * Crafter is the automatic one). Breaking the block drops the grid ({@link #preRemoveSideEffects}).
 */
public class StorageCraftingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SIZE = 9;
    private static final Component TITLE = Component.translatable("container.simplebuilding.storage_crafting_table");

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final List<StorageCraftingMenu> viewers = new ArrayList<>();
    private final Grid grid = new Grid();

    public StorageCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_CRAFTING_TABLE_BE, pos, state);
    }

    /** The shared grid (3x3), the one container of all open menus. */
    public CraftingContainer grid() {
        return this.grid;
    }

    /** The stacks (live list; read only outside this class). */
    public List<ItemStack> items() {
        return this.items;
    }

    public void open(StorageCraftingMenu menu) {
        if (!this.viewers.contains(menu)) this.viewers.add(menu);
    }

    public void close(StorageCraftingMenu menu) {
        this.viewers.remove(menu);
    }

    /** A grid change: every open menu recomputes its result, the world saves it and the clients redraw the top. */
    private void gridChanged() {
        for (StorageCraftingMenu menu : List.copyOf(this.viewers)) {
            menu.slotsChanged(this.grid);
        }
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return this.level == null ? null
                : new StorageCraftingMenu(containerId, inventory, ContainerLevelAccess.create(this.level, this.worldPosition), this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SIZE; i++) this.items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The grid as a crafting container over {@link #items}; every write goes through {@link #gridChanged()}. */
    private final class Grid implements CraftingContainer {
        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return List.copyOf(items);
        }

        @Override
        public void fillStackedContents(StackedItemContents contents) {
            for (ItemStack stack : items) contents.accountSimpleStack(stack);
        }

        @Override
        public int getContainerSize() {
            return SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) if (!stack.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack taken = ContainerHelper.removeItem(items, slot, count);
            if (!taken.isEmpty()) gridChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            gridChanged();
        }

        /** Slots call this after changing a stack in place (stacking onto it). */
        @Override
        public void setChanged() {
            gridChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return !isRemoved();
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < SIZE; i++) items.set(i, ItemStack.EMPTY);
            gridChanged();
        }
    }
}
