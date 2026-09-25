package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Spawn-Teleporter (Simple Tweaks): 5 s stillstehen teleportiert zum Ziel der Stufe (Spawn 1-4,
 * sonst Weltspawn). Stufe V ({@link #ENDERITE_TIER}) ist neu: Ziel ist der eigene
 * Wiedereinstiegspunkt (Bett/Anker), 3 s.
 */
public class SpawnTeleporterBlock extends WaterloggedPadBlock {
    public static final int ENDERITE_TIER = 5;
    public static final MapCodec<SpawnTeleporterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(SpawnTeleporterBlock::getTier)
    ).apply(i, SpawnTeleporterBlock::new));

    private final int tier;

    public SpawnTeleporterBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnTeleporterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TweaksBlockEntities.SPAWN_TELEPORTER,
                level.isClientSide() ? SpawnTeleporterBlockEntity::clientTick : SpawnTeleporterBlockEntity::serverTick);
    }
}
