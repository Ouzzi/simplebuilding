package com.simplecrucibles.registry;

import com.simplecrucibles.SimpleCrucibles;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Block items. The crucible blank has none (it only exists in the world while being built). */
public final class CrucibleItems {
    public static Item IRON_CRUCIBLE, REINFORCED_CRUCIBLE, NETHERITE_CRUCIBLE, ENDERITE_CRUCIBLE, COPPER_BARREL, REINFORCED_BARREL, NETHERITE_BARREL, ENDERITE_BARREL, REINFORCED_CAULDRON;

    public static void register() {
        IRON_CRUCIBLE = blockItem("iron_crucible", CrucibleBlocks.IRON_CRUCIBLE, new Item.Properties());
        REINFORCED_CRUCIBLE = blockItem("reinforced_crucible", CrucibleBlocks.REINFORCED_CRUCIBLE, new Item.Properties());
        NETHERITE_CRUCIBLE = blockItem("netherite_crucible", CrucibleBlocks.NETHERITE_CRUCIBLE, new Item.Properties().fireResistant());
        ENDERITE_CRUCIBLE = blockItem("enderite_crucible", CrucibleBlocks.ENDERITE_CRUCIBLE, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        COPPER_BARREL = blockItem("copper_barrel", CrucibleBlocks.COPPER_BARREL, new Item.Properties());
        REINFORCED_BARREL = blockItem("reinforced_barrel", CrucibleBlocks.REINFORCED_BARREL, new Item.Properties());
        NETHERITE_BARREL = blockItem("netherite_barrel", CrucibleBlocks.NETHERITE_BARREL, new Item.Properties().fireResistant());
        ENDERITE_BARREL = blockItem("enderite_barrel", CrucibleBlocks.ENDERITE_BARREL, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC));
        REINFORCED_CAULDRON = blockItem("reinforced_cauldron", CrucibleBlocks.REINFORCED_CAULDRON, new Item.Properties());
        com.simplecrucibles.guide.CruciblesGuide.register();
    }

    private static Item blockItem(String name, Block block, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleCrucibles.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties.setId(key).useBlockDescriptionPrefix()));
    }

    private CrucibleItems() {}
}
