package com.simplebuilding.mixin;

import com.simplebuilding.util.SledgehammerCrafting;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Formlose Rezepte mit einem Vorschlaghammer geben den Hammer beschaedigt zurueck statt ihn zu
 * verbrauchen ({@link SledgehammerCrafting}). Ohne Hammer im Raster ist das Vanillas eigener Rest.
 * Das Rezept bleibt ein gewoehnliches {@code minecraft:crafting_shapeless} - JEI, Rezeptbuch und Wiki
 * zeigen es ohne Zusatzwissen.
 */
@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin implements CraftingRecipe {

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return SledgehammerCrafting.remainingItems(input);
    }
}
