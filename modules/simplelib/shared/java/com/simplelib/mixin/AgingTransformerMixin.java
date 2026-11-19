package com.simplelib.mixin;

import com.simplelib.api.aging.Aging;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * N100: aging chains ({@link Aging}) on the item's way too. A sneaking player with an item never reaches the block's
 * {@code useItemOn} ({@link AgingStateMixin}); Vanilla copper still scrapes then because the axe's and the honeycomb's
 * {@link BlockTransformer} do it. When no Vanilla rule matched, the chain answers here with the same SUCCESS (arm swing,
 * particles, sound, durability) instead of PASS (nothing happened, no swing).
 */
@Mixin(BlockTransformer.class)
abstract class AgingTransformerMixin {
    @Inject(method = "transformBlock", at = @At("RETURN"), cancellable = true)
    private void simplelib$waxOrScrape(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (cir.getReturnValue() != InteractionResult.PASS || player == null) return;
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        InteractionResult result = Aging.useItemOn(context.getLevel().getBlockState(context.getClickedPos()), context.getItemInHand(),
                context.getLevel(), player, context.getHand(), hit);
        if (result != null) cir.setReturnValue(result);
    }
}
