package com.simplebuilding.mixin.client;

import com.simplebuilding.fluid.ModFluids;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soul lava is in the {@code minecraft:lava} fluid tag, so the camera gets Vanilla's lava fog; inside soul lava the
 * fog turns turquoise instead of orange (crucible plan, open point "Nebel"). Common client code for every loader;
 * on lines without soul lava ({@code ModFluids.SOUL_LAVA == null}) nothing changes.
 */
@Mixin(LavaFogEnvironment.class)
public abstract class SoulLavaFogMixin {
    @Unique
    private static final Vector3fc SIMPLEBUILDING_SOUL_LAVA_FOG = ARGB.vector3fFromRGB24(0x1EB4C8);

    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$soulLavaFog(ClientLevel level, Camera camera, int renderDistance, float partialTicks,
                                            CallbackInfoReturnable<Vector3fc> cir) {
        if (ModFluids.SOUL_LAVA == null) return;
        Fluid fluid = level.getFluidState(camera.blockPosition()).getType();
        if (fluid == ModFluids.SOUL_LAVA || fluid == ModFluids.FLOWING_SOUL_LAVA) {
            cir.setReturnValue(SIMPLEBUILDING_SOUL_LAVA_FOG);
        }
    }
}
