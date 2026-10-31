package com.simplefoundry;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fuels of the grate: own table (burn ticks, heat), independent of the furnace fuel components. Heat language as the crucibles. */
public final class Fuels {
    public record Fuel(int ticks, Heat heat) {}

    /** The fuel of the stack, or null when it does not burn here. */
    public static Fuel of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return new Fuel(1600, Heat.MEDIUM);
        if (stack.is(Items.COAL_BLOCK)) return new Fuel(16000, Heat.HIGH);
        if (stack.is(Items.BLAZE_ROD)) return new Fuel(2400, Heat.HIGH);
        if (stack.is(Items.LAVA_BUCKET)) return new Fuel(20000, Heat.HIGH);
        if (stack.is(ItemTags.LOGS_THAT_BURN)) return new Fuel(300, Heat.MEDIUM);
        return null;
    }

    private Fuels() {}
}
