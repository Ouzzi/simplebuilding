package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.custom.HammockShape;
import com.simplebuilding.blocks.entity.custom.HammockBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a whole hammock from its cloth head (docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md): the boxes of
 * {@link HammockShape} along the line between the anchors, at any angle - wool of the hammock's colour, stripped-oak
 * spreaders, string ropes. The other cells of the hammock draw nothing (their block models are empty).
 *
 * <p>Culling: a hammock spans up to seven blocks and crosses chunk sections, so it is drawn globally
 * ({@link #shouldRenderOffScreen}) within {@link #getViewDistance} of its middle; Forge and NeoForge also cull it by
 * its bounds ({@link HammockBlockEntity#getRenderBoundingBox}, {@link #getRenderBoundingBox}). Loader-neutral;
 * registered per loader.
 */
public class HammockRenderer implements BlockEntityRenderer<HammockBlockEntity, HammockRenderer.State> {
    private static final Identifier SPREADER = Identifier.withDefaultNamespace("textures/block/stripped_oak_log.png");
    private static final Identifier ROPE = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/block/hammock_rope.png");

    public HammockRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The boxes relative to the cloth head block, and the wool texture. */
    public static class State extends BlockEntityRenderState {
        public final List<HammockShape.Box> boxes = new ArrayList<>();
        public @Nullable Identifier wool;
        public double offsetX;
        public double offsetY;
        public double offsetZ;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static boolean draws(HammockBlockEntity entity) {
        BlockState state = entity.getBlockState();
        return state.getBlock() instanceof HammockBlock && state.getValue(HammockBlock.PART) == BedPart.HEAD && entity.spot() != null;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(HammockBlockEntity entity, Vec3 cameraPosition) {
        HammockLayout.Spot spot = entity.spot();
        return spot != null && draws(entity)
                && new Vec3(spot.middleX(), spot.anchor().getY(), spot.middleZ()).closerThan(cameraPosition, getViewDistance());
    }

    /** NeoForge ({@code IBlockEntityRendererExtension}): frustum culling by the hammock's bounds. No {@code @Override} (Fabric/Forge lack it). */
    public AABB getRenderBoundingBox(HammockBlockEntity entity) {
        return entity.getRenderBoundingBox();
    }

    @Override
    public void extractRenderState(HammockBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.boxes.clear();
        state.wool = null;
        HammockLayout.Spot spot = entity.spot();
        if (spot == null || !draws(entity)) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        state.offsetX = -pos.getX();
        state.offsetY = -pos.getY();
        state.offsetZ = -pos.getZ();
        state.boxes.addAll(HammockShape.boxes(spot));
        state.wool = Identifier.withDefaultNamespace("textures/block/"
                + ((HammockBlock) entity.getBlockState().getBlock()).getColor().getSerializedName() + "_wool.png");
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.boxes.isEmpty() || state.wool == null) {
            return;
        }
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(state.wool), (pose, buffer) -> draw(state, pose, buffer, light, 0));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(SPREADER), (pose, buffer) -> draw(state, pose, buffer, light, 1));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROPE), (pose, buffer) -> draw(state, pose, buffer, light, 2));
    }

    /** Texture group: 0 wool (cloth, hems), 1 stripped oak (spreaders), 2 rope (ropes, knots, ties). */
    private static int group(HammockShape.Part part) {
        return switch (part) {
            case CLOTH, HEM -> 0;
            case SPREADER -> 1;
            case ROPE, KNOT, TIE -> 2;
        };
    }

    /** Pixel rectangle on the part's texture. */
    private static float[] uv(HammockShape.Part part) {
        return switch (part) {
            case CLOTH -> new float[] {2, 0, 14, 16};
            case HEM -> new float[] {1, 0, 2, 16};
            case SPREADER -> new float[] {0, 4, 16, 6};
            case ROPE -> new float[] {7, 0, 8, 15};
            case KNOT -> new float[] {4, 4, 6, 6};
            case TIE -> new float[] {7, 0, 8, 7};
        };
    }

    private static void draw(State state, PoseStack.Pose pose, VertexConsumer buffer, int light, int group) {
        for (HammockShape.Box box : state.boxes) {
            if (group(box.part()) != group) {
                continue;
            }
            double[] c = {box.centre()[0] + state.offsetX, box.centre()[1] + state.offsetY, box.centre()[2] + state.offsetZ};
            float[] uv = uv(box.part());
            // six faces; (s1, s2) span the face with s1 x s2 = its normal, so the corners run counter-clockwise from outside
            face(buffer, pose, light, uv, c, box.a(), box.ha(), box.b(), box.hb(), box.c(), box.hc());
            face(buffer, pose, light, uv, c, neg(box.a()), box.ha(), box.c(), box.hc(), box.b(), box.hb());
            face(buffer, pose, light, uv, c, box.b(), box.hb(), box.c(), box.hc(), box.a(), box.ha());
            face(buffer, pose, light, uv, c, neg(box.b()), box.hb(), box.a(), box.ha(), box.c(), box.hc());
            face(buffer, pose, light, uv, c, box.c(), box.hc(), box.a(), box.ha(), box.b(), box.hb());
            face(buffer, pose, light, uv, c, neg(box.c()), box.hc(), box.b(), box.hb(), box.a(), box.ha());
        }
    }

    private static double[] neg(double[] v) {
        return new double[] {-v[0], -v[1], -v[2]};
    }

    /** One face with normal {@code n} at distance {@code hn}, spanned by {@code s1} (half {@code h1}) and {@code s2}. */
    private static void face(VertexConsumer buffer, PoseStack.Pose pose, int light, float[] uv, double[] c,
                             double[] n, double hn, double[] s1, double h1, double[] s2, double h2) {
        float u0 = uv[0] / 16.0F;
        float v0 = uv[1] / 16.0F;
        float u1 = uv[2] / 16.0F;
        float v1 = uv[3] / 16.0F;
        vertex(buffer, pose, light, c, n, hn, s1, -h1, s2, -h2, u0, v1);
        vertex(buffer, pose, light, c, n, hn, s1, h1, s2, -h2, u1, v1);
        vertex(buffer, pose, light, c, n, hn, s1, h1, s2, h2, u1, v0);
        vertex(buffer, pose, light, c, n, hn, s1, -h1, s2, h2, u0, v0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, double[] c, double[] n, double hn,
                               double[] s1, double k1, double[] s2, double k2, float u, float v) {
        float x = (float) (c[0] + n[0] * hn + s1[0] * k1 + s2[0] * k2);
        float y = (float) (c[1] + n[1] * hn + s1[1] * k1 + s2[1] * k2);
        float z = (float) (c[2] + n[2] * hn + s1[2] * k1 + s2[2] * k2);
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, (float) n[0], (float) n[1], (float) n[2]);
    }
}
