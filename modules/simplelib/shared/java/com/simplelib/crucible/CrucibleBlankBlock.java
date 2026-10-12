package com.simplelib.crucible;

import com.simplelib.api.SimpleLibApi;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The iron crucible built in the world (owner addition 8 + answer 7 B): strike an iron block with the
 * off-hand material, one strike per second. Strikes 1-4 each use a heavy weighted pressure plate
 * (the four walls, tag {@code simplelib:crucible_walls}), strikes 5-6 each a handle (tag
 * {@code simplelib:crucible_handles}: iron ingot; SimpleBuilding adds its iron pole). This block is
 * the half-built blank; its loot table returns the iron block and every part used so far.
 * Without SimpleBuilding the tool is an axe (principle 5a); a partner with its own way switches the
 * axe way off through {@link SimpleLibApi#disableAxeWays()} and calls {@link #strike} itself.
 */
public class CrucibleBlankBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, 5);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final int STRIKES = 6;
    public static final int WALLS = 4;
    public static final int STRIKE_COOLDOWN = 20;

    public CrucibleBlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 1).setValue(FACING, Direction.NORTH));
    }

    /** Model boxes per stage (see {@code crucible_blank_N.json}): base 0-4, then the walls north/east/south/west, then a handle. */
    private static final double[][] PARTS = {{0, 0, 0, 16, 4, 16}, {1, 4, 1, 15, 14, 3}, {13, 4, 3, 15, 14, 13},
            {1, 4, 13, 15, 14, 15}, {1, 4, 3, 3, 14, 13}, {15, 9, 6, 16, 11, 10}};
    private static final net.minecraft.world.phys.shapes.VoxelShape[][] SHAPES = new net.minecraft.world.phys.shapes.VoxelShape[5][4];

    static {
        for (int stage = 1; stage <= 5; stage++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                // stage 1 = base + north wall, stage n = n walls (stage 5 = four walls + one handle)
                int parts = Math.min(stage, 4) + 1 + (stage == 5 ? 1 : 0);
                net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
                for (int k = 0; k < parts; k++) {
                    double[] b = PARTS[k];
                    shape = net.minecraft.world.phys.shapes.Shapes.or(shape, CrucibleBlock.turned(d, b[0], b[1], b[2], b[3], b[4], b[5]));
                }
                SHAPES[stage - 1][d.get2DDataValue()] = shape;
            }
        }
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES[state.getValue(STAGE) - 1][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, FACING);
    }

    /** Strikes already done at a block: 0 on an iron block, 1-5 on the blank, -1 elsewhere. */
    public static int strikesAt(BlockState state) {
        if (state.is(Blocks.IRON_BLOCK)) return 0;
        if (state.getBlock() instanceof CrucibleBlankBlock) return state.getValue(STAGE);
        return -1;
    }

    /** Whether {@code material} is the right off-hand part for the next strike. */
    public static boolean fitsNext(int done, ItemStack material) {
        if (done < 0 || done >= STRIKES || material.isEmpty()) return false;
        return done < WALLS ? material.is(LibTags.CRUCIBLE_WALLS) : material.is(LibTags.CRUCIBLE_HANDLES);
    }

    /** The axe way: any axe in the main hand (tag {@code minecraft:axes}). */
    public static boolean isAxe(ItemStack tool) {
        return tool.is(ItemTags.AXES);
    }

    /**
     * One strike with {@code tool} (main hand) and the off-hand material. Returns true when it hit.
     * {@code toolDamage} is the durability the tool loses (axe 1, sledgehammer 2 - owner 8 A).
     */
    /** Crack stage 0..9 after {@code done} of {@code total} strikes (shared with the upgrades, owner 2026-10-06). */
    public static int crackStage(int done, int total) {
        return com.simplelib.api.InWorldStrikes.crackStage(done, Math.max(1, total));
    }

    public static boolean strike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        BlockState state = level.getBlockState(pos);
        int done = strikesAt(state);
        ItemStack material = player.getOffhandItem();
        if (!fitsNext(done, material) || player.getCooldowns().isOnCooldown(tool)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        Direction facing = player.getDirection().getOpposite();
        if (state.getBlock() instanceof CrucibleBlankBlock) facing = state.getValue(FACING);
        int next = done + 1;
        // Shared feedback of every in-world conversion; the blank's own stage models are the intermediate models.
        com.simplelib.api.InWorldStrikes.feedback(server, pos, Blocks.IRON_BLOCK.defaultBlockState(),
                next >= STRIKES ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_PLACE, 0.6F, next, STRIKES);
        if (next >= STRIKES) {
            server.setBlock(pos, LibBlocks.IRON_CRUCIBLE.defaultBlockState().setValue(CrucibleBlock.FACING, facing), Block.UPDATE_ALL);
        } else {
            server.setBlock(pos, LibBlocks.CRUCIBLE_BLANK.defaultBlockState().setValue(STAGE, next).setValue(FACING, facing), Block.UPDATE_ALL);
        }
        if (!player.getAbilities().instabuild) {
            material.shrink(1);
            tool.hurtAndBreak(toolDamage, player, EquipmentSlot.MAINHAND);
        }
        player.getCooldowns().addCooldown(tool, STRIKE_COOLDOWN);
        return true;
    }
}
