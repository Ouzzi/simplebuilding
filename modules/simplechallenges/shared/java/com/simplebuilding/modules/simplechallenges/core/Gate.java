package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The safety lock: every hook asks the gate first. It is open only when the mod is switched on, a challenge is chosen in the
 * world state and that challenge covers the level's dimension. Everything else returns null/false and the hook returns at once.
 */
public final class Gate {
    private static final ConcurrentHashMap<ResourceKey<Level>, String> NAMES = new ConcurrentHashMap<>();

    /** Whether a player counts as creative for the Lucky and Skyline hooks. Test seam: GameTest mock players always report creative. */
    public static volatile java.util.function.Predicate<net.minecraft.world.entity.player.Player> creativeCheck = net.minecraft.world.entity.player.Player::isCreative;

    private Gate() {}

    /** The dimension id as text, cached (the hooks run for every block change and every move). */
    public static String dim(Level level) {
        return NAMES.computeIfAbsent(level.dimension(), k -> k.identifier().toString());
    }

    public static View view(Level level) {
        View v = ChallengeRuntime.view(level);
        return v != null && SimpleChallenges.on() ? v : null;
    }

    /** The unlocked cells of this level's dimension, or null when no challenge covers it. */
    public static UnlockGrid grid(Level level) {
        View v = view(level);
        return v == null ? null : v.grid(dim(level));
    }

    public static boolean active(Level level) {
        return grid(level) != null;
    }
}
