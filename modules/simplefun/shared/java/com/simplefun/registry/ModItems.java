package com.simplefun.registry;

import com.simplefun.Constants;
import com.simplefun.item.BrickSnowballItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final ResourceKey<Item> BRICK_SNOWBALL_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "brick_snowball"));

    public static final Item BRICK_SNOWBALL =
            new BrickSnowballItem(new Item.Properties().stacksTo(16).setId(BRICK_SNOWBALL_KEY));
}
