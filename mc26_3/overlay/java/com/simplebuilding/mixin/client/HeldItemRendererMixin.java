package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.world.item.ItemUseAnimation;
import me.shedaniel.autoconfig.AutoConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * MC 26.3 twin of common/src/mc26_2/java/.../HeldItemRendererMixin.java. 26.3 renamed
 * ItemInHandRenderer to FirstPersonHandsAndItemsRenderer and made submitArmWithItem work on render
 * states: no player parameter (first person is always the local player, taken from Minecraft), and
 * the item is submitted through ItemStackRenderState#submit instead of renderItem. Same behaviour
 * and the same field names (the client tests read mainHandChiselProgress by reflection).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class HeldItemRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique private int hintTick = -1;
    @Unique private BlockHitResult hintHit;
    @Unique private boolean mainHint;
    @Unique private boolean offHint;
    @Unique private boolean mainPartial;
    @Unique private boolean offPartial;
    /** Teil-Hinweis halb so stark; der Fortschritt selbst bleibt 0..1 (die Client-Tests lesen ihn). */
    @Unique private float mainScale = 1.0F;
    @Unique private float offScale = 1.0F;
    @Unique private float mainHandChiselProgress = 0.0F;
    @Unique private float offHandChiselProgress = 0.0F;
    /**
     * Compatibility alias for the previous nugget hint; client tests read this field by reflection.
     * All items now share the per-hand progress and a single mirrored tilt/bob path.
     */
    @Unique private float offHandNuggetProgress = 0.0F;
    /**
     * Wie weit der Hammer im letzten Frame ausgeholt war (0..1), siehe
     * {@link SledgehammerUpgrades#drawBack}. Nur gespeichert, damit der Client-Test es lesen kann.
     */
    @Unique private float mainHandHammerDrawBack = 0.0F;
    /** Neigung des Resonanzstabs beim Strahlen (0..1), je Hand. */
    @Unique private float mainHandRodTilt = 0.0F;
    @Unique private float offHandRodTilt = 0.0F;

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void onRenderFirstPersonItem(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        AbstractClientPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
        // Baukern: gewuerfelte Bewegung beim Benutzen bzw. die lange Erz-Animation (CoreHandMotion, Nachtrag 11).
        if (item.getItem() instanceof com.simplebuilding.items.custom.BuildingCoreItem
                || item.getItem() instanceof com.simplebuilding.items.custom.SageOrbItem) { // N21: Weisheitskugel
            float[] pose = com.simplebuilding.items.custom.CoreHandMotion.currentPose(player.tickCount, tickProgress, hand);
            if (pose != null && config.tools.enableToolAnimations && config.tools.enableCoreAnimations) {
                this.applyCorePose(matrices, pose, hand);
            }
        }
        boolean animationsEnabled = config.tools.enableToolAnimations && config.tools.enableChiselAnimation;
        float targetProgress = 0.0F;

        if (animationsEnabled && this.minecraft.hitResult instanceof BlockHitResult blockHit) {
            if (hintTick != player.tickCount || hintHit == null || !hintHit.getBlockPos().equals(blockHit.getBlockPos())
                    || hintHit.getDirection() != blockHit.getDirection()
                    || com.simplebuilding.util.HammerCorners.corner(hintHit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(hintHit.getBlockPos())))
                       != com.simplebuilding.util.HammerCorners.corner(blockHit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(blockHit.getBlockPos())))
                    || (hintHit.getLocation().y - hintHit.getBlockPos().getY() < 0.5)
                       != (blockHit.getLocation().y - blockHit.getBlockPos().getY() < 0.5)) {
                hintTick = player.tickCount;
                hintHit = blockHit;
                mainHint = com.simplebuilding.util.TransformTargets.canTransformTarget(this.minecraft.level, blockHit, player, InteractionHand.MAIN_HAND);
                offHint = com.simplebuilding.util.TransformTargets.canTransformTarget(this.minecraft.level, blockHit, player, InteractionHand.OFF_HAND);
                mainPartial = !mainHint && com.simplebuilding.util.TransformTargets.partialTransformTarget(this.minecraft.level, blockHit, player, InteractionHand.MAIN_HAND);
                offPartial = !offHint && com.simplebuilding.util.TransformTargets.partialTransformTarget(this.minecraft.level, blockHit, player, InteractionHand.OFF_HAND);
            }
            boolean partial = hand == InteractionHand.MAIN_HAND ? mainPartial : offPartial;
            targetProgress = (hand == InteractionHand.MAIN_HAND ? mainHint : offHint) || partial ? 1.0F : 0.0F;
            if (hand == InteractionHand.MAIN_HAND) {
                mainScale = partial ? 0.5F : 1.0F;
            } else {
                offScale = partial ? 0.5F : 1.0F;
            }
        }

        // Waehrend einer Aufwertung holt der Hammer aus, statt sich zu neigen.
        if (hand == InteractionHand.MAIN_HAND && SledgehammerUpgrades.isHammering(player)) {
            targetProgress = 0.0F;
        }

        float smoothingSpeed = 0.15F;

        // Resonanzstab: beim Strahlen nach vorn geneigt (Besitzer 2026-09-29), unabhaengig von der
        // Werkzeug-Animations-Einstellung - er steht dann still, statt zu wackeln.
        boolean beaming = item.getItem() instanceof com.simplebuilding.tweaks.item.LaserPointerItem
                && player.isUsingItem() && player.getUsedItemHand() == hand;
        float rodTarget = beaming ? 1.0F : 0.0F;
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandRodTilt += (rodTarget - this.mainHandRodTilt) * 0.25F;
            if (this.mainHandRodTilt > 0.001F) {
                this.applyRodTilt(matrices, this.mainHandRodTilt);
            }
        } else {
            this.offHandRodTilt += (rodTarget - this.offHandRodTilt) * 0.25F;
            if (this.offHandRodTilt > 0.001F) {
                this.applyRodTilt(matrices, this.offHandRodTilt);
            }
        }

        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandChiselProgress += (targetProgress - this.mainHandChiselProgress) * smoothingSpeed;
            if (this.mainHandChiselProgress > 0.001F) {
                this.applyTransformHint(matrices, this.mainHandChiselProgress * this.mainScale * config.tools.transformHintStrength / 100.0F,
                        player.tickCount + tickProgress, hand);
            }
            // Aufwertung: zwischen zwei Schlaegen wie ein Bogen ausholen, kurz vor dem Schlag nach
            // vorn sausen; den Schlag selbst zeigt der Armschwung, den der Server schickt.
            float phase = config.tools.enableToolAnimations ? SledgehammerUpgrades.blowPhase(player, tickProgress) : -1.0F;
            float drawBack = SledgehammerUpgrades.drawBack(phase);
            float followThrough = phase >= SledgehammerUpgrades.STRIKE_PHASE
                    ? (float) Math.sin(Math.PI * (phase - SledgehammerUpgrades.STRIKE_PHASE) / (1.0F - SledgehammerUpgrades.STRIKE_PHASE))
                    : 0.0F;
            this.mainHandHammerDrawBack = drawBack;
            if (drawBack > 0.001F || followThrough > 0.001F) {
                this.applyHammerDrawBack(matrices, drawBack, followThrough);
            }
        } else {
            this.offHandChiselProgress += (targetProgress - this.offHandChiselProgress) * smoothingSpeed;
            if (this.offHandChiselProgress > 0.001F) {
                this.applyTransformHint(matrices, this.offHandChiselProgress * this.offScale * config.tools.transformHintStrength / 100.0F,
                        player.tickCount + tickProgress, hand);
            }
            this.offHandNuggetProgress = SledgehammerUpgrades.isUpgradeNugget(item) ? this.offHandChiselProgress : 0.0F; // compatibility with existing client tests
        }
    }

    /**
     * The same tilt and bob for every valid transformation item, mirrored toward the center per hand.
     */
    @Unique
    private void applyTransformHint(PoseStack matrices, float progress, float time, InteractionHand hand) {
        float sign = hand == InteractionHand.OFF_HAND ? 1.0F : -1.0F;
        float wobble = (float) Math.sin(time * 0.35F) * 4.0F * progress;
        matrices.translate(sign * 0.06 * progress, 0.08 * progress, -0.04 * progress);
        matrices.rotate(Axis.YP.rotationDegrees(sign * 18.0F * progress));
        matrices.rotate(Axis.XP.rotationDegrees(-22.0F * progress));
        matrices.rotate(Axis.ZP.rotationDegrees(sign * (12.0F * progress + wobble)));
    }

    /**
     * Erste Person: waehrend einer Aufwertung mit dem Vorschlaghammer zeigt die Hand statt der
     * gespannten Bogen-Pose (BOW) die Bundle-Pose - und nur deren Zweig wendet im Benutzen den
     * Armschwung an. Jeder Hammerschlag, dessen Schwung der Server auch an den Spieler selbst
     * schickt, wird so in der eigenen Hand sichtbar. Unabhaengig von der Animations-Einstellung,
     * damit die Bogen-Pose nie erscheint. {@code @ModifyExpressionValue}, nie {@code @Redirect}.
     */
    @ModifyExpressionValue(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;"))
    private ItemUseAnimation simplebuilding$hammerBlows(ItemUseAnimation original,
                                                        @Local(argsOnly = true) InteractionHand hand) {
        AbstractClientPlayer player = this.minecraft.player;
        return hand == InteractionHand.MAIN_HAND && player != null && SledgehammerUpgrades.isHammering(player)
                ? ItemUseAnimation.BUNDLE : original;
    }

    /**
     * Hammer ausholen: angehoben und mit dem Kopf zur Schulter zurueckgekippt (so weit, dass er im
     * Bild bleibt); im Schlag kippt er ueber die Ruhelage hinaus nach vorn auf die Maschine.
     */
    @Unique
    private void applyHammerDrawBack(PoseStack matrices, float drawBack, float followThrough) {
        matrices.translate(0.0, 0.2 * drawBack - 0.06 * followThrough, 0.06 * drawBack - 0.08 * followThrough);
        matrices.rotate(Axis.XP.rotationDegrees(28.0F * drawBack - 22.0F * followThrough));
    }

    /**
     * Baukern-Pose aus {@link com.simplebuilding.items.custom.CoreHandMotion#pose}: verschieben, dann um die Item-Mitte
     * drehen und skalieren (die Modell-Transformation danach zentriert das Item um den Ursprung). Die Nebenhand
     * spiegelt die Seitenbewegung und die Drehrichtung.
     */
    @Unique
    private void applyCorePose(PoseStack matrices, float[] pose, InteractionHand hand) {
        float mirror = hand == InteractionHand.OFF_HAND ? -1.0F : 1.0F;
        matrices.translate(mirror * pose[com.simplebuilding.items.custom.CoreHandMotion.TX],
                pose[com.simplebuilding.items.custom.CoreHandMotion.TY], pose[com.simplebuilding.items.custom.CoreHandMotion.TZ]);
        matrices.rotate(Axis.YP.rotationDegrees(mirror * pose[com.simplebuilding.items.custom.CoreHandMotion.RY]));
        matrices.rotate(Axis.XP.rotationDegrees(pose[com.simplebuilding.items.custom.CoreHandMotion.RX]));
        matrices.rotate(Axis.ZP.rotationDegrees(mirror * pose[com.simplebuilding.items.custom.CoreHandMotion.RZ]));
        float scale = pose[com.simplebuilding.items.custom.CoreHandMotion.SCALE];
        matrices.scale(scale, scale, scale);
    }

    /** Resonanzstab: Spitze nach vorn zum Ziel gekippt, leicht vor und nach unten geschoben. */
    @Unique
    private void applyRodTilt(PoseStack matrices, float progress) {
        matrices.translate(0.0, -0.04 * progress, -0.06 * progress);
        matrices.rotate(Axis.XP.rotationDegrees(-60.0F * progress));
    }

}
