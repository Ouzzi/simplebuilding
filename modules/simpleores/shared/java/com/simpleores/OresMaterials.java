package com.simpleores;

import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.resources.ResourceKey;

/**
 * The two rose gold alloys (concept RG30, section 3). Hard (Rose Copper, about 585) is the early-game step: iron durability,
 * about 90 % of gold's tempo and enchantability. Soft (Rose Gold, about 750) is the fast one: gold tempo and
 * enchantability, about 90 % of iron's durability. Both mine like iron (can break diamond ore, not ancient debris), neither
 * beats diamond's durability (A12: small intervention). Simple Hammers carries a copy of these numbers for its optional
 * rose gold hammers (module rule: it must not import this class).
 */
public final class OresMaterials {
    public static final TagKey<Item> REPAIRS_ROSE_BRONZE = TagKey.create(Registries.ITEM, SimpleOres.id("repairs_rose_bronze_equipment"));
    public static final TagKey<Item> REPAIRS_ROSE_GOLD = TagKey.create(Registries.ITEM, SimpleOres.id("repairs_rose_gold_equipment"));

    /** durability, speed, attack bonus, enchantability (Vanilla: copper 190/5/1/13, iron 250/6/2/14, gold 32/12/0/22; 05.11.: Rose Copper = copper durability, Rose Gold = iron durability, both faster). */
    public static final ToolMaterial ROSE_BRONZE_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 190, 8.0F, 1.0F, 15, REPAIRS_ROSE_BRONZE);
    public static final ToolMaterial ROSE_GOLD_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 12.0F, 2.0F, 22, REPAIRS_ROSE_GOLD);

    public static final ResourceKey<EquipmentAsset> ROSE_BRONZE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, SimpleOres.id("rose_bronze"));
    public static final ResourceKey<EquipmentAsset> ROSE_GOLD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, SimpleOres.id("rose_gold"));

    /** Durability factor (Vanilla: copper 11, iron 15, gold 7), protection head/chest/legs/boots, body (horse), enchantability. */
    public static final ArmorMaterial ROSE_BRONZE_ARMOR = new ArmorMaterial(11, defense(2, 4, 3, 1, 4), 12,
            SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, REPAIRS_ROSE_BRONZE, ROSE_BRONZE_ASSET);
    public static final ArmorMaterial ROSE_GOLD_ARMOR = new ArmorMaterial(15, defense(2, 5, 3, 1, 6), 25,
            SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F, REPAIRS_ROSE_GOLD, ROSE_GOLD_ASSET);

    private static EnumMap<ArmorType, Integer> defense(int head, int chest, int legs, int boots, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET, head);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.BODY, body);
        return map;
    }

    private OresMaterials() {}
}
