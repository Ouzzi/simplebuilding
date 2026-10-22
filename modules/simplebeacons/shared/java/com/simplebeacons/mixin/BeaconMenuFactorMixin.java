package com.simplebeacons.mixin;

import com.simplebeacons.beacon.RangeFactorSlot;
import com.simplebeacons.beacon.RangeFactorView;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SP22-10: one more data slot on every beacon menu (Vanilla and Strong, which extends it) with the server's range
 * factor in percent, so both screens show the factor that really applies ({@link RangeFactorSlot}; 0 = feature off).
 * The client menu (no level access) gets a plain slot the sync fills.
 */
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuFactorMixin extends AbstractContainerMenu implements RangeFactorView {
    @Unique
    private DataSlot simplebeacons$factor;

    private BeaconMenuFactorMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void simplebeacons$addFactorSlot(int containerId, Container inventory, ContainerData data, ContainerLevelAccess access, CallbackInfo ci) {
        this.simplebeacons$factor = this.addDataSlot(access == ContainerLevelAccess.NULL ? DataSlot.standalone() : new RangeFactorSlot(access, data));
    }

    @Override
    public int simplebeacons$rangePercent() {
        return this.simplebeacons$factor == null ? 0 : this.simplebeacons$factor.get();
    }
}
