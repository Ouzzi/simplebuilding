package com.simplebuilding.mixin.client;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.client.render.TieredShulkerRenderState;
import com.simplebuilding.util.RareShulkers;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Die Huelle zeigt die Stufe eines seltenen Shulkers ({@link RareShulkers}): die Textur der Stufen-Shulkerkiste
 * ({@code textures/entity/rare_shulker/<stufe>.png}: Stufen-Shulkerkiste mit Vanillas Kopf).
 */
@Mixin(ShulkerRenderer.class)
public abstract class ShulkerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Shulker;Lnet/minecraft/client/renderer/entity/state/ShulkerRenderState;F)V",
            at = @At("TAIL"))
    private void simplebuilding$tier(Shulker entity, ShulkerRenderState state, float partialTicks, CallbackInfo ci) {
        ((TieredShulkerRenderState) state).simplebuilding$setTier(RareShulkers.tierOf(entity));
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/ShulkerRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void simplebuilding$tierTexture(ShulkerRenderState state, CallbackInfoReturnable<Identifier> cir) {
        ChestTier tier = ((TieredShulkerRenderState) state).simplebuilding$tier();
        if (tier != null) {
            cir.setReturnValue(RareShulkers.textureOf(tier));
        }
    }
}
