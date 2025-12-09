package com.simplebuilding.block.entity.custom;

import com.simplebuilding.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public class NetheriteShulkerBlockEntity extends ShulkerBoxBlockEntity {

    public NetheriteShulkerBlockEntity(BlockPos pos, BlockState state) {
        super(DyeColor.MAGENTA, pos, state); // null = Standard Farbe (wir überschreiben die Textur eh)
    }

    @Override
    public BlockEntityType<?> getType() {
        // Wir müssen hier unseren eigenen Typ zurückgeben, sonst nutzt Minecraft den Vanilla Renderer
        return ModBlockEntities.NETHERITE_SHULKER_BE;
    }
}