package com.simplebuilding.mixin.forge;

import com.simplebuilding.client.render.MultiBlockBreakingSupport;
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
 * Forge-Gegenstueck zu NeoForges {@code ExtractLevelRenderStateEvent} und Fabrics
 * {@code LevelRenderEvents.END_EXTRACTION}: Abbau-Risse auf allen Bloecken, die mit dem gerade
 * abgebauten fallen (Vorschlaghammer, Tunnelgraeber, Aderabbau). Direkt nach Vanillas eigener
 * Riss-Extraktion, die die Liste vorher leert - frueher eingehaengt wuerden die Zusatzrisse
 * wieder geloescht.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Inject(method = "extractBlockDestroyAnimation", at = @At("TAIL"))
    private void simplebuilding$extractExtraBreakingStates(Camera camera, LevelRenderState levelRenderState,
                                                           CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            MultiBlockBreakingSupport.extractExtraBreakingStates(levelRenderState, level);
        }
    }
}
