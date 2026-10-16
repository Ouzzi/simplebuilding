package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Items of SimpleLib (the crucible items live in Simple Crucibles): the warped wart. */
public final class LibItems {
    public static Item WARPED_WART;

    public static void register() {
        if (WARPED_WART != null) return;
        // Like the nether wart: a plain item name (not "block ..."), compostable at 65 %.
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLib.id("warped_wart"));
        WARPED_WART = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(LibBlocks.WARPED_WART,
                new Item.Properties().setId(key).useItemDescriptionPrefix()
                        .compostable(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH)));
    }

    private LibItems() {}
}
