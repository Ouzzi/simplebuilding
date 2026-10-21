package com.simplestorage.mixin.client;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.client.TieredShulkerRenderState;
import com.simplestorage.rare.RareShulkers;
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
 * The hull shows the tier of a rare shulker ({@link RareShulkers}): the texture of the tier shulker box
 * ({@code textures/entity/rare_shulker/<tier>.png}: tier shulker box with Vanilla's head).
 */
@Mixin(ShulkerRenderer.class)
public abstract class ShulkerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Shulker;Lnet/minecraft/client/renderer/entity/state/ShulkerRenderState;F)V",
            at = @At("TAIL"))
    private void simplestorage$tier(Shulker entity, ShulkerRenderState state, float partialTicks, CallbackInfo ci) {
        ((TieredShulkerRenderState) state).simplestorage$setTier(RareShulkers.tierOf(entity));
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/ShulkerRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void simplestorage$tierTexture(ShulkerRenderState state, CallbackInfoReturnable<Identifier> cir) {
        ChestTier tier = ((TieredShulkerRenderState) state).simplestorage$tier();
        if (tier != null) {
            cir.setReturnValue(RareShulkers.textureOf(tier));
        }
    }
}
