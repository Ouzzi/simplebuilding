package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.net.FactsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class QuestionaryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleQuestionary.init(FabricLoader.getInstance().getConfigDir());
        SimpleQuestionary.modName = ns -> FabricLoader.getInstance().getModContainer(ns).map(c -> c.getMetadata().getName()).orElse(ns);
        QuestionaryRegistry.registerBlocks();
        QuestionaryRegistry.registerItems();
        QuestionaryRegistry.registerMenus();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out ->
                out.insertAfter(Items.LECTERN, QuestionaryRegistry.tabStacks(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        CreativeModeTabEvents.modifyOutputEvent(QuestionaryRegistry.SB_FUNCTIONAL_TAB).register(out ->
                QuestionaryRegistry.tabStacks().forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)));
        PayloadTypeRegistry.clientboundPlay().register(FactsPayload.TYPE, FactsPayload.CODEC);
        SimpleQuestionary.toClient = (player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
    }
}
