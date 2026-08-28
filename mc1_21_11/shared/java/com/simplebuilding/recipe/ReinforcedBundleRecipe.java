package com.simplebuilding.recipe;

import com.simplebuilding.util.ModRegistries;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;

public class ReinforcedBundleRecipe extends ShapedRecipe {
    private final ItemStack resultStack;
    private final ShapedRecipePattern rawPattern;

    public ReinforcedBundleRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStack result) {
        // MC 1.21.11: ShapedRecipe nimmt group/category/pattern/result noch einzeln entgegen
        // (Recipe.CommonInfo und CraftingRecipe.CraftingBookInfo gibt es erst ab 26.2); der
        // 4-Argument-Konstruktor setzt showNotification=true -- identisch zu CommonInfo(true).
        super(
                group,
                category,
                raw,
                result
        );
        this.resultStack = result;
        this.rawPattern = raw;
    }

    public ItemStack getResultStack() {
        return this.resultStack;
    }

    public ShapedRecipePattern getRaw() {
        return this.rawPattern;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.getItem() instanceof BundleItem) {
                var contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents != null) {
                    result.set(DataComponents.BUNDLE_CONTENTS, contents);
                }

                var customName = stack.get(DataComponents.CUSTOM_NAME);
                if (customName != null) {
                    result.set(DataComponents.CUSTOM_NAME, customName);
                }

                break;
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRegistries.REINFORCED_BUNDLE_SERIALIZER;
    }
}
