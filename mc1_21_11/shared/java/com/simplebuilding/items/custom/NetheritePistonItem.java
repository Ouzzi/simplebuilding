package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The netherite and enderite piston's item: the breaker tooltip (redstone strength decides what it
 * breaks, which nugget repairs it) and, once it carries wear from the world ({@code block_state}
 * component, written by {@code NetheriteBreakerPistonBlock#getDrops}), {@code Wear n/8} in the colour
 * of the damage bar.
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

    /** The wear stage the stack carries in its block state component; 0 without one. */
    public static int wearOf(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties == null) {
            return 0;
        }
        Integer wear = properties.get(NetheriteBreakerPistonBlock.WEAR);
        return wear == null ? 0 : wear;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
                                Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        int wear = wearOf(stack);
        if (wear > 0) {
            ChatFormatting colour = wear >= NetheriteBreakerPistonBlock.WEAR_STAGES - 2 ? ChatFormatting.RED
                    : wear >= NetheriteBreakerPistonBlock.WEAR_STAGES / 2 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            textConsumer.accept(Component.translatable("tooltip.simplebuilding.netherite_piston.wear",
                    wear, NetheriteBreakerPistonBlock.WEAR_STAGES).withStyle(colour));
        }
    }
}
