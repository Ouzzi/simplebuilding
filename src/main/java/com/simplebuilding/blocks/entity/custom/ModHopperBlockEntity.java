package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BooleanSupplier;

// WICHTIG: "implements ExtendedScreenHandlerFactory" hinzufügen!
public class ModHopperBlockEntity extends LootableContainerBlockEntity implements Hopper, ExtendedScreenHandlerFactory {

    private DefaultedList<ItemStack> inventory;
    // Ghost Items und Filter Modes
    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);

    // Globaler Filter Modus (Passend zu deinem einen Button in der GUI)
    private HopperFilterMode currentFilterMode = HopperFilterMode.NONE;

    private int transferCooldown = -1;
    private long lastTickTime;

    // Für Synchronisation mit ScreenHandler
    protected final PropertyDelegate propertyDelegate;

    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];

    public ModHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_HOPPER_BE, pos, state);
        this.inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);

        // Delegate initialisieren
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return index == 0 ? currentFilterMode.ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    currentFilterMode = HopperFilterMode.values()[value % HopperFilterMode.values().length];
                    markDirty();
                }
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }

    // --- Filter & Ghost Logic ---

    @Override
    public boolean isValid(int slot, ItemStack stack) {
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
                return ItemStack.areItemsAndComponentsEqual(stack, ghost);
            } else if (currentFilterMode == HopperFilterMode.TYPE) {
                // Nur Item Typ
                return stack.isOf(ghost.getItem());
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
            markDirty();
        }
    }

    // Diese Methode sorgt dafür, dass das GUI sofort aktualisiert wird
    private void updateListeners() {
        markDirty();
        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
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

    public PropertyDelegate getPropertyDelegate() {
        return this.propertyDelegate;
    }

    // --- READ / WRITE DATA ---

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.readLootTable(view)) {
            Inventories.readData(view, this.inventory);
        }

        // KORRIGIERT: Ghost Items lesen
        view.getOptionalReadView("GhostItems").ifPresent(ghostView -> {
            Inventories.readData(ghostView, this.ghostItems);
        });

        // Filter Mode lesen
        this.currentFilterMode = HopperFilterMode.values()[view.getInt("FilterMode", 0)];

        this.transferCooldown = view.getInt("TransferCooldown", -1);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!this.writeLootTable(view)) {
            Inventories.writeData(view, this.inventory);
        }

        WriteView ghostView = view.get("GhostItems"); // Achtung: Hängt von deiner Implementation von WriteView ab
        // Falls .get() null liefert, müsste man ggf. view.put(...) nutzen.
        // Ich übernehme hier deine Logik, aber idealerweise nutzt man NBT für komplexe Strukturen.
        // Da du unten toInitialChunkDataNbt hast, scheint das Speichern hier evtl. custom zu sein?
        // Standard Vanilla wäre Inventories.writeNbt(nbt, items).
        if(ghostView != null) {
             Inventories.writeData(ghostView, this.ghostItems);
        }

        view.putInt("FilterMode", currentFilterMode.ordinal());
        view.putInt("TransferCooldown", this.transferCooldown);
    }

    // --- Netzwerk Sync (KORRIGIERT) ---

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        // Wir nutzen super implementation als Basis
        NbtCompound nbt = super.toInitialChunkDataNbt(registryLookup);

        nbt.putInt("FilterMode", currentFilterMode.ordinal());

        // Ghost Items serialisieren für Client
        NbtCompound ghostRoot = new NbtCompound();
        NbtList ghostList = new NbtList();
        for (int i = 0; i < ghostItems.size(); i++) {
            ItemStack stack = ghostItems.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte)i);
                try {
                    // Item encode via Codec für Client
                    NbtElement stackTag = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow();
                    if (stackTag instanceof NbtCompound stackCompound) {
                        itemTag.copyFrom(stackCompound);
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
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        // Hier wird der Server-Konstruktor aufgerufen
        return new NetheriteHopperScreenHandler(syncId, playerInventory, this, this);
    }


    @Override
    public boolean canBlockFromAbove() {
        return false;
    }

    @Override
    protected Text getContainerName() { return Text.translatable("container.hopper"); }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() { return inventory; }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) { this.inventory = inventory; }

    @Override
    public int size() { return 5; }

    @Override
    public double getHopperX() { return (double)this.pos.getX() + 0.5D; }

    @Override
    public double getHopperY() { return (double)this.pos.getY() + 0.5D; }

    @Override
    public double getHopperZ() { return (double)this.pos.getZ() + 0.5D; }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
        // Automatisches Setzen des Ghost Items beim Einlegen (Optional, wie du es wolltest)
        if (!stack.isEmpty() && currentFilterMode != HopperFilterMode.NONE) {
             // Nur setzen wenn leer? Oder immer überschreiben?
             // Hier einfach mal checken ob slot leer ist:
             if(ghostItems.get(slot).isEmpty()) {
                ItemStack ghost = stack.copy();
                ghost.setCount(1);
                ghostItems.set(slot, ghost);
                markDirty(); // Sync Nötig? Eigentlich nur Server-Side Logic
             }
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, ModHopperBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();
        if (!blockEntity.needsCooldown()) {
            blockEntity.setTransferCooldown(0);
            insertAndExtract(world, pos, state, blockEntity, () -> HopperBlockEntity.extract(world, blockEntity));
        }
    }

    private static boolean insertAndExtract(World world, BlockPos pos, BlockState state, ModHopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClient()) return false;

        if (!blockEntity.needsCooldown() && state.get(HopperBlock.ENABLED)) {
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
                markDirty(world, pos, state);
                return true;
            }
        }
        return false;
    }

    private static boolean insert(World world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        Inventory inventory = getOutputInventory(world, pos, blockEntity);
        if (inventory == null) return false;

        Direction direction = stateToFacing(blockEntity.getCachedState()).getOpposite();
        if (isInventoryFull(inventory, direction)) return false;

        for (int i = 0; i < blockEntity.size(); ++i) {
            ItemStack itemStack = blockEntity.getStack(i);
            if (!itemStack.isEmpty()) {
                int count = itemStack.getCount();
                ItemStack itemStack2 = HopperBlockEntity.transfer(blockEntity, inventory, blockEntity.removeStack(i, 1), direction);
                if (itemStack2.isEmpty()) {
                    inventory.markDirty();
                    return true;
                }
                itemStack.setCount(count);
                if (count == 1) blockEntity.setStack(i, itemStack);
            }
        }
        return false;
    }

    private static @Nullable Inventory getOutputInventory(World world, BlockPos pos, ModHopperBlockEntity blockEntity) {
        return HopperBlockEntity.getInventoryAt(world, pos.offset(stateToFacing(blockEntity.getCachedState())));
    }

    private static Direction stateToFacing(BlockState state) {
        return state.get(HopperBlock.FACING);
    }

    private boolean isFull() {
        for (ItemStack itemStack : this.inventory) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxCount()) return false;
        }
        return true;
    }

    private static boolean isInventoryFull(Inventory inventory, Direction direction) {
        int[] slots = getAvailableSlots(inventory, direction);
        for (int i : slots) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.getCount() < itemStack.getMaxCount()) return false;
        }
        return true;
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        if (inventory instanceof SidedInventory sided) return sided.getAvailableSlots(side);
        int i = inventory.size();
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
        if (world != null && !world.isClient()) {
            net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(this).forEach(player -> {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.simplebuilding.networking.SyncHopperGhostItemPayload(pos, slot, stack));
            });
        }
        markDirty();
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

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return this.pos;
    }
}