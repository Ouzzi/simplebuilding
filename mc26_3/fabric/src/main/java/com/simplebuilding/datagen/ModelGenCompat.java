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

    /** Vanilla's trim materials with the palette suffix of their item models, in vanilla's order. */
    static java.util.Map<ResourceKey<net.minecraft.world.item.equipment.trim.TrimMaterial>, String> vanillaTrimModels() {
        java.util.Map<ResourceKey<net.minecraft.world.item.equipment.trim.TrimMaterial>, String> out = new java.util.LinkedHashMap<>();
        for (ItemModelGenerators.TrimMaterialData material : ItemModelGenerators.TRIM_MATERIAL_MODELS) {
            out.put(material.materialKey(), material.palette().suffix());
        }
        return out;
    }

    static void trimmableArmorItem(ItemModelGenerators generators, Item armor, ResourceKey<EquipmentAsset> asset,
                                   Identifier slotTrimPrefix) {
        generators.generateTrimmableItem(armor, slotTrimPrefix, false, Map.of());
    }
}
