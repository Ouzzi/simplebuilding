package com.simplebuilding.recipe;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
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
    public PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(List.of(this.template, Optional.of(this.base), this.addition));
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level world) {
        return Ingredient.testOptionalIngredient(this.template, input.template()) &&
                this.base.test(input.base()) &&
                Ingredient.testOptionalIngredient(this.addition, input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack baseStack = input.base();
        ItemStack templateStack = input.template();
        ItemStack additionStack = input.addition();

        // Kopie der Rüstung erstellen
        ItemStack result = baseStack.copy();

        // 1. Fall: Glowing Template (Visuelles Leuchten)
        // Logik: Erhöhe das Level, wenn es noch nicht max (2) ist
        if (templateStack.is(ModItems.GLOWING_TRIM_TEMPLATE) && additionStack.is(Items.GLOW_INK_SAC)) {

            // Wir nutzen unsere Utils, um das aktuelle Level (auch von alten Items) zu holen
            int currentLevel = GlowingTrimUtils.getGlowLevel(baseStack);

            if (currentLevel < 2) {
                // Erhöhe Level um 1
                result.set(ModDataComponentTypes.GLOW_LEVEL, currentLevel + 1);

                // Sauberkeit: Entferne das alte Boolean-Flag, da wir jetzt Integer nutzen
                result.remove(ModDataComponentTypes.VISUAL_GLOW);
            } else {
                // Wenn schon Level 2 ist, geht es nicht weiter -> Kein Ergebnis
                return ItemStack.EMPTY;
            }
        }

        // 2. Fall: Emitting Template (Lichtquelle)
        if (templateStack.is(ModItems.EMITTING_TRIM_TEMPLATE) && additionStack.is(Items.GLOWSTONE_DUST)) {
            // Setze die Lichtquellen-Komponente
            result.set(ModDataComponentTypes.LIGHT_SOURCE, true);
        }

        return result;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return ModRecipes.UPGRADE_SMITHING_SERIALIZER;
    }

    @Override
    public Optional<Ingredient> templateIngredient() { return this.template; }
    @Override
    public Ingredient baseIngredient() { return this.base; }
    @Override
    public Optional<Ingredient> additionIngredient() { return this.addition; }
}