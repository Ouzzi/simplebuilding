package com.simplebuilding.modules.simplechisels;

import com.simplechisels.SimpleChisels;
import com.simplechisels.data.ChiselTables;
import com.simplechisels.item.ChiselItem;
import com.simplechisels.net.ChiselTablesPayload;
import com.simplechisels.registry.ChiselRegistry;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/**
 * Fabric entry of Simple Chisels: registries are open during init, so everything registers directly. SimpleBuilding calls it
 * before its own registration (its tabs, datagen and tests find the chisels by id); the loader's second call returns.
 */
public final class ChiselsFabric implements ModInitializer {
    private static boolean initialized;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        FabricLoader loader = FabricLoader.getInstance();
        SimpleChisels.init(loader.getConfigDir(), loader::isModLoaded);
        ChiselRegistry.registerItems();
        ChiselRegistry.registerTab();
        // The hand hint: SimpleBuilding's renderer (TransformTargets) asks SimpleLib's item hints where it asked its own chisel.
        for (ChiselItem chisel : ChiselRegistry.CHISELS.values()) {
            com.simplelib.api.GadgetActions.registerItemHint(chisel, context -> context.getPlayer() != null
                    && chisel.canChisel(context.getLevel(), context.getClickedPos(), context.getItemInHand(), context.getPlayer()));
        }
        // Datapack tables: read on every (re)load, sent to each client on join and after /reload.
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SimpleChisels.id(ChiselTables.DIRECTORY), ChiselTables.reloadListener());
        PayloadTypeRegistry.clientboundPlay().register(ChiselTablesPayload.TYPE, ChiselTablesPayload.CODEC);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            ChiselTablesPayload payload = ChiselTablesPayload.current();
            if (payload != null && ServerPlayNetworking.canSend(player, ChiselTablesPayload.TYPE)) ServerPlayNetworking.send(player, payload);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleChisels.on()) return;
            var book = List.of(com.simplechisels.guide.ChiselsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplechisels.guide.ChiselsGuide.installQuests(loader.getConfigDir());
    }
}
