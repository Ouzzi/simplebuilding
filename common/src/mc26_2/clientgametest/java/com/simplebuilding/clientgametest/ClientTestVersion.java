package com.simplebuilding.clientgametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-test-only calls that differ between MC 26.2 and 26.3 - 26.2 side (twin in
 * mc26_3/overlay/clientgametest/java). Test source sets only, never shipped.
 */
public final class ClientTestVersion {

    /**
     * Whether a setting command fails when nothing would change. 26.3 answers "/gamerule x v" with x
     * already v ("already set"), "/time set noon" at noon and "/weather clear" in clear weather with a
     * command error; 26.2 accepts all three. Scene set-up passes this as "may match nothing", so the
     * commands stay strict (a misspelled rule still fails) wherever the game allows it.
     */
    public static final boolean SET_COMMANDS_REJECT_NO_CHANGE = false;

    /**
     * Whether Cloth Config's screen works on this line. The 26.4 snapshots run on the 26.3 Cloth Config
     * (no 26.4 build yet), whose screen crashes there (RenderPipeline moved back to blaze3d), so the
     * 26.4 build hides the ModMenu button and the client test skips the screen.
     */
    public static final boolean CLOTH_CONFIG_SCREEN = true;

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
