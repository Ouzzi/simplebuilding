package com.simplemusic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplemusic.pocket.PocketJukeboxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shown disc of a placed Pocket Jukebox (PJ23-1, like Simple Storage's placed bundle): it floats above the case while
 * this client's player sneaks and looks at it, turned to the camera, without text. Which disc that is the server decides
 * (Shift + mouse wheel, {@link com.simplemusic.pocket.PocketJukeboxBlock#scroll}); here it is only drawn.
 */
public class PocketJukeboxRenderer implements BlockEntityRenderer<PocketJukeboxBlockEntity, PocketJukeboxRenderer.State> {
    /** Height of the disc's middle above the block's bottom (the case with record and arm is 6.5 px high). */
    private static final float HOVER_Y = 0.85F;
    private static final float ITEM_SCALE = 0.55F;

    private final ItemModelResolver itemModelResolver;

    public PocketJukeboxRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public boolean visible;
        public float yaw;
        public float pitch;
        public float bob;
        public final ItemStackRenderState item = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PocketJukeboxBlockEntity be, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.visible = false;
        state.item.clear();
        if (!lookedAt(be)) return;
        ItemStack shown = be.shownDisc();
        if (shown.isEmpty()) return;
        state.visible = true;
        float time = (be.getLevel() == null ? 0L : be.getLevel().getGameTime()) + partialTicks;
        state.bob = (float) Math.sin(time / 10.0F) * 0.04F;
        Vec3 item = Vec3.atBottomCenterOf(be.getBlockPos()).add(0.0, HOVER_Y, 0.0);
        state.yaw = (float) Math.toDegrees(Math.atan2(camera.x - item.x, camera.z - item.z));
        double dx = camera.x - item.x, dz = camera.z - item.z;
        state.pitch = (float) Math.toDegrees(Math.atan2(camera.y - item.y, Math.sqrt(dx * dx + dz * dz)));
        this.itemModelResolver.updateForTopItem(state.item, shown, ItemDisplayContext.NONE, be.getLevel(), null, (int) be.getBlockPos().asLong());
    }

    private static boolean lookedAt(PocketJukeboxBlockEntity be) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        HitResult hit = client.hitResult;
        return player != null && player.isShiftKeyDown() && hit instanceof BlockHitResult block
                && hit.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(be.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible || state.item.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, HOVER_Y + state.bob, 0.5F);
        poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
        poseStack.rotate(Axis.XP.rotationDegrees(-state.pitch));
        // a flat sprite: only width and height smaller, one whole pixel deep (like the placed bundle)
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, 1.0F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
