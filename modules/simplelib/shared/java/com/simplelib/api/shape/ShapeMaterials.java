package com.simplelib.api.shape;

import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * The shared core of the shape sub-mods (N86-4, moved out of Simple Octets' OctetRegistry): Simple Octets (octet cells) and
 * Simple Slabs (half slabs) make one shape block per material, also of other mods' blocks. Those blocks may register after
 * the shape mod, so their properties come from a fixed Vanilla template (this class) and the real block is read late.
 *
 * <ul>
 *   <li>{@link #template}: the properties template of a block of another mod (namespace + path of its full block).</li>
 *   <li>{@link #vanilla}: a Vanilla source block as its own template.</li>
 *   <li>Both pin map colour and light to the template's default state: a shape block has none of the source's block state
 *       properties, so state-dependent lambdas copied from it (log axis in the map colour, lit in the light level) would throw.</li>
 *   <li>{@link #setFlammable}/{@link #igniteOdds}: the fire table (FireOddsAccessor).</li>
 * </ul>
 */
public final class ShapeMaterials {
    /** Template block and the property changes on top of {@code ofFullCopy(block)} (map colour and light pinned). */
    public record Template(Block block, UnaryOperator<BlockBehaviour.Properties> pin) {
        /** Properties of a new shape block with this id: a full copy of the template, then {@link #pin}. */
        public BlockBehaviour.Properties properties(ResourceKey<Block> key) {
            return pin.apply(BlockBehaviour.Properties.ofFullCopy(block).setId(key));
        }
    }

    /** Simple Ores' full blocks that copy the gold block (OresRegistry), and the Netherite-like ones. */
    private static final Set<String> FULL_GOLD_ORES = Set.of("rose_gold_block", "rose_bronze_block");
    public static final Set<String> FIRE_RESISTANT_ORES = Set.of("enderite_block", "terranite_block", "infernite_block", "zenithite_block");

    /** Vanilla template of another mod's block whose properties that mod copies (Simple Blocks BlocksRegistry, SimpleLib). */
    private static final Map<String, Block> TEMPLATES = Map.ofEntries(Map.entry("blue_nether_bricks", Blocks.RED_NETHER_BRICKS),
            Map.entry("hushed_deepslate_tiles", Blocks.DEEPSLATE_TILES), Map.entry("sculk_tiles", Blocks.DEEPSLATE_TILES),
            Map.entry("polished_end_stone", Blocks.END_STONE),
            // The mossy bricks (a-palette05): the brick they age.
            Map.entry("mossy_deepslate_bricks", Blocks.DEEPSLATE_BRICKS), Map.entry("mossy_deepslate_tiles", Blocks.DEEPSLATE_TILES),
            Map.entry("mossy_tuff_bricks", Blocks.TUFF_BRICKS), Map.entry("mossy_mud_bricks", Blocks.MUD_BRICKS),
            Map.entry("mossy_end_stone_bricks", Blocks.END_STONE_BRICKS), Map.entry("mossy_cinnabar_bricks", Blocks.CINNABAR_BRICKS),
            Map.entry("mossy_sulfur_bricks", Blocks.SULFUR_BRICKS),
            // The ice bricks (a-icebricks38): their ice - as slippery, glass sound; like the ice octets they do not melt.
            Map.entry("ice_bricks", Blocks.ICE), Map.entry("packed_ice_bricks", Blocks.PACKED_ICE), Map.entry("blue_ice_bricks", Blocks.BLUE_ICE),
            // Simple Blocks' cut gold and iron (N86: their slabs get half slabs).
            Map.entry("cut_gold", Blocks.GOLD_BLOCK), Map.entry("cut_iron", Blocks.IRON_BLOCK));

    private ShapeMaterials() {}

    /** A Vanilla source block as its own template (map colour and light pinned to its default state). */
    public static Template vanilla(Block source) {
        return new Template(source, s -> pin(s, source));
    }

    /**
     * Template of a block of another mod ({@code namespace:base}, its full block). Order of the rules = the octets' rules
     * before N86: Simple End woods and logs, Simple Enchanting's blazewoods, Simple Ores' metals, the fixed table, Simple
     * Chess' checkers, else SimpleBuilding's palettes (Astralit, Nihilith, Ender Quartz, astral/nihil End stone, purpur).
     */
    public static Template template(String namespace, String base) {
        if ("simpleend".equals(namespace)) {
            boolean ash = base.contains("ashwood");
            if (base.endsWith("_planks")) {
                // Simple End planks: oak planks' properties (Simple End's own may not be registered yet).
                MapColor color = ash ? MapColor.COLOR_LIGHT_GRAY : MapColor.TERRACOTTA_LIGHT_GRAY;
                return new Template(Blocks.OAK_PLANKS, s -> s.mapColor(color).lightLevel(state -> 0));
            }
            boolean stripped = base.startsWith("stripped_");
            MapColor color = ash ? (stripped ? MapColor.COLOR_LIGHT_GRAY : MapColor.COLOR_GRAY)
                    : (stripped ? MapColor.TERRACOTTA_LIGHT_GRAY : MapColor.TERRACOTTA_GRAY);
            return new Template(stripped ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_LOG, s -> s.mapColor(color).lightLevel(state -> 0));
        }
        if ("simpleenchanting".equals(namespace) && base.contains("blazewood")) {
            // Blazewood (Nether wood, does not burn): Vanilla's crimson/warped planks.
            return vanilla(base.startsWith("crimson") ? Blocks.CRIMSON_PLANKS : Blocks.WARPED_PLANKS);
        }
        if ("simpleores".equals(namespace)) {
            // Block of lead (owner ORES31-BEXTRA): iron-like, as sturdy as the lead block.
            if (base.contains("lead")) {
                return new Template(Blocks.IRON_BLOCK, s -> pin(s.strength(25.0F, 1000.0F), Blocks.IRON_BLOCK));
            }
            // Silver and both rose alloys copy gold, Enderite and the rift metals Netherite, every other metal iron (OresRegistry).
            Block metal = base.contains("silver") || FULL_GOLD_ORES.contains(base) ? Blocks.GOLD_BLOCK
                    : FIRE_RESISTANT_ORES.contains(base) ? Blocks.NETHERITE_BLOCK : Blocks.IRON_BLOCK;
            return vanilla(metal);
        }
        Block fixed = TEMPLATES.get(base);
        if (fixed != null) return vanilla(fixed);
        if ("simplechess".equals(namespace)) {
            // Chess checkers: quartz-like; the Astralit checkers glow a little (ChessColor).
            int light = base.contains("astralit") ? 5 : 0;
            return new Template(Blocks.QUARTZ_BLOCK, s -> s.mapColor(Blocks.QUARTZ_BLOCK.defaultMapColor()).lightLevel(state -> light));
        }
        // SimpleBuilding's palettes: End stone like the astral/nihil End stone they are made of, the purpur blocks Purpur; every
        // astral block glows like it (light 10), the Ender ones are purple.
        Block palette = base.endsWith("_purpur_block") || base.endsWith("_purpur") ? Blocks.PURPUR_BLOCK : Blocks.END_STONE;
        int light = base.contains("astralit") || base.startsWith("astral_") ? 10 : 0;
        MapColor color = base.contains("ender_quartz") || base.startsWith("ender_") ? MapColor.COLOR_PURPLE : palette.defaultMapColor();
        return new Template(palette, s -> s.mapColor(color).lightLevel(state -> light));
    }

    /** Map colour and light of {@code source}'s default state, fixed. */
    public static BlockBehaviour.Properties pin(BlockBehaviour.Properties s, Block source) {
        int light = source.defaultBlockState().getLightEmission();
        return s.mapColor(source.defaultMapColor()).lightLevel(state -> light);
    }

    /** Puts a block into the fire table (odds like {@code FireBlock#bootStrap}: planks 5/20, wool 30/60, logs 5/5). */
    public static void setFlammable(Block block, int igniteOdds, int burnOdds) {
        ((com.simplelib.mixin.FireOddsAccessor) Blocks.FIRE).simplelib$setFlammable(block, igniteOdds, burnOdds);
    }

    /** The fire table's ignite odds of a state (0 = does not catch fire). */
    public static int igniteOdds(BlockState state) {
        return ((com.simplelib.mixin.FireOddsAccessor) Blocks.FIRE).simplelib$getIgniteOdds(state);
    }
}
