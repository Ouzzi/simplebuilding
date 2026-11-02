package com.simplelib.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

/**
 * Recipes of a switched-off feature disappear with it, for every namespace (mixin {@code RecipeMapFilterMixin}, applied
 * whenever the server builds its recipe table: world start and {@code /reload}). A module registers one rule per
 * namespace once at startup; the rule is asked for every recipe path of that namespace and reads the module's config
 * live, so a config reload plus {@code /reload} brings the recipes back. The core keeps its own filter for
 * {@code simplebuilding:}.
 *
 * <pre>{@code RecipeFilters.register("simplemachines", p -> !cfg.doublePiston && p.equals("double_piston"));}</pre>
 */
public final class RecipeFilters {
    private record Rule(String namespace, Predicate<String> removesPath) {}

    private static final List<Rule> RULES = new CopyOnWriteArrayList<>();

    private RecipeFilters() {}

    /** Removes every recipe of {@code namespace} whose path {@code removesPath} accepts. */
    public static void register(String namespace, Predicate<String> removesPath) {
        RULES.add(new Rule(namespace, removesPath));
    }

    /** Whether any rule is registered (the mixin skips the table otherwise). */
    public static boolean any() {
        return !RULES.isEmpty();
    }

    /** Whether a registered rule removes this recipe. */
    public static boolean removes(Identifier recipe) {
        for (Rule r : RULES) {
            if (r.namespace.equals(recipe.getNamespace()) && r.removesPath.test(recipe.getPath())) return true;
        }
        return false;
    }

    /** Test helper: drops all rules. */
    public static void clear() {
        RULES.clear();
    }
}
