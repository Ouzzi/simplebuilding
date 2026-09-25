package com.simplebuilding.blueprint;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Das Bauwerk einer Blaupause: welcher Blockzustand an welcher Stelle des lokalen Rasters steht.
 *
 * <p>Das Raster ist ein Wuerfel mit {@value BlueprintCode#GRID} Feldern Kantenlaenge, jede
 * Koordinate liegt in {@code 0..255}. Eine Stelle wird deshalb als ein {@code int} gespeichert
 * ({@code x | y << 8 | z << 16}), ohne ein {@code BlockPos}-Objekt pro Stelle; wie viele Stellen
 * belegt sein duerfen, begrenzt {@link BlueprintCode#MAX_EXPANDED_CELLS}. Luft steht nie im Modell.
 */
public final class BlueprintModel {
    private static final int BITS = 8;
    private static final int MASK = (1 << BITS) - 1;

    private final Int2ObjectOpenHashMap<BlockState> blocks = new Int2ObjectOpenHashMap<>();
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean boundsDirty = true;

    public static int key(int x, int y, int z) {
        return x | (y << BITS) | (z << (2 * BITS));
    }

    public static int keyX(int key) {
        return key & MASK;
    }

    public static int keyY(int key) {
        return (key >>> BITS) & MASK;
    }

    public static int keyZ(int key) {
        return (key >>> (2 * BITS)) & MASK;
    }

    public static boolean inGrid(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < BlueprintCode.GRID && y < BlueprintCode.GRID && z < BlueprintCode.GRID;
    }

    /** Setzt die Stelle; Luft (oder {@code null}) raeumt sie. */
    public void set(int x, int y, int z, BlockState state) {
        if (state == null || state.isAir()) {
            if (blocks.remove(key(x, y, z)) != null) {
                boundsDirty = true;
            }
        } else {
            blocks.put(key(x, y, z), state);
            boundsDirty = true;
        }
    }

    public BlockState get(int x, int y, int z) {
        if (!inGrid(x, y, z)) {
            return null;
        }
        return blocks.get(key(x, y, z));
    }

    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public Int2ObjectMap<BlockState> blocks() {
        return blocks;
    }

    /** Die Schluessel in fester Reihenfolge: von unten nach oben, dann z, dann x. */
    public int[] sortedKeys() {
        IntArrayList keys = new IntArrayList(blocks.keySet());
        int[] array = keys.toIntArray();
        for (int i = 0; i < array.length; i++) {
            int k = array[i];
            array[i] = keyY(k) << (2 * BITS) | keyZ(k) << BITS | keyX(k);
        }
        java.util.Arrays.sort(array);
        for (int i = 0; i < array.length; i++) {
            int s = array[i];
            array[i] = key(s & MASK, (s >>> (2 * BITS)) & MASK, (s >>> BITS) & MASK);
        }
        return array;
    }

    private void updateBounds() {
        if (!boundsDirty) {
            return;
        }
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (int k : blocks.keySet()) {
            int x = keyX(k), y = keyY(k), z = keyZ(k);
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
        if (blocks.isEmpty()) {
            minX = minY = minZ = 0;
            maxX = maxY = maxZ = -1;
        }
        boundsDirty = false;
    }

    public int minX() { updateBounds(); return minX; }
    public int minY() { updateBounds(); return minY; }
    public int minZ() { updateBounds(); return minZ; }
    public int maxX() { updateBounds(); return maxX; }
    public int maxY() { updateBounds(); return maxY; }
    public int maxZ() { updateBounds(); return maxZ; }

    /** Ausdehnung des Bauwerks (Bounding Box der Bloecke), 0 fuer ein leeres Modell. */
    public int sizeX() { return maxX() - minX() + 1; }
    public int sizeY() { return maxY() - minY() + 1; }
    public int sizeZ() { return maxZ() - minZ() + 1; }

    /** Laengste Kante der Bounding Box: daran misst sich, welcher Baustab das Bauwerk schafft. */
    public int maxEdge() {
        return isEmpty() ? 0 : Math.max(sizeX(), Math.max(sizeY(), sizeZ()));
    }

    /** Dasselbe Bauwerk, so verschoben, dass seine Bounding Box bei 0,0,0 beginnt. */
    public BlueprintModel normalized() {
        int dx = minX(), dy = minY(), dz = minZ();
        if (dx == 0 && dy == 0 && dz == 0) {
            return this;
        }
        BlueprintModel shifted = new BlueprintModel();
        for (Int2ObjectMap.Entry<BlockState> e : blocks.int2ObjectEntrySet()) {
            int k = e.getIntKey();
            shifted.set(keyX(k) - dx, keyY(k) - dy, keyZ(k) - dz, e.getValue());
        }
        return shifted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlueprintModel other)) return false;
        if (other.blocks.size() != blocks.size()) return false;
        for (Int2ObjectMap.Entry<BlockState> e : blocks.int2ObjectEntrySet()) {
            if (!Objects.equals(other.blocks.get(e.getIntKey()), e.getValue())) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return blocks.hashCode();
    }

    @Override
    public String toString() {
        return "BlueprintModel[" + blocks.size() + " blocks, " + sizeX() + "x" + sizeY() + "x" + sizeZ() + "]";
    }

    /** Hilfe fuer Tests: alle Stellen als lesbare Map. */
    public Map<String, BlockState> describe() {
        Map<String, BlockState> out = new java.util.TreeMap<>();
        for (Int2ObjectMap.Entry<BlockState> e : blocks.int2ObjectEntrySet()) {
            int k = e.getIntKey();
            out.put(keyX(k) + "," + keyY(k) + "," + keyZ(k), e.getValue());
        }
        return out;
    }

    /**
     * Schluessel fuer Zwischenspeicher, der das Modell nach Identitaet vergleicht:
     * {@link #hashCode()} laeuft ueber alle Stellen und waere bei Millionen Bloecken je Abfrage zu teuer.
     * {@code BlueprintCode.parseCached} liefert fuer denselben Code dasselbe Modell-Objekt.
     */
    public record Identity(BlueprintModel model) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Identity other && other.model == model;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(model);
        }
    }
}
