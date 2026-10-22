package com.simplehammers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplehammers.client.HammerVisuals;
import com.simplehammers.engine.SledgehammerUpgrades;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person during an in-world upgrade (Queue 738; until then SimpleBuilding's {@code HeldItemRendererMixin}): between
 * two strikes the hammer draws back like a bow, shortly before the strike it rushes forward onto the block; the strike
 * itself is the arm swing the server sends. The hand shows the BUNDLE pose instead of the drawn bow (BOW), whose branch
 * alone applies the swing while using - every strike becomes visible in the own hand. {@code @ModifyExpressionValue},
 * never {@code @Redirect}.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class HammerHandMixin {
    @Shadow @Final private Minecraft minecraft;

    /**
     * How far the hammer was drawn back in the last frame (0..1, {@link SledgehammerUpgrades#drawBack}); kept for the
     * client test, which reads it by name.
     */
    @Unique private float mainHandHammerDrawBack = 0.0F;

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void simplehammers$drawBack(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float tickProgress,
                                        float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                                        PoseStack matrices, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        AbstractClientPlayer player = this.minecraft.player;
        if (player == null || hand != InteractionHand.MAIN_HAND) return;
        float phase = HammerVisuals.animations() ? SledgehammerUpgrades.blowPhase(player, tickProgress) : -1.0F;
        float drawBack = SledgehammerUpgrades.drawBack(phase);
        float followThrough = phase >= SledgehammerUpgrades.STRIKE_PHASE
                ? (float) Math.sin(Math.PI * (phase - SledgehammerUpgrades.STRIKE_PHASE) / (1.0F - SledgehammerUpgrades.STRIKE_PHASE))
                : 0.0F;
        this.mainHandHammerDrawBack = drawBack;
        if (drawBack > 0.001F || followThrough > 0.001F) {
            // Raised and tipped back with the head to the shoulder (so far that it stays in view); in the strike it tips
            // past the rest position forward onto the block.
            matrices.translate(0.0, 0.2 * drawBack - 0.06 * followThrough, 0.06 * drawBack - 0.08 * followThrough);
            matrices.rotate(Axis.XP.rotationDegrees(28.0F * drawBack - 22.0F * followThrough));
        }
    }

    @ModifyExpressionValue(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;"))
    private ItemUseAnimation simplehammers$hammerBlows(ItemUseAnimation original, @Local(argsOnly = true) InteractionHand hand) {
        AbstractClientPlayer player = this.minecraft.player;
        return hand == InteractionHand.MAIN_HAND && player != null && SledgehammerUpgrades.isHammering(player)
                ? ItemUseAnimation.BUNDLE : original;
    }
}
