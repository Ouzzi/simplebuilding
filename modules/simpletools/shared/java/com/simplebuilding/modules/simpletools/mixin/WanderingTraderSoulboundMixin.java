package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Now and then ({@code soulboundTraderChance}, 5 %) a wandering trader also sells one Soulbound book for diamonds. */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderSoulboundMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void simpletools$soulboundOffer(ServerLevel level, CallbackInfo ci) {
        WanderingTrader self = (WanderingTrader) (Object) this;
        Soulbound.maybeOffer(level, self.getOffers(), self.getRandom());
    }
}
