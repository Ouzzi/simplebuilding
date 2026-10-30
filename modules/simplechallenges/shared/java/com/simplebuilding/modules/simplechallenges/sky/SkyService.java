package com.simplebuilding.modules.simplechallenges.sky;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.preset.WorldMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The server side of the Skyblock family (S4): builds the start of the chosen world mode once, refills the One Block and fills
 * freshly generated chunks with the sky grid. The world itself is made empty by the world creation screen (void generator);
 * this class only puts blocks into it. Every entry starts with the same checks as the unlock hooks: no active challenge with a
 * world mode, no effect.
 */
public final class SkyService {
    /** Height of the top layer of the start island / of the One Block. */
    public static final int ISLAND_Y = 64;

    /** The spawn column at the platform height. Tests move it (with {@link #levelOverride}) so they build in the test area, not at 0,0. */
    public static volatile BlockPos origin = new BlockPos(0, ISLAND_Y, 0);
    /** Test seam: the level the world mode builds in instead of the Overworld (null = Overworld). */
    public static volatile ServerLevel levelOverride;

    private SkyService() {}

    private static ServerLevel level(Active a) {
        ServerLevel o = levelOverride;
        return o != null ? o : a.overworld();
    }

    public static BlockState state(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        return b == null ? Blocks.AIR.defaultBlockState() : b.defaultBlockState();
    }

    /** The world mode of the running challenge, or null. */
    private static WorldMode modeOf(Active a) {
        return a == null || !SimpleChallenges.on() ? null : a.preset.world();
    }

    /** Builds the start of the world mode once per world ({@code worldBuilt} in the state) and tells the price pool what the island gives. */
    public static void start(Active a) {
        WorldMode w = a.preset.world();
        if (w == null) return;
        ServerLevel ow = level(a);
        BlockPos origin = SkyService.origin;
        if (!a.state.worldBuilt) {
            switch (w.kind()) {
                case ISLAND -> buildIsland(ow, origin, IslandPlan.of(w.style()));
                case ONEBLOCK -> ow.setBlock(origin, state(OneBlockPhases.block(a.state.seed, 0)), Block.UPDATE_ALL);
                case SKYGRID -> {
                    buildIsland(ow, origin, IslandPlan.of("mini"));
                    for (int cx = -5; cx <= 5; cx++) {
                        for (int cz = -5; cz <= 5; cz++) {
                            LevelChunk c = ow.getChunkSource().getChunkNow(cx, cz);
                            if (c != null) fillGrid(a.state.seed, w.spacing(), c);
                        }
                    }
                }
            }
            a.state.worldBuilt = true;
            a.state.setDirty();
        }
    }

    /** What the start of the world mode gives by hand (for the prices of an unlock preset that is combined with it). */
    public static java.util.List<com.simplebuilding.modules.simplechallenges.price.WeightedItem> reachable(Active a) {
        WorldMode w = a.preset.world();
        if (w == null) return java.util.List.of();
        return switch (w.kind()) {
            case ISLAND -> IslandPlan.reachable(w.style());
            case ONEBLOCK -> java.util.List.of(new com.simplebuilding.modules.simplechallenges.price.WeightedItem("minecraft:dirt", 10, 64),
                    new com.simplebuilding.modules.simplechallenges.price.WeightedItem("minecraft:oak_log", 5, 64));
            case SKYGRID -> IslandPlan.reachable("mini");
        };
    }

    /** Places an island with its chest; {@code origin} is the spawn column at the height of the top grass layer. */
    public static void buildIsland(ServerLevel level, BlockPos origin, IslandPlan plan) {
        for (IslandPlan.Block b : plan.blocks()) {
            level.setBlock(origin.offset(b.dx(), b.dy(), b.dz()), state(b.block()), Block.UPDATE_ALL);
        }
        BlockPos at = origin.offset(plan.chestX, plan.chestY, plan.chestZ);
        Direction facing = Direction.byName(plan.chestFacing.toLowerCase(java.util.Locale.ROOT));
        level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing == null ? Direction.SOUTH : facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (IslandPlan.Stack s : plan.chest) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(s.item()));
                chest.setItem(slot++, new ItemStack(item, s.count()));
            }
            chest.setChanged();
        }
    }

    /** Every five ticks: a mined One Block grows back (see {@link #refill}). */
    public static void tick(Active a) {
        WorldMode w = modeOf(a);
        if (w != null && w.kind() == WorldMode.Kind.ONEBLOCK && a.server.getTickCount() % 5 == 0) refill(a);
    }

    /**
     * A mined One Block grows back with the next block of the sequence, only while its chunk is loaded.
     * @return true when a block was placed
     */
    public static boolean refill(Active a) {
        ServerLevel ow = level(a);
        BlockPos pos = origin;
        if (!ow.hasChunkAt(pos) || !ow.getBlockState(pos).isAir()) return false;
        a.state.oneBlockCount++;
        a.state.setDirty();
        ow.setBlock(pos, state(OneBlockPhases.block(a.state.seed, a.state.oneBlockCount)), Block.UPDATE_ALL);
        return true;
    }

    /** A chunk was generated: in a sky grid world its nodes get their blocks (Overworld only; the start platform is left alone). */
    public static void chunkGenerated(ServerLevel level, LevelChunk chunk) {
        Active a = ChallengeRuntime.server();
        WorldMode w = modeOf(a);
        if (w == null || w.kind() != WorldMode.Kind.SKYGRID || level.dimension() != Level.OVERWORLD) return;
        fillGrid(a.state.seed, w.spacing(), chunk);
    }

    /** Puts the grid blocks into the empty nodes of one chunk (no neighbour updates: the chunk is just being built). */
    public static int fillGrid(long seed, int spacing, LevelChunk chunk) {
        int[] placed = {0};
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        SkyGridPlan.nodes(seed, spacing, chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), chunk.getMinY(), chunk.getMaxY(), (x, y, z, id) -> {
            if (SkyGridPlan.isStarter(x - origin.getX(), y, z - origin.getZ(), origin.getY())) return;
            pos.set(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) return;
            chunk.setBlockState(pos, state(id), 0);
            placed[0]++;
        });
        return placed[0];
    }
}
