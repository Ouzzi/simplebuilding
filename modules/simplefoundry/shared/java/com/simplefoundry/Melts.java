package com.simplefoundry;

import java.util.HashMap;
import java.util.Map;
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
        add(Metal.COPPER, 81, Items.COPPER_BLOCK, Items.RAW_COPPER_BLOCK);
    }

    private static void add(Metal metal, int units, Item... items) {
        for (Item item : items) TABLE.put(item, new Melt(metal, units));
    }

    /** The melt of the stack's item, or null when it does not melt here. */
    public static Melt of(ItemStack stack) {
        return stack.isEmpty() ? null : TABLE.get(stack.getItem());
    }

    private Melts() {}
}
