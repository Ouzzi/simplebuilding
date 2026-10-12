package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.EndRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3 strips logs through the data-driven axe block transformer, which a mod cannot extend without replacing it.
 * When no Vanilla rule matched and an axe is used on an End log/wood, strip it the same way (sound, durability).
 */
@Mixin(BlockTransformer.class)
public abstract class BlockTransformerMixin {
    @Inject(method = "transformBlock", at = @At("RETURN"), cancellable = true)
    private void simpleend$stripEndWood(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.PASS) return;
        ItemStack stack = context.getItemInHand();
        if (!stack.is(ItemTags.AXES)) return;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState stripped = EndRegistry.stripped(level.getBlockState(pos));
        if (stripped == null) return;
        Player player = context.getPlayer();
        level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(pos, stripped, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, stripped));
        if (player != null) stack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
