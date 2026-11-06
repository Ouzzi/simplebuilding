package com.simplelib.api.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Generic building block "this item is triggering something right now" (WH26-10, docs/ai/PLAN-A-COMPACT-2026-11-06.md): a soft red
 * lighthouse beam that turns slowly behind the item in its slot, drawn in every container screen (the inventory as well).
 * A mod registers a rule ({@link #register}); a slot shows the beam while any rule says yes for its stack - the rule should be
 * true only while the effect really acts, so the beam is never decoration. Simple Blocks' Collapsed blocks use it for their Slowness.
 *
 * <p>Client options (config/simplelib-client.json, SimpleLib config screen): {@link #enabled} (default true) and
 * {@link #reducedMotion} (default false: a still, softly breathing glow instead of the turning beam).
 */
public final class SlotAlert {
    private static final List<BiPredicate<LocalPlayer, ItemStack>> RULES = new CopyOnWriteArrayList<>();
    /** Master switch of the beam (client option {@code slotAlert}). */
    public static volatile boolean enabled = true;
    /** No turning: the slot glows still and breathes slowly (client option {@code slotAlertReducedMotion}). */
    public static volatile boolean reducedMotion = false;

    /** Turn time of the beam in milliseconds. */
    public static final long TURN_MS = 2600;
    private static final int RED = 0xFF2A2A;
    /** Angle of the pixel centres of a 16x16 slot seen from its middle, [0, 2pi). */
    private static final double[] ANGLE = new double[256];
    private static final double[] DIST = new double[256];

    static {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - 8, dy = y + 0.5 - 8;
                ANGLE[y * 16 + x] = Math.atan2(dy, dx) + Math.PI;
                DIST[y * 16 + x] = Math.sqrt(dx * dx + dy * dy);
            }
        }
    }

    private SlotAlert() {}

    /** Adds a rule: {@code (player, stack)} is true while the stack triggers something right now. */
    public static void register(BiPredicate<LocalPlayer, ItemStack> rule) {
        RULES.add(rule);
    }

    public static boolean active(ItemStack stack) {
        if (!enabled || stack.isEmpty()) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        for (var rule : RULES) {
            if (rule.test(player, stack)) return true;
        }
        return false;
    }

    /** Alpha (0-255) of the pixel at ({@code px}, {@code py}) of the slot at the given phase; pure, for the preview and the GameTest. */
    public static int alpha(int px, int py, double phase, boolean still) {
        int i = py * 16 + px;
        double fall = 1.0 - Math.min(1.0, DIST[i] / 14.0) * 0.45;
        if (still) return (int) ((0x1C + 0x14 * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2))) * fall);
        double beam = phase * Math.PI * 2;
        double diff = Math.abs(ANGLE[i] - beam);
        diff = Math.min(diff, Math.PI * 2 - diff);
        double cone = Math.max(0, 1.0 - diff / 0.75);
        return (int) ((0x14 + 0x5E * cone * cone) * fall);
    }

    /** Draws the beam into the 16x16 slot at ({@code x}, {@code y}); the caller draws the item afterwards. */
    public static void draw(GuiGraphicsExtractor g, int x, int y) {
        double phase = (System.currentTimeMillis() % (reducedMotion ? 4000 : TURN_MS)) / (double) (reducedMotion ? 4000 : TURN_MS);
        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 16; px++) {
                int a = alpha(px, py, phase, reducedMotion);
                if (a > 0) g.fill(x + px, y + py, x + px + 1, y + py + 1, a << 24 | RED);
            }
        }
    }
}
