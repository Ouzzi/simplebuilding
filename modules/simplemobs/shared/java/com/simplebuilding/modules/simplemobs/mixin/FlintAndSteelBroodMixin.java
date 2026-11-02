package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.BroodRitual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Brood hollow (K1, {@link BroodRitual}): Flint and Steel on the Purpur Block of a complete hollow lights the brood instead of a fire. */
@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelBroodMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplemobs$brood(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(Blocks.PURPUR_BLOCK) || !BroodRitual.figure(level, context.getClickedPos())) return;
        if (!level.isClientSide()) {
            boolean started = BroodRitual.tryStart(level, context.getClickedPos(), context.getPlayer() instanceof ServerPlayer sp ? sp : null);
            if (!started) return;
            if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand().asEquipmentSlot());
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
