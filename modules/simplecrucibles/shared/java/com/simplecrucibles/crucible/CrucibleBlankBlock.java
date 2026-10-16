package com.simplecrucibles.crucible;

import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplelib.api.SimpleLibApi;
import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplecrucibles.registry.CrucibleTags;
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
 * The iron crucible built in the world (owner addition 8 + answer 7 B, owner N51 2026-10-13, base = Vanilla cauldron): strike an empty Vanilla cauldron
 * with an iron ingot in the off hand, one strike per second, sixteen strikes, one ingot each. The first strike turns the
 * cauldron into the crucible's base (a cauldron with water, lava or powder snow must be emptied first, nothing is lost). Strikes 1-8 raise the four side plates (north, east, south, west; each first a
 * half plate, then the full one), strikes 9-12 the two handles (east, west; half, then full), strikes 13-16 the four
 * rim pieces; the 16th sets the iron crucible. Plates and rim take tag {@code simplelib:crucible_walls}, handles tag
 * {@code simplelib:crucible_handles} (iron ingot; SimpleBuilding adds its iron pole). This block is the half-built
 * blank (stage 1-15 = strikes done, one model per stage); breaking it returns the cauldron and one iron ingot
 * per strike (loot table {@code simplelib:blocks/crucible_blank}, conditions on {@code stage} and {@code cauldron}).
 * Without SimpleBuilding the tool is an axe (principle 5a); a partner with its own way switches the axe way off through {@link SimpleLibApi#disableAxeWays()} and calls {@link #strike} itself.
 */
public class CrucibleBlankBlock extends Block {
    public static final int STRIKES = 16;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, STRIKES - 1);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /**
     * Built on a Vanilla cauldron (owner N51 correction). False = an older blank started on an iron block: it still
     * builds on and gives its iron block back (saves without the property load with this default).
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CAULDRON =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("cauldron");
    /** Strikes 1-8: side plates (two per side). */
    public static final int PLATE_STRIKES = 8;
    /** Strikes 9-12: handles (two per handle). */
    public static final int HANDLE_STRIKES = 4;
    /** Strikes 13-16: rim pieces (one per side). */
    public static final int RIM_STRIKES = 4;
    /** Strikes that take a wall material (plates + rim); the rest take a handle (JEI, wiki). */
    public static final int WALLS = PLATE_STRIKES + RIM_STRIKES;
    public static final int STRIKE_COOLDOWN = 20;

    public CrucibleBlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 1).setValue(FACING, Direction.NORTH).setValue(CAULDRON, false));
    }

    /** Base (foot + floor), like the crucible model. */
    private static final double[][] BASE = {{3, 0, 3, 13, 2, 13}, {2, 2, 2, 14, 4, 14}};
    /** Belly plates north/east/south/west (full height y 4-11; the half plate ends at y 8). */
    private static final double[][] PLATES = {{1, 4, 1, 15, 11, 3}, {13, 4, 3, 15, 11, 13}, {1, 4, 13, 15, 11, 15}, {1, 4, 3, 3, 11, 13}};
    /** Handles east/west (full; the half handle is the north half). */
    private static final double[][] HANDLES = {{15, 9, 6, 16, 11, 10}, {0, 9, 6, 1, 11, 10}};
    /** Rim (neck) pieces north/east/south/west. */
    private static final double[][] RIM = {{2, 11, 2, 14, 14, 3}, {13, 11, 3, 14, 14, 13}, {2, 11, 13, 14, 14, 14}, {2, 11, 3, 3, 14, 13}};
    public static final double HALF_PLATE_TOP = 8;
    private static final net.minecraft.world.phys.shapes.VoxelShape[][] SHAPES = new net.minecraft.world.phys.shapes.VoxelShape[STRIKES - 1][4];

    /** The model boxes of a stage (strikes done), facing north; shared with the model generator's layout. */
    public static java.util.List<double[]> parts(int stage) {
        java.util.List<double[]> out = new java.util.ArrayList<>(java.util.Arrays.asList(BASE));
        for (int side = 0; side < 4; side++) {
            int done = stage - 2 * side;
            if (done >= 2) out.add(PLATES[side]);
            else if (done == 1) out.add(new double[] {PLATES[side][0], 4, PLATES[side][2], PLATES[side][3], HALF_PLATE_TOP, PLATES[side][5]});
        }
        for (int handle = 0; handle < 2; handle++) {
            int done = stage - PLATE_STRIKES - 2 * handle;
            double[] b = HANDLES[handle];
            if (done >= 2) out.add(b);
            else if (done == 1) out.add(new double[] {b[0], b[1], b[2], b[3], b[4], 8});
        }
        for (int side = 0; side < 4; side++) {
            if (stage - PLATE_STRIKES - HANDLE_STRIKES > side) out.add(RIM[side]);
        }
        return out;
    }

    static {
        for (int stage = 1; stage < STRIKES; stage++) {
            java.util.List<double[]> parts = parts(stage);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
                for (double[] b : parts) {
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
        builder.add(STAGE, FACING, CAULDRON);
    }

    /** Strikes already done at a block: 0 on an empty Vanilla cauldron (a filled one must be emptied first), 1-15 on the blank, -1 elsewhere. */
    public static int strikesAt(BlockState state) {
        if (state.is(Blocks.CAULDRON)) return 0;
        if (state.getBlock() instanceof CrucibleBlankBlock) return state.getValue(STAGE);
        return -1;
    }

    /** Whether {@code material} is the right off-hand part for the next strike. */
    public static boolean fitsNext(int done, ItemStack material) {
        if (done < 0 || done >= STRIKES || material.isEmpty()) return false;
        boolean handle = done >= PLATE_STRIKES && done < PLATE_STRIKES + HANDLE_STRIKES;
        return handle ? material.is(CrucibleTags.CRUCIBLE_HANDLES) : material.is(CrucibleTags.CRUCIBLE_WALLS);
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
        com.simplelib.api.InWorldStrikes.feedback(server, pos, Blocks.CAULDRON.defaultBlockState(),
                next >= STRIKES ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_PLACE, 0.6F, next, STRIKES);
        if (next >= STRIKES) {
            server.setBlock(pos, CrucibleBlocks.IRON_CRUCIBLE.defaultBlockState().setValue(CrucibleBlock.FACING, facing), Block.UPDATE_ALL);
        } else {
            server.setBlock(pos, CrucibleBlocks.CRUCIBLE_BLANK.defaultBlockState().setValue(STAGE, next).setValue(FACING, facing)
                    .setValue(CAULDRON, !(state.getBlock() instanceof CrucibleBlankBlock) || state.getValue(CAULDRON)), Block.UPDATE_ALL);
        }
        if (!player.getAbilities().instabuild) {
            material.shrink(1);
            tool.hurtAndBreak(toolDamage, player, EquipmentSlot.MAINHAND);
        }
        player.getCooldowns().addCooldown(tool, STRIKE_COOLDOWN);
        return true;
    }
}
