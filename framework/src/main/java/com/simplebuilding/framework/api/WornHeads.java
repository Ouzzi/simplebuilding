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
    private static final java.util.Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Map<String, SkullLook> LOOKS = new ConcurrentHashMap<>();

    /**
     * How a mod's skull types are drawn when worn outside the helmet slot (client side; Minecraft types travel as
     * {@code Object}, the asker and the answerer know them): the type a head stack shows (variants), the size factor
     * against Vanilla's worn skull (1.0 = as Vanilla draws it) and the resulting edge in model pixels (0 = unknown).
     */
    public interface SkullLook {
        /** The skull type {@code stack} shows ({@code type} = the block's own type); {@code type} itself for no change. */
        default Object type(Object stack, Object type) {
            return type;
        }

        /** Size factor of a worn head of {@code type} against Vanilla's worn skull (1.0 for none). */
        default float factor(Object type) {
            return 1.0F;
        }

        /** Longest edge of the worn head in model pixels after {@link #factor}, 0 when this look does not know the type. */
        default float edge(Object type) {
            return 0.0F;
        }
    }

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
        for (Map.Entry<String, Function<Object, List<?>>> entry : PROVIDERS.entrySet()) {
            Function<Object, List<?>> provider = entry.getValue();
            try {
                List<?> heads = provider.apply(entity);
                if (heads != null) {
                    for (Object head : heads) {
                        if (head != null) {
                            out.add(head);
                        }
                    }
                }
            } catch (RuntimeException e) {
                // a broken provider must not break the wearer; say so once per provider
                if (FAILED.add(entry.getKey())) {
                    System.getLogger("WornHeads").log(System.Logger.Level.WARNING, "Extra-head provider '" + entry.getKey() + "' failed (further failures are not logged)", e);
                }
            }
        }
        return out;
    }

    /** Adds (or replaces) the skull look of {@code owner}; see {@link SkullLook}. */
    public static void registerLook(String owner, SkullLook look) {
        LOOKS.put(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(look, "look"));
    }

    public static boolean unregisterLook(String owner) {
        return LOOKS.remove(owner) != null;
    }

    /** The skull type a worn head stack shows: every look may swap it (a failing look changes nothing). */
    public static Object skullType(Object stack, Object type) {
        Object out = type;
        for (SkullLook look : LOOKS.values()) {
            try {
                Object changed = look.type(stack, out);
                if (changed != null) {
                    out = changed;
                }
            } catch (RuntimeException ignored) {
                // a broken look must not break the render
            }
        }
        return out;
    }

    /** Size factor of a worn skull of {@code type} (product of all looks, 1.0 without one). */
    public static float skullFactor(Object type) {
        float out = 1.0F;
        for (SkullLook look : LOOKS.values()) {
            try {
                out *= look.factor(type);
            } catch (RuntimeException ignored) {
                // see skullType
            }
        }
        return out;
    }

    /** Longest edge in model pixels of the worn skull of {@code type}; 0 when no look knows it (Vanilla skull: 9.5). */
    public static float skullEdge(Object type) {
        for (SkullLook look : LOOKS.values()) {
            try {
                float edge = look.edge(type);
                if (edge > 0.0F) {
                    return edge;
                }
            } catch (RuntimeException ignored) {
                // see skullType
            }
        }
        return 0.0F;
    }
}
