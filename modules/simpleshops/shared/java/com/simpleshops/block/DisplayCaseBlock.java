package com.simpleshops.block;

import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.ShopLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The display case (and its glow variant): holds the goods of a shop. Placeable anywhere; part of a shop (protected, selling) only on a sales block of the same owner (SP22-7). */
public class DisplayCaseBlock extends BaseEntityBlock {
    /** Wooden base (2 px) and the glass box above it. */
    private static final VoxelShape SHAPE = Shapes.or(net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 2, 16),
            net.minecraft.world.level.block.Block.box(1, 2, 1, 15, 15, 15));
    /** NA24-3/WB24-5: glass and frame tint, one per Vanilla dye tone ("both soft"). */
    public static final EnumProperty<Tint> TINT = EnumProperty.create("tint", Tint.class);
    private final boolean glow;

    public enum Tint implements StringRepresentable {
        NONE(null),
        WHITE(net.minecraft.world.item.DyeColor.WHITE),
        ORANGE(net.minecraft.world.item.DyeColor.ORANGE),
        MAGENTA(net.minecraft.world.item.DyeColor.MAGENTA),
        LIGHT_BLUE(net.minecraft.world.item.DyeColor.LIGHT_BLUE),
        YELLOW(net.minecraft.world.item.DyeColor.YELLOW),
        LIME(net.minecraft.world.item.DyeColor.LIME),
        PINK(net.minecraft.world.item.DyeColor.PINK),
        GRAY(net.minecraft.world.item.DyeColor.GRAY),
        LIGHT_GRAY(net.minecraft.world.item.DyeColor.LIGHT_GRAY),
        CYAN(net.minecraft.world.item.DyeColor.CYAN),
        PURPLE(net.minecraft.world.item.DyeColor.PURPLE),
        BLUE(net.minecraft.world.item.DyeColor.BLUE),
        BROWN(net.minecraft.world.item.DyeColor.BROWN),
        GREEN(net.minecraft.world.item.DyeColor.GREEN),
        RED(net.minecraft.world.item.DyeColor.RED),
        BLACK(net.minecraft.world.item.DyeColor.BLACK);
        public final net.minecraft.world.item.@Nullable DyeColor dye;

        Tint(net.minecraft.world.item.@Nullable DyeColor dye) {
            this.dye = dye;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The tint a dye stack stands for, or NONE. */
        public static Tint of(ItemStack stack) {
            for (Tint t : values()) if (t.dye != null && stack.is(Items.DYE.pick(t.dye))) return t;
            return NONE;
        }
    }

    public DisplayCaseBlock(BlockBehaviour.Properties properties, boolean glow) {
        super(properties);
        this.glow = glow;
        registerDefaultState(this.stateDefinition.any().setValue(TINT, Tint.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(TINT);
    }

    /** The glow variant draws its goods fully bright (and gives off weak light). */
    public boolean glow() {
        return this.glow;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return ShopActions.mayPlace(context, false) ? defaultBlockState() : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayCaseBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity be) {
            be.initFor(player);
            ShopLedger.record(server, pos, player.getUUID(), false);
        }
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, com.simpleshops.ShopsRegistry.CASE_BE, DisplayCaseBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        // Sneak + dye tints glass and frame of your own case (without sneaking a dye is just goods to stock).
        Tint tint = Tint.of(stack);
        if (player.isShiftKeyDown() && tint != Tint.NONE && state.getValue(TINT) != tint) {
            if (level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity be && be.isOwner(player.getUUID())) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(TINT, tint), 3);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    stack.consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) return ShopActions.quickStock(sp, server, pos, hand);
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity be && be.isOwner(player.getUUID())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) return ShopActions.useCase(sp, server, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level instanceof Level l && !ShopGuard.mayBreak(player, l, pos)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }
}
