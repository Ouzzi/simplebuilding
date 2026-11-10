package com.simplebuilding.modules.simplelayers.world.gen;

import static com.simplebuilding.modules.simplelayers.world.gen.GenHash.unit;

/**
 * Level 0 ("the Lobby", dimension {@code simplelayers:backrooms}) and, with another {@link Style}, the exit level: the endless
 * room grid of LAYERS10/13 (cells 8 x 8, rooms 5 high, walls on the cell lines, 3-wide doors, pillar halls, dark cells, offices
 * and cobwebs) extended by BACK18 (plan docs/ai/PLAN-B-BACK18-2026-11-10.md):
 * <ul>
 * <li>Escape: regions of {@link Style#region} cells, one exit cell each (a free-standing wall with the Exit Door and an EXIT sign).
 *     Every cell forces a door towards its exit ({@link GenHash#toward}), so the way out is at most 2 * (region - 3) cells long;
 *     in the exit's row and column an EXIT sign hangs under the ceiling on the side of the door that leads on.</li>
 * <li>Nesting (Lobby only): an upper floor (carpet y 9, ceiling y 15, darker, more dead ends) above the bedrock slab y 8,
 *     reached by stairwell cells (an L-shaped stair with a parapet around the opening above).</li>
 * <li>Ways deeper (Lobby only): noclip spots (a rift in a wall, y 2-3) and maintenance doors on the upper floor lead to Level 1.</li>
 * </ul>
 * Bedrock: y 0, y 8 (between the floors) and y 16-63 (Lobby; the exit level is one floor: y 8-63).
 */
public final class LobbyGen {
    public static final int CELL = 8, FLOOR_Y = 1, CEILING_Y = 7, TOP_Y = 8, UPPER = 8, HALL = 4, HULL_TOP_Y = 63;
    public static final double OFFICE_CHANCE = 0.05, WEB_CHANCE = 0.04, STAIR_CHANCE = 0.03, RIFT_CHANCE = 0.04, MAINT_CHANCE = 0.03;

    public enum Edge { WALL, DOOR, OPEN }

    public enum Supply { NONE, OFFICE, WEBS, BREAK_ROOM, LOUNGE }

    /**
     * BACK20 height of a ground floor cell (Lobby only): the normal room (5 high), a dropped ceiling (3 high), a carpeted dais in the
     * middle (one step up, edged by slabs) or an atrium (a light well through the slab into the upper floor, a gallery with a parapet
     * up there).
     */
    public enum Shape { NORMAL, LOW, DAIS, ATRIUM }

    public static final double ROOM_CHANCE = 0.025;
    public static final double LOW_CHANCE = 0.09, DAIS_CHANCE = 0.04, ATRIUM_CHANCE = 0.035;

    /**
     * Look and numbers of a grid level. {@code salt} separates levels that share the world seed; {@code region} 0 = no exits.
     */
    public record Style(Piece carpet, Piece paper, Piece striped, Piece dotted, Piece ceiling, Piece stained, double doorChance,
                        double hallChance, double darkChance, int region, int salt, boolean upperFloor, boolean extras, boolean bright) {
    }

    public static final Style LOBBY = new Style(Piece.CARPET, Piece.WALLPAPER, Piece.STRIPED_WALLPAPER, Piece.DOTTED_WALLPAPER,
            Piece.CEILING_TILE, Piece.STAINED_CEILING_TILE, 0.4, 0.10, 0.05, 8, 0, true, true, false);
    /** The exit level: white, bright, quiet, an exit within a few rooms (region 5: at most 4 cells). */
    public static final Style END = new Style(Piece.END_FLOOR, Piece.END_WALL, Piece.END_WALL, Piece.END_WALL,
            Piece.END_CEILING, Piece.END_CEILING, 0.45, 0.18, 0.0, 5, 200, false, false, true);
    /** Supplies on (config {@code backrooms.supplies}; set by the config, read by every generator). */
    public static volatile boolean supplies = true;

    private LobbyGen() {}

    static int salt(Style s, int layer) {
        return s.salt + layer * 40;
    }

    static double doorChance(Style s, int layer) {
        return layer == 0 ? s.doorChance : 0.58;
    }

    public static boolean hall(Style s, long seed, int layer, int cellX, int cellZ) {
        double chance = layer == 0 ? s.hallChance : 0.04;
        return unit(seed, Math.floorDiv(cellX, HALL), Math.floorDiv(cellZ, HALL), 11 + salt(s, layer)) < chance;
    }

    static boolean sameHall(Style s, long seed, int layer, int ax, int az, int bx, int bz) {
        return Math.floorDiv(ax, HALL) == Math.floorDiv(bx, HALL) && Math.floorDiv(az, HALL) == Math.floorDiv(bz, HALL)
                && hall(s, seed, layer, ax, az);
    }

    static boolean escapes(Style s, int layer) {
        return layer == 0 && s.region > 0;
    }

    /** The west line of cell (cellX, cellZ) = the east line of (cellX - 1, cellZ). */
    public static Edge west(Style s, long seed, int layer, int cellX, int cellZ) {
        if (sameHall(s, seed, layer, cellX, cellZ, cellX - 1, cellZ)) return Edge.OPEN;
        if (escapes(s, layer) && GenHash.forcedWest(seed, cellX, cellZ, s.region, s.salt + 21)) return Edge.DOOR;
        return unit(seed, cellX, cellZ, 1 + salt(s, layer)) < doorChance(s, layer) ? Edge.DOOR : Edge.WALL;
    }

    /** The north line of cell (cellX, cellZ) = the south line of (cellX, cellZ - 1). */
    public static Edge north(Style s, long seed, int layer, int cellX, int cellZ) {
        if (sameHall(s, seed, layer, cellX, cellZ, cellX, cellZ - 1)) return Edge.OPEN;
        if (escapes(s, layer) && GenHash.forcedNorth(seed, cellX, cellZ, s.region, s.salt + 21)) return Edge.DOOR;
        return unit(seed, cellX, cellZ, 2 + salt(s, layer)) < doorChance(s, layer) ? Edge.DOOR : Edge.WALL;
    }

    public static boolean exitCell(Style s, long seed, int cellX, int cellZ) {
        return s.region > 0 && GenHash.root(seed, cellX, cellZ, s.region, s.salt + 21);
    }

    /** Ground floor cells on the straight last leg to the exit: they show the way with an EXIT sign. */
    public static boolean signed(Style s, long seed, int cellX, int cellZ) {
        return s.region > 0 && GenHash.onRootCross(seed, cellX, cellZ, s.region, s.salt + 21) && !exitCell(s, seed, cellX, cellZ);
    }

    /** Stairwell cell: never on the exit cross or in a hall (the stair needs its corner). */
    public static boolean stairwell(Style s, long seed, int cellX, int cellZ) {
        return s.upperFloor && !hall(s, seed, 0, cellX, cellZ) && !hall(s, seed, 1, cellX, cellZ)
                && !(s.region > 0 && GenHash.onRootCross(seed, cellX, cellZ, s.region, s.salt + 21))
                && unit(seed, cellX, cellZ, 13 + s.salt) < STAIR_CHANCE;
    }

    public static boolean dark(Style s, long seed, int layer, int cellX, int cellZ) {
        if (layer == 0 && exitCell(s, seed, cellX, cellZ)) return false;
        double chance = layer == 0 ? s.darkChance : 0.15;
        return unit(seed, cellX, cellZ, 3 + salt(s, layer)) < chance;
    }

    /** BACK20: the height of a ground floor cell (always NORMAL off the Lobby, in exit, signed, stair and hall cells). */
    public static Shape shape(Style s, long seed, int cellX, int cellZ) {
        if (!s.extras || exitCell(s, seed, cellX, cellZ) || signed(s, seed, cellX, cellZ) || stairwell(s, seed, cellX, cellZ)
                || hall(s, seed, 0, cellX, cellZ) || hall(s, seed, 1, cellX, cellZ)) return Shape.NORMAL;
        double u = unit(seed, cellX, cellZ, 14 + s.salt);
        if (u < LOW_CHANCE) return Shape.LOW;
        if (u < LOW_CHANCE + DAIS_CHANCE) return Shape.DAIS;
        return u < LOW_CHANCE + DAIS_CHANCE + ATRIUM_CHANCE ? Shape.ATRIUM : Shape.NORMAL;
    }

    /** Whether the bedrock slab y 8 of the cell is open (stairwell or atrium). */
    public static boolean opensSlab(Style s, long seed, int cellX, int cellZ) {
        return stairwell(s, seed, cellX, cellZ) || s.upperFloor && shape(s, seed, cellX, cellZ) == Shape.ATRIUM;
    }

    /** What a cell holds besides air: nothing, an office or cobwebs (nothing at all with supplies off). */
    public static Supply supply(Style s, long seed, int layer, int cellX, int cellZ) {
        if (!supplies || !s.extras || exitCell(s, seed, cellX, cellZ) && layer == 0 || stairwell(s, seed, cellX, cellZ)
                || shape(s, seed, cellX, cellZ) != Shape.NORMAL) return Supply.NONE;
        double u = unit(seed, cellX, cellZ, 7 + salt(s, layer));
        double office = layer == 0 ? OFFICE_CHANCE : 0.03, webs = layer == 0 ? WEB_CHANCE : 0.08;
        if (u < office) return Supply.OFFICE;
        // BACK20: a break room (sink, counter with a smoker, a table) and a lounge (sofa, rug, a dead plant) - household things
        // that are the Backrooms' resources (furnace, iron, wood)
        double v = unit(seed, cellX, cellZ, 17 + salt(s, layer));
        if (layer == 0 && v < ROOM_CHANCE) return Supply.BREAK_ROOM;
        if (v < 2 * ROOM_CHANCE) return Supply.LOUNGE;
        return u < office + webs || dark(s, seed, layer, cellX, cellZ) ? Supply.WEBS : Supply.NONE;
    }

    /** Noclip spot in a wall line (Lobby ground floor, wall edges only): a rift 2 high in the middle of the line. */
    static boolean riftIn(Style s, long seed, Edge edge, int cellX, int cellZ, int side) {
        return s.extras && edge == Edge.WALL && unit(seed, cellX, cellZ, 31 + side + s.salt) < RIFT_CHANCE;
    }

    /** Maintenance door in a wall line of the upper floor (to Level 1). */
    static boolean maintIn(Style s, long seed, Edge edge, int cellX, int cellZ, int side) {
        return s.upperFloor && edge == Edge.WALL && unit(seed, cellX, cellZ, 33 + side + s.salt) < MAINT_CHANCE;
    }

    /** Supply block at cell-local (lx, ly, lz), or null for air. */
    static Piece supplyPiece(Style s, long seed, int layer, int cx, int cz, int lx, int ly, int lz) {
        if (lx >= 3 && lx <= 5 || lz >= 3 && lz <= 5 || ly > 6) return null;
        Supply supply = supply(s, seed, layer, cx, cz);
        if (supply == Supply.NONE) return null;
        int sl = layer * 40;
        boolean west = lx <= 2, north = lz <= 2;
        int corner = (west ? 0 : 1) + (north ? 0 : 2);
        int wx = west ? 1 : 7, wz = north ? 1 : 7, ix = west ? 2 : 6;
        if (supply == Supply.WEBS) {
            boolean web = ly == 6 && (lx == wx || lz == wz)
                    || lx == wx && lz == wz && (ly == 5 && unit(seed, cx * 4 + corner, cz, 9 + sl) < 0.6 || ly == 2 && unit(seed, cx * 4 + corner, cz, 10 + sl) < 0.25);
            return web ? Piece.COBWEB : null;
        }
        int desk = (int) (unit(seed, cx, cz, 8 + sl) * 4);
        if (supply == Supply.BREAK_ROOM) {
            if (corner == desk && lz == wz && ly == 2) return lx == wx ? Piece.SINK : lx == ix ? Piece.COUNTER : null;
            if (corner == desk && lz == wz && ly == 3 && lx == ix) return north ? Piece.SMOKER_S : Piece.SMOKER_N;
            if (corner == (desk ^ 3) && ly == 2 && lx == wx && lz == wz) return Piece.DESK;
            if (corner == (desk ^ 3) && ly == 2 && lx == ix && lz == wz) return north ? Piece.CHAIR_S : Piece.CHAIR_N;
            return null;
        }
        if (supply == Supply.LOUNGE) {
            if (corner == desk && lz == wz && ly == 2 && (lx == wx || lx == ix)) return north ? Piece.CHAIR_N : Piece.CHAIR_S;
            if (corner == desk && ly == 2 && (lx == wx || lx == ix) && lz == (north ? wz + 1 : wz - 1)) return Piece.RUG;
            if (corner == (desk ^ 3) && ly == 2 && lx == wx && lz == wz) return Piece.COUNTER;
            if (corner == (desk ^ 1) && ly == 2 && lx == wx && lz == wz) return Piece.POT;
            return null;
        }
        if (corner == desk) {
            if (ly == 2 && lz == wz) return Piece.DESK;
            if (ly == 2 && lx == ix && lz != wz) return north ? Piece.CHAIR_S : Piece.CHAIR_N;
        } else if (corner == (desk ^ 3)) {
            if (lz == wz && (ly == 2 || ly == 3 && lx == wx)) return Piece.LOGS_X;
        } else if (corner == (desk ^ 1) && ly == 2 && lx == wx && lz == wz && unit(seed, cx, cz, 12 + sl) < 0.5) {
            return Piece.CRAFTING_TABLE;
        }
        return null;
    }

    /** BACK20: wings of the upper floor (6 x 6 cells, one in five) are an archive - walls of bookshelves. */
    public static boolean archive(Style s, long seed, int layer, int cellX, int cellZ) {
        return layer == 1 && s.upperFloor && unit(seed, Math.floorDiv(cellX, 6), Math.floorDiv(cellZ, 6), 18 + s.salt) < 0.2;
    }

    static Piece paper(Style s, long seed, int layer, int cellX, int cellZ) {
        if (archive(s, seed, layer, cellX, cellZ)) return Piece.BOOKSHELF;
        double u = unit(seed, cellX, cellZ, 5 + salt(s, layer));
        if (layer == 1) u = u * 0.8 + 0.2; // the upper floor: more striped and dotted paper (older, other wing)
        return u < 0.7 ? s.paper : u < 0.86 ? s.striped : s.dotted;
    }

    static boolean inDoor(int offset) {
        return offset >= 3 && offset <= 5;
    }

    /** Stairwell blocks at cell-local (lx, y, lz) (absolute y), or null when the stairwell leaves the position alone. */
    static Piece stairPiece(Style s, int lx, int y, int lz) {
        if (lz == 2 && lx >= 1 && lx <= 7) {
            if (y == lx + 1) return Piece.STEP_E;
            if (y >= 7 && y <= 9 && lx >= 4) return Piece.AIR;
        }
        if (lx == 7 && lz == 3 && y == 9) return Piece.STEP_S;
        if (y == 10 && (lz == 1 && lx >= 4 || lz == 3 && lx >= 4 && lx <= 6 || lz == 2 && lx == 3)) return s.dotted;
        return null;
    }

    /** The piece at (x, y, z). */
    public static Piece at(Style s, long seed, int x, int y, int z) {
        if (y < 0 || y > HULL_TOP_Y) return Piece.AIR;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        if (s.upperFloor && y >= 2 && y <= 10 && lx > 0 && lz > 0 && stairwell(s, seed, cx, cz)) {
            Piece p = stairPiece(s, lx, y, lz);
            if (p != null) return p;
        }
        if (s.upperFloor && y >= CEILING_Y && y <= TOP_Y + 2 && lx > 0 && lz > 0 && shape(s, seed, cx, cz) == Shape.ATRIUM) {
            Piece p = atriumPiece(s, lx, y, lz);
            if (p != null) return p;
        }
        int layer;
        if (y == 0 || y == TOP_Y) return Piece.BEDROCK;
        if (y < TOP_Y) layer = 0;
        else if (s.upperFloor && y < TOP_Y + UPPER) layer = 1;
        else return Piece.BEDROCK;
        return room(s, seed, layer, cx, cz, lx, y - layer * UPPER, lz);
    }

    /**
     * Atrium blocks from the ground floor ceiling (y 7) to the upper parapet (y 10), or null: the 3 x 3 well is open, four lights
     * hang around it under the ground floor ceiling, a wallpaper parapet one block high rings it on the gallery above.
     */
    static Piece atriumPiece(Style s, int lx, int y, int lz) {
        int ring = Math.max(Math.abs(lx - 4), Math.abs(lz - 4));
        if (ring <= 1 && y <= TOP_Y + 1) return Piece.AIR;
        if (y == CEILING_Y && ring == 2 && (lx == 2 || lx == 6) && (lz == 2 || lz == 6)) return Piece.NEON_ON;
        if (y == TOP_Y && ring == 2) return s.paper; // the well is lined, no bedrock shows
        if (y == TOP_Y + 2 && ring == 2) return s.dotted;
        return null;
    }

    /** Whether a door in the line between the two cells is cut down to 3 high (a dropped ceiling on either side). */
    static boolean lowDoor(Style s, long seed, int layer, int ax, int az, int bx, int bz) {
        return layer == 0 && s.extras && (shape(s, seed, ax, az) == Shape.LOW || shape(s, seed, bx, bz) == Shape.LOW);
    }

    /** One floor; ly = 1 carpet, 2-6 room, 7 ceiling. */
    static Piece room(Style s, long seed, int layer, int cx, int cz, int lx, int ly, int lz) {
        if (ly == FLOOR_Y) return s.carpet;
        Shape shape = layer == 0 ? shape(s, seed, cx, cz) : Shape.NORMAL;
        if (shape == Shape.LOW && lx > 0 && lz > 0 && ly >= 5) {
            // dropped ceiling: rooms 3 high, the light one block lower
            if (ly == 5 && (lx == 3 || lx == 4) && lz == 4) {
                double u = unit(seed, cx, cz, 4 + salt(s, layer));
                return u < 0.1 ? Piece.NEON_BROKEN : u < 0.3 ? Piece.NEON_FLICKER : Piece.NEON_ON;
            }
            return ly == 5 && unit(seed, cx * CELL + lx, cz * CELL + lz, 15) < 0.08 ? s.stained : s.ceiling;
        }
        if (ly == CEILING_Y) {
            if ((lx == 3 || lx == 4) && lz == 4) {
                double u = unit(seed, cx, cz, 4 + salt(s, layer));
                if (dark(s, seed, layer, cx, cz) || u < (layer == 0 ? 0.06 : 0.12)) return Piece.NEON_BROKEN;
                return u < (layer == 0 ? 0.13 : 0.24) ? Piece.NEON_FLICKER : Piece.NEON_ON;
            }
            if (s.bright && (lx == 2 || lx == 6) && (lz == 2 || lz == 6)) return Piece.NEON_ON;
            return unit(seed, cx * CELL + lx, cz * CELL + lz, 6 + salt(s, layer)) < (layer == 0 ? 0.03 : 0.08) ? s.stained : s.ceiling;
        }
        if (lx == 0 && lz == 0) return paper(s, seed, layer, cx, cz);
        Edge edge;
        int along, side;
        if (lx == 0) {
            edge = west(s, seed, layer, cx, cz);
            along = lz;
            side = 0;
        } else if (lz == 0) {
            edge = north(s, seed, layer, cx, cz);
            along = lx;
            side = 1;
        } else {
            return inside(s, seed, layer, cx, cz, lx, ly, lz);
        }
        if (edge == Edge.OPEN) return Piece.AIR;
        if (edge == Edge.DOOR && inDoor(along) && ly <= 5) {
            boolean low = lx == 0 ? lowDoor(s, seed, layer, cx, cz, cx - 1, cz) : lowDoor(s, seed, layer, cx, cz, cx, cz - 1);
            if (ly <= 4 || !low) return Piece.AIR;
        }
        if (along == 4 && ly <= 3) {
            if (layer == 0 && riftIn(s, seed, edge, cx, cz, side)) return Piece.RIFT;
            if (layer == 1 && maintIn(s, seed, edge, cx, cz, side)) return ly == 2 ? Piece.LEVEL_DOOR_LOWER : Piece.LEVEL_DOOR_UPPER;
        }
        return paper(s, seed, layer, cx, cz);
    }

    static Piece inside(Style s, long seed, int layer, int cx, int cz, int lx, int ly, int lz) {
        if (layer == 0 && exitCell(s, seed, cx, cz)) {
            // The exit: a free-standing wall across the cell with the Exit Door and the EXIT sign above it (like the exit room).
            if (lz == 4 && lx >= 2 && lx <= 6) {
                if (lx == 4 && ly == 2) return Piece.EXIT_DOOR_LOWER;
                if (lx == 4 && ly == 3) return Piece.EXIT_DOOR_UPPER;
                if (lx == 4 && ly == 4) return Piece.EXIT_SIGN;
                return s.paper;
            }
            return Piece.AIR;
        }
        if (layer == 0 && ly == 6 && signed(s, seed, cx, cz)) {
            int dir = GenHash.toward(seed, cx, cz, s.region, s.salt + 21);
            if (dir == GenHash.EAST && lx == 7 && lz == 4 || dir == GenHash.WEST && lx == 1 && lz == 4
                    || dir == GenHash.SOUTH && lx == 4 && lz == 7 || dir == GenHash.NORTH && lx == 4 && lz == 1) return Piece.EXIT_SIGN;
        }
        if (layer == 0 && ly <= 3) {
            Shape shape = shape(s, seed, cx, cz);
            if (shape == Shape.DAIS) {
                // a carpeted dais one step up, edged by slabs; now and then a lone chair stands on it
                int ring = Math.max(Math.abs(lx - 4), Math.abs(lz - 4));
                if (ly == 2 && ring <= 1) return s.carpet;
                if (ly == 2 && ring == 2) return Piece.SLAB;
                if (ly == 3 && lx == 4 && lz == 3 && unit(seed, cx, cz, 16) < 0.5) return Piece.CHAIR_S;
            }
        }
        Piece supply = supplyPiece(s, seed, layer, cx, cz, lx, ly, lz);
        return supply != null ? supply : Piece.AIR;
    }

    /** Where travellers arrive in the cell holding (x, z): its centre at y 2, 2 blocks south of the exit wall in an exit cell. */
    public static int[] arrival(Style s, long seed, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        int y = shape(s, seed, cx, cz) == Shape.DAIS ? FLOOR_Y + 2 : FLOOR_Y + 1;
        return new int[] {cx * CELL + 4, y, cz * CELL + (exitCell(s, seed, cx, cz) ? 6 : 4)};
    }
}
