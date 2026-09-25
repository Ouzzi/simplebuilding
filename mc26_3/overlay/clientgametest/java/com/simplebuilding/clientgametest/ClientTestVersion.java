package com.simplebuilding.clientgametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** MC 26.3 side of {@code ClientTestVersion} (see the 26.2 twin). */
public final class ClientTestVersion {

    /** 26.3: gamerule / time set / weather without a change are command errors. */
    public static final boolean SET_COMMANDS_REJECT_NO_CHANGE = true;

    /** Cloth Config 26.3 draws its screen fine on 26.3 (see the 26.2 twin). */
    public static final boolean CLOTH_CONFIG_SCREEN = true;

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
