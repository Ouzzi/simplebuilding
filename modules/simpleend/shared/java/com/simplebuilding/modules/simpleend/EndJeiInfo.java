package com.simplebuilding.modules.simpleend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * JEI24: the drops of Simple End's mobs as JEI info pages (the module has no SimpleLib and so no "Mob Drops" tab): per
 * mob the items its loot table can drop plus its spawn egg, the text {@code jei.simpleend.info.<mob>_drops}. Free of
 * JEI classes - the Fabric plugin {@code EndJeiPlugin} shows it, {@code EndTests} rolls the loot tables against it.
 */
public final class EndJeiInfo {
    public static final String PREFIX = "jei.simpleend.info.";

    private EndJeiInfo() {
    }

    /** Mob -> the items its loot table names (keep in step with {@code loot_table/entities}; the game test checks). */
    public static Map<EntityType<?>, List<Item>> drops() {
        Map<EntityType<?>, List<Item>> out = new LinkedHashMap<>();
        if (EndRegistry.NIBBLEMITE != null) out.put(EndRegistry.NIBBLEMITE, List.of(Items.CHORUS_FRUIT));
        return out;
    }

    /** Translation key -> the items that carry the page (drops + spawn egg). */
    public static Map<String, List<Item>> pages() {
        Map<String, List<Item>> out = new LinkedHashMap<>();
        if (EndRegistry.NIBBLEMITE != null && EndRegistry.NIBBLEMITE_EGG != null) {
            out.put(PREFIX + "nibblemite_drops", List.of(Items.CHORUS_FRUIT, EndRegistry.NIBBLEMITE_EGG));
        }
        return out;
    }
}
