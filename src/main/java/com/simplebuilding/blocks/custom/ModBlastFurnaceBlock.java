// ModBlastFurnaceBlock.java
package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import net.minecraft.block.BlastFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ModBlastFurnaceBlock extends BlastFurnaceBlock {
    private final float speedMultiplier;

    public ModBlastFurnaceBlock(Settings settings, float speedMultiplier) {
        super(settings);
        this.speedMultiplier = speedMultiplier;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ModBlastFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // FIX: Prüfen ob ServerWorld, dann casten und Lambda statt Methodenreferenz nutzen
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.MOD_BLAST_FURNACE_BE, (w, pos, st, be) ->
            ModBlastFurnaceBlockEntity.tick((ServerWorld) w, pos, st, be));
    }
}