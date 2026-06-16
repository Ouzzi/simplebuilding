package com.simplebuilding.items.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

// Dies ist ein Java Record - eine Klasse, die nur Daten hält
public record ReinforcedBundleTooltipData(BundleContents contents, int maxCapacity) implements TooltipComponent {
}