package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.custom.ModHopperBlock;
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

import java.util.function.BooleanSupplier;

public class ModHopperBlockEntity extends LootableContainerBlockEntity implements Hopper {
    private DefaultedList<ItemStack> inventory;
    private int transferCooldown = -1;
    private long lastTickTime;

    private static final int[][] AVAILABLE_SLOTS_CACHE = new int[54][];

    public ModHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_HOPPER_BE, pos, state);
        this.inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.readLootTable(view)) {
            Inventories.readData(view, this.inventory);
        }
        this.transferCooldown = view.getInt("TransferCooldown", -1);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!this.writeLootTable(view)) {
            Inventories.writeData(view, this.inventory);
        }
        view.putInt("TransferCooldown", this.transferCooldown);
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

                if (block == com.simplebuilding.blocks.ModBlocks.NETHERITE_HOPPER) {speed = 2;
                } else if (block == com.simplebuilding.blocks.ModBlocks.REINFORCED_HOPPER) {speed = 4;
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

    private void setTransferCooldown(int transferCooldown) {
        this.transferCooldown = transferCooldown;
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }
}