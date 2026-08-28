package com.simplebuilding.mixin.client;

import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import me.shedaniel.autoconfig.AutoConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.simplebuilding.config.SimplebuildingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
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

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique private float mainHandChiselProgress = 0.0F;
    @Unique private float offHandChiselProgress = 0.0F;

    // MC 1.21.11: Die Methode heisst hier renderArmWithItem (ab 26.2 submitArmWithItem);
    // Parameterliste und der getroffene renderItem-Aufruf sind identisch.
    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
    )
    private void onRenderFirstPersonItem(
            AbstractClientPlayer player,
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
                // SLEDGEHAMMER
                else if (item.getItem() instanceof SledgehammerItem sledgehammerItem) {
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

        float smoothingSpeed = 0.15F;

        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandChiselProgress += (targetProgress - this.mainHandChiselProgress) * smoothingSpeed;
            if (this.mainHandChiselProgress > 0.001F) {
                this.applyChiselTransform(matrices, this.mainHandChiselProgress);
            }
        } else {
            this.offHandChiselProgress += (targetProgress - this.offHandChiselProgress) * smoothingSpeed;
            if (this.offHandChiselProgress > 0.001F) {
                this.applyChiselTransform(matrices, this.offHandChiselProgress);
            }
        }
    }

    @Unique
    private void applyChiselTransform(PoseStack matrices, float progress) {
        matrices.mulPose(Axis.YP.rotationDegrees(-15.0F * progress));
        matrices.mulPose(Axis.XP.rotationDegrees(-10.0F * progress));
        matrices.translate(0.05 * progress, 0.05 * progress, 0.05 * progress);
    }
}