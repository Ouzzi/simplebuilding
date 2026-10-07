package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.version.BlockCodecs;
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
import org.jetbrains.annotations.Nullable;

/**
 * Elytra-Pad I-III: gibt im Bereich der Stufe eine Spawn-Elytra und laedt sie auf (siehe {@link PadTiers}).
 * Versorgt es gerade jemanden, steht es auf {@link #ACTIVE} (Besitzer 2026-09-29): die Spirale leuchtet
 * windhell, der Aufwind ueber dem Pad wird dichter.
 */
public class ElytraPadBlock extends WaterloggedPadBlock {
    public static final MapCodec<ElytraPadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(ElytraPadBlock::getTier)
    ).apply(i, ElytraPadBlock::new));

    /** Mindestens ein Spieler im Bereich wird versorgt (gesetzt von {@link ElytraPadBlockEntity#serverTick}). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;

    public ElytraPadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(0, 0, 0, 16, 2, 16), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
        addFadeProperty(builder);
    }

    public int getTier() {
        return tier;
    }

    /**
     * Nur im Client: ein Aufwind ueber dem Pad - ab und zu steigt ein weisses Woelkchen, ueber der
     * Boost-Saeule, auf (Immersion 2026-09-28). Unter Wasser Blasen statt Wolken. Versorgt es jemanden
     * ({@link #ACTIVE}), steigt der Aufwind dreimal so dicht und Feuerwerksfunken ziehen mit hoch;
     * abgeschaltet (Redstone) nur ein Rauchwoelkchen.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (animateSwitchedOff(level, pos, random)) {
            return;
        }
        boolean active = state.getValue(ACTIVE);
        if (random.nextInt(active ? 3 : 8) != 0) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        boolean water = state.getValue(WATERLOGGED);
        level.addParticle(water ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.CLOUD,
                x, pos.getY() + 0.2, z, 0.0, active ? 0.09 : 0.05, 0.0);
        if (active && !water && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FIREWORK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.4, pos.getY() + 0.3,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.4, 0.0, 0.12, 0.0);
        }
    }

    @Override
    protected boolean isRedstoneControlled() {
        return true;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
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
