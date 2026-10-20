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
    public static Item REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST;
    public static Item REINFORCED_TRAPPED_CHEST, NETHERITE_TRAPPED_CHEST, ENDERITE_TRAPPED_CHEST;
    /** The eight trapped copper chests in {@link StorageBlocks#trappedCopperChests} order. */
    public static final java.util.List<Item> TRAPPED_COPPER_CHESTS = new java.util.ArrayList<>();
    public static Item REINFORCED_SHULKER_BOX, NETHERITE_SHULKER_BOX, ENDERITE_SHULKER_BOX;

    public static void register() {
        StorageBlocks.CRATES.forEach((wood, crate) -> CRATES.put(wood, blockItem(crate, wood.nether() ? 0 : CRATE_FUEL)));
        STORAGE_CRAFTING_TABLE = blockItem(StorageBlocks.STORAGE_CRAFTING_TABLE, 0);
        // Split stage 8b, same rarities as in SimpleBuilding: Netherite uncommon, Enderite epic, both fire-proof.
        REINFORCED_CHEST = chest(StorageBlocks.REINFORCED_CHEST, p -> p);
        NETHERITE_CHEST = chest(StorageBlocks.NETHERITE_CHEST, StorageItems::netherite);
        ENDERITE_CHEST = chest(StorageBlocks.ENDERITE_CHEST, StorageItems::enderite);
        REINFORCED_TRAPPED_CHEST = chest(StorageBlocks.REINFORCED_TRAPPED_CHEST, p -> p);
        NETHERITE_TRAPPED_CHEST = chest(StorageBlocks.NETHERITE_TRAPPED_CHEST, StorageItems::netherite);
        ENDERITE_TRAPPED_CHEST = chest(StorageBlocks.ENDERITE_TRAPPED_CHEST, StorageItems::enderite);
        for (Block chest : StorageBlocks.trappedCopperChests()) TRAPPED_COPPER_CHESTS.add(blockItem(chest, 0));
        REINFORCED_SHULKER_BOX = shulker(StorageBlocks.REINFORCED_SHULKER_BOX, p -> p);
        NETHERITE_SHULKER_BOX = shulker(StorageBlocks.NETHERITE_SHULKER_BOX, StorageItems::netherite);
        ENDERITE_SHULKER_BOX = shulker(StorageBlocks.ENDERITE_SHULKER_BOX, StorageItems::enderite);
        com.simplestorage.guide.StorageGuide.register();
    }

    private static Item.Properties netherite(Item.Properties p) {
        return p.fireResistant().rarity(net.minecraft.world.item.Rarity.UNCOMMON);
    }

    private static Item.Properties enderite(Item.Properties p) {
        return p.fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC);
    }

    private static Item chest(Block block, java.util.function.UnaryOperator<Item.Properties> extra) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        return Registry.register(BuiltInRegistries.ITEM, key, new com.simplestorage.chest.TieredChestItem(block, extra.apply(new Item.Properties().setId(key))));
    }

    /** One per slot like Vanilla's box, with an empty {@code minecraft:container} so that it counts as a container item. */
    private static Item shulker(Block block, java.util.function.UnaryOperator<Item.Properties> extra) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item.Properties p = extra.apply(new Item.Properties().setId(key)).stacksTo(1)
                .component(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        return Registry.register(BuiltInRegistries.ITEM, key, new com.simplestorage.chest.TieredShulkerBoxItem(block, p));
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
