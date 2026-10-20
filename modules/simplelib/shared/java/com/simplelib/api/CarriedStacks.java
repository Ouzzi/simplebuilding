package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Stacks a player carries inside a worn container that tools may draw from (split stage 8c-1,
 * docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md): SimpleBuilding's worn backpack with Master Builder offers its entries, and
 * Simple Storage's quivers take arrows from a quiver in there - neither mod imports the other. A {@link Source} answers
 * with copies plus a write-back; a tool changes the copy and hands it to {@link Entry#write}. Without a source a player
 * carries nothing here.
 */
public final class CarriedStacks {
    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    private CarriedStacks() {}

    /** One stack inside a worn container: a copy, and how a changed copy goes back in. */
    public record Entry(ItemStack stack, Consumer<ItemStack> writeBack) {
        public void write(ItemStack changed) {
            writeBack.accept(changed);
        }
    }

    /** A kind of worn container (one per mod). */
    public interface Source {
        /** The entries {@code player} may draw from right now, in the container's order; never null. */
        List<Entry> drawable(Player player);
    }

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static void unregister(Source source) {
        SOURCES.remove(source);
    }

    /** Every drawable entry of {@code player}, source by source (a failing source counts as none). */
    public static List<Entry> drawable(Player player) {
        if (SOURCES.isEmpty() || player == null) return List.of();
        List<Entry> out = new ArrayList<>();
        for (Source source : SOURCES) {
            try {
                List<Entry> entries = source.drawable(player);
                if (entries != null) out.addAll(entries);
            } catch (RuntimeException ignored) {
                // a broken source must not break the bow
            }
        }
        return out;
    }
}
