package com.simplebuilding.woodwork;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The contents of a crate: {@link #SLOTS} stacks, one stack per layer from the bottom up. Only food (items with the
 * {@code minecraft:food} component) and items in {@link #STORABLE} go in. A plain Vanilla {@link Container}, so
 * hoppers fill and empty it, comparators read it and the contents drop when the crate is broken
 * ({@code BlockEntity#preRemoveSideEffects}). Sent to the client, which draws the layers (CrateRenderer).
 */
public class CrateBlockEntity extends BlockEntity implements Container {
    public static final int SLOTS = 8;
    /** Extra items a crate takes besides food (empty by default; for data packs). */
    public static final TagKey<Item> STORABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("simplebuilding", "crate_storable"));

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE_BE, pos, state);
    }

    public static boolean storable(ItemStack stack) {
        return !stack.isEmpty() && (stack.has(DataComponents.FOOD) || stack.is(STORABLE));
    }

    public NonNullList<ItemStack> items() {
        return this.items;
    }

    /** Puts as much of {@code stack} as fits in (same items first, then empty layers); returns what is left. */
    public ItemStack insert(ItemStack stack) {
        if (!storable(stack)) {
            return stack;
        }
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int slot = 0; slot < SLOTS && !rest.isEmpty(); slot++) {
                ItemStack here = this.items.get(slot);
                if (pass == 0 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, rest)) {
                    int move = Math.min(rest.getCount(), getMaxStackSize(here) - here.getCount());
                    if (move > 0) {
                        here.grow(move);
                        rest.shrink(move);
                    }
                } else if (pass == 1 && here.isEmpty()) {
                    int move = Math.min(rest.getCount(), getMaxStackSize(rest));
                    this.items.set(slot, rest.split(move));
                }
            }
        }
        if (rest.getCount() != stack.getCount()) {
            setChanged();
        }
        return rest;
    }

    /** Takes {@code count} (at most) from the top layer. */
    public ItemStack takeTop(int count) {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!this.items.get(slot).isEmpty()) {
                return removeItem(slot, count);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Filled share 0..1 over all eight stacks. */
    public float fill() {
        float sum = 0;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                sum += (float) stack.getCount() / getMaxStackSize(stack);
            }
        }
        return sum / SLOTS;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(this.items, slot, count);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return storable(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ saving and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
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
}
