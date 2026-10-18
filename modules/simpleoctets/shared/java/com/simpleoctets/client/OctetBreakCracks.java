package com.simpleoctets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** 26.3: break cracks for mixed octet cells (the world only draws them for RenderShape.MODEL, the cell is invisible). */
public final class OctetBreakCracks {

    private OctetBreakCracks() {
    }

    public static void submit(List<MovingBlockRenderState> blocks, int breakStage, PoseStack poseStack, SubmitNodeCollector collector) {
        if (breakStage < 0) {
            return;
        }
        RandomSource random = RandomSource.create();
        for (MovingBlockRenderState block : blocks) {
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block.blockState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(block.blockState.getSeed(block.blockPos));
            model.collectParts(random, parts);
            collector.submitBreakingBlockModel(poseStack, parts, breakStage, false);
        }
    }
}
