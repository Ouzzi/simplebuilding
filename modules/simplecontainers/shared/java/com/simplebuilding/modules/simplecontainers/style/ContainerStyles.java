package com.simplebuilding.modules.simplecontainers.style;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/**
 * The central list of screen styles. Each group of screens has its own class with a {@code STYLES} list (W0:
 * {@link StorageStyles}); a new group adds one line to {@link #GROUPS} and, for screen classes not yet covered, its
 * own background mixin. Loads on a server (no client classes).
 */
public final class ContainerStyles {
    /** One entry per group, in config order. */
    public static final List<List<ScreenStyle>> GROUPS = List.of(
            StorageStyles.STYLES,
            StationStyles.STYLES);

    private static final List<ScreenStyle> ALL = flatten();

    private ContainerStyles() {}

    private static List<ScreenStyle> flatten() {
        List<ScreenStyle> all = new ArrayList<>();
        GROUPS.forEach(all::addAll);
        return List.copyOf(all);
    }

    /** Every registered style. */
    public static List<ScreenStyle> all() {
        return ALL;
    }

    /** The style for menu {@code type} shown by the screen class {@code screenClass}, or {@code null}. */
    public static @Nullable ScreenStyle find(MenuType<?> type, String screenClass) {
        screenClass = StationStyles.SUBCLASSES.getOrDefault(screenClass, screenClass);
        for (ScreenStyle style : ALL) {
            if (style.screenClass().equals(screenClass) && style.menus().contains(type)) return style;
        }
        return null;
    }

    /** The style of a screen whose menu has no type (the player inventory), by its exact class, or {@code null}. */
    public static @Nullable ScreenStyle findMenuless(String screenClass) {
        for (ScreenStyle style : ALL) {
            if (style.menus().isEmpty() && style.screenClass().equals(screenClass)) return style;
        }
        return null;
    }

    /** The style with config id {@code id}, or {@code null}. */
    public static @Nullable ScreenStyle byId(String id) {
        for (ScreenStyle style : ALL) {
            if (style.id().equals(id)) return style;
        }
        return null;
    }
}
