package com.simplelib.warm;

import com.mojang.serialization.Codec;
import com.simplelib.config.LibConfig;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibTags;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Warm food (plan section 12): the component stores the world game time until which the stack is
 * warm. Game time advances everywhere while the server runs, so food cools in chests and unloaded
 * chunks too, only not inside a crucible (owner 39), which keeps refreshing it. A warm stack is
 * eaten 15 % faster.
 */
public record Warm(long until, boolean insulated) {
    public static final Codec<Warm> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("until").forGetter(Warm::until),
            Codec.BOOL.optionalFieldOf("insulated", false).forGetter(Warm::insulated)).apply(i, Warm::new));
    public static final StreamCodec<io.netty.buffer.ByteBuf, Warm> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Warm::until, ByteBufCodecs.BOOL, Warm::insulated, Warm::new);

    public Warm(long until) {
        this(until, false);
    }

    public static boolean warmable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(LibTags.WARMABLE_FOOD);
    }

    /** Remaining warm ticks at {@code now}; 0 when cold. */
    public static long remaining(ItemStack stack, long now) {
        Warm warm = stack.get(LibComponents.WARM);
        return warm == null ? 0 : Math.max(0, warm.until() - now);
    }

    public static boolean isWarm(ItemStack stack, Level level) {
        return level != null && remaining(stack, level.getGameTime()) > 0;
    }

    /** Warms the stack for the full duration from {@code now}. */
    public static void warm(ItemStack stack, long now) {
        stack.set(LibComponents.WARM, new Warm(now + LibConfig.warmDurationTicks));
    }

    /** How much slower food cools inside a bundle (owner: 2 day-night cycles there instead of half a cycle). */
    public static double insulation() {
        return Math.max(1.0, LibConfig.warmBundleDurationTicks / (double) LibConfig.warmDurationTicks);
    }

    /** Entering a bundle: the remaining time is stretched by the insulation factor. */
    public static void insulate(ItemStack stack, long now) {
        Warm warm = stack.get(LibComponents.WARM);
        if (warm == null || warm.insulated()) return;
        long left = Math.max(0, warm.until() - now);
        if (left == 0) { stack.remove(LibComponents.WARM); return; }
        stack.set(LibComponents.WARM, new Warm(now + Math.round(left * insulation()), true));
    }

    /** Leaving a bundle: back to the normal cooling speed. */
    public static void uninsulate(ItemStack stack, long now) {
        Warm warm = stack.get(LibComponents.WARM);
        if (warm == null || !warm.insulated()) return;
        long left = Math.max(0, warm.until() - now);
        if (left == 0) { stack.remove(LibComponents.WARM); return; }
        stack.set(LibComponents.WARM, new Warm(now + Math.round(left / insulation()), false));
    }

    /** Use duration of a warm stack: 15 % shorter (config {@code eatSpeedBonus}), at least one tick. */
    public static int fasterUse(int ticks) {
        return Math.max(1, (int) Math.round(ticks * (1.0 - LibConfig.eatSpeedBonus)));
    }

    /** Warmth 0..1 for display (glow strength). */
    public static float warmth(ItemStack stack, long now) {
        return Math.min(1.0F, remaining(stack, now) / (float) LibConfig.warmDurationTicks);
    }
}
