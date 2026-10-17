package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.*;
import com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Block log (breaks), /inspect clicks and freeze for the player's block interactions. */
@Mixin(ServerPlayerGameMode.class)
public abstract class AdminGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Unique private String[] simpletweaks$before;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$click(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (Freeze.frozen(player) || action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && Inspect.click(player, pos)
                || action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && Inspect.inspecting(player)) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Freeze.frozen(player)) { cir.setReturnValue(false); return; }
        simpletweaks$before = BlockRecorder.before(level, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void simpletweaks$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var before = simpletweaks$before;
        simpletweaks$before = null;
        if (cir.getReturnValueZ()) BlockRecorder.broke(player, pos, before);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$use(ServerPlayer p, Level l, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (Freeze.frozen(p)) { cir.setReturnValue(InteractionResult.FAIL); return; }
        if (hand == InteractionHand.MAIN_HAND ? Inspect.click(p, hit.getBlockPos()) : Inspect.inspecting(p)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$useItem(ServerPlayer p, Level l, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (Freeze.frozen(p)) cir.setReturnValue(InteractionResult.FAIL);
    }
}
