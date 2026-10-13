package com.simplebeacons.mixin.client;

import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Strong Beacon screen's extra buttons set the same choice Vanilla's confirm button sends. */
@Mixin(BeaconScreen.class)
public interface BeaconScreenAccessor {
    @Accessor("primary")
    Holder<MobEffect> simplebeacons$primary();

    @Accessor("primary")
    void simplebeacons$setPrimary(Holder<MobEffect> effect);

    @Accessor("secondary")
    Holder<MobEffect> simplebeacons$secondary();

    @Accessor("secondary")
    void simplebeacons$setSecondary(Holder<MobEffect> effect);
}
