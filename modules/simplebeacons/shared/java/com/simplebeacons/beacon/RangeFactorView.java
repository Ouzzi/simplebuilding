package com.simplebeacons.beacon;

/**
 * Added to every {@code BeaconMenu} (Vanilla and Strong) by {@code BeaconMenuFactorMixin}: the server's range percent of
 * the open beacon ({@link RangeFactor}), synced as an extra data slot; 0 = the server has the feature off (no display).
 */
public interface RangeFactorView {
    int simplebeacons$rangePercent();
}
