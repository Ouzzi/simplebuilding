package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Height handover between modules (IG31-56, docs/ai/PLAN-A-LAYERS10-2026-11-08.md): a module that takes over a player above a
 * height of a dimension (Simple Earth: the orbit view) announces that height here, and a module that would otherwise catch the
 * player on the way up (Simple Layers: the Cloud Sea above y 700) leaves everything from that height on to it and hands its own
 * high flyers over there. Neither module imports the other; without a provider nothing takes over.
 */
public final class HeightHandover {
    /** A provider's answer for "I do not take this player over in this dimension". */
    public static final int NONE = Integer.MIN_VALUE;

    /** The lowest height from which the provider takes {@code player} over in {@code dimension}, or {@link #NONE}. */
    @FunctionalInterface
    public interface Provider {
        int takeoverY(ServerPlayer player, ResourceKey<Level> dimension);
    }

    private static final List<Provider> PROVIDERS = new ArrayList<>();

    private HeightHandover() {}

    public static synchronized void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /** The lowest takeover height any provider announces for {@code player} in {@code dimension}. */
    public static synchronized OptionalInt takeoverY(ServerPlayer player, ResourceKey<Level> dimension) {
        int best = NONE;
        for (Provider provider : PROVIDERS) {
            int y;
            try {
                y = provider.takeoverY(player, dimension);
            } catch (RuntimeException e) {
                continue;
            }
            if (y != NONE && (best == NONE || y < best)) best = y;
        }
        return best == NONE ? OptionalInt.empty() : OptionalInt.of(best);
    }

    /** Test hook: the number of registered providers. */
    public static synchronized int providers() {
        return PROVIDERS.size();
    }
}
