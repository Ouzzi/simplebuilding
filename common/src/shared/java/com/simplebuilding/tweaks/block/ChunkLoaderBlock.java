package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * Chunk-Loader (Simple Tweaks): haelt den eigenen Chunk geladen; die Enderit-Stufe ({@code radius} 1)
 * die 3x3 Chunks darum. Neu: das Erzwingen haengt am Setzen/Entfernen des Blocks statt am Laden der
 * Block-Entity - in Simple Tweaks gab das Entladen beim Serverstopp den Chunk frei, und nach dem
 * Neustart tickte der Loader nie wieder.
 */
public class ChunkLoaderBlock extends PadBlock {
    public static final MapCodec<ChunkLoaderBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("radius").forGetter(ChunkLoaderBlock::getRadius)
    ).apply(i, ChunkLoaderBlock::new));

    private final int radius;

    public ChunkLoaderBlock(BlockBehaviour.Properties properties, int radius) {
        super(properties, Block.box(1, 0, 1, 15, 2, 15), PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE);
        this.radius = radius;
    }

    /** 0 = nur der eigene Chunk, 1 = 3x3. */
    public int getRadius() {
        return radius;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TweaksBlockEntities.CHUNK_LOADER, ChunkLoaderBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            ChunkLoaderBlockEntity.setForced(serverLevel, pos, radius, ChunkLoaderBlockEntity.enabled());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        ChunkLoaderBlockEntity.setForced(level, pos, radius, false);
    }
}
