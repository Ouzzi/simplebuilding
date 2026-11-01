package com.simplefoundry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What melts into which metal: the input table of stage 1. Ore to ingot stays 1:1 like Vanilla (no ore bonus). */
public final class Melts {
    public record Melt(Metal metal, int units) {}

    private static final Map<Item, Melt> TABLE = new HashMap<>();

    static {
        add(Metal.IRON, 9, Items.RAW_IRON, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.IRON_INGOT);
        add(Metal.IRON, 1, Items.IRON_NUGGET);
        add(Metal.IRON, 81, Items.IRON_BLOCK, Items.RAW_IRON_BLOCK);
        add(Metal.GOLD, 9, Items.RAW_GOLD, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.GOLD_INGOT);
        add(Metal.GOLD, 1, Items.GOLD_NUGGET);
        add(Metal.GOLD, 81, Items.GOLD_BLOCK, Items.RAW_GOLD_BLOCK);
        add(Metal.COPPER, 9, Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.COPPER_INGOT);
        add(Metal.COPPER, 1, Items.COPPER_NUGGET);
        add(Metal.COPPER, 81, Items.COPPER_BLOCK.weathering().unaffected(), Items.RAW_COPPER_BLOCK);
    }

    /** Ingots a piece of gear costs to make (owner SA7 = A: gear melts down). */
    private static final Map<String, Integer> GEAR_INGOTS = new LinkedHashMap<>();
    private static final Map<Item, Melt> GEAR = new HashMap<>();

    static {
        GEAR_INGOTS.put("pickaxe", 3);
        GEAR_INGOTS.put("axe", 3);
        GEAR_INGOTS.put("shovel", 1);
        GEAR_INGOTS.put("hoe", 2);
        GEAR_INGOTS.put("sword", 2);
        GEAR_INGOTS.put("helmet", 5);
        GEAR_INGOTS.put("chestplate", 8);
        GEAR_INGOTS.put("leggings", 7);
        GEAR_INGOTS.put("boots", 4);
        String[][] materials = {{"iron", "IRON"}, {"golden", "GOLD"}, {"copper", "COPPER"}};
        for (String[] material : materials) {
            GEAR_INGOTS.forEach((kind, ingots) -> {
                Identifier id = Identifier.withDefaultNamespace(material[0] + "_" + kind);
                // looked up by name: not every Minecraft version has every piece (copper gear came late)
                if (BuiltInRegistries.ITEM.containsKey(id)) GEAR.put(BuiltInRegistries.ITEM.getValue(id), new Melt(Metal.valueOf(material[1]), ingots * Metal.INGOT));
            });
        }
    }

    private static void add(Metal metal, int units, Item... items) {
        for (Item item : items) TABLE.put(item, new Melt(metal, units));
    }

    /** The melt of the stack's item, or null when it does not melt here. */
    public static Melt of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Melt plain = TABLE.get(stack.getItem());
        if (plain != null) return plain;
        Melt gear = GEAR.get(stack.getItem());
        if (gear == null) return null;
        // worn gear gives less (rounded down, so a melt never yields more than the gear cost): a broken piece gives nothing
        int max = stack.getMaxDamage();
        int units = max <= 0 ? gear.units() : gear.units() * Math.max(0, max - stack.getDamageValue()) / max;
        return units <= 0 ? null : new Melt(gear.metal(), units);
    }

    /** Whether the item is a piece of gear that melts down (Melts.of then depends on its wear). */
    public static boolean isGear(Item item) {
        return GEAR.containsKey(item);
    }

    private Melts() {}
}
