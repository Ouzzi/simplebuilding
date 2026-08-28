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

            Simplebuilding.LOGGER.info("Applied Emitting Upgrade! New Level: " + newLevel + "/5");
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
}