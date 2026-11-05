package com.simpleblocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything Simple Blocks registers: 80 blocks with their block items (plus the 15 moved from SimpleBuilding in
 * {@link com.simpleblocks.nature.NatureBlocks}), the Shapecutter ({@link com.simpleblocks.saw.Sawing}) and one creative tab. Properties are copied from a
 * Vanilla block, so hardness, sound, tool and map color match the family; the models of the missing stairs, slabs and
 * walls point at the Vanilla block textures (nothing is copied). The blocks are created once, in this class; loaders call
 * {@link #blocks}/{@link #items} in their matching registry event (Fabric: {@link #registerAll}).
 */
public final class BlocksRegistry {
    /** Block path -> block, in creative-tab order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    /** Block path -> the Vanilla block its properties came from (cracked/mossy: the brick they age). */
    public static final Map<String, Block> BASES = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static CreativeModeTab TAB;

    // --- Hushed Deepslate Tiles: dampen vibrations like wool (block tags dampens_vibrations/occludes_vibration_signals).
    public static final Block HUSHED_DEEPSLATE_TILES = add("hushed_deepslate_tiles", Blocks.DEEPSLATE_TILES, Block::new);
    public static final Block HUSHED_DEEPSLATE_TILE_STAIRS = add("hushed_deepslate_tile_stairs", Blocks.DEEPSLATE_TILES,
            s -> new StairBlock(HUSHED_DEEPSLATE_TILES.defaultBlockState(), s));
    public static final Block HUSHED_DEEPSLATE_TILE_SLAB = add("hushed_deepslate_tile_slab", Blocks.DEEPSLATE_TILES, SlabBlock::new);
    public static final Block HUSHED_DEEPSLATE_TILE_WALL = add("hushed_deepslate_tile_wall", Blocks.DEEPSLATE_TILES, s -> new WallBlock(s.forceSolidOn()));
    /** Craftable and found in Ancient City chests only; no world generation. 1-2 experience when mined without Silk Touch. */
    public static final Block SCULK_DEEPSLATE = add("sculk_deepslate", Blocks.DEEPSLATE, s -> new DropExperienceBlock(UniformInt.of(1, 2), s));
    /** Sculk Tiles (W23-3): deepslate tiles in sculk colours with a slow pulse (animated texture); a plain building block. */
    public static final Block SCULK_TILES = add("sculk_tiles", Blocks.DEEPSLATE_TILES, Block::new);

    // --- Blue nether bricks (N71): turquoise cousin of the red nether bricks, made with SimpleLib's warped wart.
    public static final Block BLUE_NETHER_BRICKS = add("blue_nether_bricks", Blocks.RED_NETHER_BRICKS, Block::new);
    public static final Block BLUE_NETHER_BRICK_STAIRS = add("blue_nether_brick_stairs", Blocks.RED_NETHER_BRICKS,
            s -> new StairBlock(BLUE_NETHER_BRICKS.defaultBlockState(), s));
    public static final Block BLUE_NETHER_BRICK_SLAB = add("blue_nether_brick_slab", Blocks.RED_NETHER_BRICKS, SlabBlock::new);
    public static final Block BLUE_NETHER_BRICK_WALL = add("blue_nether_brick_wall", Blocks.RED_NETHER_BRICKS, s -> new WallBlock(s.forceSolidOn()));

    // --- Missing stairs, slabs and walls of Vanilla blocks (textures: the Vanilla block's).
    public static final List<String> SHAPES = List.of(
            "calcite_stairs", "calcite_slab", "calcite_wall", "netherrack_stairs", "netherrack_slab", "netherrack_wall",
            "smooth_basalt_stairs", "smooth_basalt_slab", "smooth_basalt_wall", "cut_sandstone_stairs", "cut_sandstone_wall",
            "cut_red_sandstone_stairs", "cut_red_sandstone_wall", "smooth_sandstone_wall", "smooth_red_sandstone_wall",
            // a-palette05 (IG31-42): the missing members of the Vanilla families ...
            "stone_wall", "smooth_stone_stairs", "polished_granite_wall", "polished_diorite_wall", "polished_andesite_wall",
            "quartz_wall", "smooth_quartz_wall", "purpur_wall", "quartz_brick_stairs", "quartz_brick_slab", "quartz_brick_wall",
            "end_stone_stairs", "end_stone_slab", "end_stone_wall", "prismarine_brick_wall", "dark_prismarine_wall",
            // ... and the shapes of the mossy bricks (registered right behind their block, see AGED).
            "mossy_deepslate_brick_stairs", "mossy_deepslate_brick_slab", "mossy_deepslate_brick_wall",
            "mossy_deepslate_tile_stairs", "mossy_deepslate_tile_slab", "mossy_deepslate_tile_wall",
            "mossy_tuff_brick_stairs", "mossy_tuff_brick_slab", "mossy_tuff_brick_wall",
            "mossy_mud_brick_stairs", "mossy_mud_brick_slab", "mossy_mud_brick_wall",
            "mossy_end_stone_brick_stairs", "mossy_end_stone_brick_slab", "mossy_end_stone_brick_wall",
            "mossy_cinnabar_brick_stairs", "mossy_cinnabar_brick_slab", "mossy_cinnabar_brick_wall",
            "mossy_sulfur_brick_stairs", "mossy_sulfur_brick_slab", "mossy_sulfur_brick_wall");

    static {
        shapes("calcite", Blocks.CALCITE, true, true, true);
        shapes("netherrack", Blocks.NETHERRACK, true, true, true);
        shapes("smooth_basalt", Blocks.SMOOTH_BASALT, true, true, true);
        shapes("cut_sandstone", Blocks.CUT_SANDSTONE, true, false, true);
        shapes("cut_red_sandstone", Blocks.CUT_RED_SANDSTONE, true, false, true);
        shapes("smooth_sandstone", Blocks.SMOOTH_SANDSTONE, false, false, true);
        shapes("smooth_red_sandstone", Blocks.SMOOTH_RED_SANDSTONE, false, false, true);
        shapes("stone", Blocks.STONE, false, false, true);
        shapes("smooth_stone", Blocks.SMOOTH_STONE, true, false, false);
        shapes("polished_granite", Blocks.POLISHED_GRANITE, false, false, true);
        shapes("polished_diorite", Blocks.POLISHED_DIORITE, false, false, true);
        shapes("polished_andesite", Blocks.POLISHED_ANDESITE, false, false, true);
        shapes("quartz", Blocks.QUARTZ_BLOCK, false, false, true);
        shapes("smooth_quartz", Blocks.SMOOTH_QUARTZ, false, false, true);
        shapes("purpur", Blocks.PURPUR_BLOCK, false, false, true);
        shapes("quartz_brick", Blocks.QUARTZ_BRICKS, true, true, true);
        shapes("end_stone", Blocks.END_STONE, true, true, true);
        shapes("prismarine_brick", Blocks.PRISMARINE_BRICKS, false, false, true);
        shapes("dark_prismarine", Blocks.DARK_PRISMARINE, false, false, true);
    }

    /** Cracked and mossy bricks: full blocks only (no new brick sets); (new path, Vanilla brick it ages). */
    public static final List<String[]> AGED = List.of(
            new String[]{"cracked_red_nether_bricks", "red_nether_bricks"},
            new String[]{"cracked_tuff_bricks", "tuff_bricks"}, new String[]{"mossy_tuff_bricks", "tuff_bricks"},
            new String[]{"cracked_mud_bricks", "mud_bricks"}, new String[]{"mossy_mud_bricks", "mud_bricks"},
            new String[]{"cracked_end_stone_bricks", "end_stone_bricks"}, new String[]{"mossy_end_stone_bricks", "end_stone_bricks"},
            new String[]{"mossy_deepslate_bricks", "deepslate_bricks"}, new String[]{"mossy_deepslate_tiles", "deepslate_tiles"},
            new String[]{"cracked_cinnabar_bricks", "cinnabar_bricks"}, new String[]{"mossy_cinnabar_bricks", "cinnabar_bricks"},
            new String[]{"cracked_sulfur_bricks", "sulfur_bricks"}, new String[]{"mossy_sulfur_bricks", "sulfur_bricks"},
            new String[]{"cracked_prismarine", "prismarine"}, new String[]{"cracked_prismarine_bricks", "prismarine_bricks"},
            new String[]{"cracked_dark_prismarine", "dark_prismarine"}, new String[]{"cracked_polished_deepslate", "polished_deepslate"},
            new String[]{"cracked_blue_nether_bricks", "simpleblocks:blue_nether_bricks"});

    static {
        for (String[] aged : AGED) {
            // The brick it ages: Vanilla by default, "namespace:path" for our own (blue nether bricks).
            Block block = add(aged[0], agedBase(aged[1]), Block::new);
            // Mossy bricks have stairs, slab and wall (a-palette05, IG31-42), right behind them: mossy_tuff_bricks -> mossy_tuff_brick_stairs.
            if (aged[0].startsWith("mossy_")) shapes(aged[0].replace("_bricks", "_brick").replace("_tiles", "_tile"), block, true, true, true);
        }
    }

    /** The block an aged brick copies: Vanilla's, or (with a namespace) one of ours, which is not in the registry yet while this class loads. */
    public static Block agedBase(String id) {
        return id.startsWith(SimpleBlocks.MOD_ID + ":") ? BLOCKS.get(id.substring(SimpleBlocks.MOD_ID.length() + 1))
                : BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    /** Every block path in registration order. */
    public static final List<String> NAMES = List.copyOf(BLOCKS.keySet());

    private static void shapes(String base, Block block, boolean stairs, boolean slab, boolean wall) {
        if (stairs) add(base + "_stairs", block, s -> new StairBlock(block.defaultBlockState(), s));
        if (slab) add(base + "_slab", block, SlabBlock::new);
        if (wall) add(base + "_wall", block, s -> new WallBlock(s.forceSolidOn()));
    }

    private static Block add(String name, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(name));
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key));
        BLOCKS.put(name, block);
        BASES.put(name, base);
        return block;
    }

    /** Vanilla {@code Registry.register} works inside the matching registry event of every loader. */
    public static void registerBlocks() {
        BLOCKS.forEach((name, block) -> Registry.register(BuiltInRegistries.BLOCK, SimpleBlocks.id(name), block));
        com.simpleblocks.nature.NatureBlocks.registerBlocks();
        com.simpleblocks.snow.SnowBlocks.registerBlocks();
        com.simpleblocks.pale.PaleBlocks.registerBlocks();
        com.simpleblocks.saw.Sawing.registerBlock();
    }

    /** The Shapecutter's menu, recipe type and serializer (principle A4b). */
    public static void registerMenus() {
        com.simpleblocks.saw.Sawing.registerMenu();
    }

    public static void registerRecipeTypes() {
        com.simpleblocks.saw.Sawing.registerRecipeType();
    }

    public static void registerRecipeSerializers() {
        com.simpleblocks.saw.Sawing.registerRecipeSerializer();
    }

    public static void registerItems() {
        BLOCKS.forEach((name, block) -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBlocks.id(name));
            Item item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, item));
        });
        com.simpleblocks.nature.NatureBlocks.registerItems();
        com.simpleblocks.snow.SnowBlocks.registerItems();
        com.simpleblocks.pale.PaleBlocks.registerItems();
        com.simpleblocks.saw.Sawing.registerItem();
        com.simpleblocks.guide.BlocksGuide.register();
        SimpleBlocks.registerStation();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleBlocks.id("blocks"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simpleblocks.blocks"))
                .icon(() -> new ItemStack(HUSHED_DEEPSLATE_TILES))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The rising block of levitating sand and gravel. */
    public static void registerEntityTypes() {
        com.simpleblocks.nature.NatureBlocks.registerEntityTypes();
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        registerBlocks();
        registerItems();
        registerEntityTypes();
        registerMenus();
        registerRecipeTypes();
        registerRecipeSerializers();
        registerTab();
    }

    /**
     * All items in tab order (empty while the module is switched off): the saw, the own blocks, the building blocks that
     * moved here from SimpleBuilding (tab stage T2: gravity, slabs, ice and shell, froglights), frost grass (SN30-2), the pale
     * blocks of the Pale Garden (DW04B), the guide book. With
     * SimpleBuilding loaded its remaining building blocks follow behind (tab stage T1, appended by SimpleBuilding).
     */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleBlocks.on()) return out;
        out.add(new ItemStack(com.simpleblocks.saw.Sawing.SAW_ITEM));
        ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        com.simpleblocks.nature.NatureBlocks.ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        if (com.simpleblocks.snow.SnowBlocks.FROST_GRASS_ITEM != null) out.add(new ItemStack(com.simpleblocks.snow.SnowBlocks.FROST_GRASS_ITEM));
        out.addAll(com.simpleblocks.pale.PaleBlocks.tabStacks()); // DW04B: the pale stages and their waxed copies
        out.add(com.simpleblocks.guide.BlocksGuide.stack());
        return out;
    }

    /** A group of items that goes in front of a Vanilla item of a Vanilla tab ({@code after}: right behind it, in order). */
    public record Placement(boolean natural, Item before, List<String> names, boolean after) {
        public Placement(boolean natural, Item before, List<String> names) {
            this(natural, before, names, false);
        }

        public static Placement after(boolean natural, Item anchor, String... names) {
            return new Placement(natural, anchor, List.of(names), true);
        }

        public List<ItemStack> stacks() {
            return names.stream().map(n -> new ItemStack(BuiltInRegistries.ITEM.getValue(SimpleBlocks.id(n)))).toList();
        }
    }

    /**
     * Where the items also appear in the Vanilla tabs (shared switch {@code CreativeTabSettings}): each group in front of the
     * Vanilla item that follows its family, so other mods' insertions behind a Vanilla item keep their place.
     */
    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>(List.of(
                new Placement(false, Items.REINFORCED_DEEPSLATE, List.of("mossy_deepslate_bricks", "mossy_deepslate_brick_stairs", "mossy_deepslate_brick_slab", "mossy_deepslate_brick_wall",
                        "mossy_deepslate_tiles", "mossy_deepslate_tile_stairs", "mossy_deepslate_tile_slab", "mossy_deepslate_tile_wall",
                        "hushed_deepslate_tiles", "hushed_deepslate_tile_stairs", "hushed_deepslate_tile_slab", "hushed_deepslate_tile_wall",
                        "sculk_tiles")),
                new Placement(false, Items.CHISELED_TUFF_BRICKS, List.of("cracked_tuff_bricks", "mossy_tuff_bricks", "mossy_tuff_brick_stairs", "mossy_tuff_brick_slab", "mossy_tuff_brick_wall")),
                new Placement(false, Items.RESIN_BRICKS, List.of("cracked_mud_bricks", "mossy_mud_bricks", "mossy_mud_brick_stairs", "mossy_mud_brick_slab", "mossy_mud_brick_wall")),
                new Placement(false, Items.RED_SANDSTONE, List.of("cut_sandstone_stairs", "cut_sandstone_wall", "smooth_sandstone_wall")),
                new Placement(false, Items.CINNABAR, List.of("cut_red_sandstone_stairs", "cut_red_sandstone_wall", "smooth_red_sandstone_wall")),
                new Placement(false, Items.SULFUR, List.of("cracked_cinnabar_bricks", "mossy_cinnabar_bricks", "mossy_cinnabar_brick_stairs", "mossy_cinnabar_brick_slab", "mossy_cinnabar_brick_wall")),
                new Placement(false, Items.SEA_LANTERN, List.of("cracked_sulfur_bricks", "mossy_sulfur_bricks", "mossy_sulfur_brick_stairs", "mossy_sulfur_brick_slab", "mossy_sulfur_brick_wall")),
                new Placement(false, Items.NETHER_BRICKS, List.of("netherrack_stairs", "netherrack_slab", "netherrack_wall")),
                new Placement(false, Items.BASALT, List.of("cracked_red_nether_bricks", "blue_nether_bricks", "blue_nether_brick_stairs",
                        "blue_nether_brick_slab", "blue_nether_brick_wall", "cracked_blue_nether_bricks")),
                new Placement(false, Items.POLISHED_BASALT, List.of("smooth_basalt_stairs", "smooth_basalt_slab", "smooth_basalt_wall")),
                new Placement(false, Items.PURPUR_BLOCK, List.of("cracked_end_stone_bricks", "mossy_end_stone_bricks", "mossy_end_stone_brick_stairs", "mossy_end_stone_brick_slab", "mossy_end_stone_brick_wall")),
                new Placement(false, Items.SMOOTH_STONE_SLAB, List.of("smooth_stone_stairs")),
                new Placement(true, Items.TUFF, List.of("calcite_stairs", "calcite_slab", "calcite_wall")),
                new Placement(true, Items.SCULK_VEIN, List.of("sculk_deepslate"))));
        // a-palette05 (IG31-42): the missing members of the Vanilla families, each right behind the Vanilla member that precedes it.
        out.add(Placement.after(false, Items.STONE_SLAB, "stone_wall"));
        out.add(Placement.after(false, Items.POLISHED_GRANITE_SLAB, "polished_granite_wall"));
        out.add(Placement.after(false, Items.POLISHED_DIORITE_SLAB, "polished_diorite_wall"));
        out.add(Placement.after(false, Items.POLISHED_ANDESITE_SLAB, "polished_andesite_wall"));
        out.add(Placement.after(false, Items.QUARTZ_SLAB, "quartz_wall"));
        out.add(Placement.after(false, Items.SMOOTH_QUARTZ_SLAB, "smooth_quartz_wall"));
        out.add(Placement.after(false, Items.QUARTZ_BRICKS, "quartz_brick_stairs", "quartz_brick_slab", "quartz_brick_wall"));
        out.add(Placement.after(false, Items.PURPUR_SLAB, "purpur_wall"));
        out.add(Placement.after(false, Items.END_STONE, "end_stone_stairs", "end_stone_slab", "end_stone_wall"));
        out.add(Placement.after(false, Items.PRISMARINE_WALL, "cracked_prismarine"));
        out.add(Placement.after(false, Items.PRISMARINE_BRICK_SLAB, "prismarine_brick_wall", "cracked_prismarine_bricks"));
        out.add(Placement.after(false, Items.DARK_PRISMARINE_SLAB, "dark_prismarine_wall", "cracked_dark_prismarine"));
        out.add(Placement.after(false, Items.POLISHED_DEEPSLATE_WALL, "cracked_polished_deepslate"));
        // The blocks that moved from SimpleBuilding, behind their base block as SimpleBuilding placed them. With SimpleBuilding
        // loaded its search-tab chains keep placing them (by id, in its order with the octets), so only without it.
        if (!SimpleBlocks.simplebuildingLoaded) {
            out.add(Placement.after(true, Items.GRASS_BLOCK, "grass_slab"));
            out.add(Placement.after(true, Items.DIRT, "dirt_slab"));
            out.add(Placement.after(true, Items.GRAVEL, "gravel_slab", "suspended_gravel", "levitating_gravel"));
            out.add(Placement.after(true, Items.SAND, "sand_slab", "suspended_sand", "levitating_sand"));
            out.add(Placement.after(true, Items.ICE, "cracked_ice"));
            out.add(Placement.after(true, Items.PACKED_ICE, "chiseled_packed_ice", "cracked_packed_ice"));
            out.add(Placement.after(true, Items.BLUE_ICE, "chiseled_blue_ice", "cracked_blue_ice"));
            out.add(Placement.after(true, Items.BONE_BLOCK, "nautilus_shell_block"));
            out.add(Placement.after(true, Items.PEARLESCENT_FROGLIGHT, "scarlet_froglight", "aqua_froglight", "azure_froglight"));
        }
        return out;
    }

    private BlocksRegistry() {}
}
