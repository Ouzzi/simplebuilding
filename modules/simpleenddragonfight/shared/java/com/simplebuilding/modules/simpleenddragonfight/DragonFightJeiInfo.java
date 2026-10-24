package com.simplebuilding.modules.simpleenddragonfight;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * JEI24: the Dragonmite's drop (Dragon Chitin) as a JEI info page (the module has no SimpleLib and so no "Mob Drops"
 * tab), text {@code jei.simpleenddragonfight.info.dragonmite_drops} on the chitin and the spawn egg. Free of JEI classes -
 * the Fabric plugin {@code DragonFightJeiPlugin} shows it, {@code DragonFightTests} rolls the loot table against it.
 */
public final class DragonFightJeiInfo {
    public static final String PREFIX = "jei.simpleenddragonfight.info.";

    private DragonFightJeiInfo() {
    }

    /** Mob -> the items its loot table names (keep in step with {@code loot_table/entities}; the game test checks). */
    public static Map<EntityType<?>, List<Item>> drops() {
        return Map.of(DragonFightRegistry.DRAGONMITE, List.of(DragonFightRegistry.DRAGON_CHITIN));
    }

    /** Translation key -> the items that carry the page (drops + spawn egg). */
    public static Map<String, List<Item>> pages() {
        Map<String, List<Item>> out = new LinkedHashMap<>();
        out.put(PREFIX + "dragonmite_drops", List.of(DragonFightRegistry.DRAGON_CHITIN, DragonFightRegistry.DRAGONMITE_EGG));
        return out;
    }
}
