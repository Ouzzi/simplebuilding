package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

/**
 * Shared "Constructor's Touch" block-state cycling (enchanted stick right-clicks a block
 * and turns its first orientation property).
 *
 * <p>Loader-neutral on purpose: both Fabric ({@code UseBlockCallback}) and NeoForge
 * ({@code PlayerInteractEvent.RightClickBlock}) delegate here, so the status message can no
 * longer drift apart between loaders. It previously did: the NeoForge copy sent the property
 * readout to the chat while Fabric put it on the actionbar.
 *
 * <p>The readout goes to the actionbar via {@code ServerPlayer#displayClientMessage(message, true)},
 * the MC 1.21.11 actionbar entry point (26.2 has {@code sendOverlayMessage} instead). On this line
 * the logic sat twice until 2026-09 - in Fabric's {@code ModRegistries} and in NeoForge's
 * {@code ModRegistriesNeoForge} - and the NeoForge copy wrote the readout to the chat.
 *
 * <p>Only orientation properties are turned (audit 2026-09-26, P1 #1): before that the stick was a
 * survival debug stick - candles 1 -> 4, slabs to {@code type=double}, crop {@code age}, composter
 * {@code level}, respawn anchor charges, {@code waterlogged} - each an item or resource out of
 * nothing. It also needs the same permissions as placing a block (no adventure or spectator mode,
 * no spawn protection or claims), works from the main hand only on every loader (audit P4 #50:
 * Fabric used to accept the off hand as well), and leaves multi-block structures alone whose
 * halves would come apart (beds, double chests, extended pistons and their heads). Doors turn
 * both halves together.
 */
public final class ConstructorsTouchInteraction {
    private ConstructorsTouchInteraction() {
    }

    /**
     * The non-directional orientation properties the stick may turn. Every property whose values
     * are a {@link Direction} or a {@link Direction.Axis} counts as orientation too.
     */
    private static final Set<Property<?>> ORIENTATION_PROPERTIES = Set.of(
            BlockStateProperties.ROTATION_16,
            BlockStateProperties.HALF,
            BlockStateProperties.DOOR_HINGE,
            BlockStateProperties.STAIRS_SHAPE,
            BlockStateProperties.RAIL_SHAPE,
            BlockStateProperties.RAIL_SHAPE_STRAIGHT,
            BlockStateProperties.ATTACH_FACE,
            BlockStateProperties.ORIENTATION);

    /** Whether the stick may turn {@code property} at all. */
    public static boolean isOrientationProperty(Property<?> property) {
        Class<?> values = property.getValueClass();
        return values == Direction.class || values == Direction.Axis.class
                || ORIENTATION_PROPERTIES.contains(property);
    }

    /**
     * Blocks whose orientation belongs to a structure spanning several blocks: turning one part
     * tears it apart, and a later shape update can then pop the lone half as an extra drop.
     */
    private static boolean isLockedStructure(BlockState state) {
        if (state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.PISTON_TYPE)) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.CHEST_TYPE)
                && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE;
    }

    /**
     * @return {@link InteractionResult#SUCCESS} when the interaction was consumed by
     *         Constructor's Touch, {@link InteractionResult#PASS} otherwise.
     */
    public static InteractionResult handleUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH) || !stack.is(Items.STICK)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        // Same permission a block placement needs: adventure/spectator (mayBuild), spawn
        // protection and claim mods (mayInteract / mayUseItemAt). PASS lets the click fall
        // through to the block's own interaction, so a door still opens.
        if (!player.mayBuild() || player.isSpectator() || !world.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hitResult.getDirection(), stack)) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            BlockState state = world.getBlockState(pos);
            Property<?> property = firstTurnableProperty(state, player);
            if (property != null) {
                BlockState newState = cycleState(state, property, player.isShiftKeyDown());
                world.setBlock(pos, newState, 18);
                turnOtherDoorHalf(world, pos, state, newState, property);
                Component message = Component.literal(property.getName() + ": ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(newState.getValue(property))).withStyle(ChatFormatting.WHITE));
                if (player instanceof ServerPlayer serverPlayer) {
                    // Actionbar, not chat.
                    serverPlayer.displayClientMessage(message, true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The first orientation property of {@code state} the stick may turn, or null. */
    private static Property<?> firstTurnableProperty(BlockState state, Player player) {
        if (isLockedStructure(state)
                || (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks())) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (isOrientationProperty(property)) {
                return property;
            }
        }
        return null;
    }

    /** Doors: the other half gets the same value, so the two halves never disagree. */
    private static void turnOtherDoorHalf(Level world, BlockPos pos, BlockState before, BlockState after,
                                          Property<?> property) {
        if (!before.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return;
        }
        DoubleBlockHalf half = before.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState other = world.getBlockState(otherPos);
        if (other.is(before.getBlock()) && other.hasProperty(property)
                && other.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half) {
            world.setBlock(otherPos, copyValue(other, after, property), 18);
        }
    }

    private static <T extends Comparable<T>> BlockState copyValue(BlockState target, BlockState source, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean inverse) {
        return state.setValue(property, cycle(property.getPossibleValues(), state.getValue(property), inverse));
    }

    private static <T> T cycle(Iterable<T> elements, T current, boolean inverse) {
        List<T> values = new ArrayList<>();
        for (T value : elements) {
            values.add(value);
        }

        if (values.isEmpty()) {
            return current;
        }

        int index = values.indexOf(current);
        if (index < 0) {
            return inverse ? values.get(values.size() - 1) : values.get(0);
        }

        int nextIndex = inverse
                ? (index - 1 + values.size()) % values.size()
                : (index + 1) % values.size();
        return values.get(nextIndex);
    }
}
