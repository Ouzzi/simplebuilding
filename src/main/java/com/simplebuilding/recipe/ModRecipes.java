package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ModRecipes {

    public static final RecipeSerializer<CountBasedSmithingRecipe> COUNT_BASED_SMITHING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Simplebuilding.MOD_ID, "count_based_smithing"),
            new RecipeSerializer<CountBasedSmithingRecipe>() {
                @Override
                public MapCodec<CountBasedSmithingRecipe> codec() {
                    return CountBasedSmithingRecipe.CODEC;
                }

                @Override
                public PacketCodec<RegistryByteBuf, CountBasedSmithingRecipe> packetCodec() {
                    return CountBasedSmithingRecipe.PACKET_CODEC;
                }
            }
    );

    public static final RecipeType<CountBasedSmithingRecipe> COUNT_BASED_SMITHING = Registry.register(
            Registries.RECIPE_TYPE,
            Identifier.of(Simplebuilding.MOD_ID, "count_based_smithing"),
            new RecipeType<CountBasedSmithingRecipe>() {
                @Override
                public String toString() {
                    return "count_based_smithing";
                }
            });


    public static final RecipeSerializer<UpgradeSmithingRecipe> UPGRADE_SMITHING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Simplebuilding.MOD_ID, "upgrade_smithing"),
            new RecipeSerializer<UpgradeSmithingRecipe>() {

                // MAP CODEC
                private final MapCodec<UpgradeSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        // In 1.21 nutzen wir Ingredient.CODEC (oder OPTIONAL_CODEC wenn verfügbar)
                        Ingredient.CODEC.optionalFieldOf("template").forGetter(UpgradeSmithingRecipe::template),
                        Ingredient.CODEC.fieldOf("base").forGetter(UpgradeSmithingRecipe::base),
                        Ingredient.CODEC.optionalFieldOf("addition").forGetter(UpgradeSmithingRecipe::addition)
                ).apply(instance, UpgradeSmithingRecipe::new));

                // PACKET CODEC (Netzwerk)
                private final PacketCodec<RegistryByteBuf, UpgradeSmithingRecipe> PACKET_CODEC = PacketCodec.ofStatic(
                        (buf, recipe) -> {
                            Ingredient.OPTIONAL_PACKET_CODEC.encode(buf, recipe.template());
                            Ingredient.PACKET_CODEC.encode(buf, recipe.base());
                            Ingredient.OPTIONAL_PACKET_CODEC.encode(buf, recipe.addition());
                        },
                        buf -> new UpgradeSmithingRecipe(
                                Ingredient.OPTIONAL_PACKET_CODEC.decode(buf),
                                Ingredient.PACKET_CODEC.decode(buf),
                                Ingredient.OPTIONAL_PACKET_CODEC.decode(buf)
                        )
                );

                @Override
                public MapCodec<UpgradeSmithingRecipe> codec() {
                    return CODEC;
                }

                @Override
                public PacketCodec<RegistryByteBuf, UpgradeSmithingRecipe> packetCodec() {
                    return PACKET_CODEC;
                }
            }
    );

    public static void registerRecipes() {
        Simplebuilding.LOGGER.info("Registering Mod Recipes for " + Simplebuilding.MOD_ID);
    }
}