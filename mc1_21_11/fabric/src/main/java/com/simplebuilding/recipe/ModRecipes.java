package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class ModRecipes {

    public static RecipeSerializer<CountBasedSmithingRecipe> COUNT_BASED_SMITHING_SERIALIZER;
    public static RecipeType<CountBasedSmithingRecipe> COUNT_BASED_SMITHING;

    private static final MapCodec<UpgradeSmithingRecipe> UPGRADE_SMITHING_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.optionalFieldOf("template").forGetter(UpgradeSmithingRecipe::templateIngredient),
            Ingredient.CODEC.fieldOf("base").forGetter(UpgradeSmithingRecipe::baseIngredient),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(UpgradeSmithingRecipe::additionIngredient)
    ).apply(instance, UpgradeSmithingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSmithingRecipe> UPGRADE_SMITHING_STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buf, recipe.templateIngredient());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.baseIngredient());
                Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buf, recipe.additionIngredient());
            },
            buf -> new UpgradeSmithingRecipe(
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buf)
            )
    );

    public static void registerRecipes() {
        Simplebuilding.LOGGER.info("Registering Recipes for " + Simplebuilding.MOD_ID);

        COUNT_BASED_SMITHING_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "count_based_smithing"),
            // MC 1.21.11: RecipeSerializer ist noch ein Interface (codec()/streamCodec());
            // die konkrete Record-Klasse mit (MapCodec, StreamCodec) gibt es erst ab 26.2.
            new RecipeSerializer<CountBasedSmithingRecipe>() {
                @Override
                public MapCodec<CountBasedSmithingRecipe> codec() {
                    return CountBasedSmithingRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, CountBasedSmithingRecipe> streamCodec() {
                    return CountBasedSmithingRecipe.STREAM_CODEC;
                }
            }
        );

        COUNT_BASED_SMITHING = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "count_based_smithing"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return "count_based_smithing";
                }
            });
    }

    public static final RecipeSerializer<UpgradeSmithingRecipe> UPGRADE_SMITHING_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "upgrade_smithing"),
            new RecipeSerializer<UpgradeSmithingRecipe>() {
                @Override
                public MapCodec<UpgradeSmithingRecipe> codec() {
                    return UPGRADE_SMITHING_CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, UpgradeSmithingRecipe> streamCodec() {
                    return UPGRADE_SMITHING_STREAM_CODEC;
                }
            }
    );
}
