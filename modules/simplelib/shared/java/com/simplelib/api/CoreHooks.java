package com.simplelib.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Hooks for the building cores ({@code simplelib:*_core}, IG31-13) and other small "held show" items: SimpleLib owns the
 * items, a partner mod gives them their behaviour. Without a partner the hooks are empty and the items stay plain.
 * <ul>
 *   <li>{@link #setCore}: right click of a core in the air and on a block (SimpleBuilding: animation, ore transmutation);</li>
 *   <li>{@link #setHeldMotion}: the client hand motion of an item that is "charged" like the cores (Simple Ores' sage orb).</li>
 * </ul>
 * Install in the mod constructor / initializer; the last install wins.
 */
public final class CoreHooks {
    /** Right click of a core. */
    public interface Core {
        InteractionResult use(Level level, Player player, InteractionHand hand);

        InteractionResult useOn(UseOnContext context);
    }

    /** Client hand motion of a held item ({@code kind}: e.g. {@code "orb"}). */
    @FunctionalInterface
    public interface HeldMotion {
        void start(Player player, InteractionHand hand, String kind);
    }

    private static volatile @Nullable Core core;
    private static volatile @Nullable HeldMotion heldMotion;

    private CoreHooks() {}

    public static void setCore(@Nullable Core hook) {
        core = hook;
    }

    public static @Nullable Core core() {
        return core;
    }

    public static void setHeldMotion(@Nullable HeldMotion hook) {
        heldMotion = hook;
    }

    /** Starts the partner's hand motion, if any (call on the client side). */
    public static void startHeldMotion(Player player, InteractionHand hand, String kind) {
        HeldMotion hook = heldMotion;
        if (hook != null) hook.start(player, hand, kind);
    }
}
