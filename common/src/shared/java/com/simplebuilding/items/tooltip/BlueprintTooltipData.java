package com.simplebuilding.items.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Tooltip-Bild einer Blaupause: ihr Bau-Code. Der Client baut daraus die sich drehende
 * 3D-Miniatur und, mit Umschalt, die Materialliste ({@code BlueprintTooltip}).
 */
public record BlueprintTooltipData(String code) implements TooltipComponent {
}
