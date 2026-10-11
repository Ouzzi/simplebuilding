package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.net.FactsPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleQuestionary.MOD_ID)
public final class QuestionaryNeoForge {
    public QuestionaryNeoForge(IEventBus bus, Dist dist) {
        SimpleQuestionary.init(FMLPaths.CONFIGDIR.get());
        SimpleQuestionary.modName = ns -> ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName()).orElse(ns);
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) QuestionaryRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) QuestionaryRegistry.registerItems();
            if (key.equals(Registries.MENU)) QuestionaryRegistry.registerMenus();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) || event.getTabKey().equals(QuestionaryRegistry.SB_FUNCTIONAL_TAB)) {
                QuestionaryRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToClient(FactsPayload.TYPE, FactsPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> FactsPayload.receive(payload))));
        SimpleQuestionary.toClient = (player, payload) -> {
            if (player.connection != null && player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
        };
        if (dist == Dist.CLIENT) QuestionaryNeoClient.init(bus);
    }
}
