package com.simplemoney.mixin;

import com.simplemoney.MoneyLinks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MerchantMenu.class)
public abstract class LinkedMenuMixin {
    @Shadow @Final private MerchantContainer tradeContainer;
    // Vanilla quickMoveStack bypasses Slot.mayPickup: validate before moving any output.
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void validate(Player player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == 2 && player instanceof ServerPlayer server && !MoneyLinks.allowed(server, tradeContainer.getActiveOffer()))
            cir.setReturnValue(ItemStack.EMPTY);
    }
}
