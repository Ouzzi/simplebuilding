package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.inventory.MenuType;

/**
 * One styled screen kind: config id ({@code screens.<id>}, lang {@code simpleinterfaces.option.screen.<id>}), the exact
 * Vanilla screen class it applies to (by name, so this record loads on a server; subclasses from other mods stay
 * Vanilla), the menu types it covers and the container box colours for an open container.
 */
public record ScreenStyle(String id, String screenClass, List<MenuType<?>> menus, Function<StyleContext, UiPalette> palette) {
    /** Package of the Vanilla container screens. */
    public static final String VANILLA = "net.minecraft.client.gui.screens.inventory.";
}
