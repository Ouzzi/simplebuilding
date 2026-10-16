package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Sculk-Kiefer (Besitzer N24, N34): lautlose Falle. Wer hineingeraet, wird staerker gebremst als im Spinnennetz
 * ({@link #STUCK}) und bekommt Dunkelheit; die offene Kiefer klappt beim ersten Opfer zu ({@link #SNAPPED}) und beisst
 * einmal ({@link #BITE_DAMAGE}). Danach bleibt sie geschlossen und still, solange jemand drinsteht - kein weiterer
 * Schaden, kein Auf-und-Zu. Erst {@link #REOPEN_TICKS} nachdem niemand mehr drin ist, oeffnet sie sich wieder.
 * Das Zuschnappen klingt wie ein Sculk-Sensor mit einem leisen Shrieker-Schrei darueber (N65, Sound nie vorher vorhanden:
 * die Falle war von Anfang an lautlos); Oeffnen und Gehen bleiben still (Klangtyp {@code EMPTY}). Der Ton ist ein reiner
 * Klang, kein Vibrations-Ereignis: Wardens hoeren ihn nicht. Wardens ignoriert sie, Zuschauer auch.
 */
public class SculkJawBlock extends WebBlock {
    public static final BooleanProperty SNAPPED = BlockStateProperties.TRIGGERED;
    /** Schaden des einen Bisses: 1 Herz. */
    public static final float BITE_DAMAGE = 2.0F;
    /** Ticks zwischen zwei Pruefungen der geschlossenen Kiefer; ist dann niemand mehr drin, oeffnet sie sich. */
    public static final int REOPEN_TICKS = 20;
    /** N65: Schliess-Sound (Sculk-Sensor-Klick, tief) plus leiser Shrieker-Schrei; Lautstaerke/Tonhoehe als Konstanten fuer die Spieltests. */
    public static final float CLOSE_CLICK_VOLUME = 1.0F;
    public static final float CLOSE_CLICK_PITCH = 0.6F;
    public static final float CLOSE_SHRIEK_VOLUME = 0.3F;
    public static final float CLOSE_SHRIEK_PITCH = 1.6F;
    /** Bremse in der Kiefer (Spinnennetz: 0,25 / 0,05). */
    public static final Vec3 STUCK = new Vec3(0.06, 0.015, 0.06);
    /**
     * Besitzer N38/N41: Dunkelheit (seit N41 statt Blindheit), die mit der Verweildauer exponentiell waechst. Nach {@code t} Sekunden in der Kiefer haelt
     * sie {@code d(t) = min(60, max(3, 3 * (5/3)^(t - 2)))} Sekunden an: raus nach 2 s -> 3 s, nach 3 s -> 5 s,
     * nach 4 s -> 8,3 s, nach 5 s -> 13,9 s, ab knapp 8 s Verweilen das Maximum von 60 s. Waehrend man drinsteht, wird sie
     * fortlaufend auf d(t) verlaengert, also bleibt nach dem Herauskommen genau d(Verweildauer).
     */
    public static final double DARK_BASE_SECONDS = 3.0;
    public static final double DARK_GROWTH = 5.0 / 3.0;
    public static final double DARK_MAX_SECONDS = 60.0;
    /** Verweildauer je Lebewesen: erster und letzter Tick in einer Kiefer (nur Server, schwach referenziert). */
    private static final java.util.Map<LivingEntity, long[]> DWELL = new java.util.WeakHashMap<>();
    /**
     * Besitzer N37: Bewegungsbild zwischen offen und zu. 0 = in Ruhe (offen oder zu, je nach {@link #SNAPPED}); 2 und 1 sind
     * die Zwischenbilder, die beim Zuschnappen (je {@link #CLOSE_FRAME_TICKS}) und Oeffnen (je {@link #OPEN_FRAME_TICKS})
     * abgezaehlt werden.
     */
    public static final IntegerProperty MOTION = IntegerProperty.create("motion", 0, 2);
    public static final int MOTION_FRAMES = 2;
    public static final int CLOSE_FRAME_TICKS = 1;
    public static final int OPEN_FRAME_TICKS = 3;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public SculkJawBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(SNAPPED, false).setValue(MOTION, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** N37 (owner 2026-10-11): only on a block whose top is solid and full - not on slabs, fences, glass panes or air. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP, SupportType.FULL);
    }

    /** Loses its floor: gone (like a carpet), no drop of its own beyond the normal loot. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effects, boolean intersects) {
        if (entity instanceof Warden || entity.isSpectator()) {
            return;
        }
        entity.makeStuckInBlock(state, STUCK);
        if (level instanceof ServerLevel server) {
            bite(state, server, pos, entity);
            if (entity instanceof LivingEntity living) {
                darken(server, living);
            }
        }
    }

    /** Dunkelheit in Ticks nach {@code dwellTicks} Ticks in der Kiefer (Formel an {@link #DARK_BASE_SECONDS}). */
    public static int darknessTicks(long dwellTicks) {
        double seconds = DARK_BASE_SECONDS * Math.pow(DARK_GROWTH, dwellTicks / 20.0 - 2.0);
        return (int) Math.round(Math.min(DARK_MAX_SECONDS, Math.max(DARK_BASE_SECONDS, seconds)) * 20.0);
    }

    /** Zaehlt die Verweildauer (ein Tick Luecke beendet sie) und verlaengert die Dunkelheit auf {@link #darknessTicks}. */
    private static void darken(ServerLevel level, LivingEntity living) {
        long now = level.getGameTime();
        long[] dwell = DWELL.get(living);
        if (dwell == null || now - dwell[1] > 2) {
            dwell = new long[] {now, now};
            DWELL.put(living, dwell);
        }
        dwell[1] = now;
        int ticks = darknessTicks(now - dwell[0]);
        MobEffectInstance dark = living.getEffect(MobEffects.DARKNESS);
        if (dark == null || dark.getDuration() < ticks - 10) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false, true));
        }
    }

    /**
     * Beisst zu, wenn die Kiefer offen ist und {@code entity} ein beissbares Lebewesen: die Kiefer schliesst sich und
     * bleibt zu. Eine geschlossene Kiefer beisst nicht. Oeffentlich fuer die Spieltests.
     */
    public static boolean bite(BlockState state, ServerLevel level, BlockPos pos, Entity entity) {
        if (state.getValue(SNAPPED) || !isVictim(entity)) {
            return false;
        }
        // Besitzer N37: Zuschnappen als kurze Bildfolge (MOTION 2 -> 1 -> 0 je Tick), danach bleibt sie zu.
        level.setBlock(pos, state.setValue(SNAPPED, true).setValue(MOTION, MOTION_FRAMES), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), CLOSE_FRAME_TICKS);
        level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, CLOSE_CLICK_VOLUME, CLOSE_CLICK_PITCH);
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, CLOSE_SHRIEK_VOLUME, CLOSE_SHRIEK_PITCH);
        ((LivingEntity) entity).hurtServer(level, level.damageSources().generic(), BITE_DAMAGE);
        return true;
    }

    private static boolean isVictim(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && !(living instanceof Warden);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int motion = state.getValue(MOTION);
        boolean snapped = state.getValue(SNAPPED);
        if (motion > 0) {
            // Naechstes Bild der Bewegung; am Ende der Schliessbewegung beginnt die Wache, ob noch jemand drin ist.
            level.setBlock(pos, state.setValue(MOTION, motion - 1), Block.UPDATE_CLIENTS);
            if (motion - 1 > 0) {
                level.scheduleTick(pos, this, snapped ? CLOSE_FRAME_TICKS : OPEN_FRAME_TICKS);
            } else if (snapped) {
                level.scheduleTick(pos, this, REOPEN_TICKS);
            }
            return;
        }
        if (!snapped) {
            return;
        }
        if (level.getEntitiesOfClass(LivingEntity.class, SHAPE.bounds().move(pos), SculkJawBlock::isVictim).isEmpty()) {
            level.setBlock(pos, state.setValue(SNAPPED, false).setValue(MOTION, MOTION_FRAMES), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, OPEN_FRAME_TICKS);
        } else {
            // Noch jemand drin: geschlossen bleiben (Besitzer N34, kein Flackern vor jedem Biss).
            level.scheduleTick(pos, this, REOPEN_TICKS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNAPPED, MOTION);
    }
}
