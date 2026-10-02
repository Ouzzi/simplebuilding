package com.simplebuilding.guide;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

/**
 * Registry of guide tabs and how each one opens (plan P5/P6, step 2): a book is a list of tabs, and a
 * tab says only how it unlocks. Nothing here knows SimpleBuilding's books; SimpleBuilding registers
 * its two shelves through {@link #register} like any other book would ({@code GuideBooks}).
 *
 * <ul>
 *   <li>{@link Access#ALWAYS}: open for everyone (the shelf hub, the book's own first tab).</li>
 *   <li>{@link Access#RECIPES}: open as soon as one of its {@link Tab#gates} recipes is in the player's
 *       recipe book. Once open it stays open for that player ({@link GuideUnlocks}), even if the recipe
 *       is taken away again. Nothing is consumed and nothing has to be clicked.</li>
 *   <li>{@link Access#OPERATOR}: open while the player is an operator (permission level 2), checked live.</li>
 * </ul>
 */
public final class GuideTabs {

    public enum Access { ALWAYS, RECIPES, OPERATOR }

    /**
     * One tab. {@code gates} are recipe ids, {@code hint} the item a locked tab names in its tooltip
     * (the result of the first gate), {@code advancement} an optional advancement awarded when the tab
     * opens (quests and the advancement screen read it); null for none.
     */
    public record Tab(Identifier id, Access access, List<Identifier> gates, Supplier<? extends ItemLike> hint, Identifier advancement) {
        public Tab {
            gates = List.copyOf(gates);
            if (access == Access.RECIPES && gates.isEmpty()) throw new IllegalArgumentException(id + " opens by recipes but names none");
        }

        public List<ResourceKey<Recipe<?>>> gateKeys() {
            List<ResourceKey<Recipe<?>>> keys = new ArrayList<>(gates.size());
            for (Identifier gate : gates) keys.add(ResourceKey.create(Registries.RECIPE, gate));
            return keys;
        }
    }

    private static final Map<Identifier, Tab> TABS = new LinkedHashMap<>();

    private GuideTabs() {
    }

    /** Registers a tab; ids are unique. */
    public static synchronized void register(Tab tab) {
        if (TABS.putIfAbsent(tab.id(), tab) != null) throw new IllegalStateException("guide tab registered twice: " + tab.id());
    }

    /** All tabs in registration order. */
    public static synchronized List<Tab> all() {
        return List.copyOf(TABS.values());
    }

    /** The tab with this id, or null. */
    public static synchronized Tab get(Identifier id) {
        return TABS.get(id);
    }
}
