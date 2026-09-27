package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.version.BlockCodecs;
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
 * Chunk-Loader (Simple Tweaks) in drei Stufen: I haelt nur den eigenen Chunk, II (Netherit) dazu die
 * vier Nachbarn im Kreuz (5 Chunks), III (Enderit) die 3x3 Chunks darum. Erzwingen und Freigeben
 * regelt {@link ChunkLoaderBlockEntity}.
 */
public class ChunkLoaderBlock extends PadBlock {
    public static final MapCodec<ChunkLoaderBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(ChunkLoaderBlock::getTier)
    ).apply(i, ChunkLoaderBlock::new));

    public static final int MAX_TIER = 3;

    private final int tier;

    public ChunkLoaderBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 2, 15), PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE);
        this.tier = Math.max(1, Math.min(MAX_TIER, tier));
    }

    /** 1 = nur der eigene Chunk, 2 = Kreuz aus 5 Chunks, 3 = 3x3. */
    public int getTier() {
        return tier;
    }

    /**
     * Ob der Chunk mit dem Abstand (dx, dz) zum eigenen im Bereich der Stufe liegt: I nur (0, 0),
     * II zusaetzlich die vier Nachbarn mit gemeinsamer Kante, III alle acht Nachbarn.
     */
    public static boolean inArea(int tier, int dx, int dz) {
        return inArea(tier, dx, dz, false);
    }

    /**
     * Wie {@link #inArea(int, int, int)}; {@code doubled} = letzte Easter-Stufe
     * ({@code tweaks.easter.EasterEggs}): der Radius der Enderit-Stufe verdoppelt, also 5x5 Chunks.
     */
    public static boolean inArea(int tier, int dx, int dz, boolean doubled) {
        if (doubled) {
            return Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
        }
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        return switch (tier) {
            case 1 -> adx == 0 && adz == 0;
            case 2 -> adx + adz <= 1;
            default -> adx <= 1 && adz <= 1;
        };
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
}
