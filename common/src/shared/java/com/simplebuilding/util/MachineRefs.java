package com.simplebuilding.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The parts of the required, bundled sub-mod Simple Machines by id (MACH32-MOD, like {@link OvenRefs}): SimpleBuilding imports none
 * of its classes (principle 3), tabs, tests and the test centre find the parts in the registry. Call after registration; an
 * unregistered id gives air.
 */
public final class MachineRefs {
    public static final String NAMESPACE = "simplemachines";

    private MachineRefs() {
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

    /** Stage 3: an entity type of the module ({@code "reinforced_hopper_minecart"}). */
    public static net.minecraft.world.entity.EntityType<?> entityType(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(id(path));
    }

    /** Stage 3: a block entity type of the module ({@code "hopper"}); null if it has none under that id. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType(String path) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id(path));
    }
}
