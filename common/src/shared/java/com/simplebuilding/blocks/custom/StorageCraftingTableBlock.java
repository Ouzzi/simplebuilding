package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.custom.StorageCraftingTableBlockEntity;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Storage Crafting Table (owner 2026-10-09, queue N26): a crafting table whose grid stays when the menu closes and
 * lies on its top (see {@link StorageCraftingTableBlockEntity}). Breaking it drops the table and the grid.
 */
public class StorageCraftingTableBlock extends BaseEntityBlock {
    public static final MapCodec<StorageCraftingTableBlock> CODEC = BlockCodecs.simple(StorageCraftingTableBlock::new);

    public StorageCraftingTableBlock(Properties properties) {
        super(properties);
    }

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override).
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageCraftingTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageCraftingTableBlockEntity table) {
            player.openMenu(table);
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return InteractionResult.SUCCESS;
    }
}
