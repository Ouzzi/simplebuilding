package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.util.PlacedBundles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Das gezeigte ("oberste") Item eines abgestellten Buendels ({@link PlacedBundleBlockEntity}): es
 * schwebt langsam drehend ueber dem Buendel, solange der eigene Spieler schleicht und sein Fadenkreuz
 * auf dem Buendel liegt - ohne Tooltip-Text. Welches Item oben liegt, entscheidet der Server
 * ({@link PlacedBundles#tickCycle}); hier wird nur gezeichnet. Das Buendel selbst ist ein
 * gewoehnliches Blockmodell. Loader-neutral; registriert wird der Renderer je Loader.
 */
public class PlacedBundleRenderer implements BlockEntityRenderer<PlacedBundleBlockEntity, PlacedBundleRenderer.State> {
    /** Hoehe der Itemmitte ueber der Blockunterkante (das Buendel ist 11 Pixel hoch). */
    private static final float HOVER_Y = 1.0F;
    /** Groesse des schwebenden Items (dazu kommt Vanillas Boden-Transformation). */
    private static final float ITEM_SCALE = 1.1F;

    private final ItemModelResolver itemModelResolver;

    public PlacedBundleRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Was der Renderer je Bild braucht: ob gezeigt wird, das Item und der Drehwinkel. */
    public static class State extends BlockEntityRenderState {
        public boolean visible;
        public float spin;
        public float bob;
        public final ItemStackRenderState item = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlacedBundleBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.visible = false;
        state.item.clear();
        if (!isLookedAt(blockEntity)) {
            return;
        }
        ItemStack shown = blockEntity.shownItem();
        if (shown.isEmpty()) {
            return;
        }
        state.visible = true;
        float time = (blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime()) + partialTicks;
        state.spin = time * 3.0F % 360.0F;
        state.bob = (float) Math.sin(time / 10.0F) * 0.04F;
        this.itemModelResolver.updateForTopItem(state.item, shown, ItemDisplayContext.GROUND,
                blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
    }

    /** Schleicht der eigene Spieler und zeigt sein Fadenkreuz auf dieses Buendel? */
    private static boolean isLookedAt(PlacedBundleBlockEntity blockEntity) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        HitResult hit = client.hitResult;
        return player != null && player.isShiftKeyDown() && hit instanceof BlockHitResult block
                && hit.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(blockEntity.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible || state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, HOVER_Y + state.bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
