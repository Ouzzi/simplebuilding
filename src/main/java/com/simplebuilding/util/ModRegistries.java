package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;

public class ModRegistries {

    // --- Serializer Definition ---
    public static final RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER = new RecipeSerializer<>(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> ((ReinforcedBundleRecipe) recipe).getRaw()),
                    ItemStack.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::getResultStack)
            ).apply(instance, ReinforcedBundleRecipe::new)),
            StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeUtf(recipe.group());
                        buf.writeEnum(recipe.category());
                        ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.getRaw());
                        ItemStack.STREAM_CODEC.encode(buf, recipe.getResultStack());
                    },
                    buf -> new ReinforcedBundleRecipe(
                            buf.readUtf(),
                            buf.readEnum(CraftingBookCategory.class),
                            ShapedRecipePattern.STREAM_CODEC.decode(buf),
                            ItemStack.STREAM_CODEC.decode(buf)
                    )
            )
    );

    public static void registerModStuffs() {
        registerEvents();
        // registerNetworking(); <--- ENTFERNT! Das macht jetzt ModMessages.
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "reinforced_bundle"), REINFORCED_BUNDLE_SERIALIZER);
    }

    private static void registerEvents() {
        // Constructor's Touch: delegate rather than keep a second copy of the cycling logic.
        // The copy that used to live here drifted away from NeoForge once already (chat vs.
        // actionbar readout), which is why ConstructorsTouchInteraction exists at all - a fix
        // made there has to reach this loader too, and only delegation guarantees that.
        UseBlockCallback.EVENT.register(ConstructorsTouchInteraction::handleUseBlock);
    }
}