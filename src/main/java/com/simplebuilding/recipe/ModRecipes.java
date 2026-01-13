package com.simplebuilding.recipe;

import com.simplebuilding.Simplebuilding;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer; // Manchmal anders je nach Version, hier custom
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class ModRecipes {
    
    // Wir definieren den Codec für unser Rezept (damit Minecraft die JSON lesen kann)
    public static final RecipeSerializer<UpgradeSmithingRecipe> UPGRADE_SMITHING_SERIALIZER = Registry.register(
        Registries.RECIPE_SERIALIZER,
        Identifier.of(Simplebuilding.MOD_ID, "upgrade_smithing"),
        new RecipeSerializer<UpgradeSmithingRecipe>() {
            private final MapCodec<UpgradeSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("template").forGetter(r -> r.template),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("base").forGetter(r -> r.base),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("addition").forGetter(r -> r.addition)
            ).apply(instance, UpgradeSmithingRecipe::new));

            // Packet Codec für Netzwerk-Sync (Wichtig für 1.21!)
            private final PacketCodec<RegistryByteBuf, UpgradeSmithingRecipe> PACKET_CODEC = PacketCodec.ofStatic(
                (buf, recipe) -> {
                    Ingredient.PACKET_CODEC.encode(buf, recipe.template);
                    Ingredient.PACKET_CODEC.encode(buf, recipe.base);
                    Ingredient.PACKET_CODEC.encode(buf, recipe.addition);
                },
                buf -> new UpgradeSmithingRecipe(
                    Ingredient.PACKET_CODEC.decode(buf),
                    Ingredient.PACKET_CODEC.decode(buf),
                    Ingredient.PACKET_CODEC.decode(buf)
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