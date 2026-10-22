package com.simplebuilding.modules.simplehammers;

import com.simplehammers.client.HammerAreaClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/** Fabric client entry: the hammer's area outline and extra breaking cracks (Queue 738). */
public final class HammersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> HammerAreaClient.submitOutline(
                context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos));
        LevelRenderEvents.END_EXTRACTION.register(context -> HammerAreaClient.extractCracks(context.levelState(), context.level()));
    }
}
