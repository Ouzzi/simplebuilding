package com.simplelib.api.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The readout box every Simple gadget shares next to the crosshair (OW24-2, docs/ai/PLAN-B-GADGETS24-2026-10-24.md):
 * SimpleBuilding's Octant and Resonance Rod, Simple Gadgets' Gauge. Moved here from SimpleBuilding's {@code HudPanel},
 * {@code HudFrame} and {@code ModHud} so a gadget looks and stacks the same with or without SimpleBuilding:
 * a flat dark box at 78 % with a three layer frame (outline, bevel, band; corners a 4-2-1-1 staircase), 4 px of air,
 * the item's name as the title, values in contrast-lifted colours (AGENTS.md rule 15, at least 4.5:1 over a white world).
 *
 * <p>Several boxes at once stack around the anchor in the order of their {@link #register registered} slots,
 * {@link #STACK_PITCH} px apart. Where the anchor sits, the scale and the on/off switch come from {@link #useSettings}
 * (SimpleBuilding hands in its client config {@code hudPositionX/Y}, {@code hudScale}, {@code showModHud}); alone the
 * defaults apply (shown, left edge, middle height, 100 %). No user is named here (principle 6a).
 */
public final class GadgetHud {
    /** Where and how large the boxes are: anchor in percent of the free screen room, scale in percent (50..200). */
    public record Settings(boolean visible, int percentX, int percentY, int scalePercent) {
    }

    public static final Settings DEFAULTS = new Settings(true, 0, 50, 100);

    /** Pixels the frame takes on every side. */
    public static final int FRAME = 3;
    /** Distance from the box's outer edge (its frame included) to the content: the frame plus 3 px of air. */
    public static final int PADDING = FRAME + 3;
    public static final int TITLE_GAP = 3;
    public static final int LINE_GAP = 2;
    /** Distance between the centres of two stacked boxes. */
    public static final int STACK_PITCH = 76;
    /** Gap in GUI pixels between a box and the screen edge. */
    public static final int MARGIN = 10;
    /** Fixed box opacity (78 %). */
    public static final int ALPHA = 0xC8;
    /** The bronze the box is mixed from (SimpleBuilding's octant UI fill). */
    private static final int BRONZE = 0x6F5F4B;
    private static final int BOX_FILL = ALPHA << 24 | scale(BRONZE, 0.20);
    private static final int[] FRAME_LAYERS = {ALPHA << 24 | 0x1E1F23, ALPHA << 24 | scale(BRONZE, 0.82), ALPHA << 24 | scale(BRONZE, 0.64)};
    /** Inset of the outermost frame layer in the first rows of the box (row 0, 1, 2, 3), 0 from row 4 on. */
    private static final int[] CORNER = {4, 2, 1, 1};
    private static final int WORST_BACKGROUND = over(BOX_FILL, 0xFFFFFF);
    public static final double MIN_CONTRAST = 4.5;
    /** Primary value tone (orange #FF7F4C, lifted to {@link #MIN_CONTRAST}). */
    public static final int COLOR_VALUE = ink(0xFFFF7F4C);
    /** Secondary lines (grey #AAAAAA, lifted). */
    public static final int COLOR_SECONDARY = ink(0xFFAAAAAA);
    /** Warnings (red #FF5555, lifted). */
    public static final int COLOR_DANGER = ink(0xFFFF5555);

    private record Slot(String id, int order, Predicate<Minecraft> active) {
    }

    private static final Map<String, Slot> SLOTS = new ConcurrentHashMap<>();
    private static volatile Supplier<Settings> settings = () -> DEFAULTS;

    private GadgetHud() {
    }

    /** Registers (or replaces) the box {@code id} (e.g. {@code "simplegadgets:gauge"}) at stacking position {@code order}. */
    public static void register(String id, int order, Predicate<Minecraft> active) {
        SLOTS.put(id, new Slot(id, order, active));
    }

    /** Who decides where the boxes go (last call wins; SimpleBuilding hands in its client config). */
    public static void useSettings(Supplier<Settings> source) {
        settings = source == null ? () -> DEFAULTS : source;
    }

    public static Settings settings() {
        try {
            Settings s = settings.get();
            return s == null ? DEFAULTS : s;
        } catch (RuntimeException e) {
            return DEFAULTS;
        }
    }

    /** Whether boxes are drawn this frame: switched on and the HUD not hidden (F1). */
    public static boolean visible(Minecraft client) {
        return client.player != null && settings().visible() && !client.gui.hud.isHidden();
    }

    /** Whether the registered box {@code id} is shown this frame. */
    public static boolean active(String id, Minecraft client) {
        Slot slot = SLOTS.get(id);
        if (slot == null || client.player == null) return false;
        try {
            return slot.active().test(client);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Vertical offset (unscaled GUI px) of box {@code id} around the anchor: the shown boxes spread evenly, centred. */
    public static int stackOffset(String id, Minecraft client) {
        List<Slot> ordered = new ArrayList<>(SLOTS.values());
        ordered.sort(Comparator.comparingInt(Slot::order).thenComparing(Slot::id));
        int shown = 0;
        int index = -1;
        for (Slot other : ordered) {
            boolean self = other.id().equals(id);
            if (self || active(other.id(), client)) {
                if (self) index = shown;
                shown++;
            }
        }
        if (index < 0) {
            index = shown;
            shown++;
        }
        return stackOffset(index, shown);
    }

    /** Offset of box {@code index} (0 = top) among {@code shown} boxes. */
    public static int stackOffset(int index, int shown) {
        return (int) Math.round((index - (shown - 1) / 2.0) * STACK_PITCH);
    }

    /** Top-left corner of a box {@code width} x {@code height} (unscaled) drawn at {@code scale}; 0 % = left/top margin, 100 % = right/bottom. */
    public static int[] origin(int screenWidth, int screenHeight, int width, int height, float scale, int percentX, int percentY) {
        double w = width * scale;
        double h = height * scale;
        double x = MARGIN + Math.max(0.0, screenWidth - 2.0 * MARGIN - w) * clampPercent(percentX) / 100.0;
        double y = MARGIN + Math.max(0.0, screenHeight - 2.0 * MARGIN - h) * clampPercent(percentY) / 100.0;
        return new int[] {(int) Math.round(x), (int) Math.round(y)};
    }

    /** Box scale as a factor (0.5 to 2.0). */
    public static float scale() {
        return Math.max(50, Math.min(200, settings().scalePercent())) / 100.0f;
    }

    /** Moves and scales the pose so a box drawn at (0, 0) lands at the anchor, shifted by {@code offsetY}; pair with {@link #end}. */
    public static void begin(GuiGraphicsExtractor context, int width, int height, int offsetY) {
        Settings s = settings();
        float scale = scale();
        int[] origin = origin(context.guiWidth(), context.guiHeight(), width, height, scale, s.percentX(), s.percentY());
        context.pose().pushMatrix();
        context.pose().translate(origin[0], origin[1] + offsetY * scale);
        context.pose().scale(scale, scale);
    }

    public static void end(GuiGraphicsExtractor context) {
        context.pose().popMatrix();
    }

    /** The box title for a gadget: its hover name, aqua when enchanted, white otherwise. */
    public static Component title(ItemStack stack) {
        return stack.getHoverName().copy().withStyle(stack.isEnchanted() ? ChatFormatting.AQUA : ChatFormatting.WHITE);
    }

    /** A text-only box: {@code title}, then {@code lines}, at least {@code minContentWidth} wide, in slot {@code id}. */
    public static void draw(GuiGraphicsExtractor context, Component title, List<Component> lines, int minContentWidth, String id) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int contentWidth = Math.max(minContentWidth, font.width(title));
        for (Component line : lines) contentWidth = Math.max(contentWidth, font.width(line));
        int contentHeight = font.lineHeight + (lines.isEmpty() ? 0 : TITLE_GAP + lines.size() * (font.lineHeight + LINE_GAP) - LINE_GAP);
        begin(context, contentWidth + PADDING * 2, contentHeight + PADDING * 2, stackOffset(id, client));
        background(context, PADDING, PADDING, contentWidth, contentHeight);
        context.text(font, title, PADDING, PADDING, 0xFFFFFFFF, true);
        int y = PADDING + font.lineHeight + TITLE_GAP;
        for (Component line : lines) {
            context.text(font, line, PADDING, y, 0xFFFFFFFF, true);
            y += font.lineHeight + LINE_GAP;
        }
        end(context);
    }

    /** The box around content at ({@code x}, {@code y}); it reaches {@link #PADDING} px beyond the content on every side. */
    public static void background(GuiGraphicsExtractor context, int x, int y, int contentWidth, int contentHeight) {
        frame(context, x - PADDING, y - PADDING, contentWidth + PADDING * 2, contentHeight + PADDING * 2);
    }

    private static void frame(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        int n = FRAME_LAYERS.length;
        for (int r = 0; r < h; r++) {
            int d = Math.min(r, h - 1 - r);
            for (int i = 0; i <= n && d >= i; i++) {
                int left = start(i, d);
                int color = i < n ? FRAME_LAYERS[i] : BOX_FILL;
                if (i < n && d >= i + 1) {
                    int inner = start(i + 1, d);
                    context.fill(x + left, y + r, x + inner, y + r + 1, color);
                    context.fill(x + w - inner, y + r, x + w - left, y + r + 1, color);
                } else {
                    context.fill(x + left, y + r, x + w - left, y + r + 1, color);
                }
            }
        }
    }

    private static int start(int i, int d) {
        int k = d - i;
        return i + (k >= 0 && k < CORNER.length ? CORNER[k] : 0);
    }

    /** {@code rgb} itself when it reads on the box with {@link #MIN_CONTRAST} over a white world, else lifted towards white just enough. */
    public static int ink(int rgb) {
        int color = 0xFF000000 | rgb & 0xFFFFFF;
        for (int percent = 0; percent <= 100; percent++) {
            int candidate = mix(color, 0xFFFFFFFF, percent / 100.0);
            if (contrast(candidate, WORST_BACKGROUND) >= MIN_CONTRAST) return candidate;
        }
        return 0xFFFFFFFF;
    }

    /** WCAG contrast of an opaque text colour on the box over a world of colour {@code worldRgb}. */
    public static double contrastOver(int text, int worldRgb) {
        return contrast(text, over(BOX_FILL, worldRgb));
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private static int over(int argb, int worldRgb) {
        double a = (argb >>> 24) / 255.0;
        int r = (int) Math.round(((argb >> 16) & 255) * a + ((worldRgb >> 16) & 255) * (1 - a));
        int g = (int) Math.round(((argb >> 8) & 255) * a + ((worldRgb >> 8) & 255) * (1 - a));
        int b = (int) Math.round((argb & 255) * a + (worldRgb & 255) * (1 - a));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static double luminance(int c) {
        double[] v = {(c >> 16) & 255, (c >> 8) & 255, c & 255};
        for (int i = 0; i < 3; i++) {
            v[i] /= 255.0;
            v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    private static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static int scale(int color, double f) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * f), g = (int) Math.min(255, ((color >> 8) & 255) * f),
                b = (int) Math.min(255, (color & 255) * f);
        return r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
