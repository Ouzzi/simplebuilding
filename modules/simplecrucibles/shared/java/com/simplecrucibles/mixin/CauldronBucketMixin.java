package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CauldronBuckets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Copper and Enderite buckets on the Vanilla cauldron (Crucible P5, owner 33): pour water/lava into an empty
 * cauldron and take a full water/lava cauldron like the iron bucket - the copper bucket still breaks on lava and
 * oxidizes when pouring. Soul lava never goes into a normal cauldron (owner 30: only the reinforced one). A full
 * Enderite bucket pours one of its two buckets, a half one tops up from a full cauldron of its fluid (owner N21).
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronBucketMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplecrucibles$crucibleBuckets(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = CauldronBuckets.interact(stack, state, level, pos, player, hand);
        if (result != null) cir.setReturnValue(result);
    }
}
