package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.AutonomousCrafterBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.screen.AutonomousCrafterMenu;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.ItemFilter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Autonomous Crafter (owner 2026-10-09, queue N26): a Vanilla crafter that crafts its set recipe by itself, every
 * {@link #CRAFT_INTERVAL_TICKS} ticks, as long as a hopper stands below it and no redstone signal reaches it. Result and
 * remainders go into that hopper; a result that does not fit completely is not crafted. Slots can be switched off like
 * the crafter's.
 *
 * <p>Filter principle (docs/ai/PRINZIPIEN-FILTER.md), with the filter key on: every filled slot keeps one real item as
 * its recipe item - it crafts only when each filled slot holds two or more, and uses up one of each; hoppers above or at
 * the sides only top up slots that hold a match (exact or same kind). Filter off: like the Vanilla crafter, the last
 * items are used up and insertion spreads like Vanilla's. Nothing can be pulled out by automation (the hopper below
 * must not suck up the ingredients).
 */
public class AutonomousCrafterBlockEntity extends BaseContainerBlockEntity implements CraftingContainer, WorldlyContainer {
    public static final int SIZE = 9;
    /** Ticks between two crafting attempts (the crafter's 4 tick delay). */
    public static final int CRAFT_INTERVAL_TICKS = 4;
    /** How long the "crafting" face stays lit (the crafter's 6 ticks). */
    public static final int CRAFTING_TICKS = 6;
    /** Container data: 0..8 slot disabled (1), 9 triggered (1), 10 filter mode ordinal. */
    public static final int DATA_TRIGGERED = 9, DATA_FILTER = 10, DATA_COUNT = 11;

    private static final int[] SLOTS = IntStream.range(0, SIZE).toArray();
    private static final int[] NONE = new int[0];
    private static final Component DEFAULT_NAME = Component.translatable("container.simplebuilding.autonomous_crafter");

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final boolean[] disabled = new boolean[SIZE];
    private HopperFilterMode filterMode = HopperFilterMode.NONE;
    private int cooldown;
    private int craftingTicksRemaining;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            if (id < SIZE) return disabled[id] ? 1 : 0;
            if (id == DATA_TRIGGERED) return getBlockState().getValue(AutonomousCrafterBlock.TRIGGERED) ? 1 : 0;
            return filterMode.ordinal();
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AutonomousCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTONOMOUS_CRAFTER_BE, pos, state);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutonomousCrafterBlockEntity crafter) {
        if (crafter.craftingTicksRemaining > 0 && --crafter.craftingTicksRemaining == 0 && state.getValue(AutonomousCrafterBlock.CRAFTING)) {
            level.setBlock(pos, state.setValue(AutonomousCrafterBlock.CRAFTING, false), Block.UPDATE_CLIENTS);
        }
        if (crafter.cooldown > 0) {
            crafter.cooldown--;
            return;
        }
        crafter.cooldown = CRAFT_INTERVAL_TICKS;
        if (level instanceof ServerLevel server && crafter.tryCraft(server)) {
            crafter.craftingTicksRemaining = CRAFTING_TICKS;
            BlockState now = level.getBlockState(pos);
            if (now.hasProperty(AutonomousCrafterBlock.CRAFTING) && !now.getValue(AutonomousCrafterBlock.CRAFTING)) {
                level.setBlock(pos, now.setValue(AutonomousCrafterBlock.CRAFTING, true), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The hopper (Vanilla or mod) right below, or null: without it the crafter does not craft. */
    public @Nullable Container hopperBelow() {
        if (this.level == null) return null;
        return this.level.getBlockEntity(this.worldPosition.below()) instanceof Hopper hopper && hopper instanceof Container container
                ? container : null;
    }

    /** Whether a redstone signal reaches the crafter (it then does not craft). */
    public boolean isTriggered() {
        return getBlockState().getValue(AutonomousCrafterBlock.TRIGGERED);
    }

    /** One crafting attempt: hopper below, no signal, a recipe, enough items (filter: two per filled slot), room below. */
    public boolean tryCraft(ServerLevel level) {
        Container into = hopperBelow();
        if (into == null || isTriggered() || isEmpty()) return false;
        if (filterMode != HopperFilterMode.NONE) {
            for (ItemStack stack : this.items) {
                if (!stack.isEmpty() && stack.getCount() < 2) return false;
            }
        }
        CraftingInput input = asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = CrafterBlock.getPotentialResults(level, input);
        if (recipe.isEmpty()) return false;
        ItemStack result = recipe.get().value().assemble(input);
        if (result.isEmpty() || !fits(into, result)) return false;
        List<ItemStack> remainders = recipe.get().value().getRemainingItems(input);

        result.onCraftedBySystem(level);
        HopperBlockEntity.addItem(this, into, result, Direction.UP);
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) stack.shrink(1);
        }
        for (ItemStack rest : remainders) {
            if (rest.isEmpty()) continue;
            ItemStack left = HopperBlockEntity.addItem(this, into, rest.copy(), Direction.UP);
            if (!left.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, left);
            }
        }
        into.setChanged();
        setChanged();
        return true;
    }

    /** Whether {@code stack} fits completely into {@code into} from above (so nothing is crafted that cannot leave). */
    private static boolean fits(Container into, ItemStack stack) {
        int room = 0;
        int[] slots = into instanceof WorldlyContainer sided ? sided.getSlotsForFace(Direction.UP) : IntStream.range(0, into.getContainerSize()).toArray();
        for (int slot : slots) {
            if (!into.canPlaceItem(slot, stack)
                    || (into instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, stack, Direction.UP))) {
                continue;
            }
            ItemStack held = into.getItem(slot);
            int max = Math.min(into.getMaxStackSize(stack), stack.getMaxStackSize());
            if (held.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(held, stack)) room += Math.max(0, max - held.getCount());
            if (room >= stack.getCount()) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ slots, filter

    public boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < SIZE && disabled[slot];
    }

    /** Switches a slot off or on; only an empty slot can be switched off (like the crafter). */
    public void setSlotDisabled(int slot, boolean off) {
        if (slot < 0 || slot >= SIZE || (off && !getItem(slot).isEmpty())) return;
        disabled[slot] = off;
        setChanged();
    }

    public HopperFilterMode filterMode() {
        return filterMode;
    }

    public void cycleFilterMode() {
        filterMode = filterMode.next();
        setChanged();
    }

    public ContainerData data() {
        return data;
    }

    /** Automation: never into a switched off slot; with a filter only onto a match; without one like the Vanilla crafter. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isSlotDisabled(slot)) return false;
        ItemStack held = getItem(slot);
        if (held.getCount() >= held.getMaxStackSize() && !held.isEmpty()) return false;
        if (!ItemFilter.accepts(filterMode, held, stack)) return false;
        if (held.isEmpty()) return true;
        // Spread like Vanilla's crafter: not onto a stack while a later enabled slot that could take it holds fewer.
        for (int i = slot + 1; i < SIZE; i++) {
            if (isSlotDisabled(i)) continue;
            ItemStack other = getItem(i);
            if (filterMode == HopperFilterMode.NONE) {
                if (other.isEmpty() || (other.getCount() < held.getCount() && ItemStack.isSameItemSameComponents(other, held))) return false;
            } else if (!other.isEmpty() && other.getCount() < held.getCount() && ItemFilter.matches(filterMode, other, stack)) {
                return false;
            }
        }
        return true;
    }

    /** A player in the menu: an enabled empty slot takes anything (that sets its recipe item), a filled one a match. */
    public boolean mayPlayerPlace(int slot, ItemStack stack) {
        ItemStack held = getItem(slot);
        return !isSlotDisabled(slot) && (filterMode == HopperFilterMode.NONE || held.isEmpty() || ItemFilter.matches(filterMode, held, stack));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? NONE : SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isSlotDisabled(slot) && !stack.isEmpty()) disabled[slot] = false;
        super.setItem(slot, stack);
    }

    /** Comparator: filled or switched off slots, like the crafter. */
    public int redstoneSignal() {
        int n = 0;
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty() || disabled[i]) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ container plumbing

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutonomousCrafterMenu(containerId, inventory, this, this.data);
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
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public void fillStackedContents(StackedItemContents contents) {
        for (ItemStack stack : this.items) {
            contents.accountSimpleStack(stack);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        java.util.Arrays.fill(this.disabled, false);
        input.getIntArray("disabled_slots").ifPresent(slots -> {
            for (int slot : slots) {
                if (slot >= 0 && slot < SIZE) this.disabled[slot] = true;
            }
        });
        int mode = input.getIntOr("FilterMode", 0);
        this.filterMode = mode >= 0 && mode < HopperFilterMode.values().length ? HopperFilterMode.values()[mode] : HopperFilterMode.NONE;
        this.cooldown = input.getIntOr("cooldown", 0);
        this.craftingTicksRemaining = input.getIntOr("crafting_ticks_remaining", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        IntList off = new IntArrayList();
        for (int i = 0; i < SIZE; i++) {
            if (this.disabled[i]) off.add(i);
        }
        output.putIntArray("disabled_slots", off.toIntArray());
        output.putInt("FilterMode", this.filterMode.ordinal());
        output.putInt("cooldown", this.cooldown);
        output.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);
    }
}
