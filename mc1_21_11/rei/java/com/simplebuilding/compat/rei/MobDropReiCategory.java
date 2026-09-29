package com.simplebuilding.compat.rei;

import com.simplebuilding.compat.MobDropCatalog;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * "Mob drops" as an REI category - the counterpart of the JEI {@code MobDropCategory}: victim
 * (spawn egg) + killer (spawn egg), arrow, the result(s) cycling; the way it has to die underneath.
 * Same title key as JEI.
 */
final class MobDropReiCategory implements DisplayCategory<MobDropDisplay> {

    private static final int WIDTH = 176;
    private static final int PADDING = 5;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int TEXT_LINES = 4;

    private final Renderer icon = EntryStacks.of(Items.CREEPER_HEAD);

    @Override
    public CategoryIdentifier<? extends MobDropDisplay> getCategoryIdentifier() {
        return MobDropDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simplebuilding.category.mob_drop");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayHeight() {
        return TEXT_TOP + TEXT_LINES * ReiText.LINE_HEIGHT + PADDING * 2;
    }

    @Override
    public int getDisplayWidth(MobDropDisplay display) {
        return WIDTH;
    }

    @Override
    public List<Widget> setupDisplay(MobDropDisplay display, Rectangle bounds) {
        MobDropCatalog.Drop drop = display.drop();
        int left = bounds.x + PADDING;
        int top = bounds.y + PADDING;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createSlot(new Point(left + 1, top + SLOT_Y + 1)).entries(display.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createSlot(new Point(left + 25, top + SLOT_Y + 1)).entries(display.getInputEntries().get(1)).markInput());
        widgets.add(Widgets.createArrow(new Point(left + 49, top + SLOT_Y)).disableAnimation());
        widgets.add(Widgets.createResultSlotBackground(new Point(left + 83, top + SLOT_Y + 1)));
        widgets.add(Widgets.createSlot(new Point(left + 83, top + SLOT_Y + 1))
                .entries(display.getOutputEntries().get(0)).disableBackground().markOutput());
        Component note = Component.translatable(drop.cause().noteKey(), drop.victim().getDescription(), drop.results().size());
        List<Component> lines = ReiText.wrap(note, WIDTH - PADDING * 2);
        ReiText.addLines(widgets, lines.size() > TEXT_LINES ? lines.subList(0, TEXT_LINES) : lines, left, top + TEXT_TOP);
        return widgets;
    }
}
