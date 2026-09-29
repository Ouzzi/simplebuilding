package com.simplebuilding.forge;

import com.simplebuilding.data.ModDataTables;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;

/** Forge wiring of {@link ModDataTables}: the reload listener and the datapack sync to clients. */
public final class ForgeDataTables {

    private ForgeDataTables() {
    }

    public static void register() {
        AddReloadListenerEvent.BUS.addListener((AddReloadListenerEvent event) -> event.addListener(ModDataTables.reloadListener()));
        OnDatapackSyncEvent.BUS.addListener((OnDatapackSyncEvent event) -> event.getPlayers().forEach(ModDataTables::sync));
    }
}
