package com.simplebuilding.modules.simpleoptmemory.mixin.state_tables;

import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
import com.simplebuilding.modules.simpleoptmemory.StateTables;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O4: a new state shares its key and value arrays with an identical earlier one (StateTables; the arrays are read-only). */
@Mixin(StateHolder.class)
abstract class StateHolderMixin {
    @Shadow @Final @Mutable private Property<?>[] propertyKeys;
    @Shadow @Final @Mutable private Comparable<?>[] propertyValues;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simpleoptmemory$share(Object owner, Property<?>[] keys, Comparable<?>[] values, CallbackInfo ci) {
        if (!MemoryOpt.live(MemoryOpt.STATE_TABLES)) return;
        propertyKeys = StateTables.share(propertyKeys);
        propertyValues = StateTables.share(propertyValues);
    }
}
