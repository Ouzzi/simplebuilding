package com.simplegadgets.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplegadgets.rod.ResonanceRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Resonance Rod tilts forward while it beams (owner 2026-09-29), independent of any tool animation setting - it stands still
 * then instead of wobbling. Moved from SimpleBuilding's {@code HeldItemRendererMixin} (OW24-2 G2): same injection point (just before
 * the item is submitted), eased per hand over a few frames.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class RodHandTiltMixin {
    @Shadow @Final private Minecraft minecraft;

    /** Tilt of the rod while beaming (0..1), per hand. */
    @Unique private float simplegadgets$mainHandRodTilt = 0.0F;
    @Unique private float simplegadgets$offHandRodTilt = 0.0F;

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void simplegadgets$tiltRod(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float tickProgress, float pitch,
                                       InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
                                       SubmitNodeCollector queue, int light, CallbackInfo ci) {
        AbstractClientPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        boolean beaming = item.getItem() instanceof ResonanceRodItem && player.isUsingItem() && player.getUsedItemHand() == hand;
        float target = beaming ? 1.0F : 0.0F;
        float tilt;
        if (hand == InteractionHand.MAIN_HAND) {
            this.simplegadgets$mainHandRodTilt += (target - this.simplegadgets$mainHandRodTilt) * 0.25F;
            tilt = this.simplegadgets$mainHandRodTilt;
        } else {
            this.simplegadgets$offHandRodTilt += (target - this.simplegadgets$offHandRodTilt) * 0.25F;
            tilt = this.simplegadgets$offHandRodTilt;
        }
        if (tilt > 0.001F) {
            // Tip forward to the target, pushed slightly forward and down.
            matrices.translate(0.0, -0.04 * tilt, -0.06 * tilt);
            matrices.rotate(Axis.XP.rotationDegrees(-60.0F * tilt));
        }
    }
}
