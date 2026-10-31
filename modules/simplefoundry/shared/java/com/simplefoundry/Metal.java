package com.simplefoundry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The melts of stage 1, in density order from the bottom of the tank up (gold, copper, iron). 1 ingot = 9 units, 1 block = 81. */
public enum Metal {
    GOLD("gold", Heat.MEDIUM),
    COPPER("copper", Heat.MEDIUM),
    IRON("iron", Heat.HIGH);

    /** Units of one ingot (a nugget is 1, a block 81). */
    public static final int INGOT = 9;
    /** Units one melting chamber holds: 27 ingots (concept S8). */
    public static final int CHAMBER_UNITS = 27 * INGOT;

    public final String id;
    public final Heat heat;

    Metal(String id, Heat heat) {
        this.id = id;
        this.heat = heat;
    }

    public Item ingot() {
        return switch (this) {
            case GOLD -> Items.GOLD_INGOT;
            case COPPER -> Items.COPPER_INGOT;
            case IRON -> Items.IRON_INGOT;
        };
    }
}
