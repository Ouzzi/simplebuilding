package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateBlock;
import com.simplestorage.crate.CrateWood;
import com.simplestorage.table.StorageCraftingTableBlock;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The blocks, with the properties SimpleBuilding gave them before split stage 8a. */
public final class StorageBlocks {
    /** Crates in creative-tab order (Vanilla woods, then the End woods with Simple End). */
    public static final Map<CrateWood, CrateBlock> CRATES = new EnumMap<>(CrateWood.class);
    public static Block STORAGE_CRAFTING_TABLE;

    /** {@code simpleEnd}: Simple End is loaded (only then the Ashwood and Duskwood crates exist, as before the move). */
    public static void register(boolean simpleEnd) {
        for (CrateWood wood : CrateWood.values()) {
            if (wood.external() && !simpleEnd) continue;
            CRATES.put(wood, block(wood.crateId(), wood.template(),
                    p -> new CrateBlock(wood, (wood.external() ? p.mapColor(wood.endColor()) : p).strength(1.5F).noOcclusion())));
        }
        STORAGE_CRAFTING_TABLE = block(SimpleStorage.STORAGE_CRAFTING_TABLE, Blocks.CRAFTING_TABLE, StorageCraftingTableBlock::new);
    }

    public static Block[] crates() {
        return CRATES.values().toArray(Block[]::new);
    }

    /** Crates burn like planks (not the Nether woods); call once after registration. */
    public static void registerFlammability() {
        com.simplestorage.mixin.FireBlockInvoker fire = (com.simplestorage.mixin.FireBlockInvoker) Blocks.FIRE;
        CRATES.forEach((wood, crate) -> {
            if (!wood.nether()) fire.simplestorage$setFlammable(crate, 5, 20);
        });
    }

    private static <B extends Block> B block(String name, Block base, Function<BlockBehaviour.Properties, B> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleStorage.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key)));
    }

    private StorageBlocks() {}
}
