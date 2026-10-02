package com.simplebuilding.compat.rei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Arrow;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * One in-world transformation kind as an REI category - the counterpart of the JEI
 * {@code InWorldCategory}, same layout: the inputs, the tool slot (every tool that can do it,
 * cycling), an arrow (animated over the real duration where there is one), the result; the notes
 * from the catalog underneath, word-wrapped.
 */
final class InWorldReiCategory implements DisplayCategory<InWorldDisplay> {

    static final int WIDTH = 176;
    private static final int PADDING = 5;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;

    private final InWorldRecipeCatalog.Kind kind;
    private final Renderer icon;
    private final int height;

    InWorldReiCategory(InWorldRecipeCatalog.Kind kind, List<InWorldRecipeCatalog.Entry> entries) {
        this.kind = kind;
        this.icon = EntryStacks.of(iconOf(kind));
        int lines = 0;
        for (InWorldRecipeCatalog.Entry entry : entries) {
            lines = Math.max(lines, ReiText.wrapAll(entry.notes(), textWidth()).size());
        }
        this.height = TEXT_TOP + lines * ReiText.LINE_HEIGHT + PADDING * 2;
    }

    /** Exhaustive on purpose: a new kind does not compile until it has an icon here. */
    static ItemLike iconOf(InWorldRecipeCatalog.Kind kind) {
        return switch (kind) {
            case MACHINE_UPGRADE -> ModItems.NETHERITE_SLEDGEHAMMER;
            case RESHAPE -> Items.STONE_STAIRS;
            case DIAMOND_CRUSH -> ModItems.DIAMOND_PEBBLE;
            case CHISEL -> ModItems.IRON_CHISEL;
            case SHEAR_WOOL -> Items.SHEARS;
            case TRIM_TEMPLATE -> ModItems.GLOWING_TRIM_TEMPLATE;
            case CAULDRON_WASH -> Items.CAULDRON;
            case PISTON_REPAIR -> ModItems.NETHERITE_NUGGET;
            case COPPER_PLATE -> Items.HONEYCOMB;
            case ROTATE -> ModItems.ROTATOR;
            case CONSTRUCTORS_TOUCH -> Items.STICK;
            case CORE_ORE -> ModItems.DIAMOND_CORE;
        };
    }

    private static int textWidth() {
        return WIDTH - PADDING * 2;
    }

    @Override
    public CategoryIdentifier<? extends InWorldDisplay> getCategoryIdentifier() {
        return InWorldDisplay.category(kind);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(kind.titleKey());
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public int getDisplayWidth(InWorldDisplay display) {
        return WIDTH;
    }

    @Override
    public List<Widget> setupDisplay(InWorldDisplay display, Rectangle bounds) {
        InWorldRecipeCatalog.Entry entry = display.entry();
        int left = bounds.x + PADDING;
        int top = bounds.y + PADDING;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int x = left;
        for (int i = 0; i < display.stackInputs(); i++) {
            widgets.add(Widgets.createSlot(new Point(x + 1, top + SLOT_Y + 1)).entries(display.getInputEntries().get(i)).markInput());
            x += 20;
        }
        int toolX = left + display.stackInputs() * 20 + 4;
        widgets.add(Widgets.createSlot(new Point(toolX + 1, top + SLOT_Y + 1))
                .entries(display.getInputEntries().get(display.stackInputs())).markInput());

        int arrowX = toolX + 18 + 6;
        Arrow arrow = Widgets.createArrow(new Point(arrowX, top + SLOT_Y));
        if (entry.durationTicks() > 0) {
            arrow.animationDurationTicks(entry.durationTicks());
        } else {
            arrow.disableAnimation();
        }
        widgets.add(arrow);

        int outputX = arrowX + 24 + 10;
        widgets.add(Widgets.createResultSlotBackground(new Point(outputX, top + SLOT_Y + 1)));
        widgets.add(Widgets.createSlot(new Point(outputX, top + SLOT_Y + 1))
                .entries(display.getOutputEntries().get(0)).disableBackground().markOutput());

        ReiText.addLines(widgets, ReiText.wrapAll(entry.notes(), textWidth()), left, top + TEXT_TOP);
        return widgets;
    }
}
