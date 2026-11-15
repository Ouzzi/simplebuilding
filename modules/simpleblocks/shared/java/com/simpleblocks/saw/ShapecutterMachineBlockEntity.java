package com.simpleblocks.saw;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.jspecify.annotations.Nullable;

/**
 * Inside of the Pulse and Auto Shapecutter (N62, {@link ShapecutterMachineBlock}): one input slot and the chosen cut, kept as the
 * recipe id of a {@link Sawing.Entry} (not as an index), so the choice stays when the slot runs empty and a hopper refills it.
 *
 * <p>One cut uses one input item and makes the entry's result. It goes into the container in front ({@code FACING}); the Pulse
 * Shapecutter throws what does not fit out of the front like the crafter, the Auto Shapecutter only cuts when a container is in
 * front and the whole result fits (no items piling up in the world, like the Auto Crafter). Automation fills the input from every
 * side but the front and can take nothing out (the hopper in front must not suck the input away).
 */
public class ShapecutterMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SIZE = 1;
    /** Ticks between two cuts of the Auto Shapecutter: a Vanilla hopper's transfer time, so one hopper keeps it busy. */
    public static final int AUTO_INTERVAL_TICKS = 8;
    /** Container data: 0 = index of the chosen cut in the cuts for the current input (-1 none). */
    public static final int DATA_SELECTED = 0, DATA_COUNT = 1;
    private static final int[] SLOTS = {0};
    private static final int[] NONE = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private @Nullable Identifier selected;
    private int cooldown;
    private List<Sawing.Entry> cutsSource = List.of();
    private ItemStack cutsFor = ItemStack.EMPTY;
    private List<Sawing.Entry> cuts = List.of();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return id == DATA_SELECTED ? selectedIndex() : 0;
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ShapecutterMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Sawing.MACHINE_ENTITY, pos, state);
    }

    // ------------------------------------------------------------------ cutting

    /** Ticker of the Auto Shapecutter: one attempt every {@link #AUTO_INTERVAL_TICKS} ticks while no signal reaches it. */
    public static void autoTick(Level level, BlockPos pos, BlockState state, ShapecutterMachineBlockEntity machine) {
        if (machine.cooldown > 0) {
            machine.cooldown--;
            return;
        }
        machine.cooldown = AUTO_INTERVAL_TICKS;
        if (level instanceof ServerLevel server && !machine.isTriggered()) machine.cut(server, true);
    }

    /** Whether a redstone signal reaches the block (world state, as the Auto Crafter: the cached state can lag one step). */
    public boolean isTriggered() {
        BlockState state = this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
        return state.hasProperty(ShapecutterMachineBlock.TRIGGERED) && state.getValue(ShapecutterMachineBlock.TRIGGERED);
    }

    private Direction front() {
        BlockState state = this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
        return state.hasProperty(ShapecutterMachineBlock.FACING) ? state.getValue(ShapecutterMachineBlock.FACING) : Direction.NORTH;
    }

    /** The container right in front, or null. */
    public @Nullable Container containerInFront() {
        return this.level == null ? null : HopperBlockEntity.getContainerAt(this.level, this.worldPosition.relative(front()));
    }

    /**
     * One cut: the chosen cut of the current input, one input item used. {@code mustFit} (Auto): only into a container in front that
     * takes the whole result; otherwise (Pulse) the rest flies out of the front. Returns whether it cut.
     */
    public boolean cut(ServerLevel level, boolean mustFit) {
        ItemStack input = this.items.get(0);
        Sawing.Entry cut = selectedCut();
        if (input.isEmpty() || cut == null) return false;
        ItemStack result = cut.resultStack();
        if (result.isEmpty()) return false;
        Direction front = front();
        Container into = containerInFront();
        if (mustFit && (into == null || !fits(into, result, front.getOpposite()))) return false;
        input.shrink(1);
        setChanged();
        result.onCraftedBySystem(level);
        ItemStack left = into != null ? HopperBlockEntity.addItem(this, into, result, front.getOpposite()) : result;
        if (into != null) into.setChanged();
        if (!left.isEmpty()) eject(level, left, front);
        level.playSound(null, this.worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, mustFit ? 0.35F : 0.8F, 1.0F);
        return true;
    }

    /** Throws the stack out of the front face, slowly, like the crafter's ejection. */
    private void eject(ServerLevel level, ItemStack stack, Direction front) {
        double x = this.worldPosition.getX() + 0.5 + front.getStepX() * 0.7;
        double y = this.worldPosition.getY() + 0.5 - 0.15;
        double z = this.worldPosition.getZ() + 0.5 + front.getStepZ() * 0.7;
        ItemEntity item = new ItemEntity(level, x, y, z, stack);
        item.setDeltaMovement(front.getStepX() * 0.1, 0.05, front.getStepZ() * 0.1);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** Whether {@code stack} fits completely into {@code into} through {@code face} (the Auto Crafter's check). */
    private static boolean fits(Container into, ItemStack stack, Direction face) {
        int room = 0;
        int[] slots = into instanceof WorldlyContainer sided ? sided.getSlotsForFace(face) : IntStream.range(0, into.getContainerSize()).toArray();
        for (int slot : slots) {
            if (!into.canPlaceItem(slot, stack) || (into instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, stack, face))) continue;
            ItemStack held = into.getItem(slot);
            int max = Math.min(into.getMaxStackSize(stack), stack.getMaxStackSize());
            if (held.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(held, stack)) room += Math.max(0, max - held.getCount());
            if (room >= stack.getCount()) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ the chosen cut

    /** The cuts for the current input (the Shapecutter's list, same order as the screen), cached per item and recipe list. */
    public List<Sawing.Entry> cuts() {
        if (this.level == null) return List.of();
        List<Sawing.Entry> all = Sawing.entries(this.level);
        ItemStack now = this.items.get(0);
        if (all != this.cutsSource || !now.is(this.cutsFor.getItem()) || now.isEmpty() != this.cutsFor.isEmpty()) {
            this.cutsSource = all;
            this.cutsFor = now.copyWithCount(1);
            this.cuts = Sawing.forInput(all, now);
        }
        return this.cuts;
    }

    public @Nullable Identifier selected() {
        return this.selected;
    }

    /** Chooses the cut by its recipe id (null = none); the menu's button and the GameTests call this. */
    public void select(@Nullable Identifier id) {
        this.selected = id;
        setChanged();
    }

    /** The chosen cut if the current input has it, else null. */
    public Sawing.@Nullable Entry selectedCut() {
        if (this.selected == null) return null;
        for (Sawing.Entry e : cuts()) {
            if (e.id().equals(this.selected)) return e;
        }
        return null;
    }

    /** Index of the chosen cut in {@link #cuts()}, -1 when the current input does not have it. */
    public int selectedIndex() {
        if (this.selected == null) return -1;
        List<Sawing.Entry> list = cuts();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(this.selected)) return i;
        }
        return -1;
    }

    /** What the next cut makes (the menu's preview slot), empty when nothing would be cut. */
    public ItemStack previewResult() {
        Sawing.Entry cut = this.items.get(0).isEmpty() ? null : selectedCut();
        return cut == null ? ItemStack.EMPTY : cut.resultStack();
    }

    public ContainerData data() {
        return this.data;
    }

    /** Comparator: the input slot's fill, 0-15 like any container. */
    public int redstoneSignal() {
        ItemStack stack = this.items.get(0);
        if (stack.isEmpty()) return 0;
        return 1 + (int) (14.0F * stack.getCount() / Math.min(getMaxStackSize(stack), stack.getMaxStackSize()));
    }

    // ------------------------------------------------------------------ automation

    /** Only items the saw can cut. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && this.level != null && Sawing.accepts(this.level, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == front() ? NONE : SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != front() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    // ------------------------------------------------------------------ container plumbing

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ShapecutterMachineMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        String chosen = input.getStringOr("Selected", "");
        this.selected = chosen.isEmpty() ? null : Identifier.tryParse(chosen);
        this.cooldown = input.getIntOr("cooldown", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        if (this.selected != null) output.putString("Selected", this.selected.toString());
        output.putInt("cooldown", this.cooldown);
    }
}
