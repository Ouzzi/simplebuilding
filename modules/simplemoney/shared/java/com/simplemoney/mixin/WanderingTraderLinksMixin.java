package com.simplemoney.mixin;

import com.simplemoney.SimpleMoney;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's wandering trader only reads the buying/uncommon/common trade sets; the price-list links sit in the
 * tag {@code wandering_trader/rare}, which no vanilla set reads. After the three vanilla draws this adds one more
 * draw from our own set {@code simplemoney:wandering_trader/links} (one extra offer, never replacing a vanilla one).
 */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderLinksMixin extends AbstractVillager {
    @Unique
    private static final ResourceKey<TradeSet> LINKS = ResourceKey.create(Registries.TRADE_SET,
            Identifier.fromNamespaceAndPath(SimpleMoney.MOD_ID, "wandering_trader/links"));

    protected WanderingTraderLinksMixin(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void simplemoney$linkOffers(ServerLevel level, CallbackInfo ci) {
        if (!SimpleMoney.enabled("enableWanderingTrades")) return;
        addOffersFromTradeSet(level, getOffers(), LINKS);
    }
}
