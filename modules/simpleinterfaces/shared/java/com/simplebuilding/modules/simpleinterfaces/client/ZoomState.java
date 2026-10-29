package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

/**
 * Spyglass and zoom key (BS29-3): one field-of-view factor for both, changed steplessly by the mouse wheel.
 * The factor is Vanilla's 0.1 for the spyglass (10x) and 0.25 for the zoom key; every hook asks here first and
 * does nothing when the matching option is off, so Vanilla stays Vanilla.
 */
public final class ZoomState {
    public static final double SPYGLASS = 0.1, KEY_START = 0.25;
    public static final KeyMapping KEY = new KeyMapping("key.simpleinterfaces.zoom", com.mojang.blaze3d.platform.InputConstants.KEY_C,
            StyleToggleClient.CATEGORY);
    private static double factor = SPYGLASS;
    private static boolean was;
    private static boolean fromKey;

    private ZoomState() {}

    private static ContainersConfig cfg() {
        return ContainersClient.config();
    }

    /** The zoom key is held, allowed by the config, and no screen is open. */
    public static boolean keyZoom() {
        Minecraft mc = Minecraft.getInstance();
        return ContainersConfig.on() && cfg().zoomHotkey && mc.player != null && mc.gui.screen() == null && KEY.isDown();
    }

    /** Looking through the spyglass or zooming with the key. */
    public static boolean zooming(AbstractClientPlayer player) {
        return player.isScoping() || keyZoom();
    }

    /** True when this mod sets the field of view (wheel zoom on the spyglass, or the zoom key); false = Vanilla value. */
    public static boolean overridesFov(AbstractClientPlayer player) {
        return ContainersConfig.on() && (keyZoom() || player.isScoping() && cfg().scopeWheelZoom);
    }

    public static float fov() {
        return (float) factor;
    }

    /** Sensitivity scale against the Vanilla value of the current view (the spyglass already lowers it itself). */
    public static double sensitivity(AbstractClientPlayer player) {
        return player.isScoping() ? factor / SPYGLASS : factor;
    }

    /** Client tick: a fresh zoom starts at the source's own factor. */
    public static void tick(Minecraft mc) {
        boolean now = mc.player != null && mc.player.isScoping() || keyZoom();
        if (now && !was) {
            fromKey = !(mc.player != null && mc.player.isScoping());
            factor = fromKey ? KEY_START : SPYGLASS;
        }
        was = now;
    }

    /** Wheel while zooming: true = used (the hotbar does not scroll). */
    public static boolean scroll(double wheel) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || wheel == 0 || !ContainersConfig.on() || !cfg().scopeWheelZoom || !zooming(mc.player)) return false;
        factor = com.simplebuilding.modules.simpleinterfaces.ZoomMath.factorAfter(factor, wheel, cfg().zoomMaxMagnification);
        return true;
    }

    /** Whether the HUD is reduced to overlay and crosshair right now. */
    public static boolean hideHud() {
        Minecraft mc = Minecraft.getInstance();
        return ContainersConfig.on() && cfg().scopeHideHud && mc.player != null && mc.options.getCameraType().isFirstPerson() && zooming(mc.player);
    }

    public static boolean edgeOverlay() {
        return ContainersConfig.on() && cfg().scopeEdgeOverlay;
    }

    public static final Identifier EDGE = Identifier.fromNamespaceAndPath("simpleinterfaces", "textures/gui/scope_edge.png");
}
