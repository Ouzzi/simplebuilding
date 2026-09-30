package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * A matched portal in the world, in concrete world coordinates: the cells that become
 * portal blocks ({@link #interior()}) and the frame blocks that hold them
 * ({@link #frame()}, each with its block id so the exact shape can be rebuilt at a
 * destination). Produced both by the rectangular scanner ({@link #fromRectangular}) and
 * the recipe matcher, so the loaders have a single code path.
 */
public final class MatchedPortal {

    public record FrameBlock(BlockPos3i pos, String blockId) {
    }

    private final PortalAxis axis;
    private final List<BlockPos3i> interior;
    private final List<FrameBlock> frame;
    private final PortalBounds interiorBounds;

    public MatchedPortal(PortalAxis axis, List<BlockPos3i> interior, List<FrameBlock> frame) {
        if (interior == null || interior.isEmpty()) {
            throw new IllegalArgumentException("a portal must have at least one interior cell");
        }
        this.axis = axis;
        this.interior = List.copyOf(interior);
        this.frame = List.copyOf(frame);
        this.interiorBounds = computeBounds(this.interior);
    }

    public PortalAxis axis() {
        return axis;
    }

    public List<BlockPos3i> interior() {
        return interior;
    }

    public List<FrameBlock> frame() {
        return frame;
    }

    public PortalBounds interiorBounds() {
        return interiorBounds;
    }

    /** Stable anchor used for coordinate mapping across dimensions: the interior min corner. */
    public BlockPos3i anchor() {
        return new BlockPos3i(interiorBounds.minX(), interiorBounds.minY(), interiorBounds.minZ());
    }

    public boolean containsInterior(int x, int y, int z) {
        for (BlockPos3i cell : interior) {
            if (cell.x() == x && cell.y() == y && cell.z() == z) {
                return true;
            }
        }
        return false;
    }

    /** Where to place an arriving player: the middle of the lowest interior row. */
    public BlockPos3i landing() {
        int minY = interiorBounds.minY();
        List<BlockPos3i> bottom = new ArrayList<>();
        for (BlockPos3i cell : interior) {
            if (cell.y() == minY) {
                bottom.add(cell);
            }
        }
        bottom.sort((a, b) -> a.x() != b.x() ? Integer.compare(a.x(), b.x()) : Integer.compare(a.z(), b.z()));
        return bottom.get(bottom.size() / 2);
    }

    /** A copy translated by the given delta (used to rebuild the same shape at a destination). */
    public MatchedPortal translate(int dx, int dy, int dz) {
        List<BlockPos3i> movedInterior = new ArrayList<>(interior.size());
        for (BlockPos3i c : interior) {
            movedInterior.add(new BlockPos3i(c.x() + dx, c.y() + dy, c.z() + dz));
        }
        List<FrameBlock> movedFrame = new ArrayList<>(frame.size());
        for (FrameBlock f : frame) {
            movedFrame.add(new FrameBlock(new BlockPos3i(f.pos().x() + dx, f.pos().y() + dy, f.pos().z() + dz), f.blockId()));
        }
        return new MatchedPortal(axis, movedInterior, movedFrame);
    }

    /**
     * A floor laid around the base of a freshly generated portal, using the portal's
     * <b>bottom-row</b> blocks tiled in the same left-to-right pattern. Used to give the
     * portal ground when it materialises over a void (skyblock/travel). Caller only places
     * these where the world is currently empty.
     */
    public List<FrameBlock> platformBlocks(int radius) {
        boolean alongX = axis == PortalAxis.X;
        int minFrameY = Integer.MAX_VALUE;
        for (FrameBlock fb : frame) {
            minFrameY = Math.min(minFrameY, fb.pos().y());
        }
        int perpBase = alongX ? interiorBounds.minZ() : interiorBounds.minX();

        TreeMap<Integer, String> bottom = new TreeMap<>();
        for (FrameBlock fb : frame) {
            if (fb.pos().y() != minFrameY) {
                continue;
            }
            bottom.put(alongX ? fb.pos().x() : fb.pos().z(), fb.blockId());
        }

        List<FrameBlock> out = new ArrayList<>();
        if (bottom.isEmpty()) {
            return out;
        }
        int minAlong = bottom.firstKey();
        int maxAlong = bottom.lastKey();
        int patternLength = maxAlong - minAlong + 1;
        for (int dPerp = -radius; dPerp <= radius; dPerp++) {
            for (int a = minAlong - radius; a <= maxAlong + radius; a++) {
                String blockId = bottom.get(minAlong + Math.floorMod(a - minAlong, patternLength));
                if (blockId == null) {
                    continue;
                }
                int perp = perpBase + dPerp;
                BlockPos3i pos = alongX
                        ? new BlockPos3i(a, minFrameY, perp)
                        : new BlockPos3i(perp, minFrameY, a);
                out.add(new FrameBlock(pos, blockId));
            }
        }
        return out;
    }

    /**
     * Cells to clear to air so a generated portal is enterable even when it materialises
     * inside solid terrain (a cave/stone), mirroring how a nether portal carves its pocket.
     * Only the columns in front of and behind the interior are cleared; the portal plane
     * (frame + portal blocks) is left intact. Caller clears these only if currently solid.
     */
    public List<BlockPos3i> clearanceCells(int clearance, int headroom) {
        boolean alongX = axis == PortalAxis.X;
        int perpBase = alongX ? interiorBounds.minZ() : interiorBounds.minX();
        int minAlong = alongX ? interiorBounds.minX() : interiorBounds.minZ();
        int maxAlong = alongX ? interiorBounds.maxX() : interiorBounds.maxZ();
        int minY = interiorBounds.minY();
        int maxY = interiorBounds.maxY();

        List<BlockPos3i> out = new ArrayList<>();
        for (int dPerp = -clearance; dPerp <= clearance; dPerp++) {
            if (dPerp == 0) {
                continue; // the portal plane stays
            }
            int perp = perpBase + dPerp;
            for (int a = minAlong; a <= maxAlong; a++) {
                for (int y = minY; y <= maxY + headroom; y++) {
                    out.add(alongX ? new BlockPos3i(a, y, perp) : new BlockPos3i(perp, y, a));
                }
            }
        }
        return out;
    }

    /** Build a matched portal from a rectangular frame of a single block. */
    public static MatchedPortal fromRectangular(PortalFrame frame, String frameBlockId) {
        List<BlockPos3i> interior = frame.interiorPositions();
        List<FrameBlock> ring = new ArrayList<>();
        int ax = frame.axis() == PortalAxis.X ? 1 : 0;
        int az = frame.axis() == PortalAxis.Z ? 1 : 0;
        int ix = frame.interiorMinX();
        int iy = frame.interiorMinY();
        int iz = frame.interiorMinZ();
        int w = frame.width();
        int h = frame.height();
        for (int col = -1; col <= w; col++) {
            ring.add(new FrameBlock(new BlockPos3i(ix + ax * col, iy - 1, iz + az * col), frameBlockId));
            ring.add(new FrameBlock(new BlockPos3i(ix + ax * col, iy + h, iz + az * col), frameBlockId));
        }
        for (int row = 0; row < h; row++) {
            ring.add(new FrameBlock(new BlockPos3i(ix - ax, iy + row, iz - az), frameBlockId));
            ring.add(new FrameBlock(new BlockPos3i(ix + ax * w, iy + row, iz + az * w), frameBlockId));
        }
        return new MatchedPortal(frame.axis(), interior, ring);
    }

    private static PortalBounds computeBounds(List<BlockPos3i> cells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos3i c : cells) {
            minX = Math.min(minX, c.x());
            minY = Math.min(minY, c.y());
            minZ = Math.min(minZ, c.z());
            maxX = Math.max(maxX, c.x());
            maxY = Math.max(maxY, c.y());
            maxZ = Math.max(maxZ, c.z());
        }
        return new PortalBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
