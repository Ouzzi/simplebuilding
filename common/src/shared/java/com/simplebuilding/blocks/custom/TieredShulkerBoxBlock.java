package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity;
import com.simplebuilding.platform.TieredChestMenus;
import com.simplebuilding.screen.TieredChestOpenData;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.TieredChests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Reinforced, Netherite and Enderite Shulker Box: a vanilla shulker box in everything the world
 * sees (lid, shape while opening, piston breaks it and it drops with its contents, dispensers
 * place it, it never fits into another shulker box or a bundle) with the slots and stack factor of
 * its {@link ChestTier} and its own block entity ({@link TieredShulkerBoxBlockEntity}).
 *
 * <p>Extends {@link ShulkerBoxBlock} so that every {@code instanceof ShulkerBoxBlock} in vanilla,
 * the loaders and other mods treats it as one - above all {@code BlockItem#canFitInsideContainerItems}
 * (no nesting) and the sided hopper checks. Every method of the superclass that expects vanilla's
 * block entity is overridden here. The superclass' color is null; the dye color lives in the block
 * entity (one block per tier, sixteen colors plus undyed as {@code minecraft:base_color}).
 */
public class TieredShulkerBoxBlock extends ShulkerBoxBlock {
    private final ChestTier tier;

    public TieredShulkerBoxBlock(ChestTier tier, BlockBehaviour.Properties properties) {
        super(null, properties);
        this.tier = tier;
    }

    public ChestTier tier() {
        return this.tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredShulkerBoxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TIERED_SHULKER_BOX_BE, TieredShulkerBoxBlockEntity::tick);
    }

    /**
     * Like vanilla, with the tier menu. In the smithing stance (hammer plus upgrade material) the
     * click goes on to the hammer when the upgrade can begin.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box && canOpen(state, level, pos, box)) {
            TieredChestMenus.open(serverPlayer, box, TieredChestOpenData.shulker(this.tier));
            player.awardStat(Stats.OPEN_SHULKER_BOX);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Vanilla's rule: a closed box opens only when its lid has room to rise. */
    public static boolean canOpen(BlockState state, Level level, BlockPos pos, TieredShulkerBoxBlockEntity box) {
        if (box.getAnimationStatus() != net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
            return true;
        }
        AABB lid = Shulker.getProgressDeltaAabb(1.0F, state.getValue(FACING), 0.0F, 0.5F, Vec3.atBottomCenterOf(pos)).deflate(1.0E-6);
        return level.noCollision(lid);
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box ? box : null;
    }

    /** Like vanilla: in creative a filled box still drops as an item with its contents. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box) {
            if (!level.isClientSide() && player.preventsBlockDrops() && !box.isEmpty()) {
                ItemStack stack = new ItemStack(this);
                stack.applyComponents(box.collectComponents());
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            } else {
                box.unpackLootTable(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof TieredShulkerBoxBlockEntity box) {
            params = params.withDynamicDrop(CONTENTS, output -> {
                for (int i = 0; i < box.getContainerSize(); i++) {
                    output.accept(box.getItem(i));
                }
            });
        }
        return super.getDrops(state, params);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box && !box.isClosed()
                ? SHAPES_OPEN_SUPPORT.get(state.getValue(FACING).getOpposite())
                : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box
                ? Shapes.create(box.getBoundingBox(state))
                : Shapes.block();
    }

    /** Like vanilla, but against the stack limit of the tier (256 stone are full, not 64). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box ? TieredChests.analogSignal(box, this.tier) : 0;
    }

    /** Whether the box at {@code pos} lets suffocation, view and redstone through right now (open lid). */
    public static boolean isClosedAt(BlockState state, BlockGetter level, BlockPos pos) {
        return !(level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box) || box.isClosed();
    }
}
