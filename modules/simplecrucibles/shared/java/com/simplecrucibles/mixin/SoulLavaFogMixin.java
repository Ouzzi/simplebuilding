package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleFluids;
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
 * on lines without soul lava ({@code CrucibleFluids.SOUL_LAVA == null}) nothing changes.
 */
@Mixin(LavaFogEnvironment.class)
public abstract class SoulLavaFogMixin {
    @Unique
    private static final Vector3fc SIMPLECRUCIBLES_SOUL_LAVA_FOG = ARGB.vector3fFromRGB24(0x1EB4C8);

    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void simplecrucibles$soulLavaFog(ClientLevel level, Camera camera, int renderDistance, float partialTicks,
                                            CallbackInfoReturnable<Vector3fc> cir) {
        if (CrucibleFluids.SOUL_LAVA == null) return;
        Fluid fluid = level.getFluidState(camera.blockPosition()).getType();
        if (fluid == CrucibleFluids.SOUL_LAVA || fluid == CrucibleFluids.FLOWING_SOUL_LAVA) {
            cir.setReturnValue(SIMPLECRUCIBLES_SOUL_LAVA_FOG);
        }
    }
}
