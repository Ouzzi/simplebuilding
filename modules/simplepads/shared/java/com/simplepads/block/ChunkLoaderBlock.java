package com.simplepads.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplepads.block.entity.ChunkLoaderBlockEntity;
import com.simplepads.block.entity.PadsBlockEntities;
import com.simplepads.util.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Chunk-Loader (Simple Tweaks) in drei Stufen: I haelt nur den eigenen Chunk, II (Netherit) dazu die
 * vier Nachbarn im Kreuz (5 Chunks), III (Enderit) die 3x3 Chunks darum. Erzwingen und Freigeben
 * regelt {@link ChunkLoaderBlockEntity}.
 *
 * <p>Drei sichtbare Zustaende (a-chunk36, Besitzer 12.11.2026): <b>aktiv</b> (haelt Chunks: Spirale glimmt, Portal-Funken),
 * <b>matt</b> (Besitzer offline, Dimension gesperrt, Pads aus) und <b>abgeschaltet</b> ({@link #DISABLED}: Redstone-Signal
 * oder Limit je Spieler erreicht - eigene Textur: dieselbe Platte ausgeschaltet, Spirale und Sterne dunkel, kein Riss
 * (a-chunk36b) - und wenige kaputte Partikel).
 */
public class ChunkLoaderBlock extends PadBlock {
    public static final MapCodec<ChunkLoaderBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(ChunkLoaderBlock::getTier)
    ).apply(i, ChunkLoaderBlock::new));

    public static final int MAX_TIER = 3;

    /**
     * Haelt gerade Chunks (Immersion 2026-09-28): leuchtende Textur, heller, schwebende Portal-Funken
     * und das Summen eines Seelenankers; aus = matte Platte. Gesetzt von
     * {@link ChunkLoaderBlockEntity#update}.
     */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * Abgeschaltet (a-chunk36): ein Redstone-Signal liegt an (wie beim Vanilla-Trichter: Signal = aus) oder der Besitzer
     * hat schon {@code tuning.chunkLoaderMaxPerPlayer} laufende Loader. Haelt nichts, eigene Textur
     * ({@code <stufe>_disabled}), Licht 1, wenige kaputte Partikel ({@link Ambient#BROKEN}). Gesetzt von
     * {@link ChunkLoaderBlockEntity#update}.
     */
    public static final BooleanProperty DISABLED = BooleanProperty.create("disabled");

    /** Was {@link #animateTick} zeigt: nichts (matt), Portal-Funken (aktiv) oder kaputte Partikel (abgeschaltet). */
    public enum Ambient { IDLE, ACTIVE, BROKEN }

    /** Aktiv: ein Portal-Funke in {@value #ACTIVE_SPARKS} von 3 Anzeige-Ticks (bis a-chunk36: 1 von 3). */
    public static final int ACTIVE_SPARKS = 2;
    /** Abgeschaltet: je ein Rauchwoelkchen, Elektro-Funke bzw. stotternder Portal-Funke in 1 von so vielen Anzeige-Ticks. */
    public static final int BROKEN_SMOKE_ONE_IN = 16, BROKEN_SPARK_ONE_IN = 32, BROKEN_SPUTTER_ONE_IN = 40;

    private final int tier;

    public ChunkLoaderBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 2, 15), PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE);
        this.tier = Math.max(1, Math.min(MAX_TIER, tier));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(DISABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, DISABLED);
    }

    /** Lichtstaerke: eingeschaltet die der Stufe, matt nur ein Glimmen, abgeschaltet fast dunkel. */
    public static int lightLevel(BlockState state, int active) {
        if (state.hasProperty(DISABLED) && state.getValue(DISABLED)) {
            return 1;
        }
        return state.hasProperty(ACTIVE) && !state.getValue(ACTIVE) ? 3 : active;
    }

    /** Welche Partikel der Zustand zeigt (abgeschaltet geht vor aktiv). */
    public static Ambient ambient(BlockState state) {
        if (state.hasProperty(DISABLED) && state.getValue(DISABLED)) {
            return Ambient.BROKEN;
        }
        return state.hasProperty(ACTIVE) && state.getValue(ACTIVE) ? Ambient.ACTIVE : Ambient.IDLE;
    }

    /**
     * Nur im Client. Aktiv: aufsteigende Portal-Funken ueber der Platte (seit a-chunk36 doppelt so oft) und selten das
     * Summen eines Seelenankers (wie Vanillas Seelenanker: 1 in 100 Ticks). Abgeschaltet: wenige kaputte Partikel - ein
     * Rauchwoelkchen, ab und zu ein Elektro-Funke und ein Portal-Funke, der kaum steigt und verlischt.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        switch (ambient(state)) {
            case ACTIVE -> {
                if (random.nextInt(3) < ACTIVE_SPARKS) {
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.15,
                            pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
                }
                if (random.nextInt(100) == 0) {
                    level.playLocalSound(pos, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.3f, false);
                }
            }
            case BROKEN -> {
                double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
                if (random.nextInt(BROKEN_SMOKE_ONE_IN) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.15, z, 0.0, 0.01, 0.0);
                }
                if (random.nextInt(BROKEN_SPARK_ONE_IN) == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.14, z,
                            (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
                }
                if (random.nextInt(BROKEN_SPUTTER_ONE_IN) == 0) {
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, x, pos.getY() + 0.12, z, 0.0, 0.004, 0.0);
                }
            }
            default -> {
            }
        }
    }

    /** Redstone wirkt sofort (nicht erst bei der naechsten Pruefung alle 100 Ticks). */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity loader) {
            loader.redstoneChanged(serverLevel);
        }
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
        return level.isClientSide() ? null : createTickerHelper(type, PadsBlockEntities.CHUNK_LOADER, ChunkLoaderBlockEntity::serverTick);
    }
}
