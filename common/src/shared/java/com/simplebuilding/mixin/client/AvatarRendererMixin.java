package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dritte Person: Wer mit dem Vorschlaghammer eine Maschine aufwertet, haelt den Hammer wie einen
 * Hammer, nicht wie einen gespannten Bogen. Die Benutzungsanimation BOW wuerde die Arm-Pose
 * BOW_AND_ARROW waehlen; BUNDLE faellt in {@code getArmPose} auf ITEM durch, und der Armschwung, den
 * der Server bei jedem Schlag schickt, liest sich dann als Hammerschlag.
 *
 * <p>{@code @ModifyExpressionValue} statt {@code @Redirect} (MixinExtras 0.5.4 mit Mixin 0.17.4
 * stuerzt bei jedem Redirect ab). Das Ziel ist der einzige {@code getUseAnimation()}-Aufruf der
 * Methode, in 26.2 und 1.21.11 wie in den NeoForge-gepatchten Quellen.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @ModifyExpressionValue(
            method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;"))
    private static ItemUseAnimation simplebuilding$hammerPose(ItemUseAnimation original,
                                                              @Local(argsOnly = true) Avatar avatar,
                                                              @Local(argsOnly = true) InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND && SledgehammerUpgrades.isHammering(avatar) ? ItemUseAnimation.BUNDLE : original;
    }
}
