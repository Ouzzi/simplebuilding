package com.simplebuilding.neoforge.clienttest;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point of the renderer proof. Lives in its own source set and its own mod so that not a
 * single class of it can end up in the shipped jar, and it is inert unless the run configuration
 * sets {@code -Dsimplebuilding.clienttest=true}.
 *
 * <p>Only two hooks are needed:
 * <ul>
 *   <li>{@link ClientTickEvent.Post} drives the test as a plain state machine on the client thread -
 *       no second thread, no semaphores, nothing that could deadlock against the render loop.</li>
 *   <li>{@link ExtractLevelRenderStateEvent} at {@link EventPriority#LOWEST} observes what the mod's
 *       own listener (default priority, so it runs first) appended to the breaking render states.</li>
 * </ul>
 */
@Mod(value = SimplebuildingClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class SimplebuildingClientTestMod {

    static final String MOD_ID = "simplebuildingclienttest";

    private static final String ENABLE_PROPERTY = "simplebuilding.clienttest";

    public SimplebuildingClientTestMod(IEventBus modEventBus, ModContainer modContainer) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            Log.info("inactive (-D" + ENABLE_PROPERTY + "=true not set)");
            return;
        }

        Log.info("active - the client will build a test scene, prove the renderers and then halt");

        // The hand written renderer proof that used to live beside this has been deleted: all
        // twelve of its checkpoints are in the shared scripts, the last one - the control that
        // takes the octant away again - as highlight-k-octant-removed. Keeping it would not have
        // been free either: the parity gate reads screenshot names out of this directory, so its
        // dead highlight-f-octant-removed kept showing up as a checkpoint NeoForge promises and
        // Fabric does not.
        SharedScriptRun run = new SharedScriptRun();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> run.onClientTick());
        // Same job as the Fabric driver's registration: let the shared recorder see what the mod
        // appended. LOWEST priority so it runs after the mod's own listener. Without it the
        // recorder stays empty and the control case passes for the wrong reason.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ExtractLevelRenderStateEvent.class,
                event -> com.simplebuilding.clientgametest.BreakingStateRecorder
                        .observe(event.getRenderState()));
    }
}
