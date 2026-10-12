package com.simplemaps.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderMapItem;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Owner N38: a wayfinder map held outside the dimension it charts shows an animated jamming signal instead of a
 * picture: TV static tinted after the map (Nether reddish, End violet, Overworld grey), a slow rolling bright band and
 * now and then a torn line. One 128x128 texture per map kind, redrawn every tick while a hand shows it.
 */
public final class StaticTexture {
    private static final Map<WayfinderMapItem.Kind, StaticTexture> ALL = new EnumMap<>(WayfinderMapItem.Kind.class);

    public final Identifier id;
    private final DynamicTexture texture;
    private final int tint;
    private long lastUsed = Long.MIN_VALUE;
    private long seed;

    private StaticTexture(WayfinderMapItem.Kind kind) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        this.id = SimpleMaps.id("static/" + name);
        this.texture = new DynamicTexture(() -> "simplemaps static " + name, ViewTexture.SIZE, ViewTexture.SIZE, true);
        this.tint = tint(kind);
        this.seed = name.hashCode() * 0x9E3779B97F4A7C15L;
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    /** Tint (RGB) of the static for a map kind. */
    public static int tint(WayfinderMapItem.Kind kind) {
        return switch (kind) {
            case NETHER -> 0xFF5A46;
            case END -> 0xB874FF;
            case OVERWORLD -> 0xD2D2C4;
        };
    }

    /** The texture for {@code kind}; marks it as in use so the next ticks animate it. */
    public static StaticTexture of(WayfinderMapItem.Kind kind, long tick) {
        StaticTexture t = ALL.computeIfAbsent(kind, StaticTexture::new);
        if (t.lastUsed < tick - 1) t.draw(tick);
        t.lastUsed = tick;
        return t;
    }

    /** Once per client tick: redraws the textures a hand showed recently. */
    public static void tick(long tick) {
        for (StaticTexture t : ALL.values()) if (tick - t.lastUsed <= 4) t.draw(tick);
    }

    public static void releaseAll() {
        ALL.values().forEach(t -> Minecraft.getInstance().getTextureManager().release(t.id));
        ALL.clear();
    }

    private int next() {
        seed ^= seed << 13;
        seed ^= seed >>> 7;
        seed ^= seed << 17;
        return (int) (seed >>> 33);
    }

    private void draw(long tick) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        int size = ViewTexture.SIZE;
        int band = (int) Math.floorMod(tick * 3, size + 40) - 20;
        int r = tint >> 16 & 0xFF, g = tint >> 8 & 0xFF, b = tint & 0xFF;
        for (int y = 0; y < size; y++) {
            int row = next();
            boolean torn = (row & 0x3F) == 0;
            int shift = torn ? (row >> 8 & 0x1F) : 0;
            int lift = Math.max(0, 40 - Math.abs(y - band) * 3) + (torn ? 70 : 0);
            for (int x = 0; x < size; x++) {
                int noise = next() & 0xFF;
                // Grainy: most pixels dark, some bright, like a dead channel.
                int v = Math.min(255, (noise * noise >> 8) + lift + ((x + shift) % 9 == 0 && torn ? 60 : 0));
                v = Math.max(18, v);
                pixels.setPixel(x, y, 0xFF000000 | (r * v / 255) << 16 | (g * v / 255) << 8 | (b * v / 255));
            }
        }
        texture.upload();
    }
}
