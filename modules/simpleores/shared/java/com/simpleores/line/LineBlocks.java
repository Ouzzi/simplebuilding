package com.simpleores.line;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import com.simplebuilding.framework.api.Protection;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * N46 / N47-M2 (b-circ46, 14.11.2026, docs/ai/PLAN-B-CIRC46-2026-11-14.md): the Astralit/Nihilit line moved here from
 * SimpleBuilding's core with its sources - End ores (hidden until brushed, plus the dull twins), dust and shards - and its
 * devices: Astral/Nihil/Bridge Redstone (dust, block, torch), lamps, End rods, pistons and rails. Same ids under
 * {@code simpleores:}; SimpleBuilding moves its old ids ({@code IdMovesHook}, {@code SharedMaterials.ORES_*}).
 * Registered from {@link OresRegistry} after its own blocks (no automatic block items: the torches have one item for two blocks).
 */
public final class LineBlocks {
    /** Blocks an Astral/Nihil piston never moves (data: tags/block/end_piston_immovable.json). */
    public static final TagKey<Block> END_PISTON_IMMOVABLE = TagKey.create(Registries.BLOCK, SimpleOres.id("end_piston_immovable"));

    // Sources: the End ores (W24-4: hidden in End stone until a brush frees them) and their dull twins (W23-2).
    public static final Block NIHILITH_ORE = block("nihilith_ore", Blocks.END_STONE, s -> new com.simpleores.block.HiddenOreBlock(UniformInt.of(3, 7),
            s.strength(25.0f, 1200.0f).requiresCorrectToolForDrops()));
    public static final Block ASTRALIT_ORE = block("astralit_ore", Blocks.END_STONE, s -> new com.simpleores.block.HiddenOreBlock(UniformInt.of(3, 7),
            s.strength(20.0f, 1200.0f).lightLevel(state -> 5).requiresCorrectToolForDrops()));
    public static final Block DULL_ASTRALIT_ORE = block("dull_astralit_ore", Blocks.END_STONE, s -> new DropExperienceBlock(UniformInt.of(1, 3),
            s.strength(15.0f, 1200.0f).lightLevel(state -> 3).requiresCorrectToolForDrops()));
    public static final Block DULL_NIHILITH_ORE = block("dull_nihilith_ore", Blocks.END_STONE, s -> new DropExperienceBlock(UniformInt.of(1, 3),
            s.strength(20.0f, 1200.0f).requiresCorrectToolForDrops()));

    // The line: vanilla-like dust of its own line, a permanent source block, a torch (B23-M1), End rods (W24-4).
    public static final Block NIHIL_REDSTONE = block("nihil_redstone", Blocks.GLASS, s -> new SignalDustBlock(SignalDustBlock.Line.NIHIL, signalDust(s, LineSounds.NIHIL_DUST)));
    public static final Block ASTRAL_REDSTONE = block("astral_redstone", Blocks.GLASS, s -> new SignalDustBlock(SignalDustBlock.Line.ASTRAL, signalDust(s, LineSounds.ASTRAL_DUST)));
    public static final Block BRIDGE_REDSTONE = block("bridge_redstone", Blocks.GLASS, s -> new SignalDustBlock(SignalDustBlock.Line.BRIDGE, signalDust(s, LineSounds.BRIDGE_DUST)));
    public static final Block NIHILITH_LAMP = block("nihilith_lamp", Blocks.GLASS, s -> new EndSignalLampBlock(false, endLamp(s, LineSounds.NIHIL_LAMP)));
    public static final Block ASTRALIT_LAMP = block("astralit_lamp", Blocks.GLASS, s -> new EndSignalLampBlock(true, endLamp(s, LineSounds.ASTRAL_LAMP)));
    public static final Block ASTRAL_REDSTONE_BLOCK = block("astral_redstone_block", Blocks.REDSTONE_BLOCK, s -> new LineRedstoneBlock(SignalDustBlock.Line.ASTRAL, s.mapColor(MapColor.COLOR_PINK)));
    public static final Block NIHIL_REDSTONE_BLOCK = block("nihil_redstone_block", Blocks.REDSTONE_BLOCK, s -> new LineRedstoneBlock(SignalDustBlock.Line.NIHIL, s.mapColor(MapColor.COLOR_CYAN)));
    public static final Block BRIDGE_REDSTONE_BLOCK = block("bridge_redstone_block", Blocks.REDSTONE_BLOCK, s -> new LineRedstoneBlock(SignalDustBlock.Line.BRIDGE, s.mapColor(MapColor.COLOR_MAGENTA)));
    public static final Block ASTRAL_REDSTONE_TORCH = block("astral_redstone_torch", Blocks.REDSTONE_TORCH, s -> new LineRedstoneTorchBlock(SignalDustBlock.Line.ASTRAL, s.mapColor(MapColor.COLOR_PINK)));
    public static final Block ASTRAL_REDSTONE_WALL_TORCH = block("astral_redstone_wall_torch", Blocks.REDSTONE_TORCH, s -> new LineRedstoneWallTorchBlock(SignalDustBlock.Line.ASTRAL, wallTorch(s, ASTRAL_REDSTONE_TORCH).mapColor(MapColor.COLOR_PINK)));
    public static final Block NIHIL_REDSTONE_TORCH = block("nihil_redstone_torch", Blocks.REDSTONE_TORCH, s -> new LineRedstoneTorchBlock(SignalDustBlock.Line.NIHIL, s.mapColor(MapColor.COLOR_CYAN)));
    public static final Block NIHIL_REDSTONE_WALL_TORCH = block("nihil_redstone_wall_torch", Blocks.REDSTONE_TORCH, s -> new LineRedstoneWallTorchBlock(SignalDustBlock.Line.NIHIL, wallTorch(s, NIHIL_REDSTONE_TORCH).mapColor(MapColor.COLOR_CYAN)));
    public static final Block ASTRALIT_END_ROD = block("astralit_end_rod", Blocks.END_ROD, s -> new LineEndRodBlock(SignalDustBlock.Line.ASTRAL, s.mapColor(MapColor.COLOR_PINK)));
    public static final Block NIHILITH_END_ROD = block("nihilith_end_rod", Blocks.END_ROD, s -> new LineEndRodBlock(SignalDustBlock.Line.NIHIL, s.mapColor(MapColor.COLOR_CYAN)));
    // Devices: pistons move one block in all six directions at once (immovable themselves), rails boost or brake.
    public static final Block NIHIL_PISTON = block("nihil_piston", Blocks.GLASS, s -> new EndPistonBlock(false, s.strength(1.5F).sound(LineSounds.NIHIL_PISTON)
            .isRedstoneConductor((state, world, pos) -> false).pushReaction(PushReaction.IMMOVEABLE)));
    public static final Block ASTRAL_PISTON = block("astral_piston", Blocks.GLASS, s -> new EndPistonBlock(true, s.strength(1.5F).sound(LineSounds.ASTRAL_PISTON)
            .isRedstoneConductor((state, world, pos) -> false).pushReaction(PushReaction.IMMOVEABLE)));
    public static final Block NIHIL_RAIL = block("nihil_rail", Blocks.POWERED_RAIL, s -> new EndRailBlock(false, s));
    public static final Block ASTRAL_RAIL = block("astral_rail", Blocks.POWERED_RAIL, s -> new EndRailBlock(true, s));

    /** Creative order (Redstone tab, behind the comparator): the line's devices. */
    public static final String[] REDSTONE = {"astral_redstone", "nihil_redstone", "bridge_redstone", "astral_redstone_block", "nihil_redstone_block",
            "bridge_redstone_block", "astral_redstone_torch", "nihil_redstone_torch", "astralit_lamp", "nihilith_lamp", "astral_piston", "nihil_piston",
            "astral_rail", "nihil_rail"};
    /** Natural blocks tab: the End ores. */
    public static final String[] NATURAL = {"astralit_ore", "dull_astralit_ore", "nihilith_ore", "dull_nihilith_ore"};
    /** Ingredients tab: dust and shards. */
    public static final String[] INGREDIENTS = {"astralit_dust", "dull_astralit_dust", "nihilith_shard", "dull_nihilith_shard"};
    /** Functional/building: the End rods. */
    public static final String[] DECORATIONS = {"astralit_end_rod", "nihilith_end_rod"};

    private LineBlocks() {}

    /** Called once at the end of {@link OresRegistry}'s initialisation: the items (block items, the torch items, dust). */
    public static void init() {
        for (String path : new String[] {"astralit_ore", "nihilith_ore", "dull_astralit_ore", "dull_nihilith_ore", "astral_redstone", "nihil_redstone",
                "bridge_redstone", "astral_redstone_block", "nihil_redstone_block", "bridge_redstone_block", "astralit_lamp", "nihilith_lamp",
                "astralit_end_rod", "nihilith_end_rod", "astral_piston", "nihil_piston", "astral_rail", "nihil_rail"}) {
            OresRegistry.putItem(path, p -> new BlockItem(OresRegistry.BLOCKS.get(path), p.useBlockDescriptionPrefix()));
        }
        OresRegistry.putItem("astral_redstone_torch", p -> new StandingAndWallBlockItem(ASTRAL_REDSTONE_TORCH, ASTRAL_REDSTONE_WALL_TORCH, Direction.DOWN, p.useBlockDescriptionPrefix()));
        OresRegistry.putItem("nihil_redstone_torch", p -> new StandingAndWallBlockItem(NIHIL_REDSTONE_TORCH, NIHIL_REDSTONE_WALL_TORCH, Direction.DOWN, p.useBlockDescriptionPrefix()));
        // The trim materials of dust and shard stay SimpleBuilding's (SharedMaterialTrimMixin adds them there).
        for (String path : INGREDIENTS) OresRegistry.putItem(path, p -> new Item(p));
    }

    private static Block block(String path, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        return OresRegistry.putBlock(path, base, factory);
    }

    private static BlockBehaviour.Properties endLamp(BlockBehaviour.Properties s, SoundType sound) {
        return s.strength(0.3F).sound(sound).mapColor(MapColor.COLOR_PURPLE).isRedstoneConductor((state, world, pos) -> false)
                .lightLevel(state -> state.getValue(EndSignalLampBlock.LIT) ? 15 : 0);
    }

    /** Vanilla dust's properties (no collision, breaks at once, pistons destroy it), own powder sound (SP22-5). */
    static BlockBehaviour.Properties signalDust(BlockBehaviour.Properties s, SoundType sound) {
        return s.noCollision().instabreak().pushReaction(PushReaction.POPPED).noOcclusion().sound(sound)
                .isRedstoneConductor((state, world, pos) -> false)
                .lightLevel(state -> state.getValue(SignalDustBlock.POWER) > 0 ? 3 : 0);
    }

    private static BlockBehaviour.Properties wallTorch(BlockBehaviour.Properties s, Block standing) {
        return s.overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
    }

    /**
     * Whether a device at {@code source} may move the block at {@code target} (claims): framework {@link Protection}, no player.
     * Always true on the client and without a protection provider (as SimpleBuilding's {@code WorldPermissions.mayAutomate}).
     */
    public static boolean mayAutomate(Level level, BlockPos source, BlockPos target) {
        if (!(level instanceof ServerLevel server) || !Protection.active(server.getServer())) return true;
        String dimension = server.dimension().identifier().toString();
        return Protection.allowsAutomation(server.getServer(), new Protection.Target(null, dimension, source.getX(), source.getY(), source.getZ(), false),
                new Protection.Target(null, dimension, target.getX(), target.getY(), target.getZ(), false));
    }

    private static final java.util.Set<String> SIGNAL_RECIPES = java.util.Set.of("nihil_redstone", "astral_redstone", "bridge_redstone", "nihilith_lamp",
            "astralit_lamp", "nihil_redstone_block", "astral_redstone_block", "bridge_redstone_block", "nihil_redstone_from_nihil_redstone_block",
            "astral_redstone_from_astral_redstone_block", "bridge_redstone_from_bridge_redstone_block", "astral_redstone_torch", "nihil_redstone_torch");

    /** Recipe paths of this mod that a switched-off line device takes away (SimpleLib RecipeFilters). */
    public static boolean recipeRemoved(String p) {
        return (!com.simpleores.config.OresConfig.endPistons && p.startsWith("astral_piston") || !com.simpleores.config.OresConfig.endPistons && p.startsWith("nihil_piston"))
                || (!com.simpleores.config.OresConfig.endRails && (p.equals("astral_rail") || p.equals("nihil_rail")))
                || (!com.simpleores.config.OresConfig.endSignals && SIGNAL_RECIPES.contains(p));
    }

    /** The key of a line block (tests, data checks). */
    public static ResourceKey<Block> key(String path) {
        return ResourceKey.create(Registries.BLOCK, SimpleOres.id(path));
    }
}
