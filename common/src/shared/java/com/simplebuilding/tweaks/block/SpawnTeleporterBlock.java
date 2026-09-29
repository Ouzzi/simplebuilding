package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
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
 * Spawn-Teleporter, drei Stufen (Besitzer 2026-09-28, wie das Launchpad): stillstehen teleportiert. Die
 * Stufen unterscheiden sich nur in der Wartezeit - I 50 s, II 20 s, III 5 s
 * ({@code SpawnTeleporterBlockEntity#requiredTicks}).
 *
 * <p><b>Ziel</b> (Besitzer 2026-09-29, jede Stufe): der eigene Spawn des Spielers (Bett oder
 * Seelenanker, ohne Ladung zu verbrauchen), ohne einen gueltigen eigenen Spawn das Spawn-Ziel
 * ({@code worldspawn setspawn1}, sonst Weltspawn). Bekommt der Teleporter ein Redstone-Signal, schaltet er
 * nicht ab wie die anderen Pads, sondern springt immer zum Spawn-Ziel/Weltspawn. Beide Ziele klingen und
 * sehen verschieden aus (SpawnTeleporterBlockEntity.Destination): eigener Spawn violett mit
 * Seelenanker-Klaengen, Weltspawn weiss-gold mit Glocke und Leuchtfeuer.
 */
public class SpawnTeleporterBlock extends WaterloggedPadBlock {
    public static final int ENDERITE_TIER = 3;
    /** Hoechste Stufe. */
    public static final int MAX_TIER = 3;
    public static final MapCodec<SpawnTeleporterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(SpawnTeleporterBlock::getTier)
    ).apply(i, SpawnTeleporterBlock::new));

    /** Jemand steht still und laedt gerade den Sprung auf (gesetzt von {@link SpawnTeleporterBlockEntity#serverTick}). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;

    public SpawnTeleporterBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /** Redstone wechselt nur das Ziel (Weltspawn statt eigener Spawn), abgeschaltet wird nichts. */
    @Override
    protected boolean switchesOffWithRedstone() {
        return false;
    }

    /**
     * Nur im Client: im Leerlauf treiben ab und zu Funken ueber die Platte - violette Portalfunken,
     * solange er zum eigenen Spawn springt, weisse Endstab-Funken mit Redstone-Signal (Weltspawn).
     * Laedt jemand auf ({@link #ACTIVE}), kreisen die Funken dicht am Rand der Platte.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean world = level.hasNeighborSignal(pos);
        ParticleOptions spark = world ? ParticleTypes.END_ROD : ParticleTypes.PORTAL;
        if (state.getValue(ACTIVE)) {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2.0;
                level.addParticle(world ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                        pos.getX() + 0.5 + Math.cos(a) * 0.45, pos.getY() + 0.1, pos.getZ() + 0.5 + Math.sin(a) * 0.45,
                        0.0, 0.03 + random.nextDouble() * 0.04, 0.0);
            }
            return;
        }
        if (random.nextInt(world ? 10 : 4) == 0) {
            level.addParticle(spark, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + (world ? 0.15 : 0.4),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    world ? 0.0 : (random.nextDouble() - 0.5) * 0.3, world ? 0.02 : -0.2, world ? 0.0 : (random.nextDouble() - 0.5) * 0.3);
        }
    }

    public int getTier() {
        return tier;
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
        return new SpawnTeleporterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TweaksBlockEntities.SPAWN_TELEPORTER,
                level.isClientSide() ? SpawnTeleporterBlockEntity::clientTick : SpawnTeleporterBlockEntity::serverTick);
    }
}
