package com.simplebuilding.recipe;

import com.simplebuilding.util.ModRegistries;
import net.minecraft.component.DataComponentTypes; // WICHTIG
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;

public class ReinforcedBundleRecipe extends ShapedRecipe {
    private final ItemStack resultStack;
    private final RawShapedRecipe rawPattern;

    public ReinforcedBundleRecipe(String group, CraftingRecipeCategory category, RawShapedRecipe raw, ItemStack result) {
        super(group, category, raw, result);
        this.resultStack = result;
        this.rawPattern = raw;
    }

    public ItemStack getResultStack() {
        return this.resultStack;
    }

    public RawShapedRecipe getRaw() {
        return this.rawPattern;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        // Erzeugt ein frisches Reinforced Bundle (mit korrektem Namen/Textur als Basis)
        ItemStack result = super.craft(input, lookup);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (stack.getItem() instanceof BundleItem) {
                // FIX: Wir kopieren NICHT mehr alles blind (applyComponentsFrom).
                // Wir kopieren nur den INHALT.
                var contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (contents != null) {
                    result.set(DataComponentTypes.BUNDLE_CONTENTS, contents);
                }

                // Optional: Falls du willst, dass Umbenennungen (im Amboss) erhalten bleiben:
                var customName = stack.get(DataComponentTypes.CUSTOM_NAME);
                if (customName != null) {
                    result.set(DataComponentTypes.CUSTOM_NAME, customName);
                }

                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<ReinforcedBundleRecipe> getSerializer() {
        return ModRegistries.REINFORCED_BUNDLE_SERIALIZER;
    }
}