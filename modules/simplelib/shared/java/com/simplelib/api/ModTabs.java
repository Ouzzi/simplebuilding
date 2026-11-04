package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * QW11 (owner 04.11., "creative tabs aufraeumen"): the one creative tab of a module that has no tab list of its own. The module
 * registers the tab with {@link #register} (title key {@code itemgroup.<namespace>.<path>} given as a literal for the style lint, a name "Simple X", an icon); it stays empty
 * until {@link CreativeTabPolicy} fills it after Vanilla built the tabs: every item of the namespace that the module offered to a Vanilla
 * tab (tools, ingredients, combat ...) moves here, in the order Vanilla's tab had, and then {@link TabSort} orders the block families.
 * So a module keeps inserting its items where it always did, and the Vanilla tab no longer shows them (spawn eggs excepted: Vanilla's
 * convention keeps them in the Spawn Eggs tab as well). Without items (module switched off) the tab stays hidden.
 */
public final class ModTabs {
    /** One collecting tab: namespace, the tab and the id paths that stay out (e.g. an item a super-mod shows in its own tab). */
    public record Collector(String namespace, CreativeModeTab tab, Set<String> excluded) {}

    private static final List<Collector> COLLECTORS = new ArrayList<>();

    private ModTabs() {}

    /** Registers the tab {@code <namespace>:<path>}; call while registries are open (mod initialisation). */
    public static CreativeModeTab register(String namespace, String path, String titleKey, Supplier<ItemStack> icon, String... excludedPaths) {
        CreativeModeTab tab = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(namespace, path),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable(titleKey))
                        .icon(icon)
                        .displayItems((parameters, output) -> { })
                        .build());
        COLLECTORS.add(new Collector(namespace, tab, Set.of(excludedPaths)));
        return tab;
    }

    /** The collecting tabs registered so far. */
    public static List<Collector> collectors() {
        return List.copyOf(COLLECTORS);
    }
}
