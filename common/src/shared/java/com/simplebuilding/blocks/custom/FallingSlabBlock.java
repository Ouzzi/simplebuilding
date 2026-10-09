package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.phys.AABB;

/**
 * Sand- und Kies-Stufe (Besitzer N25): eine Stufe, die faellt wie Sand ({@link FallingBlock} nachgebaut, weil eine
 * Klasse nicht zugleich {@link SlabBlock} und {@code FallingBlock} sein kann).
 *
 * <ul>
 *   <li>Ist unter ihr frei ({@link FallingBlock#isFree}), faellt sie als Entity - eine obere Stufe faellt als untere,
 *       eine doppelte bleibt doppelt (Wasser bleibt wie bei Vanilla zurueck).</li>
 *   <li>Landet eine untere Stufe auf einer unteren Stufe derselben Art, werden beide zur Doppelstufe
 *       ({@link #canBeReplaced} + {@link #onLand}).</li>
 * </ul>
 */
public class FallingSlabBlock extends SlabBlock implements Fallable {
    /** Wie Vanilla: zwei Ticks Vorlauf, damit eine frisch gesetzte Stufe nicht sofort faellt. */
    private static final int DELAY_AFTER_PLACE = 2;

    public FallingSlabBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, DELAY_AFTER_PLACE);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        ticks.scheduleTick(pos, this, DELAY_AFTER_PLACE);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
            BlockState falling = state.getValue(TYPE) == SlabType.TOP ? state.setValue(TYPE, SlabType.BOTTOM) : state;
            FallingBlockEntity.fall(level, pos, falling);
        }
    }

    /**
     * Nur fuer eine fallende untere Stufe derselben Art (FallingBlockEntity prueft mit leerer Hand und
     * {@link DirectionalPlaceContext}): sie darf die untere Stufe "ersetzen", {@link #onLand} macht daraus die Doppelstufe.
     * Ohne das stuende die Entity auf halber Hoehe in deren Zelle und zerfiele zu einem Item.
     */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context instanceof DirectionalPlaceContext && context.getItemInHand().isEmpty() && state.getValue(TYPE) == SlabType.BOTTOM) {
            return !context.getLevel().getEntitiesOfClass(FallingBlockEntity.class, new AABB(context.getClickedPos()),
                    entity -> entity.getBlockState().is(this) && entity.getBlockState().getValue(TYPE) == SlabType.BOTTOM).isEmpty();
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState falling, BlockState replaced, FallingBlockEntity entity) {
        if (replaced.is(this) && replaced.getValue(TYPE) == SlabType.BOTTOM && falling.getValue(TYPE) == SlabType.BOTTOM) {
            level.setBlockAndUpdate(pos, falling.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) == 0 && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            ParticleUtils.spawnParticleBelow(level, pos, random, new BlockParticleOption(ParticleTypes.FALLING_DUST, state));
        }
    }
}
