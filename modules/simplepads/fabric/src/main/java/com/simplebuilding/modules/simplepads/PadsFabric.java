package com.simplebuilding.modules.simplepads;

import com.simplepads.PadsConfig;
import com.simplepads.PadsContent;
import com.simplepads.SimplePads;
import com.simplepads.command.PadsCommands;
import com.simplepads.item.PadsItems;
import com.simplepads.network.ElytraBoostPayload;
import com.simplepads.network.PadsConfigPayload;
import com.simplepads.network.PadsNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fabric entry of Simple Pads: registries are open during init, so everything registers directly. SimpleBuilding calls it before
 * its own registration (its pad fields point at these blocks); the loader's second call returns.
 */
public final class PadsFabric implements ModInitializer {
    private static boolean initialized;
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        SimplePads.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simplepads config for every option; a change goes to the clients at once.
        com.simplelib.api.config.ConfigCommands.server(SimplePads.MOD_ID)
                .gson(PadsConfig.class, SimplePads::localConfig, SimplePads::replaceConfig, () -> new PadsConfig().validate())
                .keys(PadsConfig.KEYS)
                .file(FabricLoader.getInstance().getConfigDir(), PadsConfig.FILE)
                .after(() -> PadsNetwork.sendConfigToAll(server))
                .register();
        com.simplepads.guide.PadsGuide.register();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimplePads.id("pads"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplepads.pads"))
                        .icon(() -> new ItemStack(com.simplepads.block.PadsBlocks.ELYTRA_PAD))
                        .displayItems((parameters, output) -> {
                            if (!SimplePads.on()) return;
                            PadsItems.tabStacks().forEach(output::accept);
                            output.accept(com.simplepads.guide.PadsGuide.stack());
                        })
                        .build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(out -> {
            if (!SimplePads.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(
                    FabricLoader.getInstance().getConfigDir(), SimplePads.MOD_ID)) return;
            var stacks = java.util.List.of(new ItemStack(com.simplepads.block.PadsBlocks.DIAMOND_PRESSURE_PLATE),
                    new ItemStack(com.simplepads.block.PadsBlocks.NETHERITE_PRESSURE_PLATE),
                    new ItemStack(com.simplepads.block.PadsBlocks.ENDERITE_PRESSURE_PLATE));
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.HEAVY_WEIGHTED_PRESSURE_PLATE))) {
                out.insertAfter(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimplePads.on()) return;
            var book = java.util.List.of(com.simplepads.guide.PadsGuide.stack());
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        // Since 26.x Vanilla sends no recipes to the client; JEI gets them on Fabric through the Fabric recipe sync, which only
        // carries announced serializers (Elytra Pad I and Flypad I use enchanted_shapeless, the easter chain easter_smithing).
        for (var entry : BuiltInRegistries.RECIPE_SERIALIZER.entrySet()) {
            if (SimplePads.MOD_ID.equals(entry.getKey().identifier().getNamespace())) {
                net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(entry.getValue());
            }
        }

        PayloadTypeRegistry.serverboundPlay().register(ElytraBoostPayload.ID, ElytraBoostPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PadsConfigPayload.ID, PadsConfigPayload.CODEC);
        PadsNetwork.setSenders((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        }, payload -> {});
        ServerPlayNetworking.registerGlobalReceiver(ElytraBoostPayload.ID,
                (payload, context) -> context.server().execute(() -> PadsNetwork.handleBoost(payload, context.player())));

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        ServerTickEvents.END_SERVER_TICK.register(PadsContent::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> PadsContent.onPlayerJoin(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PadsContent.onPlayerRespawn(newPlayer));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PadsCommands.register(dispatcher));
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplepads.guide.PadsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
