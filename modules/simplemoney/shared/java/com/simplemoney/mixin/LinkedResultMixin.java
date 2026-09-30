package com.simplemoney.mixin;

import com.simplemoney.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MerchantResultSlot.class)
public abstract class LinkedResultMixin {
    @Shadow @Final private MerchantContainer slots;
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void validate(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer server && !MoneyLinks.allowed(server, slots.getActiveOffer())) cir.setReturnValue(false);
    }
    // This invocation only occurs after Vanilla successfully consumes payment.
    @Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void spent(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer server && MoneyLinks.price(slots.getActiveOffer()) != null)
            LinkBudget.get(server).spend(server.getUUID(), server.level().getServer().overworld().getGameTime());
    }
}
