package com.simplebuilding.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The pads, pressure plates and Spawn Elytra of the required, bundled sub-mod Simple Pads by id (b-pads01, like {@link MachineRefs}): SimpleBuilding imports none
 * of its classes (principle 3), tabs, tests and the test centre find the parts in the registry. Call after registration; an
 * unregistered id gives air.
 */
public final class PadRefs {
    public static final String NAMESPACE = com.simplebuilding.version.McVersion.PADS_NAMESPACE;

    private PadRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(id(path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** A block entity type of the module ({@code "launchpad_be"}); null if it has none under that id. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType(String path) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id(path));
    }

    /** An item component type of the module ({@code "flight_time"}); null if it has none under that id. */
    @SuppressWarnings("unchecked")
    public static <T> net.minecraft.core.component.DataComponentType<T> component(String path) {
        return (net.minecraft.core.component.DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id(path));
    }
}
