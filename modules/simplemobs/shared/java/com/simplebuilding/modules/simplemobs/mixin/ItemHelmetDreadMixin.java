package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.DreadPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dread summoning: right-clicking a skeleton skull with an Iron Helmet puts the helmet on. On a complete figure the Dread
 * rises and the helmet is used up; otherwise smoke, and the helmet stays in hand (the click is used, no equipping).
 */
@Mixin(Item.class)
public abstract class ItemHelmetDreadMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplemobs$helmetOnSkull(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!context.getItemInHand().is(Items.IRON_HELMET)) return;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.SKELETON_SKULL) && !state.is(Blocks.SKELETON_WALL_SKULL)) return;
        if (!DreadPattern.matches(level, pos)) {
            DreadPattern.fizzle(level, pos);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (!level.isClientSide() && DreadPattern.trySummon(level, pos) && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
