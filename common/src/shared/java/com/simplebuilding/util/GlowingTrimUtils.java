package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class GlowingTrimUtils {
    // Wir nennen den Key "Emission", um Verwirrung mit dem visuellen "Glow" zu vermeiden
    public static final String EMISSION_LEVEL_KEY = "SimpleBuildingEmissionLevel";

    // --- LOGIK FÜR EMITTING (Lichtquelle / Fackel-Effekt) ---

    /**
     * Gibt zurück, wie stark das Item Licht emittiert (0-5).
     * Dies ist für den DynamicLightHandler relevant.
     */
    public static int getEmissionLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // Prüfen, ob das generelle Upgrade überhaupt vorhanden ist (optional, falls du eine strikte Trennung willst)
        // Wenn du nur über den Level gehen willst, reicht der NBT Check.
        // boolean hasUpgrade = stack.getOrDefault(ModDataComponentTypes.LIGHT_SOURCE, false);
        // if (!hasUpgrade) return 0;

        CustomData component = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return component.copyTag().getIntOr(EMISSION_LEVEL_KEY, 0); // Default 0
    }

    public static void incrementEmissionLevel(ItemStack stack) {
        CustomData component = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = component.copyTag();
        int current = nbt.getIntOr(EMISSION_LEVEL_KEY, 0);

        if (current < 5) {
            int newLevel = current + 1;
            nbt.putInt(EMISSION_LEVEL_KEY, newLevel);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

            // Optional: Setze auch die LIGHT_SOURCE Component auf true, damit man es leicht abfragen kann
            // stack.set(ModDataComponentTypes.LIGHT_SOURCE, true);

            Simplebuilding.LOGGER.debug("Applied Emitting Upgrade! New Level: {}/5", newLevel);
        }
    }

    // --- LOGIK FÜR GLOWING (Visual / RGB / Fullbright Render) ---

    /**
     * Gibt zurück, ob der Trim visuell leuchten soll (Fullbright).
     * Dies ist NUR für den EquipmentRendererMixin relevant.
     */
    public static boolean hasVisualGlow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Nutzt deine existierende DataComponent für das visuelle Upgrade
        return stack.getOrDefault(ModDataComponentTypes.VISUAL_GLOW, false);
    }

    /**
     * Aktiviert das visuelle Leuchten für ein Item.
     */
    public static void setVisualGlow(ItemStack stack, boolean glowing) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponentTypes.VISUAL_GLOW, glowing);
    }

    /**
     * Die einzige Glowing-Stufe (Besitzer 2026-09-29: Glowing II gibt es nicht mehr). Hoehere gespeicherte
     * Werte liest schon der Codec von {@code simplebuilding:glow_level} als 1
     * ({@link #normalizeGlowLevel}); diese Methode kappt zusaetzlich, falls ein Stapel im Speicher noch
     * einen hoeheren Wert traegt.
     */
    public static final int MAX_GLOW_LEVEL = 1;

    /** Migration Glowing II (und hoeher) -> Glowing: alles ueber {@link #MAX_GLOW_LEVEL} wird 1, der Rest bleibt. */
    public static int normalizeGlowLevel(int level) {
        return Math.min(level, MAX_GLOW_LEVEL);
    }

    public static int getGlowLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // 1. Prüfe auf das neue Level-System
        Integer level = stack.get(ModDataComponentTypes.GLOW_LEVEL);
        if (level != null && level > 0) {
            return normalizeGlowLevel(level);
        }

        // 2. Fallback: Alte Items mit Boolean gelten als Level 1
        if (Boolean.TRUE.equals(stack.get(ModDataComponentTypes.VISUAL_GLOW))) {
            return 1;
        }

        return 0;
    }

    public  static void setGlowLevel(ItemStack stack, int level) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponentTypes.GLOW_LEVEL, normalizeGlowLevel(level));
    }

    // --- LICHTREGELN DES BESATZES (Besitzer 2026-09-29) ---
    //
    // Glowing leuchtet ruhig mit voller Helligkeit (15), pulsiert NICHT und heisst im Tooltip nur
    // "Glowing"; eine Stufe II gibt es nicht mehr (alte Stuecke mit Stufe 2 lesen sich als Glowing).
    // Nur Pulsating + Glowing schwankt in der Helligkeit (Licht 1 bis 15 im Pulstakt).
    // Pulsating allein laesst das Licht, wie es ist, und pulsiert stattdessen in der Saettigung:
    // volle Farbe -> grau -> volle Farbe (EquipmentRendererMixin, TrimPulseTextures).

    /** Lichtstufe des Besatzes mit Glowing: volle Helligkeit (Besitzer 2026-09-29). */
    public static final int GLOW_LIGHT = 15;
    /** Spanne der Helligkeit bei Pulsating + Glowing (Tal, Spitze). */
    public static final int PULSE_LIGHT_MIN = 1;
    public static final int PULSE_LIGHT_MAX = 15;

    /** Packt Block- und Himmelslicht wie {@code LightCoordsUtil.pack} (Block in Bit 4-7, Himmel in Bit 20-23). */
    public static int packLight(int block, int sky) {
        return block << 4 | sky << 20;
    }

    public static int blockLight(int packed) {
        return (packed >> 4) & 0xF;
    }

    public static int skyLight(int packed) {
        return (packed >> 20) & 0xF;
    }

    /**
     * Licht, mit dem der Besatz gezeichnet wird. {@code environment} ist das gepackte Licht der Umgebung.
     * Ohne Glowing bleibt es unveraendert (auch mit Pulsating); Glowing zeichnet Block- und Himmelslicht
     * mit {@link #GLOW_LIGHT} (voll hell); Glowing mit Pulsating ersetzt es durch {@link #pulseLight} - die
     * einzige Kombination, deren Helligkeit schwankt.
     */
    public static int trimLight(int environment, int glowLevel, boolean pulsating, long millis) {
        if (glowLevel <= 0) {
            return environment;
        }
        if (pulsating) {
            int level = pulseLight(millis);
            return packLight(level, level);
        }
        return packLight(GLOW_LIGHT, GLOW_LIGHT);
    }

    /** Vertex brightness also carries the pulse, so bright lightmaps cannot wash it out. */
    public static int trimColor(int argb, int glowLevel, boolean pulsating, long millis) {
        if (glowLevel <= 0 || !pulsating) return argb;
        float brightness = pulseLight(millis) / 15.0F;
        return (argb & 0xFF000000)
                | Math.round(((argb >>> 16) & 255) * brightness) << 16
                | Math.round(((argb >>> 8) & 255) * brightness) << 8
                | Math.round((argb & 255) * brightness);
    }

    /** Lichtstufe bei Pulsating + Glowing zur Zeit {@code millis}: 15 auf der Spitze, 1 im Tal des Pulses. */
    public static int pulseLight(long millis) {
        return Math.round(PULSE_LIGHT_MIN + (PULSE_LIGHT_MAX - PULSE_LIGHT_MIN) * pulseWave(millis));
    }

    // --- LOGIK FÜR PULSATING (Besitzer 2026-09-28, Lichtregeln 2026-09-29) ---

    /** Pulsiert der Besatz dieses Stapels? (Pulsating Armor Trim, Besitzer 2026-09-28) */
    public static boolean isPulsating(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(ModDataComponentTypes.PULSATING, false);
    }

    public static void setPulsating(ItemStack stack, boolean pulsating) {
        if (stack.isEmpty()) return;
        if (pulsating) {
            stack.set(ModDataComponentTypes.PULSATING, true);
        } else {
            stack.remove(ModDataComponentTypes.PULSATING);
        }
    }

    /** Dauer eines Pulses (Spitze - Tal - Spitze) in Millisekunden: ein ruhiger Herzschlag wie der des Wardens. */
    public static final long PULSE_PERIOD_MS = 2400L;

    /**
     * Der Puls zur Zeit {@code millis}: 1 auf der Spitze (volle Farbe bzw. volles Licht), 0 im Tal.
     * Weicher Kosinus, damit der Besatz einen Moment oben und einen Moment unten verweilt.
     */
    public static float pulseWave(long millis) {
        double phase = Math.floorMod(millis, PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        return (float) (0.5 + 0.5 * Math.cos(phase * Math.PI * 2.0));
    }

    /** Stufen der Entsaettigung (0 = volle Farbe ... STEPS-1 = grau); je Stufe eine Textur. */
    public static final int DESATURATION_STEPS = 16;

    /** Pulsating allein: Entsaettigungsstufe zur Zeit {@code millis} (0 auf der Spitze, STEPS-1 im Tal). */
    public static int desaturationStep(long millis) {
        return Math.round((1.0F - pulseWave(millis)) * (DESATURATION_STEPS - 1));
    }

    /** Anteil Grau einer Stufe: 0 = volle Farbe, 1 = vollstaendig grau. */
    public static float desaturationAmount(int step) {
        return Math.max(0, Math.min(DESATURATION_STEPS - 1, step)) / (float) (DESATURATION_STEPS - 1);
    }

    /**
     * Entsaettigt einen ARGB-Pixel um {@code amount} (0 = unveraendert, 1 = grau). Das Grau hat die Luma
     * (Rec. 601) des Pixels, damit der Besatz dabei weder heller noch dunkler wird; Alpha bleibt.
     */
    public static int desaturate(int argb, float amount) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float luma = 0.299F * r + 0.587F * g + 0.114F * b;
        int nr = Math.round(r + (luma - r) * a);
        int ng = Math.round(g + (luma - g) * a);
        int nb = Math.round(b + (luma - b) * a);
        return (argb & 0xFF000000) | (nr << 16) | (ng << 8) | nb;
    }
}
