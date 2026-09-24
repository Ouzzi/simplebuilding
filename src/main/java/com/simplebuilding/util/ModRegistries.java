package com.simplebuilding.util;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Blocks;

public class ModRegistries {

    // --- Serializer Definition ---
    // Die Codecs liegen im gemeinsamen Baum bei der Rezeptklasse, damit Fabric, NeoForge und Forge
    // dasselbe JSON-Format lesen (vorher stand hier eine von drei Abschriften).
    public static final RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER =
            new RecipeSerializer<>(ReinforcedBundleRecipe.MAP_CODEC, ReinforcedBundleRecipe.STREAM_CODEC);

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