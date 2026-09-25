package com.simplebuilding.clientgametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** MC 26.4 snapshot side of {@code ClientTestVersion}: the 26.3 one, but without the Cloth Config screen. */
public final class ClientTestVersion {

    /** 26.3: gamerule / time set / weather without a change are command errors. */
    public static final boolean SET_COMMANDS_REJECT_NO_CHANGE = true;

    /**
     * No Cloth Config for 26.4 yet; the 26.3 build crashes drawing its screen (NoSuchFieldError on
     * RenderPipelines.GUI_TEXTURED - RenderPipeline moved back to blaze3d). The 26.4 build hides the
     * ModMenu button (mc26_4/fabric/build.gradle), so the test does not open the screen either.
     */
    public static final boolean CLOTH_CONFIG_SCREEN = false;

    private ClientTestVersion() {
    }

    /**
     * 26.3 takes the fade-in duration explicitly; the client tests freeze chunkSectionFadeInTime at 0,
     * so 0 asks exactly what 26.2 asked.
     */
    public static boolean isSectionCompiledAndVisible(Minecraft client, BlockPos pos) {
        return client.levelRenderer.isSectionCompiledAndVisible(pos, 0L);
    }

    /** 26.3 renamed ItemInHandRenderer and moved it onto the game renderer. */
    public static Object firstPersonItemRenderer(Minecraft client) {
        return client.gameRenderer.firstPersonHandsAndItemsRenderer;
    }
}
