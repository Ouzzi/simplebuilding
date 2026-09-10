package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.state.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;

/**
 * Holds what {@code LevelRenderState.blockBreakingRenderStates} contained at the end of the last
 * extraction pass.
 *
 * <p><b>Why this is the observed output.</b> {@code MultiBlockBreakingSupport} draws nothing
 * itself - it appends {@link BlockBreakingRenderState} entries which vanilla then renders as the
 * usual crack overlay. The verifiable output of the mod is therefore the content of that list, and
 * that is what a test reads here.
 *
 * <p><b>Why not a pixel difference.</b> Mining continuously spawns block crumble particles that
 * cannot be switched off, so a difference between two mining screenshots would not be attributable
 * to the mod. This recorder is the substitute, and it is a stronger statement than pixels anyway:
 * it names the exact positions.
 *
 * <p><b>The listener that feeds this is NOT here, and cannot be.</b> The two loaders fire different
 * events - Fabric {@code LevelExtractionEvents.END_EXTRACTION}, NeoForge
 * {@code ExtractLevelRenderStateEvent} at {@code EventPriority.LOWEST} - and both have to run
 * <em>after</em> the mod's own listener to see what it appended. Both manage that, for different
 * reasons: Fabric events keep their registration order and the mod registers during client
 * initialisation, so a listener registered later from the test always runs behind it; NeoForge has
 * no such order and needs the explicit LOWEST priority instead. Neither piece of wiring is
 * expressible in shared code, so each loader's driver registers its own listener and both call
 * {@link #observe(LevelRenderState)}. What is shared is everything after that: the arming, the
 * snapshotting and the two views onto the result.
 *
 * <p><b>Arming exists because the recorder outlives the measurement.</b> The listener is registered
 * once for the whole run and fires on every frame, including the frames of the tests before and
 * after. Only the window between {@link #arm()} and {@link #disarm()} is recorded, so a measurement
 * cannot pick up cracks that another test left on screen.
 */
public final class BreakingStateRecorder {

    private static volatile boolean armed;
    private static volatile List<BlockBreakingRenderState> lastStates = List.of();
    private static volatile List<BlockPos> lastPositions = List.of();

    private BreakingStateRecorder() {
    }

    /**
     * Records one extraction pass. Called from the loader's own render state listener, on the
     * client thread, once per frame - so the {@code armed} check comes before any allocation.
     *
     * <p>The positions are snapshotted with {@code immutable()} rather than kept as handed out:
     * vanilla is free to reuse a {@code MutableBlockPos} inside a render state, and a reused
     * position would silently rewrite an already recorded measurement. Nothing observed so far
     * needs the copy on this Minecraft line, and it is taken anyway because the failure it prevents
     * is invisible: the set would simply agree with whatever the last frame held.
     */
    public static void observe(LevelRenderState renderState) {
        if (!armed) {
            return;
        }

        List<BlockBreakingRenderState> states = new ArrayList<>();
        List<BlockPos> positions = new ArrayList<>();

        for (BlockBreakingRenderState state : renderState.blockBreakingRenderStates) {
            states.add(state);
            positions.add(state.blockPos.immutable());
        }

        lastStates = List.copyOf(states);
        lastPositions = List.copyOf(positions);
    }

    /** Starts recording and throws away whatever an earlier measurement left behind. */
    public static void arm() {
        lastStates = List.of();
        lastPositions = List.of();
        armed = true;
    }

    /**
     * Stops recording. The last recorded pass stays readable on purpose, so a measurement can be
     * evaluated after the mouse button has been released again - releasing it ends the mining and
     * empties the list within a frame or two.
     */
    public static void disarm() {
        armed = false;
    }

    /**
     * The positions of the last recorded pass, as a set.
     *
     * <p>A set, and read once per tick into a growing collection by the caller, because the
     * interesting statement is "this position appeared at all during the measurement window" -
     * a single frame can miss a block that the mod contributes only while the aim is exact.
     */
    public static Set<BlockPos> lastSeen() {
        return new LinkedHashSet<>(lastPositions);
    }

    /**
     * The full content of the last recorded pass, in extraction order.
     *
     * <p>Needed for the one claim the positions alone cannot carry: that every entry of a single
     * frame agrees about the destroy stage, because the mod copies the stage of the block actually
     * being mined onto each extra block. That check has to look at one frame - stages taken from
     * different frames legitimately differ - which is why this returns the last pass rather than
     * the union {@link #lastSeen()} is read into.
     */
    public static List<BlockBreakingRenderState> lastFrame() {
        return lastStates;
    }
}
