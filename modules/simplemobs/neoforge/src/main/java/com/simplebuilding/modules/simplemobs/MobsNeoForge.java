package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("simplemobs")
public final class MobsNeoForge {
    public MobsNeoForge(IEventBus bus, Dist dist) {
        MobsConfig.load(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.SOUND_EVENT, h -> MobsSounds.forEach(h::register));
event.register(Registries.ENTITY_TYPE, h -> {
                h.register(MobsRegistry.DECEIVER_KEY, MobsRegistry.DECEIVER);
                h.register(MobsRegistry.HOWLER_KEY, MobsRegistry.HOLLOW_HOWLER);
                h.register(MobsRegistry.RITUAL_KEY, MobsRegistry.HOWLER_RITUAL);
                h.register(DisguiseRegistry.MARKER_KEY, DisguiseRegistry.MARKER);
            });
            event.register(Registries.ITEM, h -> {
                h.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                h.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
                h.register(MobsRegistry.ECHO_CORE_KEY, MobsRegistry.ECHO_CORE);
                h.register(MobsRegistry.HOWLER_EGG_KEY, MobsRegistry.HOWLER_EGG);
            });
            event.register(Registries.ITEM, h -> DisguiseRegistry.items(h::register));
            event.register(Registries.POTION, h -> DisguiseRegistry.potions(h::register));
            event.register(Registries.MOB_EFFECT, h -> h.register(MobsRegistry.SILENCE_KEY, MobsRegistry.SILENCE));
            event.register(Registries.POTION, h -> {
                h.register(MobsRegistry.SILENCE_POTION_KEY, MobsRegistry.silencePotion());
                h.register(MobsRegistry.LONG_SILENCE_POTION_KEY, MobsRegistry.longSilencePotion());
            });
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes().build());
            event.put(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!MobsConfig.vanillaTabs()) return;
            if (!MobsRegistry.on()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                event.accept(MobsRegistry.EGG);
                insertAfter(event, new ItemStack(Items.HOGLIN_SPAWN_EGG), new ItemStack(MobsRegistry.HOWLER_EGG));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(DisguiseRegistry.CLOAK);
                if (MobsConfig.trickMirrorEnabled) event.accept(DisguiseRegistry.MIRROR);
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                event.accept(MobsRegistry.CLOTH);
                event.accept(DisguiseRegistry.SHARD);
                insertAfter(event, new ItemStack(Items.ECHO_SHARD), new ItemStack(MobsRegistry.ECHO_CORE));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> MobsServer.tick(event.getServer()));
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> EndNetherMobs.entityTypes(h::register));
            event.register(Registries.ITEM, h -> EndNetherMobs.items(h::register));
            event.register(Registries.MOB_EFFECT, h -> EndNetherMobs.effects(h::register));
            event.register(Registries.POTION, h -> EndNetherMobs.potions(h::register));
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(EndNetherMobs.SHELLKER, ShellkerEntity.createAttributes().build());
            event.put(EndNetherMobs.SOULFIRE_BLAZE, net.minecraft.world.entity.monster.Blaze.createAttributes().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!MobsConfig.vanillaTabs()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                insertAfter(event, new ItemStack(Items.SHULKER_SPAWN_EGG), new ItemStack(EndNetherMobs.SHELLKER_EGG));
                insertAfter(event, new ItemStack(Items.BLAZE_SPAWN_EGG), new ItemStack(EndNetherMobs.SOULFIRE_EGG));
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                insertAfter(event, new ItemStack(Items.SHULKER_SHELL), new ItemStack(EndNetherMobs.SHELL));
                insertAfter(event, new ItemStack(Items.BLAZE_ROD), new ItemStack(EndNetherMobs.ROD));
                insertAfter(event, new ItemStack(Items.BLAZE_POWDER), new ItemStack(EndNetherMobs.POWDER));
            }
            if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
                insertAfter(event, new ItemStack(Items.TURTLE_HELMET), new ItemStack(EndNetherMobs.HELMET));
                insertAfter(event, new ItemStack(Items.FIRE_CHARGE), new ItemStack(EndNetherMobs.SOUL_FIRE_CHARGE));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> EndNetherServer.tick(event.getServer()));
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> FearMobs.entityTypes(h::register));
            event.register(Registries.ITEM, h -> FearMobs.items(h::register));
            event.register(Registries.MOB_EFFECT, h -> {
                FearMobs.simplebuildingPresent = net.neoforged.fml.ModList.get().isLoaded("simplebuilding");
                FearMobs.effects(h::register);
            });
            event.register(Registries.POTION, h -> FearMobs.potions(h::register));
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(FearMobs.DREAD, DreadEntity.createAttributes().build());
            event.put(FearMobs.WATCHER, HollowWatcherEntity.createAttributes().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!MobsConfig.vanillaTabs()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                insertAfter(event, new ItemStack(Items.WITHER_SKELETON_SPAWN_EGG), new ItemStack(FearMobs.DREAD_EGG));
                insertAfter(event, new ItemStack(Items.ENDERMAN_SPAWN_EGG), new ItemStack(FearMobs.WATCHER_EGG));
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                insertAfter(event, new ItemStack(Items.ENDER_EYE), new ItemStack(FearMobs.EYE));
                insertAfter(event, new ItemStack(Items.ENDER_EYE), new ItemStack(FearMobs.LINK));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) event.accept(FearMobs.AMULET);
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> FearServer.tick(event.getServer()));
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> WitherMobs.entityTypes(h::register));
            event.register(Registries.ITEM, h -> WitherMobs.items(h::register));
            if (event.getRegistryKey().equals(Registries.ITEM)) com.simplebuilding.modules.simplemobs.guide.MobsGuide.register();
            if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) MobsRegistry.registerTab();
        });
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(WitherMobs.BLIGHTED_WITHER, BlightedWither.createAttributes().build()));
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(WitherMobs.ABYSSAL_WITHER, BlightedWither.createAttributes(WitherLogic.ENDERITE).build()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) { if (MobsConfig.vanillaTabs()) insertAfter(event, new ItemStack(Items.NETHER_STAR), new ItemStack(WitherMobs.HEART)); }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) { if (MobsConfig.vanillaTabs()) insertAfter(event, new ItemStack(Items.BRUSH), new ItemStack(WitherMobs.BRUSH)); }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) event.accept(com.simplebuilding.modules.simplemobs.guide.MobsGuide.stack());
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simplemobs.guide.MobsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        // client FOV for the Hollow Watcher's gaze zones
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToServer(FearNet.FovPayload.TYPE, FearNet.FovPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> FearNet.handle(payload, (net.minecraft.server.level.ServerPlayer) context.player()))));
        if (dist == Dist.CLIENT) {
            FearNet.toServer = net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer;
            NeoForgeClientExample.init(bus);
        }
    }

    private static void insertAfter(BuildCreativeModeTabContentsEvent event, ItemStack anchor, ItemStack stack) {
        if (event.getParentEntries().contains(anchor)) event.insertAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else event.accept(stack);
    }
}
