package com.simplebuilding.clientgametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-test-only calls that differ between MC 26.2 and 26.3 - 1.21.11 copy (same calls as the 26.2 side in
 * common/src/mc26_2/clientgametest/java). Test source sets only, never shipped.
 */
public final class ClientTestVersion {

    private ClientTestVersion() {
    }

    /** Vanilla's own "is the section at this position built and faded in" question. */
    public static boolean isSectionCompiledAndVisible(Minecraft client, BlockPos pos) {
        return client.levelRenderer.isSectionCompiledAndVisible(pos);
    }

    /** The first person hand renderer HeldItemRendererMixin decorates (read by reflection). */
    public static Object firstPersonItemRenderer(Minecraft client) {
        return client.getEntityRenderDispatcher().getItemInHandRenderer();
    }
}
