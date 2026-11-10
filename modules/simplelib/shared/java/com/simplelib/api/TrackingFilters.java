package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Who may see whom (BACK22 core §3.4): providers can hide an entity from a player although it is within tracking range - Simple
 * Layers hides everything on other levels of its stacked Backrooms dimension (Vanilla tracks by horizontal distance only, so name
 * tags would show through the bedrock between levels). Asked by the mixin on {@code Entity.broadcastToPlayer}; without providers
 * nothing changes. Providers must be cheap (asked per player and tracked entity on every tracking update).
 */
public final class TrackingFilters {
    @FunctionalInterface
    public interface Filter {
        /** false = {@code viewer} must not see {@code entity}. */
        boolean canSee(ServerPlayer viewer, Entity entity);
    }

    private static volatile List<Filter> filters = List.of();

    private TrackingFilters() {}

    public static synchronized void register(Filter filter) {
        List<Filter> next = new ArrayList<>(filters);
        next.add(filter);
        filters = List.copyOf(next);
    }

    public static boolean allows(ServerPlayer viewer, Entity entity) {
        for (Filter f : filters) {
            try {
                if (!f.canSee(viewer, entity)) return false;
            } catch (RuntimeException e) {
                // a broken provider never hides anything
            }
        }
        return true;
    }

    /** Test hook. */
    public static int size() {
        return filters.size();
    }
}
