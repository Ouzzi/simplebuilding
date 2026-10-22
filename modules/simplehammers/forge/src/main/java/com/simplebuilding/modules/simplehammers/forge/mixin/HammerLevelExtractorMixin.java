package com.simplebuilding.modules.simplehammers.forge.mixin;

import com.simplehammers.client.HammerAreaClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge counterpart of NeoForge's {@code ExtractLevelRenderStateEvent} / Fabric's {@code LevelRenderEvents.END_EXTRACTION}:
 * the extra breaking cracks of the hammer's area, right after vanilla's own crack extraction, which clears the list first.
 */
@Mixin(LevelExtractor.class)
public abstract class HammerLevelExtractorMixin {
    @Inject(method = "extractBlockDestroyAnimation", at = @At("TAIL"))
    private void simplehammers$extractAreaCracks(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) HammerAreaClient.extractCracks(levelRenderState, level);
    }
}
