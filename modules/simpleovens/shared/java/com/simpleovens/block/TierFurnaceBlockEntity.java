package com.simpleovens.block;

import com.simpleovens.OvenPerks;
import com.simpleovens.OvenTier;
import com.simpleovens.OvensRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Block entity of all three furnace tiers (the tier comes from the block state). */
public class TierFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public TierFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(OvensRegistry.FURNACE_BE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        OvenTier tier = OvenPerks.tierOf(this.getBlockState());
        return Component.translatable("container.simpleovens." + (tier == null ? OvenTier.REINFORCED : tier).prefix() + "_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new FurnaceMenu(syncId, playerInventory, this, this.dataAccess);
    }

    /** A finished smelt; Netherite and Enderite count the recipe twice and so pay double experience. */
    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        super.setRecipeUsed(recipe);
        if (OvenPerks.doublesExperience(this.getBlockState(), recipe)) super.setRecipeUsed(recipe);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, TierFurnaceBlockEntity entity) {
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, entity);
        OvenPerks.boost(state, entity.dataAccess);
    }
}
