package com.simplemachines.hopper;

import com.simplemachines.config.MachinesConfig;
import com.simplemachines.upgrade.PistonGuard;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The mod hoppers' block entity (SimpleBuilding's {@code ModHopperBlockEntity}, stage 3; saved data unchanged: {@code Items},
 * {@code FilterMode}, {@code TransferCooldown}, so a hopper of an old world keeps contents and filter after the id move). With a
 * filter on, the owner's filter principle applies ({@link ItemFilter}, docs/ai/PRINZIPIEN-FILTER.md): the real item lying in a
 * slot is that slot's filter. Automation only tops up slots that hold a matching item, and one item always stays behind: this
 * hopper pushes, and others pull, only the second and every further one.
 */
public class TierHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper, WorldlyContainer, FilterHopper {
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];

    private NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    /** The one filter key of the menu. */
    private HopperFilterMode currentFilterMode = HopperFilterMode.NONE;
    private int transferCooldown = -1;

    /** Synced to the menu: slot 0 the filter mode's ordinal. */
    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? currentFilterMode.ordinal() : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                // floorMod: Java's remainder stays negative for negative values.
                currentFilterMode = HopperFilterMode.values()[Math.floorMod(value, HopperFilterMode.values().length)];
                setChanged();
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public TierHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Hoppers.BLOCK_ENTITY, pos, state);
    }

    /** The tier of the block it stands in (Netherite if unknown). */
    public HopperTier tier() {
        return getBlockState().getBlock() instanceof TierHopperBlock block ? block.tier() : HopperTier.NETHERITE;
    }

    // --- filter (principle: the real item in the slot is the filter) ---

    /** Automation (hoppers, pipes, droppers): with a filter on only onto a slot that already holds a match. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 5 && ItemFilter.accepts(currentFilterMode, getItem(slot), stack);
    }

    /** A player in the menu: an empty slot takes anything (that sets its filter), a filled one only a match. */
    @Override
    public boolean mayPlayerPlace(int slot, ItemStack stack) {
        ItemStack held = getItem(slot);
        return currentFilterMode == HopperFilterMode.NONE || held.isEmpty() || ItemFilter.matches(currentFilterMode, held, stack);
    }

    /** Automation may only take what lies above the one filter item. */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return ItemFilter.movable(currentFilterMode, getItem(slot)) > 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return ItemFilter.movable(currentFilterMode, getItem(slot)) > 0;
    }

    /** The filter key (menu button 0). */
    @Override
    public void toggleFilterMode() {
        this.currentFilterMode = this.currentFilterMode.next();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public HopperFilterMode getFilterMode() {
        return this.currentFilterMode;
    }

    @Override
    public ContainerData getPropertyDelegate() {
        return this.propertyDelegate;
    }

    // --- saving ---

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(view)) {
            ContainerHelper.loadAllItems(view, this.inventory);
        }
        // An unknown mode (the ordinal comes from the region file or the server) falls back to NONE instead of losing the block entity.
        this.currentFilterMode = HopperFilterMode.of(view.getIntOr("FilterMode", 0));
        this.transferCooldown = view.getIntOr("TransferCooldown", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.trySaveLootTable(view)) {
            ContainerHelper.saveAllItems(view, this.inventory);
        }
        view.putInt("FilterMode", currentFilterMode.ordinal());
        view.putInt("TransferCooldown", this.transferCooldown);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        CompoundTag nbt = super.getUpdateTag(registryLookup);
        nbt.putInt("FilterMode", currentFilterMode.ordinal());
        return nbt;
    }

    // --- menu ---

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new TierHopperMenu(tier(), syncId, playerInventory, this, this);
    }

    @Override
    public boolean isGridAligned() {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getContainerSize() {
        return 5;
    }

    @Override
    public double getLevelX() {
        return this.worldPosition.getX() + 0.5D;
    }

    @Override
    public double getLevelY() {
        return this.worldPosition.getY() + 0.5D;
    }

    @Override
    public double getLevelZ() {
        return this.worldPosition.getZ() + 0.5D;
    }

    // --- ticking (Vanilla's hopper logic with the tier's cooldown) ---

    public static void serverTick(Level world, BlockPos pos, BlockState state, TierHopperBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        if (!blockEntity.needsCooldown()) {
            blockEntity.transferCooldown = 0;
            insertAndExtract(world, pos, state, blockEntity, () -> HopperBlockEntity.suckInItems(world, blockEntity));
        }
    }

    private static boolean insertAndExtract(Level world, BlockPos pos, BlockState state, TierHopperBlockEntity blockEntity, BooleanSupplier suck) {
        if (world.isClientSide() || blockEntity.needsCooldown() || !state.getValue(HopperBlock.ENABLED)) return false;
        boolean moved = false;
        if (!blockEntity.isEmpty()) {
            moved = insert(world, pos, blockEntity);
        }
        if (!blockEntity.isFull()) {
            moved |= suck.getAsBoolean();
        }
        if (moved) {
            // Speed per tier from hoppers.* (default 2x/4x/8x = 4/2/1 ticks).
            blockEntity.transferCooldown = state.getBlock() instanceof TierHopperBlock block ? MachinesConfig.hopperCooldown(block.tier()) : 8;
            setChanged(world, pos, state);
            return true;
        }
        return false;
    }

    private static boolean insert(Level world, BlockPos pos, TierHopperBlockEntity blockEntity) {
        Direction facing = blockEntity.getBlockState().getValue(HopperBlock.FACING);
        if (!PistonGuard.mayAutomate(world, pos, pos.relative(facing))) return false;
        Container inventory = HopperBlockEntity.getContainerAt(world, pos.relative(facing));
        if (inventory == null) return insertThroughItemAutomation(world, pos, blockEntity);

        Direction direction = facing.getOpposite();
        if (isInventoryFull(inventory, direction)) return false;

        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);
            // Filter on: the last item of a slot is its filter and stays.
            if (ItemFilter.movable(blockEntity.currentFilterMode, itemStack) > 0) {
                int count = itemStack.getCount();
                ItemStack rest = HopperBlockEntity.addItem(blockEntity, inventory, blockEntity.removeItem(i, 1), direction);
                if (rest.isEmpty()) {
                    inventory.setChanged();
                    return true;
                }
                itemStack.setCount(count);
                if (count == 1) blockEntity.setItem(i, itemStack);
            }
        }
        return false;
    }

    /**
     * No Vanilla {@code Container} in front: the loader's transfer interface ({@link HopperAutomation}; Fabric
     * {@code ItemStorage.SIDED}), like Vanilla's hopper on Fabric. One item per transfer, as above.
     */
    private static boolean insertThroughItemAutomation(Level world, BlockPos pos, TierHopperBlockEntity blockEntity) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server) || !HopperAutomation.available()) return false;
        Direction facing = blockEntity.getBlockState().getValue(HopperBlock.FACING);
        BlockPos target = pos.relative(facing);
        if (!server.isLoaded(target)) return false;
        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);
            if (ItemFilter.movable(blockEntity.currentFilterMode, itemStack) < 1) continue;
            int moved = HopperAutomation.insert(server, target, facing.getOpposite(), itemStack.copyWithCount(1));
            if (moved == HopperAutomation.NO_HANDLER) return false;
            if (moved > 0) {
                blockEntity.removeItem(i, 1);
                return true;
            }
        }
        return false;
    }

    private boolean isFull() {
        for (ItemStack itemStack : this.inventory) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxStackSize()) return false;
        }
        return true;
    }

    /** Containers with a raised slot limit (tiered chests of Simple Storage) count by their own {@code getMaxStackSize(stack)}. */
    private static boolean isInventoryFull(Container inventory, Direction direction) {
        for (int i : getAvailableSlots(inventory, direction)) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getCount() < Math.max(itemStack.getMaxStackSize(), inventory.getMaxStackSize(itemStack))) return false;
        }
        return true;
    }

    private static int[] getAvailableSlots(Container inventory, Direction side) {
        if (inventory instanceof WorldlyContainer sided) return sided.getSlotsForFace(side);
        int size = inventory.getContainerSize();
        if (size < AVAILABLE_SLOTS_CACHE.length) {
            int[] cache = AVAILABLE_SLOTS_CACHE[size];
            if (cache != null) return cache;
            int[] created = indexArray(size);
            AVAILABLE_SLOTS_CACHE[size] = created;
            return created;
        }
        return indexArray(size);
    }

    private static int[] indexArray(int size) {
        int[] is = new int[size];
        for (int i = 0; i < is.length; i++) is[i] = i;
        return is;
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }
}
