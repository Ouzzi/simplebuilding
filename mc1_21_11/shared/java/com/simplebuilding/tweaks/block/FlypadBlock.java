package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.FlypadBlockEntity;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Flypad I-III: Kreativflug im Bereich der Stufe (siehe {@link PadTiers}). Traegt es gerade jemanden,
 * steht es auf {@link #ACTIVE}: hellere Textur und schimmernde Eckpfosten des Flugfelds.
 */
public class FlypadBlock extends PadBlock {
    public static final MapCodec<FlypadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(FlypadBlock::getTier)
    ).apply(i, FlypadBlock::new));

    /** Mindestens ein Spieler fliegt gerade mit diesem Pad (gesetzt von {@link FlypadBlockEntity#update}). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;

    public FlypadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(0, 0, 0, 16, 3, 16), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    /**
     * Nur im Client: solange das Pad jemanden traegt, schimmern die vier senkrechten Kanten des
     * Flugfelds (ein Endstab-Funke je paar Ticks an einer zufaelligen Kante und Hoehe) - so sieht
     * man, wo das Feld endet, ohne Text. Dazu steigt ab und zu ein Funke vom Pad auf.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (random.nextInt(4) == 0) {
            AABB area = FlypadBlockEntity.areaOf(level, pos, state);
            double x = random.nextBoolean() ? area.minX : area.maxX;
            double z = random.nextBoolean() ? area.minZ : area.maxZ;
            double y = area.minY + random.nextDouble() * (area.maxY - area.minY);
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.25,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03, 0.0);
        }
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
        return new FlypadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TweaksBlockEntities.FLYPAD, FlypadBlockEntity::serverTick);
    }
}
