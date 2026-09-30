package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.List;

/**
 * A matched, rectangular portal frame - the equivalent of vanilla's
 * {@code PortalShape} but loader-agnostic.
 *
 * <p>The frame is described by its interior: the lowest corner of the empty
 * area ({@code interiorMinX/Y/Z}), the interior {@code width} measured along the
 * horizontal {@link PortalAxis}, and the interior {@code height} measured along
 * Y. The surrounding frame blocks sit one block outside the interior on every
 * side.
 */
public record PortalFrame(
        PortalAxis axis,
        int interiorMinX,
        int interiorMinY,
        int interiorMinZ,
        int width,
        int height
) {
    public PortalFrame {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("portal interior must have positive size");
        }
    }

    /** World position of the interior cell at column {@code col} (along the axis) and row {@code row} (up). */
    public BlockPos3i interiorCell(int col, int row) {
        int x = axis == PortalAxis.X ? interiorMinX + col : interiorMinX;
        int y = interiorMinY + row;
        int z = axis == PortalAxis.Z ? interiorMinZ + col : interiorMinZ;
        return new BlockPos3i(x, y, z);
    }

    /** All interior cells (the cells that become portal blocks when active). */
    public List<BlockPos3i> interiorPositions() {
        List<BlockPos3i> out = new ArrayList<>(width * height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                out.add(interiorCell(col, row));
            }
        }
        return out;
    }

    public PortalBounds interiorBounds() {
        BlockPos3i lo = interiorCell(0, 0);
        BlockPos3i hi = interiorCell(width - 1, height - 1);
        return new PortalBounds(lo.x(), lo.y(), lo.z(), hi.x(), hi.y(), hi.z());
    }

    /** True if the given world position is one of this portal's interior cells. */
    public boolean containsInterior(int x, int y, int z) {
        if (y < interiorMinY || y >= interiorMinY + height) {
            return false;
        }
        if (axis == PortalAxis.X) {
            return z == interiorMinZ && x >= interiorMinX && x < interiorMinX + width;
        }
        return x == interiorMinX && z >= interiorMinZ && z < interiorMinZ + width;
    }
}
