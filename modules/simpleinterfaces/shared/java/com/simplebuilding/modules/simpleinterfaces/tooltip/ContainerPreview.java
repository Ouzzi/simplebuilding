package com.simplebuilding.modules.simpleinterfaces.tooltip;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Tooltip image of a shulker box or the own ender chest. {@code grid}: the slot grid is drawn ({@code slots} in slot
 * order, {@code more} hidden stacks in the last cell, {@code face} the RGB of the panel); otherwise only {@code note}
 * (Shift hint or "empty"). {@code replacesList}: the Vanilla content lines are left out while the grid shows.
 */
public record ContainerPreview(List<ItemStack> slots, int more, int face, boolean grid, @Nullable Component note, boolean replacesList)
        implements TooltipComponent {
    public int rows() {
        return (slots.size() + ContainerTooltips.COLUMNS - 1) / ContainerTooltips.COLUMNS;
    }
}
