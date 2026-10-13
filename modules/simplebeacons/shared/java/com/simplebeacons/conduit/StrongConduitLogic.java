package com.simplebeacons.conduit;

import com.simplebeacons.config.BeaconsConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The rules of the Strong Conduit (concept §4): Vanilla's 5x5x5 frame (42 blocks) or the 7x7x7 frame (three 7x7
 * rings, 66 blocks), active from 16 frame blocks, range {@code (blocks / 7) x 16} (66 = 144, capped by
 * {@code strongConduitMaxRange}), Conduit Power plus Dolphin's Grace for players in the water; beyond Vanilla's 96
 * blocks only for players really under water (not rain). Static and side free, so tests call it directly.
 */
public final class StrongConduitLogic {
    public static final int MIN_ACTIVE = 16;
    public static final int FULL_SMALL = 42, FULL_LARGE = 66;
    public static final int VANILLA_MAX_RANGE = 96;
    public static final int EFFECT_TICKS = 260;

    /** The frame positions of a shell with radius {@code r}: one coordinate 0, one with magnitude r (r=2: 42, r=3: 66). */
    public static List<BlockPos> shell(BlockPos center, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int ox = -r; ox <= r; ox++) {
            for (int oy = -r; oy <= r; oy++) {
                for (int oz = -r; oz <= r; oz++) {
                    int ax = Math.abs(ox), ay = Math.abs(oy), az = Math.abs(oz);
                    if (ox == 0 && (ay == r || az == r) || oy == 0 && (ax == r || az == r) || oz == 0 && (ax == r || ay == r)) {
                        out.add(center.offset(ox, oy, oz));
                    }
                }
            }
        }
        return out;
    }

    /** Whether the 3x3x3 around the conduit is water (Vanilla). */
    public static boolean inWater(Level level, BlockPos center) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (!level.isWaterAt(center.offset(ox, oy, oz))) return false;
                }
            }
        }
        return true;
    }

    /** Frame blocks of the larger complete-enough shell into {@code out}; the radius used (2, 3) or 0 when inactive. */
    public static int scan(Level level, BlockPos center, List<BlockPos> out) {
        out.clear();
        if (!inWater(level, center)) return 0;
        List<BlockPos> large = frame(level, shell(center, 3));
        List<BlockPos> small = frame(level, shell(center, 2));
        List<BlockPos> best = large.size() >= small.size() ? large : small;
        if (best.size() < MIN_ACTIVE) return 0;
        out.addAll(best);
        return best == large ? 3 : 2;
    }

    private static List<BlockPos> frame(Level level, List<BlockPos> shell) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : shell) {
            if (level.getBlockState(pos).is(BlockTags.CONDUIT_EFFECT_BLOCK)) found.add(pos);
        }
        return found;
    }

    /** Range for {@code frameBlocks} frame blocks: Vanilla's formula, capped by the server config. */
    public static int range(int frameBlocks) {
        return Math.min(frameBlocks / 7 * 16, BeaconsConfig.strongConduitMaxRange);
    }

    /** Attack radius: 8 with the full 5x5x5 frame, 12 with the full 7x7x7 frame, else no attack (0). */
    public static int killRange(int frameBlocks, int radius) {
        if (radius == 3 && frameBlocks >= FULL_LARGE) return 12;
        return frameBlocks >= FULL_SMALL ? 8 : 0;
    }

    /** Effects for players in range; returns the players that got them. */
    public static List<Player> applyEffects(Level level, BlockPos pos, int frameBlocks) {
        int range = range(frameBlocks);
        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> reached = new ArrayList<>();
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (!pos.closerThan(player.blockPosition(), range)) continue;
            boolean far = !pos.closerThan(player.blockPosition(), VANILLA_MAX_RANGE);
            boolean wet = far ? player.isInWater() : player.isInWaterOrRain();
            if (!wet) continue;
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 0, true, true));
            if (player.isInWater()) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_TICKS, 0, true, true));
            reached.add(player);
        }
        return reached;
    }

    private StrongConduitLogic() {}
}
