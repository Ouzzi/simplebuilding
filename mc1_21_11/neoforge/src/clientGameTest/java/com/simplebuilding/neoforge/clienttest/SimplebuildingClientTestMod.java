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

        RendererProofRun run = new RendererProofRun();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> run.onClientTick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ExtractLevelRenderStateEvent.class,
                event -> BreakingStateRecorder.observe(event.getRenderState()));
    }
}
