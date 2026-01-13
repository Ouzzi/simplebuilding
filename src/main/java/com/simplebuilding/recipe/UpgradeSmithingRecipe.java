package com.simplebuilding.recipe;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import java.util.stream.Stream;

public class UpgradeSmithingRecipe implements SmithingRecipe {
    private final Ingredient base;      // Die Rüstung
    private final Ingredient template;  // Dein Template (Glowing oder Emitting)
    private final Ingredient addition;  // Das Material (z.B. Glow Ink Sac oder Copper)

    public UpgradeSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(net.minecraft.inventory.SmithingRecipeInput input, World world) {
        // Prüfen, ob alle Slots passen
        return this.template.test(input.template()) && this.base.test(input.base()) && this.addition.test(input.addition());
    }

    @Override
    public ItemStack craft(net.minecraft.inventory.SmithingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack baseStack = input.base();
        ItemStack templateStack = input.template();

        // Wir kopieren die Rüstung (inklusive Enchants, Trims, Namen...)
        ItemStack result = baseStack.copy();

        // 1. Fall: Glowing Template (Visuell)
        if (templateStack.isOf(ModItems.GLOWING_TRIM_TEMPLATE)) {
            // Setze den VISUAL_GLOW Schalter auf true
            result.set(ModDataComponentTypes.VISUAL_GLOW, true);
        }
        
        // 2. Fall: Emitting Template (Lichtquelle)
        if (templateStack.isOf(ModItems.EMITTING_TRIM_TEMPLATE)) {
            // Setze den LIGHT_SOURCE Schalter auf true
            result.set(ModDataComponentTypes.LIGHT_SOURCE, true);
        }

        return result;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return ItemStack.EMPTY; // Wird für Smithing nicht direkt benötigt, da dynamisch
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.UPGRADE_SMITHING_SERIALIZER; // Das erstellen wir gleich
    }
}