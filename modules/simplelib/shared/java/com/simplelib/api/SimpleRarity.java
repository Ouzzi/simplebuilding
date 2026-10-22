package com.simplelib.api;

import com.mojang.serialization.Codec;
import com.simplelib.registry.LibComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;

/**
 * Rarity tiers above Vanilla's Epic (owner 21.10., docs/ai/PLAN-B-RARITY21-2026-10-21.md), for every module.
 * Stored as the item component {@code simplelib:rarity}; the Vanilla {@code rarity} stays Epic as the fallback. The name
 * style comes from one mixin on {@link ItemStack#getHoverName()}, so tooltip, hotbar name and chat item all show it.
 * <ul>
 *   <li>{@link #CURSED}: radiant red with a black shadow; in the tooltip the letters burn: orange and gold flames rise
 *       into them from below and run through the name (owner 21.10., docs/ai/PLAN-B-RARITY21B-2026-10-22.md).</li>
 *   <li>{@link #MYTHIC}: an opal that moves through the letters (aquamarine, azure, lavender) with a pearl gloss on top,
 *       a pearl sheen sweeping across in the tooltip, plus the enchantment glint (the rarest items).</li>
 * </ul>
 * The tooltip paints both tiers pixel row by pixel row ({@code com.simplelib.client.RarityText}, colours from
 * {@link #pixel}); everywhere else a letter has one colour, and off the client (server, chat) the name has the plain tier
 * style ({@link #color()}).
 */
public enum SimpleRarity implements StringRepresentable {
    CURSED("cursed", 0xFF3030, 0xFF000000),
    MYTHIC("mythic", 0x5CF2DE, 0xFF0F1838);

    /**
     * Flame ramp of the cursed letters, cool to hot: radiant red, red-orange, orange, gold, white-gold (the bright steps
     * of {@code UiFlames}' fire, each at least 4.5:1 on the tooltip background).
     */
    public static final int[] FIRE = {0xFF3030, 0xFF5A1E, 0xF7921C, 0xFFC832, 0xFFE98A};
    /** Opal of the mythic letters (cyclic): aquamarine, azure, lavender. */
    public static final int[] OPAL = {0x5CF2DE, 0x6AB6FF, 0xC6A4FF};
    /** Pearl: gloss on the top rows of the mythic letters. The sweeping sheen goes to white. */
    public static final int PEARL = 0xF2FBFF;
    /** Style insertion on tooltip letters: the client overlay paints letters carrying it row by row. */
    public static final String MARKER = "simplelib:rarity/";
    /** Pixel rows of a letter the colour functions know: 0 = top of a capital, 7 = the descender row. */
    public static final int ROWS = 8;

    public static final Codec<SimpleRarity> CODEC = StringRepresentable.fromEnum(SimpleRarity::values);
    public static final StreamCodec<ByteBuf, SimpleRarity> STREAM_CODEC =
            ByteBufCodecs.idMapper(ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Enum::ordinal);

    private static volatile @Nullable Thread clientThread;

    private final String name;
    private final int color;
    private final int shadow;

    SimpleRarity(String name, int color, int shadow) {
        this.name = name;
        this.color = color;
        this.shadow = shadow;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Text colour (RGB) where a letter cannot have its own: server, chat, the cursed hotbar name. */
    public int color() {
        return color;
    }

    /** Shadow colour (ARGB, opaque). */
    public int shadow() {
        return shadow;
    }

    public Style style() {
        return Style.EMPTY.withColor(color).withShadowColor(shadow);
    }

    /** Item properties: this tier, Vanilla Epic as the fallback and, for {@link #MYTHIC}, the enchantment glint. */
    public Item.Properties apply(Item.Properties properties) {
        properties.rarity(Rarity.EPIC).component(LibComponents.RARITY, this);
        if (this == MYTHIC) properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return properties;
    }

    public static @Nullable SimpleRarity of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(LibComponents.RARITY);
    }

    /** The tier whose {@link #MARKER} a letter's style carries, else null. */
    public static @Nullable SimpleRarity byMarker(@Nullable String insertion) {
        if (insertion == null || !insertion.startsWith(MARKER)) return null;
        for (SimpleRarity r : values()) if (insertion.length() == MARKER.length() + r.name.length() && insertion.endsWith(r.name)) return r;
        return null;
    }

    /**
     * The styled name ({@code getHoverName}). Off the client render thread (server, integrated server) and for cursed
     * names a translatable name with the plain tier style; on the client the mythic name gets its still opal letter by
     * letter (the hotbar compares names with equals, so nothing here may move with time).
     */
    public Component name(Component base) {
        if (this != MYTHIC || !onClient()) return Component.empty().append(base).withStyle(style());
        return letters(base.getString(), 0L, false);
    }

    /**
     * {@code text} letter by letter in this tier's colours at {@code millis} (pixel row 4, a letter's middle), with the
     * tier shadow; {@code marked} adds the {@link #MARKER} insertion so the tooltip overlay paints the letters row by row.
     */
    public MutableComponent letters(String text, long millis, boolean marked) {
        MutableComponent out = Component.empty().withStyle(s -> s.withShadowColor(shadow));
        int[] cps = text.codePoints().toArray();
        String marker = marked ? MARKER + name : null;
        for (int i = 0; i < cps.length; i++) {
            int c = pixel(i, cps.length, 4, millis);
            out.append(Component.literal(new String(Character.toChars(cps[i]))).withStyle(s -> s.withColor(c).withInsertion(marker)));
        }
        return out;
    }

    /**
     * Colour (RGB) of letter {@code index} of {@code count} in pixel row {@code row} (0 = top of a capital, 7 = the
     * descender row; rows above/below count as 0/7) at {@code millis}. The same functions draw the preview page.
     * <ul>
     *   <li>CURSED: every letter has a flame height that rises and falls as a wave running from the left through the name
     *       (1.7 s) and flickers (smooth noise, 110 ms); heat = flame - height of the row, so the bottom rows burn gold and
     *       orange, the top stays radiant red, and the orange climbs into the letters and sinks back.</li>
     *   <li>MYTHIC: the opal moves through the letters (5.2 s round), the top two rows glow pearl, and every 3.4 s a
     *       leaning white sheen sweeps across (1.1 s).</li>
     * </ul>
     */
    public int pixel(int index, int count, int row, long millis) {
        int r = Math.max(0, Math.min(ROWS - 1, row));
        if (this == CURSED) {
            double wave = Math.sin(2 * Math.PI * ((millis % 1700L) / 1700.0 - index * 0.12));
            double flame = 0.5 + 0.32 * wave + 0.18 * noise(index, millis / 110.0);
            double heat = flame * 1.3 - (ROWS - 1 - r) / (double) (ROWS - 1) + 0.12 * noise(index * 8 + r + 101, millis / 90.0);
            return ramp(FIRE, heat);
        }
        double s = (count <= 1 ? 0.0 : (double) index / count) * 0.6 + (millis % 5200L) / 5200.0;
        int c = cyclic(OPAL, s);
        if (r <= 1) c = mix(c, PEARL, r == 0 ? 0.32f : 0.16f);
        double phase = (millis % 3400L) / 1100.0;
        if (phase < 1.0) {
            double d = Math.abs(index - (ROWS - 1 - r) * 0.35 - (phase * (count + 6) - 3));
            if (d < 1.6) c = mix(c, 0xFFFFFF, (float) ((1.0 - d / 1.6) * 0.85));
        }
        return c;
    }

    /** {@code stops} spread evenly over 0..1, linear between them, clamped at both ends. */
    static int ramp(int[] stops, double x) {
        double pos = Math.max(0.0, Math.min(1.0, x)) * (stops.length - 1);
        int k = Math.min(stops.length - 2, (int) pos);
        return mix(stops[k], stops[k + 1], (float) (pos - k));
    }

    /** {@code stops} around a circle (period 1), eased between neighbours. */
    static int cyclic(int[] stops, double x) {
        double pos = (x - Math.floor(x)) * stops.length;
        int k = (int) pos;
        double f = pos - k;
        return mix(stops[k % stops.length], stops[(k + 1) % stops.length], (float) ((1 - Math.cos(Math.PI * f)) / 2));
    }

    /** Smooth value noise in -1..1 along {@code u} (one random value per whole step, eased in between). */
    static double noise(int seed, double u) {
        double k = Math.floor(u);
        double f = u - k;
        f = f * f * (3 - 2 * f);
        return hash(seed, (long) k) * (1 - f) + hash(seed, (long) k + 1) * f;
    }

    /** Deterministic -1..1 per (seed, step); integer math that JavaScript's Math.imul repeats exactly (preview). */
    static double hash(int seed, long step) {
        int h = seed * 374761393 + (int) step * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 32767.5 - 1;
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Called once by the client (render thread); never on a dedicated server. */
    public static void markClientThread() {
        clientThread = Thread.currentThread();
    }

    public static boolean onClient() {
        return Thread.currentThread() == clientThread;
    }
}
