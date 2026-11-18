package com.simpleblocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import com.simpleblocks.nature.IceShapes;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Everything Simple Blocks registers: 158 blocks with their block items (+ the 14 aged/waxed copper stages of
 * {@link com.simpleblocks.metal.MetalBlocks}) (plus the 20 nature blocks, 15 of them moved from SimpleBuilding, in
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
    /** PLAN40 simpleblocks-3: stairs, slab and wall of the sculk tiles, like the hushed tiles' shapes (the octet comes from Simple Octets). */
    public static final Block SCULK_TILE_STAIRS = add("sculk_tile_stairs", Blocks.DEEPSLATE_TILES,
            s -> new StairBlock(SCULK_TILES.defaultBlockState(), s));
    public static final Block SCULK_TILE_SLAB = add("sculk_tile_slab", Blocks.DEEPSLATE_TILES, SlabBlock::new);
    public static final Block SCULK_TILE_WALL = add("sculk_tile_wall", Blocks.DEEPSLATE_TILES, s -> new WallBlock(s.forceSolidOn()));

    // --- Blue nether bricks (N71): turquoise cousin of the red nether bricks, made with SimpleLib's warped wart.
    public static final Block BLUE_NETHER_BRICKS = add("blue_nether_bricks", Blocks.RED_NETHER_BRICKS, Block::new);
    public static final Block BLUE_NETHER_BRICK_STAIRS = add("blue_nether_brick_stairs", Blocks.RED_NETHER_BRICKS,
            s -> new StairBlock(BLUE_NETHER_BRICKS.defaultBlockState(), s));
    public static final Block BLUE_NETHER_BRICK_SLAB = add("blue_nether_brick_slab", Blocks.RED_NETHER_BRICKS, SlabBlock::new);
    public static final Block BLUE_NETHER_BRICK_WALL = add("blue_nether_brick_wall", Blocks.RED_NETHER_BRICKS, s -> new WallBlock(s.forceSolidOn()));
    /**
     * PLAN40 simpleblocks-3: the chiseled member, like the chiseled red nether bricks (2 slabs on top of each other, stonecutter 1:1),
     * right behind its family; own texture (tools/textures/blue_chiseled_2026_11_12.py). No fence: Vanilla's red nether bricks have none.
     */
    public static final Block CHISELED_BLUE_NETHER_BRICKS = add("chiseled_blue_nether_bricks", Blocks.RED_NETHER_BRICKS, Block::new);

    // --- Missing stairs, slabs and walls of Vanilla blocks (textures: the Vanilla block's).
    public static final List<String> SHAPES = List.of(
            "calcite_stairs", "calcite_slab", "calcite_wall", "netherrack_stairs", "netherrack_slab", "netherrack_wall",
            "smooth_basalt_stairs", "smooth_basalt_slab", "smooth_basalt_wall", "cut_sandstone_stairs", "cut_sandstone_wall",
            "cut_red_sandstone_stairs", "cut_red_sandstone_wall", "smooth_sandstone_wall", "smooth_red_sandstone_wall",
            // a-palette05 (IG31-42): the missing members of the Vanilla families ...
            "stone_wall", "smooth_stone_stairs", "smooth_stone_wall", "polished_granite_wall", "polished_diorite_wall", "polished_andesite_wall",
            "quartz_wall", "smooth_quartz_wall", "purpur_wall", "quartz_brick_stairs", "quartz_brick_slab", "quartz_brick_wall",
            "end_stone_stairs", "end_stone_slab", "end_stone_wall", "prismarine_brick_wall", "dark_prismarine_wall",
            // ... and the shapes of the mossy bricks (registered right behind their block, see AGED).
            "mossy_deepslate_brick_stairs", "mossy_deepslate_brick_slab", "mossy_deepslate_brick_wall",
            "mossy_deepslate_tile_stairs", "mossy_deepslate_tile_slab", "mossy_deepslate_tile_wall",
            "mossy_tuff_brick_stairs", "mossy_tuff_brick_slab", "mossy_tuff_brick_wall",
            "mossy_mud_brick_stairs", "mossy_mud_brick_slab", "mossy_mud_brick_wall",
            "mossy_end_stone_brick_stairs", "mossy_end_stone_brick_slab", "mossy_end_stone_brick_wall",
            "mossy_cinnabar_brick_stairs", "mossy_cinnabar_brick_slab", "mossy_cinnabar_brick_wall",
            "mossy_sulfur_brick_stairs", "mossy_sulfur_brick_slab", "mossy_sulfur_brick_wall",
            // PAL14 (owner 09.11.): stairs, slab and wall of the three ices (registered with the ice family below).
            "ice_stairs", "ice_slab", "ice_wall", "packed_ice_stairs", "packed_ice_slab", "packed_ice_wall",
            "blue_ice_stairs", "blue_ice_slab", "blue_ice_wall",
            // a-icebricks38 (owner 12.11.): stairs, slab and wall of the ice bricks (registered with their bricks below).
            "ice_brick_stairs", "ice_brick_slab", "ice_brick_wall", "packed_ice_brick_stairs", "packed_ice_brick_slab", "packed_ice_brick_wall",
            "blue_ice_brick_stairs", "blue_ice_brick_slab", "blue_ice_brick_wall",
            // N77 (owner 16.11.): the concrete walls (registered with the Vanilla shapes above).
            "white_concrete_wall", "orange_concrete_wall", "magenta_concrete_wall", "light_blue_concrete_wall", "yellow_concrete_wall",
            "lime_concrete_wall", "pink_concrete_wall", "gray_concrete_wall", "light_gray_concrete_wall", "cyan_concrete_wall",
            "purple_concrete_wall", "blue_concrete_wall", "brown_concrete_wall", "green_concrete_wall", "red_concrete_wall", "black_concrete_wall");

    static {
        shapes("calcite", Blocks.CALCITE, true, true, true);
        shapes("netherrack", Blocks.NETHERRACK, true, true, true);
        shapes("smooth_basalt", Blocks.SMOOTH_BASALT, true, true, true);
        shapes("cut_sandstone", Blocks.CUT_SANDSTONE, true, false, true);
        shapes("cut_red_sandstone", Blocks.CUT_RED_SANDSTONE, true, false, true);
        shapes("smooth_sandstone", Blocks.SMOOTH_SANDSTONE, false, false, true);
        shapes("smooth_red_sandstone", Blocks.SMOOTH_RED_SANDSTONE, false, false, true);
        shapes("stone", Blocks.STONE, false, false, true);
        shapes("smooth_stone", Blocks.SMOOTH_STONE, true, false, true);
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
        // N77 (owner 16.11.2026, "Beton-Mauer"): a wall of every concrete colour - 26.3 has concrete stairs and slabs, no walls.
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            shapes(color.getName() + "_concrete", Blocks.CONCRETE.pick(color), false, false, true);
        }
    }

    /**
     * N89b (red cell, owner N77 "alles was fehlt"): the nether brick fence gate Vanilla lacks. Properties of the nether brick fence
     * (hardness, blast resistance, pickaxe, sound, map colour); open/close sounds of the Nether wood gates. Not in {@code #fence_gates}:
     * Vanilla puts that tag into {@code mineable/axe}, and the nether brick fence is pickaxe-only too. Fences still connect (by class).
     */
    public static final Block NETHER_BRICK_FENCE_GATE = add("nether_brick_fence_gate", Blocks.NETHER_BRICK_FENCE,
            s -> new FenceGateBlock(WoodType.CRIMSON, s.forceSolidOn()) {
                // The WoodType constructor sets the Nether wood sound; the gate sounds like nether bricks when placed, broken, walked on.
                @Override
                protected net.minecraft.world.level.block.SoundType getSoundType(net.minecraft.world.level.block.state.BlockState state) {
                    return net.minecraft.world.level.block.SoundType.NETHER_BRICKS;
                }
            });

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

    /** Chiseled cubes (a-pal09): (new path, Vanilla block it is cut from and whose properties it copies). */
    public static final List<String[]> CHISELED = List.of(
            new String[]{"chiseled_polished_andesite", "polished_andesite"}, new String[]{"chiseled_polished_diorite", "polished_diorite"},
            new String[]{"chiseled_polished_granite", "polished_granite"}, new String[]{"chiseled_prismarine", "prismarine"},
            new String[]{"chiseled_red_nether_bricks", "red_nether_bricks"},
            new String[]{"chiseled_dark_prismarine", "dark_prismarine"}); // PAL14

    static {
        for (String[] chiseled : CHISELED) add(chiseled[0], agedBase(chiseled[1]), Block::new);
    }

    // --- PAL14 (owner 09.11., docs/ai/PLAN-B-PAL14-2026-11-09.md): the missing members of the Vanilla families.
    /** Sandstone pillars like the quartz pillar: two sandstone on top of each other, ends show the sandstone top. */
    public static final Block SANDSTONE_PILLAR = add("sandstone_pillar", Blocks.SANDSTONE, RotatedPillarBlock::new);
    public static final Block RED_SANDSTONE_PILLAR = add("red_sandstone_pillar", Blocks.RED_SANDSTONE, RotatedPillarBlock::new);
    /**
     * Ice families: polished ice and ice bricks of each ice, chiseled ice, and stairs/slab/wall of each ice. Everything copies its
     * ice (slipperiness, sound, hardness, see-through ice). Only the members of normal ice melt like Vanilla ice (full blocks are
     * {@link IceBlock}s, shapes {@link IceShapes}); packed and blue ice do not melt in Vanilla, so neither do theirs.
     */
    public static final Block CHISELED_ICE = add("chiseled_ice", Blocks.ICE, IceBlock::new);
    public static final Block POLISHED_ICE = add("polished_ice", Blocks.ICE, IceBlock::new);
    public static final Block ICE_BRICKS = add("ice_bricks", Blocks.ICE, IceBlock::new);
    /** a-icebricks38 (owner 12.11.): the ice bricks are a family of their own - cracked bricks and the bricks' stairs/slab/wall. */
    public static final Block CRACKED_ICE_BRICKS = add("cracked_ice_bricks", Blocks.ICE, IceBlock::new);

    static {
        iceBrickShapes("ice", Blocks.ICE, ICE_BRICKS);
    }

    static {
        add("ice_stairs", Blocks.ICE, s -> new IceShapes.Stairs(Blocks.ICE.defaultBlockState(), s));
        add("ice_slab", Blocks.ICE, IceShapes.Slab::new);
        add("ice_wall", Blocks.ICE, IceShapes.Wall::new);
    }

    public static final Block POLISHED_PACKED_ICE = add("polished_packed_ice", Blocks.PACKED_ICE, Block::new);
    public static final Block PACKED_ICE_BRICKS = add("packed_ice_bricks", Blocks.PACKED_ICE, Block::new);
    public static final Block CRACKED_PACKED_ICE_BRICKS = add("cracked_packed_ice_bricks", Blocks.PACKED_ICE, Block::new);

    static {
        iceBrickShapes("packed_ice", Blocks.PACKED_ICE, PACKED_ICE_BRICKS);
    }

    static {
        shapes("packed_ice", Blocks.PACKED_ICE, true, true, true);
    }

    public static final Block POLISHED_BLUE_ICE = add("polished_blue_ice", Blocks.BLUE_ICE, Block::new);
    public static final Block BLUE_ICE_BRICKS = add("blue_ice_bricks", Blocks.BLUE_ICE, Block::new);
    public static final Block CRACKED_BLUE_ICE_BRICKS = add("cracked_blue_ice_bricks", Blocks.BLUE_ICE, Block::new);

    static {
        iceBrickShapes("blue_ice", Blocks.BLUE_ICE, BLUE_ICE_BRICKS);
    }

    static {
        shapes("blue_ice", Blocks.BLUE_ICE, true, true, true);
    }

    /** The PAL14 ice members and the a-icebricks38 brick families (loot: Silk Touch only, like Vanilla ice). */
    public static final List<String> ICE = List.of("chiseled_ice", "polished_ice", "ice_bricks", "ice_stairs", "ice_slab", "ice_wall",
            "polished_packed_ice", "packed_ice_bricks", "packed_ice_stairs", "packed_ice_slab", "packed_ice_wall",
            "polished_blue_ice", "blue_ice_bricks", "blue_ice_stairs", "blue_ice_slab", "blue_ice_wall",
            "cracked_ice_bricks", "ice_brick_stairs", "ice_brick_slab", "ice_brick_wall",
            "cracked_packed_ice_bricks", "packed_ice_brick_stairs", "packed_ice_brick_slab", "packed_ice_brick_wall",
            "cracked_blue_ice_bricks", "blue_ice_brick_stairs", "blue_ice_brick_slab", "blue_ice_brick_wall");

    // --- PAL35b (owner 11./12.11., docs/ai/PLAN-A-PAL35-2026-11-12.md part b): the metal row of the Vanilla metals and ore materials.
    /**
     * (new path, Vanilla block whose properties it copies): copper tiles and pillar (the base stage of SimpleLib aging chains, see
     * {@link com.simpleblocks.metal.MetalBlocks}), the family of gold and iron like the Simple Ores metals (cut block with stairs and slab,
     * tiles, pillar, chiseled) and the chiseled storage blocks of the other ore materials (chiseled redstone powers like the redstone block).
     */
    public static final List<String[]> METALS = List.of(
            new String[]{"copper_tiles", "cut_copper"}, new String[]{"copper_pillar", "cut_copper"},
            new String[]{"cut_gold", "gold_block"}, new String[]{"cut_gold_stairs", "gold_block"}, new String[]{"cut_gold_slab", "gold_block"},
            new String[]{"gold_tiles", "gold_block"}, new String[]{"gold_pillar", "gold_block"}, new String[]{"chiseled_gold", "gold_block"},
            new String[]{"cut_iron", "iron_block"}, new String[]{"cut_iron_stairs", "iron_block"}, new String[]{"cut_iron_slab", "iron_block"},
            new String[]{"iron_tiles", "iron_block"}, new String[]{"iron_pillar", "iron_block"}, new String[]{"chiseled_iron", "iron_block"},
            new String[]{"chiseled_diamond", "diamond_block"}, new String[]{"chiseled_emerald", "emerald_block"},
            new String[]{"chiseled_lapis", "lapis_block"}, new String[]{"chiseled_redstone", "redstone_block"},
            new String[]{"chiseled_coal", "coal_block"}, new String[]{"chiseled_netherite", "netherite_block"});
    /** Block items that survive fire and lava (like the netherite block's). */
    public static final Set<String> FIRE_RESISTANT = Set.of("chiseled_netherite");

    static {
        for (String[] metal : METALS) {
            String name = metal[0];
            Function<BlockBehaviour.Properties, Block> factory = name.endsWith("_stairs")
                    ? s -> new StairBlock(BLOCKS.get(name.substring(0, name.length() - "_stairs".length())).defaultBlockState(), s)
                    : name.endsWith("_slab") ? SlabBlock::new
                    : name.endsWith("_pillar") ? RotatedPillarBlock::new
                    : name.equals("chiseled_redstone") ? net.minecraft.world.level.block.PoweredBlock::new
                    : Block::new;
            add(name, agedBase(metal[1]), factory);
        }
    }

    /** The block an aged brick copies: Vanilla's, or (with a namespace) one of ours, which is not in the registry yet while this class loads. */
    public static Block agedBase(String id) {
        return id.startsWith(SimpleBlocks.MOD_ID + ":") ? BLOCKS.get(id.substring(SimpleBlocks.MOD_ID.length() + 1))
                : BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    /** Every block path in registration order. */
    public static final List<String> NAMES = List.copyOf(BLOCKS.keySet());

    /**
     * Stairs, slab and wall of an ice's bricks (a-icebricks38): properties of the Vanilla ice (slipperiness, sound, hardness), the
     * stairs shaped like the bricks. Only normal ice melts ({@link IceShapes}); packed and blue ice never do, like in Vanilla.
     */
    private static void iceBrickShapes(String ice, Block vanilla, Block bricks) {
        String stem = ice + "_brick";
        if (vanilla == Blocks.ICE) {
            add(stem + "_stairs", vanilla, s -> new IceShapes.Stairs(bricks.defaultBlockState(), s));
            add(stem + "_slab", vanilla, IceShapes.Slab::new);
            add(stem + "_wall", vanilla, IceShapes.Wall::new);
        } else {
            add(stem + "_stairs", vanilla, s -> new StairBlock(bricks.defaultBlockState(), s));
            add(stem + "_slab", vanilla, SlabBlock::new);
            add(stem + "_wall", vanilla, s -> new WallBlock(s.forceSolidOn()));
        }
    }

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
        com.simpleblocks.metal.MetalBlocks.registerBlocks();   // PAL35b: copper tiles/pillar stages (after their base above)
        com.simpleblocks.compact.CompactBlocks.registerBlocks();
        com.simpleblocks.saw.Sawing.registerBlock();
    }

    /** The Shapecutter's menus (hand saw and N62 machines), recipe type and serializer (principle A4b). */
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
            Item.Properties properties = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            Item item = new BlockItem(block, FIRE_RESISTANT.contains(name) ? properties.fireResistant() : properties);
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, item));
        });
        com.simpleblocks.nature.NatureBlocks.registerItems();
        com.simpleblocks.snow.SnowBlocks.registerItems();
        com.simpleblocks.pale.PaleBlocks.registerItems();
        com.simpleblocks.metal.MetalBlocks.registerItems();
        com.simpleblocks.compact.CompactBlocks.registerItems();
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
        // N62: the Pulse and the Auto Shapecutter right behind the Shapecutter.
        out.add(new ItemStack(com.simpleblocks.saw.Sawing.PULSE_SAW_ITEM));
        out.add(new ItemStack(com.simpleblocks.saw.Sawing.AUTO_SAW_ITEM));
        ITEMS.forEach((name, item) -> {
            out.add(new ItemStack(item));
            // PAL35b: each copper form's aged and waxed stages right behind its base (like Vanilla's copper rows)
            out.addAll(com.simpleblocks.metal.MetalBlocks.tabStacks(name));
        });
        com.simpleblocks.nature.NatureBlocks.ITEMS.values().forEach(item -> out.add(new ItemStack(item)));
        if (com.simpleblocks.snow.SnowBlocks.FROST_GRASS_ITEM != null) out.add(new ItemStack(com.simpleblocks.snow.SnowBlocks.FROST_GRASS_ITEM));
        out.addAll(com.simpleblocks.pale.PaleBlocks.tabStacks()); // DW04B: the pale stages and their waxed copies
        out.addAll(com.simpleblocks.compact.CompactBlocks.tabStacks()); // WH26-10: Compact and Dense blocks
        // N48 (14.11.): the plain Crimson/Warped blazewood bookshelves are SimpleLib content (group nether_bookshelves, requested by this
        // module); with SimpleBuilding they stand in its blazewood row (appended behind this tab), so only list them alone.
        if (!SimpleBlocks.simplebuildingLoaded) {
            for (String shelf : com.simplelib.registry.LibMaterials.NETHER_BOOKSHELVES) out.add(new ItemStack(com.simplelib.api.SimpleLibApi.materialItem(shelf)));
        }
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
                        "sculk_tiles", "sculk_tile_stairs", "sculk_tile_slab", "sculk_tile_wall")),
                new Placement(false, Items.CHISELED_TUFF_BRICKS, List.of("cracked_tuff_bricks", "mossy_tuff_bricks", "mossy_tuff_brick_stairs", "mossy_tuff_brick_slab", "mossy_tuff_brick_wall")),
                new Placement(false, Items.RESIN_BRICKS, List.of("cracked_mud_bricks", "mossy_mud_bricks", "mossy_mud_brick_stairs", "mossy_mud_brick_slab", "mossy_mud_brick_wall")),
                new Placement(false, Items.RED_SANDSTONE, List.of("cut_sandstone_stairs", "cut_sandstone_wall", "smooth_sandstone_wall")),
                new Placement(false, Items.CINNABAR, List.of("cut_red_sandstone_stairs", "cut_red_sandstone_wall", "smooth_red_sandstone_wall")),
                new Placement(false, Items.SULFUR, List.of("cracked_cinnabar_bricks", "mossy_cinnabar_bricks", "mossy_cinnabar_brick_stairs", "mossy_cinnabar_brick_slab", "mossy_cinnabar_brick_wall")),
                new Placement(false, Items.SEA_LANTERN, List.of("cracked_sulfur_bricks", "mossy_sulfur_bricks", "mossy_sulfur_brick_stairs", "mossy_sulfur_brick_slab", "mossy_sulfur_brick_wall")),
                new Placement(false, Items.NETHER_BRICKS, List.of("netherrack_stairs", "netherrack_slab", "netherrack_wall")),
                new Placement(false, Items.BASALT, List.of("cracked_red_nether_bricks", "chiseled_red_nether_bricks", "blue_nether_bricks", "blue_nether_brick_stairs",
                        "blue_nether_brick_slab", "blue_nether_brick_wall", "cracked_blue_nether_bricks", "chiseled_blue_nether_bricks")),
                new Placement(false, Items.POLISHED_BASALT, List.of("smooth_basalt_stairs", "smooth_basalt_slab", "smooth_basalt_wall")),
                new Placement(false, Items.PURPUR_BLOCK, List.of("cracked_end_stone_bricks", "mossy_end_stone_bricks", "mossy_end_stone_brick_stairs", "mossy_end_stone_brick_slab", "mossy_end_stone_brick_wall")),
                new Placement(false, Items.SMOOTH_STONE_SLAB, List.of("smooth_stone_stairs")),
                new Placement(true, Items.TUFF, List.of("calcite_stairs", "calcite_slab", "calcite_wall")),
                new Placement(true, Items.SCULK_VEIN, List.of("sculk_deepslate"))));
        // a-palette05 (IG31-42): the missing members of the Vanilla families, each right behind the Vanilla member that precedes it.
        out.add(Placement.after(false, Items.STONE_SLAB, "stone_wall"));
        out.add(Placement.after(false, Items.SMOOTH_STONE_SLAB, "smooth_stone_wall")); // a-pal09
        out.add(Placement.after(false, Items.POLISHED_GRANITE_SLAB, "polished_granite_wall", "chiseled_polished_granite"));
        out.add(Placement.after(false, Items.POLISHED_DIORITE_SLAB, "polished_diorite_wall", "chiseled_polished_diorite"));
        out.add(Placement.after(false, Items.POLISHED_ANDESITE_SLAB, "polished_andesite_wall", "chiseled_polished_andesite"));
        out.add(Placement.after(false, Items.QUARTZ_SLAB, "quartz_wall"));
        out.add(Placement.after(false, Items.SMOOTH_QUARTZ_SLAB, "smooth_quartz_wall"));
        out.add(Placement.after(false, Items.QUARTZ_BRICKS, "quartz_brick_stairs", "quartz_brick_slab", "quartz_brick_wall"));
        out.add(Placement.after(false, Items.PURPUR_SLAB, "purpur_wall"));
        out.add(Placement.after(false, Items.NETHER_BRICK_FENCE, "nether_brick_fence_gate")); // N89b
        // N77: each concrete wall right behind its Vanilla concrete slab.
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            out.add(Placement.after(false, BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_concrete_slab")),
                    color.getName() + "_concrete_wall"));
        }
        out.add(Placement.after(false, Items.END_STONE, "end_stone_stairs", "end_stone_slab", "end_stone_wall"));
        out.add(Placement.after(false, Items.PRISMARINE_WALL, "cracked_prismarine", "chiseled_prismarine"));
        out.add(Placement.after(false, Items.PRISMARINE_BRICK_SLAB, "prismarine_brick_wall", "cracked_prismarine_bricks"));
        out.add(Placement.after(false, Items.DARK_PRISMARINE_SLAB, "dark_prismarine_wall", "cracked_dark_prismarine", "chiseled_dark_prismarine"));
        // PAL14: sandstone pillars behind the chiseled sandstone; the ice members in the natural tab behind their ice.
        out.add(Placement.after(false, Items.CHISELED_SANDSTONE, "sandstone_pillar"));
        out.add(Placement.after(false, Items.CHISELED_RED_SANDSTONE, "red_sandstone_pillar"));
        // a-icebricks38: Vanilla order (the ice's shapes, chiseled, polished, then the bricks with their cracked block and shapes, like deepslate).
        out.add(Placement.after(true, Items.ICE, "ice_stairs", "ice_slab", "ice_wall", "chiseled_ice", "polished_ice", "ice_bricks", "cracked_ice_bricks",
                "ice_brick_stairs", "ice_brick_slab", "ice_brick_wall"));
        out.add(Placement.after(true, Items.PACKED_ICE, "packed_ice_stairs", "packed_ice_slab", "packed_ice_wall", "polished_packed_ice", "packed_ice_bricks",
                "cracked_packed_ice_bricks", "packed_ice_brick_stairs", "packed_ice_brick_slab", "packed_ice_brick_wall"));
        out.add(Placement.after(true, Items.BLUE_ICE, "blue_ice_stairs", "blue_ice_slab", "blue_ice_wall", "polished_blue_ice", "blue_ice_bricks",
                "cracked_blue_ice_bricks", "blue_ice_brick_stairs", "blue_ice_brick_slab", "blue_ice_brick_wall"));
        out.add(Placement.after(false, Items.POLISHED_DEEPSLATE_WALL, "cracked_polished_deepslate"));
        // PLAN40 simpleblocks-3: the red sand variants are new (SimpleBuilding's search-tab chains do not know them), so always here.
        out.add(Placement.after(true, Items.RED_SAND, "red_sand_slab", "suspended_red_sand", "levitating_red_sand"));
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
