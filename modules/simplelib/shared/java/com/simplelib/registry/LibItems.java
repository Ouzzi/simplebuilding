package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Block items. The crucible blank has none (it only exists in the world while being built). */
public final class LibItems {
    public static Item IRON_CRUCIBLE, REINFORCED_CRUCIBLE, NETHERITE_CRUCIBLE;

    public static void register() {
        IRON_CRUCIBLE = blockItem("iron_crucible", LibBlocks.IRON_CRUCIBLE, new Item.Properties());
        REINFORCED_CRUCIBLE = blockItem("reinforced_crucible", LibBlocks.REINFORCED_CRUCIBLE, new Item.Properties());
        NETHERITE_CRUCIBLE = blockItem("netherite_crucible", LibBlocks.NETHERITE_CRUCIBLE, new Item.Properties().fireResistant());
    }

    private static Item blockItem(String name, Block block, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLib.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties.setId(key).useBlockDescriptionPrefix()));
    }

    private LibItems() {}
}
