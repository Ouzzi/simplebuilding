package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateWood;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/** Block items (crates are furnace fuel like planks, 300 ticks, not the Nether woods) and the guide book. */
public final class StorageItems {
    public static final int CRATE_FUEL = 300;
    public static final Map<CrateWood, Item> CRATES = new EnumMap<>(CrateWood.class);
    public static Item STORAGE_CRAFTING_TABLE;

    public static void register() {
        StorageBlocks.CRATES.forEach((wood, crate) -> CRATES.put(wood, blockItem(crate, wood.nether() ? 0 : CRATE_FUEL)));
        STORAGE_CRAFTING_TABLE = blockItem(StorageBlocks.STORAGE_CRAFTING_TABLE, 0);
        com.simplestorage.guide.StorageGuide.register();
    }

    private static Item blockItem(Block block, int fuel) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item.Properties properties = new Item.Properties().setId(key);
        if (fuel > 0) {
            properties = properties.component(DataComponents.COOKING_FUEL,
                    new CookingFuel(new ResolvableInt.Constant(fuel), new ResolvableFloat.Constant(1.0F)));
        }
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties));
    }

    private StorageItems() {}
}
