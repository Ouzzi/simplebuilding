package com.simplebuilding.items.tooltip;

import com.simplebuilding.guide.GuideBooks;
import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record GuideTooltipData(List<GuideBooks.Book> books) implements TooltipComponent {
    public GuideTooltipData { books = List.copyOf(books); }
}
