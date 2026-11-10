package com.simpleblocks.nature;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * (ABN30-7: Texturen aus Vanillas Eis abgeleitet, Variante B Trittbruch, tools/textures/ice31_2026_10_30.py), nach der letzten Stufe bricht es weg (ohne Wasserquelle, Nachtrag 35). Steht niemand mehr darauf, bleibt der erreichte Riss stehen. Sonst verhaelt es sich wie Eis
 * (Licht schmilzt es, abgebaut hinterlaesst es anders als Eis kein Wasser).
 *
 * <p>N53 (Besitzer 2026-10-13): zwischen Eis getarnt - Eis und rissiges Eis blenden die gemeinsamen Flaechen aus wie
 * Eis neben Eis ({@link #hidesFaceTowards}, CrackedIceFacesMixin), sonst zeichneten sich die Innenflaechen als
 * dunkler Rand ab. BS25-8 (Besitzer 29.10.): das N53-Platzierverbot an allen Seiten ist weg - auf und neben rissigem Eis
 * laesst sich normal bauen.
 * <p>Kleber (WD25-2/WD25-4, 10.11.): ein Schleimball (oder Honigwabe, Vanilla-Wachs) fixiert den Riss-Zustand ({@link #WAXED}):
 * kein weiteres Reissen, kein Schmelzen durch Licht. Eine Axt kratzt es wieder ab. Mit Behutsamkeit abgebaut traegt der Drop
 * Riss-Stufe und Fixierung (Block-State-Komponente), Platzieren stellt sie wieder her (Muster wie EndLeavesBlock).
 */
public class CrackedIceBlock extends IceBlock {
    public static final int MAX_AGE = 3;
    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** Ticks je Rissstufe: vier Stufen, also nach gut vier Sekunden Draufstehen Wasser. */
    public static final int TICKS_PER_CRACK = 20;

    public CrackedIceBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(WAXED, false));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        scheduleCrack(level, pos, this, entity, TICKS_PER_CRACK);
        super.stepOn(level, pos, state, entity);
    }

    /** Fixiertes (verklebtes) Eis schmilzt nicht durch Licht. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WAXED)) super.randomTick(state, level, pos, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        InteractionResult r = glue(stack, state, level, pos, player, hand);
        return r != null ? r : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return withState(state, super.getDrops(state, params));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        crack(state, level, pos, this, TICKS_PER_CRACK);
    }

    /** Eine Lebewesen betritt den Block: der naechste Riss wird eingeplant (geteilt mit {@link CrackedHardIceBlock}, ABN30-7). */
    static void scheduleCrack(Level level, BlockPos pos, Block self, Entity entity, int ticksPerCrack) {
        if (level.getBlockState(pos).getValue(WAXED)) {
            return;
        }
        if (!level.isClientSide() && entity instanceof LivingEntity && !entity.isSpectator()
                && !level.getBlockTicks().hasScheduledTick(pos, self)) {
            level.scheduleTick(pos, self, ticksPerCrack);
        }
    }

    /** Eine Rissstufe weiter, solange jemand darauf steht; nach der letzten bricht der Block weg (ohne Wasser, Nachtrag 35). */
    static void crack(BlockState state, ServerLevel level, BlockPos pos, Block self, int ticksPerCrack) {
        if (state.getValue(WAXED) || !isStoodOn(level, pos)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.7F, 0.6F + 0.15F * age);
            level.scheduleTick(pos, self, ticksPerCrack);
        } else {
            level.destroyBlock(pos, false);
        }
    }

    /** Kleber/Wachs drauf (Schleimball oder Honigwabe, ein Stueck verbraucht) bzw. Axt ab; null = nicht zustaendig. Geteilt mit {@link CrackedHardIceBlock}. */
    static InteractionResult glue(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        boolean slime = stack.is(Items.SLIME_BALL);
        if ((slime || stack.is(Items.HONEYCOMB)) && !state.getValue(WAXED)) {
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_ON, pos, 0);
            if (slime) level.playSound(player, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(WAXED, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ItemTags.AXES) && state.getValue(WAXED)) {
            level.playSound(player, pos, SoundEvents.AXE_WAX_OFF.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WAXED, false), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                stack.hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    /** Behutsamkeits-Drop traegt Riss-Stufe und Fixierung, wenn eine davon gesetzt ist (sonst stapelt er wie gehabt). */
    static List<ItemStack> withState(BlockState state, List<ItemStack> drops) {
        if (state.getValue(WAXED) || state.getValue(AGE) > 0) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == state.getBlock().asItem()) {
                    drop.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(AGE, state.getValue(AGE)).with(WAXED, state.getValue(WAXED)));
                }
            }
        }
        return drops;
    }

    /**
     * N53: Eis und rissiges Eis gelten fuer das Ausblenden der Flaechen als derselbe Block (Vanilla tut das nur fuer
     * gleiche Bloecke), damit rissiges Eis in einer Eisflaeche keine sichtbaren Innenflaechen/Raender hat.
     */
    public static boolean hidesFaceTowards(Block self, BlockState neighbor) {
        Block cracked = NatureBlocks.CRACKED_ICE;
        if (cracked == null || !(self == net.minecraft.world.level.block.Blocks.ICE || self == cracked)) {
            return false;
        }
        return neighbor.is(net.minecraft.world.level.block.Blocks.ICE) || neighbor.is(cracked);
    }

    /** Steht ein Lebewesen (kein Zuschauer) auf der Oberseite? */
    public static boolean isStoodOn(Level level, BlockPos pos) {
        AABB top = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.25, pos.getZ() + 1.0);
        return !level.getEntitiesOfClass(LivingEntity.class, top, entity -> !entity.isSpectator()).isEmpty();
    }

    /** Abgebaut hinterlaesst es kein Wasser (anders als Eis): nur Statistik, Erschoepfung und Drops. */
    // Kein @Override (wie in SimpleBuilding, wo die 26.2-Linie eine andere Signatur hat).
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack destroyedWith) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, blockEntity, player, destroyedWith);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WAXED);
    }
}
