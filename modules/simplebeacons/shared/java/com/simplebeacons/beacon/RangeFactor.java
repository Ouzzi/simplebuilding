package com.simplebeacons.beacon;

import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.config.BeaconsConfig;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * SP22-10 (plan docs/ai/PLAN-A-BEACONS22-2026-10-22.md): the beacon range grows with the pyramid's material. Every block
 * of the counted (complete) levels has a factor - Netherite 2.0, Diamond 1.5, Gold and Emerald 1.1, Iron and every other
 * base block 1.0 - and the pyramid's factor is their average, in whole percent so the screen shows what applies.
 * Example: four Vanilla levels, the bottom one Diamond = (83 x 100 + 81 x 150) / 164 = 124.7 -> 125 %.
 * Static and side free, so tests call it directly.
 */
public final class RangeFactor {
    /** No change (Iron pyramid, feature off or no pyramid). */
    public static final int NONE = 100;
    /** Percent per base block; missing blocks count {@link #NONE}. */
    public static final Map<Block, Integer> PERCENT = Map.of(
            Blocks.NETHERITE_BLOCK, 200,
            Blocks.DIAMOND_BLOCK, 150,
            Blocks.GOLD_BLOCK, 110,
            Blocks.EMERALD_BLOCK, 110,
            Blocks.IRON_BLOCK, 100);

    public static int percent(Block block) {
        return PERCENT.getOrDefault(block, NONE);
    }

    /** Average percent of the blocks of {@code levels} pyramid levels below {@code beacon} (the scan already found them complete). */
    public static int percent(Level level, BlockPos beacon, int levels) {
        if (levels <= 0) return NONE;
        long sum = 0;
        int count = 0;
        for (int step = 1; step <= levels; step++) {
            int y = beacon.getY() - step;
            for (int x = beacon.getX() - step; x <= beacon.getX() + step; x++) {
                for (int z = beacon.getZ() - step; z <= beacon.getZ() + step; z++) {
                    sum += percent(level.getBlockState(new BlockPos(x, y, z)).getBlock());
                    count++;
                }
            }
        }
        return (int) Math.round(sum / (double) count);
    }

    /** The Vanilla beacon's percent: {@link #NONE} while the module or the switch {@code vanillaBeaconMaterialRange} is off. */
    public static int vanillaPercent(Level level, BlockPos beacon, int levels) {
        return SimpleBeacons.on() && BeaconsConfig.vanillaBeaconMaterialRange ? percent(level, beacon, levels) : NONE;
    }

    /** The Strong Beacon's percent: {@link #NONE} while {@code strongBeaconMaterialRange} is off. */
    public static int strongPercent(Level level, BlockPos beacon, int levels) {
        return BeaconsConfig.strongBeaconMaterialRange ? percent(level, beacon, levels) : NONE;
    }

    public static double apply(double range, int percent) {
        return range * percent / 100.0;
    }

    /** "1.25" (the screens swap the separator for the language's). */
    public static String format(int percent) {
        return String.format(Locale.ROOT, "%d.%02d", percent / 100, percent % 100);
    }

    private RangeFactor() {}
}
