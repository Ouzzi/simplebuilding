package com.simpleblocks.saw;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A recipe only the Shapecutter cuts ({@code simpleblocks:sawing}): the same JSON as {@code minecraft:stonecutting}
 * ({@code ingredient}, {@code result}). The saw lists these together with every Vanilla stonecutting recipe
 * ({@link Sawing#entries}). No recipe-book entry (empty display), like a station-only recipe.
 */
public final class SawingRecipe extends SingleItemRecipe {
    public static final MapCodec<SawingRecipe> MAP_CODEC = simpleMapCodec(SawingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = simpleStreamCodec(SawingRecipe::new);
    public static final RecipeSerializer<SawingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public SawingRecipe(Recipe.CommonInfo info, Ingredient ingredient, ItemStackTemplate result) {
        super(info, ingredient, result);
    }

    @Override
    public RecipeType<SawingRecipe> getType() {
        return Sawing.TYPE;
    }

    @Override
    public RecipeSerializer<SawingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.STONECUTTER;
    }
}
