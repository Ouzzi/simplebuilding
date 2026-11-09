package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The endless room grid of the Backrooms (concept §2.2 variant B), a pure function of the world seed and the position: no
 * storage, deterministic, seamless. Rooms are 8 x 8 cells, 5 high: bedrock at y 0 and y 8, damp carpet at y 1, ceiling tiles
 * and neon lights at y 7. Walls stand on the cell lines (x or z divisible by 8); a cell owns its west and north line, so a line
 * is decided once - by the hash of that shared edge - and both neighbours see the same wall or door. Doors are 3 wide (offsets
 * 3-5, symmetric so rotated special rooms fit) and 4 high. Four by four cells sometimes form a pillar hall (no inner walls,
 * only the posts on the line crossings); some cells are dark (every light broken).
 *
 * <p>LAYERS13 (plan docs/ai/PLAN-B-LAYERS13-2026-11-09.md): the bedrock hull is the floor y 0 and everything from the ceiling
 * y 8 up to the build limit y {@link #HULL_TOP_Y} - no air above the maze to escape into. Some cells hold supplies
 * ({@link #supply}, config {@code backrooms.supplies}): an office (desk, chair, a pile of stripped oak logs, sometimes a crafting
 * table) or cobwebs in the corners (also every dark cell) - wood and string to survive and craft. Supplies stand only in the
 * four 2 x 2 corners of a cell, never in a doorway or the arrival centre.
 */
public final class BackroomsGrid {
    public static final int CELL = 8, FLOOR_Y = 1, CEILING_Y = 7, TOP_Y = 8, HALL = 4;
    /** The top of the bedrock hull: the last block of the dimension (height 64). */
    public static final int HULL_TOP_Y = 63;
    /** Share of cell edges with a door (above the percolation threshold 0.5: one huge connected maze). */
    public static final double DOOR_CHANCE = 0.66, HALL_CHANCE = 0.10, DARK_CHANCE = 0.05;
    /** Shares of cells with an office (wood) or cobwebs (string); dark cells get cobwebs too. */
    public static final double OFFICE_CHANCE = 0.05, WEB_CHANCE = 0.04;

    public enum Edge { WALL, DOOR, OPEN }

    public enum Supply { NONE, OFFICE, WEBS }

    private BackroomsGrid() {}

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** A uniform number in [0, 1) for (a, b, salt). */
    public static double unit(long seed, int a, int b, int salt) {
        long h = mix(seed ^ mix(a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L));
        return (h >>> 11) * 0x1.0p-53;
    }

    public static boolean hall(long seed, int cellX, int cellZ) {
        return unit(seed, Math.floorDiv(cellX, HALL), Math.floorDiv(cellZ, HALL), 11) < HALL_CHANCE;
    }

    static boolean sameHall(long seed, int ax, int az, int bx, int bz) {
        return Math.floorDiv(ax, HALL) == Math.floorDiv(bx, HALL) && Math.floorDiv(az, HALL) == Math.floorDiv(bz, HALL) && hall(seed, ax, az);
    }

    /** The west line of cell (cellX, cellZ) = the east line of (cellX - 1, cellZ). */
    public static Edge west(long seed, int cellX, int cellZ) {
        if (sameHall(seed, cellX, cellZ, cellX - 1, cellZ)) return Edge.OPEN;
        return unit(seed, cellX, cellZ, 1) < DOOR_CHANCE ? Edge.DOOR : Edge.WALL;
    }

    /** The north line of cell (cellX, cellZ) = the south line of (cellX, cellZ - 1). */
    public static Edge north(long seed, int cellX, int cellZ) {
        if (sameHall(seed, cellX, cellZ, cellX, cellZ - 1)) return Edge.OPEN;
        return unit(seed, cellX, cellZ, 2) < DOOR_CHANCE ? Edge.DOOR : Edge.WALL;
    }

    public static boolean dark(long seed, int cellX, int cellZ) {
        return unit(seed, cellX, cellZ, 3) < DARK_CHANCE;
    }

    /** What a cell holds besides air: nothing, an office or cobwebs (nothing at all with {@code backrooms.supplies} off). */
    public static Supply supply(long seed, int cellX, int cellZ) {
        if (!LayersConfig.supplies) return Supply.NONE;
        double u = unit(seed, cellX, cellZ, 7);
        if (u < OFFICE_CHANCE) return Supply.OFFICE;
        return u < OFFICE_CHANCE + WEB_CHANCE || dark(seed, cellX, cellZ) ? Supply.WEBS : Supply.NONE;
    }

    /** The supply block at cell-local (lx, y, lz) of an inner position, or null for air. */
    static BlockState supplyBlock(long seed, int cx, int cz, int lx, int y, int lz) {
        if (lx >= 3 && lx <= 5 || lz >= 3 && lz <= 5 || y > 6) return null;
        Supply supply = supply(seed, cx, cz);
        if (supply == Supply.NONE) return null;
        boolean west = lx <= 2, north = lz <= 2;
        int corner = (west ? 0 : 1) + (north ? 0 : 2);
        int wx = west ? 1 : 7, wz = north ? 1 : 7, ix = west ? 2 : 6;
        if (supply == Supply.WEBS) {
            boolean web = y == 6 && (lx == wx || lz == wz)
                    || lx == wx && lz == wz && (y == 5 && unit(seed, cx * 4 + corner, cz, 9) < 0.6 || y == 2 && unit(seed, cx * 4 + corner, cz, 10) < 0.25);
            return web ? Blocks.COBWEB.defaultBlockState() : null;
        }
        int desk = (int) (unit(seed, cx, cz, 8) * 4);
        if (corner == desk) {
            // A desk of two top slabs along the wall, the chair (stairs, its back away from the desk) in front of it.
            if (y == 2 && lz == wz) return Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            if (y == 2 && lx == ix && lz != wz) return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, north ? Direction.SOUTH : Direction.NORTH);
        } else if (corner == (desk ^ 3)) {
            // A pile of stripped oak logs in the opposite corner: two along the wall, one on top in the corner (12 planks).
            if (lz == wz && (y == 2 || y == 3 && lx == wx)) return Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        } else if (corner == (desk ^ 1) && y == 2 && lx == wx && lz == wz && unit(seed, cx, cz, 12) < 0.5) {
            return Blocks.CRAFTING_TABLE.defaultBlockState();
        }
        return null;
    }

    /** Wallpaper of the lines a cell owns: mostly plain, some striped or dotted. */
    static BlockState paper(long seed, int cellX, int cellZ) {
        double u = unit(seed, cellX, cellZ, 5);
        return (u < 0.7 ? LayersRegistry.WALLPAPER : u < 0.86 ? LayersRegistry.STRIPED_WALLPAPER : LayersRegistry.DOTTED_WALLPAPER).defaultBlockState();
    }

    static boolean inDoor(int offset) {
        return offset >= 3 && offset <= 5;
    }

    /** The block at (x, y, z); bedrock at y 0 and y 8..63, air outside 0..63. */
    public static BlockState at(long seed, int x, int y, int z) {
        if (y < 0 || y > HULL_TOP_Y) return Blocks.AIR.defaultBlockState();
        if (y == 0 || y >= TOP_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y == FLOOR_Y) return LayersRegistry.DAMP_CARPET.defaultBlockState();
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        if (y == CEILING_Y) {
            if ((lx == 3 || lx == 4) && lz == 4) {
                NeonLightBlock.Mode mode;
                double u = unit(seed, cx, cz, 4);
                if (dark(seed, cx, cz) || u < 0.06) mode = NeonLightBlock.Mode.BROKEN;
                else mode = u < 0.13 ? NeonLightBlock.Mode.FLICKER : NeonLightBlock.Mode.ON;
                return LayersRegistry.NEON_LIGHT.defaultBlockState().setValue(NeonLightBlock.MODE, mode);
            }
            return (unit(seed, x, z, 6) < 0.03 ? LayersRegistry.STAINED_CEILING_TILE : LayersRegistry.CEILING_TILE).defaultBlockState();
        }
        if (lx == 0 && lz == 0) return LayersRegistry.WALLPAPER.defaultBlockState();
        Edge edge;
        int along;
        if (lx == 0) {
            edge = west(seed, cx, cz);
            along = lz;
        } else if (lz == 0) {
            edge = north(seed, cx, cz);
            along = lx;
        } else {
            BlockState supply = supplyBlock(seed, cx, cz, lx, y, lz);
            return supply != null ? supply : Blocks.AIR.defaultBlockState();
        }
        if (edge == Edge.OPEN || edge == Edge.DOOR && inDoor(along) && y <= 5) return Blocks.AIR.defaultBlockState();
        return paper(seed, cx, cz);
    }

    /** Centre of the cell holding (x, z), at standing height: where travellers arrive (always free, no wall line). */
    public static int cellCentre(int coordinate) {
        return Math.floorDiv(coordinate, CELL) * CELL + 4;
    }
}
