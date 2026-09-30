package com.simplefun;

import com.simplefun.command.SimplefunCommands;
import com.simplefun.event.PlayerHeadDrop;
import com.simplefun.registry.ModEffects;
import com.simplefun.registry.ModEntities;
import com.simplefun.registry.ModItems;
import com.simplefun.trade.SimplefunTrades;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class SimplefunNeoForge {

    public SimplefunNeoForge(IEventBus modBus, ModContainer modContainer, Dist dist) {
        SimplefunCommon.init();
        com.simplebuilding.modules.simplefun.ModuleNeoTests.register(modBus);

        SimplefunAttachments.ATTACHMENT_TYPES.register(modBus);

        // NeoForge requires registering through the event helper (not vanilla Registry.register).
        modBus.addListener((RegisterEvent event) -> {
            if(event.getRegistryKey().equals(Registries.BLOCK))com.simplefun.heads.AnimalHeads.blocks();
            if(event.getRegistryKey().equals(Registries.ITEM)){event.register(Registries.ITEM, h -> h.register(ModItems.BRICK_SNOWBALL_KEY, ModItems.BRICK_SNOWBALL));com.simplefun.heads.AnimalHeads.items();}
            if(event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))com.simplefun.heads.AnimalHeads.tab();
            if(event.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS))event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,com.simplefun.heads.AnimalHeads.id("trades_enabled"),()->FunNeoData.CODEC);
            event.register(Registries.ENTITY_TYPE, h -> h.register(ModEntities.BRICK_PROJECTILE_KEY, ModEntities.BRICK_PROJECTILE));
            event.register(Registries.MOB_EFFECT, h -> h.register(ModEffects.PIGGY_KEY, ModEffects.PIGGY_EFFECT));
        });

        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                event.accept(ModItems.BRICK_SNOWBALL);
            }
        });

        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) ->
                PlayerHeadDrop.onDeath(event.getEntity(), event.getSource()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                SimplefunCommands.register(event.getDispatcher()));

        // Server-side: keep the synced piggy attachment in step with the effect (sent to all trackers).
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            Player player = event.getEntity();
            if (!player.level().isClientSide()) {
                boolean piggy = player.hasEffect(ModEffects.holder());
                if (player.getData(SimplefunAttachments.PIGGY) != piggy) {
                    player.setData(SimplefunAttachments.PIGGY, piggy);
                }
            }
        });

        if (dist == Dist.CLIENT) {
            SimplefunNeoForgeClient.init(modBus, modContainer);
        }
    }
}