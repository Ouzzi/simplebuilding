package com.simplebuilding.enchanting;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.version.BlockCodecs;
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
 * Astral Enchanting Table (owner 2026-10-09, queue N27): made in the world from a Vanilla enchanting table with about
 * twenty blows of a Netherite or Enderite sledgehammer and an Enderite nugget in the off hand
 * ({@link com.simplebuilding.util.SledgehammerUpgrades}). Shaped like Vanilla's table with the floating book; takes twice
 * as long to break ({@value #HARDNESS} instead of 5) and drops a Vanilla enchanting table plus the nugget (loot table)
 * and its stored lapis and blaze powder ({@link AstralEnchantingTableBlockEntity}). The menu is
 * {@link com.simplebuilding.screen.AstralEnchantingMenu}, the rules {@link AstralEnchanting}.
 */
public class AstralEnchantingTableBlock extends BaseEntityBlock {
    public static final MapCodec<AstralEnchantingTableBlock> CODEC = BlockCodecs.simple(AstralEnchantingTableBlock::new);
    /** Twice the Vanilla enchanting table's 5.0. */
    public static final float HARDNESS = 10.0F;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);

    public AstralEnchantingTableBlock(Properties properties) {
        super(properties);
    }

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override).
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
                int value = AstralEnchanting.shelfValue(level, pos, offset);
                if (value > 0) {
                    level.addParticle(value == AstralEnchanting.BLAZE_SHELF && random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.ENCHANT,
                            pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                            offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1.0F, offset.getZ() + random.nextFloat() - 0.5);
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AstralEnchantingTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.ASTRAL_ENCHANTING_TABLE_BE, AstralEnchantingTableBlockEntity::bookAnimationTick)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AstralEnchantingTableBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
