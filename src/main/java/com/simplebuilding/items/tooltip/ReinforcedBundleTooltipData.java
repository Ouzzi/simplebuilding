package com.simplebuilding.items.tooltip;

import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.tooltip.TooltipData;

// Dies ist ein Java Record - eine Klasse, die nur Daten hält
public record ReinforcedBundleTooltipData(BundleContentsComponent contents, int maxCapacity) implements TooltipData {
}