package com.simplebuilding.clientgametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** MC 26.3 side of {@code ClientTestVersion} (see the 26.2 twin). */
public final class ClientTestVersion {

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
