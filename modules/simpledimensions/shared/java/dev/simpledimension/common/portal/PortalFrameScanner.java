package dev.simpledimension.common.portal;

import java.util.Optional;

/**
 * Detects a rectangular portal frame around a position, mirroring the behaviour
 * of vanilla's nether portal ({@code net.minecraft.world.level.block.PortalShape}).
 *
 * <p>Activation requires only a complete frame of the configured block with an
 * empty interior - there is no light requirement. The accepted interior size
 * range (width along the horizontal axis, height along Y) is configurable so a
 * pack author can describe the "portal constellation" they want.
 */
public final class PortalFrameScanner {

    private final int minWidth;
    private final int maxWidth;
    private final int minHeight;
    private final int maxHeight;

    public PortalFrameScanner(int minWidth, int maxWidth, int minHeight, int maxHeight) {
        if (minWidth < 1 || minHeight < 1) {
            throw new IllegalArgumentException("minimum interior size must be >= 1");
        }
        if (maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("maximum interior size must be >= minimum");
        }
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    /**
     * Try to find a valid frame whose interior contains the given cell. Both
     * horizontal orientations are attempted. The cell itself must be an interior
     * candidate (empty or an existing portal block).
     */
    public Optional<PortalFrame> find(PortalWorldView world, int x, int y, int z) {
        for (PortalAxis axis : PortalAxis.values()) {
            Optional<PortalFrame> frame = findOnAxis(world, x, y, z, axis);
            if (frame.isPresent()) {
                return frame;
            }
        }
        return Optional.empty();
    }

    private Optional<PortalFrame> findOnAxis(PortalWorldView world, int sx, int sy, int sz, PortalAxis axis) {
        if (!world.isInterior(sx, sy, sz)) {
            return Optional.empty();
        }

        int ax = axis == PortalAxis.X ? 1 : 0;
        int az = axis == PortalAxis.Z ? 1 : 0;

        // 1. Descend to the bottom-most interior cell of this column.
        int by = sy;
        int floorLimit = sy - (maxHeight + 1);
        while (by > floorLimit && world.isInterior(sx, by - 1, sz)) {
            by--;
        }
        // The cell directly below the interior must be the frame floor.
        if (!world.isFrame(sx, by - 1, sz)) {
            return Optional.empty();
        }

        // 2. Walk towards the negative axis direction to find the left interior edge.
        int leftSteps = 0;
        while (leftSteps < maxWidth) {
            int cx = sx - ax * (leftSteps + 1);
            int cz = sz - az * (leftSteps + 1);
            if (world.isInterior(cx, by, cz) && world.isFrame(cx, by - 1, cz)) {
                leftSteps++;
            } else {
                break;
            }
        }
        int blX = sx - ax * leftSteps;
        int blZ = sz - az * leftSteps;

        // 3. Measure interior width along the axis from the left edge.
        int width = 0;
        while (width < maxWidth + 1) {
            int cx = blX + ax * width;
            int cz = blZ + az * width;
            if (world.isInterior(cx, by, cz) && world.isFrame(cx, by - 1, cz)) {
                width++;
            } else {
                break;
            }
        }
        if (width < minWidth || width > maxWidth) {
            return Optional.empty();
        }

        // The columns just outside the interior (bottom row) must be frame.
        if (!world.isFrame(blX - ax, by, blZ - az)) {
            return Optional.empty();
        }
        if (!world.isFrame(blX + ax * width, by, blZ + az * width)) {
            return Optional.empty();
        }

        // 4. Measure interior height: each row interior is empty and both side columns are frame.
        int height = 0;
        while (height < maxHeight + 1) {
            int ry = by + height;
            boolean rowInterior = true;
            for (int col = 0; col < width; col++) {
                if (!world.isInterior(blX + ax * col, ry, blZ + az * col)) {
                    rowInterior = false;
                    break;
                }
            }
            if (!rowInterior) {
                break;
            }
            if (!world.isFrame(blX - ax, ry, blZ - az)) {
                break;
            }
            if (!world.isFrame(blX + ax * width, ry, blZ + az * width)) {
                break;
            }
            height++;
        }
        if (height < minHeight || height > maxHeight) {
            return Optional.empty();
        }

        // 5. The top row above the interior must be frame.
        for (int col = 0; col < width; col++) {
            if (!world.isFrame(blX + ax * col, by + height, blZ + az * col)) {
                return Optional.empty();
            }
        }

        return Optional.of(new PortalFrame(axis, blX, by, blZ, width, height));
    }

    public int minWidth() {
        return minWidth;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int minHeight() {
        return minHeight;
    }

    public int maxHeight() {
        return maxHeight;
    }
}
