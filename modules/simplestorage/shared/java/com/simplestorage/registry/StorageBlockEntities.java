package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.chest.TieredShulkerBoxBlockEntity;
import com.simplestorage.chest.TrappedCopperChestBlockEntity;
import com.simplestorage.crate.CrateBlockEntity;
import com.simplestorage.table.StorageCraftingTableBlockEntity;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity types are built per loader (the constructors differ, {@link Types}) and registered here. */
public final class StorageBlockEntities {
    public static BlockEntityType<CrateBlockEntity> CRATE;
    public static BlockEntityType<StorageCraftingTableBlockEntity> STORAGE_CRAFTING_TABLE;
    /** Split stage 8b: ids unchanged apart from the namespace ({@code tiered_chest}, {@code trapped_copper_chest}, {@code tiered_shulker_box}). */
    public static BlockEntityType<TieredChestBlockEntity> TIERED_CHEST;
    public static BlockEntityType<TrappedCopperChestBlockEntity> TRAPPED_COPPER_CHEST;
    public static BlockEntityType<TieredShulkerBoxBlockEntity> TIERED_SHULKER_BOX;
    /** Split stage 8c-1: the placed bundle ({@code placed_bundle}). */
    public static BlockEntityType<com.simplestorage.bundle.PlacedBundleBlockEntity> PLACED_BUNDLE;

    /** The loader's {@code BlockEntityType} constructor. */
    public interface Types {
        <T extends BlockEntity> BlockEntityType<T> create(BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<Block> blocks);
    }

    public static void register(Types types) {
        CRATE = register("crate", types.create(CrateBlockEntity::new, Set.of(StorageBlocks.crates())));
        STORAGE_CRAFTING_TABLE = register(SimpleStorage.STORAGE_CRAFTING_TABLE,
                types.create(StorageCraftingTableBlockEntity::new, Set.of(StorageBlocks.STORAGE_CRAFTING_TABLE)));
        TIERED_CHEST = register("tiered_chest", types.create(TieredChestBlockEntity::new, Set.of(StorageBlocks.tieredChests())));
        TRAPPED_COPPER_CHEST = register("trapped_copper_chest", types.create(TrappedCopperChestBlockEntity::new, Set.of(StorageBlocks.trappedCopperChests())));
        TIERED_SHULKER_BOX = register("tiered_shulker_box", types.create(TieredShulkerBoxBlockEntity::new, Set.of(StorageBlocks.shulkerBoxes())));
        PLACED_BUNDLE = register("placed_bundle", types.create(com.simplestorage.bundle.PlacedBundleBlockEntity::new, Set.of(StorageBlocks.PLACED_BUNDLE)));
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String path, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleStorage.id(path), type);
    }

    private StorageBlockEntities() {}
}
