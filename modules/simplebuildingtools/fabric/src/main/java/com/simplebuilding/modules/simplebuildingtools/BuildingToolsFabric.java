package com.simplebuilding.modules.simplebuildingtools;

import com.simplebuildingtools.SimpleBuildingTools;
import com.simplebuildingtools.config.BtConfig;
import com.simplebuildingtools.items.BtItems;
import com.simplebuildingtools.networking.BlueprintEditPayload;
import com.simplebuildingtools.networking.BlueprintRotatePayload;
import com.simplebuildingtools.networking.BtConfigPayload;
import com.simplebuildingtools.networking.BuildingWandConfigurePayload;
import com.simplebuildingtools.networking.ModMessageHandlers;
import com.simplebuildingtools.networking.OctantConfigurePayload;
import com.simplebuildingtools.networking.OctantScrollPayload;
import com.simplebuildingtools.util.OctantCauldronWash;
import java.util.function.BiConsumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric entry of Simple Building Tools: registries are open during init, so everything registers directly. SimpleBuilding calls
 * it before its own registration (its tabs, test centre and datagen find the tools by id); the loader's second call returns.
 */
public final class BuildingToolsFabric implements ModInitializer {
    private static boolean initialized;
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        SimpleBuildingTools.init(FabricLoader.getInstance().getConfigDir());
        com.simplelib.api.config.ConfigCommands.server(SimpleBuildingTools.MOD_ID)
                .gson(BtConfig.class, SimpleBuildingTools::localConfig, SimpleBuildingTools::replaceConfig, () -> new BtConfig().validate())
                .file(FabricLoader.getInstance().getConfigDir(), BtConfig.FILE)
                .after(BuildingToolsFabric::sendConfigToAll)
                .register();
        com.simplebuildingtools.guide.BuildingToolsGuide.register();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleBuildingTools.id("tools"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplebuildingtools.tools"))
                        .icon(() -> new ItemStack(BtItems.IRON_BUILDING_WAND))
                        .displayItems((parameters, output) -> {
                            if (!SimpleBuildingTools.on()) return;
                            BtItems.tabStacks().forEach(output::accept);
                        })
                        .build());
        // Washing a dyed octant in a water cauldron (the table is package-private: accessor mixin).
        for (Item octant : OctantCauldronWash.washableOctants()) {
            ((com.simplebuildingtools.mixin.CauldronDispatcherAccessor) (Object) CauldronInteractions.WATER)
                    .simplebuildingtools$put(octant, OctantCauldronWash.INTERACTION);
        }
        // Claim and protection mods answer the loader's break event for a cell the wand fills (as SimpleBuilding asked).
        com.simplebuildingtools.util.BuildPermissions.setPlaceProbe((level, player, pos, state) ->
                PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, level.getBlockState(pos), level.getBlockEntity(pos)));

        PayloadTypeRegistry.serverboundPlay().register(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OctantScrollPayload.ID, OctantScrollPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BlueprintEditPayload.ID, BlueprintEditPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BlueprintRotatePayload.ID, BlueprintRotatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BtConfigPayload.ID, BtConfigPayload.CODEC);
        receive(OctantConfigurePayload.ID, ModMessageHandlers::handleOctantConfigure);
        receive(OctantScrollPayload.ID, ModMessageHandlers::handleOctantScroll);
        receive(BuildingWandConfigurePayload.ID, ModMessageHandlers::handleBuildingWandConfigure);
        receive(BlueprintEditPayload.ID, ModMessageHandlers::handleBlueprintEdit);
        receive(BlueprintRotatePayload.ID, ModMessageHandlers::handleBlueprintRotate);

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            com.simplebuildingtools.util.WandUndo.clearAll();
            com.simplebuildingtools.blueprint.BlueprintBuilder.clearAll();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> sendConfig(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
            com.simplebuildingtools.util.WandUndo.onDisconnect(handler.player);
            com.simplebuildingtools.blueprint.BlueprintBuilder.onDisconnect(handler.player);
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuildingtools.guide.BuildingToolsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }

    private static <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, BiConsumer<T, ServerPlayer> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> context.server().execute(() -> handler.accept(payload, context.player())));
    }

    static void sendConfig(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, BtConfigPayload.ID)) {
            ServerPlayNetworking.send(player, new BtConfigPayload(SimpleBuildingTools.localConfigJson()));
        }
    }

    static void sendConfigToAll() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendConfig(player);
    }
}
