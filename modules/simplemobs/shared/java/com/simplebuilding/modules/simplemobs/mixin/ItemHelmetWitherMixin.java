package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.BlightedPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blighted Wither summoning (owner N50): right-clicking the middle wither skeleton skull of an unfinished Wither figure
 * with a Netherite (or Enderite) Helmet puts the helmet on; the third skull then summons the stronger Wither. Anywhere
 * else on a wither skull: smoke, and the helmet stays in hand (the click is used, no equipping).
 */
@Mixin(Item.class)
public abstract class ItemHelmetWitherMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplemobs$helmetOnWitherSkull(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (BlightedPattern.stageOf(context.getItemInHand()) == null) return;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.WITHER_SKELETON_SKULL) && !state.is(Blocks.WITHER_SKELETON_WALL_SKULL)) return;
        if (!BlightedPattern.canCrown(level, pos)) {
            BlightedPattern.fizzle(level, pos);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (!level.isClientSide() && BlightedPattern.crown(level, pos, context.getItemInHand())
                && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
