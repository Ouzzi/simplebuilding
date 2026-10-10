package com.simplelib.registry;

import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral registration order: components, blocks, items, block entity (per loader), menus, creative tab. */
public final class LibRegistry {
    public static void tab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleLib.id("main"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplelib.main"))
                        .icon(() -> new ItemStack(LibItems.IRON_CRUCIBLE))
                        .displayItems((params, out) -> tabStacks().forEach(out::accept))
                        .build());
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(LibItems.IRON_CRUCIBLE));
        out.add(new ItemStack(LibItems.REINFORCED_CRUCIBLE));
        out.add(new ItemStack(LibItems.NETHERITE_CRUCIBLE));
        out.add(new ItemStack(LibItems.COPPER_BARREL));
        out.add(new ItemStack(LibItems.REINFORCED_BARREL));
        out.add(new ItemStack(LibItems.NETHERITE_BARREL));
        out.add(new ItemStack(LibItems.REINFORCED_CAULDRON));
        return out;
    }

    /** SimpleBuilding's own functional tab: the crucibles etc. also belong there when that mod is installed. */
    public static final net.minecraft.resources.Identifier SB_FUNCTIONAL_TAB =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "functional");

    /** Whether {@code tab} should receive {@link #tabStacks()}: Vanilla functional blocks (if allowed) or SimpleBuilding's. */
    public static boolean wantsStacks(net.minecraft.resources.ResourceKey<CreativeModeTab> tab, boolean vanillaAllowed) {
        if (tab.identifier().equals(SB_FUNCTIONAL_TAB)) return true;
        return vanillaAllowed && tab.equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS);
    }

    private LibRegistry() {}
}
