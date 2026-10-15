package com.simpleshops;

import com.simpleshops.block.DisplayCaseBlock;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlock;
import com.simpleshops.block.SalesStorageBlockEntity;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Shops registers. Loaders call the methods inside their matching registry event (Fabric: directly in the
 * initializer), in this order: blocks, items, block entity types, menus.
 */
public final class ShopsRegistry {
    /** Blast resistance of a placed shop: Bedrock's, so no explosion (also Wither, Ender Dragon breath, Respawn Anchor) touches it. */
    public static final float BLAST_RESISTANCE = 3_600_000.0F;
    public static final float HARDNESS = 2.0F;

    public static Block SALES_BLOCK, DISPLAY_CASE, GLOW_DISPLAY_CASE, SALES_STORAGE;
    public static Item SALES_BLOCK_ITEM, DISPLAY_CASE_ITEM, GLOW_DISPLAY_CASE_ITEM, SALES_STORAGE_ITEM;
    public static BlockEntityType<SalesBlockEntity> SALES_BE;
    public static BlockEntityType<DisplayCaseBlockEntity> CASE_BE;
    public static BlockEntityType<SalesStorageBlockEntity> STORAGE_BE;
    public static MenuType<SalesOwnerMenu> OWNER_MENU;
    public static MenuType<ShopBuyerMenu> BUYER_MENU;
    public static MenuType<StockMenu> STOCK_MENU;
    /** Owner screen with the display case above (goods, price, earnings); same class as {@link #OWNER_MENU}, other layout. */
    public static MenuType<SalesOwnerMenu> SHOP_MENU;
    public static MenuType<StorageMenu> STORAGE_MENU;

    private static BlockBehaviour.Properties base(MapColor color) {
        // pushReaction BLOCK: pistons and slime never move or pull a shop; no ignitedByLava/flammable entry: fire and lava leave it alone.
        return BlockBehaviour.Properties.of().mapColor(color).strength(HARDNESS, BLAST_RESISTANCE).pushReaction(PushReaction.IMMOVEABLE)
                .sound(SoundType.WOOD).instrument(NoteBlockInstrument.BASS);
    }

    public static void registerBlocks() {
        SALES_BLOCK = block("sales_block", SalesBlock::new, base(MapColor.WOOD));
        SALES_STORAGE = block("sales_storage", SalesStorageBlock::new, base(MapColor.WOOD));
        DISPLAY_CASE = block("display_case", p -> new DisplayCaseBlock(p, false),
                base(MapColor.WOOD).noOcclusion().sound(SoundType.GLASS).isSuffocating((s, l, pos) -> false));
        GLOW_DISPLAY_CASE = block("glow_display_case", p -> new DisplayCaseBlock(p, true),
                base(MapColor.WOOD).noOcclusion().sound(SoundType.GLASS).lightLevel(s -> 6).isSuffocating((s, l, pos) -> false));
    }

    public static void registerItems() {
        SALES_BLOCK_ITEM = item("sales_block", p -> new BlockItem(SALES_BLOCK, p), new Item.Properties().useBlockDescriptionPrefix());
        SALES_STORAGE_ITEM = item("sales_storage", p -> new BlockItem(SALES_STORAGE, p), new Item.Properties().useBlockDescriptionPrefix());
        DISPLAY_CASE_ITEM = item("display_case", p -> new BlockItem(DISPLAY_CASE, p), new Item.Properties().useBlockDescriptionPrefix());
        GLOW_DISPLAY_CASE_ITEM = item("glow_display_case", p -> new BlockItem(GLOW_DISPLAY_CASE, p), new Item.Properties().useBlockDescriptionPrefix());
        com.simpleshops.guide.ShopsGuide.register();
    }

    public static void registerBlockEntities(BlockEntityType<SalesBlockEntity> sales, BlockEntityType<DisplayCaseBlockEntity> cases,
                                              BlockEntityType<SalesStorageBlockEntity> storages) {
        SALES_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleShops.id("sales_block"), sales);
        CASE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleShops.id("display_case"), cases);
        STORAGE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleShops.id("sales_storage"), storages);
    }

    public static void registerMenus() {
        OWNER_MENU = Registry.register(BuiltInRegistries.MENU, SimpleShops.id("sales_owner"), new MenuType<>(SalesOwnerMenu::new, FeatureFlags.VANILLA_SET));
        BUYER_MENU = Registry.register(BuiltInRegistries.MENU, SimpleShops.id("buyer"), new MenuType<>(ShopBuyerMenu::new, FeatureFlags.VANILLA_SET));
        STOCK_MENU = Registry.register(BuiltInRegistries.MENU, SimpleShops.id("stock"), new MenuType<>(StockMenu::new, FeatureFlags.VANILLA_SET));
        SHOP_MENU = Registry.register(BuiltInRegistries.MENU, SimpleShops.id("shop"), new MenuType<>(SalesOwnerMenu::combinedClient, FeatureFlags.VANILLA_SET));
        STORAGE_MENU = Registry.register(BuiltInRegistries.MENU, SimpleShops.id("storage"), new MenuType<>(StorageMenu::new, FeatureFlags.VANILLA_SET));
    }

    /** The block entity types' valid blocks, for the loader adapters. */
    public static java.util.Set<Block> salesBlocks() {
        return java.util.Set.of(SALES_BLOCK);
    }

    public static java.util.Set<Block> storageBlocks() {
        return java.util.Set.of(SALES_STORAGE);
    }

    public static java.util.Set<Block> caseBlocks() {
        return java.util.Set.of(DISPLAY_CASE, GLOW_DISPLAY_CASE);
    }

    /** Creative tab stacks (empty while the module is switched off): the shop blocks after the Vanilla item frames. */
    public static List<ItemStack> tabStacks() {
        if (!SimpleShops.on()) return List.of();
        return List.of(new ItemStack(SALES_BLOCK_ITEM), new ItemStack(SALES_STORAGE_ITEM), new ItemStack(DISPLAY_CASE_ITEM), new ItemStack(GLOW_DISPLAY_CASE_ITEM));
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleShops.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleShops.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    private ShopsRegistry() {}
}
