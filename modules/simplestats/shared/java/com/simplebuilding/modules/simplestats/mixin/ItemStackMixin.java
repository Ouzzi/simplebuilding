package com.simplebuilding.modules.simplestats.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simplestats.StatItems;
import com.simplebuilding.modules.simplestats.StatsRuntime;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block mining (every vanilla and modded way ends in {@code mineBlock}) and the use of axes, hoes and flint and steel. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "mineBlock", at = @At("HEAD"))
    private void simplestats$mined(Level level, BlockState state, BlockPos pos, Player owner, CallbackInfo ci) {
        if (!level.isClientSide() && owner instanceof ServerPlayer player) StatsRuntime.onMine(player, (ItemStack) (Object) this, state);
    }

    @WrapMethod(method = "useOn")
    private InteractionResult simplestats$used(UseOnContext context, Operation<InteractionResult> original) {
        ItemStack self = (ItemStack) (Object) this;
        Level level = context.getLevel();
        if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) return original.call(context);
        BlockPos pos = context.getClickedPos();
        BlockState before = level.getBlockState(pos);
        InteractionResult result = original.call(context);
        if (result.consumesAction() && !self.isEmpty()) {
            BlockState after = level.getBlockState(pos);
            if (after.getBlock() != before.getBlock()) {
                if (self.is(ItemTags.AXES) && before.is(BlockTags.LOGS)) StatsRuntime.onUse(player, self, StatKeys.LOGS_STRIPPED);
                else if (self.is(ItemTags.HOES)) StatsRuntime.onUse(player, self, StatKeys.TILLED);
            }
            if (StatItems.lighter(self)) StatsRuntime.onUse(player, self, StatKeys.IGNITED);
        }
        return result;
    }
}
