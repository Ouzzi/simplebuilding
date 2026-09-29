package com.simplebuilding.mixin;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.util.TradePrices;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Preisfaktor {@code server.loot.tradePriceMultiplier} fuer die datengetriebenen Handelsangebote der
 * Mod (MC 26.x: {@code data/simplebuilding/villager_trade/**}, Dorfbewohner und fahrender Haendler).
 * Erkannt am Registernamen des Angebots; Vanilla- und fremde Angebote bleiben. Die 1.21.11-Linie
 * skaliert in {@code TradeDefinition#toListing}.
 */
@Mixin(VillagerTrade.class)
public abstract class VillagerTradePriceMixin {

    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$scaleModPrices(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        double multiplier = ServerTuning.tradePriceMultiplier();
        if (offer == null || multiplier == 1.0) {
            return;
        }
        Identifier id = context.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
                .getKey((VillagerTrade) (Object) this);
        if (id != null && "simplebuilding".equals(id.getNamespace())) {
            cir.setReturnValue(TradePrices.scale(offer, multiplier));
        }
    }
}
