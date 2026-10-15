package com.simplemaps;

import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;

/** Icon choices of the waypoints (owner N65): the pool a new waypoint draws from, and the migration of the old colour. */
public final class WaypointIcons {
    /** Recognisable blocks and items for the random icon of a new waypoint. */
    public static final List<String> POOL = List.of("diamond", "emerald", "gold_ingot", "iron_ingot", "amethyst_shard", "apple",
            "bone", "clock", "spyglass", "lantern", "torch", "campfire", "oak_log", "birch_log", "stone", "cobblestone", "sand",
            "grass_block", "crafting_table", "furnace", "chest", "barrel", "bookshelf", "pumpkin", "melon", "hay_block", "tnt",
            "bell", "anvil", "cake", "beacon", "ender_pearl", "blaze_rod", "nether_star", "heart_of_the_sea", "lodestone",
            "compass", "name_tag", "lapis_block", "redstone_block", "gold_block", "copper_block", "honey_block", "bamboo");
    private static final String[] DYES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /** Vanilla dye text colours, same order as {@link #DYES}. */
    private static final int[] DYE_RGB = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52, 0x9D9D97,
            0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

    private WaypointIcons() {}

    /** Migration: the wool of the dye colour closest to {@code argb}. */
    public static Identifier forColor(int argb) {
        int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF, best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < DYES.length; i++) {
            long dr = r - (DYE_RGB[i] >> 16 & 0xFF), dg = g - (DYE_RGB[i] >> 8 & 0xFF), db = b - (DYE_RGB[i] & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return Identifier.withDefaultNamespace(DYES[best] + "_wool");
    }

    public static Identifier random(Random random) {
        return Identifier.withDefaultNamespace(POOL.get(random.nextInt(POOL.size())));
    }
}
