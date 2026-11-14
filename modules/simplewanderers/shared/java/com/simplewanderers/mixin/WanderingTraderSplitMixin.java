package com.simplewanderers.mixin;

import com.simplewanderers.logic.WandererTrades;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Vanilla Wandering Trader draws his common and buying offers from his half of the split (concept section 5) - only while
 * Simple Wanderers is on; off, he reads the untouched Vanilla sets (AUDSB31E1, see {@link WandererTrades#traderSet}).
 * The Village Wanderer overrides {@code updateTrades} without calling it, so this only touches the Vanilla trader.
 */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderSplitMixin {
    @ModifyArg(method = "updateTrades", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;addOffersFromTradeSet(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/trading/MerchantOffers;Lnet/minecraft/resources/ResourceKey;)V"))
    private ResourceKey<TradeSet> simplewanderers$traderHalf(ResourceKey<TradeSet> vanilla) {
        return WandererTrades.traderSet(vanilla);
    }

    /** SW2B-2: goods that mods or data packs added to the Vanilla pools are not lost with the split; they stay with the trader. */
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void simplewanderers$foreignGoods(ServerLevel level, CallbackInfo ci) {
        WanderingTrader self = (WanderingTrader) (Object) this;
        WandererTrades.addForeign(level, self, self.getOffers());
    }
}
