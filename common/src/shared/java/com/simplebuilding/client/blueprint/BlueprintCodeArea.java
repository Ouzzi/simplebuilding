package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.mixin.client.MultilineTextFieldAccessor;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * Das Code-Feld des Blaupausen-Editors: ein mehrzeiliges Textfeld mit Syntax-Einfaerbung,
 * Zeilennummern, unterstrichenen Fehlern und eigener Bildlaufleiste. Die Textlogik (Cursor,
 * Auswahl, Kopieren/Einfuegen, Umbruch) ist Vanillas {@link MultilineTextField}; gezeichnet wird
 * hier Zeichenlauf fuer Zeichenlauf in der Farbe seiner Klasse (BlueprintCode#STYLE_*).
 *
 * <p>Die Schrift ist bei grossem GUI-Massstab kleiner (ganzzahlige Pixel: 3/4 bei Massstab 4,
 * 2/3 bei Massstab 3), damit mehr Code auf den Bildschirm passt, ohne unscharf zu werden.
 */
public final class BlueprintCodeArea extends AbstractWidget {
    public static final int[] STYLE_COLORS = {
            0xFF3B2F20, // TEXT
            0xFF7D7A5C, // COMMENT
            0xFF1F4E9A, // BLOCK
            0xFF6A7FA8, // NAMESPACE
            0xFF7A3E9D, // PROPERTY
            0xFF2E7D32, // VALUE
            0xFF00797A, // ALIAS
            0xFFA0421D, // NUMBER
            0xFF6B5B45, // OPERATOR
            0xFFC62828, // ERROR
            0xFF8D6E63, // AIR
    };
    private static final int BACKGROUND = 0xFFF3EBD2;
    private static final int GUTTER = 0xFFE4D8B4;
    private static final int BORDER = 0xFF8B7355;
    private static final int GUTTER_TEXT = 0xFF9C8B6A;
    private static final int SELECTION = 0x5A3A6EA5;
    private static final int CURSOR = 0xFF20160C;
    private static final int SCROLLBAR = 5;
    private static final int PAD = 3;

    private final Font font;
    private final MultilineTextField field;
    private final float scale;
    private final boolean readOnly;
    private final int gutterWidth;
    private final int wrapWidth;
    private byte[] styles = new byte[0];
    private List<BlueprintCode.Problem> problems = List.of();
    private double scroll;
    private boolean draggingScrollbar;
    private long focusedTime = Util.getMillis();

    public BlueprintCodeArea(Font font, int x, int y, int width, int height, float scale, boolean readOnly, String value,
                             Consumer<String> listener) {
        super(x, y, width, height, Component.translatable("simplebuilding.blueprint.editor.code"));
        this.font = font;
        this.scale = scale;
        this.readOnly = readOnly;
        this.gutterWidth = (int) Math.ceil(font.width("000") * scale) + 5;
        this.wrapWidth = (int) (textWidth() / scale);
        this.field = new MultilineTextField(font, wrapWidth);
        this.field.setCharacterLimit(BlueprintCode.MAX_CODE_LENGTH);
        this.field.setValue(value, true);
        this.field.setValueListener(listener);
        this.field.setCursorListener(this::scrollToCursor);
    }

    private String linesFor;
    private List<int[]> lines = List.of();

    /** Die Bildschirmzeilen, genau wie MultilineTextField sie umbricht (gleiche Breite, gleicher Splitter). */
    private List<int[]> lines() {
        String value = field.value();
        if (value != linesFor) {
            List<int[]> out = new java.util.ArrayList<>();
            if (value.isEmpty()) {
                out.add(new int[]{0, 0});
            } else {
                font.getSplitter().splitLines(value, wrapWidth, net.minecraft.network.chat.Style.EMPTY, false,
                        (style, start, end) -> out.add(new int[]{start, end}));
                if (value.charAt(value.length() - 1) == '\n') {
                    out.add(new int[]{value.length(), value.length()});
                }
            }
            lines = out;
            linesFor = value;
        }
        return lines;
    }

    public String getValue() {
        return field.value();
    }

    public void setHighlight(byte[] styles, List<BlueprintCode.Problem> problems) {
        this.styles = styles;
        this.problems = problems;
    }

    /** Springt zum Fehler (Cursor an seinen Anfang). */
    public void jumpTo(int index) {
        field.setSelecting(false);
        field.seekCursor(net.minecraft.client.gui.components.Whence.ABSOLUTE, Math.min(index, field.value().length()));
    }

    public int cursor() {
        return field.cursor();
    }

    private int textLeft() {
        return getX() + gutterWidth + PAD;
    }

    private int textTop() {
        return getY() + PAD;
    }

    private int textWidth() {
        return width - gutterWidth - PAD * 2 - SCROLLBAR;
    }

    private int viewHeight() {
        return height - PAD * 2;
    }

    private float lineHeight() {
        return font.lineHeight * scale;
    }

    private double contentHeight() {
        return field.getLineCount() * lineHeight();
    }

    private double maxScroll() {
        return Math.max(0, contentHeight() - viewHeight());
    }

    private void setScroll(double value) {
        scroll = Math.max(0, Math.min(maxScroll(), value));
    }

    private void scrollToCursor() {
        double top = field.getLineAtCursor() * lineHeight();
        if (top < scroll) {
            setScroll(top);
        } else if (top + lineHeight() > scroll + viewHeight()) {
            setScroll(top + lineHeight() - viewHeight());
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX(), y = getY();
        g.fill(x, y, x + width, y + height, BORDER);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND);
        g.fill(x + 1, y + 1, x + gutterWidth, y + height - 1, GUTTER);

        String value = field.value();
        int cursor = field.cursor();
        boolean showCursor = isFocused() && !readOnly && ((Util.getMillis() - focusedTime) / 300) % 2 == 0;
        int anchor = ((MultilineTextFieldAccessor) field).simplebuilding$selectCursor();
        int selBegin = field.hasSelection() ? Math.min(cursor, anchor) : -1;
        int selEnd = field.hasSelection() ? Math.max(cursor, anchor) : -1;

        g.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        float lh = lineHeight();
        int first = (int) Math.floor(scroll / lh);
        int lineIndex = 0;
        int logicalLine = 0;
        for (int[] line : lines()) {
            boolean startsLogical = line[0] == 0 || value.charAt(line[0] - 1) == '\n';
            if (startsLogical && lineIndex > 0) {
                logicalLine++;
            }
            if (lineIndex >= first && (lineIndex * lh - scroll) < viewHeight() + lh) {
                float ly = (float) (textTop() + lineIndex * lh - scroll);
                g.pose().pushMatrix();
                g.pose().translate(textLeft(), ly);
                g.pose().scale(scale, scale);
                if (startsLogical) {
                    String number = Integer.toString(logicalLine + 1);
                    int nx = (int) ((-PAD - 2) / scale) - font.width(number);
                    g.text(font, number, nx, 0, GUTTER_TEXT, false);
                }
                drawLine(g, value, line[0], line[1], selBegin, selEnd, cursor, showCursor);
                g.pose().popMatrix();
            }
            lineIndex++;
        }
        g.disableScissor();

        // Bildlaufleiste
        if (maxScroll() > 0) {
            int barX = x + width - SCROLLBAR - 1;
            int trackH = height - 2;
            int thumbH = Math.max(12, (int) (trackH * viewHeight() / contentHeight()));
            int thumbY = y + 1 + (int) ((trackH - thumbH) * (scroll / maxScroll()));
            g.fill(barX, y + 1, barX + SCROLLBAR, y + height - 1, 0x30000000);
            g.fill(barX, thumbY, barX + SCROLLBAR, thumbY + thumbH, 0xFF8B7355);
        }
    }

    private void drawLine(GuiGraphicsExtractor g, String value, int begin, int end,
                          int selBegin, int selEnd, int cursor, boolean showCursor) {
        // Auswahl hinterlegen
        if (selBegin >= 0 && selBegin <= end && selEnd >= begin) {
            int s = Math.max(begin, selBegin);
            int e = Math.min(end, selEnd);
            int sx = font.width(value.substring(begin, s));
            int ex = font.width(value.substring(begin, e)) + (selEnd > end ? 3 : 0);
            g.fill(sx, -1, Math.max(ex, sx + 1), font.lineHeight, SELECTION);
        }
        // Zeichenlaeufe gleicher Klasse in einem Zug
        int xPos = 0;
        int run = begin;
        while (run < end) {
            byte style = styleAt(run);
            int runEnd = run + 1;
            while (runEnd < end && styleAt(runEnd) == style) {
                runEnd++;
            }
            String text = value.substring(run, runEnd);
            g.text(font, text, xPos, 0, STYLE_COLORS[style], false);
            xPos += font.width(text);
            run = runEnd;
        }
        // Fehler unterstreichen
        for (BlueprintCode.Problem problem : problems) {
            int s = Math.max(begin, problem.start());
            int e = Math.min(end, problem.end());
            if (s < e || (problem.start() >= begin && problem.start() <= end && begin == end)) {
                int sx = font.width(value.substring(begin, Math.min(s, end)));
                int ex = Math.max(sx + 3, font.width(value.substring(begin, Math.min(e, end))));
                g.fill(sx, font.lineHeight - 1, ex, font.lineHeight, 0xFFE53935);
            }
        }
        if (showCursor && cursor >= begin && cursor <= end) {
            int cx = font.width(value.substring(begin, cursor));
            g.fill(cx, -1, cx + 1, font.lineHeight, CURSOR);
        }
    }

    private byte styleAt(int index) {
        if (index < styles.length) {
            byte s = styles[index];
            return s >= 0 && s < STYLE_COLORS.length ? s : 0;
        }
        return BlueprintCode.STYLE_TEXT;
    }

    // =====================================================================================
    // EINGABE
    // =====================================================================================

    private void seekToMouse(double mouseX, double mouseY) {
        double tx = (mouseX - textLeft()) / scale;
        double ty = (mouseY - textTop() + scroll) / scale;
        field.seekCursorToPoint(tx, ty);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (maxScroll() > 0 && event.x() >= getX() + width - SCROLLBAR - 2) {
            draggingScrollbar = true;
            return;
        }
        draggingScrollbar = false;
        if (doubleClick) {
            field.selectWordAtCursor();
        } else {
            field.setSelecting(event.hasShiftDown());
            seekToMouse(event.x(), event.y());
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            double trackH = height - 2;
            setScroll(scroll + dy * contentHeight() / trackH);
            return;
        }
        field.setSelecting(true);
        seekToMouse(event.x(), event.y());
        field.setSelecting(event.hasShiftDown());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        setScroll(scroll - scrollY * lineHeight() * 3);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) {
            return false;
        }
        int key = event.key();
        if (readOnly) {
            boolean editing = key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_ENTER
                    || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_TAB
                    || (event.hasControlDown() && (key == GLFW.GLFW_KEY_V || key == GLFW.GLFW_KEY_X));
            if (editing) {
                return key != GLFW.GLFW_KEY_TAB;
            }
            return field.keyPressed(event);
        }
        if (key == GLFW.GLFW_KEY_TAB && !event.hasControlDown()) {
            field.insertText("  ");
            return true;
        }
        return field.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!visible || !isFocused() || readOnly || !event.isAllowedChatCharacter()) {
            return false;
        }
        field.insertText(event.codepointAsString());
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            focusedTime = Util.getMillis();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", getMessage(), getValue()));
    }
}
