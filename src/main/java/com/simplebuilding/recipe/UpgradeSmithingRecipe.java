package com.simplebuilding.recipe;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class UpgradeSmithingRecipe implements SmithingRecipe {
    private final Optional<Ingredient> template;
    private final Ingredient base;
    private final Optional<Ingredient> addition;

    public UpgradeSmithingRecipe(Optional<Ingredient> template, Ingredient base, Optional<Ingredient> addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forMultipleSlots(List.of(this.template, Optional.of(this.base), this.addition));
    }

    @Override
    public boolean matches(SmithingRecipeInput input, World world) {
        return Ingredient.matches(this.template, input.template()) &&
                this.base.test(input.base()) &&
                Ingredient.matches(this.addition, input.addition());
    }

    @Override
    public ItemStack craft(SmithingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack baseStack = input.base();
        ItemStack templateStack = input.template();
        ItemStack additionStack = input.addition();

        // Kopie der Rüstung erstellen
        ItemStack result = baseStack.copy();

        // 1. Fall: Glowing Template (Visuelles Leuchten des Trims)
        if (templateStack.isOf(ModItems.GLOWING_TRIM_TEMPLATE) && additionStack.isOf(Items.GLOW_INK_SAC)) {
            // Setze die Komponente auf TRUE
            result.set(ModDataComponentTypes.VISUAL_GLOW, true);
        }

        // 2. Fall: Emitting Template (Lichtquelle)
        if (templateStack.isOf(ModItems.EMITTING_TRIM_TEMPLATE) && additionStack.isOf(Items.GLOWSTONE_DUST)) {
            // Setze die Komponente auf TRUE
            result.set(ModDataComponentTypes.LIGHT_SOURCE, true);
        }

        return result;
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return ModRecipes.UPGRADE_SMITHING_SERIALIZER;
    }

    @Override
    public Optional<Ingredient> template() { return this.template; }
    @Override
    public Ingredient base() { return this.base; }
    @Override
    public Optional<Ingredient> addition() { return this.addition; }
}