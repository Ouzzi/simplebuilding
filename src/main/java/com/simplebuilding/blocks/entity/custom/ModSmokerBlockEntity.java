package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ModSmokerBlockEntity extends AbstractFurnaceBlockEntity {

    public ModSmokerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_SMOKER_BE, pos, state, RecipeType.SMOKING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.simplebuilding.reinforced_smoker");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new SmokerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ModSmokerBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);

        PropertyDelegate data = blockEntity.propertyDelegate;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            if (state.isOf(ModBlocks.NETHERITE_SMOKER)) {
                extraTicks = 3;
            } else if (state.isOf(ModBlocks.REINFORCED_SMOKER)) {
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