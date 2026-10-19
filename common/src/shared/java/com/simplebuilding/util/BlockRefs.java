package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * SimpleBuilding's former building blocks without a home of their own, by id (b-blocksmove,
 * docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md). On 26.3 they belong to the required, bundled sub-mod Simple Blocks
 * ({@code simpleblocks:*}) and SimpleBuilding imports none of its classes (principle 3): tabs, the hammer, the chisel,
 * world generation, JEI, datagen and the tests find them in the registry. On 26.2 ({@link McVersion#BLOCKS_IN_SB}) the
 * gravity blocks are SimpleBuilding's own under {@code simplebuilding:*} (the nature variants do not exist there), so the
 * same calls work on both lines. Call after registration (registry lookups); never from a static initializer. An
 * unregistered id gives air.
 */
public final class BlockRefs {
    public static final String SUSPENDED_SAND = "suspended_sand";
    public static final String SUSPENDED_GRAVEL = "suspended_gravel";
    public static final String LEVITATING_SAND = "levitating_sand";
    public static final String LEVITATING_GRAVEL = "levitating_gravel";
    public static final String DIRT_SLAB = "dirt_slab";
    public static final String GRASS_SLAB = "grass_slab";
    public static final String SAND_SLAB = "sand_slab";
    public static final String GRAVEL_SLAB = "gravel_slab";
    public static final String CRACKED_ICE = "cracked_ice";
    public static final String CHISELED_PACKED_ICE = "chiseled_packed_ice";
    public static final String CHISELED_BLUE_ICE = "chiseled_blue_ice";
    public static final String NAUTILUS_SHELL_BLOCK = "nautilus_shell_block";
    public static final String SCARLET_FROGLIGHT = "scarlet_froglight";
    public static final String AQUA_FROGLIGHT = "aqua_froglight";
    public static final String AZURE_FROGLIGHT = "azure_froglight";
    /** The four gravity blocks (both lines). */
    public static final List<String> GRAVITY = List.of(SUSPENDED_SAND, SUSPENDED_GRAVEL, LEVITATING_SAND, LEVITATING_GRAVEL);
    /** The eleven nature variants (only with {@link McVersion#NATURE_VARIANTS}). */
    public static final List<String> NATURE = List.of(DIRT_SLAB, GRASS_SLAB, SAND_SLAB, GRAVEL_SLAB, CRACKED_ICE, CHISELED_PACKED_ICE,
            CHISELED_BLUE_ICE, NAUTILUS_SHELL_BLOCK, SCARLET_FROGLIGHT, AQUA_FROGLIGHT, AZURE_FROGLIGHT);
    /** The rising block of levitating sand and gravel (entity type path). */
    public static final String LEVITATING_ENTITY = "levitating_block";

    private BlockRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.BLOCKS_NAMESPACE, path);
    }

    /** One of the blocks by path ({@code "cracked_ice"}); air when it is not registered. */
    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(id(path));
    }

    /** Its item; air when it is not registered. */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** {@code true} when {@code path} is registered (the nature variants exist only on 26.3). */
    public static boolean present(String path) {
        return BuiltInRegistries.BLOCK.containsKey(id(path));
    }

    /** {@code true} for one of the moved blocks given by path (no air confusion: air is never one of them). */
    public static boolean is(Block block, String path) {
        return block != Blocks.AIR && block == block(path);
    }

    /** {@code true} for the rising block of levitating sand/gravel (a {@code FallingBlockEntity} subclass of Simple Blocks on 26.3). */
    public static boolean isRising(net.minecraft.world.entity.Entity entity) {
        return id(LEVITATING_ENTITY).equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /** {@code true} for one of the moved items given by path. */
    public static boolean is(Item item, String path) {
        return item != Items.AIR && item == item(path);
    }
}
