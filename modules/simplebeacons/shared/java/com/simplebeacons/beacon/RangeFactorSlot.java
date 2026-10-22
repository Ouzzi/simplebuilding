package com.simplebeacons.beacon;

import com.simplebeacons.config.BeaconsConfig;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * Server side of the beacon menus' range factor slot ({@link RangeFactorView}): reads the open beacon - Strong: its last
 * pyramid scan, Vanilla: computed from the menu's level count, cached for 20 ticks. 0 = the feature is off on this server.
 */
public final class RangeFactorSlot extends DataSlot {
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private long checked = -1;
    private int value;

    public RangeFactorSlot(ContainerLevelAccess access, ContainerData data) {
        this.access = access;
        this.data = data;
    }

    @Override
    public int get() {
        return this.access.evaluate((level, pos) -> {
            var be = level.getBlockEntity(pos);
            if (be instanceof StrongBeaconBlockEntity strong) return BeaconsConfig.strongBeaconMaterialRange ? strong.rangePercent() : 0;
            if (!(be instanceof BeaconBlockEntity) || !BeaconsConfig.vanillaBeaconMaterialRange) return 0;
            if (this.checked < 0 || level.getGameTime() - this.checked >= 20) {
                this.checked = level.getGameTime();
                this.value = RangeFactor.vanillaPercent(level, pos, this.data.get(0));
            }
            return this.value;
        }, 0);
    }

    @Override
    public void set(int value) {
    }
}
