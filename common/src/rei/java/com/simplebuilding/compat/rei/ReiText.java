package com.simplebuilding.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Word-wrapped note lines for the REI categories. REI's label widget draws a single line, while
 * JEI's {@code addText} wraps by itself; this splits the text with the game font first. Only
 * labels, no own drawing code, so the file is the same on every Minecraft line.
 */
final class ReiText {

    static final int LINE_HEIGHT = 10;
    private static final int COLOR_LIGHT = 0xFF404040;
    private static final int COLOR_DARK = 0xFFBBBBBB;

    private ReiText() {
    }

    /** {@code text} split into lines no wider than {@code width} pixels, styles kept. */
    static List<Component> wrap(Component text, int width) {
        List<Component> out = new ArrayList<>();
        for (FormattedText line : Minecraft.getInstance().font.getSplitter().splitLines(text, width, Style.EMPTY)) {
            MutableComponent component = Component.empty();
            line.visit((style, part) -> {
                component.append(Component.literal(part).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            out.add(component);
        }
        return out;
    }

    /** All notes wrapped, in order. */
    static List<Component> wrapAll(List<? extends Component> notes, int width) {
        List<Component> out = new ArrayList<>();
        for (Component note : notes) {
            out.addAll(wrap(note, width));
        }
        return out;
    }

    /** One label per line, starting at {@code (x, y)}, in the colors of REI's own categories. */
    static void addLines(List<Widget> widgets, List<Component> lines, int x, int y) {
        for (Component line : lines) {
            widgets.add(Widgets.createLabel(new Point(x, y), line).leftAligned().noShadow().color(COLOR_LIGHT, COLOR_DARK));
            y += LINE_HEIGHT;
        }
    }
}
