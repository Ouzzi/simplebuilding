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
        // Hier implementieren wir die Stufen-Logik (Level 1 -> Level 2)
        if (templateStack.isOf(ModItems.GLOWING_TRIM_TEMPLATE) && additionStack.isOf(Items.GLOW_INK_SAC)) {
            // Lese das aktuelle Level aus (Standard ist 0)
            int currentLevel = baseStack.getOrDefault(ModDataComponentTypes.GLOW_LEVEL, 0);

            // Wenn das Level noch unter 2 ist, erhöhen wir es
            if (currentLevel < 2) {
                result.set(ModDataComponentTypes.GLOW_LEVEL, currentLevel + 1);
            } else {
                // Wenn es schon Level 2 (oder höher) ist, geben wir nichts zurück (Rezept ungültig/nichts passiert)
                // Oder wir geben das Item unverändert zurück (Materialverschwendung).
                // ItemStack.EMPTY sorgt dafür, dass man es nicht craften kann, wenn es schon max ist.
                return ItemStack.EMPTY;
            }
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