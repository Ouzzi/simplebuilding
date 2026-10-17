package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;

import java.util.List;

/** 26.2: submitBreakingBlockModel takes a Mesh here; mixed octet break cracks come with the port run. */
public final class OctetBreakCracks {

    private OctetBreakCracks() {
    }

    public static void submit(List<MovingBlockRenderState> blocks, int breakStage, PoseStack poseStack, SubmitNodeCollector collector) {
    }
}
