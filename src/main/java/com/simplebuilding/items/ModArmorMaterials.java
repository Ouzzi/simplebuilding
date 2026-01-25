package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import java.util.EnumMap;

public class ModArmorMaterials {

    // Manueller Key, falls RegistryKeys.EQUIPMENT_ASSET fehlt
    private static final RegistryKey<Registry<EquipmentAsset>> ASSET_REGISTRY_KEY =
            RegistryKey.ofRegistry(Identifier.ofVanilla("equipment_asset"));

    public static final RegistryKey<EquipmentAsset> ENDERITE_ASSET_KEY =
            RegistryKey.of(ASSET_REGISTRY_KEY, Identifier.of(Simplebuilding.MOD_ID, "enderite"));

    public static final ArmorMaterial ENDERITE = new ArmorMaterial(
            42,
            new EnumMap<>(EquipmentType.class) {{
                put(EquipmentType.HELMET, 4);
                put(EquipmentType.CHESTPLATE, 9);
                put(EquipmentType.LEGGINGS, 7);
                put(EquipmentType.BOOTS, 4);
            }},
            18,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.2F,
            ItemTags.NETHERITE_TOOL_MATERIALS,
            ENDERITE_ASSET_KEY
    );
}