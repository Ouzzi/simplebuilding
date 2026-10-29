package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;

/**
 * Leggings fix (BS29-4): the leg part of the leggings layer has no underside in Vanilla's texture, so legs look open from
 * below. The 4x4 underside is filled from the hem row of the leg's front face, only where it is transparent.
 */
public final class LeggingsFix {
    private LeggingsFix() {}

    public static boolean isLeggings(String path) {
        return path.startsWith("textures/entity/equipment/humanoid_leggings/") && path.endsWith(".png");
    }

    /** Fills the underside of a loaded leggings layer (64-wide armor layout, scaled by width/64). Returns the pixels filled. */
    public static int fill(NativeImage image) {
        if (!ContainersConfig.on() || !ContainersClient.config().leggingsFix) return 0;
        int w = image.getWidth(), h = image.getHeight();
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) argb[y * w + x] = image.getPixel(x, y);
        int filled = com.simplebuilding.modules.simpleinterfaces.ZoomMath.fillLeggingsUnderside(argb, w, h);
        if (filled > 0) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) image.setPixel(x, y, argb[y * w + x]);
        return filled;
    }
}
