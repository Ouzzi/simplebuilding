package com.simplebuilding.modules.simplelayers.vault.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One Bedrock Vault complex (UNDER21, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md): pure function of world seed and region
 * (128 x 128 blocks). 3 x 3 fields of 40 blocks on two floors (y 20 and y 44); the middle is the Ascent Hall (both floors, gallery,
 * altar, sealed shaft in the dome), an edge field below holds the Entrance Hall with the gate in its outer wall, three riddle
 * chambers (light / sound / path), quiet filler rooms, 1-2 stairwells; corridors (3 wide, arched) along a spanning tree plus
 * extra loops. Everything else of the region is solid: {@link VaultGen} turns solid next to open into cladding, the rest into
 * bedrock. Positions are world block coordinates.
 */
public final class VaultLayout {
    public static final int REGION = 128, SLOT = 40, ORIGIN = 4, MARGIN = 6, F0 = 20, F1 = 44;
    public static final int HALL = 24, HALL_TOP = 55, SHAFT_FROM = 60, SHAFT_TOP = 100;
    public static final int LIGHT = 0, SOUND = 1, PATH = 2, PARTS = 3, LEVERS = 5, TONES = 4, SEQUENCE = 5, PATH_COLS = 5, PATH_ROWS = 7;

    public enum Type { ALTAR, ENTRANCE, LIGHT, SOUND, PATH, STAIRWELL, PILLAR_HALL, GALLERY, CISTERN, ARCHIVE, EMPTY }

    // cladding styles: floor, floor alternate (checker), wall base, wall, wall top, ceiling
    public static final int S_HALL = 0, S_CORRIDOR = 1, S_ALTAR = 2, S_PUZZLE = 3, S_CISTERN = 4, S_POOL = 5, S_ARCHIVE = 6, S_PILLAR = 7,
            S_TUFF_MASS = 8, S_TILE_MASS = 9, S_SHAFT = 10;
    static final VaultPiece[][] STYLES = {
            {VaultPiece.POLISHED_DEEPSLATE, VaultPiece.DEEPSLATE_TILES, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.CALCITE, VaultPiece.DEEPSLATE_TILES, VaultPiece.SMOOTH_STONE},
            {VaultPiece.DEEPSLATE_TILES, VaultPiece.DEEPSLATE_TILES, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.DEEPSLATE_BRICKS, VaultPiece.DEEPSLATE_BRICKS, VaultPiece.POLISHED_DEEPSLATE},
            {VaultPiece.POLISHED_TUFF, VaultPiece.TUFF_BRICKS, VaultPiece.TUFF_BRICKS, VaultPiece.CALCITE, VaultPiece.CHISELED_TUFF_BRICKS, VaultPiece.POLISHED_TUFF},
            {VaultPiece.DEEPSLATE_TILES, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.DEEPSLATE_BRICKS, VaultPiece.POLISHED_TUFF, VaultPiece.CHISELED_TUFF, VaultPiece.DEEPSLATE_TILES},
            {VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.DARK_PRISMARINE, VaultPiece.CALCITE, VaultPiece.DARK_PRISMARINE, VaultPiece.SMOOTH_STONE},
            {VaultPiece.PRISMARINE_BRICKS, VaultPiece.PRISMARINE_BRICKS, VaultPiece.PRISMARINE_BRICKS, VaultPiece.PRISMARINE_BRICKS, VaultPiece.PRISMARINE_BRICKS, VaultPiece.PRISMARINE_BRICKS},
            {VaultPiece.SPRUCE_PLANKS, VaultPiece.SPRUCE_PLANKS, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.CALCITE, VaultPiece.DEEPSLATE_TILES, VaultPiece.SMOOTH_STONE},
            {VaultPiece.CHISELED_DEEPSLATE, VaultPiece.CHISELED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.CHISELED_DEEPSLATE},
            {VaultPiece.TUFF_BRICKS, VaultPiece.TUFF_BRICKS, VaultPiece.POLISHED_TUFF, VaultPiece.POLISHED_TUFF, VaultPiece.POLISHED_TUFF, VaultPiece.POLISHED_TUFF},
            {VaultPiece.DEEPSLATE_TILES, VaultPiece.DEEPSLATE_TILES, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.POLISHED_DEEPSLATE},
            {VaultPiece.POLISHED_TUFF, VaultPiece.POLISHED_TUFF, VaultPiece.CHISELED_TUFF_BRICKS, VaultPiece.POLISHED_TUFF, VaultPiece.CHISELED_TUFF_BRICKS, VaultPiece.POLISHED_TUFF}};

    /** An axis-aligned block box (inclusive): carved open, or (fill) solid again inside a carved room; lights every {@code step} in its ceiling. */
    public record Box(int x0, int y0, int z0, int x1, int y1, int z1, int style, boolean fill, int step, int off, int group) {
        public boolean contains(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        boolean touches(int ax, int ay, int az, int bx, int by, int bz) {
            return x1 >= ax && x0 <= bx && y1 >= ay && y0 <= by && z1 >= az && z0 <= bz;
        }
    }

    /** A room: interior (air) box, floor y = {@link #y0}. */
    public record Room(Type type, int level, int sx, int sz, int x0, int y0, int z0, int x1, int y1, int z1) {
        public int cx() {
            return (x0 + x1) / 2;
        }

        public int cz() {
            return (z0 + z1) / 2;
        }

        public boolean inside(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }
    }

    private static final Map<Long, VaultLayout> CACHE = new ConcurrentHashMap<>();

    public final long seed;
    public final int rx, rz, bx, bz;
    public final List<Box> boxes = new ArrayList<>();
    public final List<Room> rooms = new ArrayList<>();
    final Map<Integer, VaultPiece> decor = new HashMap<>();
    /** Corridor edges: {level, slotA, slotB} with slot = sx + 3 * sz. */
    public final List<int[]> edges = new ArrayList<>();
    /** The six gate blocks in the Entrance Hall's outer wall, the arrival spot (feet) in front of them. */
    public final List<int[]> gate = new ArrayList<>();
    public int[] arrival;
    /** Altar core (floor of the hall), sockets (light, sound, path), the 3 x 3 seal in the dome, the stream column top. */
    public int[] altar;
    public final int[][] sockets = new int[PARTS][];
    public final List<int[]> seal = new ArrayList<>();
    public final int[][] pedestals = new int[PARTS][];
    public final int[][] levers = new int[LEVERS][], lamps = new int[LEVERS][];
    /** Lamp pattern of the mural (bit i = lamp i lit), never 0; lever i toggles lamps i and i + 1. */
    public int target;
    public final int[][] crystals = new int[TONES][];
    public final int[] crystalTone = new int[TONES];
    public final int[] sequence = new int[SEQUENCE];
    /** Path field: origin (corner block, floor plane y), step along rows (dx,dz) and along columns; path cells c + PATH_COLS * r. */
    public int[] pathOrigin, pathRow, pathCol;
    public final List<Integer> path = new ArrayList<>();
    /** Supply barrel (feature) in the Entrance Hall. */
    public int[] cache;
    public int entranceSide;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    /** Group of the boxes being added (room index, 1000 + corridor index): thin parts inside one group are fine. */
    private int group;

    private VaultLayout(long seed, int rx, int rz) {
        this.seed = seed;
        this.rx = rx;
        this.rz = rz;
        this.bx = rx * REGION;
        this.bz = rz * REGION;
        build();
        for (Box b : boxes) {
            minY = Math.min(minY, b.y0);
            maxY = Math.max(maxY, b.y1);
        }
    }

    public static VaultLayout of(long seed, int rx, int rz) {
        long key = seed * 31L + (((long) rx) << 32 ^ (rz & 0xFFFFFFFFL));
        VaultLayout l = CACHE.get(key);
        if (l != null && l.seed == seed && l.rx == rx && l.rz == rz) return l;
        if (CACHE.size() > 256) CACHE.clear();
        l = new VaultLayout(seed, rx, rz);
        CACHE.put(key, l);
        return l;
    }

    public static VaultLayout at(long seed, int x, int z) {
        return of(seed, Math.floorDiv(x, REGION), Math.floorDiv(z, REGION));
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public Room room(Type type) {
        for (Room r : rooms) if (r.type == type) return r;
        return null;
    }

    /** The room whose interior holds the position, or null (corridors, shell). */
    public Room roomAt(int x, int y, int z) {
        for (Room r : rooms) if (r.inside(x, y, z)) return r;
        return null;
    }

    /** Whether the position lies in this complex's bounding box (the region minus its bedrock margin). */
    public boolean inComplex(int x, int y, int z) {
        int lx = x - bx, lz = z - bz;
        return lx >= ORIGIN && lx < ORIGIN + 3 * SLOT && lz >= ORIGIN && lz < ORIGIN + 3 * SLOT && y >= minY - 1 && y <= maxY + 1;
    }

    public VaultPiece decorAt(int x, int y, int z) {
        int lx = x - bx, lz = z - bz;
        if (lx < 0 || lz < 0 || lx >= REGION || lz >= REGION || y < 0 || y >= 128) return null;
        return decor.get(key(lx, y, lz));
    }

    private static int key(int lx, int y, int lz) {
        return (lx * REGION + lz) * 128 + y;
    }

    private void put(int x, int y, int z, VaultPiece p) {
        decor.put(key(x - bx, y, z - bz), p);
    }

    private long h(long a, long b) {
        return VaultHash.mix(seed, rx, rz, a * 1_000_003L + b);
    }

    private int pick(long a, long b, int n) {
        return (int) Math.floorMod(h(a, b), (long) n);
    }

    private void carve(int x0, int y0, int z0, int x1, int y1, int z1, int style) {
        carve(x0, y0, z0, x1, y1, z1, style, 0, 0);
    }

    private void carve(int x0, int y0, int z0, int x1, int y1, int z1, int style, int step, int off) {
        boxes.add(new Box(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), style, false, step, off, group));
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, int style) {
        boxes.add(new Box(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1), style, true, 0, 0, group));
    }

    // ------------------------------------------------------------------------------------------------------------------ build

    private void build() {
        Type[][] plan = new Type[2][9];
        int center = 4;
        plan[0][center] = plan[1][center] = Type.ALTAR;
        entranceSide = pick(1, 0, 4); // 0 north, 1 east, 2 south, 3 west
        int entrance = new int[] {1, 5, 7, 3}[entranceSide];
        plan[0][entrance] = Type.ENTRANCE;
        int[] corners = {0, 2, 6, 8};
        int stairs = 1 + pick(2, 0, 2);
        int first = pick(3, 0, 4);
        for (int i = 0; i < stairs; i++) {
            int c = corners[(first + i * 2) % 4];
            plan[0][c] = plan[1][c] = Type.STAIRWELL;
        }
        List<Integer> free0 = new ArrayList<>(), free1 = new ArrayList<>();
        for (int s = 0; s < 9; s++) {
            if (plan[0][s] == null) free0.add(s);
            if (plan[1][s] == null) free1.add(s);
        }
        Type[] riddles = {Type.LIGHT, Type.SOUND, Type.PATH};
        for (int i = 2; i > 0; i--) {
            int j = pick(4, i, i + 1);
            Type t = riddles[i];
            riddles[i] = riddles[j];
            riddles[j] = t;
        }
        plan[0][free0.remove(pick(5, 0, free0.size()))] = riddles[0];
        plan[1][free1.remove(pick(5, 1, free1.size()))] = riddles[1];
        boolean low = pick(5, 2, 2) == 0 && !free0.isEmpty();
        List<Integer> third = low ? free0 : free1;
        plan[low ? 0 : 1][third.remove(pick(5, 3, third.size()))] = riddles[2];
        Type[] fillers0 = {Type.PILLAR_HALL, Type.CISTERN, Type.GALLERY, Type.ARCHIVE, Type.EMPTY};
        Type[] fillers1 = {Type.PILLAR_HALL, Type.GALLERY, Type.ARCHIVE, Type.EMPTY, Type.GALLERY};
        int n = 0;
        for (int s : free0) plan[0][s] = fillers0[(pick(6, s, 5) + n++) % 5];
        for (int s : free1) plan[1][s] = fillers1[(pick(7, s, 5) + n++) % 5];

        Room[][] at = new Room[2][9];
        for (int level = 0; level < 2; level++) {
            for (int s = 0; s < 9; s++) {
                Type t = plan[level][s];
                if (level == 1 && (t == Type.ALTAR || t == Type.STAIRWELL)) {
                    at[1][s] = at[0][s];
                    continue;
                }
                at[level][s] = room(t, level, s % 3, s / 3);
            }
        }
        for (int level = 0; level < 2; level++) chooseEdges(level);
        guideEdges(plan);
        for (int[] e : edges) {
            group = 1000 + e[3];
            corridor(e[0], at[e[0]][e[1]], at[e[0]][e[2]], e[2] - e[1] == 1, e[4]);
        }
    }

    private int slotX(int sx) {
        return bx + ORIGIN + sx * SLOT;
    }

    private int slotZ(int sz) {
        return bz + ORIGIN + sz * SLOT;
    }

    /** Places the room of {@code type} in slot (sx, sz) and carves/decorates it. */
    private Room room(Type type, int level, int sx, int sz) {
        int f = level == 0 ? F0 : F1;
        int w, d, hgt;
        switch (type) {
            case ALTAR -> { w = HALL; d = HALL; hgt = HALL_TOP - F0 + 1; }
            case STAIRWELL -> { w = 18; d = 18; hgt = F1 + 8 - F0 + 1; }
            case ENTRANCE -> { w = 18; d = 14; hgt = 7; }
            case LIGHT -> { w = 15; d = 11; hgt = 7; }
            case SOUND -> { w = 15; d = 13; hgt = 8; }
            case PATH -> { w = PATH_COLS * 2; d = PATH_ROWS * 2 + 6; hgt = 5; }
            case PILLAR_HALL -> { w = 25; d = 25; hgt = 9; }
            case GALLERY -> { w = 11; d = 27; hgt = 7; }
            case CISTERN -> { w = 23; d = 23; hgt = 8; }
            case ARCHIVE -> { w = 19; d = 15; hgt = 7; }
            default -> { w = 11 + 2 * pick(8, sx + 3 * sz + 9 * level, 5); d = 11 + 2 * pick(9, sx + 3 * sz + 9 * level, 5); hgt = 6; }
        }
        int outer = outerSide(sx, sz, level);
        boolean turn = switch (type) {
            case ALTAR, STAIRWELL, PILLAR_HALL, CISTERN -> false;
            case ENTRANCE, LIGHT, SOUND -> outer == 1 || outer == 3; // the gate / riddle wall is a long wall facing the region edge
            default -> pick(10, sx + 3 * sz + 9 * level, 2) == 1;
        };
        if (turn) {
            int t = w;
            w = d;
            d = t;
        }
        int room = SLOT - 2 * MARGIN;
        int jx = Math.max(0, room - w), jz = Math.max(0, room - d);
        int x0 = slotX(sx) + MARGIN + (type == Type.ALTAR ? jx / 2 : pick(11, sx + 3 * sz + 9 * level, jx + 1));
        int z0 = slotZ(sz) + MARGIN + (type == Type.ALTAR ? jz / 2 : pick(12, sx + 3 * sz + 9 * level, jz + 1));
        if (type == Type.ENTRANCE) { // flush with the outer margin so the gate wall faces the region edge
            if (entranceSide == 0) z0 = slotZ(sz) + MARGIN;
            if (entranceSide == 2) z0 = slotZ(sz) + SLOT - MARGIN - d;
            if (entranceSide == 3) x0 = slotX(sx) + MARGIN;
            if (entranceSide == 1) x0 = slotX(sx) + SLOT - MARGIN - w;
        }
        Room r = new Room(type, type == Type.ALTAR || type == Type.STAIRWELL ? -1 : level, sx, sz, x0, f, z0, x0 + w - 1, f + hgt - 1, z0 + d - 1);
        rooms.add(r);
        group = rooms.size() - 1;
        switch (type) {
            case ALTAR -> altarHall(r);
            case STAIRWELL -> stairwell(r);
            case ENTRANCE -> entranceHall(r);
            case LIGHT -> lightChamber(r, outer);
            case SOUND -> soundChamber(r, outer);
            case PATH -> pathChamber(r, turn);
            case PILLAR_HALL -> pillarHall(r);
            case GALLERY -> gallery(r, turn);
            case CISTERN -> cistern(r);
            case ARCHIVE -> archive(r, turn);
            default -> emptyRoom(r);
        }
        return r;
    }

    /**
     * A wall of field (sx, sz) that faces the region edge (0 north, 1 east, 2 south, 3 west): no corridor ever opens there, so the
     * gate and the riddles (levers, lamps, mural, frieze) sit on it. The middle field has none (-1).
     */
    private int outerSide(int sx, int sz, int level) {
        if (sx == 1 && sz == 0) return 0;
        if (sx == 2 && sz == 1) return 1;
        if (sx == 1 && sz == 2) return 2;
        if (sx == 0 && sz == 1) return 3;
        if (sx == 1 && sz == 1) return -1;
        int a = sz == 0 ? 0 : 2, b = sx == 0 ? 3 : 1;
        return pick(13, sx + 3 * sz + 9 * level, 2) == 0 ? a : b;
    }

    private void box(Room r, int style, int step, int off) {
        carve(r.x0, r.y0, r.z0, r.x1, r.y1, r.z1, style, step, off);
    }

    // ----------------------------------------------------------------------------------------------------------------- rooms

    private void altarHall(Room r) {
        box(r, S_ALTAR, 0, 0);
        int cx = r.x0 + HALL / 2, cz = r.z0 + HALL / 2;
        for (int i = 1; i <= 4; i++) { // stepped dome up to the shaft
            int in = 2 * i;
            carve(r.x0 + in, HALL_TOP + i, r.z0 + in, r.x1 - in, HALL_TOP + i, r.z1 - in, S_ALTAR, i == 1 ? 4 : 0, 1);
        }
        carve(cx - 1, SHAFT_FROM, cz - 1, cx + 1, SHAFT_TOP, cz + 1, S_SHAFT);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            put(cx + dx, SHAFT_FROM, cz + dz, VaultPiece.SEAL);
            seal.add(new int[] {cx + dx, SHAFT_FROM, cz + dz});
        }
        balconyAndStairs(r, S_TUFF_MASS, VaultPiece.TUFF_STAIRS_N, VaultPiece.TUFF_STAIRS_S, VaultPiece.TUFF_RAIL);
        // altar: 3 x 3 platform, core in the middle, sockets north / east / west, the south side open to step on
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(cx + dx, F0, cz + dz, dx == 0 && dz == 0 ? VaultPiece.ALTAR : VaultPiece.PLINTH);
        altar = new int[] {cx, F0, cz};
        int[][] socket = {{cx, F0 + 1, cz - 1}, {cx + 1, F0 + 1, cz}, {cx - 1, F0 + 1, cz}};
        for (int p = 0; p < PARTS; p++) {
            sockets[p] = socket[p];
            put(socket[p][0], socket[p][1], socket[p][2], VaultPiece.SOCKETS[p]);
        }
        // a ring of chiseled tuff in the floor around the altar (radius 4) draws the eye to the middle
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) == 4 && (dx + dz) % 2 == 0) put(cx + dx, F0 - 1, cz + dz, VaultPiece.CHISELED_TUFF);
        }
        // the colour language under the gallery: one froglight of each part behind its socket, in the floor
        put(cx, F0 - 1, cz - 3, VaultPiece.FROG_LIGHT);
        put(cx + 3, F0 - 1, cz, VaultPiece.FROG_SOUND);
        put(cx - 3, F0 - 1, cz, VaultPiece.FROG_PATH);
        // soft wall lights on both floors (liminal: even light, no visible source but the panels)
        for (int a = 3; a < HALL; a += 5) {
            for (int y : new int[] {F0 + 4, F1 + 3}) {
                put(r.x0 + a, y, r.z0 - 1, VaultPiece.SEA_LANTERN);
                put(r.x0 + a, y, r.z1 + 1, VaultPiece.SEA_LANTERN);
                put(r.x0 - 1, y, r.z0 + a, VaultPiece.SEA_LANTERN);
                put(r.x1 + 1, y, r.z0 + a, VaultPiece.SEA_LANTERN);
            }
        }
    }

    /**
     * A gallery ring (3 wide, floor at the upper level) and a double stair run up to it inside the ring on the west side: run A
     * north (12 steps), landing, run B south (12 steps) onto the south gallery. The masses under the steps and the gallery are
     * fills (solid again), the steps themselves stair blocks in the open space above.
     */
    private void balconyAndStairs(Room r, int mass, VaultPiece up, VaultPiece down, VaultPiece rail) {
        int gy = F1 - 1;
        fill(r.x0, gy, r.z0, r.x1, gy, r.z0 + 2, mass);
        fill(r.x0, gy, r.z1 - 2, r.x1, gy, r.z1, mass);
        fill(r.x0, gy, r.z0 + 3, r.x0 + 2, gy, r.z1 - 3, mass);
        fill(r.x1 - 2, gy, r.z0 + 3, r.x1, gy, r.z1 - 3, mass);
        int zEnd = r.z1 - 3;
        int ax = r.x0 + 3, bxx = r.x0 + 5;
        for (int i = 0; i < 12; i++) {
            int z = zEnd - i, y = F0 + i;
            if (i > 0) fill(ax, F0, z, ax + 1, y - 1, z, mass);
            put(ax, y, z, up);
            put(ax + 1, y, z, up);
        }
        fill(ax, F0, zEnd - 13, ax + 3, F0 + 11, zEnd - 12, mass); // landing
        for (int i = 12; i < 24; i++) {
            int z = zEnd - 11 + (i - 12), y = F0 + i;
            fill(bxx, F0, z, bxx + 1, y - 1, z, mass);
            put(bxx, y, z, down);
            put(bxx + 1, y, z, down);
        }
        for (int i = 14; i < 24; i += 4) put(bxx + 1, F0 + i / 2, zEnd - 11 + (i - 12), VaultPiece.SEA_LANTERN); // light in the stair tower's face
        // railing on the inner edge of the gallery, open where run B arrives
        for (int x = r.x0 + 3; x <= r.x1 - 3; x++) {
            if (x < bxx || x > bxx + 1) put(x, F1, r.z1 - 3 + 1, rail);
            put(x, F1, r.z0 + 3 - 1, rail);
        }
        for (int z = r.z0 + 3; z <= r.z1 - 3; z++) {
            put(r.x0 + 2, F1, z, rail);
            put(r.x1 - 2, F1, z, rail);
        }
    }

    private void stairwell(Room r) {
        box(r, S_HALL, 0, 0);
        balconyAndStairs(r, S_TILE_MASS, VaultPiece.TILE_STAIRS_N, VaultPiece.TILE_STAIRS_S, VaultPiece.RAIL);
        int cx = r.cx() + 2, cz = r.cz();
        for (int y = F1 + 3; y <= r.y1; y++) put(cx, y, cz, VaultPiece.CHAIN);
        put(cx, F1 + 2, cz, VaultPiece.SOUL_LANTERN);
        for (int y = F0 + 5; y <= F0 + 9; y++) put(r.x1 - 4, y, r.cz(), VaultPiece.CHAIN);
        put(r.x1 - 4, F0 + 4, r.cz(), VaultPiece.SOUL_LANTERN);
        put(r.x0 - 1, F0 + 3, r.cz(), VaultPiece.SEA_LANTERN);
        put(r.x1 + 1, F1 + 3, r.cz(), VaultPiece.SEA_LANTERN);
    }

    private void entranceHall(Room r) {
        box(r, S_HALL, 5, 2);
        int side = entranceSide;
        // gate: 2 wide, 3 high, in the outer wall plane, a chiseled frame around it
        int ca = side == 0 || side == 2 ? r.cx() : r.cz();
        for (int a = -2; a <= 2; a++) {
            for (int y = F0; y <= F0 + 3; y++) {
                int[] p = wall(r, side, ca + a, y);
                boolean in = a >= -1 && a <= 0 && y <= F0 + 2;
                if (in) {
                    put(p[0], p[1], p[2], VaultPiece.GATE_OPEN);
                    gate.add(p);
                } else {
                    put(p[0], p[1], p[2], VaultPiece.CHISELED_DEEPSLATE);
                }
            }
        }
        int[] in = inward(side);
        int[] g = wall(r, side, ca, F0);
        arrival = new int[] {g[0] + in[0] * 3, F0, g[2] + in[1] * 3};
        // mural on the gate wall above the frame: the three seal colours around the altar sign
        int[] al = along(side);
        for (int a : new int[] {-1, 0}) {
            int[] sign = wall(r, side, ca + a, F0 + 4), top = wall(r, side, ca + a, F0 + 5);
            put(sign[0], sign[1], sign[2], VaultPiece.PLINTH);
            put(top[0], top[1], top[2], VaultPiece.FROG_SOUND);
        }
        int[] ml = wall(r, side, ca - 3, F0 + 4), mr = wall(r, side, ca + 2, F0 + 4);
        put(ml[0], ml[1], ml[2], VaultPiece.FROG_LIGHT);
        put(mr[0], mr[1], mr[2], VaultPiece.FROG_PATH);
        // abandoned: two chairs at a table, a pot, the supply barrel in a corner (placed by the cache feature)
        int tx = r.cx() + al[0] * 4 + in[0] * 3, tz = r.cz() + al[1] * 4 + in[1] * 3;
        put(tx, F0, tz, VaultPiece.TABLE);
        put(tx, F0 + 1, tz, VaultPiece.CANDLES);
        put(tx, F0, tz - 1, VaultPiece.CHAIR_N);
        put(tx, F0, tz + 1, VaultPiece.CHAIR_S);
        put(r.x0, F0, r.z1, VaultPiece.POT);
        cache = new int[] {r.x1, F0, r.z0};
    }

    /** The wall-plane block (cladding layer) of {@code side} at coordinate {@code a} along it and height y. */
    private static int[] wall(Room r, int side, int a, int y) {
        return switch (side) {
            case 0 -> new int[] {a, y, r.z0 - 1};
            case 2 -> new int[] {a, y, r.z1 + 1};
            case 3 -> new int[] {r.x0 - 1, y, a};
            default -> new int[] {r.x1 + 1, y, a};
        };
    }

    /** The open block in front of the wall of {@code side}. */
    private static int[] front(Room r, int side, int a, int y) {
        int[] w = wall(r, side, a, y), in = inward(side);
        return new int[] {w[0] + in[0], y, w[2] + in[1]};
    }

    private static int[] inward(int side) {
        return switch (side) {
            case 0 -> new int[] {0, 1};
            case 2 -> new int[] {0, -1};
            case 3 -> new int[] {1, 0};
            default -> new int[] {-1, 0};
        };
    }

    /** Unit step along a wall, left to right as seen by a player facing that wall from inside. */
    private static int[] along(int side) {
        return switch (side) {
            case 0 -> new int[] {1, 0};   // facing north: east is right
            case 2 -> new int[] {-1, 0};  // facing south: west is right
            case 3 -> new int[] {0, -1};  // facing west: north is right
            default -> new int[] {0, 1};  // facing east: south is right
        };
    }

    private static VaultPiece lever(int side) {
        return switch (side) { // the lever points away from its wall
            case 0 -> VaultPiece.LEVER_S;
            case 2 -> VaultPiece.LEVER_N;
            case 3 -> VaultPiece.LEVER_E;
            default -> VaultPiece.LEVER_W;
        };
    }

    /**
     * Light riddle on the outer wall: five levers at hip height, the five lamps above them, and above the lamps - behind a chiseled
     * band - the mural: the lamp pattern to reach (vertical comparison, lamp i under mural lamp i). Pedestal free-standing in the room.
     */
    private void lightChamber(Room r, int side) {
        box(r, S_PUZZLE, 0, 0);
        int ca = side == 0 || side == 2 ? r.cx() : r.cz();
        int[] al = along(side);
        int st = al[0] + al[1];
        target = 1 + pick(20, 0, 31);
        for (int i = 0; i < LEVERS; i++) {
            int a = ca + st * (-4 + 2 * i);
            levers[i] = front(r, side, a, r.y0 + 1);
            put(levers[i][0], levers[i][1], levers[i][2], lever(side));
            lamps[i] = wall(r, side, a, r.y0 + 3);
            put(lamps[i][0], lamps[i][1], lamps[i][2], VaultPiece.LAMP_OFF);
            int[] m = wall(r, side, a, r.y0 + 5);
            put(m[0], m[1], m[2], (target >> i & 1) == 1 ? VaultPiece.LAMP_ON : VaultPiece.LAMP_OFF);
        }
        for (int a = -5; a <= 5; a++) {
            for (int dy : new int[] {2, 4, 6}) {
                int[] p = wall(r, side, ca + st * a, r.y0 + dy);
                put(p[0], p[1], p[2], VaultPiece.CHISELED_DEEPSLATE);
            }
        }
        freePedestal(r, side, LIGHT);
        lightsAt(r, 3);
    }

    /** The riddle's seal pedestal, free-standing two blocks before the wall opposite the riddle wall. */
    private void freePedestal(Room r, int side, int part) {
        int depth = side == 0 || side == 2 ? r.z1 - r.z0 + 1 : r.x1 - r.x0 + 1;
        int ca = side == 0 || side == 2 ? r.cx() : r.cz();
        int[] w = wall(r, side, ca, r.y0), in = inward(side);
        int[] p = {w[0] + in[0] * (depth - 2), r.y0, w[2] + in[1] * (depth - 2)};
        pedestals[part] = p;
        put(p[0], p[1], p[2], VaultPiece.PEDESTALS[part]);
    }

    /** The mural (lamp pattern) a target forms; lamp i = levers i-1 and i (lever i toggles lamps i and i + 1). */
    public static int lampsFor(int leverBits) {
        int out = 0;
        for (int i = 0; i < LEVERS; i++) {
            if ((leverBits >> i & 1) == 0) continue;
            out ^= 1 << i;
            if (i + 1 < LEVERS) out ^= 1 << (i + 1);
        }
        return out;
    }

    /** The lever setting that shows {@code lamps} (unique: lamp 0 only depends on lever 0, lamp i on levers i-1 and i). */
    public static int leversFor(int lamps) {
        int levers = 0, prev = 0;
        for (int i = 0; i < LEVERS; i++) {
            int want = lamps >> i & 1, bit = want ^ prev;
            levers |= bit << i;
            prev = bit;
        }
        return levers;
    }

    private void soundChamber(Room r, int side) {
        box(r, S_PUZZLE, 0, 0);
        int ca = side == 0 || side == 2 ? r.cx() : r.cz();
        int[] al = along(side);
        for (int i = 0; i < SEQUENCE; i++) sequence[i] = pick(30, i, TONES);
        if (sequence[0] == sequence[1] && sequence[1] == sequence[2]) sequence[1] = (sequence[1] + 1) % TONES;
        boolean[] used = new boolean[TONES];
        for (int s : sequence) used[s] = true;
        for (int t = 0; t < TONES; t++) if (!used[t]) sequence[pick(31, t, SEQUENCE)] = t; // every crystal sounds at least once if possible
        // frieze: five tones left to right, each two blocks high at eye height, in a chiseled frame
        for (int i = -3; i <= 3; i++) {
            for (int dy = 2; dy <= 5; dy++) {
                int[] p = wall(r, side, ca + (al[0] + al[1]) * i, r.y0 + dy);
                boolean tile = (dy == 3 || dy == 4) && i >= -2 && i <= 2;
                put(p[0], p[1], p[2], tile ? VaultPiece.FRIEZE[sequence[i + 2]] : VaultPiece.CHISELED_DEEPSLATE);
            }
        }
        int[] perm = {0, 1, 2, 3};
        for (int i = 3; i > 0; i--) {
            int j = pick(32, i, i + 1), t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }
        int[] in = inward(side);
        for (int k = 0; k < TONES; k++) {
            int a = ca + (al[0] + al[1]) * (-3 + 2 * k);
            int[] w = wall(r, side, a, r.y0);
            int x = w[0] + in[0] * 4, z = w[2] + in[1] * 4;
            put(x, r.y0, z, VaultPiece.PLINTH);
            put(x, r.y0 + 1, z, VaultPiece.CRYSTALS[perm[k]]);
            crystals[k] = new int[] {x, r.y0 + 1, z};
            crystalTone[k] = perm[k];
        }
        freePedestal(r, side, SOUND);
        lightsAt(r, 3);
    }

    private void pathChamber(Room r, boolean turn) {
        box(r, S_PUZZLE, 0, 0);
        // rows run along the long axis (z, or x when turned); start platform 3 deep at the low end, end platform at the high end
        pathRow = turn ? new int[] {1, 0} : new int[] {0, 1};
        pathCol = turn ? new int[] {0, 1} : new int[] {1, 0};
        pathOrigin = turn ? new int[] {r.x0 + 3, r.y0 - 1, r.z0} : new int[] {r.x0, r.y0 - 1, r.z0 + 3};
        int c = pick(40, 0, PATH_COLS);
        for (int row = 0; row < PATH_ROWS; row++) {
            path.add(c + PATH_COLS * row);
            if (row == PATH_ROWS - 1) break;
            int to = pick(41, row, PATH_COLS);
            while (c != to) {
                c += Integer.signum(to - c);
                path.add(c + PATH_COLS * row);
            }
        }
        for (int row = 0; row < PATH_ROWS; row++) {
            for (int col = 0; col < PATH_COLS; col++) {
                boolean on = path.contains(col + PATH_COLS * row);
                for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                    int x = pathOrigin[0] + pathRow[0] * (2 * row + i) + pathCol[0] * (2 * col + j);
                    int z = pathOrigin[2] + pathRow[1] * (2 * row + i) + pathCol[1] * (2 * col + j);
                    put(x, r.y0 - 1, z, VaultPiece.PATH_TILE);
                    if (on) {
                        put(x, r.y1, z, VaultPiece.LICHEN);
                        put(x, r.y1 + 1, z, VaultPiece.LICHEN_BACK);
                    }
                }
            }
        }
        // dim: only the lichen, the lit tiles and a lantern over each platform
        put(turn ? r.x0 + 1 : r.cx(), r.y1, turn ? r.cz() : r.z0 + 1, VaultPiece.SOUL_LANTERN);
        put(turn ? r.x1 - 1 : r.cx(), r.y1, turn ? r.cz() : r.z1 - 1, VaultPiece.SOUL_LANTERN);
        // pedestal in the middle of the end platform; a chiseled threshold marks the start
        int mx = turn ? r.x1 - 1 : r.cx(), mz = turn ? r.cz() : r.z1 - 1;
        pedestals[PATH] = new int[] {mx, r.y0, mz};
        put(mx, r.y0, mz, VaultPiece.PEDESTAL_PATH);
        for (int a = 0; a < PATH_COLS * 2; a++) {
            int x = turn ? r.x0 + 2 : r.x0 + a, z = turn ? r.z0 + a : r.z0 + 2;
            put(x, r.y0 - 1, z, VaultPiece.CHISELED_DEEPSLATE);
        }
    }

    /** The world block of path cell (col, row), sub-block (i, j) of its 2 x 2. */
    public int[] pathBlock(int cell, int i, int j) {
        int col = cell % PATH_COLS, row = cell / PATH_COLS;
        return new int[] {pathOrigin[0] + pathRow[0] * (2 * row + i) + pathCol[0] * (2 * col + j), pathOrigin[1],
                pathOrigin[2] + pathRow[1] * (2 * row + i) + pathCol[1] * (2 * col + j)};
    }

    /** The path cell of a floor block, or -1. */
    public int pathCell(int x, int y, int z) {
        if (pathOrigin == null || y != pathOrigin[1]) return -1;
        int dx = x - pathOrigin[0], dz = z - pathOrigin[2];
        int along = dx * pathRow[0] + dz * pathRow[1], across = dx * pathCol[0] + dz * pathCol[1];
        if (along < 0 || across < 0 || along >= PATH_ROWS * 2 || across >= PATH_COLS * 2) return -1;
        return across / 2 + PATH_COLS * (along / 2);
    }

    private void lightsAt(Room r, int step) {
        for (int x = r.x0 + 2; x <= r.x1 - 2; x += step) for (int z = r.z0 + 2; z <= r.z1 - 2; z += step) put(x, r.y1 + 1, z, VaultPiece.SEA_LANTERN);
    }

    private void pillarHall(Room r) {
        box(r, S_HALL, 7, 0);
        for (int px = 4; px + 2 < r.x1 - r.x0; px += 7) {
            for (int pz = 4; pz + 2 < r.z1 - r.z0; pz += 7) fill(r.x0 + px, r.y0, r.z0 + pz, r.x0 + px + 2, r.y1, r.z0 + pz + 2, S_PILLAR);
        }
        if (pick(50, r.sx + 3 * r.sz, 3) == 0) put(r.cx(), r.y0, r.cz() + 3, VaultPiece.CHAIR_N); // one lone chair
    }

    /** Long gallery; pilasters (fills) along both long walls form niches with a pot, candles or a lantern - or nothing. */
    private void gallery(Room r, boolean turn) {
        box(r, S_HALL, 0, 0);
        int len = turn ? r.x1 - r.x0 + 1 : r.z1 - r.z0 + 1;
        for (int a = 2; a < len - 1; a += 5) {
            if (turn) {
                fill(r.x0 + a, r.y0, r.z0, r.x0 + a, r.y1, r.z0 + 1, S_PILLAR);
                fill(r.x0 + a, r.y0, r.z1 - 1, r.x0 + a, r.y1, r.z1, S_PILLAR);
            } else {
                fill(r.x0, r.y0, r.z0 + a, r.x0 + 1, r.y1, r.z0 + a, S_PILLAR);
                fill(r.x1 - 1, r.y0, r.z0 + a, r.x1, r.y1, r.z0 + a, S_PILLAR);
            }
            for (int sd = 0; sd < 2 && a + 2 < len - 1; sd++) {
                int kind = pick(51, a * 2 + sd + 100 * (r.sx + 3 * r.sz), 4);
                int nx = turn ? r.x0 + a + 2 : (sd == 0 ? r.x0 : r.x1), nz = turn ? (sd == 0 ? r.z0 : r.z1) : r.z0 + a + 2;
                if (kind == 0) put(nx, r.y0, nz, VaultPiece.POT);
                else if (kind == 1) put(nx, r.y0, nz, VaultPiece.CANDLES);
                else if (kind == 2) put(nx, r.y0 + 3, nz, VaultPiece.SOUL_LANTERN);
            }
        }
        int cx = r.cx(), cz = r.cz();
        for (int a = 2; a < len - 1; a += 5) put(turn ? r.x0 + a + 2 : cx, r.y1 + 1, turn ? cz : r.z0 + a + 2, VaultPiece.SEA_LANTERN);
    }

    private void cistern(Room r) {
        box(r, S_CISTERN, 6, 2);
        int in = 4;
        carve(r.x0 + in, r.y0 - 2, r.z0 + in, r.x1 - in, r.y0 - 1, r.z1 - in, S_POOL);
        for (int x = r.x0 + in; x <= r.x1 - in; x++) for (int z = r.z0 + in; z <= r.z1 - in; z++) {
            put(x, r.y0 - 2, z, VaultPiece.WATER);
            put(x, r.y0 - 1, z, VaultPiece.WATER);
        }
        fill(r.cx() - 1, r.y0 - 2, r.cz() - 1, r.cx() + 1, r.y0 - 1, r.cz() + 1, S_PILLAR); // island
        put(r.cx(), r.y0, r.cz(), VaultPiece.POT);
    }

    private void archive(Room r, boolean turn) {
        box(r, S_ARCHIVE, 5, 2);
        int s = pick(60, r.sx + 3 * r.sz, 1000);
        if (!turn) {
            for (int x = r.x0 + 1; x <= r.x1 - 1; x++) {
                if ((x - r.x0) % 4 == 0) continue;
                for (int y = r.y0; y <= r.y0 + 2; y++) {
                    put(x, y, r.z0, VaultPiece.SHELF_N);
                    put(x, y, r.z1, VaultPiece.SHELF_S);
                }
            }
        } else {
            for (int z = r.z0 + 1; z <= r.z1 - 1; z++) {
                if ((z - r.z0) % 4 == 0) continue;
                for (int y = r.y0; y <= r.y0 + 2; y++) {
                    put(r.x0, y, z, VaultPiece.SHELF_W);
                    put(r.x1, y, z, VaultPiece.SHELF_E);
                }
            }
        }
        put(r.cx() - 2, r.y0, r.cz(), VaultPiece.LECTERN);
        put(r.cx() + 2, r.y0, r.cz(), VaultPiece.TABLE);
        put(r.cx() + 2, r.y0, r.cz() - 1, VaultPiece.CHAIR_N);
        put(r.cx() + 2, r.y0 + 1, r.cz(), VaultPiece.CANDLES);
        if (s % 2 == 0) put(r.x0, r.y1, r.z0, VaultPiece.COBWEB);
        if (s % 3 == 0) put(r.x1, r.y1, r.z1, VaultPiece.COBWEB);
    }

    /** An empty room with one odd thing left behind (or nothing): a chair facing the wall, a pot, a lectern, candles. */
    private void emptyRoom(Room r) {
        box(r, S_HALL, 4, 1);
        switch (pick(52, r.sx + 3 * r.sz + 9 * Math.max(0, r.level), 5)) {
            case 0 -> put(r.cx(), r.y0, r.z0 + 1, VaultPiece.CHAIR_N);
            case 1 -> put(r.x1 - 1, r.y0, r.z1 - 1, VaultPiece.POT);
            case 2 -> put(r.cx(), r.y0, r.cz(), VaultPiece.LECTERN);
            case 3 -> put(r.x0 + 1, r.y0, r.cz(), VaultPiece.CANDLES);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- corridors

    /** Spanning tree over the 3 x 3 fields (hashed weights) plus extra edges: {level, slotA, slotB, index, guide}. */
    private void chooseEdges(int level) {
        List<int[]> all = new ArrayList<>();
        for (int s = 0; s < 9; s++) {
            if (s % 3 < 2) all.add(new int[] {s, s + 1, pick(70 + level, s * 2, 1000)});
            if (s / 3 < 2) all.add(new int[] {s, s + 3, pick(70 + level, s * 2 + 1, 1000)});
        }
        all.sort((a, b) -> Integer.compare(a[2], b[2]));
        int[] parent = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        for (int[] e : all) {
            int a = root(parent, e[0]), b = root(parent, e[1]);
            boolean tree = a != b;
            if (tree) parent[a] = b;
            if (tree || e[2] < 330) edges.add(new int[] {level, e[0], e[1], edges.size(), 0});
        }
    }

    /**
     * Orientation (UNDER21 rounds 3/4): breadth-first from the Ascent Hall over the corridors and the stairwells/hall (both
     * floors); in every room the one door on its shortest way to the hall gets a chiseled tuff frame (the hall's stone) - follow
     * the framed doors and you reach the altar. e[4]: 1 = mark the door of room A, 2 = of room B, 0 = none.
     */
    private void guideEdges(Type[][] plan) {
        int[] dist = new int[18];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        for (int level = 0; level < 2; level++) {
            dist[level * 9 + 4] = 0;
            q.add(level * 9 + 4);
        }
        while (!q.isEmpty()) {
            int n = q.poll(), level = n / 9, slot = n % 9;
            List<Integer> next = new ArrayList<>();
            for (int[] e : edges) {
                if (e[0] != level) continue;
                if (e[1] == slot) next.add(level * 9 + e[2]);
                if (e[2] == slot) next.add(level * 9 + e[1]);
            }
            if (plan[level][slot] == Type.STAIRWELL) next.add((1 - level) * 9 + slot);
            for (int m : next) {
                if (dist[m] == Integer.MAX_VALUE) {
                    dist[m] = dist[n] + (m / 9 == level ? 1 : 0);
                    q.add(m);
                }
            }
        }
        boolean[] marked = new boolean[18];
        for (int[] e : edges) {
            int na = e[0] * 9 + e[1], nb = e[0] * 9 + e[2];
            if (dist[na] == dist[nb] + 1 && !marked[na]) {
                e[4] = 1;
                marked[na] = true;
            } else if (dist[nb] == dist[na] + 1 && !marked[nb]) {
                e[4] = 2;
                marked[nb] = true;
            }
        }
    }

    private static int root(int[] p, int i) {
        while (p[i] != i) i = p[i];
        return i;
    }

    /** Corridor between rooms a (west / north) and b; L-shaped through the gap between the fields when they do not line up. */
    private void corridor(int level, Room a, Room b, boolean eastWest, int guide) {
        int f = level == 0 ? F0 : F1;
        if (eastWest) {
            int gap = slotX(a.sx + 1);
            int ta = clamp(a.cz() + pick(80, a.sx + 3 * a.sz + 9 * level, 5) - 2, a.z0 + 2, a.z1 - 2);
            int tb = clamp(b.cz() + pick(81, b.sx + 3 * b.sz + 9 * level, 5) - 2, b.z0 + 2, b.z1 - 2);
            segX(a.x1 + 1, gap + 1, ta, f);
            segZ(gap, ta, tb, f);
            segX(gap - 1, b.x0 - 1, tb, f);
            door(a, 1, ta, f, guide == 1);
            door(b, 3, tb, f, guide == 2);
        } else {
            int gap = slotZ(a.sz + 1);
            int ta = clamp(a.cx() + pick(82, a.sx + 3 * a.sz + 9 * level, 5) - 2, a.x0 + 2, a.x1 - 2);
            // the stairwell's landing stands in front of the west half of its north wall
            int lo = b.type == Type.STAIRWELL ? b.x0 + 9 : b.x0 + 2;
            int tb = clamp(b.cx() + pick(83, b.sx + 3 * b.sz + 9 * level, 5) - 2, lo, b.x1 - 2);
            segZ2(a.z1 + 1, gap + 1, ta, f);
            segX2(gap, ta, tb, f);
            segZ2(gap - 1, b.z0 - 1, tb, f);
            door(a, 2, ta, f, guide == 1);
            door(b, 0, tb, f, guide == 2);
        }
    }

    /**
     * Keeps the door of room r on {@code side} at coordinate t free (furniture, wall lights), marks riddle chambers, and frames
     * the door towards the Ascent Hall ({@code guide}) in chiseled tuff on the room side.
     */
    private void door(Room r, int side, int t, int f, boolean guide) {
        for (int a = t - 1; a <= t + 1; a++) {
            for (int y = f; y <= f + 4; y++) {
                int[] w = wall(r, side, a, y), in = front(r, side, a, y);
                decor.remove(key(w[0] - bx, y, w[2] - bz));
                decor.remove(key(in[0] - bx, y, in[2] - bz));
            }
        }
        int[] w = wall(r, side, t, f + 4);
        marker(r, w[0], f, w[2]);
        if (guide && r.type != Type.ALTAR) {
            for (int y = f; y <= f + 4; y++) {
                for (int a : new int[] {t - 2, t + 2}) {
                    int[] p = wall(r, side, a, y);
                    put(p[0], p[1], p[2], VaultPiece.CHISELED_TUFF_BRICKS);
                }
            }
            for (int a = t - 1; a <= t + 1; a += 2) {
                int[] p = wall(r, side, a, f + 4);
                put(p[0], p[1], p[2], VaultPiece.CHISELED_TUFF_BRICKS);
            }
        }
    }

    /** A froglight of the riddle's colour above the door of a riddle chamber (both faces of the wall). */
    private void marker(Room r, int x, int f, int z) {
        int part = switch (r.type) {
            case LIGHT -> LIGHT;
            case SOUND -> SOUND;
            case PATH -> PATH;
            default -> -1;
        };
        if (part < 0) return;
        put(x, f + 4, z, VaultPiece.MARKERS[part]);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void segX(int xa, int xb, int z, int f) { // along x at z
        carve(xa, f, z - 1, xb, f + 3, z + 1, S_CORRIDOR);
        carve(xa, f + 4, z, xb, f + 4, z, S_CORRIDOR);
        lanterns(Math.min(xa, xb), Math.max(xa, xb), z, f, true);
    }

    private void segZ(int x, int za, int zb, int f) { // along z at x (the gap part of an east-west corridor)
        carve(x - 1, f, Math.min(za, zb) - 1, x + 1, f + 3, Math.max(za, zb) + 1, S_CORRIDOR);
        carve(x, f + 4, Math.min(za, zb), x, f + 4, Math.max(za, zb), S_CORRIDOR);
    }

    private void segZ2(int za, int zb, int x, int f) {
        carve(x - 1, f, za, x + 1, f + 3, zb, S_CORRIDOR);
        carve(x, f + 4, za, x, f + 4, zb, S_CORRIDOR);
        lanterns(Math.min(za, zb), Math.max(za, zb), x, f, false);
    }

    private void segX2(int z, int xa, int xb, int f) {
        carve(Math.min(xa, xb) - 1, f, z - 1, Math.max(xa, xb) + 1, f + 3, z + 1, S_CORRIDOR);
        carve(Math.min(xa, xb), f + 4, z, Math.max(xa, xb), f + 4, z, S_CORRIDOR);
    }

    private void lanterns(int from, int to, int at, int f, boolean alongX) {
        for (int a = from + 3; a < to - 1; a += 6) put(alongX ? a : at, f + 4, alongX ? at : a, VaultPiece.SOUL_LANTERN);
    }
}
