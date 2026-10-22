package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Owner SP22-11: a right-click into the air with a mob head swaps it into the extra head slot ({@link ExtraHead#useInAir}).
 * At the base {@code Item#use}, which the head items (Vanilla skulls, Simple Mobheads' heads) do not override; every other
 * item gets null at once and Vanilla goes on.
 */
@Mixin(Item.class)
public abstract class ItemUseExtraHeadMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$putOnExtraHead(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = ExtraHead.useInAir(player, hand);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
