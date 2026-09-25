package com.simplebuilding.datagen;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;

import java.util.Map;

/** Model datagen calls, MC 26.3 side (see the 26.2 twin in src/mc26_2/java). */
final class ModelGenCompat {

    private ModelGenCompat() {
    }

    static void trimmableArmorItem(ItemModelGenerators generators, Item armor, ResourceKey<EquipmentAsset> asset,
                                   Identifier slotTrimPrefix) {
        generators.generateTrimmableItem(armor, slotTrimPrefix, false, Map.of());
    }
}
