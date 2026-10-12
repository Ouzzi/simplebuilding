package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;

@Mod("simplerespawn")
public final class RespawnForge {
    private static Channel<CustomPacketPayload> channel;

    public RespawnForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplerespawn.forge.ModuleForgeTests.register(context.getModBusGroup());
        RespawnConfig.load(FMLPaths.CONFIGDIR.get());
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.register();
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.installQuests(FMLPaths.CONFIGDIR.get());
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) || !SimpleRespawn.on()) return;
            var book = new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.book());
            var anchor = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            if (e.getEntries().contains(anchor)) e.getEntries().putBefore(anchor, book, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else e.accept(book);
        });
        channel = ChannelBuilder.named(Identifier.parse("simplerespawn:main")).payloadChannel().protocol(NetworkProtocol.PLAY).clientbound()
                .add(DownedListPayload.TYPE, DownedListPayload.CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> RespawnForgeClient.receive(p));
                })
                .add(RespawnStatePayload.TYPE, RespawnStatePayload.CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> RespawnForgeClient.receive(p));
                }).build();
        SimpleRespawn.send = (player, payload) -> {
            if (player.connection != null && channel.isRemotePresent(player.connection.getConnection()))
                channel.send(payload, PacketDistributor.PLAYER.with(player));
        };
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> RespawnServer.tick(e.server()));
        ServerStoppedEvent.BUS.addListener(e -> RespawnServer.reset());
        RegisterCommandsEvent.BUS.addListener(e -> RespawnCommands.register(e.getDispatcher()));
    }
}
