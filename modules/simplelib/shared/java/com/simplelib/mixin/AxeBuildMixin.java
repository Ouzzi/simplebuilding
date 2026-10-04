package com.simplelib.mixin;

import com.simplelib.api.SimpleLibApi;
import com.simplelib.crucible.CrucibleBlankBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Vanilla way (principle 5a): an axe in the main hand and the right part in the off hand builds
 * the iron crucible on an iron block, strike by strike. Runs before the axe's block transformer (26.3
 * axes are plain items with a {@code block_transformers} component), so it never strips
 * anything by accident; any other click is Vanilla.
 */
@Mixin(ItemStack.class)
public abstract class AxeBuildMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplelib$buildCrucible(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND || !SimpleLibApi.axeWaysEnabled()) return;
        if (!CrucibleBlankBlock.isAxe((ItemStack) (Object) this)) return;
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if (com.simplelib.crucible.CrucibleUpgrades.stepFor(state, player.getOffhandItem()) != null) {
            boolean hit = com.simplelib.crucible.CrucibleUpgrades.strike(context.getLevel(), context.getClickedPos(), player, context.getItemInHand());
            cir.setReturnValue(hit ? InteractionResult.SUCCESS : InteractionResult.FAIL);
            return;
        }
        int done = CrucibleBlankBlock.strikesAt(state);
        if (!CrucibleBlankBlock.fitsNext(done, player.getOffhandItem())) return;
        if (CrucibleBlankBlock.strike(context.getLevel(), context.getClickedPos(), player, context.getItemInHand(), 1)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
