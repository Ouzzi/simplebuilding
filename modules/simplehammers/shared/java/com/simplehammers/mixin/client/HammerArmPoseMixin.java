package com.simplehammers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplehammers.engine.SledgehammerUpgrades;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Third person during an in-world upgrade (Queue 738; until then SimpleBuilding's {@code AvatarRendererMixin}): the
 * hammer is held like a hammer, not like a drawn bow. BUNDLE falls through to the ITEM arm pose and the server's arm
 * swing of each strike reads as the blow; while drawing back ({@link SledgehammerUpgrades#isDrawingBack}) TRIDENT lifts
 * the arm over the head. {@code @ModifyExpressionValue}, never {@code @Redirect}; the target is the only
 * {@code getUseAnimation()} call of the method.
 */
@Mixin(AvatarRenderer.class)
public abstract class HammerArmPoseMixin {
    @ModifyExpressionValue(
            method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;"))
    private static ItemUseAnimation simplehammers$hammerPose(ItemUseAnimation original, @Local(argsOnly = true) Avatar avatar,
                                                            @Local(argsOnly = true) InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SledgehammerUpgrades.isHammering(avatar)) return original;
        return SledgehammerUpgrades.isDrawingBack(avatar, 0.0F) ? ItemUseAnimation.TRIDENT : ItemUseAnimation.BUNDLE;
    }
}
