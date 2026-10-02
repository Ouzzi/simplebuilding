package com.simplebuilding.framework.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional hand hints: a module tells SimpleBuilding's first-person renderer that a right click with
 * the item in one hand would transform the aimed block, so the held item tilts slightly - the same
 * hint SimpleBuilding shows for its own tools. Purely cosmetic: never use it for gameplay.
 *
 * <p><b>Server-safe.</b> This class touches no game or client class. Register from common
 * initialisation (dedicated servers just never ask); the predicates run only on the client, once per
 * tick and aimed block, from the render thread. They must be read-only and cheap.
 *
 * <p><b>Loader- and version-neutral.</b> The framework has no Minecraft dependency, so a
 * {@link Query} carries the game objects as {@code Object}: on Minecraft 26.3 {@code level} is a
 * {@code net.minecraft.world.level.Level} (client level), {@code player} a
 * {@code net.minecraft.world.entity.player.Player}, {@code hit} a
 * {@code net.minecraft.world.phys.BlockHitResult}. A hint that cannot use them returns false.
 * The predicate only answers "would this hand's click transform the block"; SimpleBuilding still
 * applies its own checks (spectator, cooldown, the vanilla interaction order between the hands).
 */
public final class TransformHints {
    /** One question: the hand that would click ({@code mainHand} false = off hand) and what it aims at. */
    public record Query(Object level, Object player, Object hit, boolean mainHand) {}

    @FunctionalInterface
    public interface Hint {
        /** True if a right click with the item in this hand would transform the aimed block now. */
        boolean wouldTransform(Query query);
    }

    private static final Map<String, Hint> HINTS = new ConcurrentHashMap<>();

    private TransformHints() {}

    /** Adds or replaces the hint of {@code id} (usually the mod id; one hint per id). */
    public static void register(String id, Hint hint) {
        HINTS.put(Objects.requireNonNull(id), Objects.requireNonNull(hint));
    }

    public static void unregister(String id) {
        HINTS.remove(id);
    }

    /** The registered ids, for tests and diagnostics. */
    public static List<String> ids() {
        return List.copyOf(HINTS.keySet());
    }

    /**
     * Whether any registered hint answers yes. A hint that throws counts as "no" for that query: a
     * broken cosmetic hint of one module must not take the renderer down.
     */
    public static boolean any(Query query) {
        for (Hint hint : HINTS.values()) {
            try {
                if (hint.wouldTransform(query)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // cosmetic only, see above
            }
        }
        return false;
    }
}
