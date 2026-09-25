package com.simplebuilding.datagen;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;

/**
 * Model datagen calls whose signature differs between MC 26.2 and 26.3 (Fabric only; twin in
 * mc26_3/fabric/src/main/java). 26.3 no longer takes the equipment asset for trimmed armor items -
 * the trim palette replacements moved into a map, which is empty for the mod's armor.
 */
final class ModelGenCompat {

    private ModelGenCompat() {
    }

    static void trimmableArmorItem(ItemModelGenerators generators, Item armor, ResourceKey<EquipmentAsset> asset,
                                   Identifier slotTrimPrefix) {
        generators.generateTrimmableItem(armor, asset, slotTrimPrefix, false);
    }
}
