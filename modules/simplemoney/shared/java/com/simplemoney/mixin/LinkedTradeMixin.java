package com.simplemoney.mixin;

import com.simplemoney.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(VillagerTrade.class)
public abstract class LinkedTradeMixin {
    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void normalize(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        var price = MoneyLinks.price(cir.getReturnValue());
        if (price == null) return;
        SimpleMoney.config.links.normalize();
        var offer = cir.getReturnValue();
        cir.setReturnValue(new MerchantOffer(new ItemCost(MoneyItems.ITEMS.get("money_bill"), MoneyLinks.buy(price)),
                offer.getResult().copyWithCount(1), Math.min(price.stock(), SimpleMoney.config.links.stock), 0, 0));
    }
}
