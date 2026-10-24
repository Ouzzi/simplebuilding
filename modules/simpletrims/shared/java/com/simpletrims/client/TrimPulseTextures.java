package com.simpletrims.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.simpletrims.TrimGlow;
import com.simpletrims.TrimInversion;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Texturen fuer den Saettigungs-Puls des Pulsating Armor Trim (Besitzer 2026-09-29): Pulsating allein
 * aendert Licht und Helligkeit nicht, der Besatz verliert im Takt nur seine Farbe (volle Farbe -> grau ->
 * volle Farbe). Die Besatz-Ebene wird dafuer so eingefaerbt wie von Vanilla (Graustufen-Muster, dessen
 * Schluesselfarben durch die Palette des Materials ersetzt werden) und dann je Stufe um
 * {@link TrimGlow#desaturationAmount} auf die Luma jedes Pixels zu entsaettigt - das Grau ist genau so
 * hell wie die Farbe, die es ersetzt. Je Muster-Ebene und Palette gibt es hoechstens
 * {@link TrimGlow#DESATURATION_STEPS} - 1 kleine Texturen (Stufe 0 ist die Vanilla-Ebene), angelegt
 * erst, wenn sie gebraucht werden.
 *
 * <p>Loader- und linienneutral (nur Vanilla-APIs); nur auf dem Render-Thread aufrufen. Fehlt eine Datei
 * (etwa die Palette eines fremden Materials), liefert {@link #texture} {@code null} und der Besatz wird wie
 * bei Vanilla gezeichnet - er pulsiert dann nur nicht.
 */
public final class TrimPulseTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Key, Entry> CACHE = new HashMap<>();
    /** Platzhalter fuer eine Kombination, deren Dateien fehlen (kein zweiter Versuch je Sitzung). */
    private static final Entry MISSING = new Entry(null, -1);
    private static int nextIndex;
    /** Der Ressourcen-Manager, mit dem {@link #CACHE} gefuellt wurde; ein Reload (F3+T) legt einen neuen an (REV24-F-11). */
    private static @Nullable ResourceManager cachedFor;

    private TrimPulseTextures() {
    }

    private record Key(Identifier base, Identifier keyPalette, @Nullable Identifier palette, boolean inverted) {
    }

    private static final class Entry {
        private final @Nullable NativeImage colored;
        private final int index;
        private final Identifier[] steps = new Identifier[TrimGlow.DESATURATION_STEPS];

        private Entry(@Nullable NativeImage colored, int index) {
            this.colored = colored;
            this.index = index;
        }
    }

    /**
     * Die Textur der Besatz-Ebene {@code base} (PNG-Pfad, z. B. {@code minecraft:textures/trims/entity/humanoid/sentry.png})
     * in den Farben von {@code palette} (PNG, eine Zeile; die Schluesselfarben stehen in {@code keyPalette}),
     * entsaettigt auf Stufe {@code step}. {@code palette == null}: die Ebene bleibt in ihren eigenen Farben.
     *
     * @param inverted Inverting upgrade: the layer is the negative of the pattern ({@link com.simpletrims.TrimInversion}); then
     *                 step 0 (full colour) is a texture of its own, too
     * @return die Textur, oder {@code null} bei Stufe 0 ohne Invertierung (volle Farbe - Vanilla zeichnen) und wenn eine Datei fehlt
     */
    public static @Nullable Identifier texture(Identifier base, Identifier keyPalette, @Nullable Identifier palette, int step, boolean inverted) {
        if (step < (inverted ? 0 : 1) || step >= TrimGlow.DESATURATION_STEPS) {
            return null;
        }
        dropIfReloaded();
        Entry entry = CACHE.computeIfAbsent(new Key(base, keyPalette, palette, inverted), TrimPulseTextures::load);
        if (entry.colored == null) {
            return null;
        }
        Identifier id = entry.steps[step];
        if (id == null) {
            float amount = TrimGlow.desaturationAmount(step);
            NativeImage image = step == 0 ? entry.colored.mappedCopy(argb -> argb) : entry.colored.mappedCopy(argb -> TrimGlow.desaturate(argb, amount));
            id = Identifier.fromNamespaceAndPath("simpletrims", "trim_pulse/" + entry.index + "_" + step);
            String label = id.toString();
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> label, image));
            entry.steps[step] = id;
        }
        return id;
    }

    /** Nach einem Ressourcen-Reload: native Bilder schliessen, Texturen abmelden, Cache leeren (auch MISSING, damit nachgelieferte Dateien greifen). */
    private static void dropIfReloaded() {
        ResourceManager now = Minecraft.getInstance().getResourceManager();
        if (now == cachedFor) {
            return;
        }
        cachedFor = now;
        for (Entry entry : CACHE.values()) {
            for (Identifier id : entry.steps) {
                if (id != null) {
                    Minecraft.getInstance().getTextureManager().release(id);
                }
            }
            if (entry.colored != null) {
                entry.colored.close();
            }
        }
        CACHE.clear();
    }

    private static Entry load(Key key) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try (NativeImage base = read(resources, key.base())) {
            if (key.palette() == null) {
                return new Entry(invertIf(key, base.mappedCopy(argb -> argb), null), nextIndex++);
            }
            int[] from = paletteRow(resources, key.keyPalette());
            int[] to = paletteRow(resources, key.palette());
            int n = Math.min(from.length, to.length);
            NativeImage colored = base.mappedCopy(argb -> {
                if ((argb >>> 24) == 0) {
                    return argb;
                }
                int rgb = argb & 0xFFFFFF;
                for (int i = 0; i < n; i++) {
                    if ((from[i] & 0xFFFFFF) == rgb) {
                        return (argb & 0xFF000000) | (to[i] & 0xFFFFFF);
                    }
                }
                return argb;
            });
            return new Entry(invertIf(key, colored, to), nextIndex++);
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("No pulsating trim texture for {} with palette {}: {}", key.base(), key.palette(), e.toString());
            return MISSING;
        }
    }

    /** The Inverting upgrade: replaces the coloured layer by its negative (fill = palette colour), else the layer itself. */
    private static NativeImage invertIf(Key key, NativeImage colored, int @Nullable [] palette) {
        if (!key.inverted()) {
            return colored;
        }
        int w = colored.getWidth();
        int h = colored.getHeight();
        int[] pattern = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pattern[y * w + x] = colored.getPixel(x, y);
            }
        }
        int[] colours = TrimInversion.colours(palette, pattern);
        int[] out = TrimInversion.invert(pattern, w, h, colours[0], colours[1], TrimInversion.DEFAULT);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                colored.setPixel(x, y, out[y * w + x]);
            }
        }
        return colored;
    }

    private static NativeImage read(ResourceManager resources, Identifier id) throws IOException {
        try (InputStream in = resources.open(id)) {
            return NativeImage.read(in);
        }
    }

    /** Die erste Zeile einer Palette als ARGB-Werte. */
    private static int[] paletteRow(ResourceManager resources, Identifier id) throws IOException {
        try (NativeImage image = read(resources, id)) {
            int[] row = new int[image.getWidth()];
            for (int x = 0; x < row.length; x++) {
                row[x] = image.getPixel(x, 0);
            }
            return row;
        }
    }
}
