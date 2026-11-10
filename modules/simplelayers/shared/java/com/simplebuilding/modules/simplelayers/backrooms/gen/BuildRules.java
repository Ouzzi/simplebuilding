package com.simplebuilding.modules.simplelayers.backrooms.gen;

import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import java.util.ArrayList;
import java.util.List;

/**
 * Build-rule checks the core runs for every level (BACK22 core §4.3): the outer layers ly 0 and ly 95 are never passable (the bedrock
 * between levels is never seen), hull sections agree with the pieces. Sampled columns, pure.
 */
public final class BuildRules {
    private BuildRules() {}

    public static List<String> hull(BandLayout layout, long seed, int chunkX, int chunkZ) {
        List<String> out = new ArrayList<>();
        for (int x = 0; x < 16; x += 3) {
            for (int z = 0; z < 16; z += 3) {
                int wx = chunkX * 16 + x, wz = chunkZ * 16 + z;
                if (layout.at(seed, wx, 0, wz).passable()) out.add("ly 0 open at " + wx + " " + wz);
                if (layout.at(seed, wx, BandLayout.HEIGHT - 1, wz).passable()) out.add("ly 95 open at " + wx + " " + wz);
            }
        }
        for (int s = 0; s < BandLayout.HEIGHT / 16; s++) {
            if (!layout.hullSection(seed, chunkX, chunkZ, s)) continue;
            for (int ly = s * 16; ly < s * 16 + 16; ly += 5) {
                BandPiece p = layout.at(seed, chunkX * 16 + 7, ly, chunkZ * 16 + 9);
                if (p != CorePiece.HULL) out.add("hull section " + s + " has " + p + " at ly " + ly);
            }
        }
        return out;
    }
}
