package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.EnchantmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD des Geschwindigkeitsmessers ("Velocity"), solange er in einer Hand liegt: ein kleiner
 * Zeigertacho im Vanilla-Tooltip-Kasten (Besitzer 2026-09-28). Der Zeiger faehrt wie beim Auto
 * von unten links (Stillstand) ueber oben nach unten rechts (Vollausschlag bei
 * {@link #FULL_SCALE_BPS}); daneben die Zahl in Bloecken je Sekunde. Die Skala ist eine
 * Wurzelskala, damit Gehen (4,3 b/s) und Sprinten (5,6 b/s) noch sichtbar auseinanderliegen und
 * trotzdem ein Elytra-Flug auf die Skala passt. Der Zeiger glaettet sanft (Zeitkonstante
 * {@link #NEEDLE_TIME_CONSTANT} s, bildratenunabhaengig), die Zahl nicht.
 *
 * <p>Mit Beruehrung des Konstrukteurs kommen darunter grau Hoechst- und Durchschnittstempo seit
 * dem Anlegen dazu, am Boden die X/Z-Anteile, im Gleitflug ein Warnzeichen ab 15 b/s.
 */
public class SpeedometerHudOverlay {

    private static final int COLOR_SPEED = 0xFF7F4C;
    private static final int COLOR_STATS = 0xFFAAAAAA;
    private static final int COLOR_DANGER = 0xFFFF5555;
    private static final int COLOR_SAFE = 0xFF55FF55;

    /** Geschwindigkeit bei Vollausschlag (Bloecke/s). */
    public static final double FULL_SCALE_BPS = 50.0;
    /** Ab diesem Skalenanteil ist der Bogen rot (entspricht 32 b/s). */
    private static final double RED_ZONE = 0.8;
    /** Radius des Zifferblatts in GUI-Pixeln. */
    private static final int RADIUS = 13;
    private static final double NEEDLE_TIME_CONSTANT = 0.12;

    private static final int COLOR_ARC = 0xFF6F6F6F;
    private static final int COLOR_ARC_RED = 0xFFA83232;
    private static final int COLOR_TICK = 0xFFC6C6C6;
    private static final int COLOR_NEEDLE = 0xFFFF6A3D;
    private static final int COLOR_HUB = 0xFFE0E0E0;
    private static final int COLOR_UNIT = 0xFF8B8B8B;

    private static double topSpeed = 0.0;
    private static double speedSum = 0.0;
    private static long tickCount = 0;
    private static boolean wasHoldingSpeedometer = false;
    private static double needle = 0.0;
    private static long lastFrameNanos = 0L;

    /** Skalenanteil 0..1 fuer eine Geschwindigkeit (Wurzelskala, oben abgeschnitten). */
    public static double scaleFraction(double bps) {
        if (bps <= 0.0) return 0.0;
        return Math.min(1.0, Math.sqrt(bps / FULL_SCALE_BPS));
    }

    /**
     * Zeigerwinkel in Grad (mathematisch, 0 = rechts, gegen den Uhrzeigersinn): 225 unten links bei
     * 0, 90 oben bei der Haelfte, -45 unten rechts bei Vollausschlag.
     */
    public static double needleAngle(double fraction) {
        return 225.0 - 270.0 * fraction;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (client.options.hideGui) {
            // Fabric's element registry hangs the mod's overlays inside vanilla's own layers,
            // which F1 switches off as a whole; NeoForge's layer event does not, and there the
            // air jump bar, the velocity gauge and the rangefinder stayed on a hidden HUD. The
            // question has to be asked here, once, so both loaders give the same answer.
            return;
        }

        // 1. Liegt der Geschwindigkeitsmesser in einer Hand?
        ItemStack main = client.player.getMainHandItem();
        ItemStack off = client.player.getOffhandItem();
        boolean hasSpeedometer = main.is(ModItems.VELOCITY_GAUGE) || off.is(ModItems.VELOCITY_GAUGE);

        if (!hasSpeedometer) {
            if (wasHoldingSpeedometer) {
                // Statistik und Zeiger zuruecksetzen, wenn er weggelegt wird
                topSpeed = 0.0;
                speedSum = 0.0;
                tickCount = 0;
                needle = 0.0;
                lastFrameNanos = 0L;
                wasHoldingSpeedometer = false;
            }
            return;
        }
        wasHoldingSpeedometer = true;

        // 2. Oktant gleichzeitig gehalten? Dann rueckt der Kasten nach unten (RangefinderHudOverlay nach oben).
        boolean hasOctant = (main.getItem() instanceof OctantItem) || (off.getItem() instanceof OctantItem);

        // --- BERECHNUNG --- (im Sattel/Boot zaehlt das Fahrzeug)
        Entity moving = client.player.getVehicle() != null ? client.player.getVehicle() : client.player;
        Vec3 velocity = moving.getDeltaMovement();
        double velX = velocity.x;
        double velY = velocity.y;
        double velZ = velocity.z;

        // Am Boden zaehlt die Schwerkraft nicht mit, sonst stuenden im Stand dauernd ~1,6 b/s da.
        if (moving.onGround()) {
            velY = 0;
        }

        double speedBps = Math.sqrt(velX * velX + velY * velY + velZ * velZ) * 20.0;
        if (speedBps < 0.05) speedBps = 0.0;

        if (speedBps > topSpeed) topSpeed = speedBps;
        if (speedBps > 0.1) {
            speedSum += speedBps;
            tickCount++;
        }
        double avgSpeed = tickCount > 0 ? (speedSum / tickCount) : 0.0;

        // Zeiger sanft nachfuehren (nach echter Zeit, nicht je Bild).
        long now = System.nanoTime();
        double target = scaleFraction(speedBps);
        if (lastFrameNanos == 0L) {
            needle = target;
        } else {
            double dt = Math.min(0.25, (now - lastFrameNanos) / 1.0e9);
            needle += (target - needle) * (1.0 - Math.exp(-dt / NEEDLE_TIME_CONSTANT));
        }
        lastFrameNanos = now;

        ItemStack activeStack = main.is(ModItems.VELOCITY_GAUGE) ? main : off;
        boolean touched = EnchantmentHelper.hasConstructorsTouch(activeStack, client.level);

        // --- TEXT ---
        Component title = Component.translatable("hud.simplebuilding.velocity_gauge.title")
                .withStyle(touched ? ChatFormatting.AQUA : ChatFormatting.WHITE);
        Component number = Component.literal(String.format("%.1f", speedBps))
                .setStyle(Style.EMPTY.withColor(COLOR_SPEED));
        Component unit = Component.translatable("hud.simplebuilding.velocity_gauge.unit");

        List<Component> extras = new ArrayList<>();
        if (touched) {
            Component stats = Component.translatable("hud.simplebuilding.velocity_gauge.stats",
                    String.format("%.1f", topSpeed), String.format("%.1f", avgSpeed)).setStyle(Style.EMPTY.withColor(COLOR_STATS));
            if (client.player.isFallFlying()) {
                boolean danger = speedBps > 15.0;
                Component symbol = Component.literal(danger ? "⚠ " : "✔ ")
                        .setStyle(Style.EMPTY.withColor(danger ? COLOR_DANGER : COLOR_SAFE).withBold(true));
                extras.add(Component.empty().append(symbol).append(stats));
            } else {
                extras.add(stats);
                extras.add(Component.literal(String.format("X: %.1f  Z: %.1f", Math.abs(velX * 20), Math.abs(velZ * 20)))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        // --- LAYOUT ---
        Font font = client.font;
        int dial = RADIUS * 2 + 1;                                     // Breite des Zifferblatts
        int dialHeight = RADIUS + 1 + (int) Math.ceil(RADIUS * Math.sqrt(0.5)); // unten offen
        int readoutWidth = Math.max(font.width(number), font.width(unit));
        int gaugeRowWidth = dial + 5 + Math.max(readoutWidth, font.width("88.8"));
        int contentWidth = Math.max(gaugeRowWidth, font.width(title));
        for (Component line : extras) contentWidth = Math.max(contentWidth, font.width(line));

        int titleGap = 4;
        int lineStep = font.lineHeight + 2;
        int contentHeight = font.lineHeight + titleGap + dialHeight + (extras.isEmpty() ? 0 : 3 + extras.size() * lineStep - 2);

        int padding = 6;
        int x = 10 + padding;
        int y = context.guiHeight() / 2 - (contentHeight + padding * 2) / 2 + padding;
        if (hasOctant) {
            y += 35;
        }

        // Vanilla-Tooltip-Hintergrund (Sprites tooltip/background + tooltip/frame); bekommt den
        // INHALT und legt selbst 3 px Rand darum.
        TooltipRenderUtil.renderTooltipBackground(context, x, y, contentWidth, contentHeight, null);

        context.drawString(font, title, x, y, 0xFFFFFFFF, true);
        int dialTop = y + font.lineHeight + titleGap;
        drawDial(context, x, dialTop, needle);

        int readoutX = x + dial + 5;
        int readoutY = dialTop + (dialHeight - (font.lineHeight * 2 + 1)) / 2;
        context.drawString(font, number, readoutX, readoutY, 0xFFFFFFFF, true);
        context.drawString(font, unit, readoutX, readoutY + font.lineHeight + 1, COLOR_UNIT, true);

        int lineY = dialTop + dialHeight + 3;
        for (Component line : extras) {
            context.drawString(font, line, x, lineY, 0xFFFFFFFF, true);
            lineY += lineStep;
        }
    }

    /** Bogen (270 Grad, unten offen), fuenf Skalenstriche, Zeiger und Nabe; links oben bei (left, top). */
    private static void drawDial(GuiGraphics context, int left, int top, double fraction) {
        double cx = left + RADIUS + 0.5;
        double cy = top + RADIUS + 0.5;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int step = 0; step <= 270; step++) {
            double f = step / 270.0;
            double a = Math.toRadians(needleAngle(f));
            int px = (int) Math.floor(cx + RADIUS * Math.cos(a));
            int py = (int) Math.floor(cy - RADIUS * Math.sin(a));
            if (px == lastX && py == lastY) continue;
            lastX = px;
            lastY = py;
            context.fill(px, py, px + 1, py + 1, f >= RED_ZONE ? COLOR_ARC_RED : COLOR_ARC);
        }
        for (int tick = 0; tick <= 4; tick++) {
            double a = Math.toRadians(needleAngle(tick / 4.0));
            for (double r = RADIUS - 2.5; r <= RADIUS - 1.0; r += 0.5) {
                int px = (int) Math.floor(cx + r * Math.cos(a));
                int py = (int) Math.floor(cy - r * Math.sin(a));
                context.fill(px, py, px + 1, py + 1, tick == 4 ? COLOR_ARC_RED : COLOR_TICK);
            }
        }
        double a = Math.toRadians(needleAngle(Math.max(0.0, Math.min(1.0, fraction))));
        double length = RADIUS - 3.0;
        for (double r = 0.0; r <= length; r += 0.5) {
            int px = (int) Math.floor(cx + r * Math.cos(a));
            int py = (int) Math.floor(cy - r * Math.sin(a));
            context.fill(px, py, px + 1, py + 1, COLOR_NEEDLE);
        }
        int hx = (int) Math.floor(cx);
        int hy = (int) Math.floor(cy);
        context.fill(hx - 1, hy - 1, hx + 2, hy + 2, COLOR_HUB);
    }
}
