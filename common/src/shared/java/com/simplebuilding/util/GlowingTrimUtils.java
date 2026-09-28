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

    public static int getGlowLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // 1. Prüfe auf das neue Level-System
        Integer level = stack.get(ModDataComponentTypes.GLOW_LEVEL);
        if (level != null && level > 0) {
            return level;
        }

        // 2. Fallback: Alte Items mit Boolean gelten als Level 1
        if (Boolean.TRUE.equals(stack.get(ModDataComponentTypes.VISUAL_GLOW))) {
            return 1;
        }

        return 0;
    }

    public  static void setGlowLevel(ItemStack stack, int level) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponentTypes.GLOW_LEVEL, level);
    }

    // --- LOGIK FÜR PULSATING (Besatz blendet im Takt nach Schwarz und zurueck) ---

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

    /** Dauer eines Pulses (hell - schwarz - hell) in Millisekunden: ein ruhiger Herzschlag wie der des Wardens. */
    public static final long PULSE_PERIOD_MS = 2400L;

    /**
     * Helligkeit des pulsierenden Besatzes zur Zeit {@code millis}: 1 = volle Farbe, 0 = schwarz.
     * Weicher Kosinus, damit der Besatz einen Moment hell und einen Moment dunkel verweilt.
     */
    public static float pulseBrightness(long millis) {
        double phase = (millis % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        return (float) (0.5 + 0.5 * Math.cos(phase * Math.PI * 2.0));
    }

    /** Die Tönung (ARGB) fuer den Besatz: Weiss mit der Helligkeit {@code brightness} multipliziert. */
    public static int pulseTint(float brightness) {
        int v = Math.round(Math.max(0.0F, Math.min(1.0F, brightness)) * 255.0F);
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }
}