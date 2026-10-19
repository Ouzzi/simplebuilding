package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ContainerCounts;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Item components (split stage 8b): {@code container_counts}, the real counts of a tier shulker box item above 99 per
 * slot, which {@code minecraft:container} does not save (it holds a readable copy of 99). Synced, or the creative
 * inventory would lose them when moving the stack. Was {@code simplebuilding:container_counts} (id move). Registered in
 * the DATA_COMPONENT_TYPE phase (Fabric directly).
 */
public final class StorageComponents {
    public static final String CONTAINER_COUNTS_ID = "container_counts";
    public static DataComponentType<ContainerCounts> CONTAINER_COUNTS;

    public static void register() {
        CONTAINER_COUNTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleStorage.id(CONTAINER_COUNTS_ID),
                DataComponentType.<ContainerCounts>builder().persistent(ContainerCounts.CODEC).networkSynchronized(ContainerCounts.STREAM_CODEC).build());
    }

    private StorageComponents() {}
}
