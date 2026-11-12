package dev.simpledimension.common.portal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Detects a portal of free arch shape around a position (N33, owner 12.11.2026): a 4-connected interior in one
 * vertical plane, filled from the lit cell, that follows the owner's rules.
 * <ol>
 *   <li>dense: every side neighbour of an interior cell is interior or frame (else {@link Verdict#GAP});</li>
 *   <li>bounded: the outline fits {@code maxWidth × maxHeight} (else {@link Verdict#OPEN}: open or too big);</li>
 *   <li>straight bottom, sides only up or inward: every interior cell above the bottom row stands on an interior
 *       cell ({@link Verdict#BOTTOM} for a bump out of the floor, {@link Verdict#SIDES} for a shelf or overhang);</li>
 *   <li>free top edge (round, pointed, stepped or double arch);</li>
 *   <li>frame 8-connected: the frame is exactly the side neighbourhood of the interior, which is always
 *       8-connected for such a hole-free shape - corner blocks (diagonal steps) are never required.</li>
 * </ol>
 * A plain rectangle satisfies the same rules; {@link Mode} decides whether rectangles, arches or both are allowed.
 * Sizes: width = bottom row, height = tallest column.
 */
public final class ArchPortalScanner {

    /** A cell test in world coordinates. */
    @FunctionalInterface
    public interface Cells {
        boolean test(int x, int y, int z);
    }

    /** Which outlines a portal may have ({@code shape} in the portal config). */
    public enum Mode {
        RECTANGLE, ARCH, ANY;

        /** "rectangle", "arch" or "any" (case-insensitive); null or blank gives the fallback, anything else null. */
        public static Mode parse(String value, Mode fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
            return null;
        }
    }

    /** Why a shape does (not) qualify; {@link #key()} names the action-bar text. */
    public enum Verdict {
        OK, OPEN, GAP, BOTTOM, SIDES, TOO_SMALL, RECTANGLE_ONLY, ARCH_ONLY;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Scan result; interior/frame are only filled for {@link Verdict#OK}. */
    public record Result(Verdict verdict, PortalAxis axis, List<BlockPos3i> interior, List<BlockPos3i> frame,
                         int width, int height, boolean rectangle) {
        public boolean ok() {
            return verdict == Verdict.OK;
        }

        public MatchedPortal toMatched(String frameBlockId) {
            List<MatchedPortal.FrameBlock> ring = new ArrayList<>(frame.size());
            for (BlockPos3i pos : frame) {
                ring.add(new MatchedPortal.FrameBlock(pos, frameBlockId));
            }
            return new MatchedPortal(axis, interior, ring);
        }

        static Result fail(Verdict verdict, PortalAxis axis) {
            return new Result(verdict, axis, List.of(), List.of(), 0, 0, false);
        }
    }

    private final Mode mode;
    private final int minWidth;
    private final int maxWidth;
    private final int minHeight;
    private final int maxHeight;

    public ArchPortalScanner(Mode mode, int minWidth, int maxWidth, int minHeight, int maxHeight) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must be set");
        }
        if (minWidth < 1 || minHeight < 1) {
            throw new IllegalArgumentException("minimum interior size must be >= 1");
        }
        if (maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("maximum interior size must be >= minimum");
        }
        this.mode = mode;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    public Mode mode() {
        return mode;
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

    /** The first axis (X, then Z) on which a valid portal contains the cell. */
    public Optional<Result> find(Cells frame, Cells interior, int x, int y, int z) {
        for (PortalAxis axis : PortalAxis.values()) {
            Result result = scan(frame, interior, x, y, z, axis);
            if (result.ok()) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    /** Fill the interior from the cell on one axis and judge it by the rules above. */
    public Result scan(Cells frame, Cells interior, int x, int y, int z, PortalAxis axis) {
        if (!interior.test(x, y, z)) {
            return Result.fail(Verdict.OPEN, axis);
        }
        boolean alongX = axis == PortalAxis.X;
        int startAlong = alongX ? x : z;
        int fixed = alongX ? z : x;
        // Window around the start: any cell farther than the maximum size already breaks the bounds.
        int spanA = 2 * maxWidth + 1;
        int spanY = 2 * maxHeight + 1;
        int originA = startAlong - maxWidth;
        int originY = y - maxHeight;
        // 0 = unknown, 1 = interior, 2 = frame
        byte[] seen = new byte[spanA * spanY];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        List<int[]> cells = new ArrayList<>();
        Set<BlockPos3i> ring = new LinkedHashSet<>();
        seen[(startAlong - originA) * spanY + (y - originY)] = 1;
        queue.add(new int[]{startAlong, y});
        int minA = startAlong, maxA = startAlong, minY = y, maxY = y;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            cells.add(cell);
            for (int[] step : steps) {
                int na = cell[0] + step[0];
                int ny = cell[1] + step[1];
                int ia = na - originA;
                int iy = ny - originY;
                if (ia < 0 || ia >= spanA || iy < 0 || iy >= spanY) {
                    return Result.fail(Verdict.OPEN, axis);
                }
                int index = ia * spanY + iy;
                if (seen[index] != 0) {
                    continue;
                }
                int wx = alongX ? na : fixed;
                int wz = alongX ? fixed : na;
                if (interior.test(wx, ny, wz)) {
                    seen[index] = 1;
                    minA = Math.min(minA, na);
                    maxA = Math.max(maxA, na);
                    minY = Math.min(minY, ny);
                    maxY = Math.max(maxY, ny);
                    if (maxA - minA + 1 > maxWidth || maxY - minY + 1 > maxHeight) {
                        return Result.fail(Verdict.OPEN, axis);
                    }
                    queue.add(new int[]{na, ny});
                } else if (frame.test(wx, ny, wz)) {
                    seen[index] = 2;
                    ring.add(new BlockPos3i(wx, ny, wz));
                } else {
                    return Result.fail(Verdict.GAP, axis);
                }
            }
        }
        // Rule 3: every cell above the bottom row stands on an interior cell. A frame block rising from the floor
        // between bottom-row cells is a bump in the bottom edge; anything else is a side that widens or overhangs.
        int bottomMin = Integer.MAX_VALUE, bottomMax = Integer.MIN_VALUE;
        for (int[] cell : cells) {
            if (cell[1] == minY) {
                bottomMin = Math.min(bottomMin, cell[0]);
                bottomMax = Math.max(bottomMax, cell[0]);
            }
        }
        for (int[] cell : cells) {
            if (cell[1] == minY) {
                continue;
            }
            if (seen[(cell[0] - originA) * spanY + (cell[1] - 1 - originY)] != 1) {
                boolean bump = cell[1] - 1 == minY && cell[0] > bottomMin && cell[0] < bottomMax;
                return Result.fail(bump ? Verdict.BOTTOM : Verdict.SIDES, axis);
            }
        }
        int width = maxA - minA + 1;
        int height = maxY - minY + 1;
        boolean rectangle = cells.size() == width * height;
        if (width < minWidth || height < minHeight) {
            return Result.fail(Verdict.TOO_SMALL, axis);
        }
        if (mode == Mode.RECTANGLE && !rectangle) {
            return Result.fail(Verdict.RECTANGLE_ONLY, axis);
        }
        if (mode == Mode.ARCH && rectangle) {
            return Result.fail(Verdict.ARCH_ONLY, axis);
        }
        List<BlockPos3i> interiorCells = new ArrayList<>(cells.size());
        for (int[] cell : cells) {
            interiorCells.add(new BlockPos3i(alongX ? cell[0] : fixed, cell[1], alongX ? fixed : cell[0]));
        }
        return new Result(Verdict.OK, axis, interiorCells, List.copyOf(ring), width, height, rectangle);
    }

    /**
     * True if the cell looks like an attempt at a portal on this axis: walking down and to both sides over interior
     * cells reaches the frame within the maximum size. Used to decide whether a refusal deserves an explanation
     * (a fire on an open obsidian floor stays silent).
     */
    public boolean enclosed(Cells frame, Cells interior, int x, int y, int z, PortalAxis axis) {
        if (!interior.test(x, y, z)) {
            return false;
        }
        int ax = axis == PortalAxis.X ? 1 : 0;
        int az = axis == PortalAxis.Z ? 1 : 0;
        return reaches(frame, interior, x, y, z, 0, -1, 0, maxHeight)
                && reaches(frame, interior, x, y, z, ax, 0, az, maxWidth)
                && reaches(frame, interior, x, y, z, -ax, 0, -az, maxWidth);
    }

    private static boolean reaches(Cells frame, Cells interior, int x, int y, int z, int dx, int dy, int dz, int limit) {
        for (int i = 1; i <= limit + 1; i++) {
            int cx = x + dx * i, cy = y + dy * i, cz = z + dz * i;
            if (frame.test(cx, cy, cz)) {
                return true;
            }
            if (!interior.test(cx, cy, cz)) {
                return false;
            }
        }
        return false;
    }
}
