package com.simpleovens.block;

import com.simpleovens.OvenPerks;
import com.simpleovens.OvenTier;
import com.simpleovens.OvensRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Block entity of all three smoker tiers (the tier comes from the block state). */
public class TierSmokerBlockEntity extends AbstractFurnaceBlockEntity {

    public TierSmokerBlockEntity(BlockPos pos, BlockState state) {
        super(OvensRegistry.SMOKER_BE, pos, state, RecipeType.SMOKING);
    }

    @Override
    protected Component getDefaultName() {
        OvenTier tier = OvenPerks.tierOf(this.getBlockState());
        return Component.translatable("container.simpleovens." + (tier == null ? OvenTier.REINFORCED : tier).prefix() + "_smoker");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new SmokerMenu(syncId, playerInventory, this, this.dataAccess);
    }

    /** A finished smelt; Netherite and Enderite count the recipe twice and so pay double experience. */
    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        super.setRecipeUsed(recipe);
        if (OvenPerks.doublesExperience(this.getBlockState(), recipe)) super.setRecipeUsed(recipe);
    }

    /**
     * MC 26.3 cooks blasting/smoking recipes at the furnace's time and speeds the Vanilla blast furnace and smoker up
     * through the fuel - only for blocks in {@code minecraft:block/fast_cooking}, which names the two Vanilla blocks.
     * Without this the tiers would cook at half the Vanilla speed of their family.
     */
    protected float getSpeedMultiplier(ServerLevel level, ItemStack fuelItem) {
        return 2.0F;
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, TierSmokerBlockEntity entity) {
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, entity);
        OvenPerks.boost(state, entity.dataAccess);
    }
}
