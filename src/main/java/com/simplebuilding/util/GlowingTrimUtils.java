package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class GlowingTrimUtils {
    public static final String GLOW_LEVEL_KEY = "SimpleBuildingGlowLevel";

    public static int getGlowLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return component.copyNbt().getInt(GLOW_LEVEL_KEY, 0);
    }

    public static void incrementGlowLevel(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        int current = nbt.getInt(GLOW_LEVEL_KEY, 0);

        if (current < 5) {
            int newLevel = current + 1;
            nbt.putInt(GLOW_LEVEL_KEY, newLevel);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            // Logger (Deebok)
            Simplebuilding.LOGGER.info("Applied Glowing Upgrade! New Level: " + newLevel + "/5");
        }
    }
}