package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalRecipeMatcherTest {

    private static final Map<String, String> LEGEND = Map.of(
            "O", "minecraft:obsidian",
            "G", "minecraft:glowstone"
    );

    @Test
    void mixedBlockRecipeMatchesWhenBuiltCorrectly() {
        PortalRecipe recipe = PortalRecipe.parse(List.of(
                "GOOG",
                "O..O",
                "O..O",
                "O..O",
                "GOOG"
        ), LEGEND, '.', ' ');

        FakeWorld world = new FakeWorld();
        build(world, recipe, 0, 70, 0); // axis Z, fixed X=0

        // Light an interior cell: recipe (1,1) -> world (x=0, y=69, z=1)
        Optional<MatchedPortal> match = PortalRecipeMatcher.match(world, List.of(recipe), 0, 69, 1);
        assertTrue(match.isPresent());
        assertEquals(6, match.get().interior().size()); // 2 wide x 3 tall interior
        assertEquals(PortalAxis.Z, match.get().axis());
        // corners are glowstone, edges obsidian -> recorded in the frame
        assertTrue(match.get().frame().stream().anyMatch(f -> f.blockId().equals("minecraft:glowstone")));
        assertTrue(match.get().frame().stream().anyMatch(f -> f.blockId().equals("minecraft:obsidian")));
    }

    @Test
    void generatedSiteHelpersProduceASingleFloorLayerAndClearance() {
        PortalRecipe recipe = PortalRecipe.parse(List.of(
                "OOOO",
                "O..O",
                "O..O",
                "OOOO"
        ), LEGEND, '.', ' ');
        FakeWorld world = new FakeWorld();
        build(world, recipe, 0, 70, 0);
        MatchedPortal m = PortalRecipeMatcher.match(world, List.of(recipe), 0, 69, 1).orElseThrow();

        assertFalse(m.platformBlocks(2).isEmpty());
        long floorLayers = m.platformBlocks(2).stream().map(fb -> fb.pos().y()).distinct().count();
        assertEquals(1, floorLayers); // the floor is a single layer
        assertFalse(m.clearanceCells(1, 1).isEmpty());
    }

    @Test
    void missingFrameBlockBreaksTheMatch() {
        PortalRecipe recipe = PortalRecipe.parse(List.of(
                "OOOO",
                "O..O",
                "O..O",
                "O..O",
                "OOOO"
        ), LEGEND, '.', ' ');

        FakeWorld world = new FakeWorld();
        build(world, recipe, 0, 70, 0);
        world.blocks.remove(key(0, 70, 0)); // knock out a corner

        assertTrue(PortalRecipeMatcher.match(world, List.of(recipe), 0, 69, 1).isEmpty());
    }

    @Test
    void wrongBlockTypeBreaksTheMatch() {
        PortalRecipe recipe = PortalRecipe.parse(List.of(
                "OOO",
                "O.O",
                "O.O",
                "OOO"
        ), LEGEND, '.', ' ');
        FakeWorld world = new FakeWorld();
        build(world, recipe, 0, 70, 0);
        world.blocks.put(key(0, 70, 1), "minecraft:stone"); // wrong block in the frame

        assertTrue(PortalRecipeMatcher.match(world, List.of(recipe), 0, 69, 1).isEmpty());
    }

    // Place a recipe into the fake world on the Z axis with the interior bottom-left near (fixedX, topY, originZ).
    private static void build(FakeWorld world, PortalRecipe recipe, int fixedX, int topY, int originZ) {
        for (int row = 0; row < recipe.height(); row++) {
            for (int col = 0; col < recipe.width(); col++) {
                int wx = fixedX;
                int wy = topY - row;
                int wz = originZ + col;
                switch (recipe.cellAt(col, row)) {
                    case FRAME -> world.blocks.put(key(wx, wy, wz), recipe.blockAt(col, row));
                    case INTERIOR -> world.empty.add(key(wx, wy, wz));
                    case IGNORE -> { /* leave untouched */ }
                }
            }
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }

    private static final class FakeWorld implements PortalWorldView {
        final Map<Long, String> blocks = new HashMap<>();
        final HashSet<Long> empty = new HashSet<>();
        final HashSet<Long> portal = new HashSet<>();

        @Override
        public boolean isFrame(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean isBlock(int x, int y, int z, String blockId) {
            return blockId != null && blockId.equals(blocks.get(key(x, y, z)));
        }

        @Override
        public boolean isEmpty(int x, int y, int z) {
            return empty.contains(key(x, y, z));
        }

        @Override
        public boolean isPortal(int x, int y, int z) {
            return portal.contains(key(x, y, z));
        }
    }
}
