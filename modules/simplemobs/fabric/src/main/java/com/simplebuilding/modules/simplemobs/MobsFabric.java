package com.simplebuilding.modules.simplemobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;

public final class MobsFabric implements ModInitializer {
    @Override public void onInitialize() {
        WitherLoot.idMoves();
        MobsConfig.load(FabricLoader.getInstance().getConfigDir());
        MobsRegistry.registerAll();
        DisguiseRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(MobsServer::tick);
        // WP-22: Simple Mobs' own boss kills count for the killer's resonance value (the spawn hook itself is a mixin).
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(ZoneSpawns::onDeath);
        // MOB27-3 / ZO9 A: Simple Mobs applies the armour trim abilities (the SimpleBuilding core only feeds the inputs as host).
        com.simplebuilding.modules.simplemobs.trim.TrimAbilities.install();
        EndNetherMobs.registerAll();
        FabricDefaultAttributeRegistry.register(EndNetherMobs.SHELLKER, ShellkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(EndNetherMobs.SOULFIRE_BLAZE, net.minecraft.world.entity.monster.Blaze.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(EndNetherServer::tick);
        FearMobs.simplebuildingPresent = FabricLoader.getInstance().isModLoaded("simplebuilding");
        FearMobs.registerAll();
        FabricDefaultAttributeRegistry.register(FearMobs.DREAD, DreadEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FearMobs.WATCHER, HollowWatcherEntity.createAttributes());
        BossMobs.registerAll(); // WP-19: Void Runner, Brimguard, brood hollow
        FabricDefaultAttributeRegistry.register(BossMobs.VOID_RUNNER, VoidRunnerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BossMobs.BRIMGUARD, BrimguardEntity.createAttributes());
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(FearNet.FovPayload.TYPE, FearNet.FovPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(FearNet.FovPayload.TYPE,
                (payload, context) -> context.server().execute(() -> FearNet.handle(payload, context.player())));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(TrumpetNet.TonePayload.TYPE, TrumpetNet.TonePayload.CODEC);
        TrumpetNet.sender = (player, payload) -> { if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload); };
        com.simplelib.api.PlayerState.onLeave("simplemobs:trumpet", TrumpetNet::forget);
        com.simplelib.api.PlayerState.onStop("simplemobs:trumpet", TrumpetNet::forgetAll);
        ServerTickEvents.END_SERVER_TICK.register(FearServer::tick);
        com.simplelib.api.PlayerState.onLeave("simplemobs:fear", FearNet::forget);
        com.simplelib.api.PlayerState.onStop("simplemobs:fear", FearNet::forgetAll);
        com.simplebuilding.modules.simplemobs.trials.Trials.registerEffects();
        WitherMobs.registerAll();
        com.simplebuilding.modules.simplemobs.trials.TrialsHooks.register();
        ServerTickEvents.END_SERVER_TICK.register(com.simplebuilding.modules.simplemobs.trials.EvilOmen::serverTick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(com.simplebuilding.modules.simplemobs.trials.EvilOmen::onRaiderLoaded);
        com.simplebuilding.modules.simplemobs.guide.MobsGuide.register();
        MobsRegistry.registerTab();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simplemobs.guide.MobsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        FabricDefaultAttributeRegistry.register(WitherMobs.BLIGHTED_WITHER, BlightedWither.createAttributes());
        FabricDefaultAttributeRegistry.register(WitherMobs.ABYSSAL_WITHER, BlightedWither.createAttributes(WitherLogic.ENDERITE));
        // IG31-3 / QW-5: every other item stands in the Mobs tab (MobsRegistry.TAB_ITEMS); only the mirror (off by default, in no tab) is offered in Tools.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (MobsConfig.vanillaTabs() && MobsConfig.trickMirrorEnabled) out.accept(DisguiseRegistry.MIRROR);
        });
    }
}
