package com.simplebuilding.items.tooltip;

import com.simplebuilding.component.BackpackContents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip-Bild eines Rucksacks: sein Inhalt und die Slotzahl der Stufe. Der Client zeichnet daraus
 * mit gedrueckter Umschalttaste ein kleines Raster, ohne sie nur einen Hinweis
 * ({@code BackpackTooltip}).
 */
public record BackpackTooltipData(BackpackContents contents, int slotCount) implements TooltipComponent {
    /** Frische Kopien aller Stapel, nach Slot sortiert - so, wie das Rucksack-Menue sie zeigt. */
    public List<ItemStack> itemsInSlotOrder() {
        List<BackpackContents.Entry> entries = new ArrayList<>(this.contents.entries());
        entries.sort(Comparator.comparingInt(BackpackContents.Entry::slot));
        List<ItemStack> result = new ArrayList<>(entries.size());
        for (BackpackContents.Entry entry : entries) {
            result.add(entry.toStack());
        }
        return result;
    }
}
