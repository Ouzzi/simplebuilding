package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.TweaksBlocks;
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
public final class TweaksBlockEntities {

    @FunctionalInterface
    public interface Factory {
        <T extends BlockEntity> BlockEntityType<T> create(Supplier<T> supplier, Block... blocks);
    }

    /** 1.21.11: BlockEntityType.BlockEntitySupplier ist privat, daher ein eigenes Gegenstueck. */
    @FunctionalInterface
    public interface Supplier<T extends BlockEntity> {
        T create(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state);
    }

    public static BlockEntityType<SpawnTeleporterBlockEntity> SPAWN_TELEPORTER;
    public static BlockEntityType<LaunchpadBlockEntity> LAUNCHPAD;
    public static BlockEntityType<ElytraPadBlockEntity> ELYTRA_PAD;
    public static BlockEntityType<FlypadBlockEntity> FLYPAD;
    public static BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER;
    public static BlockEntityType<CopperPressurePlateBlockEntity> COPPER_PRESSURE_PLATE;
    public static BlockEntityType<FilterPlateBlockEntity> FILTER_PLATE;

    private static boolean registered;

    private TweaksBlockEntities() {
    }

    public static void register(Factory factory) {
        if (registered) {
            return;
        }
        registered = true;
        SPAWN_TELEPORTER = register("spawn_teleporter_be", factory.create(SpawnTeleporterBlockEntity::new,
                TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.SPAWN_TELEPORTER_TIER_3,
                TweaksBlocks.SPAWN_TELEPORTER_TIER_4, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER));
        LAUNCHPAD = register("launchpad_be", factory.create(LaunchpadBlockEntity::new,
                TweaksBlocks.LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD));
        ELYTRA_PAD = register("elytra_pad_be", factory.create(ElytraPadBlockEntity::new,
                TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD));
        FLYPAD = register("flypad_be", factory.create(FlypadBlockEntity::new,
                TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.NETHERITE_FLYPAD,
                TweaksBlocks.ENDERITE_FLYPAD, TweaksBlocks.STELLAR_FLYPAD));
        CHUNK_LOADER = register("chunk_loader_be", factory.create(ChunkLoaderBlockEntity::new,
                TweaksBlocks.CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER));
        COPPER_PRESSURE_PLATE = register("copper_pressure_plate_be", factory.create(CopperPressurePlateBlockEntity::new,
                TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE));
        // Simple Tweaks nannte den Typ netherite_pressure_plate_be; die Enderit-Platte teilt ihn.
        FILTER_PLATE = register("netherite_pressure_plate_be", factory.create(FilterPlateBlockEntity::new,
                TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE));
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleTweaks.id(name), type);
    }
}
