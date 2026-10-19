package com.simplestorage.chest;


/**
 * What a tiered chest menu needs to know on the client: the tier (slots, stack factor) and whether it is a double chest
 * (columns and rows, see {@link ChestTier}) - or a tier shulker box (always single, its slots take no shulker boxes).
 * Each combination has its own menu type ({@code StorageMenus}, like Vanilla's {@code GENERIC_9x*}), so no loader needs
 * extra opening data.
 */
public record TieredChestOpenData(int tierId, boolean isDouble, boolean isShulkerBox) {
    public static TieredChestOpenData of(ChestTier tier, boolean isDouble) {
        return new TieredChestOpenData(tier.ordinal(), isDouble, false);
    }

    /** A tier shulker box: always single, and its slots refuse what vanilla's shulker slots refuse. */
    public static TieredChestOpenData shulker(ChestTier tier) {
        return new TieredChestOpenData(tier.ordinal(), false, true);
    }

    public ChestTier tier() {
        return ChestTier.byId(this.tierId);
    }
}
