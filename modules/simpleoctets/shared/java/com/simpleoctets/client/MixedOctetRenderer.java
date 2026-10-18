package com.simpleoctets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleoctets.block.MixedOctetBlockEntity;
import com.simpleoctets.block.OctetCellBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet eine gemischte Achtelzelle (Queue N48, docs/ai/PLAN-OCTETS-MIX-2026-10-13.md) mit den gebackenen Modellen der
 * reinen Zellen: je Material einmal dessen Zellzustand mit genau seinen Achteln, wie der Kolben einen bewegten Block
 * ({@code submitMovingBlock}: Weltlicht aus der Licht-Engine, Aussenflaechen nehmen das Licht des Nachbarn). Keine
 * eigenen Modelle oder Texturen; reine Zellen brauchen diesen Renderer nicht. Loader-neutral, registriert je Loader.
 */
public class MixedOctetRenderer implements BlockEntityRenderer<MixedOctetBlockEntity, MixedOctetRenderer.State> {
    public MixedOctetRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Je Material ein bewegter Blockzustand (hoechstens acht). */
    public static class State extends BlockEntityRenderState {
        public final List<MovingBlockRenderState> blocks = new ArrayList<>(2);
        /** Abbaustufe 0-9 (Bruch-Risse) oder -1; die Welt zeichnet sie fuer unsichtbare Zellen nicht. */
        public int breakStage = -1;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MixedOctetBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.blocks.clear();
        state.breakStage = breakProgress == null ? -1 : breakProgress.progress();
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        for (BlockState material : materialStates(blockEntity)) {
            MovingBlockRenderState block = new MovingBlockRenderState();
            block.randomSeedPos = pos;
            block.blockPos = pos;
            block.blockState = material;
            block.biome = level.getBiome(pos);
            block.cardinalLighting = level.cardinalLighting();
            block.lightEngine = level.getLightEngine();
            state.blocks.add(block);
        }
    }

    /** Je Material der Zustand seiner reinen Zelle mit genau seinen Achteln (ohne Wasser; das zeichnet die Welt). */
    public static List<BlockState> materialStates(MixedOctetBlockEntity blockEntity) {
        BlockState cell = blockEntity.getBlockState();
        int mask = cell.getBlock() instanceof OctetCellBlock ? OctetCellBlock.mask(cell) : 0;
        List<BlockState> materials = new ArrayList<>(2);
        List<Integer> masks = new ArrayList<>(2);
        for (int i = 0; i < 8; i++) {
            BlockState part = blockEntity.part(i);
            if (part == null || (mask & (1 << i)) == 0) {
                continue;
            }
            int at = materials.indexOf(part);
            if (at < 0) {
                materials.add(part);
                masks.add(1 << i);
            } else {
                masks.set(at, masks.get(at) | 1 << i);
            }
        }
        List<BlockState> out = new ArrayList<>(materials.size());
        for (int i = 0; i < materials.size(); i++) {
            out.add(OctetCellBlock.withMask(materials.get(i), masks.get(i)));
        }
        return out;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (MovingBlockRenderState block : state.blocks) {
            collector.submitMovingBlock(poseStack, block, 0);
        }
        OctetBreakCracks.submit(state.blocks, state.breakStage, poseStack, collector);
    }
}
