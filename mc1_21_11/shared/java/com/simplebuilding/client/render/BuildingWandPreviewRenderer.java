package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.items.custom.BuildingWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Zeichnet die transluzente Ghost-Block-Vorschau des Building Wand
 * (Platzierungspositionen aus {@link BuildingWandItem#getPreviewStates}).
 * Loader-neutral: die Geometrie wird über das Submit-Node-System eingereicht
 * (Fabric 1.21.11: WorldRenderEvents.BEFORE_ENTITIES + WorldRenderContext#commandQueue,
 * das ist die Phase, in der die Submit-Nodes noch gesammelt werden).
 */
public final class BuildingWandPreviewRenderer {

    private static final int GHOST_ALPHA = 180;
    private static final float GHOST_ALPHA_F = GHOST_ALPHA / 255.0f;
    private static final float GHOST_SCALE = 0.5f;
    private static final Direction[] DIRECTIONS = Direction.values();

    private BuildingWandPreviewRenderer() {
    }

    /** Pose stack is camera-relative; world positions are translated by -cameraPos before drawing. */
    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BuildingWandItem wandItem)) {
            return;
        }

        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Map<BlockPos, BlockState> previewMap = BuildingWandItem.getPreviewStates(
                level, player, stack, blockHit.getBlockPos(), blockHit.getDirection(), wandItem.getWandSquareDiameter());
        if (previewMap.isEmpty()) {
            return;
        }

        RenderType renderType = RenderTypes.translucentMovingBlock();
        BlockColors blockColors = client.getBlockColors();
        RandomSource random = RandomSource.create();

        for (Map.Entry<BlockPos, BlockState> entry : previewMap.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState renderState = entry.getValue();

            if (!level.getBlockState(pos).canBeReplaced()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            // Ghost-Effekt: Modell um den Blockmittelpunkt herum verkleinern
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
            poseStack.translate(-0.5, -0.5, -0.5);

            // Pro Position ein eigener Submit-Node: submitCustomGeometry kopiert die aktuelle Pose,
            // der Zeichen-Callback läuft später mit dem VertexConsumer der RenderType-Gruppe.
            int lightCoords = LevelRenderer.getLightColor(level, pos);

            BlockStateModel model = client.getModelManager().getBlockModelShaper().getBlockModel(renderState);
            random.setSeed(renderState.getSeed(pos));
            List<BlockModelPart> parts = new ArrayList<>();
            model.collectParts(random, parts);

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (BlockModelPart part : parts) {
                    for (Direction direction : DIRECTIONS) {
                        putQuads(part.getQuads(direction), pose, lightCoords, renderState, level, pos, blockColors, buffer);
                    }
                    putQuads(part.getQuads(null), pose, lightCoords, renderState, level, pos, blockColors, buffer);
                }
            });

            poseStack.popPose();
        }
    }

    private static void putQuads(List<BakedQuad> quads, PoseStack.Pose pose, int lightCoords,
                                 BlockState state, ClientLevel level, BlockPos pos, BlockColors blockColors,
                                 VertexConsumer buffer) {
        for (BakedQuad quad : quads) {
            // 1.21.11 kennt weder QuadInstance noch BlockTintSource: die Tint-Farbe kommt direkt
            // von BlockColors und wird als r/g/b-Faktor an putBulkData übergeben (dort landet sie
            // per ARGB.colorFromFloat wieder in der Vertex-Farbe) — identisch zum QuadInstance-Weg.
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            int tintIndex = quad.tintIndex();
            if (tintIndex != -1) {
                int rgb = blockColors.getColor(state, level, pos, tintIndex);
                r = ARGB.red(rgb) / 255.0f;
                g = ARGB.green(rgb) / 255.0f;
                b = ARGB.blue(rgb) / 255.0f;
            }
            buffer.putBulkData(pose, quad, r, g, b, GHOST_ALPHA_F, lightCoords, OverlayTexture.NO_OVERLAY);
        }
    }
}
