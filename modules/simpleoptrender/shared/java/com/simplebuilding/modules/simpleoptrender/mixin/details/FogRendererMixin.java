package com.simplebuilding.modules.simpleoptrender.mixin.details;

import com.simplebuilding.modules.simpleoptrender.Details;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O9 fog false: the render-distance fog is lifted everywhere, the atmospheric fog only in dimensions with sky light and no ceiling (Overworld); fluid, snow, blindness, darkness, Nether and End fog stay (Details#liftFog). */
@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void simpleoptrender$noAirFog(Camera camera, int renderDistance, DeltaTracker delta, float darken, ClientLevel level,
                                          CallbackInfoReturnable<FogData> cir) {
        if (!RenderOpt.live(RenderOpt.DETAILS)) return;
        boolean blind = camera.entity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
        if (!Details.liftFog(RenderOpt.details(), camera.getFluidInCamera() != FogType.NONE, blind)) return;
        FogData fog = cir.getReturnValue();
        if (Details.liftEnvironmentalFog(level.dimensionType().hasSkyLight(), level.dimensionType().hasCeiling()))
            fog.environmentalStart = fog.environmentalEnd = Details.NO_FOG;
        fog.renderDistanceStart = fog.renderDistanceEnd = Details.NO_FOG;
    }
}
