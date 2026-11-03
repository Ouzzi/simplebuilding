package com.simpleenchanting.block;

import com.simpleenchanting.EnchanterStage;
import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.EnchantingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Dark and Astral Enchanter (owner 2026-10-09, queue N27; two stages ENCH01 03.11.): made in the world from a Vanilla enchanting
 * table with sixteen strikes of a Netherite sledgehammer and eight Netherite nuggets in the off hand (Dark), and from the Dark
 * Enchanter with an Enderite sledgehammer and eight Enderite nuggets (Astral; data/simpleenchanting/in_world_upgrades). Shaped
 * like Vanilla's table with the floating book; takes twice as long to break ({@value #HARDNESS} instead of 5) and drops what it was
 * made of (loot table: the table below plus the eight nuggets, WI26-1) and its stored lapis and blaze powder
 * ({@link EnchanterBlockEntity}). The menu is {@link com.simpleenchanting.menu.EnchanterMenu}, the rules {@link EnchantingRules}.
 */
public class EnchanterBlock extends BaseEntityBlock {
    /** Twice the Vanilla enchanting table's 5.0. */
    public static final float HARDNESS = 10.0F;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);
    private final EnchanterStage stage;

    public EnchanterBlock(EnchanterStage stage, Properties properties) {
        super(properties);
        this.stage = stage;
    }

    public EnchanterStage stage() {
        return this.stage;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Vanilla's enchanting particles from every shelf that counts, a few more from blazewood shelves. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(16) == 0) {
                int value = EnchantingRules.shelfValue(level, pos, offset);
                if (value > 0) {
                    level.addParticle(value == EnchantingRules.BLAZE_SHELF && random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.ENCHANT,
                            pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                            offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1.0F, offset.getZ() + random.nextFloat() - 0.5);
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchanterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, EnchantingRegistry.ENCHANTER_BE, EnchanterBlockEntity::bookAnimationTick)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnchanterBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
