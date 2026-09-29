package com.simplebuilding.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.simplebuilding.util.GlowingTrimUtils;
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
 * {@link GlowingTrimUtils#desaturationAmount} auf die Luma jedes Pixels zu entsaettigt - das Grau ist genau so
 * hell wie die Farbe, die es ersetzt. Je Muster-Ebene und Palette gibt es hoechstens
 * {@link GlowingTrimUtils#DESATURATION_STEPS} - 1 kleine Texturen (Stufe 0 ist die Vanilla-Ebene), angelegt
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

    private TrimPulseTextures() {
    }

    private record Key(Identifier base, Identifier keyPalette, @Nullable Identifier palette) {
    }

    private static final class Entry {
        private final @Nullable NativeImage colored;
        private final int index;
        private final Identifier[] steps = new Identifier[GlowingTrimUtils.DESATURATION_STEPS];

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
     * @return die Textur, oder {@code null} bei Stufe 0 (volle Farbe - Vanilla zeichnen) und wenn eine Datei fehlt
     */
    public static @Nullable Identifier texture(Identifier base, Identifier keyPalette, @Nullable Identifier palette, int step) {
        if (step <= 0 || step >= GlowingTrimUtils.DESATURATION_STEPS) {
            return null;
        }
        Entry entry = CACHE.computeIfAbsent(new Key(base, keyPalette, palette), TrimPulseTextures::load);
        if (entry.colored == null) {
            return null;
        }
        Identifier id = entry.steps[step];
        if (id == null) {
            float amount = GlowingTrimUtils.desaturationAmount(step);
            NativeImage image = entry.colored.mappedCopy(argb -> GlowingTrimUtils.desaturate(argb, amount));
            id = Identifier.fromNamespaceAndPath("simplebuilding", "trim_pulse/" + entry.index + "_" + step);
            String label = id.toString();
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> label, image));
            entry.steps[step] = id;
        }
        return id;
    }

    private static Entry load(Key key) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try (NativeImage base = read(resources, key.base())) {
            if (key.palette() == null) {
                return new Entry(base.mappedCopy(argb -> argb), nextIndex++);
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
            return new Entry(colored, nextIndex++);
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("No pulsating trim texture for {} with palette {}: {}", key.base(), key.palette(), e.toString());
            return MISSING;
        }
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
