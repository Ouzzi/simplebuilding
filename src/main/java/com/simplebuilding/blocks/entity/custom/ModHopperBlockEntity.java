package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.HopperFilterMode;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
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

public class ModHopperBlockEntity extends LootableContainerBlockEntity implements Hopper {
    private DefaultedList<ItemStack> inventory;
    // Ghost Items und Filter Modes
    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final HopperFilterMode[] filterModes = new HopperFilterMode[5];

    private int transferCooldown = -1;
    private long lastTickTime;

    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];

    public ModHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_HOPPER_BE, pos, state);
        this.inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
        Arrays.fill(this.filterModes, HopperFilterMode.NONE);
    }

    // --- Filter & Ghost Logic ---

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        HopperFilterMode mode = filterModes[slot];

        if (mode == HopperFilterMode.NONE) {
            return true;
        }

        ItemStack ghost = ghostItems.get(slot);
        if (ghost.isEmpty()) {
            return true;
        }

        if (mode == HopperFilterMode.WHITELIST) {
            return ItemStack.areItemsAndComponentsEqual(stack, ghost);
        } else if (mode == HopperFilterMode.TYPE) {
            return stack.isOf(ghost.getItem());
        }

        return true;
    }



    public void toggleFilterMode(int slot) {
        if (slot >= 0 && slot < 5) {
            filterModes[slot] = filterModes[slot].next();

            // Wenn Filter aktiviert wird und Item drin liegt -> Ghost setzen
            if (filterModes[slot] != HopperFilterMode.NONE && !this.getStack(slot).isEmpty()) {
                setGhostItem(slot, this.getStack(slot));
            }

            markDirty();
        }
    }

    public void setGhostItem(int slot, ItemStack stack) {
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

    public ItemStack getGhostItem(int slot) {
        if (slot >= 0 && slot < 5) {
            return ghostItems.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public HopperFilterMode getFilterMode(int slot) {
        if (slot >= 0 && slot < 5) {
            return filterModes[slot];
        }
        return HopperFilterMode.NONE;
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

        // KORRIGIERT: Filter Modes lesen
        view.getOptionalIntArray("FilterModes").ifPresent(array -> {
            for (int i = 0; i < 5 && i < array.length; i++) {
                int ordinal = array[i];
                if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                    filterModes[i] = HopperFilterMode.values()[ordinal];
                }
            }
        });

        this.transferCooldown = view.getInt("TransferCooldown", -1);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!this.writeLootTable(view)) {
            Inventories.writeData(view, this.inventory);
        }

        // KORRIGIERT: Ghost Items schreiben
        WriteView ghostView = view.get("GhostItems"); // oder create/put wenn nötig, 'get' liefert oft den Sub-Writer
        Inventories.writeData(ghostView, this.ghostItems);

        // KORRIGIERT: Filter Modes schreiben
        int[] modesAsInt = new int[5];
        for (int i = 0; i < 5; i++) {
            modesAsInt[i] = filterModes[i].ordinal();
        }
        view.putIntArray("FilterModes", modesAsInt);

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

        int[] modes = new int[5];
        for (int i = 0; i < 5; i++) modes[i] = filterModes[i].ordinal();
        nbt.putIntArray("FilterModes", modes);

        // Ghost Items manuell serialisieren für Client Sync
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
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new HopperScreenHandler(syncId, playerInventory, this);
    }


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
        if (!stack.isEmpty()) {
            ItemStack ghost = stack.copy();
            ghost.setCount(1);
            ghostItems.set(slot, ghost);
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
}