package com.simplebuilding.modules.simpletweaks.play;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * "autoOffWith": a feature switches itself off when one of its partner mods (ledger, chunky, ...) is loaded. The decision is a
 * pure function of the configured lists and the set of loaded mod ids; the loader only supplies {@link Mods#check}.
 * An explicit {@code enabled: true} cannot be told from the default in the file, so the partner always wins; the log line
 * tells the admin to empty the list to keep the feature on.
 */
public final class AutoOff {
    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_.-]{0,63}");

    /** Feature to the first loaded partner id of its list (lists in config order). */
    public static Map<String, String> match(Map<String, List<String>> lists, Predicate<String> loaded) {
        var out = new LinkedHashMap<String, String>();
        if (lists != null) lists.forEach((feature, ids) -> ids.stream().filter(loaded).findFirst().ifPresent(id -> out.put(feature, id)));
        return out;
    }
    /** Valid mod ids only, bounded sizes, immutable. */
    public static Map<String, List<String>> clean(Map<String, List<String>> lists) {
        var out = new LinkedHashMap<String, List<String>>();
        if (lists != null) lists.forEach((k, v) -> {
            if (k != null && out.size() < 32) out.put(k, v == null ? List.of() : v.stream().filter(s -> s != null && MOD_ID.matcher(s).matches()).distinct().limit(16).toList());
        });
        return Collections.unmodifiableMap(out);
    }
    /** One INFO line per feature. {@code hintOnly}: features that are only reported, never switched off. */
    public static void log(org.slf4j.Logger log, String file, Map<String, String> hits, Set<String> hintOnly) {
        hits.forEach((feature, mod) -> {
            if (hintOnly.contains(feature)) log.info("Simple Tweaks: mod '{}' is loaded; note for '{}' (nothing is switched off, rule stays as configured in {}).", mod, feature, file);
            else log.info("Simple Tweaks: feature '{}' is switched off because mod '{}' is loaded (autoOffWith). To keep it on, empty autoOffWith.{} in {}.", feature, mod, feature, file);
        });
    }
    private AutoOff() {}
}
