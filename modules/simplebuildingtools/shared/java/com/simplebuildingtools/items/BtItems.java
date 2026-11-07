package com.simplebuildingtools.items;

import com.simplebuildingtools.SimpleBuildingTools;
import com.simplebuildingtools.items.custom.BlueprintItem;
import com.simplebuildingtools.items.custom.BuildingWandItem;
import com.simplebuildingtools.items.custom.OctantItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The tools (until 07.11.2026 SimpleBuilding's {@code ModItems}): durability twice the sledgehammer of the same tier, square
 * and enchantability as before, Netherite and Enderite fire resistant.
 */
public final class BtItems {
    /** Durability base of SimpleBuilding's sledgehammers (x4), the wands have twice that. */
    private static final int BASE = 4;
    public static BuildingWandItem COPPER_BUILDING_WAND;
    public static BuildingWandItem IRON_BUILDING_WAND;
    public static BuildingWandItem GOLD_BUILDING_WAND;
    public static BuildingWandItem DIAMOND_BUILDING_WAND;
    public static BuildingWandItem NETHERITE_BUILDING_WAND;
    public static BuildingWandItem ENDERITE_BUILDING_WAND;
    /** Creative tab only, no recipe: area like Enderite, unlimited reach, no durability or material (BuildingWandItem#freeBuild). */
    public static BuildingWandItem CREATIVE_BUILDING_WAND;
    public static OctantItem OCTANT;
    /** EnumMap: datagen and the tab iterate it in dye order. */
    public static final Map<DyeColor, OctantItem> COLORED_OCTANT_ITEMS = new EnumMap<>(DyeColor.class);
    /** Empty ones stack like maps, filled ones are single. */
    public static BlueprintItem BLUEPRINT;
    /** Creative tab only, no recipe: unlimited reach, signs for good. */
    public static BlueprintItem CREATIVE_BLUEPRINT;

    private static final List<Item> ALL = new ArrayList<>();
    private static boolean initialized;

    private BtItems() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        COPPER_BUILDING_WAND = wand("copper_building_wand", 190 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_COPPER, 18);
        IRON_BUILDING_WAND = wand("iron_building_wand", 250 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_IRON, 14);
        GOLD_BUILDING_WAND = wand("gold_building_wand", 32 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_GOLD, 22);
        DIAMOND_BUILDING_WAND = wand("diamond_building_wand", 1561 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_DIAMOND, 10);
        NETHERITE_BUILDING_WAND = wand("netherite_building_wand", 2031 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_NETHERITE, 15);
        ENDERITE_BUILDING_WAND = wand("enderite_building_wand", 2500 * BASE * 2, BuildingWandItem.BUILDING_WAND_SQUARE_ENDERITE, 18);
        CREATIVE_BUILDING_WAND = (BuildingWandItem) register("creative_building_wand", s -> new BuildingWandItem(
                s.stacksTo(1).fireResistant().rarity(Rarity.EPIC).enchantable(18)).creative());
        CREATIVE_BUILDING_WAND.setWandSquareDiameter(BuildingWandItem.BUILDING_WAND_SQUARE_ENDERITE);
        OCTANT = (OctantItem) register("octant", s -> new OctantItem(s.durability(OctantItem.DURABILITY_OCTANT).enchantable(15), null));
        for (DyeColor color : DyeColor.values()) {
            COLORED_OCTANT_ITEMS.put(color, (OctantItem) register("octant_" + color.getName(),
                    s -> new OctantItem(s.durability(OctantItem.DURABILITY_OCTANT).enchantable(15), color)));
        }
        BLUEPRINT = (BlueprintItem) register("blueprint", s -> new BlueprintItem(s.stacksTo(16)));
        CREATIVE_BLUEPRINT = (BlueprintItem) register("creative_blueprint", s -> new BlueprintItem(s.stacksTo(16).rarity(Rarity.EPIC), true));
    }

    private static BuildingWandItem wand(String name, int durability, int square, int enchantability) {
        BuildingWandItem wand = (BuildingWandItem) register(name, s -> new BuildingWandItem(
                (name.startsWith("netherite_") || name.startsWith("enderite_") ? s.fireResistant() : s).durability(durability).enchantable(enchantability)));
        wand.setWandSquareDiameter(square);
        return wand;
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        var id = SimpleBuildingTools.id(name);
        Item item = factory.apply(com.simplelib.api.RarityRules.apply(name, new Item.Properties()).setId(ResourceKey.create(Registries.ITEM, id)));
        Registry.register(BuiltInRegistries.ITEM, id, item);
        ALL.add(item);
        return item;
    }

    /** Every tool item in tab order. */
    public static List<Item> all() {
        return List.copyOf(ALL);
    }

    /** The tab: wands, the octants, the blueprints (creative ones included, as before). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : ALL) out.add(new ItemStack(item));
        return out;
    }
}
