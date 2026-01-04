package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ModFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public ModFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_FURNACE_BE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Text getContainerName() {
        // Du kannst hier unterscheiden oder einen generischen Key nutzen
        return Text.translatable("container.simplebuilding.reinforced_furnace");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ModFurnaceBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);

        PropertyDelegate data = blockEntity.propertyDelegate;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            if (state.isOf(ModBlocks.NETHERITE_FURNACE)) {
                extraTicks = 3;
            } else if (state.isOf(ModBlocks.REINFORCED_FURNACE)) {
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