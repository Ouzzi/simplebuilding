package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.BlastFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate; // Import PropertyDelegate
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
        // FEHLER 1 BEHOBEN: Der Konstruktor erwartet 4 Argumente (inkl. PropertyDelegate)
        // this.propertyDelegate ist protected in der Superklasse und hier verfügbar.
        return new BlastFurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tickModified(ServerWorld world, BlockPos pos, BlockState state, ModBlastFurnaceBlockEntity blockEntity) {
        // Vanilla Tick ausführen
        AbstractFurnaceBlockEntity.tick(world, pos, state, blockEntity);

        // FEHLER 2 & 3 BEHOBEN: Zugriff über propertyDelegate statt private Felder
        // Index 0 = litTimeRemaining (Brennzeit)
        // Index 2 = cookingTimeSpent (Fortschritt)
        // Index 3 = cookingTotalTime (Zielzeit)

        PropertyDelegate delegate = blockEntity.propertyDelegate;
        boolean isBurning = delegate.get(0) > 0;
        int cookingTime = delegate.get(2);
        int totalTime = delegate.get(3);

        // Speed Boost Logik
        if (isBurning && cookingTime > 0) {
            float multiplier = 1.0f;

            // Sicherer Block-Check
            if (state.isOf(ModBlocks.NETHERITE_BLAST_FURNACE)) {
                multiplier = 1.5f;
            } else if (state.isOf(ModBlocks.REINFORCED_BLAST_FURNACE)) {
                multiplier = 1.25f;
            }

            // Fortschritt manipulieren (Vanilla macht +1 pro Tick)
            // 1.25x -> +1 extra alle 4 Ticks
            if (multiplier == 1.25f && world.getTime() % 4 == 0) {
                delegate.set(2, cookingTime + 1);
            }
            // 1.50x -> +1 extra alle 2 Ticks
            else if (multiplier == 1.5f && world.getTime() % 2 == 0) {
                delegate.set(2, cookingTime + 1);
            }

            // Clamp, damit es nicht über das Ziel hinausschießt (verhindert Grafik-Glitches)
            int newTime = delegate.get(2);
            if (newTime >= totalTime) {
                delegate.set(2, totalTime);
            }
        }
    }
}