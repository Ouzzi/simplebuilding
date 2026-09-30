package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.List;

public record PortalBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public PortalBounds {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("min values must be <= max values");
        }
    }

    public PortalBounds expand(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be >= 0");
        }
        return new PortalBounds(
                minX - radius,
                minY - radius,
                minZ - radius,
                maxX + radius,
                maxY + radius,
                maxZ + radius
        );
    }

    public List<BlockPos3i> allPositions() {
        List<BlockPos3i> out = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    out.add(new BlockPos3i(x, y, z));
                }
            }
        }
        return out;
    }
}
