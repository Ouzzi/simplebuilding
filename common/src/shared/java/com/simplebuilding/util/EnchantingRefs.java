package com.simplebuilding.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The enchanters of the required, bundled sub-mod Simple Enchanting by id (ENCH02 03.11., like {@link MachineRefs}): SimpleBuilding
 * imports none of its classes (principle 3); tab, test centre and guide find the tables in the registry. Call after registration
 * (SimpleBuilding runs the module's initializer first); an unregistered id gives air (NeoForge/Forge: the module is Fabric only).
 */
public final class EnchantingRefs {
    public static final String NAMESPACE = "simpleenchanting";

    private EnchantingRefs() {
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
}
