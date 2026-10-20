package com.simplelib.api.client.ui;

import com.mojang.blaze3d.platform.InputConstants;

import com.simplelib.mixin.client.CodeAreaSelectAccessor;
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

/**
 * Multi-line code field with syntax colouring, line numbers, underlined problems and its own scroll bar (moved from
 * the SimpleBuilding blueprint editor, owner decision KONZ20 N1; used by the blueprint and the Simple Music sheet
 * editor). Text logic (cursor, selection, copy/paste, wrapping) is Vanilla's {@link MultilineTextField}; drawing goes
 * run by run in the colour of its style byte ({@link #setProblems}: index into the palette given to the constructor,
 * 0 = plain text).
 *
 * <p>At a large GUI scale the font is drawn smaller (whole pixels: 3/4 at scale 4, 2/3 at scale 3) so more code fits
 * without blurring; the caller passes that factor as {@code scale}.
 */
public class UiCodeArea extends AbstractWidget {
    /** A problem range to underline: [start, end) in characters. */
    public record Range(int start, int end) {}

    private static final int BACKGROUND = 0xFFF3EBD2;
    private static final int GUTTER = 0xFFE4D8B4;
    private static final int BORDER = 0xFF8B7355;
    private static final int GUTTER_TEXT = 0xFF9C8B6A;
    private static final int SELECTION = 0x5A3A6EA5;
    private static final int CURSOR = 0xFF20160C;
    private static final int SCROLLBAR = 5;
    private static final int PAD = 3;

    private final int[] palette;
    private final Font font;
    private final MultilineTextField field;
    private final float scale;
    private final boolean readOnly;
    private final int gutterWidth;
    private final int wrapWidth;
    private byte[] styles = new byte[0];
    private List<Range> problems = List.of();
    private double scroll;
    private boolean draggingScrollbar;
    private long focusedTime = Util.getMillis();

    /**
     * @param palette  ARGB colour per style byte (index 0 = plain text)
     * @param title    narration title of the field
     */
    public UiCodeArea(Font font, int x, int y, int width, int height, float scale, boolean readOnly, String value,
                      Consumer<String> listener, int characterLimit, int[] palette, Component title) {
        super(x, y, width, height, title);
        this.palette = palette;
        this.font = font;
        this.scale = scale;
        this.readOnly = readOnly;
        this.gutterWidth = (int) Math.ceil(font.width("000") * scale) + 5;
        this.wrapWidth = (int) (textWidth() / scale);
        this.field = new MultilineTextField(font, wrapWidth);
        this.field.setCharacterLimit(characterLimit);
        this.field.setValue(value, true);
        this.field.setValueListener(listener);
        this.field.setCursorListener(this::scrollToCursor);
    }

    private String linesFor;
    private List<int[]> lines = List.of();

    /** The screen lines exactly as MultilineTextField wraps them (same width, same splitter). */
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

    /** Style byte per character (index into the palette) and the problem ranges to underline. */
    public void setProblems(byte[] styles, List<Range> problems) {
        this.styles = styles;
        this.problems = problems;
    }

    /** Jumps to a problem (cursor to its start). */
    public void jumpTo(int index) {
        field.setSelecting(false);
        field.seekCursor(net.minecraft.client.gui.components.Whence.ABSOLUTE, Math.min(index, field.value().length()));
    }

    public int cursor() {
        return field.cursor();
    }

    /** Inserts text at the cursor (insert bar). */
    public void insert(String text) {
        if (!readOnly) {
            field.insertText(text);
        }
    }

    /** Replaces the whole code (insert an example); the change listener is notified. */
    public void replaceAll(String text) {
        if (!readOnly) {
            field.setValue(text, true);
            setScroll(0);
        }
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
        int anchor = ((CodeAreaSelectAccessor) field).simplelib$selectCursor();
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

        // scroll bar
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
        // selection background
        if (selBegin >= 0 && selBegin <= end && selEnd >= begin) {
            int s = Math.max(begin, selBegin);
            int e = Math.min(end, selEnd);
            int sx = font.width(value.substring(begin, s));
            int ex = font.width(value.substring(begin, e)) + (selEnd > end ? 3 : 0);
            g.fill(sx, -1, Math.max(ex, sx + 1), font.lineHeight, SELECTION);
        }
        // runs of the same style in one go
        int xPos = 0;
        int run = begin;
        while (run < end) {
            byte style = styleAt(run);
            int runEnd = run + 1;
            while (runEnd < end && styleAt(runEnd) == style) {
                runEnd++;
            }
            String text = value.substring(run, runEnd);
            g.text(font, text, xPos, 0, palette[style], false);
            xPos += font.width(text);
            run = runEnd;
        }
        // underline problems
        for (Range problem : problems) {
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
            return s >= 0 && s < palette.length ? s : 0;
        }
        return 0;
    }

    // =====================================================================================
    // INPUT
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
            boolean editing = key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE || key == InputConstants.KEY_RETURN
                    || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB
                    || (event.hasControlDown() && (key == InputConstants.KEY_V || key == InputConstants.KEY_X));
            if (editing) {
                return key != InputConstants.KEY_TAB;
            }
            return field.keyPressed(event);
        }
        if (key == InputConstants.KEY_TAB && !event.hasControlDown()) {
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
        // Text input on/off, as vanilla's MultiLineEditBox and EditBox do. From 26.3 on the window
        // is SDL, which delivers typed characters (charTyped) only while text input is started;
        // without this, Backspace and Control+V (key events) worked and letters never arrived
        // (owner report 2026-09-29). On 26.2 (GLFW) it switches the IME. A signed, read-only
        // field takes no text, so it never asks.
        net.minecraft.client.Minecraft.getInstance().onTextInputFocusChange(this, focused && !readOnly);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", getMessage(), getValue()));
    }
}
