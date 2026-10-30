package com.simplelib.api.client.keys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The logic of the key binding page (UI28-1 / WP-25), without Minecraft classes: which key is free, taken or shared, and
 * the list filter. Works on {@link Binding} copies of Minecraft's key mappings (key names like {@code key.keyboard.h}),
 * so it knows nothing of any mod and is tested alone.
 */
public final class KeyConflicts {
    /** Minecraft's name of "no key". */
    public static final String UNBOUND = "key.keyboard.unknown";

    /** One action: {@code id} the mapping's name, {@code action} its shown name, {@code group} its category, {@code owner} its mod. */
    public record Binding(String id, String action, String group, String owner, String key, String defaultKey) {
        public boolean unbound() {
            return key == null || key.isEmpty() || UNBOUND.equals(key);
        }

        public boolean isDefault() {
            return key != null && key.equals(defaultKey);
        }
    }

    /** How the keyboard picture colours a key: grey free, green one action, red several. */
    public enum State { FREE, BOUND, SHARED }

    private KeyConflicts() {}

    /** The bound actions per key name, in the order of {@code all}; unbound actions are left out. */
    public static Map<String, List<Binding>> byKey(Collection<Binding> all) {
        Map<String, List<Binding>> out = new LinkedHashMap<>();
        for (Binding b : all) {
            if (!b.unbound()) out.computeIfAbsent(b.key(), k -> new ArrayList<>()).add(b);
        }
        return out;
    }

    public static State state(Map<String, List<Binding>> byKey, String key) {
        List<Binding> on = byKey.get(key);
        return on == null || on.isEmpty() ? State.FREE : on.size() == 1 ? State.BOUND : State.SHARED;
    }

    /** Number of keys that carry more than one action. */
    public static int sharedKeys(Map<String, List<Binding>> byKey) {
        return (int) byKey.values().stream().filter(l -> l.size() > 1).count();
    }

    /** The other actions on the same key as {@code b}. */
    public static List<Binding> sharedWith(Map<String, List<Binding>> byKey, Binding b) {
        if (b.unbound()) return List.of();
        return byKey.getOrDefault(b.key(), List.of()).stream().filter(o -> !o.id().equals(b.id())).toList();
    }

    public static boolean conflicts(Map<String, List<Binding>> byKey, Binding b) {
        return !sharedWith(byKey, b).isEmpty();
    }

    /**
     * The list the page shows: {@code query} (words, all must occur in the action, group, mod or shown key; empty = all),
     * optionally only actions in conflict or only unbound ones. {@code keyShown} gives the text of a key ("H").
     */
    public static List<Binding> filter(List<Binding> all, String query, boolean onlyConflicts, boolean onlyUnbound,
            Function<Binding, String> keyShown) {
        Map<String, List<Binding>> byKey = byKey(all);
        String[] words = query == null ? new String[0] : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<Binding> out = new ArrayList<>();
        for (Binding b : all) {
            if (onlyConflicts && !conflicts(byKey, b)) continue;
            if (onlyUnbound && !b.unbound()) continue;
            String haystack = (b.action() + " " + b.group() + " " + b.owner() + " " + keyShown.apply(b)).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String w : words) {
                if (!w.isEmpty() && !haystack.contains(w)) {
                    matches = false;
                    break;
                }
            }
            if (matches) out.add(b);
        }
        return out;
    }

    /** {@code all} with the actions of {@code ids} set to their default key (the page's "reset"); others unchanged. */
    public static List<Binding> reset(List<Binding> all, java.util.Set<String> ids) {
        List<Binding> out = new ArrayList<>();
        for (Binding b : all) {
            out.add(ids.contains(b.id()) ? new Binding(b.id(), b.action(), b.group(), b.owner(), b.defaultKey(), b.defaultKey()) : b);
        }
        return out;
    }

    /** {@code all} with {@code id} moved to {@code key} (others on that key keep it: the page only shows the conflict, never resolves it). */
    public static List<Binding> assign(List<Binding> all, String id, String key) {
        List<Binding> out = new ArrayList<>();
        for (Binding b : all) {
            out.add(b.id().equals(id) ? new Binding(b.id(), b.action(), b.group(), b.owner(), key, b.defaultKey()) : b);
        }
        return out;
    }
}
