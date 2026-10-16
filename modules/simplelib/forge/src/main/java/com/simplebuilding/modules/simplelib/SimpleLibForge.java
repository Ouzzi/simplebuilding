package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.registry.LibComponents;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleLib.MOD_ID)
public final class SimpleLibForge {
    static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;
    public SimpleLibForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplelib.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleLib.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) LibComponents.register();
            if (key.equals(Registries.BLOCK)) com.simplelib.registry.LibMaterials.ensureBlocks();
            if (key.equals(Registries.ITEM)) com.simplelib.registry.LibMaterials.ensureItems();
            if (key.equals(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS)) {
                event.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS, SimpleLib.id("material_requested"),
                        () -> com.simplebuilding.modules.simplelib.forge.MaterialConditionForge.CODEC);
            }
            // Explicit id moves (IdMoves) as Forge registry aliases: the defaulted wrapper answers getValue/containsKey
            // from the ForgeRegistry, which the mixin never sees, and a world's id snapshot finds old ids. Lazy aliases,
            // so the order against other mods' registrations does not matter; all mods are constructed by now.
            if (event.getForgeRegistry() instanceof net.minecraftforge.registries.ForgeRegistry<?> registry) {
                com.simplelib.api.IdMoves.explicit(key).forEach(registry::addAlias);
            }
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event ->
                com.simplelib.api.InWorldStrikes.tick(event.server()));
        // In-world upgrade files: SimpleLib's own load and client sync (idle while a partner such as SimpleBuilding loads them).
        net.minecraftforge.event.AddReloadListenerEvent.BUS.addListener(event -> event.addListener(com.simplelib.api.InWorldUpgrades.reloadListener()));
        channel = net.minecraftforge.network.ChannelBuilder.named(SimpleLib.id("main")).payloadChannel()
                .protocol(net.minecraftforge.network.NetworkProtocol.PLAY)
                .clientbound()
                    .add(com.simplelib.net.UpgradeTablePayload.TYPE, com.simplelib.net.UpgradeTablePayload.CODEC.cast(), (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> com.simplelib.api.InWorldUpgrades.receive(payload.files()));
                    })
                .build();
        net.minecraftforge.event.OnDatapackSyncEvent.BUS.addListener(event -> {
            var payload = com.simplelib.net.UpgradeTablePayload.current();
            if (payload == null) return;
            event.getPlayers().forEach(player -> {
                if (player.connection != null && channel.isRemotePresent(player.connection.getConnection())) {
                    channel.send(payload, net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
                }
            });
        });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            SimpleLibForgeClient.init(context);
        }
    }
}
