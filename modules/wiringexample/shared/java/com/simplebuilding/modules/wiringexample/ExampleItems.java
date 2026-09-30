package com.simplebuilding.modules.wiringexample;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** A plain test token, deliberately without gameplay behavior or creative tabs. */
public final class ExampleItems {
    public static void register() {
        var id = Identifier.fromNamespaceAndPath("wiringexample", "token");
        Registry.register(BuiltInRegistries.ITEM, id,
                new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }
    private ExampleItems() {}
}
