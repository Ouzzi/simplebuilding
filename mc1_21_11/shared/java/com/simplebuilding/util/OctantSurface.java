package com.simplebuilding.util;

import java.util.Arrays;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Die sichtbare Huelle einer Oktant-Figur: welche Blockseiten aussen liegen und welche ihrer vier
 * Kanten eine Linie bekommen. Loader- und seitenneutral (keine Client-Klassen), damit die Spieltests
 * sie pruefen koennen; gezeichnet wird sie im {@code BlockHighlightRenderer}.
 *
 * <p>Bis 2026-09-28 lief diese Suche in jedem Bild neu: jede Stelle der Figur (bei einer Kugel mit
 * 32 Bloecken Durchmesser 32768) plus je Aussenseite sechs Nachbarn und acht Kantenproben, jede Probe
 * ein {@code Math.pow}-Praedikat - rund 10^5 Praedikataufrufe je Bild. Die Huelle haengt nur an
 * Form, Ausrichtung und den beiden Ecken; der Renderer rechnet sie jetzt nur neu, wenn sich davon
 * etwas aendert (docs/PERFORMANCE.md).
 *
 * <p>Reihenfolge und Inhalt sind genau die der alten Schleife (x, dann y, dann z; Seiten und Kanten
 * in {@link Direction#values()}-Reihenfolge), die Vertices bleiben also dieselben.
 */
public final class OctantSurface {
    private static final Direction[] DIRECTIONS = Direction.values();
    /** Eintraege je Aussenseite: x, y, z, Seite (ordinal), Kantenmaske (Bit i = DIRECTIONS[i]). */
    public static final int STRIDE = 5;

    private final int[] data;
    private final int faces;
    private final long predicateCalls;

    private OctantSurface(int[] data, int faces, long predicateCalls) {
        this.data = data;
        this.faces = faces;
        this.predicateCalls = predicateCalls;
    }

    /** Zahl der Aussenseiten. */
    public int faces() {
        return faces;
    }

    /** Wie oft das Praedikat fuer diese Huelle gefragt wurde (die alte Schleife fragte so oft je Bild). */
    public long predicateCalls() {
        return predicateCalls;
    }

    public int x(int face) {
        return data[face * STRIDE];
    }

    public int y(int face) {
        return data[face * STRIDE + 1];
    }

    public int z(int face) {
        return data[face * STRIDE + 2];
    }

    public Direction side(int face) {
        return DIRECTIONS[data[face * STRIDE + 3]];
    }

    /** Ob die Kante der Seite {@code face} in Richtung {@code edge} eine Linie bekommt. */
    public boolean hasEdge(int face, Direction edge) {
        return (data[face * STRIDE + 4] & (1 << edge.ordinal())) != 0;
    }

    /** Zahl der Kantenlinien insgesamt. */
    public int edges() {
        int total = 0;
        for (int face = 0; face < faces; face++) {
            total += Integer.bitCount(data[face * STRIDE + 4]);
        }
        return total;
    }

    /** Sucht die Huelle der Figur {@code inShape} innerhalb von {@code bounds}. */
    public static OctantSurface compute(AABB bounds, Predicate<BlockPos> inShape) {
        int minX = (int) bounds.minX; int minY = (int) bounds.minY; int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX; int maxY = (int) bounds.maxY; int maxZ = (int) bounds.maxZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos sidePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos diagPos = new BlockPos.MutableBlockPos();
        int[] data = new int[STRIDE * 64];
        int faces = 0;
        long calls = 0;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    calls++;
                    if (!inShape.test(pos)) {
                        continue;
                    }
                    for (Direction dir : DIRECTIONS) {
                        neighborPos.set(pos).move(dir);
                        calls++;
                        if (inShape.test(neighborPos)) {
                            continue;
                        }
                        int mask = 0;
                        for (Direction edgeDir : DIRECTIONS) {
                            if (edgeDir == dir || edgeDir == dir.getOpposite()) continue;
                            sidePos.set(pos).move(edgeDir);
                            diagPos.set(neighborPos).move(edgeDir);
                            calls += 2;
                            boolean sideIsShape = inShape.test(sidePos);
                            boolean diagIsShape = inShape.test(diagPos);
                            if (!sideIsShape || diagIsShape) {
                                mask |= 1 << edgeDir.ordinal();
                            }
                        }
                        if ((faces + 1) * STRIDE > data.length) {
                            data = Arrays.copyOf(data, data.length * 2);
                        }
                        int base = faces * STRIDE;
                        data[base] = x;
                        data[base + 1] = y;
                        data[base + 2] = z;
                        data[base + 3] = dir.ordinal();
                        data[base + 4] = mask;
                        faces++;
                    }
                }
            }
        }
        return new OctantSurface(Arrays.copyOf(data, faces * STRIDE), faces, calls);
    }
}
