package com.simplemaps;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Icons of map markers that come from partner mods, by public registry id only (principle 4: no class of the partner).
 * Owner 19.10.: shops (marker kind {@code "shop"} of Simple Shops' layer) carry Simple Money's bill when it is loaded;
 * without it they stay the plain diamond.
 */
public final class MapsIcons {
    public static final String SHOP_KIND = "shop", MONEY_MOD = "simplemoney";
    public static final Identifier MONEY_BILL = Identifier.fromNamespaceAndPath(MONEY_MOD, "money_bill");

    private MapsIcons() {}

    /** Simple Money's bill item, when that mod is loaded and the item registered; else empty. */
    public static Optional<Item> shopIcon() {
        if (!SimpleMaps.modLoaded.test(MONEY_MOD)) return Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(MONEY_BILL).filter(item -> item != Items.AIR);
    }

    /** Icon name of a marker kind for the web map: {@code "money_bill"} for a shop with Simple Money, else {@code "diamond"}. */
    public static String webIcon(String kind) {
        return SHOP_KIND.equals(kind) && shopIcon().isPresent() ? MONEY_BILL.getPath() : "diamond";
    }
}
