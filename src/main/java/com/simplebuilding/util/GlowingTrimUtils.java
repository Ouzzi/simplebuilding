package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

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

        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return component.copyNbt().getInt(EMISSION_LEVEL_KEY, 0); // Default 0
    }

    public static void incrementEmissionLevel(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        int current = nbt.getInt(EMISSION_LEVEL_KEY, 0);

        if (current < 5) {
            int newLevel = current + 1;
            nbt.putInt(EMISSION_LEVEL_KEY, newLevel);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

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
}