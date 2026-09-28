package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Spieler in einem Bereich, ohne die Entity-Sektionen des Bereichs abzulaufen (docs/PERFORMANCE.md).
 *
 * <p>{@code level.getEntitiesOfClass(Player.class, box, ...)} besucht jede Entity-Sektion (16^3),
 * die der Bereich schneidet - ein Elytra-Pad der Stufe V deckt 128 x 127 x 128 Bloecke ab, also rund
 * 8 x 8 x 8 = 512 Sektionen, als letzte Easter-Stufe doppelt so breit und hoch rund 4096, und das je
 * Pad alle zehn Ticks. Spieler gibt es dagegen nur eine Handvoll je Level: hier wird die
 * Spielerliste des Levels durchgegangen, O(Spieler) statt O(Sektionen).
 *
 * <p>Gleiches Ergebnis wie die Sektionssuche: sie nimmt jede Entity, deren Hitbox den Bereich
 * schneidet ({@code getBoundingBox().intersects(box)}), und jeder Spieler eines Levels steht auch in
 * dessen Spielerliste. Nur die Reihenfolge kann abweichen; die Sektionssuche liefert ebenfalls keine
 * feste Reihenfolge.
 */
public final class PlayerScan {

    private PlayerScan() {
    }

    /** Alle Spieler der Klasse {@code type}, deren Hitbox {@code box} schneidet und die {@code filter} erfuellen. */
    public static <P extends Player> List<P> playersIn(Level level, AABB box, Class<P> type, Predicate<? super P> filter) {
        List<? extends Player> all = level.players();
        if (all.isEmpty()) {
            return List.of();
        }
        List<P> found = null;
        for (int i = 0, n = all.size(); i < n; i++) {
            Player player = all.get(i);
            if (type.isInstance(player) && player.getBoundingBox().intersects(box)) {
                P typed = type.cast(player);
                if (filter.test(typed)) {
                    if (found == null) {
                        found = new ArrayList<>(2);
                    }
                    found.add(typed);
                }
            }
        }
        return found == null ? List.of() : found;
    }

    /** Wie {@link #playersIn(Level, AABB, Class, Predicate)} ohne Zusatzbedingung. */
    public static <P extends Player> List<P> playersIn(Level level, AABB box, Class<P> type) {
        return playersIn(level, box, type, p -> true);
    }

    /** Ob irgendein Spieler (auch Zuschauer) den Bereich schneidet - ohne Liste. */
    public static boolean anyPlayerIn(Level level, AABB box) {
        List<? extends Player> all = level.players();
        for (int i = 0, n = all.size(); i < n; i++) {
            if (all.get(i).getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }
}
