package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * N46/N47-M2 (b-circ46, 14.11.2026): the Astralit/Nihilit line (dust, lamps, pistons, rails) lives in Simple Ores
 * ({@code com.simpleores.line}); the core must not import a sub-mod (AGENTS rule 10). What the core still asks about the
 * line (test centre, tests) goes through the {@code ModBlocks} fields (Simple Ores' blocks on 26.3, null on 26.2) or by
 * reflection here.
 */
public final class OresLine {
    private OresLine() {}

    /** Line name of a dust state - "redstone", "astral", "nihil", "bridge" - or null for anything else (SignalDustBlock#lineOf). */
    public static String lineOf(BlockState state) {
        if (state.is(Blocks.REDSTONE_WIRE)) return "redstone";
        if (ModBlocks.ASTRAL_REDSTONE != null && state.is(ModBlocks.ASTRAL_REDSTONE)) return "astral";
        if (ModBlocks.NIHIL_REDSTONE != null && state.is(ModBlocks.NIHIL_REDSTONE)) return "nihil";
        if (ModBlocks.BRIDGE_REDSTONE != null && state.is(ModBlocks.BRIDGE_REDSTONE)) return "bridge";
        return null;
    }

    /** Top speed of Astral rails in blocks per second (Simple Ores' {@code EndRailPhysics#maxSpeedPerTick} x 20; 16 when unknown). */
    public static int railTopSpeed() {
        Object perTick = call("com.simpleores.line.EndRailPhysics", "maxSpeedPerTick");
        return perTick instanceof Double d ? (int) Math.round(d * 20) : 16;
    }

    /** A public static no-argument method of a Simple Ores class, or null when the class or method is missing. */
    public static Object call(String className, String method) {
        try {
            return Class.forName(className).getMethod(method).invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
