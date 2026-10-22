package com.simplemusic.registry;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.BlankDiscItem;
import com.simplemusic.disc.CustomDiscItem;
import com.simplemusic.orchestrion.OrchestrionBlock;
import com.simplemusic.orchestrion.OrchestrionBlockEntity;
import com.simplemusic.pocket.PocketJukeboxBlock;
import com.simplemusic.pocket.PocketJukeboxBlockEntity;
import com.simplemusic.pocket.PocketJukeboxItem;
import com.simplemusic.press.DiscPressBlock;
import com.simplemusic.press.DiscPressBlockEntity;
import com.simplemusic.press.DiscPressMenu;
import com.simplemusic.sheet.SheetMusicItem;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;

/**
 * Everything Simple Music registers. Loaders call the methods inside their matching registry event (Fabric: directly
 * in the initializer), in this order: components, blocks, items, block entity type, tab.
 */
public final class MusicRegistry {
    public static Block ORCHESTRION, DISC_PRESS;
    public static Item SHEET_MUSIC, ORCHESTRION_ITEM, BLANK_DISC, CUSTOM_DISC, DISC_PRESS_ITEM;
    public static BlockEntityType<OrchestrionBlockEntity> ORCHESTRION_BE;
    public static BlockEntityType<DiscPressBlockEntity> DISC_PRESS_BE;
    public static BlockEntityType<PocketJukeboxBlockEntity> POCKET_JUKEBOX_BE;
    public static MenuType<DiscPressMenu> DISC_PRESS_MENU;
    /** Undyed first, then the 16 colours in {@link DyeColor} order (like Vanilla's bundles). */
    public static final List<Item> POCKET_JUKEBOXES = new ArrayList<>();
    /** The placed Pocket Jukeboxes, same ids and order as {@link #POCKET_JUKEBOXES}. */
    public static final List<Block> POCKET_JUKEBOX_BLOCKS = new ArrayList<>();

    /** {@code pocket_jukebox} or {@code <colour>_pocket_jukebox}. */
    public static String pocketJukeboxId(DyeColor color) {
        return color == null ? "pocket_jukebox" : color.getSerializedName() + "_pocket_jukebox";
    }

    public static void registerBlocks() {
        ORCHESTRION = block("orchestrion", OrchestrionBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F, 6.0F).sound(SoundType.WOOD).ignitedByLava());
        // like Vanilla's stonecutter/blast furnace: stone and iron, needs a pickaxe
        DISC_PRESS = block("disc_press", DiscPressBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.METAL));
        // a small case like a placed backpack: no tool, breaks fast, pistons knock it off (it drops with its discs)
        POCKET_JUKEBOX_BLOCKS.add(pocketBlock(null));
        for (DyeColor color : DyeColor.values()) POCKET_JUKEBOX_BLOCKS.add(pocketBlock(color));
    }

    private static Block pocketBlock(DyeColor color) {
        return block(pocketJukeboxId(color), p -> new PocketJukeboxBlock(p, color), BlockBehaviour.Properties.of()
                .mapColor(color == null ? MapColor.TERRACOTTA_ORANGE : color.getMapColor()).strength(0.8F).sound(SoundType.WOOL)
                .noOcclusion().pushReaction(PushReaction.POPPED));
    }

    public static void registerItems() {
        SHEET_MUSIC = item("sheet_music", SheetMusicItem::new, new Item.Properties().stacksTo(16));
        ORCHESTRION_ITEM = item("orchestrion", p -> new BlockItem(ORCHESTRION, p), new Item.Properties().useBlockDescriptionPrefix());
        DISC_PRESS_ITEM = item("disc_press", p -> new BlockItem(DISC_PRESS, p), new Item.Properties().useBlockDescriptionPrefix());
        BLANK_DISC = item("blank_disc", BlankDiscItem::new, new Item.Properties().stacksTo(16));
        CUSTOM_DISC = item("custom_disc", CustomDiscItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        POCKET_JUKEBOXES.add(item(pocketJukeboxId(null), p -> new PocketJukeboxItem(POCKET_JUKEBOX_BLOCKS.get(0), p, null), new Item.Properties()));
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            Block block = POCKET_JUKEBOX_BLOCKS.get(i + 1);
            DyeColor color = colors[i];
            POCKET_JUKEBOXES.add(item(pocketJukeboxId(color), p -> new PocketJukeboxItem(block, p, color), new Item.Properties()));
        }
        com.simplemusic.guide.MusicGuide.register();
    }

    public static void registerBlockEntities(BlockEntityType<OrchestrionBlockEntity> orchestrion, BlockEntityType<DiscPressBlockEntity> press,
                                             BlockEntityType<PocketJukeboxBlockEntity> pocket) {
        ORCHESTRION_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleMusic.id("orchestrion"), orchestrion);
        DISC_PRESS_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleMusic.id("disc_press"), press);
        POCKET_JUKEBOX_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleMusic.id("pocket_jukebox"), pocket);
    }

    public static void registerMenus() {
        DISC_PRESS_MENU = Registry.register(BuiltInRegistries.MENU, SimpleMusic.id("disc_press"),
                new MenuType<>(DiscPressMenu::client, FeatureFlags.VANILLA_SET));
    }

    /** The 17 Pocket Jukeboxes for the Vanilla tools tab (empty while the module is switched off). */
    public static List<ItemStack> toolStacks() {
        if (!SimpleMusic.on()) return List.of();
        return POCKET_JUKEBOXES.stream().map(ItemStack::new).toList();
    }

    /** Creative tab stacks (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        if (!SimpleMusic.on()) return List.of();
        return List.of(new ItemStack(ORCHESTRION_ITEM), new ItemStack(DISC_PRESS_ITEM), new ItemStack(SHEET_MUSIC), new ItemStack(BLANK_DISC));
    }

    public static void registerTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleMusic.id("music"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplemusic.music"))
                        .icon(() -> new ItemStack(ORCHESTRION_ITEM))
                        .displayItems((params, out) -> {
                            if (!SimpleMusic.on()) return;
                            out.accept(com.simplemusic.guide.MusicGuide.stack());
                            tabStacks().forEach(out::accept);
                            toolStacks().forEach(out::accept);
                        }).build());
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMusic.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMusic.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    private MusicRegistry() {}
}
