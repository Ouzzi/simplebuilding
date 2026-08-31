package com.simplebuilding.recipe;

import com.simplebuilding.util.ModRegistries;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.core.component.DataComponents;

public class ReinforcedBundleRecipe extends ShapedRecipe {
    private final ItemStack resultStack;
    private final ShapedRecipePattern rawPattern;

    public ReinforcedBundleRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStack result) {
        super(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(category, group),
                raw,
                ItemStackTemplate.fromNonEmptyStack(result)
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
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);

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
