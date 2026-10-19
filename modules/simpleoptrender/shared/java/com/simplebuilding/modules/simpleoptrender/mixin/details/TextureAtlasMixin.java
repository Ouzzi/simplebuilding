package com.simplebuilding.modules.simpleoptrender.mixin.details;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O9 textureAnimations false: animated sprites (water, lava, fire, portals ...) keep their current frame, no uploads. */
@Mixin(TextureAtlas.class)
abstract class TextureAtlasMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
    private void simpleoptrender$stillTextures(CallbackInfo ci) {
        if (RenderOpt.live(RenderOpt.DETAILS) && !RenderOpt.details().textureAnimations()) ci.cancel();
    }
}
