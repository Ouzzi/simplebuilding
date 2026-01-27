// ModChestBlock.java
package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class ModChestBlock extends ChestBlock {

    public enum Type { REINFORCED, NETHERITE }
    private final Type type;

    public ModChestBlock(Settings settings, Type type) {
        super(
                () -> ModBlockEntities.MOD_CHEST_BE,
                SoundEvents.BLOCK_CHEST_OPEN,
                SoundEvents.BLOCK_CHEST_CLOSE,
                settings
        );
        this.type = type;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ModChestBlockEntity(pos, state);
    }
}