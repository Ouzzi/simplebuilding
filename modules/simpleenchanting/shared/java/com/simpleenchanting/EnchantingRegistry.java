package com.simpleenchanting;

import com.simpleenchanting.block.EnchanterBlock;
import com.simpleenchanting.block.EnchanterBlockEntity;
import com.simpleenchanting.block.ObsidianFloorBlock;
import com.simpleenchanting.menu.EnchanterMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything Simple Enchanting registers (Fabric: directly in the initializer), in this order: blocks (the two enchanters, without
 * SimpleBuilding also the building-block twins), items, block entity type, menu type, creative tab. Numbers as SimpleBuilding's
 * Astral Enchanting Table before the move (hardness 10, blast resistance 1200, light 10).
 */
public final class EnchantingRegistry {
    public static Block DARK_ENCHANTER;
    public static Block ASTRAL_ENCHANTER;
    public static Item DARK_ENCHANTER_ITEM;
    public static Item ASTRAL_ENCHANTER_ITEM;
    public static BlockEntityType<EnchanterBlockEntity> ENCHANTER_BE;
    public static MenuType<EnchanterMenu> ENCHANTER_MENU;
    public static CreativeModeTab TAB;
    /** This module's building-block twins (empty with SimpleBuilding, which owns them). */
    private static final List<Block> OWN_BLOCKS = new ArrayList<>();
    private static final List<Block> OWN_CHISELED = new ArrayList<>();

    private EnchantingRegistry() {}

    public static void registerBlocks() {
        DARK_ENCHANTER = enchanter(EnchanterStage.DARK);
        ASTRAL_ENCHANTER = enchanter(EnchanterStage.ASTRAL);
        if (SimpleEnchanting.simpleBuilding()) return;
        // Without SimpleBuilding: the building blocks the tables need (SimpleBuilding's own when it is installed, ids move along).
        OWN_BLOCKS.add(block("blazing_obsidian", Blocks.CRYING_OBSIDIAN, p -> new ObsidianFloorBlock(false, p.lightLevel(s -> 12))));
        OWN_BLOCKS.add(block("nihil_obsidian", Blocks.CRYING_OBSIDIAN, p -> new ObsidianFloorBlock(true, p.lightLevel(s -> 10))));
        OWN_BLOCKS.add(block("crimson_blazewood_planks", Blocks.CRIMSON_PLANKS, Block::new));
        OWN_BLOCKS.add(block("warped_blazewood_planks", Blocks.WARPED_PLANKS, Block::new));
        OWN_BLOCKS.add(block("crimson_blazewood_bookshelf", Blocks.BOOKSHELF, p -> new Block(p.sound(SoundType.NETHER_WOOD))));
        OWN_BLOCKS.add(block("warped_blazewood_bookshelf", Blocks.BOOKSHELF, p -> new Block(p.sound(SoundType.NETHER_WOOD))));
        // DW04C-3 (b-shelves01): the chiseled twins, Vanilla's block and block entity (the loader entry adds them to its valid blocks).
        for (String wood : new String[] {"crimson", "warped"}) {
            Block chiseled = block("chiseled_" + wood + "_blazewood_bookshelf", Blocks.CHISELED_BOOKSHELF,
                    p -> new net.minecraft.world.level.block.ChiseledBookShelfBlock(p.sound(SoundType.NETHER_WOOD)));
            OWN_BLOCKS.add(chiseled);
            OWN_CHISELED.add(chiseled);
        }
    }

    /** This module's chiseled bookshelf twins (empty with SimpleBuilding); they use Vanilla's chiseled bookshelf block entity. */
    public static List<Block> ownChiseledBookshelves() {
        return List.copyOf(OWN_CHISELED);
    }

    public static void registerItems() {
        DARK_ENCHANTER_ITEM = item("dark_enchanter", p -> new BlockItem(DARK_ENCHANTER, p.fireResistant()));
        ASTRAL_ENCHANTER_ITEM = item("astral_enchanter", p -> new BlockItem(ASTRAL_ENCHANTER, p.fireResistant()));
        for (Block block : OWN_BLOCKS) {
            boolean chiseled = OWN_CHISELED.contains(block);
            item(BuiltInRegistries.BLOCK.getKey(block).getPath(), p -> new BlockItem(block, chiseled
                    ? p.component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY) : p));
        }
        com.simpleenchanting.guide.EnchantingGuide.register();
    }

    public static void registerBlockEntity() {
        ENCHANTER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleEnchanting.id("enchanter"),
                new BlockEntityType<>(EnchanterBlockEntity::new, Set.of(DARK_ENCHANTER, ASTRAL_ENCHANTER)));
        ENCHANTER_MENU = Registry.register(BuiltInRegistries.MENU, SimpleEnchanting.id("enchanter"),
                new MenuType<>(EnchanterMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleEnchanting.id("enchanting"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simpleenchanting.enchanting"))
                .icon(() -> new ItemStack(DARK_ENCHANTER_ITEM))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The two enchanters, the building blocks of this installation (SimpleBuilding's or the twins), then the guide; empty while switched off. */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleEnchanting.on()) return out;
        out.addAll(tableStacks());
        for (String path : SimpleEnchanting.BUILDING_BLOCKS) {
            Item item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(SimpleEnchanting.buildingNamespace(), path));
            if (item != Items.AIR) out.add(new ItemStack(item));
        }
        // DW04C-3: the books of the strong shelves (SimpleBuilding) and the End bookshelves (Simple End), by id, when installed.
        for (String id : SimpleEnchanting.PARTNER_ITEMS) {
            Item item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
            if (item != Items.AIR) out.add(new ItemStack(item));
        }
        out.add(com.simpleenchanting.guide.EnchantingGuide.stack());
        return out;
    }

    /** The two enchanters (Vanilla's functional tab, after the enchanting table); empty while switched off. */
    public static List<ItemStack> tableStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleEnchanting.on()) {
            out.add(new ItemStack(DARK_ENCHANTER_ITEM));
            out.add(new ItemStack(ASTRAL_ENCHANTER_ITEM));
        }
        return out;
    }

    /** This module's building-block twins (empty with SimpleBuilding). */
    public static List<Block> ownBuildingBlocks() {
        return List.copyOf(OWN_BLOCKS);
    }

    private static Block enchanter(EnchanterStage stage) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleEnchanting.id(stage.path));
        return Registry.register(BuiltInRegistries.BLOCK, key, new EnchanterBlock(stage, BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)
                .setId(key).strength(EnchanterBlock.HARDNESS, 1200.0F).lightLevel(state -> 10)));
    }

    private static Block block(String path, Block copy, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleEnchanting.id(path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(copy).setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleEnchanting.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
