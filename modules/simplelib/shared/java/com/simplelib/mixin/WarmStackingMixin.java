package com.simplelib.mixin;

import com.simplelib.config.LibConfig;
import com.simplelib.warm.Warm;
import com.simplelib.warm.WarmMerge;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Warm food stacks by mean warmth (see {@link WarmMerge}) and shows its remaining warm time in the
 * tooltip ("Warm (about 7 min)").
 */
@Mixin(ItemStack.class)
public abstract class WarmStackingMixin {
    @Inject(method = "isSameItemSameComponents", at = @At("RETURN"), cancellable = true)
    private static void simplelib$ignoreWarm(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && WarmMerge.sameIgnoringWarm(a, b)) cir.setReturnValue(true);
    }

    @Inject(method = "grow", at = @At("HEAD"))
    private void simplelib$blendWarmth(int amount, CallbackInfo ci) {
        WarmMerge.beforeGrow((ItemStack) (Object) this, amount);
    }

    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void simplelib$warmTooltip(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
                                       TooltipFlag flag, Consumer<Component> out, CallbackInfo ci) {
        if (player == null) return;
        long left = Warm.remaining((ItemStack) (Object) this, player.level().getGameTime());
        if (left <= 0) return;
        long minutes = Math.max(1, Math.round(left / 1200.0));
        out.accept(Component.translatable("tooltip.simplelib.warm", minutes).withStyle(ChatFormatting.GOLD));
        if (LibConfig.eatSpeedBonus > 0) {
            out.accept(Component.translatable("tooltip.simplelib.warm.faster", Math.round(LibConfig.eatSpeedBonus * 100)).withStyle(ChatFormatting.GRAY));
        }
    }
}
