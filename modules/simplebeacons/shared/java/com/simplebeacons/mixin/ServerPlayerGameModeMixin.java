package com.simplebeacons.mixin;

import com.simplebeacons.upgrade.StrongUpgrades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The in-world Strong upgrade ({@link StrongUpgrades}) runs before the block's own use: otherwise the beacon would open
 * its menu on the click. Only for the upgrade stance (tool + Strong template) on a beacon or conduit.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplebeacons$strongUpgrade(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSpectator() || !(level instanceof ServerLevel server)) return;
        InteractionResult result = StrongUpgrades.strike(server, hit.getBlockPos(), player, hand);
        if (result != null) cir.setReturnValue(result);
    }
}
