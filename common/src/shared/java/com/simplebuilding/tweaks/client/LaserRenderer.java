package com.simplebuilding.tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplebuilding.version.McClientVersion;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.ArrayList;
import java.util.List;
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
            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            HitResult hit = me.pick(SimpleTweaks.effectiveValues().laserRange(), partialTick, false);
            if (hit.getType() != HitResult.Type.MISS) {
                Direction side = hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : Direction.UP;
                // Der eigene Punkt liegt genau auf dem Sehstrahl (= Fadenkreuzmitte, Besitzer 2026-09-29).
                dot(collector, poseStack, camera, hit.getLocation(), side, color, me.getViewVector(partialTick));
            }
        }
        UUID self = me.getUUID();
        TweaksNetwork.ACTIVE_LASERS.forEach((uuid, laser) -> {
            if (!uuid.equals(self)) {
                dot(collector, poseStack, camera, new Vec3(laser.x(), laser.y(), laser.z()), Direction.UP, color, null);
            }
        });
    }

    /**
     * Wie stark der Punkt mit der Entfernung mindestens mitwaechst: 0,004 Bloecke je Block sind bei
     * 70 Grad Sichtfeld und 1080 Bildzeilen gut drei Pixel - gerade so sichtbar, statt zu verschwinden.
     */
    public static final float MIN_ANGULAR_SIZE = 0.005f;

    /** Der Punkt ist seit 2026-09-29 um dieses Mass groesser als die Config-Groesse (Besitzer: "etwas groesser"). */
    public static final float SIZE_FACTOR = 1.3f;

    /**
     * Groesse des Punkts in Bloecken: die Config-Groesse (auf 0,05..1 begrenzt) als feste Weltgroesse,
     * damit er auf einer fernen Wand klein bleibt; erst wenn er dort unter ~3 Pixel fiele, waechst er
     * mit. Frueher wuchs er mit 0,12 je Block, also gleich gross auf dem Bildschirm - auf 50 Bloecke
     * ein Kreis von sechs Bloecken.
     */
    public static float scaleFor(double distance) {
        float base = Math.max(0.05f, Math.min(1.0f, SimpleTweaks.config().laserPointer.scale));
        return Math.max(base * SIZE_FACTOR, (float) (distance * MIN_ANGULAR_SIZE));
    }

    /**
     * Wo der Punkt gezeichnet wird: {@code wallOffset} vor der getroffenen Flaeche gegen Z-Fighting.
     * Ohne Sehstrahl entlang der Flaechennormalen; mit Sehstrahl ({@code ray}, der eigene Punkt) auf
     * dem Strahl zurueck, bis er {@code wallOffset} vor der Flaeche liegt - so bleibt seine Mitte
     * auch schraeg auf eine Wand genau in der Fadenkreuzmitte (vorher rutschte er entlang der
     * Normalen seitlich weg). Streifender Blick: hoechstens das Fuenffache des Abstands zurueck.
     */
    public static Vec3 dotCentre(Vec3 hit, Direction side, double wallOffset, Vec3 ray) {
        if (ray == null) {
            return hit.add(side.getStepX() * wallOffset, side.getStepY() * wallOffset, side.getStepZ() * wallOffset);
        }
        double facing = Math.abs(side.getStepX() * ray.x + side.getStepY() * ray.y + side.getStepZ() * ray.z);
        return hit.subtract(ray.normalize().scale(wallOffset / Math.max(0.2, facing)));
    }

    private static void dot(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera, Vec3 hit, Direction side, int color, Vec3 ray) {
        double distance = camera.distanceTo(hit);
        float scale = scaleFor(distance);
        double wallOffset = 0.01 + distance * 0.002;
        Vec3 pos = dotCentre(hit, side, wallOffset, ray);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        poseStack.pushPose();
        poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
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

    /**
     * Anzeige des Resonanzstabs, solange man zielt (Simple Tweaks: InGameScreenHudMixin). Ohne
     * Verzauberung gar kein Text (Besitzer 2026-09-29: nur der Punkt; frueher "Laser" neben dem
     * Fadenkreuz). Mit Beruehrung des Konstrukteurs ({@link LaserPointerItem#measures}) im gemeinsamen
     * Anzeigekasten der Mod ({@link com.simplebuilding.client.gui.HudPanel}, wie der Oktant, statt eines
     * Textblocks neben dem Fadenkreuz): Titel = Name des Stabs, darunter die Entfernung bis zum
     * Laserpunkt (volle Laser-Reichweite, nicht die Blockreichweite von {@code client.hitResult}, Audit
     * #34), grau der Zielblock und seine Hoehe (Y und Unterschied zur eigenen Fusshoehe).
     */
    public static void renderHud(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer me = client.player;
        if (me == null || !TweaksClient.isAimingLaser(me) || !LaserPointerItem.measures(me.getUseItem(), me.level())
                || !com.simplebuilding.client.gui.ModHud.visible() || client.gui.hud.isHidden()) {
            return;
        }
        List<Component> lines = hudLines(client, me);
        com.simplebuilding.client.gui.HudPanel.draw(graphics, com.simplebuilding.client.gui.HudPanel.title(me.getUseItem()),
                lines, client.font.width("888.8 m"), com.simplebuilding.client.gui.HudPanel.Slot.ROD);
    }

    /** Die Zeilen unter dem Titel: Entfernung (hervorgehoben), Zielblock und Hoehe (grau); "--" ohne Treffer. */
    public static List<Component> hudLines(Minecraft client, LocalPlayer me) {
        List<Component> lines = new ArrayList<>();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        HitResult hit = me.pick(SimpleTweaks.effectiveValues().laserRange(), partialTick, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            lines.add(Component.translatable("hud.simplebuilding.amethyst_lens.distance", "--").withColor(COLOR_VALUE));
            return lines;
        }
        double distance = hit.getLocation().distanceTo(me.getEyePosition(partialTick));
        lines.add(Component.translatable("hud.simplebuilding.amethyst_lens.distance", String.format("%.1f", distance)).withColor(COLOR_VALUE));
        BlockPos pos = blockHit.getBlockPos();
        lines.add(me.level().getBlockState(pos).getBlock().getName().copy().withColor(COLOR_READOUT));
        lines.add(Component.translatable("hud.simplebuilding.amethyst_lens.height", pos.getY(),
                LaserPointerItem.signed(pos.getY() - Mth.floor(me.getY()))).withColor(COLOR_READOUT));
        return lines;
    }

    private static final int COLOR_VALUE = com.simplebuilding.client.gui.HudPanel.COLOR_VALUE & 0xFFFFFF;
    private static final int COLOR_READOUT = com.simplebuilding.client.gui.HudPanel.COLOR_SECONDARY & 0xFFFFFF;
}
