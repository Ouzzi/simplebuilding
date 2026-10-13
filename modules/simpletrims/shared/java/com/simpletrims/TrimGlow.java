package com.simpletrims;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * The three trim upgrades on an armor piece (moved from SimpleBuilding's {@code GlowingTrimUtils}, same rules):
 * <ul>
 *   <li><b>Glowing</b> ({@code simpletrims:glow_level}, one level only, owner 2026-09-29): the trim is drawn fully
 *       lit. Older stacks with the boolean {@code simpletrims:visual_glow} count as Glowing.</li>
 *   <li><b>Emitting / Radiance</b> (0-5, in {@code custom_data} under {@link #EMISSION_LEVEL_KEY}): every level gives
 *       3 light points to the invisible light block of the wearer ({@code TrimLight}). The key keeps its SimpleBuilding
 *       name on purpose: stack data, no registry id, so old worlds need no migration.</li>
 *   <li><b>Pulsating</b> ({@code simpletrims:pulsating}): alone the trim colour fades to gray and back; with Glowing its
 *       brightness swells between 1 and 15.</li>
 * </ul>
 */
public final class TrimGlow {
    public static final String EMISSION_LEVEL_KEY = "SimpleBuildingEmissionLevel";
    public static final int MAX_EMISSION_LEVEL = 5;
    /** The only Glowing level (Glowing II was removed, owner 2026-09-29). */
    public static final int MAX_GLOW_LEVEL = 1;
    /** Light of the trim with Glowing: full brightness. */
    public static final int GLOW_LIGHT = 15;
    public static final int PULSE_LIGHT_MIN = 1;
    public static final int PULSE_LIGHT_MAX = 15;
    /** One pulse (peak - valley - peak) in milliseconds: a calm heartbeat like the warden's. */
    public static final long PULSE_PERIOD_MS = 2400L;
    /** Desaturation steps (0 = full colour ... STEPS-1 = gray); one texture each. */
    public static final int DESATURATION_STEPS = 16;

    private TrimGlow() {}

    public static int getEmissionLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(EMISSION_LEVEL_KEY, 0);
    }

    public static void incrementEmissionLevel(ItemStack stack) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int current = nbt.getIntOr(EMISSION_LEVEL_KEY, 0);
        if (current < MAX_EMISSION_LEVEL) {
            nbt.putInt(EMISSION_LEVEL_KEY, current + 1);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    /** Glowing II and higher read as Glowing (codec of {@code glow_level} and every read). */
    public static int normalizeGlowLevel(int level) {
        return Math.min(level, MAX_GLOW_LEVEL);
    }

    public static int getGlowLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer level = stack.get(TrimsRegistry.GLOW_LEVEL);
        if (level != null && level > 0) return normalizeGlowLevel(level);
        return Boolean.TRUE.equals(stack.get(TrimsRegistry.VISUAL_GLOW)) ? 1 : 0;
    }

    public static void setGlowLevel(ItemStack stack, int level) {
        if (stack.isEmpty()) return;
        stack.set(TrimsRegistry.GLOW_LEVEL, normalizeGlowLevel(level));
        stack.remove(TrimsRegistry.VISUAL_GLOW);
    }

    public static boolean isPulsating(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(TrimsRegistry.PULSATING, false);
    }

    public static void setPulsating(ItemStack stack, boolean pulsating) {
        if (stack.isEmpty()) return;
        if (pulsating) stack.set(TrimsRegistry.PULSATING, true);
        else stack.remove(TrimsRegistry.PULSATING);
    }

    // --- Light rules (owner 2026-09-29) ---

    /** Packs block and sky light like {@code LightCoordsUtil.pack}. */
    public static int packLight(int block, int sky) {
        return block << 4 | sky << 20;
    }

    public static int blockLight(int packed) {
        return (packed >> 4) & 0xF;
    }

    public static int skyLight(int packed) {
        return (packed >> 20) & 0xF;
    }

    /** Light the trim is drawn with: unchanged without Glowing, full with Glowing, the pulse with Glowing + Pulsating. */
    public static int trimLight(int environment, int glowLevel, boolean pulsating, long millis) {
        if (glowLevel <= 0) return environment;
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

    public static int pulseLight(long millis) {
        return Math.round(PULSE_LIGHT_MIN + (PULSE_LIGHT_MAX - PULSE_LIGHT_MIN) * pulseWave(millis));
    }

    /** 1 on the peak, 0 in the valley; a soft cosine so the trim rests a moment at both ends. */
    public static float pulseWave(long millis) {
        double phase = Math.floorMod(millis, PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        return (float) (0.5 + 0.5 * Math.cos(phase * Math.PI * 2.0));
    }

    public static int desaturationStep(long millis) {
        return Math.round((1.0F - pulseWave(millis)) * (DESATURATION_STEPS - 1));
    }

    public static float desaturationAmount(int step) {
        return Math.max(0, Math.min(DESATURATION_STEPS - 1, step)) / (float) (DESATURATION_STEPS - 1);
    }

    /** Desaturates an ARGB pixel by {@code amount} towards its Rec. 601 luma (no brighter, no darker); alpha stays. */
    public static int desaturate(int argb, float amount) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float luma = 0.299F * r + 0.587F * g + 0.114F * b;
        return (argb & 0xFF000000) | (Math.round(r + (luma - r) * a) << 16) | (Math.round(g + (luma - g) * a) << 8)
                | Math.round(b + (luma - b) * a);
    }
}
