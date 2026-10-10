package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.ItemFilter;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * The mod hoppers (reinforced, netherite, enderite). With a filter on, the owner's filter principle applies
 * ({@link ItemFilter}, docs/ai/PRINZIPIEN-FILTER.md): the real item lying in a slot is that slot's filter - there are no
 * ghost items any more (until 2026-10-09). Automation only tops up slots that hold a matching item, and one item always
 * stays behind: this hopper pushes, and others pull, only the second and every further one.
 */
public class ModHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper, WorldlyContainer, com.simplebuilding.util.FilterHopper {

    private static final int[] SLOTS = {0, 1, 2, 3, 4};

    private NonNullList<ItemStack> inventory;

    // Globaler Filter Modus (die eine Filter-Taste in der GUI)
    private HopperFilterMode currentFilterMode = HopperFilterMode.NONE;

    private int transferCooldown = -1;
    @SuppressWarnings("unused")
    private long lastTickTime;

    // Für Synchronisation mit ScreenHandler
    protected final ContainerData propertyDelegate;

    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];

    public ModHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_HOPPER_BE, pos, state);
        this.inventory = NonNullList.withSize(5, ItemStack.EMPTY);

        // Delegate initialisieren
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? currentFilterMode.ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    // floorMod statt %: Javas Rest bleibt bei negativen Werten negativ und hätte
                    // den Ordinal aus dem Array laufen lassen. Der Umlauf nach oben bleibt.
                    currentFilterMode = HopperFilterMode.values()[
                            Math.floorMod(value, HopperFilterMode.values().length)];
                    setChanged();
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    // --- Filter (Prinzip: das echte Item im Slot ist der Filter) ---

    /** Automation (hoppers, pipes, droppers): with a filter on only onto a slot that already holds a match. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 5 && ItemFilter.accepts(currentFilterMode, getItem(slot), stack);
    }

    /**
     * A player in the menu: an empty slot takes anything (that sets its filter), a filled one only a match. Without a
     * filter every slot takes anything.
     */
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

    // Wird vom Packet aufgerufen (Button Klick)
    public void toggleFilterMode() {
        this.currentFilterMode = this.currentFilterMode.next();
        updateListeners();
    }

    // Diese Methode sorgt dafür, dass das GUI sofort aktualisiert wird
    private void updateListeners() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }



    public HopperFilterMode getFilterMode() {
        return this.currentFilterMode;
    }

    public ContainerData getPropertyDelegate() {
        return this.propertyDelegate;
    }

    // --- READ / WRITE DATA ---

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(view)) {
            ContainerHelper.loadAllItems(view, this.inventory);
        }

        // "GhostItems" of saves before 2026-10-09 are ignored on purpose: the filter is the real item now, and none
        // can be made up from a ghost. Such slots stay empty and filter nothing until a player puts an item in.

        // Filter Mode lesen. Der Ordinal kommt aus der Regionsdatei bzw. vom Server und kann
        // alles sein; ein unbekannter Modus fällt auf NONE zurück, statt beim Chunkladen die
        // ganze Blockentität (Inhalt, Filter, Modus) mitzunehmen.
        int savedFilterMode = view.getIntOr("FilterMode", 0);
        this.currentFilterMode = savedFilterMode >= 0 && savedFilterMode < HopperFilterMode.values().length
                ? HopperFilterMode.values()[savedFilterMode]
                : HopperFilterMode.NONE;

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

    // --- Netzwerk Sync (KORRIGIERT) ---

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        // Wir nutzen super implementation als Basis
        CompoundTag nbt = super.getUpdateTag(registryLookup);

        nbt.putInt("FilterMode", currentFilterMode.ordinal());
        return nbt;
    }

    // --- Vanilla Logik ---


    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return createScreenMenu(syncId, playerInventory);
    }

    public AbstractContainerMenu createScreenMenu(int syncId, Inventory playerInventory) {
        // Hier wird der Server-Konstruktor aufgerufen
        return new NetheriteHopperScreenHandler(syncId, playerInventory, this, this);
    }


    @Override
    public boolean isGridAligned() {
        return false;
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hopper"); }

    @Override
    protected NonNullList<ItemStack> getItems() { return inventory; }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) { this.inventory = inventory; }

    @Override
    public int getContainerSize() { return 5; }

    @Override
    public double getLevelX() { return (double)this.worldPosition.getX() + 0.5D; }

    @Override
    public double getLevelY() { return (double)this.worldPosition.getY() + 0.5D; }

    @Override
    public double getLevelZ() { return (double)this.worldPosition.getZ() + 0.5D; }

    public static void serverTick(Level world, BlockPos pos, BlockState state, ModHopperBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getGameTime();
        if (!blockEntity.needsCooldown()) {
            blockEntity.setTransferCooldown(0);
            insertAndExtract(world, pos, state, blockEntity, () -> HopperBlockEntity.suckInItems(world, blockEntity));
        }
    }

    private static boolean insertAndExtract(Level world, BlockPos pos, BlockState state, ModHopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClientSide()) return false;

        if (!blockEntity.needsCooldown() && state.getValue(HopperBlock.ENABLED)) {
            boolean bl = false;
            if (!blockEntity.isEmpty()) {
                bl = insert(world, pos, blockEntity);
            }
            if (!blockEntity.isFull()) {
                bl |= booleanSupplier.getAsBoolean();
            }
            if (bl) {
                int speed = 8;
                Block block = state.getBlock();
                // Tempo je Stufe aus server.machines (Standard 2x/4x/8x = 4/2/1 Ticks).
                if (block == ModBlocks.NETHERITE_HOPPER) {
                    speed = com.simplebuilding.config.ServerTuning.hopperCooldown(2);
                } else if (block == ModBlocks.REINFORCED_HOPPER) {
                    speed = com.simplebuilding.config.ServerTuning.hopperCooldown(1);
                } else if (block == ModBlocks.ENDERITE_HOPPER) {
                    // Enderit-Stufe: standardmaessig jeden Tick ein Transfer.
                    speed = com.simplebuilding.config.ServerTuning.hopperCooldown(3);
                }

                blockEntity.setTransferCooldown(speed);
                setChanged(world, pos, state);
                return true;
            }
        }
        return false;
    }

    private static boolean insert(Level world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        if (!com.simplebuilding.api.WorldPermissions.mayAutomate(world, pos, pos.relative(stateToFacing(blockEntity.getBlockState())))) return false;
        Container inventory = getOutputInventory(world, pos, blockEntity);
        if (inventory == null) return insertThroughItemAutomation(world, pos, blockEntity);

        Direction direction = stateToFacing(blockEntity.getBlockState()).getOpposite();
        if (isInventoryFull(inventory, direction)) return false;

        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);
            // Filter on: the last item of a slot is its filter and stays.
            if (ItemFilter.movable(blockEntity.currentFilterMode, itemStack) > 0) {
                int count = itemStack.getCount();
                ItemStack itemStack2 = HopperBlockEntity.addItem(blockEntity, inventory, blockEntity.removeItem(i, 1), direction);
                if (itemStack2.isEmpty()) {
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
     * Kein Vanilla-{@code Container} vorn: dann die Transfer-Schnittstelle des Loaders
     * ({@link PlatformServices#itemAutomation} - NeoForge {@code Capabilities.Item.BLOCK}, Fabric
     * {@code ItemStorage.SIDED}, Forge {@code ITEM_HANDLER}), wie Vanillas Trichter auf NeoForge und
     * Fabric es auch tut. Bis 2026-09-27 schoben die Mod-Trichter nur in Vanilla-Container, Maschinen
     * anderer Mods ohne {@code Container} blieben leer (Audit #36). Ein Item je Transfer, wie oben.
     */
    private static boolean insertThroughItemAutomation(Level world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server) || !PlatformServices.hasItemAutomation()) {
            return false;
        }
        Direction facing = stateToFacing(blockEntity.getBlockState());
        BlockPos target = pos.relative(facing);
        if (!server.isLoaded(target)) {
            return false;
        }
        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);
            if (ItemFilter.movable(blockEntity.currentFilterMode, itemStack) < 1) {
                continue;
            }
            int moved = PlatformServices.itemAutomation().insert(server, target, facing.getOpposite(), itemStack.copyWithCount(1));
            if (moved == com.simplebuilding.platform.ItemAutomation.NO_HANDLER) {
                return false;
            }
            if (moved > 0) {
                blockEntity.removeItem(i, 1);
                return true;
            }
        }
        return false;
    }

    private static @Nullable Container getOutputInventory(Level world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        return HopperBlockEntity.getContainerAt(world, pos.relative(stateToFacing(blockEntity.getBlockState())));
    }

    private static Direction stateToFacing(BlockState state) {
        return state.getValue(HopperBlock.FACING);
    }

    private boolean isFull() {
        for (ItemStack itemStack : this.inventory) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxStackSize()) return false;
        }
        return true;
    }

    private static boolean isInventoryFull(Container inventory, Direction direction) {
        int[] slots = getAvailableSlots(inventory, direction);
        for (int i : slots) {
            ItemStack itemStack = inventory.getItem(i);
            // Mod-Truhen ab Netherit fassen x2/x4 je Platz (siehe HopperBlockEntityMixin).
            if (itemStack.getCount() < com.simplebuilding.util.TieredChests.maxStackSize(inventory, itemStack, itemStack.getMaxStackSize())) return false;
        }
        return true;
    }

    private static int[] getAvailableSlots(Container inventory, Direction side) {
        if (inventory instanceof WorldlyContainer sided) return sided.getSlotsForFace(side);
        int i = inventory.getContainerSize();
        if (i < AVAILABLE_SLOTS_CACHE.length) {
            int[] cache = AVAILABLE_SLOTS_CACHE[i];
            if (cache != null) return cache;
            int[] created = indexArray(i);
            AVAILABLE_SLOTS_CACHE[i] = created;
            return created;
        }
        return indexArray(i);
    }

    private static int[] indexArray(int size) {
        int[] is = new int[size];
        for(int i = 0; i < is.length; is[i] = i++);
        return is;
    }

    private void setTransferCooldown(int transferCooldown) { this.transferCooldown = transferCooldown; }
    private boolean needsCooldown() { return this.transferCooldown > 0; }
}
