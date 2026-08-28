package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import java.util.EnumMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;

public class ModArmorMaterials {

    // Manueller Key, falls RegistryKeys.EQUIPMENT_ASSET fehlt
    private static final ResourceKey<Registry<EquipmentAsset>> ASSET_REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));

    public static final ResourceKey<EquipmentAsset> ENDERITE_ASSET_KEY =
            ResourceKey.create(ASSET_REGISTRY_KEY, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enderite"));

    public static final ArmorMaterial ENDERITE = new ArmorMaterial(
            42,
            new EnumMap<>(ArmorType.class) {{
                put(ArmorType.HELMET, 4);
                put(ArmorType.CHESTPLATE, 9);
                put(ArmorType.LEGGINGS, 7);
                put(ArmorType.BOOTS, 4);
            }},
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.2F,
            ItemTags.NETHERITE_TOOL_MATERIALS,
            ENDERITE_ASSET_KEY
    );
}