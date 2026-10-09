package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.items.custom.HammockItem;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A hammock tied to its first anchor hangs like a lead (queue N16, 2026-10-09; {@link HammockItem}): a rolled-up
 * strip of the hammock's wool sags from the anchor's middle to the holder's main hand, for every player in sight who
 * holds a tied hammock. Two crossed ribbons along a parabola, lit per segment like the world around it. Drawn from the
 * world-render hook of each loader ({@code BlockHighlightRenderer#renderInWorldWithCamera}), world coordinates.
 */
public final class HammockLeashRenderer {
    /** Segments along the rope. */
    private static final int SEGMENTS = 24;
    /** Half the width of the rolled cloth (blocks). */
    private static final float HALF_WIDTH = 0.045F;
    /** Only holders this close to the camera are drawn (blocks). */
    private static final double VIEW_RANGE = 64.0;

    private HammockLeashRenderer() {
    }

    /** Draws the tied hammocks of all players; {@code poseStack} is in world coordinates. */
    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Player player : client.level.players()) {
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof HammockItem hammock) || player.isSpectator()
                    || player.distanceToSqr(client.player) > VIEW_RANGE * VIEW_RANGE) {
                continue;
            }
            Optional<BlockPos> anchor = HammockItem.storedAnchor(stack, client.level);
            if (anchor.isEmpty()) {
                continue;
            }
            Vec3 hand = handPosition(client, player, partial);
            Vec3 from = Vec3.atCenterOf(anchor.get());
            Identifier wool = Identifier.withDefaultNamespace("textures/block/"
                    + ((HammockBlock) hammock.getBlock()).getColor().getSerializedName() + "_wool.png");
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(wool),
                    (pose, buffer) -> drawRope(client, pose, buffer, from, hand));
        }
    }

    /** The holder's main hand: in first person in front of the camera, otherwise at the side of the body. */
    static Vec3 handPosition(Minecraft client, Player player, float partial) {
        boolean right = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        if (player == client.player && client.options.getCameraType().isFirstPerson()) {
            Vec3 eye = player.getEyePosition(partial);
            Vec3 look = player.getViewVector(partial);
            float yaw = player.getViewYRot(partial) * Mth.DEG_TO_RAD;
            Vec3 rightVec = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
            return eye.add(look.scale(0.6)).add(rightVec.scale(0.35 * side)).add(0.0, -0.35, 0.0);
        }
        Vec3 base = player.getPosition(partial);
        float yaw = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 rightVec = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        return base.add(rightVec.scale(0.38 * side)).add(forward.scale(0.15)).add(0.0, player.getBbHeight() * 0.42, 0.0);
    }

    /** The rope's point at {@code t} (0 = anchor, 1 = hand): a straight line sagging by a parabola. */
    public static Vec3 point(Vec3 from, Vec3 to, double t) {
        double sag = 0.25 + from.distanceTo(to) * 0.06;
        return from.lerp(to, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
    }

    private static void drawRope(Minecraft client, PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        Vec3 horizontal = new Vec3(-dir.z, 0.0, dir.x);
        horizontal = horizontal.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : horizontal.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double length = dir.length();
        for (int i = 0; i < SEGMENTS; i++) {
            double t0 = (double) i / SEGMENTS;
            double t1 = (double) (i + 1) / SEGMENTS;
            Vec3 a = point(from, to, t0);
            Vec3 b = point(from, to, t1);
            int light = LightCoordsUtil.getLightCoords(client.level, BlockPos.containing(a.lerp(b, 0.5)));
            // u runs along the rope (one texture width per block), v across a narrow band of the wool
            float span = (float) Math.min(1.0, (t1 - t0) * length);
            float u0 = (float) ((t0 * length) % 1.0);
            if (u0 + span > 1.0F) {
                u0 = 1.0F - span;
            }
            float u1 = u0 + span;
            ribbon(pose, buffer, a, b, horizontal, light, u0, u1);
            ribbon(pose, buffer, a, b, up, light, u0, u1);
        }
    }

    /** One flat strip from {@code a} to {@code b}, {@link #HALF_WIDTH} to both sides along {@code across}, both faces. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 across, int light, float u0, float u1) {
        Vec3 w = across.scale(HALF_WIDTH);
        Vec3 n = b.subtract(a).cross(across);
        n = n.lengthSqr() < 1.0E-9 ? new Vec3(0.0, 1.0, 0.0) : n.normalize();
        float v0 = 0.4F;
        float v1 = 0.6F;
        vertex(pose, buffer, a.subtract(w), u0, v0, light, n);
        vertex(pose, buffer, b.subtract(w), u1, v0, light, n);
        vertex(pose, buffer, b.add(w), u1, v1, light, n);
        vertex(pose, buffer, a.add(w), u0, v1, light, n);
        Vec3 back = n.scale(-1.0);
        vertex(pose, buffer, a.add(w), u0, v1, light, back);
        vertex(pose, buffer, b.add(w), u1, v1, light, back);
        vertex(pose, buffer, b.subtract(w), u1, v0, light, back);
        vertex(pose, buffer, a.subtract(w), u0, v0, light, back);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 p, float u, float v, int light, Vec3 n) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
