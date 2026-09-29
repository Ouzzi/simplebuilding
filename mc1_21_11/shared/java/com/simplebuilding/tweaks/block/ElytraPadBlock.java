package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Elytra-Pad I-V: gibt im Bereich der Stufe eine Spawn-Elytra und laedt sie auf (siehe {@link PadTiers}). */
public class ElytraPadBlock extends WaterloggedPadBlock {
    public static final MapCodec<ElytraPadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(ElytraPadBlock::getTier)
    ).apply(i, ElytraPadBlock::new));

    private final int tier;

    public ElytraPadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(0, 0, 0, 16, 2, 16), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    /**
     * Nur im Client: ein Aufwind ueber dem Pad - ab und zu steigt ein weisses Woelkchen, ueber der
     * Boost-Saeule, auf (Immersion 2026-09-28). Unter Wasser Blasen statt Wolken.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        level.addParticle(state.getValue(WATERLOGGED) ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.CLOUD,
                x, pos.getY() + 0.2, z, 0.0, 0.05, 0.0);
    }

    @Override
    protected boolean isRedstoneControlled() {
        return true;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElytraPadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TweaksBlockEntities.ELYTRA_PAD, ElytraPadBlockEntity::serverTick);
    }
}
