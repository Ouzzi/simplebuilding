package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** N33: arch rules of the owner (straight bottom, sides only up or inward, free top, 4-dense, 1 × 2 .. 21 × 21). */
class ArchPortalScannerTest {

    private static final ArchPortalScanner NETHER = new ArchPortalScanner(ArchPortalScanner.Mode.ANY, 1, 21, 2, 21);
    private static final String OBSIDIAN = "minecraft:obsidian";

    // ---- valid shapes (the concept's examples) --------------------------------------------------------------------

    @Test
    void roundArchIsValidOnBothAxes() {
        for (PortalAxis axis : PortalAxis.values()) {
            Grid g = Grid.of(axis,
                    "  OOO  ",
                    " O...O ",
                    "O.....O",
                    "O.....O",
                    "O.....O",
                    "OOOOOOO");
            ArchPortalScanner.Result r = g.scan(NETHER, 3, 1);
            assertEquals(ArchPortalScanner.Verdict.OK, r.verdict(), "axis " + axis);
            assertEquals(axis, r.axis());
            assertEquals(18, r.interior().size());
            assertEquals(5, r.width());
            assertEquals(4, r.height());
            assertFalse(r.rectangle());
        }
    }

    @Test
    void pointedSteppedAndDoubleArchesAreValid() {
        Grid pointed = Grid.of(PortalAxis.X,
                "   O   ",
                "  O.O  ",
                " O...O ",
                "O.....O",
                "O.....O",
                "OOOOOOO");
        assertEquals(ArchPortalScanner.Verdict.OK, pointed.scan(NETHER, 3, 1).verdict());
        Grid stairs = Grid.of(PortalAxis.X,
                "OO   ",
                "O.O  ",
                "O..O ",
                "O...O",
                "OOOOO");
        assertEquals(ArchPortalScanner.Verdict.OK, stairs.scan(NETHER, 1, 1).verdict());
        Grid twin = Grid.of(PortalAxis.Z,
                " OO OO ",
                "O..O..O",
                "O.....O",
                "O.....O",
                "OOOOOOO");
        ArchPortalScanner.Result r = twin.scan(NETHER, 1, 1);
        assertEquals(ArchPortalScanner.Verdict.OK, r.verdict());
        assertEquals(14, r.interior().size());
    }

    @Test
    void smallestPortalIsOneByTwo() {
        Grid g = Grid.of(PortalAxis.X,
                " O ",
                "O.O",
                "O.O",
                " O ");
        ArchPortalScanner.Result r = g.scan(NETHER, 1, 1);
        assertEquals(ArchPortalScanner.Verdict.OK, r.verdict());
        assertTrue(r.rectangle());
        assertEquals(1, r.width());
        assertEquals(2, r.height());
        Grid one = Grid.of(PortalAxis.X,
                " O ",
                "O.O",
                " O ");
        assertEquals(ArchPortalScanner.Verdict.TOO_SMALL, one.scan(NETHER, 1, 1).verdict());
    }

    @Test
    void frameNeedsNoCornersAndIsEightConnected() {
        Grid g = Grid.of(PortalAxis.X,
                "  OOO  ",
                " O...O ",
                "O.....O",
                "O.....O",
                "OOOOOOO");
        ArchPortalScanner.Result r = g.scan(NETHER, 3, 1);
        assertEquals(ArchPortalScanner.Verdict.OK, r.verdict());
        // The two bottom corners and the diagonal steps' outer corners are not part of the frame.
        assertFalse(r.frame().contains(g.pos(0, 4)));
        assertFalse(r.frame().contains(g.pos(6, 4)));
        assertTrue(eightConnected(r.frame()));
        assertEquals(r.frame().size(), Set.copyOf(r.frame()).size());
    }

    @Test
    void largestPortalIsTwentyOneByTwentyOne() {
        assertEquals(ArchPortalScanner.Verdict.OK, Grid.rectangle(21, 21).scan(NETHER, 1, 1).verdict());
        assertEquals(ArchPortalScanner.Verdict.OPEN, Grid.rectangle(22, 5).scan(NETHER, 1, 1).verdict());
        assertEquals(ArchPortalScanner.Verdict.OPEN, Grid.rectangle(5, 22).scan(NETHER, 1, 1).verdict());
    }

    // ---- invalid shapes and their reasons --------------------------------------------------------------------------

    @Test
    void bumpInTheFloorIsRefused() {
        Grid g = Grid.of(PortalAxis.X,
                "OOOOO",
                "O...O",
                "O.O.O",
                "OOOOO");
        assertEquals(ArchPortalScanner.Verdict.BOTTOM, g.scan(NETHER, 1, 2).verdict());
    }

    @Test
    void sidesThatWidenOrOverhangAreRefused() {
        Grid shelf = Grid.of(PortalAxis.X,
                "OOOOO",
                "O...O",
                "OO..O",
                "O...O",
                "OOOOO");
        assertEquals(ArchPortalScanner.Verdict.SIDES, shelf.scan(NETHER, 1, 3).verdict());
        Grid funnel = Grid.of(PortalAxis.X,
                "OOOOOOO",
                "O.....O",
                " O...O ",
                "  OOO  ");
        assertEquals(ArchPortalScanner.Verdict.SIDES, funnel.scan(NETHER, 3, 2).verdict());
    }

    @Test
    void foreignBlockOrOpenFrameIsRefused() {
        Grid gap = Grid.of(PortalAxis.X,
                "OOOO",
                "O..#",
                "O..O",
                "OOOO");
        assertEquals(ArchPortalScanner.Verdict.GAP, gap.scan(NETHER, 1, 2).verdict());
        Grid open = Grid.of(PortalAxis.X,
                "O..O",
                "O..O",
                "OOOO");
        assertEquals(ArchPortalScanner.Verdict.OPEN, open.scan(NETHER, 1, 1).verdict());
        // Standing on the ground: the fill runs over the frame down to the dirt, still "open", not "foreign block".
        Grid grounded = Grid.of(PortalAxis.X,
                " O..O ",
                " O..O ",
                " OOOO ",
                "######");
        assertEquals(ArchPortalScanner.Verdict.OPEN, grounded.scan(NETHER, 2, 1).verdict());
    }

    @Test
    void modesSeparateRectanglesFromArches() {
        ArchPortalScanner rectangles = new ArchPortalScanner(ArchPortalScanner.Mode.RECTANGLE, 1, 21, 2, 21);
        ArchPortalScanner arches = new ArchPortalScanner(ArchPortalScanner.Mode.ARCH, 1, 21, 2, 21);
        Grid round = Grid.of(PortalAxis.X,
                "  O  ",
                " O.O ",
                "O...O",
                "O...O",
                "OOOOO");
        assertEquals(ArchPortalScanner.Verdict.RECTANGLE_ONLY, rectangles.scan(round.frame(), round.interior(), round.x(2, 1), round.y(1), round.z(2, 1), PortalAxis.X).verdict());
        assertEquals(ArchPortalScanner.Verdict.OK, arches.scan(round.frame(), round.interior(), round.x(2, 1), round.y(1), round.z(2, 1), PortalAxis.X).verdict());
        Grid box = Grid.rectangle(2, 3);
        assertEquals(ArchPortalScanner.Verdict.ARCH_ONLY, arches.scan(box.frame(), box.interior(), box.x(1, 1), box.y(1), box.z(1, 1), PortalAxis.X).verdict());
        assertEquals(ArchPortalScanner.Verdict.OK, rectangles.scan(box.frame(), box.interior(), box.x(1, 1), box.y(1), box.z(1, 1), PortalAxis.X).verdict());
        assertEquals(ArchPortalScanner.Mode.ANY, ArchPortalScanner.Mode.parse(null, ArchPortalScanner.Mode.ANY));
        assertEquals(ArchPortalScanner.Mode.ARCH, ArchPortalScanner.Mode.parse(" Arch ", null));
        assertNull(ArchPortalScanner.Mode.parse("circle", ArchPortalScanner.Mode.ANY));
    }

    @Test
    void existingPortalMustBeCompletelyFilled() {
        Grid g = Grid.of(PortalAxis.X,
                " OO ",
                "O..O",
                "O..O",
                "OOOO");
        // Interior predicate "portal block only": one air cell left inside breaks the shape (block dissolves).
        Set<Long> portal = new HashSet<>();
        for (int[] c : new int[][]{{1, 1}, {2, 1}, {1, 2}, {2, 2}}) {
            portal.add(Grid.key(g.x(c[0], c[1]), g.y(c[1]), g.z(c[0], c[1])));
        }
        ArchPortalScanner.Cells inside = (x, y, z) -> portal.contains(Grid.key(x, y, z));
        assertTrue(NETHER.scan(g.frame(), inside, g.x(1, 2), g.y(2), g.z(1, 2), PortalAxis.X).ok());
        portal.remove(Grid.key(g.x(2, 1), g.y(1), g.z(2, 1)));
        assertEquals(ArchPortalScanner.Verdict.GAP, NETHER.scan(g.frame(), inside, g.x(1, 2), g.y(2), g.z(1, 2), PortalAxis.X).verdict());
    }

    @Test
    void onlyEnclosedCellsCountAsAttempts() {
        Grid floor = Grid.of(PortalAxis.X, "OOOOO");
        assertFalse(NETHER.enclosed(floor.frame(), floor.interior(), floor.x(2, -1), floor.y(-1), floor.z(2, -1), PortalAxis.X));
        Grid open = Grid.of(PortalAxis.X,
                "O..O",
                "O..O",
                "OOOO");
        assertTrue(NETHER.enclosed(open.frame(), open.interior(), open.x(1, 1), open.y(1), open.z(1, 1), PortalAxis.X));
        assertFalse(NETHER.enclosed(open.frame(), open.interior(), open.x(1, 1), open.y(1), open.z(1, 1), PortalAxis.Z));
    }

    // ---- per-config shape rules ("shape" in the portal config) -----------------------------------------------------

    @Test
    void fallbackFrameAcceptsArchesByDefault() {
        DimensionPortalConfig cfg = new DimensionPortalConfig();
        cfg.portalRecipes = new java.util.ArrayList<>();
        cfg.frameBlock = "minecraft:crying_obsidian";
        List<PortalShapeRule> rules = cfg.buildShapeRules();
        assertEquals(1, rules.size());
        assertEquals(ArchPortalScanner.Mode.ANY, rules.getFirst().mode());
        Grid g = Grid.of(PortalAxis.X,
                "  OOO  ",
                " O...O ",
                "O.....O",
                "O.....O",
                "O.....O",
                "OOOOOOO").block('O', "minecraft:crying_obsidian");
        assertTrue(PortalActivationService.match(g, cfg, g.x(3, 2), g.y(2), g.z(3, 2)).isPresent());
        cfg.portalShape = "rectangle";
        assertTrue(PortalActivationService.match(g, cfg, g.x(3, 2), g.y(2), g.z(3, 2)).isEmpty());
        assertEquals(ArchPortalScanner.Verdict.RECTANGLE_ONLY,
                PortalActivationService.explain(g, cfg, g.x(3, 4), g.y(4), g.z(3, 4)).orElseThrow().verdict());
    }

    @Test
    void recipesCanMixPatternsAndFreeForms() {
        DimensionPortalConfig cfg = DimensionPortalConfig.defaultSkyblock();
        assertTrue(cfg.buildShapeRules().isEmpty(), "pattern recipes keep their old meaning");
        DimensionPortalConfig.PortalRecipeConfig arch = new DimensionPortalConfig.PortalRecipeConfig();
        arch.shape = "arch";
        arch.frame = "minecraft:crying_obsidian";
        arch.maxWidth = 5;
        cfg.portalRecipes.add(arch);
        assertEquals(6, cfg.buildRecipes().size(), "free forms are no patterns");
        List<PortalShapeRule> rules = cfg.buildShapeRules();
        assertEquals(1, rules.size());
        assertEquals(new PortalShapeRule("minecraft:crying_obsidian", ArchPortalScanner.Mode.ARCH, 1, 5, 2, 21), rules.getFirst());
        Grid wide = Grid.of(PortalAxis.X,
                "  OOOO  ",
                " O....O ",
                "O......O",
                "O......O",
                "OOOOOOOO").block('O', "minecraft:crying_obsidian");
        assertTrue(PortalActivationService.match(wide, cfg, wide.x(3, 3), wide.y(3), wide.z(3, 3)).isEmpty(), "6 wide > max 5");
        PortalActivationService.Refusal refusal = PortalActivationService.explain(wide, cfg, wide.x(3, 3), wide.y(3), wide.z(3, 3)).orElseThrow();
        assertEquals(ArchPortalScanner.Verdict.OPEN, refusal.verdict());
        assertEquals(5, refusal.rule().maxWidth());
        Grid fits = Grid.of(PortalAxis.Z,
                " OOO ",
                "O...O",
                "O...O",
                "OOOOO").block('O', "minecraft:crying_obsidian");
        fits.set(0, 0, 'O');
        fits.set(4, 0, 'O');
        // a plain 3 × 2 rectangle: refused by "arch", accepted once the top gets a step
        assertTrue(PortalActivationService.match(fits, cfg, fits.x(2, 2), fits.y(2), fits.z(2, 2)).isEmpty());
        fits.set(2, 0, '.');
        fits.set(2, -1, 'O');
        assertTrue(PortalActivationService.match(fits, cfg, fits.x(2, 2), fits.y(2), fits.z(2, 2)).isPresent());
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------

    private static boolean eightConnected(List<BlockPos3i> cells) {
        Set<BlockPos3i> left = new HashSet<>(cells);
        java.util.ArrayDeque<BlockPos3i> queue = new java.util.ArrayDeque<>();
        queue.add(cells.getFirst());
        left.remove(cells.getFirst());
        while (!queue.isEmpty()) {
            BlockPos3i c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos3i n = new BlockPos3i(c.x() + dx, c.y() + dy, c.z() + dz);
                        if (left.remove(n)) {
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return left.isEmpty();
    }

    /**
     * A vertical drawing in world space: rows top to bottom, 'O' frame, '.' or ' ' air, '#' a foreign block.
     * Everything outside the drawing is air too, so a gap really leaks. Column c of row r sits at
     * along = 10 + c, y = 64 + (rows - 1 - r); the other horizontal coordinate is 7.
     */
    private static final class Grid implements PortalWorldView {
        private final PortalAxis axis;
        private final int rows;
        private final Map<Long, Character> cells = new HashMap<>();
        private final Map<Character, String> ids = new HashMap<>(Map.of('O', OBSIDIAN, '#', "minecraft:dirt"));

        private Grid(PortalAxis axis, int rows) {
            this.axis = axis;
            this.rows = rows;
        }

        static Grid of(PortalAxis axis, String... lines) {
            Grid g = new Grid(axis, lines.length);
            for (int r = 0; r < lines.length; r++) {
                for (int c = 0; c < lines[r].length(); c++) {
                    g.set(c, r, lines[r].charAt(c));
                }
            }
            return g;
        }

        static Grid rectangle(int width, int height) {
            String[] lines = new String[height + 2];
            lines[0] = lines[height + 1] = "O".repeat(width + 2);
            for (int r = 1; r <= height; r++) {
                lines[r] = "O" + ".".repeat(width) + "O";
            }
            return of(PortalAxis.X, lines);
        }

        Grid block(char symbol, String id) {
            ids.put(symbol, id);
            return this;
        }

        void set(int col, int row, char symbol) {
            long k = key(x(col, row), y(row), z(col, row));
            if (symbol == '.' || symbol == ' ') {
                cells.remove(k);
            } else {
                cells.put(k, symbol);
            }
        }

        int x(int col, int row) {
            return axis == PortalAxis.X ? 10 + col : 7;
        }

        int y(int row) {
            return 64 + (rows - 1 - row);
        }

        int z(int col, int row) {
            return axis == PortalAxis.Z ? 10 + col : 7;
        }

        BlockPos3i pos(int col, int row) {
            return new BlockPos3i(x(col, row), y(row), z(col, row));
        }

        ArchPortalScanner.Result scan(ArchPortalScanner scanner, int col, int row) {
            return scanner.scan(frame(), interior(), x(col, row), y(row), z(col, row), axis);
        }

        ArchPortalScanner.Cells frame() {
            return (x, y, z) -> isBlock(x, y, z, OBSIDIAN);
        }

        ArchPortalScanner.Cells interior() {
            return this::isInterior;
        }

        static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
        }

        @Override
        public boolean isFrame(int x, int y, int z) {
            return isBlock(x, y, z, OBSIDIAN);
        }

        @Override
        public boolean isBlock(int x, int y, int z, String blockId) {
            Character c = cells.get(key(x, y, z));
            return c != null && blockId.equals(ids.get(c));
        }

        @Override
        public boolean isEmpty(int x, int y, int z) {
            return !cells.containsKey(key(x, y, z));
        }

        @Override
        public boolean isPortal(int x, int y, int z) {
            return false;
        }
    }
}
