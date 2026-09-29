package com.simplebuilding.platform;

import com.simplebuilding.data.ModDataTables;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

/** Fabric wiring of {@link ModDataTables}: the reload listener and the datapack sync to clients. */
public final class FabricDataTables {

    private FabricDataTables() {
    }

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(ModDataTables.LISTENER_ID, ModDataTables.reloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> ModDataTables.sync(player));
    }
}
