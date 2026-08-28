package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ModFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public ModFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_FURNACE_BE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        // Du kannst hier unterscheiden oder einen generischen Key nutzen
        return Component.translatable("container.simplebuilding.reinforced_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new FurnaceMenu(syncId, playerInventory, this, this.dataAccess);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ModFurnaceBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity);

        ContainerData data = blockEntity.dataAccess;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            if (state.is(ModBlocks.NETHERITE_FURNACE)) {
                extraTicks = 3;
            } else if (state.is(ModBlocks.REINFORCED_FURNACE)) {
                extraTicks = 1;
            }

            if (extraTicks > 0) {
                int newCookTime = cookTime + extraTicks;

                if (newCookTime >= totalTime) {
                    newCookTime = totalTime - 1;
                }

                data.set(2, newCookTime);
            }
        }
    }
}