package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.net.FactsPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleQuestionary.MOD_ID)
public final class QuestionaryForge {
    static Channel<CustomPacketPayload> channel;

    public QuestionaryForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplequestionary.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleQuestionary.init(FMLPaths.CONFIGDIR.get());
        SimpleQuestionary.modName = ns -> ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName()).orElse(ns);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) QuestionaryRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) QuestionaryRegistry.registerItems();
            if (key.equals(Registries.MENU)) QuestionaryRegistry.registerMenus();
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) || event.getTabKey().equals(QuestionaryRegistry.SB_FUNCTIONAL_TAB)) {
                QuestionaryRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        channel = ChannelBuilder.named(SimpleQuestionary.id("main")).payloadChannel().protocol(NetworkProtocol.PLAY)
                .clientbound()
                    .add(FactsPayload.TYPE, FactsPayload.CODEC.cast(), (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> FactsPayload.receive(payload));
                    })
                .build();
        SimpleQuestionary.toClient = (player, payload) -> {
            if (player.connection != null && channel.isRemotePresent(player.connection.getConnection())) channel.send(payload, PacketDistributor.PLAYER.with(player));
        };
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) QuestionaryForgeClient.init(context);
    }
}
