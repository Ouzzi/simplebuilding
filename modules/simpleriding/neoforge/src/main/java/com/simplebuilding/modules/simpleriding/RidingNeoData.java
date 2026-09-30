package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
@EventBusSubscriber(modid="simpleriding") public record RidingNeoData() implements ICondition {
 public static final MapCodec<RidingNeoData> CODEC=MapCodec.unit(new RidingNeoData());
 public boolean test(IContext context){return Riding.CONFIG.worldGen.enableVillagerTrades;}
 public MapCodec<? extends ICondition> codec(){return CODEC;}
 @SubscribeEvent public static void loot(LootTableLoadEvent event){RidingLoot.apply(ResourceKey.create(Registries.LOOT_TABLE,event.getName()),p->event.getTable().addPool(p.build()),event.getRegistries());}
}
