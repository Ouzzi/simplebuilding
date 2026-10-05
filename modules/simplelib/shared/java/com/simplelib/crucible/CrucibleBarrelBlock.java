package com.simplelib.crucible;

import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Copper barrel (owner wish round 2): next to a crucible, six strikes (axe without SimpleBuilding,
 * sledgehammer with it) attach it; it then faces the crucible, shows a copper flange towards it and
 * the crucible puts its results there first. Anyone may put items in (owner 61); a hopper below
 * takes them out. Breaking the crucible or the barrel detaches it.
 */
public class CrucibleBarrelBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int ATTACH_STRIKES = 6;

    private final BarrelTier tier;

    public CrucibleBarrelBlock(Properties properties, BarrelTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHED, false).setValue(OPEN, false));
    }

    public BarrelTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHED, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBarrelBlockEntity(pos, state);
    }

    /** A tool way (axe without SimpleBuilding, a partner's sledgehammer) takes the click instead of the menu. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be) player.openMenu(be);
        return InteractionResult.SUCCESS;
    }

    /** Detach when the crucible it faces is gone. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, orientation, moved);
        if (state.getValue(ATTACHED) && !(level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof CrucibleBlock)) {
            level.setBlock(pos, state.setValue(ATTACHED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
    }

    /** The crucible next to an unattached barrel, preferring the direction the barrel faces. */
    public static @Nullable Direction crucibleSide(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        if (level.getBlockState(pos.relative(facing)).getBlock() instanceof CrucibleBlock) return facing;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof CrucibleBlock) return d;
        }
        return null;
    }

    /** One attach strike (axe 1 durability, sledgehammer 2); true when it counted. */
    public static boolean attachStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CrucibleBarrelBlock) || state.getValue(ATTACHED)) return false;
        Direction side = crucibleSide(level, pos, state);
        if (side == null || player.getCooldowns().isOnCooldown(tool)) return false;
        BlockPos cruciblePos = pos.relative(side);
        if (CrucibleBlockEntity.attachedBarrelPos(level, cruciblePos) != null) return false; // one barrel per crucible (owner 60)
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be)) return true;
        int done = be.addAttachStrike();
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.25, 0.1, 0.25, 0.05);
        if (done >= ATTACH_STRIKES) {
            server.setBlock(pos, state.setValue(FACING, side).setValue(ATTACHED, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (server.getBlockEntity(cruciblePos) instanceof CrucibleBlockEntity crucible) crucible.markHeatDirty();
        } else {
            server.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.8F, 1.0F + 0.05F * done);
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(toolDamage, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
        return true;
    }

    /** Whether the axe way may attach (principle 5a). */
    public static boolean axeAllowed() {
        return SimpleLibApi.axeWaysEnabled();
    }
}
