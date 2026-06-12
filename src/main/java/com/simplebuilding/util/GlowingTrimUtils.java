package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class GlowingTrimUtils {
    public static final String EMISSION_LEVEL_KEY = "SimpleBuildingEmissionLevel";

    private static NbtCompound getOrCreateNbt(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            nbt = new NbtCompound();
            stack.setNbt(nbt);
        }
        return nbt;
    }

    public static int getEmissionLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(EMISSION_LEVEL_KEY);
    }

    public static void incrementEmissionLevel(ItemStack stack) {
        NbtCompound nbt = getOrCreateNbt(stack);
        int current = nbt.getInt(EMISSION_LEVEL_KEY);

        if (current < 5) {
            int newLevel = current + 1;
            nbt.putInt(EMISSION_LEVEL_KEY, newLevel);
            stack.setNbt(nbt);
            Simplebuilding.LOGGER.info("Applied Emitting Upgrade! New Level: " + newLevel + "/5");
        }
    }

    public static boolean hasVisualGlow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(ModDataComponentTypes.NBT_VISUAL_GLOW);
    }

    public static void setVisualGlow(ItemStack stack, boolean glowing) {
        if (stack.isEmpty()) return;
        NbtCompound nbt = getOrCreateNbt(stack);
        nbt.putBoolean(ModDataComponentTypes.NBT_VISUAL_GLOW, glowing);
        stack.setNbt(nbt);
    }

    public static int getGlowLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return 0;

        int level = nbt.getInt(ModDataComponentTypes.NBT_GLOW_LEVEL);
        if (level > 0) return level;

        if (nbt.getBoolean(ModDataComponentTypes.NBT_VISUAL_GLOW)) {
            return 1;
        }

        return 0;
    }

    public static void setGlowLevel(ItemStack stack, int level) {
        if (stack.isEmpty()) return;
        NbtCompound nbt = getOrCreateNbt(stack);
        nbt.putInt(ModDataComponentTypes.NBT_GLOW_LEVEL, level);
        stack.setNbt(nbt);
    }
}