package com.simplebuilding.tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Laserpunkte (Simple Tweaks: LaserRenderer). Seit 26.x ueber den SubmitNodeCollector statt
 * eines eigenen Immediate-Puffers; die Form (drei ueberlagerte Quads = "runder" Pixelpunkt), die
 * Groesse nach Entfernung und der Wandabstand gegen Z-Fighting sind uebernommen.
 */
public final class LaserRenderer {
    private LaserRenderer() {
    }

    public static void submit(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera) {
        if (!SimpleTweaks.config().laserPointer.enable) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer me = client.player;
        if (me == null) {
            return;
        }
        int color = SimpleTweaks.config().laserPointer.color;
        if (TweaksClient.isAimingLaser(me)) {
            HitResult hit = me.pick(SimpleTweaks.config().laserPointer.range, client.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
            if (hit.getType() != HitResult.Type.MISS) {
                Direction side = hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : Direction.UP;
                dot(collector, poseStack, camera, hit.getLocation(), side, color);
            }
        }
        UUID self = me.getUUID();
        TweaksNetwork.ACTIVE_LASERS.forEach((uuid, laser) -> {
            if (!uuid.equals(self)) {
                dot(collector, poseStack, camera, new Vec3(laser.x(), laser.y(), laser.z()), Direction.UP, color);
            }
        });
    }

    /** Groesse des Punkts: mindestens die Config-Groesse, mit der Entfernung wachsend. */
    public static float scaleFor(double distance) {
        return Math.max(SimpleTweaks.config().laserPointer.scale, (float) (distance * 0.12f));
    }

    private static void dot(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera, Vec3 pos, Direction side, int color) {
        double distance = camera.distanceTo(pos);
        float scale = scaleFor(distance);
        double wallOffset = 0.01 + distance * 0.002;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        poseStack.pushPose();
        poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        poseStack.translate(side.getStepX() * wallOffset, side.getStepY() * wallOffset, side.getStepZ() * wallOffset);
        switch (side) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            default -> {
            }
        }
        poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            quad(buffer, pose, -0.5f, -0.3f, 0.5f, 0.3f, r, g, b);
            quad(buffer, pose, -0.3f, -0.5f, 0.3f, 0.5f, r, g, b);
            quad(buffer, pose, -0.4f, -0.4f, 0.4f, 0.4f, r, g, b);
        });
        poseStack.popPose();
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x1, float y1, float x2, float y2, int r, int g, int b) {
        buffer.addVertex(pose, x1, y1, 0).setColor(r, g, b, 255);
        buffer.addVertex(pose, x2, y1, 0).setColor(r, g, b, 255);
        buffer.addVertex(pose, x2, y2, 0).setColor(r, g, b, 255);
        buffer.addVertex(pose, x1, y2, 0).setColor(r, g, b, 255);
    }

    /** Entfernung neben dem Fadenkreuz, solange man zielt (Simple Tweaks: InGameScreenHudMixin). */
    public static void renderHud(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer me = client.player;
        if (me == null || !TweaksClient.isAimingLaser(me) || client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS) {
            return;
        }
        double distance = client.hitResult.getLocation().distanceTo(me.getEyePosition());
        String text = String.format("%.1fm", distance);
        graphics.drawString(client.font, text, graphics.guiWidth() / 2 + 10, graphics.guiHeight() / 2 - 4, 0xFFFF5555, true);
    }
}
