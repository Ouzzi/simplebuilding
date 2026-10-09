package com.simplebuilding.items.tooltip;

import com.simplebuilding.component.PaintBoxContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip image of a paint box: its contents and the dyes one colour holds ({@code PaintBoxTooltip} draws it). */
public record PaintBoxTooltipData(PaintBoxContents contents, int capacity) implements TooltipComponent {
}
