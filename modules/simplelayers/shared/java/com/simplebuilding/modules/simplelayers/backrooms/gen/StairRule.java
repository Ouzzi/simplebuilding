package com.simplebuilding.modules.simplelayers.backrooms.gen;

import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import java.util.ArrayList;
import java.util.List;

/**
 * The stair rule of all levels (STAIRS22 generalised, BACK22 core §4.3 + K-R9.2): a flight has at most {@link #MAX_STEPS} steps before
 * a landing, every step stands on something solid (base), above every step {@link #HEADROOM} blocks are free (2 for the head + 1 for
 * the diagonal into the next step), a ladder needs {@link #LADDER_EXIT} free blocks at its exit. Checks the pieces of a layout; the
 * walking test with real collision shapes ({@code stairwells_walkable}) stays the final proof.
 */
public final class StairRule {
    public static final int MAX_STEPS = 8, HEADROOM = 3, LADDER_EXIT = 3;

    private StairRule() {}

    /** Problems of {@code run} in {@code layout} (empty = fine). */
    public static List<String> check(BandLayout layout, long seed, StairRun run) {
        List<String> out = new ArrayList<>();
        if (!run.ladder() && run.steps() > MAX_STEPS) out.add("flight of " + run.steps() + " > " + MAX_STEPS + " without landing");
        for (int i = 0; i < run.steps(); i++) {
            int x = run.ladder() ? run.x() : run.x() + DoorEnd.DX[run.dir()] * i, z = run.ladder() ? run.z() : run.z() + DoorEnd.DZ[run.dir()] * i;
            int ly = run.ly() + i;
            if (!run.ladder()) {
                BandPiece base = layout.at(seed, x, ly - 1, z);
                if (base.passable()) out.add("step " + i + " without base at " + x + " " + (ly - 1) + " " + z);
                for (int h = 1; h <= HEADROOM; h++) {
                    if (!layout.at(seed, x, ly + h, z).passable()) out.add("step " + i + " headroom " + h + " blocked");
                }
            }
        }
        if (run.ladder()) {
            int top = run.ly() + run.steps();
            for (int h = 0; h < LADDER_EXIT; h++) {
                if (!layout.at(seed, run.x(), top + h, run.z()).passable()) out.add("ladder exit " + h + " blocked");
            }
        }
        return out;
    }
}
