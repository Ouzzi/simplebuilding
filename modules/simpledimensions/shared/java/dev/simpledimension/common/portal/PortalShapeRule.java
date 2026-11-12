package dev.simpledimension.common.portal;

import java.util.Optional;

/**
 * A frame-based portal form (N33 "shape"): one frame block, the allowed outline and the interior size limits.
 * Built from the portal config: the fallback fields ({@code frameBlock}, {@code portalShape}, {@code min/maxPortal*})
 * or a recipe with {@code shape} rectangle, arch or any. Rectangles keep the proven {@link PortalFrameScanner};
 * arches use the {@link ArchPortalScanner}, which also explains a refusal.
 */
public record PortalShapeRule(String frameBlock, ArchPortalScanner.Mode mode,
                              int minWidth, int maxWidth, int minHeight, int maxHeight) {

    public ArchPortalScanner scanner() {
        return new ArchPortalScanner(mode, minWidth, maxWidth, minHeight, maxHeight);
    }

    /** A portal of this form whose interior contains the cell. */
    public Optional<MatchedPortal> find(PortalWorldView world, int x, int y, int z) {
        if (mode == ArchPortalScanner.Mode.RECTANGLE) {
            return new PortalFrameScanner(minWidth, maxWidth, minHeight, maxHeight).find(framed(world), x, y, z)
                    .map(frame -> MatchedPortal.fromRectangular(frame, frameBlock));
        }
        return scanner().find(frameCells(world), world::isInterior, x, y, z).map(r -> r.toMatched(frameBlock));
    }

    /**
     * Why lighting at the cell does not open this form - only if the cell looks like an attempt at it (frame below
     * and on both sides). Empty when it is no attempt or the shape is fine.
     */
    public Optional<ArchPortalScanner.Verdict> explain(PortalWorldView world, int x, int y, int z) {
        ArchPortalScanner scanner = scanner();
        ArchPortalScanner.Cells frame = frameCells(world);
        for (PortalAxis axis : PortalAxis.values()) {
            if (!scanner.enclosed(frame, world::isInterior, x, y, z, axis)) {
                continue;
            }
            ArchPortalScanner.Verdict verdict = scanner.scan(frame, world::isInterior, x, y, z, axis).verdict();
            return verdict == ArchPortalScanner.Verdict.OK ? Optional.empty() : Optional.of(verdict);
        }
        return Optional.empty();
    }

    private ArchPortalScanner.Cells frameCells(PortalWorldView world) {
        return (x, y, z) -> world.isBlock(x, y, z, frameBlock);
    }

    /** The world seen with this rule's block as "the" frame (the rectangle scanner asks {@code isFrame}). */
    private PortalWorldView framed(PortalWorldView world) {
        return new PortalWorldView() {
            public boolean isFrame(int x, int y, int z) {
                return world.isBlock(x, y, z, frameBlock);
            }

            public boolean isBlock(int x, int y, int z, String blockId) {
                return world.isBlock(x, y, z, blockId);
            }

            public boolean isEmpty(int x, int y, int z) {
                return world.isEmpty(x, y, z);
            }

            public boolean isPortal(int x, int y, int z) {
                return world.isPortal(x, y, z);
            }
        };
    }
}
