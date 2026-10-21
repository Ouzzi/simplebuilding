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
 *   <li>{@link #CURSED}: radiant red with a black shadow (cursed items such as the Biome Brush and the Red Nether Gem).</li>
 *   <li>{@link #MYTHIC}: turquoise to light green per letter with a shimmer in the tooltip, plus the enchantment glint
 *       (the rarest items such as the Strong Upgrade template).</li>
 * </ul>
 */
public enum SimpleRarity implements StringRepresentable {
    CURSED("cursed", 0xFF3030, 0xFF000000),
    MYTHIC("mythic", 0x3CF0D2, 0xFF0A3A32);

    /** Second colour of the mythic gradient and of its shimmer. */
    public static final int MYTHIC_GREEN = 0x9CFF6A;
    public static final int MYTHIC_SHIMMER = 0xF0FFF8;

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

    /** Text colour (RGB). */
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

    /**
     * The styled name. Off the client render thread (server, integrated server) a translatable name with the plain tier
     * style; on the client the mythic name is coloured letter by letter, {@code animate} lets the gradient move and a
     * shimmer run through it (tooltip only: the hotbar compares names with equals, so it gets the still gradient).
     */
    public Component name(Component base, boolean animate) {
        if (this != MYTHIC || !onClient()) return Component.empty().append(base).withStyle(style());
        return gradient(base.getString(), animate ? System.currentTimeMillis() : 0L, animate);
    }

    /** Mythic letters: turquoise to light green, moving with {@code millis}; with {@code shimmer} a light band every 3 s. */
    public static MutableComponent gradient(String text, long millis, boolean shimmer) {
        MutableComponent out = Component.empty().withStyle(s -> s.withShadowColor(MYTHIC.shadow));
        int[] cps = text.codePoints().toArray();
        int n = cps.length;
        float band = shimmer ? (millis % 3000L) / 3000f * (n + 8) - 4 : -100f;
        for (int i = 0; i < n; i++) {
            float pos = n <= 1 ? 0f : (float) i / (n - 1);
            float wave = 0.5f + 0.5f * (float) Math.sin(2 * Math.PI * (pos * 0.8 - millis / 2400.0));
            int c = mix(MYTHIC.color, MYTHIC_GREEN, wave);
            float d = Math.abs(i - band);
            if (d < 2f) c = mix(c, MYTHIC_SHIMMER, (2f - d) / 2f * 0.85f);
            out.append(Component.literal(new String(Character.toChars(cps[i]))).withColor(c));
        }
        return out;
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
