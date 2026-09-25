package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.items.custom.BuildingWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
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
 * Loader-neutral: die Geometrie wird ab 26.2 über das Submit-Node-System eingereicht
 * (Fabric: LevelRenderEvents.COLLECT_SUBMITS, NeoForge: entsprechendes Submit-Event).
 */
public final class BuildingWandPreviewRenderer {

    private static final int GHOST_ALPHA = 180;
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
        // Oktant mit Auswahl in der Nebenhand: die Fuellung (oder das Dach) der Figur, unabhaengig vom
        // Fadenkreuz - ein Klick auf irgendeinen Block baut genau das. Fehlendes Material rot.
        ItemStack octant = player.getOffhandItem();
        if (com.simplebuilding.blueprint.ShapeFill.hasSelection(octant)) {
            renderPreview(collector, poseStack, cameraPos, client, level,
                    com.simplebuilding.blueprint.ShapeFill.preview(level, player, stack, octant));
            return;
        }
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            // Bruecke: ein Rechtsklick in die Luft baut vom Block unter den Fuessen geradeaus.
            if (!(octant.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem)) {
                renderGhosts(collector, poseStack, cameraPos, client, level,
                        BuildingWandItem.getBridgePreview(level, player, stack, wandItem.getWandSquareDiameter()), 0xFFFFFF, GHOST_ALPHA);
            }
            return;
        }

        // Blaupause in der Nebenhand: statt der Flaeche die Geisterbloecke des Bauwerks, genau die,
        // die ein Klick jetzt setzen wuerde (vorhandenes Material, freie Stellen); Stellen, fuer die
        // Material fehlt, rot - nach einem Warn-Klick kurz kraeftig pulsierend.
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem) {
            renderPreview(collector, poseStack, cameraPos, client, level,
                    com.simplebuilding.blueprint.BlueprintBuilder.preview(level, player, stack, offHand, blockHit));
            return;
        }
        // Flaeche, Abdeckung oder (Linear + Schleichen) Linie - ausgerichtet wie beim Bau, deshalb mit
        // der echten Trefferposition (obere/untere Blockhaelfte entscheidet ueber Treppen und Stufen).
        BlockPos clicked = blockHit.getBlockPos();
        Vec3 hitRel = blockHit.getLocation().subtract(clicked.getX(), clicked.getY(), clicked.getZ());
        Map<BlockPos, BlockState> previewMap = BuildingWandItem.getPreviewStates(
                level, player, stack, clicked, blockHit.getDirection(), hitRel, wandItem.getWandSquareDiameter());
        renderGhosts(collector, poseStack, cameraPos, client, level, previewMap, 0xFFFFFF, GHOST_ALPHA);
    }

    /** Geisterbloecke eines Planer-Baus: setzbar weiss, ohne Material rot (nach einem Warnklick pulsierend). */
    private static void renderPreview(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos, Minecraft client,
                                      ClientLevel level, com.simplebuilding.blueprint.BlueprintBuilder.Preview preview) {
        renderGhosts(collector, poseStack, cameraPos, client, level, preview.placed(), 0xFFFFFF, GHOST_ALPHA);
        int alpha = GHOST_ALPHA;
        if (com.simplebuilding.blueprint.BlueprintBuilder.flashing()) {
            alpha = 150 + (int) (105 * Math.abs(Math.sin(net.minecraft.util.Util.getMillis() / 90.0)));
        }
        renderGhosts(collector, poseStack, cameraPos, client, level, preview.missing(), 0xFF3030, alpha);
    }

    /** Zeichnet Geisterbloecke; {@code tint} faerbt sie zusaetzlich ein (weiss = unveraendert). */
    private static void renderGhosts(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos, Minecraft client,
                                     ClientLevel level, Map<BlockPos, BlockState> previewMap, int tint, int alpha) {
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
            QuadInstance quadInstance = new QuadInstance();
            quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
            quadInstance.setLightCoords(LightCoordsUtil.getLightCoords(level, pos));

            BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(renderState);
            random.setSeed(renderState.getSeed(pos));
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(random, parts);

            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (BlockStateModelPart part : parts) {
                    for (Direction direction : DIRECTIONS) {
                        putQuads(part.getQuads(direction), pose, quadInstance, renderState, level, pos, blockColors, buffer, tint, alpha);
                    }
                    putQuads(part.getQuads(null), pose, quadInstance, renderState, level, pos, blockColors, buffer, tint, alpha);
                }
            });

            poseStack.popPose();
        }
    }

    private static void putQuads(List<BakedQuad> quads, PoseStack.Pose pose, QuadInstance quadInstance,
                                 BlockState state, ClientLevel level, BlockPos pos, BlockColors blockColors,
                                 VertexConsumer buffer, int tint, int alpha) {
        for (BakedQuad quad : quads) {
            int rgb = 0xFFFFFF;
            int tintIndex = quad.materialInfo().tintIndex();
            if (tintIndex != -1) {
                BlockTintSource tintSource = blockColors.getTintSource(state, tintIndex);
                if (tintSource != null) {
                    rgb = tintSource.colorInWorld(state, level, pos) & 0xFFFFFF;
                }
            }
            quadInstance.setColor(ARGB.color(alpha, ARGB.multiply(rgb | 0xFF000000, tint | 0xFF000000) & 0xFFFFFF));
            buffer.putBakedQuad(pose, quad, quadInstance);
        }
    }
}
