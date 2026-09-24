package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import com.simplebuilding.blocks.ModBlocks;

public class ModBlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    /** Passende Schmelzvorgaenge seit dem letzten Bonus-Gegenstand, siehe {@link FurnaceTierPerks}. */
    private int bonusProgress;

    public ModBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOD_BLAST_FURNACE_BE, pos, state, RecipeType.BLASTING);
    }

    @Override
    protected Component getDefaultName() {
        // Alle drei Stufen teilen sich diese Block-Entity; der Titel unterscheidet sie am Blockzustand,
        // genau wie tick() es fuer den Boost tut. Alle drei Schluessel stehen in en_us und de_de.
        if (this.getBlockState().is(ModBlocks.ENDERITE_BLAST_FURNACE)) {
            return Component.translatable("container.simplebuilding.enderite_blast_furnace");
        }
        return Component.translatable(this.getBlockState().is(ModBlocks.NETHERITE_BLAST_FURNACE)
                ? "container.simplebuilding.netherite_blast_furnace"
                : "container.simplebuilding.reinforced_blast_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new BlastFurnaceMenu(syncId, playerInventory, this, this.dataAccess);
    }

    /**
     * Ein fertig geschmolzener Gegenstand: doppelte Erfahrung ab Netherit, und im Netherit- bzw.
     * Enderit-Schmelzofen jeder vierte bzw. zweite Schmelzvorgang eines Rohmetalls einen Gegenstand
     * mehr. Ist der Ausgabeslot voll, wartet der Bonus auf den naechsten passenden Vorgang.
     */
    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        super.setRecipeUsed(recipe);
        BlockState state = this.getBlockState();
        if (FurnaceTierPerks.doublesExperience(state, recipe)) {
            super.setRecipeUsed(recipe);
        }
        int period = FurnaceTierPerks.bonusPeriod(state);
        if (period > 0 && FurnaceTierPerks.earnsOutputBonus(recipe)) {
            this.bonusProgress = Math.min(this.bonusProgress + 1, period);
            ItemStack result = this.items.get(2);
            if (this.bonusProgress >= period && !result.isEmpty()
                    && result.getCount() < Math.min(result.getMaxStackSize(), this.getMaxStackSize())) {
                result.grow(1);
                this.bonusProgress = 0;
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.bonusProgress = input.getIntOr(FurnaceTierPerks.BONUS_PROGRESS_KEY, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.bonusProgress > 0) {
            output.putInt(FurnaceTierPerks.BONUS_PROGRESS_KEY, this.bonusProgress);
        }
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ModBlastFurnaceBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity);

        ContainerData data = blockEntity.dataAccess;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            if (state.is(ModBlocks.NETHERITE_BLAST_FURNACE)) {
                extraTicks = 3;
            } else if (state.is(ModBlocks.REINFORCED_BLAST_FURNACE)) {
                extraTicks = 1;
            } else if (state.is(ModBlocks.ENDERITE_BLAST_FURNACE)) {
                // Enderit-Stufe: 1 + 7 = achtfache Geschwindigkeit, ebenfalls ohne Brennstoffkosten.
                extraTicks = 7;
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