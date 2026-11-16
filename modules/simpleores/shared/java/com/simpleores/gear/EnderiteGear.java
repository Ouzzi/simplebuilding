package com.simpleores.gear;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import com.simpleores.block.MetalRodBlock;
import java.util.EnumMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * N47-M3 (b-circ46, 14.11.2026, docs/ai/PLAN-B-CIRC46-2026-11-14.md): the Enderite tools and armor, the Netherite and Enderite food and
 * the five lightning poles (iron, gold, diamond, netherite, enderite) moved here from SimpleBuilding's core. Same ids under
 * {@code simpleores:} (SimpleBuilding moves its old ids: {@code SharedMaterials.ORES_*}, {@code IdMovesHook}); every number below is the
 * core's old value (ModToolMaterials.ENDERITE, ModArmorMaterials.ENDERITE, ModItems food, ModBlocks poles). The armor trim material of
 * Enderite stays SimpleBuilding's ({@code SharedMaterialTrimMixin}). Registered from {@link OresRegistry} after the line.
 */
public final class EnderiteGear {
    /** Enderite equipment is repaired with Enderite ingots only (owner 21.10.); the tag lives in SimpleLib. */
    public static final TagKey<Item> REPAIRS_ENDERITE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "repairs_enderite_equipment"));

    /** Durability 2530, speed 10, attack bonus 5, enchantability 18 (Netherite: 2031/9/4/15). */
    public static final ToolMaterial TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2530, 10.0F, 5.0F, 18, REPAIRS_ENDERITE);
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, SimpleOres.id("enderite"));
    /** Durability factor 42, protection 4/9/7/4, body (horse, nautilus) 22 (Netherite 19), enchantability 18, toughness 4, knockback resistance 0.2. */
    public static final ArmorMaterial ARMOR = new ArmorMaterial(42, defense(4, 9, 7, 4, 22), 18, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F,
            REPAIRS_ENDERITE, ASSET);

    public static final FoodProperties NETHERITE_CARROT_FOOD = new FoodProperties(6, 1.2f, true);
    public static final FoodProperties NETHERITE_APPLE_FOOD = new FoodProperties(8, 1.5f, true);
    public static final FoodProperties ENDERITE_CARROT_FOOD = new FoodProperties(12, 2.4f, true);
    public static final FoodProperties ENDERITE_APPLE_FOOD = new FoodProperties(14, 3.0f, true);

    public static final String[] ARMOR_PARTS = {"helmet", "chestplate", "leggings", "boots"};
    /** Creative tabs: tools, combat and food are listed in {@link OresRegistry#TOOLS}/{@code COMBAT}/{@code FOOD}; the poles go behind the copper lightning rod (functional tab). */
    public static final String[] POLES = java.util.stream.Stream.concat(GearIds.POLES.stream(), GearIds.NEW_POLES.stream())
            .sorted(java.util.Comparator.comparingInt(GearIds.TAB_ORDER::indexOf)).toArray(String[]::new);
    // The poles (owner 2026-10-02/03, N41): lightning rods of metal, each pulling a strike only within its own range (MetalRodBlock).
    public static final Block IRON_POLE = pole("iron_pole", MetalRodBlock.IRON_RANGE, MapColor.METAL, 5.0F, 6.0F, SoundType.METAL);
    public static final Block GOLD_POLE = pole("gold_pole", MetalRodBlock.GOLD_RANGE, MapColor.GOLD, 3.0F, 6.0F, SoundType.METAL);
    /** N77 (owner 16.11.): the emerald pole, like the diamond one (between gold and diamond in range). */
    public static final Block EMERALD_POLE = pole("emerald_pole", MetalRodBlock.EMERALD_RANGE, MapColor.EMERALD, 5.0F, 6.0F, SoundType.METAL);
    public static final Block DIAMOND_POLE = pole("diamond_pole", MetalRodBlock.DIAMOND_RANGE, MapColor.DIAMOND, 5.0F, 6.0F, SoundType.METAL);
    public static final Block NETHERITE_POLE = pole("netherite_pole", MetalRodBlock.NETHERITE_RANGE, MapColor.COLOR_BLACK, 50.0F, 1200.0F, SoundType.NETHERITE_BLOCK);
    public static final Block ENDERITE_POLE = pole("enderite_pole", MetalRodBlock.ENDERITE_RANGE, MapColor.COLOR_BLACK, 50.0F, 1200.0F, SoundType.NETHERITE_BLOCK);

    // Tools and armor (every piece fire resistant like everything made of Enderite; ranks by SimpleLib RarityRules).
    public static final Item ENDERITE_SWORD = OresRegistry.putItem("enderite_sword", p -> new Item(p.fireResistant().sword(TOOLS, 3.0F, -2.4F)));
    // Mirrors the vanilla spear setup so the item gets real spear mechanics/components.
    public static final Item ENDERITE_SPEAR = OresRegistry.putItem("enderite_spear",
            p -> new Item(p.fireResistant().spear(TOOLS, 1.15F, 1.2F, 0.4F, 2.5F, 7.0F, 5.5F, 5.1F, 8.75F, 4.6F)));
    public static final Item ENDERITE_PICKAXE = OresRegistry.putItem("enderite_pickaxe", p -> new Item(p.fireResistant().pickaxe(TOOLS, 1.0F, -2.8F)));
    public static final Item ENDERITE_AXE = OresRegistry.putItem("enderite_axe", p -> new Item(p.fireResistant().axe(TOOLS, 5.0F, -3.0F)));
    public static final Item ENDERITE_SHOVEL = OresRegistry.putItem("enderite_shovel", p -> new Item(p.fireResistant().shovel(TOOLS, 1.5F, -3.0F)));
    public static final Item ENDERITE_HOE = OresRegistry.putItem("enderite_hoe", p -> new Item(p.fireResistant().hoe(TOOLS, -4.0F, 0.0F)));
    public static final Item ENDERITE_HELMET = armor("enderite_helmet", ArmorType.HELMET);
    public static final Item ENDERITE_CHESTPLATE = armor("enderite_chestplate", ArmorType.CHESTPLATE);
    public static final Item ENDERITE_LEGGINGS = armor("enderite_leggings", ArmorType.LEGGINGS);
    public static final Item ENDERITE_BOOTS = armor("enderite_boots", ArmorType.BOOTS);
    // Horse and nautilus armor (owner 2026-09-28): one step above Vanilla Netherite (body 22 against 19, toughness 4 against 3, knockback 0.2 against 0.1).
    public static final Item ENDERITE_HORSE_ARMOR = OresRegistry.putItem("enderite_horse_armor", p -> new Item(p.horseArmor(ARMOR).fireResistant()));
    public static final Item ENDERITE_NAUTILUS_ARMOR = OresRegistry.putItem("enderite_nautilus_armor", p -> new Item(p.nautilusArmor(ARMOR).fireResistant()));

    private EnderiteGear() {}

    /** Called once at the end of {@link OresRegistry}'s initialisation: the food and the pole items (the gear items register in the fields above). */
    public static void init() {
        OresRegistry.putItem("netherite_carrot", p -> new Item(p.food(NETHERITE_CARROT_FOOD, netheriteFood(false)).fireResistant().rarity(Rarity.UNCOMMON)));
        OresRegistry.putItem("netherite_apple", p -> new Item(p.food(NETHERITE_APPLE_FOOD, netheriteFood(true)).fireResistant().rarity(Rarity.UNCOMMON)));
        OresRegistry.putItem("enderite_carrot", p -> new Item(p.food(ENDERITE_CARROT_FOOD, enderiteFood(false)).fireResistant().rarity(Rarity.EPIC)));
        OresRegistry.putItem("enderite_apple", p -> new Item(p.food(ENDERITE_APPLE_FOOD, enderiteFood(true)).fireResistant().rarity(Rarity.EPIC)));
        // RARE like Vanilla's enchanted golden apple (docs/RARITAETEN.md), with the glint.
        OresRegistry.putItem("enchanted_netherite_apple", p -> new Item(p.food(NETHERITE_APPLE_FOOD, enchantedNetheriteFood()).fireResistant()
                .rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        OresRegistry.putItem("enchanted_enderite_apple", p -> new Item(p.food(ENDERITE_APPLE_FOOD, enchantedEnderiteFood()).fireResistant()
                .rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        for (String path : POLES) {
            Rarity rank = path.equals("netherite_pole") ? Rarity.UNCOMMON : path.equals("enderite_pole") ? Rarity.EPIC : null;
            OresRegistry.putItem(path, p -> {
                p = p.useBlockDescriptionPrefix();
                return new BlockItem(OresRegistry.BLOCKS.get(path), rank == null ? p : p.fireResistant().rarity(rank));
            });
        }
    }

    /** Whether {@code item} is a piece of Enderite armor (helmet to boots). */
    public static boolean isEnderiteArmor(net.minecraft.world.item.ItemStack stack) {
        return stack.is(ENDERITE_BOOTS) || stack.is(ENDERITE_LEGGINGS) || stack.is(ENDERITE_CHESTPLATE) || stack.is(ENDERITE_HELMET);
    }

    private static Block pole(String path, int range, MapColor color, float hardness, float resistance, SoundType sound) {
        return OresRegistry.putBlock(path, Blocks.GLASS, s -> new MetalRodBlock(range,
                s.mapColor(color).forceSolidOn().requiresCorrectToolForDrops().strength(hardness, resistance).sound(sound).noOcclusion()));
    }

    private static Item armor(String path, ArmorType type) {
        return OresRegistry.putItem(path, p -> new Item(p.fireResistant().durability(type.getDurability(42)).repairable(ARMOR.repairIngredient()).humanoidArmor(ARMOR, type)));
    }

    private static EnumMap<ArmorType, Integer> defense(int head, int chest, int legs, int boots, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET, head);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.BODY, body);
        return map;
    }

    private static Consumable.Builder eat() {
        return Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f);
    }

    private static Consumable.Builder with(Consumable.Builder builder, MobEffectInstance... effects) {
        for (MobEffectInstance effect : effects) builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect, 1.0f));
        return builder;
    }

    /** Apple: Resistance I 1:00, Absorption II 0:30, Regeneration II 0:10. Carrot: Fire Resistance 2:00, Night Vision 2:00. */
    public static Consumable netheriteFood(boolean apple) {
        return apple
                ? with(eat(), new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0), new MobEffectInstance(MobEffects.ABSORPTION, 600, 1),
                        new MobEffectInstance(MobEffects.REGENERATION, 200, 1)).build()
                : with(eat(), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0)).build();
    }

    /** Apple: Regeneration II 0:20, Resistance I 2:30, Absorption IV 2:00, Health Boost III 2:00. Carrot: Night Vision 5:00, Speed II 1:00, Jump Boost II 1:00, Saturation. */
    public static Consumable enderiteFood(boolean apple) {
        return apple
                ? with(eat(), new MobEffectInstance(MobEffects.REGENERATION, 400, 1), new MobEffectInstance(MobEffects.RESISTANCE, 3000, 0),
                        new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), new MobEffectInstance(MobEffects.HEALTH_BOOST, 2400, 2)).build()
                : with(eat(), new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), new MobEffectInstance(MobEffects.SPEED, 1200, 1),
                        new MobEffectInstance(MobEffects.JUMP_BOOST, 1200, 1), new MobEffectInstance(MobEffects.SATURATION, 1, 0)).build();
    }

    /** Regeneration II 0:40, Resistance I 5:00, Fire Resistance 5:00, Absorption IV 2:00, Strength II 2:00. */
    public static Consumable enchantedNetheriteFood() {
        return with(eat(), new MobEffectInstance(MobEffects.REGENERATION, 800, 1), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3),
                new MobEffectInstance(MobEffects.STRENGTH, 2400, 1)).build();
    }

    /** Regeneration III 1:00, Resistance II 5:00, Fire Resistance 10:00, Absorption V 3:00, Health Boost V 3:00, Night Vision 5:00. */
    public static Consumable enchantedEnderiteFood() {
        return with(eat(), new MobEffectInstance(MobEffects.REGENERATION, 1200, 2), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 1),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 3600, 4),
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 4), new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0)).build();
    }
}
