package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.custom.ModChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView; // WICHTIGER IMPORT (aus deinem Log)
import net.minecraft.storage.WriteView; // WICHTIGER IMPORT
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class ModChestBlockEntity extends ChestBlockEntity {

    private final DefaultedList<ItemStack> inventory;

    public ModChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_CHEST_BE, pos, state);
        this.inventory = DefaultedList.ofSize(54, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    protected Text getContainerName() {
        if (this.getCachedState().getBlock() instanceof ModChestBlock) {
            return Text.translatable("container.simplebuilding.reinforced_chest");
        }
        return super.getContainerName();
    }

    // FIX: Methodennamen an deine Umgebung angepasst (readData statt readNbt)
    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
    }

    // FIX: Methodennamen an deine Umgebung angepasst (writeData statt writeNbt)
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
    }

    // Wichtig: Inventar-Zugriff überschreiben, damit das 54er Inventar genutzt wird
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
}