package com.simplebuilding.modules.simpleoptrender.mixin.lightmap_cache;

import com.simplebuilding.modules.simpleoptrender.LightmapCache;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O7: skips the light-map upload while its inputs stay within half an 8-bit step of the last upload (LightmapCache). */
@Mixin(Lightmap.class)
abstract class LightmapMixin {
    @Unique private final float[] simpleoptrender$last = new float[LightmapCache.SIZE];
    @Unique private final float[] simpleoptrender$now = new float[LightmapCache.SIZE];
    @Unique private boolean simpleoptrender$uploaded;

    @Inject(method = "render", at = @At("HEAD"))
    private void simpleoptrender$skipUnchanged(LightmapRenderState s, CallbackInfo ci) {
        if (!s.needsUpdate) return;
        float[] n = simpleoptrender$now;
        n[0] = s.skyFactor;
        n[1] = s.blockFactor;
        n[2] = s.nightVisionEffectIntensity;
        n[3] = s.darknessEffectScale;
        n[4] = s.bossOverlayWorldDarkening;
        n[5] = s.brightness;
        simpleoptrender$put(n, 6, s.blockLightTint);
        simpleoptrender$put(n, 9, s.skyLightColor);
        simpleoptrender$put(n, 12, s.ambientColor);
        simpleoptrender$put(n, 15, s.nightVisionColor);
        if (LightmapCache.unchanged(simpleoptrender$last, n, simpleoptrender$uploaded)) s.needsUpdate = false;
        else simpleoptrender$uploaded = true;
    }

    @Unique
    private static void simpleoptrender$put(float[] n, int at, Vector3fc v) {
        n[at] = v.x();
        n[at + 1] = v.y();
        n[at + 2] = v.z();
    }
}
