package com.simplebuilding.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Ruestungsstaender (2026-10-09): gesperrte Slots lesen (Ruestung tauschen) und setzen (mittlere/kleine Staender). */
@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Accessor("disabledSlots")
    int simplebuilding$disabledSlots();

    @Accessor("disabledSlots")
    void simplebuilding$setDisabledSlots(int slots);
}
