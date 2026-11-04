package com.simplepads.block.entity;

import com.simplepads.SimplePads;
import com.simplepads.block.PadsBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block-Entity-Typen der Simple-Tweaks-Platten. IDs wie in Simple Tweaks ({@code *_be}).
 * Der Konstruktor von {@link BlockEntityType} ist nicht auf jeder MC-Linie oeffentlich, deshalb
 * reicht der Loader eine {@link Factory} herein.
 */
public final class PadsBlockEntities {

    @FunctionalInterface
    public interface Factory {
        <T extends BlockEntity> BlockEntityType<T> create(BlockEntityType.BlockEntitySupplier<T> supplier, Block... blocks);
    }

    public static BlockEntityType<SpawnTeleporterBlockEntity> SPAWN_TELEPORTER;
    public static BlockEntityType<LaunchpadBlockEntity> LAUNCHPAD;
    public static BlockEntityType<ElytraPadBlockEntity> ELYTRA_PAD;
    public static BlockEntityType<FlypadBlockEntity> FLYPAD;
    public static BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER;
    public static BlockEntityType<CopperPressurePlateBlockEntity> COPPER_PRESSURE_PLATE;
    public static BlockEntityType<FilterPlateBlockEntity> FILTER_PLATE;
    public static BlockEntityType<PotionPadBlockEntity> POTION_PAD;

    private static boolean registered;

    private PadsBlockEntities() {
    }

    public static void register(Factory factory) {
        if (registered) {
            return;
        }
        registered = true;
        SPAWN_TELEPORTER = register("spawn_teleporter_be", factory.create(SpawnTeleporterBlockEntity::new,
                PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.SPAWN_TELEPORTER_TIER_2, PadsBlocks.SPAWN_TELEPORTER_TIER_3,
                PadsBlocks.SPAWN_TELEPORTER_TIER_4, PadsBlocks.ENDERITE_SPAWN_TELEPORTER));
        LAUNCHPAD = register("launchpad_be", factory.create(LaunchpadBlockEntity::new,
                PadsBlocks.LAUNCHPAD, PadsBlocks.NETHERITE_LAUNCHPAD, PadsBlocks.ENDERITE_LAUNCHPAD));
        ELYTRA_PAD = register("elytra_pad_be", factory.create(ElytraPadBlockEntity::new,
                PadsBlocks.ELYTRA_PAD, PadsBlocks.REINFORCED_ELYTRA_PAD, PadsBlocks.NETHERITE_ELYTRA_PAD,
                PadsBlocks.ENDERITE_ELYTRA_PAD, PadsBlocks.FINE_ELYTRA_PAD));
        FLYPAD = register("flypad_be", factory.create(FlypadBlockEntity::new,
                PadsBlocks.FLYPAD, PadsBlocks.REINFORCED_FLYPAD, PadsBlocks.NETHERITE_FLYPAD,
                PadsBlocks.ENDERITE_FLYPAD, PadsBlocks.STELLAR_FLYPAD));
        CHUNK_LOADER = register("chunk_loader_be", factory.create(ChunkLoaderBlockEntity::new,
                PadsBlocks.CHUNK_LOADER, PadsBlocks.NETHERITE_CHUNK_LOADER, PadsBlocks.ENDERITE_CHUNK_LOADER));
        COPPER_PRESSURE_PLATE = register("copper_pressure_plate_be", factory.create(CopperPressurePlateBlockEntity::new,
                PadsBlocks.COPPER_PRESSURE_PLATE, PadsBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WEATHERED_COPPER_PRESSURE_PLATE, PadsBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WAXED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE));
        // Simple Tweaks nannte den Typ netherite_pressure_plate_be; die Enderit-Platte teilt ihn.
        FILTER_PLATE = register("netherite_pressure_plate_be", factory.create(FilterPlateBlockEntity::new,
                PadsBlocks.NETHERITE_PRESSURE_PLATE, PadsBlocks.ENDERITE_PRESSURE_PLATE));
        POTION_PAD = register("potion_pad_be", factory.create(PotionPadBlockEntity::new,
                PadsBlocks.POTION_PAD, PadsBlocks.REINFORCED_POTION_PAD, PadsBlocks.INFUSED_POTION_PAD));
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimplePads.id(name), type);
    }
}
