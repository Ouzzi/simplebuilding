package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The furnaces by id (split plan stage 4). On 26.3 they belong to the required, bundled sub-mod Simple Ovens
 * ({@code simpleovens:*}) and SimpleBuilding imports none of its classes (principle 3): tabs, JEI pages, Jade, the test
 * centre, datagen and the tests find them in the registry. On 26.2 ({@link McVersion#OVENS_IN_SB}) they are
 * SimpleBuilding's own blocks under {@code simplebuilding:*}, so the same calls work on both lines. Call after
 * registration (registry lookups); an unregistered id gives air.
 */
public final class OvenRefs {
    public static final List<String> TIERS = List.of("reinforced", "netherite", "enderite");
    public static final List<String> FAMILIES = List.of("furnace", "smoker", "blast_furnace");

    private OvenRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.OVENS_NAMESPACE, path);
    }

    /** A furnace block by path ({@code "netherite_smoker"}). */
    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(id(path));
    }

    /** A furnace or furnace minecart item by path. */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** A furnace minecart entity type ({@code "enderite_furnace_minecart"}). */
    public static EntityType<?> entityType(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(id(path));
    }

    /** The block entity type of a family ({@code "furnace"}, {@code "smoker"}, {@code "blast_furnace"}). */
    public static BlockEntityType<?> blockEntityType(String family) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id(McVersion.OVENS_IN_SB ? "mod_" + family : family));
    }

    /** 1 Reinforced, 2 Netherite, 3 Enderite for one of the nine furnaces; 0 for any other block. */
    public static int tier(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (!McVersion.OVENS_NAMESPACE.equals(key.getNamespace())) return 0;
        for (int t = 0; t < TIERS.size(); t++) {
            for (String family : FAMILIES) {
                if (key.getPath().equals(TIERS.get(t) + "_" + family)) return t + 1;
            }
        }
        return 0;
    }

    /** Speed against the Vanilla machine as shipped (2, 4, 8); 1 for any other block. */
    public static int speedFactor(BlockState state) {
        return switch (tier(state.getBlock())) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 8;
            default -> 1;
        };
    }
}
