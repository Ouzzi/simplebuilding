package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.world.item.ItemUseAnimation;
import com.simplebuilding.items.custom.SledgehammerItem;
import me.shedaniel.autoconfig.AutoConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.simplebuilding.config.SimplebuildingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

    @Unique private float mainHandChiselProgress = 0.0F;
    @Unique private float offHandChiselProgress = 0.0F;
    /**
     * Wie weit der Hammer im letzten Frame ausgeholt war (0..1), siehe
     * {@link SledgehammerUpgrades#drawBack}. Nur gespeichert, damit der Client-Test es lesen kann.
     */
    @Unique private float mainHandHammerDrawBack = 0.0F;

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
        boolean animationsEnabled = config.tools.enableToolAnimations && config.tools.enableChiselAnimation;
        float targetProgress = 0.0F;

        if (animationsEnabled) {
            HitResult hit = this.minecraft.hitResult;

            if (hit instanceof BlockHitResult blockHit) {
                // CHISEL
                if (item.getItem() instanceof ChiselItem chiselItem) {
                    // canChisel prüft jetzt GENAU auf Sneaking + Map + Enchantment
                    if (chiselItem.canChisel(this.minecraft.level, blockHit.getBlockPos(), item, player)) {
                        targetProgress = 1.0F;
                    }
                }
                // SLEDGEHAMMER: neigt sich vor einer Maschine, die er mit dem Nugget in der
                // Nebenhand jetzt aufwerten koennte, genau wie vor einem umformbaren Block
                else if (item.getItem() instanceof SledgehammerItem sledgehammerItem) {
                    if (hand == InteractionHand.MAIN_HAND
                            && SledgehammerUpgrades.showsUpgradeHint(this.minecraft.level, blockHit.getBlockPos(), player)) {
                        targetProgress = 1.0F;
                    }
                    net.minecraft.world.phys.Vec3 relativeHit = blockHit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(blockHit.getBlockPos()));
                    if (sledgehammerItem.getTransformationState(
                            this.minecraft.level.getBlockState(blockHit.getBlockPos()),
                            blockHit.getBlockPos(), // FIX: Position übergeben
                            blockHit.getDirection(),
                            relativeHit,
                            (Player)player,
                            item
                    ) != null) {
                        targetProgress = 1.0F;
                    }
                }
            }
        }

        // Waehrend einer Aufwertung holt der Hammer aus, statt sich zu neigen.
        if (hand == InteractionHand.MAIN_HAND && SledgehammerUpgrades.isHammering(player)) {
            targetProgress = 0.0F;
        }

        float smoothingSpeed = 0.15F;

        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandChiselProgress += (targetProgress - this.mainHandChiselProgress) * smoothingSpeed;
            if (this.mainHandChiselProgress > 0.001F) {
                this.applyChiselTransform(matrices, this.mainHandChiselProgress);
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
                this.applyChiselTransform(matrices, this.offHandChiselProgress);
            }
        }
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

    @Unique
    private void applyChiselTransform(PoseStack matrices, float progress) {
        matrices.rotate(Axis.YP.rotationDegrees(-15.0F * progress));
        matrices.rotate(Axis.XP.rotationDegrees(-10.0F * progress));
        matrices.translate(0.05 * progress, 0.05 * progress, 0.05 * progress);
    }
}