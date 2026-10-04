package com.simplelib.api;

import com.simplelib.crucible.HeatLevel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The only classes a bundling mod may import from SimpleLib (principle 6a). SimpleLib never names
 * its users; a partner registers what it adds here.
 */
public final class SimpleLibApi {
    /** Heat of a block that tags cannot describe (e.g. a cauldron filled with soul lava). */
    @FunctionalInterface
    public interface HeatSource {
        HeatLevel heat(Level level, BlockPos pos, BlockState state);
    }

    private static final List<HeatSource> HEAT_SOURCES = new CopyOnWriteArrayList<>();
    private static volatile boolean axeWaysDisabled;

    public static void registerHeatSource(HeatSource source) {
        HEAT_SOURCES.add(source);
    }

    public static HeatLevel heatOf(Level level, BlockPos pos, BlockState state) {
        HeatLevel best = HeatLevel.NONE;
        for (HeatSource source : HEAT_SOURCES) {
            HeatLevel heat = source.heat(level, pos, state);
            if (heat != null && heat.ordinal() > best.ordinal()) best = heat;
        }
        return best;
    }

    /**
     * Principle 5a: a partner with a working mod way (SimpleBuilding's sledgehammer) switches the
     * Vanilla axe ways off.
     */
    public static void disableAxeWays() {
        axeWaysDisabled = true;
    }

    public static boolean axeWaysEnabled() {
        return !axeWaysDisabled;
    }

    /** Upgrades a crucible in place to {@code to}, keeping contents, progress and experience (for a partner's own way). */
    public static void upgradeCrucible(net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block to) {
        com.simplelib.crucible.CrucibleUpgrades.upgradeInPlace(level, pos, to);
    }

    private SimpleLibApi() {}
}
