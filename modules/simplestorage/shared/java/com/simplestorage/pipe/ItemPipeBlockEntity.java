package com.simplestorage.pipe;

import com.simplelib.api.ItemFilterRules;
import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The pipe's one slot (ST27-1). Every {@link PipeTier#cooldown} ticks it pushes up to {@link PipeTier#amount} items into the
 * container at its output and, if there is room, pulls the same amount from a container on any other side (round robin; never
 * from another pipe - those push on their own). Nothing happens without a container at the end, and an idle pipe costs one
 * counter per tick like a hopper.
 *
 * <p>Filter principle (docs/ai/PRINZIPIEN-FILTER.md, {@link ItemFilterRules}): with a filter on the real item in the slot is the
 * filter. One of it stays, only the second and every further one leaves ({@link #canTakeItemThroughFace}), automation fills the
 * slot only with a match and never an empty filter slot; a player sets the filter by putting an item in.
 */
public class ItemPipeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int FILTER_MODE = 0, OUTPUT = 1, TIER = 2;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int filterMode;
    private int cooldown;
    /** Index in {@link Direction#values()} the next pull starts at. */
    private int pullFrom;

    /** Menu data: filter mode, output side, tier (all read from the state, the mode is the only writable one). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case FILTER_MODE -> ItemPipeBlockEntity.this.filterMode;
                case OUTPUT -> ItemPipeBlockEntity.this.output().ordinal();
                case TIER -> ItemPipeBlockEntity.this.tier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            if (id == FILTER_MODE) ItemPipeBlockEntity.this.filterMode = ItemFilterRules.clamp(value);
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.PIPE, pos, state);
    }

    public PipeTier tier() {
        return getBlockState().getBlock() instanceof ItemPipeBlock pipe ? pipe.tier() : PipeTier.BASIC;
    }

    public Direction output() {
        return getBlockState().hasProperty(ItemPipeBlock.FACING) ? getBlockState().getValue(ItemPipeBlock.FACING) : Direction.DOWN;
    }

    public int filterMode() {
        return this.filterMode;
    }

    public ContainerData data() {
        return this.data;
    }

    /** The filter key: off - exact - same kind - off. */
    public void cycleFilterMode() {
        this.filterMode = ItemFilterRules.next(this.filterMode);
        setChanged();
    }

    /** Whether the player in the menu may put {@code stack} into the slot (an empty slot takes anything: that sets the filter). */
    public boolean mayPlayerPlace(ItemStack stack) {
        return ItemFilterRules.mayPlayerPlace(this.filterMode, this.items.get(0), stack);
    }

    /** The item and filter that moved with an upgrade (see {@link Pipes#upgradeInPlace}). */
    public void receiveUpgraded(ItemStack held, int mode) {
        this.items.set(0, held);
        this.filterMode = ItemFilterRules.clamp(mode);
        setChanged();
    }

    // ------------------------------------------------------------------ the work

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity pipe) {
        if (--pipe.cooldown > 0) return;
        PipeTier tier = pipe.tier();
        pipe.cooldown = tier.cooldown();
        boolean moved = pipe.push(level, pos);
        moved |= pipe.pull(level, pos);
        if (moved) pipe.setChanged();
    }

    /** One transfer step to the output container; true if something moved. */
    public boolean push(Level level, BlockPos pos) {
        ItemStack held = this.items.get(0);
        int count = Math.min(tier().amount(), ItemFilterRules.movable(this.filterMode, held));
        if (held.isEmpty() || count <= 0) return false;
        Direction out = output();
        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(out));
        if (target == null) return false;
        ItemStack moving = held.copyWithCount(count);
        ItemStack rest = HopperBlockEntity.addItem(this, target, moving, out.getOpposite());
        int moved = count - rest.getCount();
        if (moved <= 0) return false;
        held.shrink(moved);
        if (held.isEmpty()) this.items.set(0, ItemStack.EMPTY);
        return true;
    }

    /** One pull step from a container beside the pipe (not at the output); true if something moved. */
    public boolean pull(Level level, BlockPos pos) {
        ItemStack held = this.items.get(0);
        if (!held.isEmpty() && held.getCount() >= Math.min(getMaxStackSize(), held.getMaxStackSize())) return false;
        Direction out = output();
        Direction[] all = Direction.values();
        for (int i = 0; i < all.length; i++) {
            int index = (this.pullFrom + i) % all.length;
            Direction side = all[index];
            if (side == out) continue;
            Container source = HopperBlockEntity.getContainerAt(level, pos.relative(side));
            if (source == null || source instanceof ItemPipeBlockEntity) continue;
            if (takeFrom(source, side.getOpposite())) {
                this.pullFrom = (index + 1) % all.length;
                return true;
            }
        }
        return false;
    }

    private boolean takeFrom(Container source, Direction face) {
        int[] slots;
        if (source instanceof WorldlyContainer worldly) {
            slots = worldly.getSlotsForFace(face);
        } else {
            slots = new int[source.getContainerSize()];
            for (int i = 0; i < slots.length; i++) slots[i] = i;
        }
        ItemStack held = this.items.get(0);
        for (int slot : slots) {
            ItemStack candidate = source.getItem(slot);
            if (candidate.isEmpty()) continue;
            if (source instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, candidate, face)) continue;
            if (!ItemFilterRules.accepts(this.filterMode, held, candidate)) continue;
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, candidate)) continue;
            int limit = Math.min(getMaxStackSize(), candidate.getMaxStackSize());
            int room = limit - (held.isEmpty() ? 0 : held.getCount());
            int count = Math.min(tier().amount(), Math.min(room, candidate.getCount()));
            if (count <= 0) continue;
            ItemStack taken = source.removeItem(slot, count);
            if (taken.isEmpty()) continue;
            if (held.isEmpty()) this.items.set(0, taken);
            else held.grow(taken.getCount());
            source.setChanged();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return PipeMenu.server(containerId, inventory, this);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && ItemFilterRules.accepts(this.filterMode, this.items.get(0), stack);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return ItemFilterRules.movable(this.filterMode, this.items.get(0)) > 0;
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.filterMode = ItemFilterRules.clamp(input.getIntOr("FilterMode", 0));
        this.pullFrom = Math.floorMod(input.getIntOr("PullFrom", 0), 6);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("FilterMode", this.filterMode);
        output.putInt("PullFrom", this.pullFrom);
    }
}
