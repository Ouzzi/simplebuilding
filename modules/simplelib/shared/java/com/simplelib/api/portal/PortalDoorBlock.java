package com.simplelib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A door that is a portal (BACK22 core §5, SimpleLib so other mods can use it): opens and closes like a Vanilla door (by hand, even
 * of the iron type), and a player who steps into the open doorway of a framed, open link is moved to the other end
 * ({@link PortalTransit}). Two of them side by side with mirrored hinges form a double door. Subclasses only say where the door
 * leads ({@link #resolve}) and which item repairs/activates it ({@link #mendsWith}); the link state lives in {@link PortalStates},
 * the frame rule in {@link FrameRule}. Block state {@code portal} shows the result to the client.
 */
public abstract class PortalDoorBlock extends DoorBlock {
    public static final EnumProperty<PortalLook> PORTAL = EnumProperty.create("portal", PortalLook.class);

    protected PortalDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
        super(type, properties);
        registerDefaultState(defaultBlockState().setValue(PORTAL, PortalLook.ACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PORTAL);
    }

    /** Where the door with {@code shape} leads (server), or null when it leads nowhere (yet). */
    public abstract @Nullable PortalTarget resolve(ServerLevel level, PortalShape shape);

    /** Whether {@code stack} repairs/activates this door (one is used up). Standard: nothing does. */
    public boolean mendsWith(ServerLevel level, PortalShape shape, ItemStack stack) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        setOpen(player, level, state, pos, !isOpen(state));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        PortalShape shape = PortalShape.of(level, pos);
        if (shape == null || !mendsWith(server, shape, stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (PortalTransit.mend(server, shape, player)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel server) refresh(server, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) refresh(server, pos);
    }

    /** Recomputes {@code portal} of the whole opening at {@code pos} (frame + link state); changes only what differs. */
    public static void refresh(ServerLevel level, BlockPos pos) {
        PortalShape shape = PortalShape.of(level, pos);
        if (shape == null) return;
        PortalLook look = look(level, shape);
        for (int i = 0; i < shape.width(); i++) {
            BlockPos lower = shape.leafPos(i);
            for (BlockPos p : new BlockPos[] {lower, lower.above()}) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof PortalDoorBlock && s.getValue(PORTAL) != look) level.setBlock(p, s.setValue(PORTAL, look), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** What the opening should show: unframed, or the state of its link (dormant while it leads nowhere). */
    public static PortalLook look(ServerLevel level, PortalShape shape) {
        if (!FrameRule.framed(level, shape)) return PortalLook.UNFRAMED;
        BlockState state = level.getBlockState(shape.anchor());
        if (!(state.getBlock() instanceof PortalDoorBlock door)) return PortalLook.UNFRAMED;
        PortalTarget target = door.resolve(level, shape);
        if (target == null) return PortalLook.DORMANT;
        return PortalLook.of(PortalStates.get(level.getServer(), target.linkId(), target.rule()));
    }
}
