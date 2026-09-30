package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalFrameScannerTest {

    @Test
    void minimalVanillaFrameActivatesWithoutAnyLight() {
        FakeWorld world = new FakeWorld();
        // Vanilla minimum: 2 wide x 3 tall interior, frame all around, on the Z axis.
        buildFrame(world, PortalAxis.Z, 0, 64, 0, 2, 3);

        PortalFrameScanner scanner = new PortalFrameScanner(2, 21, 3, 21);
        Optional<PortalFrame> frame = scanner.find(world, 0, 65, 0); // inside the interior

        assertTrue(frame.isPresent(), "a complete frame must activate with no light source");
        assertEquals(2, frame.get().width());
        assertEquals(3, frame.get().height());
        assertEquals(PortalAxis.Z, frame.get().axis());
    }

    @Test
    void largerRectangularFrameIsDetected() {
        FakeWorld world = new FakeWorld();
        buildFrame(world, PortalAxis.X, 10, 70, 5, 5, 4);

        PortalFrameScanner scanner = new PortalFrameScanner(2, 21, 3, 21);
        Optional<PortalFrame> frame = scanner.find(world, 12, 71, 5);

        assertTrue(frame.isPresent());
        assertEquals(5, frame.get().width());
        assertEquals(4, frame.get().height());
        assertEquals(PortalAxis.X, frame.get().axis());
    }

    @Test
    void incompleteFrameDoesNotActivate() {
        FakeWorld world = new FakeWorld();
        buildFrame(world, PortalAxis.Z, 0, 64, 0, 2, 3);
        // Knock a hole in the top frame.
        world.frame.remove(key(0, 67, 0));

        PortalFrameScanner scanner = new PortalFrameScanner(2, 21, 3, 21);
        assertTrue(scanner.find(world, 0, 65, 0).isEmpty());
    }

    @Test
    void interiorTooSmallIsRejected() {
        FakeWorld world = new FakeWorld();
        buildFrame(world, PortalAxis.Z, 0, 64, 0, 2, 2); // only 2 tall

        PortalFrameScanner scanner = new PortalFrameScanner(2, 21, 3, 21);
        assertTrue(scanner.find(world, 0, 65, 0).isEmpty());
    }

    @Test
    void existingPortalBlocksStillCountAsInterior() {
        FakeWorld world = new FakeWorld();
        buildFrame(world, PortalAxis.Z, 0, 64, 0, 2, 3);
        // Fill interior with portal blocks (already-active portal) and re-scan.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                world.portal.add(key(col, 64 + 1 + row, 0));
                world.empty.remove(key(col, 64 + 1 + row, 0));
            }
        }
        PortalFrameScanner scanner = new PortalFrameScanner(2, 21, 3, 21);
        assertTrue(scanner.find(world, 0, 65, 0).isPresent());
    }

    // Builds a frame whose interior lower corner is (minX, minY, minZ) with the given interior size.
    private static void buildFrame(FakeWorld world, PortalAxis axis, int minX, int minY, int minZ, int width, int height) {
        int ax = axis == PortalAxis.X ? 1 : 0;
        int az = axis == PortalAxis.Z ? 1 : 0;
        // interior empties
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                world.empty.add(key(minX + ax * col, minY + row, minZ + az * col));
            }
        }
        // bottom + top frame rows
        for (int col = -1; col <= width; col++) {
            world.frame.add(key(minX + ax * col, minY - 1, minZ + az * col));
            world.frame.add(key(minX + ax * col, minY + height, minZ + az * col));
        }
        // left + right frame columns
        for (int row = 0; row < height; row++) {
            world.frame.add(key(minX - ax, minY + row, minZ - az));
            world.frame.add(key(minX + ax * width, minY + row, minZ + az * width));
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }

    private static final class FakeWorld implements PortalWorldView {
        final Set<Long> frame = new HashSet<>();
        final Set<Long> empty = new HashSet<>();
        final Set<Long> portal = new HashSet<>();

        @Override
        public boolean isFrame(int x, int y, int z) {
            return frame.contains(key(x, y, z));
        }

        @Override
        public boolean isBlock(int x, int y, int z, String blockId) {
            return false;
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
