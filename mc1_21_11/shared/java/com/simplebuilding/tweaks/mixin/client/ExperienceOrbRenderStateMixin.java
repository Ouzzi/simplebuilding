package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.client.OrbValueHolder;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExperienceOrbRenderState.class)
public class ExperienceOrbRenderStateMixin implements OrbValueHolder {
    @Unique
    private int simplebuilding$orbValue;

    @Override
    public int simplebuilding$getOrbValue() {
        return simplebuilding$orbValue;
    }

    @Override
    public void simplebuilding$setOrbValue(int value) {
        simplebuilding$orbValue = value;
    }
}
