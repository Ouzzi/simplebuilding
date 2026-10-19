package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import com.simplebuilding.modules.simpleinterfaces.tooltip.BundleView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.BundleContents;
import org.jspecify.annotations.Nullable;

/** Client: the bundle view for these contents with the local config; null = Vanilla (sub-mod or switch off, <= 12 entries). */
public final class BundleViews {
    private BundleViews() {}

    public static @Nullable BundleView of(BundleContents contents) {
        return on() ? BundleView.of(contents.size(), contents.getSelectedItemIndex(), ContainersClient.config()) : null;
    }

    /** Mouse-wheel range: every entry while the view is on, else Vanilla's {@code vanillaShown}. */
    public static int selectable(BundleContents contents, int vanillaShown) {
        return on() ? BundleView.selectable(contents.size(), vanillaShown, ContainersClient.config()) : vanillaShown;
    }

    private static boolean on() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && SimpleInterfaces.active(mc.level);
    }
}
