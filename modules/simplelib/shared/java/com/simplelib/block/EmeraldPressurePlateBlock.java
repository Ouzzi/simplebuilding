package com.simplelib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * N48c (b-circ46): the Emerald Pressure Plate. Unpowered it reacts to villagers only; while redstone powers the plate itself it is
 * exactly the other way round: every living entity except villagers presses it (a Vanilla stone plate counts every
 * {@link LivingEntity}, so the inverse is "living, not a villager").
 *
 * <p>Villager = {@link Villager} only. The Wandering Trader is a trader, not an inhabitant of a village: it counts with the
 * "others" (presses the powered plate). Zombie villagers are zombies and count with the others as well.
 *
 * <p>"Powered" is a redstone signal reaching the block from above, a side, or from below - but never the plate's own output: a plate
 * strongly powers the block below it, and Vanilla's {@code hasNeighborSignal} would read that straight back and flip the mode by
 * itself. Below, a conductor therefore counts only what its other five neighbours give it (see {@link #poweredFromOutside}).
 * Entities of the plate's own state (it is pressed) never change the mode, so it does not oscillate.
 */
public class EmeraldPressurePlateBlock extends PressurePlateBlock {
    public EmeraldPressurePlateBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.STONE, properties);
    }

    /** Whether redstone outside this plate powers it (the villager rule inverts). */
    public static boolean poweredFromOutside(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && level.getSignal(pos.relative(direction), direction) > 0) return true;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isRedstoneConductor(level, below)) {
            // A conductor below is strongly powered by this very plate: its own output (a redstone block) and its other neighbours count, not the plate.
            if (belowState.getSignal(level, below, Direction.DOWN) > 0) return true;
            for (Direction direction : Direction.values()) {
                if (direction != Direction.UP && level.getDirectSignal(below.relative(direction), direction) > 0) return true;
            }
            return false;
        }
        return level.getSignal(below, Direction.DOWN) > 0;
    }

    /** Whether this entity presses the plate in the given mode. */
    public static boolean presses(LivingEntity entity, boolean powered) {
        return (entity instanceof Villager) != powered;
    }

    /** Public for the game tests (mock players and AI-less mobs do not trigger Vanilla's entityInside check). */
    public int signalAt(Level level, BlockPos pos) {
        return getSignalStrength(level, pos);
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        boolean powered = poweredFromOutside(level, pos);
        return level.getEntitiesOfClass(LivingEntity.class, TOUCH_AABB.move(pos),
                entity -> !entity.isIgnoringBlockTriggers() && !entity.isSpectator() && presses(entity, powered)).isEmpty() ? 0 : 15;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    /** Re-reads the entities after every neighbour change (the mode may have flipped) and keeps ticking while pressed. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int now = getSignalStrength(level, pos);
        int before = getSignalForState(state);
        if (now != before) {
            level.setBlock(pos, setSignalForState(state, now), 2);
            updateNeighbours(level, pos);
            level.setBlocksDirty(pos, state, level.getBlockState(pos));
            level.playSound(null, pos, now > 0 ? type.pressurePlateClickOn() : type.pressurePlateClickOff(), SoundSource.BLOCKS);
            level.gameEvent(null, now > 0 ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
        }
        if (now > 0) level.scheduleTick(pos, this, getPressedTime());
    }
}
