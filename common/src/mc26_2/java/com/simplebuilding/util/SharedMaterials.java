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

    public static Item item(String id, Supplier<Item> own) {
        return own.get();
    }

    public static Block block(String id, Supplier<Block> own) {
        return own.get();
    }

    public static void request() {
    }
}
