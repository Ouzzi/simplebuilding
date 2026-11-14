package com.simplebuilding.util;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** 26.2 twin of the 26.3 class (no SimpleLib on 26.2): the shared materials stay SimpleBuilding's own registrations. */
public final class SharedMaterials {
    public static final List<String> GROUPS = List.of();

    private SharedMaterials() {
    }

    public static boolean isShared(Item item) {
        return false;
    }

    public static boolean isShared(Block block) {
        return false;
    }

    public static Item item(String id, Supplier<Item> own) {
        return own.get();
    }

    public static Block block(String id, Supplier<Block> own) {
        return own.get();
    }

    /** 26.3 only (the line in Simple Ores); 26.2 never calls it (McVersion.END_SYSTEMS off). */
    public static Block oresBlock(String id) {
        throw new IllegalStateException("26.2 has no Simple Ores line block " + id);
    }

    public static Item oresItem(String id) {
        throw new IllegalStateException("26.2 has no Simple Ores line item " + id);
    }

    public static void request() {
    }
}
