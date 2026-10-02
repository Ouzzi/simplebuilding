package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The netherite and enderite piston's item: the breaker tooltip (redstone strength decides what it
 * breaks, which nugget repairs it) and the durability the piston lost in the world.
 *
 * <p>A damaged piston drops with {@code max_damage}, {@code damage} and a stack size of 1
 * ({@code NetheriteBreakerPistonBlock#getDrops}), so vanilla draws the durability bar in the hand and
 * the inventory; an undamaged one carries none of them and keeps stacking. Items from before
 * 2026-09-28 carry the old wear stage in their {@code block_state} component instead; a single one
 * is moved over to the durability components the first time it ticks in an inventory
 * ({@link #inventoryTick}), a stack of several keeps it until placed (the placed piston gets the same
 * stage, so nothing is lost either way).
 */
public class NetheritePistonItem extends BlockItem {
    private final String tooltipKey;

    public NetheritePistonItem(Block block, Properties settings) {
        this(block, settings, "tooltip.simplebuilding.netherite_piston");
    }

    /** {@code tooltipKey}: the line under the name, e.g. {@code tooltip.simplebuilding.enderite_piston}. */
    public NetheritePistonItem(Block block, Properties settings, String tooltipKey) {
        super(block, settings);
        this.tooltipKey = tooltipKey;
    }

    /** The full durability of the piston this item places; 0 for a block without one. */
    public int maxDurability() {
        return NetheriteBreakerPistonBlock.maxDurabilityOf(getBlock().defaultBlockState());
    }

    /** The legacy wear stage a pre-2026-09-28 stack carries in its block state component; 0 without one. */
    public static int legacyWearOf(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties == null) {
            return 0;
        }
        Integer wear = properties.get(NetheriteBreakerPistonBlock.WEAR);
        return wear == null ? 0 : wear;
    }

    /**
     * The damage the stack carries: its {@code damage} component, or for a legacy stack the first
     * damage of its old wear stage (the same fraction n/8). 0 for an undamaged piston.
     */
    public static int damageOf(ItemStack stack) {
        if (stack.has(DataComponents.MAX_DAMAGE)) {
            return stack.getDamageValue();
        }
        int wear = legacyWearOf(stack);
        if (wear > 0 && stack.getItem() instanceof NetheritePistonItem piston && piston.maxDurability() > 0) {
            return NetheriteBreakerPistonBlock.firstDamageOfStage(wear, piston.maxDurability());
        }
        return 0;
    }

    /**
     * Gives a single piston {@code damage} of {@code max}: {@code max_damage}, {@code damage} and a
     * stack size of 1 (a stack with durability must not stack), which is what makes vanilla draw the
     * bar. Only for {@code damage > 0}; an undamaged piston stays without components.
     */
    public static void setDamage(ItemStack stack, int damage, int max) {
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.MAX_DAMAGE, max);
        stack.set(DataComponents.DAMAGE, Math.max(0, Math.min(damage, max - 1)));
    }

    /**
     * Moves a single legacy piston ({@code block_state {wear:n}}) over to the durability components,
     * so it shows the bar. A stack of several cannot carry durability; it keeps the old component,
     * and placing one still puts the stage on the block.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        migrateLegacyWear(stack);
    }

    /** See {@link #inventoryTick}; public for the game tests. Returns whether the stack changed. */
    public static boolean migrateLegacyWear(ItemStack stack) {
        if (stack.getCount() != 1 || stack.has(DataComponents.MAX_DAMAGE) || legacyWearOf(stack) == 0
                || !(stack.getItem() instanceof NetheritePistonItem piston) || piston.maxDurability() <= 0) {
            return false;
        }
        int damage = damageOf(stack);
        stack.remove(DataComponents.BLOCK_STATE);
        setDamage(stack, damage, piston.maxDurability());
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
                                Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".2").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".3").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".4").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".5").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".6").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(tooltipKey + ".7").withStyle(ChatFormatting.GRAY));
        int damage = damageOf(stack);
        int max = maxDurability();
        // Advanced tooltips (F3+H) already show vanilla's "Durability: x / y" for a damaged stack.
        if (damage > 0 && max > 0 && !(type.isAdvanced() && stack.isDamaged())) {
            int left = max - damage;
            ChatFormatting colour = left * 4 <= max ? ChatFormatting.RED
                    : left * 2 <= max ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            textConsumer.accept(Component.translatable("tooltip.simplebuilding.netherite_piston.durability",
                    left, max).withStyle(colour));
        }
    }
}
