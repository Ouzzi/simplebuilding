package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.util.PlacedPlate;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet die abgelegte Schmiedevorlage ({@link PlacedTemplateBlock}): das Item-Modell der Vorlage
 * ohne Anzeige-Transformation ({@link ItemDisplayContext#NONE}) - bei Vorlagen das ausgestanzte
 * {@code item/generated}, also die Item-Textur als Platte mit Raendern - auf {@link #SCALE} der
 * Blockflaeche, in der Dicke auf gut einen Pixel ({@link #THICKNESS_SCALE}) gestreckt und mit der
 * Rueckseite an Boden, Wand oder Decke.
 *
 * <p>Ausrichtung: an der Wand zeigt die Vorlage in {@code FACING} (Oberkante nach oben), am Boden
 * und an der Decke zeigt ihre Oberkante in die Blickrichtung beim Ablegen, man liest sie also
 * aufrecht. Dieselbe Lage rechnet {@link PlacedPlate#transform} fuer die pixelgenaue Trefferform -
 * wer hier etwas an der Lage aendert, muss es dort mitziehen. Loader-neutral; registriert wird der
 * Renderer je Loader.
 */
public class PlacedTemplateRenderer implements BlockEntityRenderer<PlacedTemplateBlockEntity, PlacedTemplateRenderer.State> {
    /** Kantenlaenge der Vorlage in Blockbreiten; Lage und Trefferform rechnet {@link PlacedPlate}. */
    public static final float SCALE = PlacedPlate.SCALE;
    /** Streckung der Dicke: das Item-Modell ist 1/16 dick, gestreckt gut 1,4 Pixel. */
    public static final float THICKNESS_SCALE = PlacedPlate.THICKNESS_SCALE;
    /** Abstand zur Auflageflaeche gegen Z-Fighting. */
    private static final float GAP = PlacedPlate.GAP;

    private final ItemModelResolver itemModelResolver;

    public PlacedTemplateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Was der Renderer je Bild braucht: Lage und das aufgeloeste Item-Modell. */
    public static class State extends BlockEntityRenderState {
        public AttachFace face = AttachFace.FLOOR;
        public Direction facing = Direction.NORTH;
        public final ItemStackRenderState item = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlacedTemplateBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof PlacedTemplateBlock) {
            state.face = blockState.getValue(PlacedTemplateBlock.FACE);
            state.facing = blockState.getValue(PlacedTemplateBlock.FACING);
        }
        this.itemModelResolver.updateForTopItem(state.item, blockEntity.getTemplate(), ItemDisplayContext.NONE,
                blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        orient(poseStack, state.face, state.facing);
        float thickness = SCALE * THICKNESS_SCALE / 16.0F;
        poseStack.translate(0.0F, 0.0F, -(0.5F - thickness / 2.0F - GAP));
        poseStack.scale(SCALE, SCALE, SCALE * THICKNESS_SCALE);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /**
     * Dreht so, dass die Schauseite des Item-Modells (+Z) von der Auflage weg zeigt und seine
     * Oberkante (+Y) wie oben beschrieben liegt; der Ursprung wandert in die Blockmitte.
     */
    public static void orient(PoseStack poseStack, AttachFace face, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (face) {
            case WALL -> McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(-facing.toYRot()));
            case FLOOR -> {
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
                McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90.0F));
            }
            case CEILING -> {
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(-facing.toYRot()));
                McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(90.0F));
            }
        }
    }
}
