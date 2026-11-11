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
    public static final int FILTER_MODE = 0, OUTPUT = 1, TIER = 2, SIDES = 3;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int filterMode;
    private int cooldown;
    /** Index in {@link Direction#values()} the next pull starts at. */
    private int pullFrom;
    /** Round robin over the outputs (ROHR33): index in {@link Direction#values()} the next push starts at. */
    private int pushTo;
    /** The state of each side ({@link Pipes#IN}/{@link Pipes#OUT}/{@link Pipes#OFF}); null = an old pipe or a fresh one: taken from the block state's one output. */
    private int[] sides;

    /** Menu data: filter mode, output side, tier (all read from the state, the mode is the only writable one). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case FILTER_MODE -> ItemPipeBlockEntity.this.filterMode;
                case OUTPUT -> ItemPipeBlockEntity.this.output().ordinal();
                case TIER -> ItemPipeBlockEntity.this.tier().ordinal();
                case SIDES -> Pipes.pack(ItemPipeBlockEntity.this.sideModes());
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            if (id == FILTER_MODE) ItemPipeBlockEntity.this.filterMode = ItemFilterRules.clamp(value);
        }

        @Override
        public int getCount() {
            return 4;
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

    /** A copy of the six side states (in {@link Direction#values()} order); an old pipe gets them from its one output (migration, ROHR33). */
    public int[] sideModes() {
        if (this.sides == null) this.sides = Pipes.fromSingleOutput(output());
        return this.sides.clone();
    }

    public int sideMode(Direction side) {
        return sideModes()[side.ordinal()];
    }

    /** Sets all six states; the block state follows (the arrow shows the first output, the arms follow the states). */
    public void setSideModes(int[] modes) {
        int[] fixed = new int[6];
        for (Direction d : Direction.values()) {
            int m = Math.floorMod(modes[d.ordinal()], 3);
            fixed[d.ordinal()] = m == Pipes.OUT && !Pipes.allowed(tier(), d) ? Pipes.OFF : m;
        }
        this.sides = fixed;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) ItemPipeBlock.refresh(this.level, this.worldPosition, getBlockState(), fixed);
    }

    /** One click on a side: in - out - off - in. */
    public void cycleSide(Direction side) {
        int[] modes = sideModes();
        modes[side.ordinal()] = Pipes.nextMode(tier(), side, modes[side.ordinal()]);
        setSideModes(modes);
    }

    /** The legacy "turn the output" way: exactly this side is the output, the others take in (sides that are off stay off). */
    public void setOnlyOutput(Direction out) {
        int[] modes = sideModes();
        for (int i = 0; i < 6; i++) modes[i] = i == out.ordinal() ? Pipes.OUT : modes[i] == Pipes.OUT ? Pipes.IN : modes[i];
        setSideModes(modes);
    }

    public int filterMode() {
        return this.filterMode;
    }

    /** The mode that counts: the first step has no filter (it is a plain conveyor), from Reinforced on the key works. */
    public int mode() {
        return tier().canFilter() ? this.filterMode : 0;
    }

    public ContainerData data() {
        return this.data;
    }

    /** The filter key: off - exact - same kind - off. */
    public void cycleFilterMode() {
        if (!tier().canFilter()) return;
        this.filterMode = ItemFilterRules.next(this.filterMode);
        setChanged();
    }

    /** Whether the player in the menu may put {@code stack} into the slot (an empty slot takes anything: that sets the filter). */
    public boolean mayPlayerPlace(ItemStack stack) {
        return ItemFilterRules.mayPlayerPlace(mode(), this.items.get(0), stack);
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

    /** One transfer step: the next output (round robin, ROHR33) that takes something; true if something moved. */
    public boolean push(Level level, BlockPos pos) {
        ItemStack held = this.items.get(0);
        int count = Math.min(tier().amount(), ItemFilterRules.movable(mode(), held));
        if (held.isEmpty() || count <= 0) return false;
        int[] modes = sideModes();
        Direction[] all = Direction.values();
        for (int i = 0; i < all.length; i++) {
            int index = (this.pushTo + i) % all.length;
            if (modes[index] != Pipes.OUT) continue;
            Direction out = all[index];
            Container target = HopperBlockEntity.getContainerAt(level, pos.relative(out));
            if (target == null) continue;
            ItemStack rest = HopperBlockEntity.addItem(this, target, held.copyWithCount(count), out.getOpposite());
            int moved = count - rest.getCount();
            if (moved <= 0) continue;
            held.shrink(moved);
            if (held.isEmpty()) this.items.set(0, ItemStack.EMPTY);
            this.pushTo = (index + 1) % all.length;
            return true;
        }
        return false;
    }

    /** One pull step from a container beside the pipe on an input side (not an output, not off); true if something moved. */
    public boolean pull(Level level, BlockPos pos) {
        ItemStack held = this.items.get(0);
        if (!held.isEmpty() && held.getCount() >= Math.min(getMaxStackSize(), held.getMaxStackSize())) return false;
        int[] modes = sideModes();
        Direction[] all = Direction.values();
        for (int i = 0; i < all.length; i++) {
            int index = (this.pullFrom + i) % all.length;
            Direction side = all[index];
            if (modes[index] != Pipes.IN) continue;
            Container source = HopperBlockEntity.getContainerAt(level, pos.relative(side));
            if (source == null || source instanceof ItemPipeBlockEntity) continue;
            if (takeFrom(source, side.getOpposite())) {
                this.pullFrom = (index + 1) % all.length;
                return true;
            }
        }
        return false;
    }

    /**
     * One pull from {@code source} through {@code face}. Like a Vanilla hopper it asks {@link Container#canTakeItem} before every
     * single item (audit b-audsb31-a, simplestorage-P2-1): a filter hopper keeps its fixed item, an autonomous crafter its grid.
     * Public for the game tests.
     */
    public boolean takeFrom(Container source, Direction face) {
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
            if (!ItemFilterRules.accepts(mode(), held, candidate)) continue;
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, candidate)) continue;
            int limit = Math.min(getMaxStackSize(), candidate.getMaxStackSize());
            int room = limit - (held.isEmpty() ? 0 : held.getCount());
            int count = Math.min(tier().amount(), Math.min(room, candidate.getCount()));
            if (count <= 0) continue;
            // One by one (at most tier().amount() = 4): the source decides again after every item, as it does for a hopper.
            ItemStack taken = ItemStack.EMPTY;
            for (int i = 0; i < count && source.canTakeItem(this, slot, source.getItem(slot)); i++) {
                ItemStack one = source.removeItem(slot, 1);
                if (one.isEmpty()) break;
                if (taken.isEmpty()) taken = one;
                else taken.grow(one.getCount());
            }
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
        return slot == 0 && ItemFilterRules.accepts(mode(), this.items.get(0), stack);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return (direction == null || sideMode(direction) != Pipes.OFF) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return sideMode(direction) != Pipes.OFF && ItemFilterRules.movable(mode(), this.items.get(0)) > 0;
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.filterMode = ItemFilterRules.clamp(input.getIntOr("FilterMode", 0));
        this.pullFrom = Math.floorMod(input.getIntOr("PullFrom", 0), 6);
        this.pushTo = Math.floorMod(input.getIntOr("PushTo", 0), 6);
        // Old worlds have no "Sides": the one output of the block state becomes the new states (ROHR33 migration).
        this.sides = input.getInt("Sides").map(Pipes::unpack).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("FilterMode", this.filterMode);
        output.putInt("PullFrom", this.pullFrom);
        output.putInt("PushTo", this.pushTo);
        output.putInt("Sides", Pipes.pack(sideModes()));
    }
}
