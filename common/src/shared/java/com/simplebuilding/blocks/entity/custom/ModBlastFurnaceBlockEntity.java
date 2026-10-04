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
import org.jetbrains.annotations.Nullable;
import com.simplebuilding.blocks.ModBlocks;

public class ModBlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

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

    /** Finished smelts pay double experience at upper tiers, without extra output. */
    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        super.setRecipeUsed(recipe);
        BlockState state = this.getBlockState();
        if (FurnaceTierPerks.doublesExperience(state, recipe)) {
            super.setRecipeUsed(recipe);
        }
    }

    // Legacy simplebuilding:bonus_progress is intentionally ignored on load and omitted on save.
    // Vanilla keeps the inventory, recipe counts and cooking timers unchanged.

    /**
     * MC 26.3 only (no @Override: 26.2 has no such hook, and there this method is simply unused).
     * 26.3 dropped the blasting/smoking recipes to the furnace's cooking time and speeds the vanilla
     * blast furnace and smoker up through the fuel instead - but only blocks in the
     * minecraft:block/fast_cooking predicate get the fast multiplier (2.0), and that list names the
     * two vanilla blocks. Without this the mod's machines would cook at half the vanilla speed on
     * 26.3; with it they keep the 26.2 relation (base speed of the vanilla machine, burn time
     * unchanged, tier bonus on top).
     */
    protected float getSpeedMultiplier(ServerLevel level, ItemStack fuelItem) {
        return 2.0F;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ModBlastFurnaceBlockEntity blockEntity) {
        AbstractFurnaceBlockEntity.serverTick(world, pos, state, blockEntity);

        ContainerData data = blockEntity.dataAccess;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        boolean isBurning = data.get(0) > 0;

        if (isBurning && cookTime > 0 && totalTime > 0) {
            int extraTicks = 0;

            // Tempo je Stufe aus server.machines (Standard 2x/4x/8x = 1/3/7 Zusatzticks).
            if (state.is(ModBlocks.NETHERITE_BLAST_FURNACE)) {
                extraTicks = com.simplebuilding.config.ServerTuning.furnaceExtraTicks(2);
            } else if (state.is(ModBlocks.REINFORCED_BLAST_FURNACE)) {
                extraTicks = com.simplebuilding.config.ServerTuning.furnaceExtraTicks(1);
            } else if (state.is(ModBlocks.ENDERITE_BLAST_FURNACE)) {
                // Enderit-Stufe: 1 + 7 = achtfache Geschwindigkeit, ebenfalls ohne Brennstoffkosten.
                extraTicks = com.simplebuilding.config.ServerTuning.furnaceExtraTicks(3);
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