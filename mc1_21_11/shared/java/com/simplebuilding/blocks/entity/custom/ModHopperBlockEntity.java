package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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

public class ModHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {

    private NonNullList<ItemStack> inventory;
    // Ghost Items und Filter Modes
    private final NonNullList<ItemStack> ghostItems = NonNullList.withSize(5, ItemStack.EMPTY);

    // Globaler Filter Modus (Passend zu deinem einen Button in der GUI)
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
                    currentFilterMode = HopperFilterMode.values()[value % HopperFilterMode.values().length];
                    setChanged();
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    // --- Filter & Ghost Logic ---

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Wenn Filter aus ist, alles erlauben
        if (currentFilterMode == HopperFilterMode.NONE) {
            return true;
        }

        // Slot-spezifische Prüfung
        if (slot >= 0 && slot < 5) {
            ItemStack ghost = ghostItems.get(slot);

            // Wenn Ghost Item leer ist, darf in diesem Modus nichts rein
            if (ghost.isEmpty()) {
                return false;
            }

            if (currentFilterMode == HopperFilterMode.WHITELIST) {
                // Exakter Match
                return ItemStack.isSameItemSameComponents(stack, ghost);
            } else if (currentFilterMode == HopperFilterMode.TYPE) {
                // Nur Item Typ
                return stack.is(ghost.getItem());
            }
        }

        return true;
    }

    // Wird vom Packet aufgerufen (Button Klick)
    public void toggleFilterMode() {
        this.currentFilterMode = this.currentFilterMode.next();
        updateListeners();
    }

    private void setGhostItemInternal(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            if (stack.isEmpty()) {
                ghostItems.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                ghostItems.set(slot, copy);
            }
            setChanged();
        }
    }

    // Diese Methode sorgt dafür, dass das GUI sofort aktualisiert wird
    private void updateListeners() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }



    public ItemStack getGhostItem(int slot) {
        if (slot >= 0 && slot < 5) {
            return ghostItems.get(slot);
        }
        return ItemStack.EMPTY;
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

        // KORRIGIERT: Ghost Items lesen
        view.child("GhostItems").ifPresent(ghostView -> {
            ContainerHelper.loadAllItems(ghostView, this.ghostItems);
        });

        // Filter Mode lesen
        this.currentFilterMode = HopperFilterMode.values()[view.getIntOr("FilterMode", 0)];

        this.transferCooldown = view.getIntOr("TransferCooldown", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.trySaveLootTable(view)) {
            ContainerHelper.saveAllItems(view, this.inventory);
        }

        ValueOutput ghostView = view.child("GhostItems"); // Achtung: Hängt von deiner Implementation von WriteView ab
        // Falls .get() null liefert, müsste man ggf. view.put(...) nutzen.
        // Ich übernehme hier deine Logik, aber idealerweise nutzt man NBT für komplexe Strukturen.
        // Da du unten toInitialChunkDataNbt hast, scheint das Speichern hier evtl. custom zu sein?
        // Standard Vanilla wäre Inventories.writeNbt(nbt, items).
        if(ghostView != null) {
             ContainerHelper.saveAllItems(ghostView, this.ghostItems);
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

        // Ghost Items serialisieren für Client
        CompoundTag ghostRoot = new CompoundTag();
        ListTag ghostList = new ListTag();
        for (int i = 0; i < ghostItems.size(); i++) {
            ItemStack stack = ghostItems.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                try {
                    // Item encode via Codec für Client
                    Tag stackTag = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow();
                    if (stackTag instanceof CompoundTag stackCompound) {
                        itemTag.merge(stackCompound);
                    }
                    ghostList.add(itemTag);
                } catch (Exception ignored) { }
            }
        }
        // Inventories.readData sucht nach Key "Items"
        ghostRoot.put("Items", ghostList);
        nbt.put("GhostItems", ghostRoot);

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

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        // Automatisches Setzen des Ghost Items beim Einlegen (Optional, wie du es wolltest)
        if (!stack.isEmpty() && currentFilterMode != HopperFilterMode.NONE) {
             // Nur setzen wenn leer? Oder immer überschreiben?
             // Hier einfach mal checken ob slot leer ist:
             if(ghostItems.get(slot).isEmpty()) {
                ItemStack ghost = stack.copy();
                ghost.setCount(1);
                ghostItems.set(slot, ghost);
                setChanged(); // Sync Nötig? Eigentlich nur Server-Side Logic
             }
        }
    }

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
                if (block == ModBlocks.NETHERITE_HOPPER) {
                    speed = 2;
                } else if (block == ModBlocks.REINFORCED_HOPPER) {
                    speed = 4;
                }

                blockEntity.setTransferCooldown(speed);
                setChanged(world, pos, state);
                return true;
            }
        }
        return false;
    }

    private static boolean insert(Level world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        Container inventory = getOutputInventory(world, pos, blockEntity);
        if (inventory == null) return false;

        Direction direction = stateToFacing(blockEntity.getBlockState()).getOpposite();
        if (isInventoryFull(inventory, direction)) return false;

        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);
            if (!itemStack.isEmpty()) {
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
            if (itemStack.getCount() < itemStack.getMaxStackSize()) return false;
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

    // Diese Methode wird vom Server aufgerufen
    public void setGhostItem(int slot, ItemStack stack) {
        setGhostItemInternal(slot, stack);

        // WICHTIG: Sende Update an alle Spieler, die zuschauen (Tracking)
        if (level != null && !level.isClientSide()) {
            PlatformServices.broadcastHopperGhostItem(this, slot, stack);
        }
        setChanged();
    }

    // Neue Methode nur für den Client (um Endlosschleifen zu vermeiden)
    public void setGhostItemClient(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            if (stack.isEmpty()) {
                ghostItems.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                ghostItems.set(slot, copy);
            }
        }
    }
}