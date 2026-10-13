package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Rissiges Eis (Besitzer N25): wer darauf steht, laesst es reissen - jede Sekunde mit jemandem darauf eine Stufe mehr
 * (Modelle auf Vanillas {@code frosted_ice_0..3}, N53 wieder wie vor N52), nach der letzten Stufe bricht es weg (ohne Wasserquelle, Nachtrag 35). Steht niemand mehr darauf, bleibt der erreichte Riss stehen. Sonst verhaelt es sich wie Eis
 * (Licht schmilzt es, abgebaut hinterlaesst es anders als Eis kein Wasser).
 *
 * <p>N53 (Besitzer 2026-10-13): zwischen Eis getarnt - Eis und rissiges Eis blenden die gemeinsamen Flaechen aus wie
 * Eis neben Eis ({@link #hidesFaceTowards}, HalfTransparentBlockMixin), sonst zeichneten sich die Innenflaechen als
 * dunkler Rand ab. An keine Seite eines rissigen Eisblocks laesst sich ein Block setzen ({@link #blocksPlacementAt}).
 */
public class CrackedIceBlock extends IceBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** Ticks je Rissstufe: vier Stufen, also nach gut vier Sekunden Draufstehen Wasser. */
    public static final int TICKS_PER_CRACK = 20;

    public CrackedIceBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity && !entity.isSpectator()
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, TICKS_PER_CRACK);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isStoodOn(level, pos)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.7F, 0.6F + 0.15F * age);
            level.scheduleTick(pos, this, TICKS_PER_CRACK);
        } else {
            // Einbrechen: das Eis bricht weg und hinterlaesst keine Wasserquelle (Nachtrag 35).
            level.destroyBlock(pos, false);
        }
    }

    /**
     * N53: Eis und rissiges Eis gelten fuer das Ausblenden der Flaechen als derselbe Block (Vanilla tut das nur fuer
     * gleiche Bloecke), damit rissiges Eis in einer Eisflaeche keine sichtbaren Innenflaechen/Raender hat.
     */
    public static boolean hidesFaceTowards(Block self, BlockState neighbor) {
        Block cracked = com.simplebuilding.blocks.ModBlocks.CRACKED_ICE;
        if (cracked == null || !(self == net.minecraft.world.level.block.Blocks.ICE || self == cracked)) {
            return false;
        }
        return neighbor.is(net.minecraft.world.level.block.Blocks.ICE) || neighbor.is(cracked);
    }

    /** N53: an keine der sechs Seiten eines rissigen Eisblocks darf ein Block gesetzt werden (BlockItemMixin). */
    public static boolean blocksPlacementAt(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Block cracked = com.simplebuilding.blocks.ModBlocks.CRACKED_ICE;
        if (cracked == null) {
            return false;
        }
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(cracked)) {
                return true;
            }
        }
        return false;
    }

    /** Steht ein Lebewesen (kein Zuschauer) auf der Oberseite? */
    public static boolean isStoodOn(Level level, BlockPos pos) {
        AABB top = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.25, pos.getZ() + 1.0);
        return !level.getEntitiesOfClass(LivingEntity.class, top, entity -> !entity.isSpectator()).isEmpty();
    }

    /** Abgebaut hinterlaesst es kein Wasser (anders als Eis): nur Statistik, Erschoepfung und Drops. */
    // Kein @Override: auf der 26.2-Linie hat die Methode eine andere Signatur (Port-Run).
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack destroyedWith) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, blockEntity, player, destroyedWith);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
