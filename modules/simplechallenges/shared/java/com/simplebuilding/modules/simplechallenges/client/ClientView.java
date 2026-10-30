package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.core.View;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.HashMap;
import java.util.Map;

/** The free cells the server told this client about (same collision logic as the server, so walking against a border feels like a wall). */
public final class ClientView implements View {
    private final Map<String, UnlockGrid> grids = new HashMap<>();
    private volatile boolean exemptCreative = true;

    @Override
    public UnlockGrid grid(String dimension) {
        return grids.get(dimension);
    }

    @Override
    public boolean exemptCreative() {
        return exemptCreative;
    }

    /** Applies one sync packet (full replaces the dimension's cells, else adds). */
    public synchronized void apply(String dimension, int cell, boolean full, boolean exempt, long[] keys) {
        exemptCreative = exempt;
        UnlockGrid g = grids.get(dimension);
        if (full || g == null || g.cell() != cell) {
            g = new UnlockGrid(cell);
            grids.put(dimension, g);
        }
        for (long k : keys) g.unlock(UnlockGrid.keyX(k), UnlockGrid.keyZ(k));
    }
}
