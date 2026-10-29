package com.simplebuilding.modules.simpleinterfaces;

import java.util.HashMap;
import java.util.Map;

/**
 * Local look only ({@code config/simpleinterfaces.json}); nothing here reaches a server. Master switch plus one switch
 * per screen kind (the style ids of {@code ContainerStyles}); a kind not in the map is on.
 */
public final class ContainersConfig {
    /** Master switch: off = every screen exactly Vanilla. */
    public boolean enabled = true;
    /** Shows the development style comparison button outside styled screens. */
    public boolean showStyleToggle = false;
    /** Per screen kind (style id), default on. */
    public Map<String, Boolean> screens = new HashMap<>();
    /** Shulker box items show their contents as a slot grid in the tooltip. */
    public boolean tooltipShulker = true;
    /** The ender chest item shows the own ender chest (needs the mod on the server). */
    public boolean tooltipEnderChest = true;
    /** The grid only while Shift is held (otherwise the Vanilla lines and a hint). */
    public boolean tooltipNeedsShift = true;
    /** Grid panel in the shulker box's dye color. */
    public boolean tooltipColorByDye = true;
    /** Bundle tooltips show every entry instead of Vanilla's twelve (pages beyond {@code bundleMaxRows}). */
    public boolean bundleShowAll = true;
    /** Rows of four entries the bundle tooltip shows at most (3..16, Vanilla: 3). */
    public int bundleMaxRows = 6;

    /** Item slot highlight only behind the item (not over it), so the selected item stays fully visible. */
    public boolean slotHighlightBehind = true;
    /** Fills the missing underside of leggings (their legs showed a hole without boots). */
    public boolean leggingsFix = true;
    /** The spyglass overlay darkens only the screen edge instead of a black frame with a round window. */
    public boolean scopeEdgeOverlay = true;
    /** The rest of the HUD hides while zooming (crosshair stays). */
    public boolean scopeHideHud = true;
    /** The mouse wheel zooms steplessly while looking through the spyglass or zooming. */
    public boolean scopeWheelZoom = true;
    /** The zoom key zooms without a spyglass, with the same overlay. Off: zoom only with the spyglass. */
    public boolean zoomHotkey = false;
    /** Strongest magnification the wheel reaches (2..100). */
    public int zoomMaxMagnification = 30;

    /** Super-mod switch "Enable Simple QoL" (framework SubMods): off = every screen exactly Vanilla, no toggle button. Runtime only. */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled("simpleinterfaces"); }

    public void normalize() {
        zoomMaxMagnification = Math.clamp(zoomMaxMagnification, 2, 100);
        if (screens == null) screens = new HashMap<>();
        screens.values().removeIf(java.util.Objects::isNull);
    }

    /** Whether the style {@code id} is drawn. */
    public boolean isOn(String id) {
        normalize();
        return enabled && screens.getOrDefault(id, true);
    }
}
