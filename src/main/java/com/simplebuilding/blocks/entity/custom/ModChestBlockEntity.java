package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.custom.ModChestBlock;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.logging.Logger;

public class ModChestBlockEntity extends ChestBlockEntity {

    private final DefaultedList<ItemStack> inventory;

    public ModChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_CHEST_BE, pos, state);
        // ZURÜCK AUF 27 SLOTS (Standard Single Chest)
        this.inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return 27; // Standard Größe
    }

    @Override
    protected Text getContainerName() {
        if (this.getCachedState().getBlock() instanceof ModChestBlock) {
            return Text.translatable("container.simplebuilding.reinforced_chest");
        }
        return super.getContainerName();
    }

    // FIX: Stack Limit erhöhen
    @Override
    public int getMaxCountPerStack() {
        BlockState state = this.getCachedState();
        if (state.isOf(ModBlocks.NETHERITE_CHEST)) {
            System.out.println("Getting max count per stack for Netherite Chest");
            return 256; // 4x Stack Size
        } else if (state.isOf(ModBlocks.REINFORCED_CHEST)) {
            System.out.println("Getting max count per stack for Reinforced Chest");
            return 128; // 2x Stack Size
        }
        System.out.println("Getting max count per stack");
        return 64; // Fallback
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory.clear();
        for (int i = 0; i < inventory.size() && i < this.inventory.size(); i++) {
            this.inventory.set(i, inventory.get(i));
        }
    }

    // ZURÜCK AUF STANDARD SCREEN HANDLER (3 Reihen)
    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }
}