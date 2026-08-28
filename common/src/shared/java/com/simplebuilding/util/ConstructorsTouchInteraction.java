package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

/**
 * Shared "Constructor's Touch" block-state cycling (enchanted stick right-clicks a block
 * and cycles its first blockstate property).
 *
 * <p>Loader-neutral on purpose: both Fabric ({@code UseBlockCallback}) and NeoForge
 * ({@code PlayerInteractEvent.RightClickBlock}) delegate here, so the status message can no
 * longer drift apart between loaders. It previously did: the NeoForge copy sent the property
 * readout to the chat while Fabric put it on the actionbar.
 *
 * <p>The readout goes to the actionbar via {@code ServerPlayer#sendOverlayMessage(Component)} —
 * in MC 26.2 that is the only actionbar entry point ({@code displayClientMessage} is gone).
 */
public final class ConstructorsTouchInteraction {
    private ConstructorsTouchInteraction() {
    }

    /**
     * @return {@link InteractionResult#SUCCESS} when the interaction was consumed by
     *         Constructor's Touch, {@link InteractionResult#PASS} otherwise.
     */
    public static InteractionResult handleUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH) || !stack.is(Items.STICK)) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            BlockState state = world.getBlockState(hitResult.getBlockPos());
            var properties = state.getProperties();
            if (!properties.isEmpty()) {
                Property<?> property = properties.iterator().next();
                BlockState newState = cycleState(state, property, player.isShiftKeyDown());
                world.setBlock(hitResult.getBlockPos(), newState, 18);
                Component message = Component.literal(property.getName() + ": ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(newState.getValue(property))).withStyle(ChatFormatting.WHITE));
                if (player instanceof ServerPlayer serverPlayer) {
                    // Actionbar, not chat.
                    serverPlayer.sendOverlayMessage(message);
                }
            }
        }
        return InteractionResult.SUCCESS;
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
