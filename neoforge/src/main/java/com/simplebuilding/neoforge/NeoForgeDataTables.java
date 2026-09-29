package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.data.ModDataTables;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** NeoForge wiring of {@link ModDataTables}: the reload listener and the datapack sync to clients. */
@EventBusSubscriber(modid = Simplebuilding.MOD_ID)
public final class NeoForgeDataTables {

    private NeoForgeDataTables() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ModDataTables.LISTENER_ID, ModDataTables.reloadListener());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(ModDataTables::sync);
    }
}
