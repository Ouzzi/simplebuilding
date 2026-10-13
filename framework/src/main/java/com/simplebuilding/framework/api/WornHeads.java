package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Extra head slots (KONZEPT-SIMPLE-MOBHEADS §2): a mod that gives entities a second place for a worn head (SimpleBuilding's
 * extra head slot) registers a provider here; a mod whose heads do something while worn (Simple Mobheads) asks
 * {@link #extra} next to the helmet slot. Pure Java, no Minecraft class: the provider gets the entity and returns the
 * item stacks it wears there (empty stacks and non-stacks are the asker's to skip). Since 0.1.11.
 *
 * <p>Nobody imports the other side: SimpleBuilding names no head, Mobheads names no slot. Without a provider every
 * entity wears its head only in the helmet slot.
 */
public final class WornHeads {
    private static final Map<String, Function<Object, List<?>>> PROVIDERS = new ConcurrentHashMap<>();

    private WornHeads() {
    }

    /** Adds (or replaces) the provider {@code owner}; it returns the extra worn heads of an entity (never null). */
    public static void register(String owner, Function<Object, List<?>> provider) {
        PROVIDERS.put(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(provider, "provider"));
    }

    /** Takes the provider of {@code owner} out again; true when it was there. */
    public static boolean unregister(String owner) {
        return PROVIDERS.remove(owner) != null;
    }

    /** Every extra head {@code entity} wears outside its helmet slot, from all providers (a failing provider counts as none). */
    public static List<Object> extra(Object entity) {
        if (PROVIDERS.isEmpty()) {
            return List.of();
        }
        List<Object> out = new ArrayList<>();
        for (Function<Object, List<?>> provider : PROVIDERS.values()) {
            try {
                List<?> heads = provider.apply(entity);
                if (heads != null) {
                    for (Object head : heads) {
                        if (head != null) {
                            out.add(head);
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // a broken provider must not break the wearer
            }
        }
        return out;
    }
}
