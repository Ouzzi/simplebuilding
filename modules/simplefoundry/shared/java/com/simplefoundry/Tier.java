package com.simplefoundry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The four desk tiers (owner SA3 = A: 3 / 6 / 9 / 12 input slots). Until the desk screen exists (FOUNDRY32-UI open) the slots show as
 * melt lanes: every metal that waits melts {@link #lanes} units per melt interval, so a higher tier works through the same load faster.
 * A tier is bought by right click on the desk with the upgrade item (one piece, taken).
 */
public enum Tier {
    STONE(3, 1, null),
    IRON(6, 2, Items.IRON_BLOCK),
    DIAMOND(9, 3, Items.DIAMOND_BLOCK),
    NETHERITE(12, 4, Items.NETHERITE_INGOT);

    public final int slots;
    public final int lanes;
    private final Item upgrade;

    Tier(int slots, int lanes, Item upgrade) {
        this.slots = slots;
        this.lanes = lanes;
        this.upgrade = upgrade;
    }

    /** The item that lifts a desk of the tier below to this one (null for the first tier). */
    public Item upgradeItem() {
        return upgrade;
    }

    public Tier next() {
        return this == NETHERITE ? null : values()[ordinal() + 1];
    }

    public static Tier byIndex(int index) {
        return values()[Math.max(0, Math.min(values().length - 1, index))];
    }
}
