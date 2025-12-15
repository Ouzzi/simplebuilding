package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.BlastFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ModBlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public ModBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_BLAST_FURNACE_BE, pos, state, RecipeType.BLASTING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.simplebuilding.reinforced_blast_furnace");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new BlastFurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ModBlastFurnaceBlockEntity blockEntity) {
        // 1. Vanilla Tick ausführen (Macht +1 Fortschritt)
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);

        // 2. Speed Boost
        PropertyDelegate data = blockEntity.propertyDelegate;
        int cookTime = data.get(2); // Fortschritt
        int totalTime = data.get(3); // Ziel (z.B. 100)
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            // Anpassung: Kleinere Werte sind flüssiger als "10 alle X Ticks"
            if (state.isOf(ModBlocks.NETHERITE_BLAST_FURNACE)) {
                // Extrem schnell: +3 extra pro Tick (insgesamt 4x Speed)
                extraTicks = 3;
            } else if (state.isOf(ModBlocks.REINFORCED_BLAST_FURNACE)) {
                // Schnell: +1 extra pro Tick (insgesamt 2x Speed)
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