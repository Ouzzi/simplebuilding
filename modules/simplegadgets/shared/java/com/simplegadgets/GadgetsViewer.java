package com.simplegadgets;

import com.simplelib.api.RecipeViewer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Simple Gadgets in the recipe viewer (JEI24, a-fix09b): info pages for what the crafting recipe does not tell - the
 * Resonance Rod, the Echo Sounder and the Gauge (SimpleBuilding's pages before the split, now {@code jei.simplegadgets.info.*})
 * and the Transposer (SimpleBuilding shows its turns in its in-world tab; this page also works without SimpleBuilding).
 * Free of JEI classes, shown by SimpleLib's JEI plugin. Registered from {@link SimpleGadgets#init}.
 */
public final class GadgetsViewer {
    public static final String INFO = "jei.simplegadgets.info.";

    private GadgetsViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimpleGadgets.MOD_ID, GadgetsViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        page(out, "amethyst_lens", GadgetsRegistry.AMETHYST_LENS);
        page(out, "echo_sounder", GadgetsRegistry.ECHO_SOUNDER);
        page(out, "velocity_gauge", GadgetsRegistry.VELOCITY_GAUGE);
        page(out, "rotator", GadgetsRegistry.ROTATOR);
        return out;
    }

    private static void page(List<RecipeViewer.Info> out, String key, Item item) {
        if (item == null) return;
        out.add(new RecipeViewer.Info(List.of(new ItemStack(item)), List.of(Component.translatable(INFO + key))));
    }
}
