package com.simpleblocks.nature;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Rissiges Packeis und rissiges Blaueis (ABN30-7): dieselbe Mechanik wie {@link CrackedIceBlock} (vier Rissstufen unter
 * Lebewesen, danach bricht der Block weg, ohne Wasser), aber abgestuft: je haerter das Eis, desto laenger haelt es
 * ({@link #PACKED_TICKS} / {@link #BLUE_TICKS} je Stufe statt 20). Kein Schmelzen durch Licht (wie Vanillas Packeis/Blaueis).
 */
public class CrackedHardIceBlock extends Block {
    /** Ticks je Rissstufe: Packeis 30 (gut 6 s bis zum Einbruch), Blaueis 40 (gut 8 s). */
    public static final int PACKED_TICKS = 30;
    public static final int BLUE_TICKS = 40;

    private final int ticksPerCrack;

    public CrackedHardIceBlock(Properties settings, int ticksPerCrack) {
        super(settings);
        this.ticksPerCrack = ticksPerCrack;
        registerDefaultState(stateDefinition.any().setValue(CrackedIceBlock.AGE, 0).setValue(CrackedIceBlock.WAXED, false));
    }

    public int ticksPerCrack() {
        return ticksPerCrack;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        CrackedIceBlock.scheduleCrack(level, pos, this, entity, ticksPerCrack);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        InteractionResult r = CrackedIceBlock.glue(stack, state, level, pos, player, hand);
        return r != null ? r : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return CrackedIceBlock.withState(state, super.getDrops(state, params));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        CrackedIceBlock.crack(state, level, pos, this, ticksPerCrack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CrackedIceBlock.AGE, CrackedIceBlock.WAXED);
    }
}
