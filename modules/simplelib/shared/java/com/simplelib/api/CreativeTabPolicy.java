package com.simplelib.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simplelib.mixin.CreativeModeTabPosition;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The creative tab rules of all Simple mods (owner 30.10., IG31-3, docs/ai/PLAN-A-IG31A-2026-10-30.md), applied once after
 * Vanilla built every tab ({@code CreativeModeTabs#buildAllTabContents}, see {@code CreativeModeTabsPolicyMixin}):
 * <ol>
 *   <li>no mod item stands in a Vanilla tab - unless no mod tab offers it (a module without a tab of its own keeps its guide book there);
 *   <li>no plain Vanilla item (no component changes) stands in a mod tab - a Vanilla item with components, such as an enchanted book of a
 *       mod enchantment, is mod content and stays;
 *   <li>mod tabs follow the mod tree: a super-mod, then its sub-mods ({@code creative_tab_order.json}, generated from
 *       {@code modules/modules.json} by {@code tools/modtree/tab_order.py}); inside one mod the registration order. Fabric places the mod tabs
 *       alphabetically by id, so the page, row and column of each mod tab are set again (Fabric only, the Vanilla pages stay).
 * </ol>
 * The pure parts ({@link #isModItem}, {@link #isPlainVanilla}, {@link #rank}) are static so tests call them without a world.
 */
public final class CreativeTabPolicy {
    private static final String ORDER_FILE = "/data/simplelib/creative_tab_order.json";
    private static final int TABS_PER_PAGE = 10;
    private static volatile List<String> order;

    private CreativeTabPolicy() {}

    public static boolean isVanilla(Identifier id) {
        return "minecraft".equals(id.getNamespace());
    }

    /** Whether the stack is an item of a mod (not of Vanilla, whatever its components). */
    public static boolean isModItem(ItemStack stack) {
        return !isVanilla(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Whether the stack is a Vanilla item without component changes (no mod content). */
    public static boolean isPlainVanilla(ItemStack stack) {
        return !isModItem(stack) && stack.getComponentsPatch().isEmpty();
    }

    /** Key of a stack for set comparisons: item id and component changes. */
    public static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + stack.getComponentsPatch().toString();
    }

    /** Mod tree order of the mod namespaces (super-mod, its sub-mods, next super-mod ...). */
    public static List<String> order() {
        List<String> result = order;
        if (result == null) {
            result = new ArrayList<>();
            try (Reader reader = new InputStreamReader(CreativeTabPolicy.class.getResourceAsStream(ORDER_FILE), StandardCharsets.UTF_8)) {
                JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("order");
                for (JsonElement element : array) result.add(element.getAsString());
            } catch (IOException | RuntimeException e) {
                result.clear(); // unreadable list: keep Fabric's own order
            }
            order = List.copyOf(result);
            result = order;
        }
        return result;
    }

    /** Position of a namespace in {@link #order()}; namespaces that are not listed come last. */
    public static int rank(String namespace) {
        int index = order().indexOf(namespace);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    /** Rule 1 and 2 on the built tabs (idempotent). */
    public static void filter() {
        Registry<CreativeModeTab> registry = BuiltInRegistries.CREATIVE_MODE_TAB;
        Set<String> homes = new HashSet<>();
        for (CreativeModeTab tab : registry) {
            Identifier id = registry.getKey(tab);
            if (id != null && !isVanilla(id) && tab.getType() == CreativeModeTab.Type.CATEGORY) {
                for (ItemStack stack : tab.getDisplayItems()) homes.add(key(stack));
            }
        }
        for (CreativeModeTab tab : registry) {
            Identifier id = registry.getKey(tab);
            if (id == null || tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            if (isVanilla(id)) {
                tab.getDisplayItems().removeIf(stack -> isModItem(stack) && homes.contains(key(stack)));
                tab.getSearchTabDisplayItems().removeIf(stack -> isModItem(stack) && homes.contains(key(stack)));
            } else {
                tab.getDisplayItems().removeIf(CreativeTabPolicy::isPlainVanilla);
                tab.getSearchTabDisplayItems().removeIf(CreativeTabPolicy::isPlainVanilla);
            }
        }
    }

    /** The mod tabs (not Vanilla's, no search/inventory/hotbar) in the display order of rule 3. */
    public static List<CreativeModeTab> orderedModTabs() {
        Registry<CreativeModeTab> registry = BuiltInRegistries.CREATIVE_MODE_TAB;
        List<CreativeModeTab> tabs = new ArrayList<>();
        for (CreativeModeTab tab : registry) {
            Identifier id = registry.getKey(tab);
            if (id != null && !isVanilla(id)) tabs.add(tab);
        }
        // shown tabs first (Fabric does the same), then by mod tree, then registration order
        tabs.sort(Comparator.<CreativeModeTab>comparingInt(tab -> tab.shouldDisplay() ? 0 : 1)
                .thenComparingInt(tab -> rank(registry.getKey(tab).getNamespace()))
                .thenComparingInt(registry::getId));
        return tabs;
    }

    /** Rule 3: pages of 10 mod tabs (5 on the top row, 5 on the bottom row), like Fabric's own pagination. */
    public static void reorder() {
        List<CreativeModeTab> tabs = orderedModTabs();
        java.lang.reflect.Method setPage = null;
        for (int i = 0; i < tabs.size(); i++) {
            CreativeModeTab tab = tabs.get(i);
            if (setPage == null) {
                try {
                    setPage = tab.getClass().getMethod("fabric_setPage", int.class);
                } catch (NoSuchMethodException e) {
                    return; // not Fabric: its loader places the tabs
                }
            }
            int slot = i % TABS_PER_PAGE;
            try {
                setPage.invoke(tab, i / TABS_PER_PAGE + 1);
            } catch (ReflectiveOperationException e) {
                return;
            }
            CreativeModeTabPosition position = (CreativeModeTabPosition) tab;
            position.simplelib$setRow(slot < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM);
            position.simplelib$setColumn(slot % 5);
        }
    }

    /** Called by the mixin after Vanilla (and Fabric's pagination) built all tabs. */
    public static void apply() {
        filter();
        reorder();
    }
}
