package com.simplebuilding.tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplebuilding.version.McClientVersion;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        if (!SimpleTweaks.effectiveValues().laserEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer me = client.player;
        if (me == null) {
            return;
        }
        int color = SimpleTweaks.config().laserPointer.color;
        if (TweaksClient.isAimingLaser(me)) {
            HitResult hit = me.pick(SimpleTweaks.effectiveValues().laserRange(), client.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
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

    /**
     * Wie stark der Punkt mit der Entfernung mindestens mitwaechst: 0,004 Bloecke je Block sind bei
     * 70 Grad Sichtfeld und 1080 Bildzeilen gut drei Pixel - gerade so sichtbar, statt zu verschwinden.
     */
    public static final float MIN_ANGULAR_SIZE = 0.004f;

    /**
     * Groesse des Punkts in Bloecken: die Config-Groesse (auf 0,05..1 begrenzt) als feste Weltgroesse,
     * damit er auf einer fernen Wand klein bleibt; erst wenn er dort unter ~3 Pixel fiele, waechst er
     * mit. Frueher wuchs er mit 0,12 je Block, also gleich gross auf dem Bildschirm - auf 50 Bloecke
     * ein Kreis von sechs Bloecken.
     */
    public static float scaleFor(double distance) {
        float base = Math.max(0.05f, Math.min(1.0f, SimpleTweaks.config().laserPointer.scale));
        return Math.max(base, (float) (distance * MIN_ANGULAR_SIZE));
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
            case DOWN -> McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(90));
            case UP -> McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90));
            case SOUTH -> McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(180));
            case WEST -> McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(90));
            case EAST -> McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(-90));
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

    /** Abstand der Anzeige zur Fadenkreuzmitte (GUI-Pixel; frueher 10). */
    public static final int HUD_GAP = 13;
    private static final int COLOR_LASER = 0xFFFF5555;
    private static final int COLOR_READOUT = 0xFFAAAAAA;

    /**
     * Anzeige neben dem Fadenkreuz, solange man zielt (Simple Tweaks: InGameScreenHudMixin).
     * Ohne Verzauberung nur "Laser" (Besitzer 2026-09-28); mit Beruehrung des Konstrukteurs
     * ({@link LaserPointerItem#measures}) die Entfernung bis zum Laserpunkt (volle Laser-Reichweite,
     * nicht die Blockreichweite von {@code client.hitResult}, Audit #34), darunter grau der Zielblock
     * und seine Hoehe (Y und Unterschied zur eigenen Fusshoehe).
     */
    public static void renderHud(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer me = client.player;
        if (me == null || !TweaksClient.isAimingLaser(me)) {
            return;
        }
        int x = graphics.guiWidth() / 2 + HUD_GAP;
        int y = graphics.guiHeight() / 2 - 4;
        if (!LaserPointerItem.measures(me.getUseItem(), me.level())) {
            graphics.text(client.font, Component.translatable("hud.simplebuilding.amethyst_lens.laser"), x, y, COLOR_LASER, true);
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        HitResult hit = me.pick(SimpleTweaks.effectiveValues().laserRange(), partialTick, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            graphics.text(client.font, Component.translatable("hud.simplebuilding.amethyst_lens.distance", "--"), x, y, COLOR_LASER, true);
            return;
        }
        double distance = hit.getLocation().distanceTo(me.getEyePosition(partialTick));
        graphics.text(client.font, Component.translatable("hud.simplebuilding.amethyst_lens.distance",
                String.format("%.1f", distance)), x, y, COLOR_LASER, true);
        BlockPos pos = blockHit.getBlockPos();
        graphics.text(client.font, me.level().getBlockState(pos).getBlock().getName(), x, y + 10, COLOR_READOUT, true);
        graphics.text(client.font, Component.translatable("hud.simplebuilding.amethyst_lens.height", pos.getY(),
                LaserPointerItem.signed(pos.getY() - Mth.floor(me.getY()))), x, y + 20, COLOR_READOUT, true);
    }
}
