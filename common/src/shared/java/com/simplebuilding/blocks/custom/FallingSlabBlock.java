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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Entity;

/**
 * Sand- und Kies-Stufe (Besitzer N25): eine Stufe, die faellt wie Sand ({@link FallingBlock} nachgebaut, weil eine
 * Klasse nicht zugleich {@link SlabBlock} und {@code FallingBlock} sein kann).
 *
 * <ul>
 *   <li>Ist unter ihr frei ({@link FallingBlock#isFree}), faellt sie als Entity - eine obere Stufe faellt als untere,
 *       eine doppelte wird zum vollen Block (Wasser bleibt wie bei Vanilla zurueck).</li>
 *   <li>Landet eine untere Stufe auf einer unteren Stufe derselben Art, werden beide zum vollen Sand-/Kiesblock
 *       ({@link #canBeReplaced}, Besitzer N31); ebenso beim Setzen der zweiten Stufe ({@link MergingSlabBlock}).</li>
 * </ul>
 */
public class FallingSlabBlock extends MergingSlabBlock implements Fallable {
    /** Wie Vanilla: zwei Ticks Vorlauf, damit eine frisch gesetzte Stufe nicht sofort faellt. */
    private static final int DELAY_AFTER_PLACE = 2;

    public FallingSlabBlock(Properties settings, Block fullBlock) {
        super(settings, fullBlock);
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
        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE) {
            // Doppelstufe (alte Welt, /setblock): wird zum vollen Block, der dann selbst faellt.
            level.setBlockAndUpdate(pos, fullState());
            return;
        }
        if (pos.getY() < level.getMinY()) {
            return;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        // Besitzer N34: eine untere Stufe derselben Art darunter faengt die fallende Stufe auf (beide werden voll).
        boolean fallsThrough = FallingBlock.isFree(below) || isMergeTarget(below);
        if (type == SlabType.BOTTOM) {
            if (fallsThrough) {
                fall(level, pos, state, 0.0);
            }
            return;
        }
        // Obere Stufe: ihre untere Haelfte ist frei. Sie faellt mindestens bis zum Boden der eigenen Zelle - auch auf
        // einen vollen Block darunter (Besitzer N34, schwebte vorher).
        if (fallsThrough || isFullHeight(below, level, belowPos)) {
            fall(level, pos, state, 0.5);
        } else {
            // Darunter etwas Niedriges, das keine Landung traegt (fremde untere Stufe, Ackerboden): die Stufe rutscht
            // in der eigenen Zelle nach unten und bleibt dort liegen wie eine untere Stufe.
            level.setBlockAndUpdate(pos, state.setValue(TYPE, SlabType.BOTTOM));
        }
    }

    /** Eine liegende untere Stufe derselben Art: Landeplatz, auf dem die fallende Stufe zum vollen Block wird. */
    private boolean isMergeTarget(BlockState below) {
        return below.is(this) && below.getValue(TYPE) == SlabType.BOTTOM;
    }

    /** Traegt der Block darunter eine Landung auf voller Hoehe (Oberkante seiner Kollision bei 1)? */
    private static boolean isFullHeight(BlockState below, BlockGetter level, BlockPos belowPos) {
        VoxelShape shape = below.getCollisionShape(level, belowPos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0;
    }

    /**
     * Wie {@link FallingBlockEntity#fall}, aber die Entity startet {@code lift} Bloecke ueber dem Zellboden: eine obere
     * Stufe faellt als untere ab ihrer sichtbaren Hoehe, statt im ersten Tick einen halben Block tiefer zu erscheinen
     * (Besitzer N34). Der private Konstruktor ist nicht erreichbar; {@link Entity#load} setzt Zustand und Lage, bevor
     * die Entity der Welt (und damit dem Client) bekannt wird.
     */
    private static void fall(ServerLevel level, BlockPos pos, BlockState state, double lift) {
        BlockState falling = state.setValue(TYPE, SlabType.BOTTOM);
        if (falling.hasProperty(BlockStateProperties.WATERLOGGED)) {
            falling = falling.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        if (lift == 0.0) {
            FallingBlockEntity.fall(level, pos, falling);
            return;
        }
        FallingBlockEntity entity = new FallingBlockEntity(EntityTypes.FALLING_BLOCK, level);
        CompoundTag tag = new CompoundTag();
        tag.store("BlockState", BlockState.CODEC, falling);
        tag.store("Pos", Vec3.CODEC, new Vec3(pos.getX() + 0.5, pos.getY() + lift, pos.getZ() + 0.5));
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        entity.blocksBuilding = true;
        entity.setStartPos(entity.blockPosition());
        level.setBlockAndUpdate(pos, state.getFluidState().createLegacyBlock());
        level.addFreshEntity(entity);
    }

    /**
     * Landende untere Stufe derselben Art (FallingBlockEntity fragt mit leerer Hand und {@link DirectionalPlaceContext},
     * ob sie die Zelle uebernehmen darf): die Entity steht dann auf halber Hoehe in der Zelle dieser unteren Stufe. Statt
     * sie zu einem Item zerfallen zu lassen (das Setzen des gleichen Zustands schlaegt fehl), wird die liegende Stufe hier
     * zum vollen Block und die Entity verschwindet ohne Drop. Nur der Server, nur diese eine Lage.
     */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context instanceof DirectionalPlaceContext && context.getItemInHand().isEmpty() && state.getValue(TYPE) == SlabType.BOTTOM
                && !context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos();
            for (FallingBlockEntity entity : context.getLevel().getEntitiesOfClass(FallingBlockEntity.class, new AABB(pos),
                    e -> e.isAlive() && e.getBlockState().is(this) && e.getBlockState().getValue(TYPE) == SlabType.BOTTOM)) {
                entity.dropItem = false;
                context.getLevel().setBlockAndUpdate(pos, fullState());
                return false;
            }
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) == 0 && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            ParticleUtils.spawnParticleBelow(level, pos, random, new BlockParticleOption(ParticleTypes.FALLING_DUST, state));
        }
    }
}
