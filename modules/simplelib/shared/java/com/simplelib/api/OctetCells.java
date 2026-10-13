package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Which full block falls apart into which octet cell (plan PLAN-MODULAUFTEILUNG §1.4, cycle hammer &lt;-&gt; octets):
 * the octet mod registers its cells, a carving tool asks {@link #cellFor}. Without a registered cell the tool does not
 * carve that material. SimpleLib registers nothing itself.
 */
public final class OctetCells {
    private record Cell(Block cell, Supplier<Item> piece) {}

    private static final Map<Block, Cell> BY_BASE = new ConcurrentHashMap<>();
    private static final Map<Block, Block> BASE_OF = new ConcurrentHashMap<>();

    /**
     * {@code base} (a full block) splits into {@code cell}; one removed octet of it is the item {@code piece} (read when
     * asked, so it may be registered later). A second registration for the same base replaces the first.
     */
    public static void register(Block base, Block cell, Supplier<Item> piece) {
        Cell old = BY_BASE.put(base, new Cell(cell, piece));
        if (old != null && old.cell() != cell) BASE_OF.remove(old.cell(), base);
        BASE_OF.put(cell, base);
    }

    /** Takes {@code base} out again (tests, a mod switching a material off). */
    public static void unregister(Block base) {
        Cell old = BY_BASE.remove(base);
        if (old != null) BASE_OF.remove(old.cell(), base);
    }

    /** The octet cell of the material whose full block is {@code base}, or null without octets. */
    public static @Nullable Block cellFor(@Nullable Block base) {
        if (base == null) return null;
        Cell cell = BY_BASE.get(base);
        return cell == null ? null : cell.cell();
    }

    /** The item of one octet of {@code cell}, or null when {@code cell} is no registered octet cell. */
    public static @Nullable Item pieceOf(@Nullable Block cell) {
        Block base = cell == null ? null : BASE_OF.get(cell);
        Cell entry = base == null ? null : BY_BASE.get(base);
        return entry == null ? null : entry.piece().get();
    }

    /** The full block whose octets {@code cell} holds, or null. */
    public static @Nullable Block baseOf(@Nullable Block cell) {
        return cell == null ? null : BASE_OF.get(cell);
    }

    /** How many materials have octets (tests, info). */
    public static int size() {
        return BY_BASE.size();
    }

    private OctetCells() {}
}
