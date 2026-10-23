package com.simplequestionary.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplequestionary.QuestionaryData;
import com.simplequestionary.QuestionaryData.Page;
import com.simplequestionary.QuestionaryFacts;
import com.simplequestionary.QuestionaryMenu;
import com.simplequestionary.QuestionarySheet;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.net.FactsPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The table's screen (QT23, owner 23.10.): the column - lapis desk box (one big item slot in a slowly turning gold
 * ring, the item's name, the mod it comes from) over the light inventory box, SimpleLib's {@code UiBoxes}/{@code
 * UiMotifs} - stands in the middle, in front of an open book that sticks out left and right (and a little above and
 * below): lapis leather with gold stitching and corner caps, parchment pages with an inner frame line. Three pages -
 * 1 Lore, 2 Use, 3 Secret - turned with the bookmark ribbons on the book's top edge ({@code UiBookmarks}), the page
 * arrows, the mouse wheel, A/D or the arrow keys. The left page is always the text: heading in the guide books'
 * brown, body in warm grey with more line space, keywords bold ({@link QuestionarySheet#spans}); the wheel over it
 * scrolls a long text. The right page shows pictures: 1 the picture plate (the item large, its family), 2 the recipe
 * grid, its station, the recipe counts and where the item comes from, 3 the magnifier (the item's texture five times
 * enlarged with its pixel grid and the texture's name). Flat colours only, no texture. W23-6 (owner 23.10. evening): the
 * book grows with the screen ({@link QuestionarySheet#bookGrowth}), a thick fold with a binding bulge at its lower end, and
 * a soft shadow behind the desk and inventory boxes.
 */
public class QuestionaryScreen extends AbstractContainerScreen<QuestionaryMenu> {
    /** The book of the smallest screen (427 x 240); it grows on bigger screens ({@link QuestionarySheet#bookGrowth}). */
    public static final int BASE_BOOK_Y = 14, BASE_BOOK_W = QuestionaryMenu.WIDTH, BASE_BOOK_H = QuestionaryMenu.HEIGHT - BASE_BOOK_Y;
    private static final int COL_X = QuestionaryMenu.COLUMN_X, COL_Y = QuestionaryMenu.COLUMN_Y, COL_W = QuestionaryMenu.COLUMN_WIDTH;
    /** Bookmark ribbons on the right page's top edge: size, how far the active one stands out, pitch. */
    public static final int TAB_W = 24, TAB_H = 14, TAB_OUT = 3, TAB_PITCH = 28, TAB_X = COL_X + COL_W + 18;
    private static final int PAGE_IN = 5, LINE = 11, PARA = 5, RIGHT_X = COL_X + COL_W + 6;
    /** W23-6: the fold (dark gutter between the pages), the binding bulge below it, the soft shadow behind the boxes. */
    private static final int FOLD = 6, BULGE_W = 22, SHADOW = 3;
    /** The book (relative to the window origin; it grows symmetrically, so the column and its slots stay) and the text
     *  areas of the two visible page parts (left of and right of the column); set in {@link #init}. */
    private int growY, bookX, bookY = BASE_BOOK_Y, bookW = BASE_BOOK_W, bookH = BASE_BOOK_H, leftX, leftW, rightW, textTop, bodyTop, footerY, bodyBottom;
    private static final int COVER = 0xFF2C4A86, COVER_LIGHT = 0xFF3F63A8, COVER_DARK = 0xFF16264A, PAPER = QuestionarySheet.PAPER,
            PAPER_DARK = QuestionarySheet.PAPER_DARK, PAPER_LIGHT = 0xFFF8F0DC, PAPER_EDGE = 0xFFD9C79F, FRAME_LINE = 0xFFDCC9A0;
    private static final int INK_SOFT = 0xFF7A6248, INK_FAINT = 0xFFA08A6A, HEADING = QuestionarySheet.HEADING, BODY = QuestionarySheet.BODY,
            KEYWORD = QuestionarySheet.KEYWORD, CAPTION = QuestionarySheet.CAPTION;
    /** The dark gutter line in the fold and on the binding bulge. */
    private static final int SPINE = 0xFF4A3A28, CURVE = 0x70F8F0DC;
    private static final int GOLD = 0xFFE8B923, GOLD_LIGHT = 0xFFFFE27A, GOLD_DARK = 0xFFB5851A;
    private static final int[] TAB_COLORS = {0xFFB03A2E, 0xFF3A63B0, 0xFF7A3FA8};
    /** The desk box: deep lapis, its marks are runes (SimpleLib motif). */
    static final UiPalette DESK = UiPalette.derived(0xFF2B3F7C);
    /** Sunk slots on the paper (recipe grid, family). */
    private static final UiPalette PAPER_SLOTS = UiPalette.derived(PAPER_DARK);
    private static final int ARROW_W = 18, ARROW_H = 10, RING_DOTS = 24, RING_R = 19, NAME_Y = COL_Y + 64;
    private static final int PLATE = 60, LENS_SCALE = 5, LENS_R = 8 * LENS_SCALE + 5;
    /** The page last read, kept while the game runs (opening the table again shows the same page). */
    private static int lastPage = 0;

    private int page = lastPage, scroll;
    private String cacheKey = "";
    /** Rows of the text page; null = paragraph gap. */
    private List<FormattedCharSequence> rows = List.of();
    private List<Identifier> family = List.of();
    private TextureAtlasSprite sprite;

    public QuestionaryScreen(QuestionaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, QuestionaryMenu.WIDTH, QuestionaryMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        int[] grow = QuestionarySheet.bookGrowth(width, height);
        bookX = -grow[0];
        growY = grow[1];
        bookY = BASE_BOOK_Y - grow[1];
        bookW = BASE_BOOK_W + 2 * grow[0];
        bookH = BASE_BOOK_H + 2 * grow[1];
        leftX = bookX + PAGE_IN + 8;
        leftW = COL_X - 6 - leftX;
        rightW = bookX + bookW - PAGE_IN - 8 - RIGHT_X;
        textTop = bookY + 12;
        bodyTop = textTop + 18;
        footerY = bookY + bookH - 30;
        bodyBottom = footerY - 3;
        cacheKey = ""; // the text page may be wider now: wrap again
    }

    public int page() {
        return page;
    }

    public void setPage(int value) {
        int next = Math.max(0, Math.min(Page.values().length - 1, value));
        if (next != page) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
        page = lastPage = next;
    }

    /** Screen position of bookmark ribbon {@code i} (for the client smoke). */
    public int tabCenterX(int i) {
        return leftPos + TAB_X + i * TAB_PITCH + TAB_W / 2;
    }

    public int tabCenterY(int i) {
        return topPos + bookY - TAB_H / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x0 = leftPos, y0 = topPos;
        // Ribbons first: their lower end tucks under the cover.
        for (int i = 0; i < 3; i++) {
            boolean active = i == page;
            int tx = x0 + TAB_X + i * TAB_PITCH, ty = y0 + bookY + 3 - TAB_H - (active ? TAB_OUT : 0);
            ribbon(g, tx, ty, TAB_W, y0 + bookY + 3 - ty, UiPalette.derived(TAB_COLORS[i]), active, inTab(mouseX, mouseY) == i && !active);
            if (active) g.fill(tx + 2, ty, tx + TAB_W - 2, ty + 1, GOLD);
        }
        book(g, x0 + bookX, y0 + bookY);
        int invTop = y0 + COL_Y + QuestionaryMenu.DESK_HEIGHT + QuestionaryMenu.BOX_GAP;
        shadow(g, x0 + COL_X, y0 + COL_Y, COL_W, QuestionaryMenu.DESK_HEIGHT);
        shadow(g, x0 + COL_X, invTop, COL_W, QuestionaryMenu.INVENTORY_BOX);
        desk(g, x0 + COL_X, y0 + COL_Y);
        UiBoxes.box(g, x0 + COL_X, invTop, COL_W, QuestionaryMenu.INVENTORY_BOX, UiPalette.INVENTORY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i == QuestionaryMenu.ITEM_SLOT) UiBoxes.bigSlot(g, x0 + slot.x, y0 + slot.y, DESK);
            else UiBoxes.slot(g, x0 + slot.x, y0 + slot.y, UiPalette.INVENTORY);
        }
        int ay = y0 + bookY + bookH - 20;
        arrow(g, x0 + leftX, ay, false, page > 0, mouseX, mouseY);
        arrow(g, x0 + bookX + bookW - PAGE_IN - 8 - ARROW_W, ay, true, page < 2, mouseX, mouseY);
    }

    /** A bookmark ribbon standing up out of the book (the {@link UiBookmarks#tab} shape turned upright). */
    private static void ribbon(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p, boolean active, boolean hovered) {
        int fill = active ? p.fill() : hovered ? p.light() : UiPalette.scale(p.fill(), 0.82);
        g.fill(x + 2, y, x + w - 2, y + 1, UiBoxes.RIM);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, UiBoxes.RIM);
        g.fill(x, y + 2, x + 1, y + h, UiBoxes.RIM);
        g.fill(x + w - 1, y + 2, x + w, y + h, UiBoxes.RIM);
        g.fill(x + 1, y + 2, x + w - 1, y + h, fill);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, fill);
        g.fill(x + 1, y + 2, x + 2, y + h, p.light());
    }

    /** The desk box: lapis with rune marks, a gold ring of dots around the item slot that turns while an item lies in it. */
    private void desk(GuiGraphicsExtractor g, int x0, int y0) {
        int w = COL_W, h = QuestionaryMenu.DESK_HEIGHT;
        UiBoxes.box(g, x0, y0, w, h, DESK);
        int cx = leftPos + QuestionaryMenu.SLOT_X + 8, cy = topPos + QuestionaryMenu.SLOT_Y + 8;
        List<int[]> avoid = new ArrayList<>();
        avoid.add(new int[] {x0 + 4, y0 + 4, x0 + 12 + font.width(title), y0 + 15});
        avoid.add(new int[] {cx - 24, cy - 24, cx + 25, cy + 25});
        avoid.add(new int[] {x0 + 8, topPos + NAME_Y - 2, x0 + w - 8, y0 + h - 7});
        UiMotifs.draw(g, 0, 0, UiMotifs.Kind.RUNE, x0 + UiBoxes.FRAME, y0 + UiBoxes.FRAME, w - 2 * UiBoxes.FRAME,
                h - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, DESK, avoid, 37);
        boolean filled = !menu.shown().isEmpty();
        double turn = filled ? (Util.getMillis() % 24000L) / 24000.0 * Math.PI * 2 : 0;
        for (int i = 0; i < RING_DOTS; i++) {
            double angle = turn + i * Math.PI * 2 / RING_DOTS;
            int px = cx + (int) Math.round(Math.cos(angle) * RING_R) - 1, py = cy + (int) Math.round(Math.sin(angle) * RING_R) - 1;
            int color = i % 4 == 0 ? (filled ? GOLD_LIGHT : GOLD) : (filled ? GOLD : GOLD_DARK);
            g.fill(px, py, px + (i % 4 == 0 ? 2 : 1), py + (i % 4 == 0 ? 2 : 1), color);
        }
    }

    /** The open book in the guide book's look, drawn with fills; the column covers its spine. */
    private void book(GuiGraphicsExtractor g, int bx, int by) {
        int mid = bx + bookW / 2, bottom = by + bookH;
        // Cover: rim with cut corners, leather with a light top line, gold stitching all round.
        g.fill(bx + 1, by, bx + bookW - 1, bottom, COVER_DARK);
        g.fill(bx, by + 1, bx + bookW, bottom - 1, COVER_DARK);
        g.fill(bx + 1, by + 1, bx + bookW - 1, bottom - 1, COVER);
        g.fill(bx + 2, by + 1, bx + bookW - 2, by + 2, COVER_LIGHT);
        for (int x = bx + 3; x < bx + bookW - 3; x += 3) {
            g.fill(x, by + 2, x + 2, by + 3, GOLD_DARK);
            g.fill(x, bottom - 3, x + 2, bottom - 2, GOLD_DARK);
        }
        for (int y = by + 4; y < bottom - 4; y += 3) {
            g.fill(bx + 2, y, bx + 3, y + 2, GOLD_DARK);
            g.fill(bx + bookW - 3, y, bx + bookW - 2, y + 2, GOLD_DARK);
        }
        // Page block: two layers of page edges below, then the two pages.
        int top = by + PAGE_IN - 1, pageBottom = bottom - PAGE_IN - 2;
        g.fill(bx + PAGE_IN, top + 1, bx + bookW - PAGE_IN, pageBottom + 2, PAPER_EDGE);
        g.fill(bx + PAGE_IN + 1, pageBottom + 1, bx + bookW - PAGE_IN - 1, pageBottom + 2, PAPER_DARK);
        page(g, bx + PAGE_IN, top, mid - 1, pageBottom, 11);
        page(g, mid + 1, top, bx + bookW - PAGE_IN, pageBottom, 29);
        fold(g, mid, top, pageBottom + 2);
        bulge(g, mid, pageBottom - 1, bottom + Math.min(3, 1 + growY));
        // Gold corner caps.
        cap(g, bx, by, 1, 1);
        cap(g, bx + bookW - 1, by, -1, 1);
        cap(g, bx, bottom - 1, 1, -1);
        cap(g, bx + bookW - 1, bottom - 1, -1, -1);
    }

    /** W23-6: the fold between the pages, thick and plain to see above and below the column and in the gap between its
     *  boxes: the paper darkens towards the binding (see-through, the page marks stay), a dark gutter line in the middle,
     *  a light page curve on both sides. */
    private static void fold(GuiGraphicsExtractor g, int mid, int y1, int y2) {
        for (int k = FOLD; k >= 1; k--) {
            int shade = (0x20 + (FOLD - k) * 0x1C) << 24 | 0x5A4630;
            g.fill(mid - 1 - k, y1, mid - k, y2, shade);
            g.fill(mid + k, y1, mid + k + 1, y2, shade);
        }
        g.fill(mid - FOLD - 2, y1, mid - FOLD - 1, y2, CURVE);
        g.fill(mid + FOLD + 1, y1, mid + FOLD + 2, y2, CURVE);
        g.fill(mid - 1, y1, mid + 1, y2, SPINE);
    }

    /** W23-6: the binding bulge where the fold meets the lower edge, like the rounded spine of a thick bound book: a
     *  half-round lump of lapis leather from the page edge down to {@code y2} (a little below the cover), dark rim, light
     *  upper line, gold stitch and the gutter line. */
    private static void bulge(GuiGraphicsExtractor g, int mid, int y1, int y2) {
        int rows = y2 - y1;
        for (int i = 0; i < rows; i++) {
            int y = y1 + i, half = Math.max(2, (int) Math.round(BULGE_W / 2.0 * Math.sqrt(1.0 - (double) i * i / (rows * rows))));
            g.fill(mid - half, y, mid + half, y + 1, COVER_DARK);
            if (i < rows - 1 && half > 2) g.fill(mid - half + 1, y, mid + half - 1, y + 1, i == 0 ? COVER_LIGHT : COVER);
        }
        int sy = y1 + rows / 2, sh = (int) Math.round(BULGE_W / 2.0 * Math.sqrt(1.0 - 0.25)) - 3;
        for (int x = mid - sh; x < mid + sh; x += 3) g.fill(x, sy, x + 2, sy + 1, GOLD_DARK);
        g.fill(mid - 1, y1, mid + 1, y2 - 1, SPINE);
    }

    /** W23-6: a soft shadow behind a box (three see-through rings, a little down and right), so the box stands off the page. */
    private static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int i = SHADOW; i >= 1; i--) g.fill(x - i + 1, y - i + 2, x + w + i + 1, y + h + i + 1, 0x1E000000);
    }

    /** One parchment page: speckled paper, a thin inner frame with small corner squares, the dotted footer line. */
    private void page(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int seed) {
        g.fill(x1, y1, x2, y2, PAPER);
        int w = x2 - x1, h = y2 - y1;
        for (int i = 0; i < 90; i++) {
            int hash = (i + 1) * 0x9E3779B1 ^ seed * 0x85EBCA6B;
            hash ^= hash >>> 15;
            int px = x1 + Math.floorMod(hash, w), py = y1 + Math.floorMod(hash >>> 8, h);
            g.fill(px, py, px + 1, py + 1, (hash & 1) == 0 ? PAPER_DARK : PAPER_LIGHT);
        }
        int fx1 = x1 + 3, fy1 = y1 + 3, fx2 = x2 - 3, fy2 = y2 - 3;
        g.fill(fx1, fy1, fx2, fy1 + 1, FRAME_LINE);
        g.fill(fx1, fy2 - 1, fx2, fy2, FRAME_LINE);
        g.fill(fx1, fy1, fx1 + 1, fy2, FRAME_LINE);
        g.fill(fx2 - 1, fy1, fx2, fy2, FRAME_LINE);
        for (int[] c : new int[][] {{fx1, fy1}, {fx2 - 3, fy1}, {fx1, fy2 - 3}, {fx2 - 3, fy2 - 3}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, FRAME_LINE);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 2, c[1] + 2, INK_FAINT);
        }
        int footer = topPos + footerY;
        for (int x = x1 + 8; x < x2 - 8; x += 2) g.fill(x, footer, x + 1, footer + 1, INK_FAINT);
    }

    /** A gold corner cap (L of 5 px with a light corner pixel) at corner pixel {@code x, y}, opening towards {@code dx, dy}. */
    private static void cap(GuiGraphicsExtractor g, int x, int y, int dx, int dy) {
        for (int i = 0; i < 5; i++) {
            int c = i == 0 ? GOLD_LIGHT : i < 3 ? GOLD : GOLD_DARK;
            g.fill(x + i * dx, y, x + i * dx + 1, y + 1, c);
            g.fill(x, y + i * dy, x + 1, y + i * dy + 1, c);
            if (i < 3) {
                g.fill(x + i * dx + dx, y + dy, x + i * dx + dx + 1, y + dy + 1, i == 0 ? GOLD : GOLD_DARK);
                g.fill(x + dx, y + i * dy + dy, x + dx + 1, y + i * dy + dy + 1, i == 0 ? GOLD : GOLD_DARK);
            }
        }
    }

    /** A page arrow in the guide book's style ({@link #ARROW_W} x {@link #ARROW_H}): soft ink, gold under the mouse. */
    private void arrow(GuiGraphicsExtractor g, int x, int y, boolean forward, boolean enabled, int mx, int my) {
        boolean over = enabled && mx >= x && mx < x + ARROW_W && my >= y && my < y + ARROW_H;
        int color = !enabled ? 0x40000000 | (INK_FAINT & 0xFFFFFF) : over ? GOLD_DARK : INK_SOFT;
        int light = !enabled ? 0 : over ? GOLD_LIGHT : INK_FAINT;
        for (int c = 0; c < ARROW_W; c++) {
            int col = forward ? c : ARROW_W - 1 - c;
            int px = x + col;
            if (c < ARROW_W - 5) {
                g.fill(px, y + 3, px + 1, y + 7, color);
                if (light != 0) g.fill(px, y + 3, px + 1, y + 4, light);
            } else {
                int i = c - (ARROW_W - 5);
                g.fill(px, y + i, px + 1, y + ARROW_H - i, color);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, COL_X + 8, COL_Y + 6, DESK.label(), false);
        g.text(font, playerInventoryTitle, QuestionaryMenu.INVENTORY_X, QuestionaryMenu.INVENTORY_Y - 11, UiPalette.INVENTORY.label(), false);
        ItemStack stack = menu.shown();
        int mid = COL_X + COL_W / 2;
        if (!stack.isEmpty()) {
            List<FormattedCharSequence> name = font.split(stack.getStyledHoverName(), COL_W - 20);
            int lines = Math.min(2, name.size());
            for (int i = 0; i < lines; i++) g.centeredText(font, name.get(i), mid, NAME_Y + i * 10, 0xFFFFFFFF);
            Component mod = Component.literal(modName(stack)).withStyle(ChatFormatting.ITALIC);
            g.centeredText(font, mod, mid, NAME_Y + lines * 10 + 4, UiPalette.mix(DESK.light(), 0xFFFFFFFF, 0.35));
        } else {
            g.centeredText(font, Component.translatable("screen.simplequestionary.welcome.title"), mid, NAME_Y + 6,
                    UiPalette.mix(DESK.light(), 0xFFFFFFFF, 0.35));
        }
        for (int i = 0; i < 3; i++) {
            int ty = bookY + 3 - TAB_H - (i == page ? TAB_OUT : 0);
            g.centeredText(font, String.valueOf(i + 1), TAB_X + i * TAB_PITCH + TAB_W / 2, ty + 3, 0xFFFFFFFF);
        }
        layout(stack);
        // Left page: heading, divider, the text rows from the scroll position on, a scroll bar when it does not fit.
        Page p = Page.values()[page];
        Component heading = stack.isEmpty() ? Component.translatable("screen.simplequestionary.welcome.title")
                : Component.translatable("screen.simplequestionary.page." + p.key);
        g.text(font, heading, leftX, textTop, HEADING, false);
        divider(g, leftX, textTop + LINE + 1, leftW);
        int y = bodyTop;
        for (int i = scroll; i < rows.size(); i++) {
            FormattedCharSequence row = rows.get(i);
            int h = row == null ? PARA : LINE;
            if (y + h > bodyBottom + 2) break;
            if (row != null) g.text(font, row, leftX, y, BODY, false);
            y += h;
        }
        int total = height(0), track = bodyBottom - bodyTop;
        if (total > track) {
            int bar = COL_X - 4, thumb = Math.max(8, track * track / total), pos = (track - thumb) * height(0, scroll) / Math.max(1, total - track);
            g.fill(bar, bodyTop, bar + 1, bodyBottom, FRAME_LINE);
            g.fill(bar - 1, bodyTop + Math.min(pos, track - thumb), bar + 1, bodyTop + Math.min(pos, track - thumb) + thumb, INK_SOFT);
        }
        // Right page: picture plate, recipe, magnifier.
        if (stack.isEmpty()) {
            g.text(font, Component.translatable("screen.simplequestionary.sheet.plate"), RIGHT_X, textTop, HEADING, false);
            divider(g, RIGHT_X, textTop + LINE + 1, rightW);
            wrapped(g, Component.translatable("screen.simplequestionary.sheet.empty"), RIGHT_X, bodyTop, BODY);
        } else if (p == Page.LORE) {
            plate(g, stack);
        } else if (p == Page.USE) {
            recipe(g, stack);
        } else {
            lens(g, stack);
        }
        int ay = bookY + bookH - 20;
        String number = (page + 1) + " / 3";
        g.text(font, number, RIGHT_X + (rightW - ARROW_W - font.width(number)) / 2, ay + 1, HEADING, false);
    }

    private static String modName(ItemStack stack) {
        return SimpleQuestionary.modName.apply(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    /** Right page heading with the guide book's divider below it. */
    private void heading(GuiGraphicsExtractor g, String key) {
        g.text(font, Component.translatable("screen.simplequestionary.sheet." + key), RIGHT_X, textTop, HEADING, false);
        divider(g, RIGHT_X, textTop + LINE + 1, rightW);
    }

    /** Page 1: the item three times enlarged in a framed plate, its name and mod below, then its family. */
    private void plate(GuiGraphicsExtractor g, ItemStack stack) {
        heading(g, "plate");
        int px = RIGHT_X + (rightW - PLATE) / 2, py = textTop + 18;
        g.fill(px, py, px + PLATE, py + PLATE, FRAME_LINE);
        g.fill(px + 1, py + 1, px + PLATE - 1, py + PLATE - 1, PAPER_DARK);
        g.fill(px + 2, py + 2, px + PLATE - 2, py + PLATE - 2, PAPER_LIGHT);
        for (int[] c : new int[][] {{px, py}, {px + PLATE - 3, py}, {px, py + PLATE - 3}, {px + PLATE - 3, py + PLATE - 3}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, GOLD_DARK);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 2, c[1] + 2, GOLD_LIGHT);
        }
        g.pose().pushMatrix();
        g.pose().translate(px + 6, py + 6);
        g.pose().scale(3.0F, 3.0F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
        int y = py + PLATE + 4;
        List<FormattedCharSequence> name = font.split(Component.literal(stack.getHoverName().getString()).withStyle(keyword()), rightW);
        for (int i = 0; i < Math.min(2, name.size()); i++, y += LINE) g.centeredText(font, name.get(i), RIGHT_X + rightW / 2, y, KEYWORD);
        String mod = modName(stack);
        g.text(font, mod, RIGHT_X + (rightW - font.width(mod)) / 2, y, CAPTION, false);
        y += LINE + 6;
        if (family.isEmpty()) return;
        g.text(font, Component.translatable("screen.simplequestionary.sheet.family"), RIGHT_X, y, HEADING, false);
        y += LINE + 1;
        int shown = Math.min(family.size(), (rightW + 2) / 18);
        for (int i = 0; i < shown; i++) {
            int sx = RIGHT_X + 1 + i * 18;
            UiBoxes.slot(g, sx, y, PAPER_SLOTS);
            g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(family.get(i))), sx, y);
        }
        if (family.size() > shown) {
            String more = "+" + (family.size() - shown);
            g.text(font, more, RIGHT_X + rightW - font.width(more), y + 21, CAPTION, false);
        }
    }

    /** Page 2: the recipe grid with arrow and result, the station, the recipe counts, then where the item comes from. */
    private void recipe(GuiGraphicsExtractor g, ItemStack stack) {
        heading(g, "recipe");
        QuestionaryFacts.Facts facts = FactsPayload.latest;
        int y = textTop + 20;
        if (!facts.item().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            wrapped(g, Component.translatable("screen.simplequestionary.facts.none"), RIGHT_X, y, BODY);
            return;
        }
        if (facts.madeFrom().isEmpty()) {
            // Only special recipes (no grid to show) make it: just the counts below.
            if (facts.makes() == 0) y = wrapped(g, Component.translatable("screen.simplequestionary.facts.not_crafted"), RIGHT_X, y, BODY) + 2;
        } else {
            for (int i = 0; i < 9; i++) {
                int sx = RIGHT_X + 1 + (i % 3) * 18, sy = y + (i / 3) * 18;
                UiBoxes.slot(g, sx, sy, PAPER_SLOTS);
                if (i < facts.madeFrom().size()) g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(facts.madeFrom().get(i))), sx, sy);
            }
            int ax = RIGHT_X + 58, ay = y + 21;
            for (int k = 0; k < 12; k++) {
                if (k < 8) g.fill(ax + k, ay + 3, ax + k + 1, ay + 7, CAPTION);
                else g.fill(ax + k, ay + k - 7, ax + k + 1, ay + 17 - k, CAPTION);
            }
            UiBoxes.bigSlot(g, RIGHT_X + 80, y + 18, PAPER_SLOTS);
            g.item(stack, RIGHT_X + 80, y + 18);
            y += 58;
            Item station = BuiltInRegistries.ITEM.getValue(facts.station());
            if (station != null && station != net.minecraft.world.item.Items.AIR) {
                ItemStack shown = new ItemStack(station);
                UiBoxes.slot(g, RIGHT_X + 1, y, PAPER_SLOTS);
                g.item(shown, RIGHT_X + 1, y);
                g.text(font, ellipsis(shown.getHoverName().getString(), rightW - 21), RIGHT_X + 21, y + 4, CAPTION, false);
                y += 22;
            }
        }
        y = wrapped(g, Component.translatable("screen.simplequestionary.facts.recipes", number(facts.makes()), number(facts.usedIn())), RIGHT_X, y, BODY) + 6;
        g.text(font, Component.translatable("screen.simplequestionary.sheet.origin"), RIGHT_X, y, HEADING, false);
        g.fill(RIGHT_X, y + LINE - 1, RIGHT_X + rightW, y + LINE, FRAME_LINE);
        String origin = "screen.simplequestionary.origin." + QuestionarySheet.origin(facts, stack.getItem()).key();
        wrapped(g, Component.empty().append(Component.translatable(origin).withStyle(keyword())).append(Component.translatable(origin + ".hint")),
                RIGHT_X, y + LINE + 3, BODY);
    }

    /** Page 3: the item's texture five times enlarged in a gold magnifier ring with its pixel grid, then its name. */
    private void lens(GuiGraphicsExtractor g, ItemStack stack) {
        heading(g, "lens");
        int d = 16 * LENS_SCALE, cx = RIGHT_X + rightW / 2, cy = textTop + 20 + LENS_R, r = LENS_R;
        // The ring and the glass, row by row: dark rim, gold ring, light glass.
        for (int dy = -r - 2; dy <= r + 2; dy++) {
            int outer = halfWidth(r + 2, dy), ring = halfWidth(r + 1, dy), glass = halfWidth(r - 1, dy);
            int y = cy + dy;
            if (outer >= 0) g.fill(cx - outer, y, cx + outer, y + 1, GOLD_DARK);
            if (ring >= 0) g.fill(cx - ring, y, cx + ring, y + 1, GOLD);
            if (glass >= 0) g.fill(cx - glass, y, cx + glass, y + 1, PAPER_LIGHT);
        }
        for (int i = 0; i < 6; i++) g.fill(cx - r + 8 + i, cy - r + 6 - i / 2, cx - r + 9 + i, cy - r + 7 - i / 2, GOLD_LIGHT);
        int x0 = cx - d / 2, y0 = cy - d / 2;
        if (sprite != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x0, y0, d, d);
        else {
            g.pose().pushMatrix();
            g.pose().translate(x0, y0);
            g.pose().scale(LENS_SCALE, LENS_SCALE);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
        }
        for (int k = 0; k <= d; k += LENS_SCALE) {
            g.fill(x0 + k, y0, x0 + k + 1, y0 + d, 0x18000000);
            g.fill(x0, y0 + k, x0 + d, y0 + k + 1, 0x18000000);
        }
        // The handle down to the right.
        for (int i = 0; i < 9; i++) {
            int hx = cx + r - 3 + i, hy = cy + r - 3 + i;
            if (hx + 3 > RIGHT_X + rightW + 4) break;
            g.fill(hx, hy, hx + 3, hy + 2, i < 2 ? GOLD_DARK : COVER_DARK);
        }
        int y = cy + r + 6;
        g.text(font, Component.translatable("screen.simplequestionary.sheet.texture"), RIGHT_X, y, HEADING, false);
        String name = sprite != null ? sprite.contents().name().toString() : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        wrapped(g, Component.literal(name), RIGHT_X, y + LINE + 1, CAPTION);
    }

    /** Half width of a disc of radius {@code r} in row {@code dy} (-1 outside). */
    private static int halfWidth(int r, int dy) {
        double v = (double) r * r - (dy + 0.5) * (dy + 0.5);
        return v < 0 ? -1 : (int) Math.round(Math.sqrt(v));
    }

    private static Style keyword() {
        return Style.EMPTY.withBold(true).withColor(KEYWORD & 0xFFFFFF);
    }

    private static Component number(int n) {
        return Component.literal(String.valueOf(n)).withStyle(keyword());
    }

    private String ellipsis(String text, int width) {
        if (font.width(text) <= width) return text;
        while (!text.isEmpty() && font.width(text + "…") > width) text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    /** The guide book's heading line: a thin ink line with a small gold diamond in its middle. */
    private static void divider(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, INK_FAINT);
        int c = x + w / 2;
        g.fill(c - 1, y - 1, c + 2, y + 2, GOLD_DARK);
        g.fill(c, y - 2, c + 1, y + 3, GOLD_DARK);
        g.fill(c, y - 1, c + 1, y + 2, GOLD);
    }

    /** Wrapped text on the right page; returns the y below it. */
    private int wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        for (FormattedCharSequence line : font.split(text, rightW)) {
            g.text(font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    /** Height of the text rows from {@code from} to {@code to} (exclusive). */
    private int height(int from, int to) {
        int h = 0;
        for (int i = from; i < to && i < rows.size(); i++) h += rows.get(i) == null ? PARA : LINE;
        return h;
    }

    private int height(int from) {
        return height(from, rows.size());
    }

    /** Largest scroll position: the last rows exactly fill the text area. */
    private int maxScroll() {
        int track = bodyBottom - bodyTop, s = rows.size();
        while (s > 0 && height(s - 1) <= track) s--;
        return s;
    }

    /** Wraps the current page's text, finds the item's family and its texture (cached until item, page, language or data change). */
    private void layout(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        String lang = mc.getLanguageManager().getSelected();
        QuestionaryData.Library library = QuestionaryData.current;
        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String itemKey = id + "|" + lang + "|" + System.identityHashCode(library) + "|" + stack.isEmpty();
        String key = itemKey + "|" + page;
        if (key.equals(cacheKey)) return;
        boolean newItem = !cacheKey.startsWith(itemKey + "|");
        cacheKey = key;
        scroll = 0;
        List<List<QuestionarySheet.Span>> paragraphs = new ArrayList<>();
        Function<Identifier, Predicate<Identifier>> tagsOf = other -> {
            Item value = BuiltInRegistries.ITEM.getValue(other);
            return tag -> value.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
        };
        if (stack.isEmpty()) {
            for (String k : List.of("welcome.1", "welcome.2")) {
                paragraphs.add(List.of(new QuestionarySheet.Span(Component.translatable("screen.simplequestionary." + k).getString(), false)));
            }
            family = List.of();
            sprite = null;
        } else {
            QuestionaryData.Resolved resolved = library.resolve(id, QuestionaryData.kindOf(item), tagsOf.apply(id), lang);
            String name = stack.getHoverName().getString(), mod = SimpleQuestionary.modName.apply(id.getNamespace());
            Function<Identifier, String> names = other -> BuiltInRegistries.ITEM.getOptional(other).map(i -> new ItemStack(i).getHoverName().getString()).orElse(other.toString());
            for (String paragraph : resolved.page(Page.values()[page])) paragraphs.add(QuestionarySheet.spans(paragraph, name, mod, names));
            if (paragraphs.isEmpty()) paragraphs.add(List.of(new QuestionarySheet.Span(Component.translatable("screen.simplequestionary.empty").getString(), false)));
            if (newItem) {
                family = QuestionarySheet.family(library.family(id, tagsOf.apply(id), lang), id, BuiltInRegistries.ITEM.keySet(), tagsOf);
                sprite = textureOf(mc, stack);
            }
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (List<QuestionarySheet.Span> spans : paragraphs) {
            if (!lines.isEmpty()) lines.add(null);
            MutableComponent text = Component.empty();
            for (QuestionarySheet.Span span : spans) text.append(Component.literal(span.text()).withStyle(span.bold() ? keyword() : Style.EMPTY));
            lines.addAll(font.split(text, leftW));
        }
        rows = lines;
    }

    /** The sprite the item model shows (its particle texture), or null. */
    private static TextureAtlasSprite textureOf(Minecraft mc, ItemStack stack) {
        try {
            ItemStackRenderState state = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, mc.level, null, 0);
            var material = state.pickParticleMaterial(RandomSource.create(0));
            return material == null ? null : material.sprite();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private int inTab(double mx, double my) {
        for (int i = 0; i < 3; i++) {
            int tx = leftPos + TAB_X + i * TAB_PITCH, ty = topPos + bookY + 3 - TAB_H - TAB_OUT;
            if (mx >= tx && mx < tx + TAB_W && my >= ty && my < topPos + bookY + 3) return i;
        }
        return -1;
    }

    private boolean inBook(double mx, double my) {
        return mx >= leftPos + bookX && mx < leftPos + bookX + bookW && my >= topPos + bookY - TAB_H - TAB_OUT && my < topPos + bookY + bookH;
    }

    private boolean inText(double mx, double my) {
        return mx >= leftPos + bookX && mx < leftPos + COL_X && my >= topPos + bodyTop && my < topPos + bodyBottom;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int tab = inTab(mx, my);
        if (tab >= 0) {
            setPage(tab);
            return true;
        }
        int ay = topPos + bookY + bookH - 20;
        if (my >= ay - 2 && my < ay + ARROW_H + 2) {
            if (mx >= leftPos + leftX - 2 && mx < leftPos + leftX + ARROW_W + 2) {
                setPage(page - 1);
                return true;
            }
            int right = leftPos + bookX + bookW - PAGE_IN - 8 - ARROW_W;
            if (mx >= right - 2 && mx < right + ARROW_W + 2) {
                setPage(page + 1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && inText(mouseX, mouseY) && maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        boolean column = mouseX >= leftPos + COL_X && mouseX < leftPos + COL_X + COL_W;
        if (inBook(mouseX, mouseY) && !column && scrollY != 0) {
            setPage(page + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A) {
            setPage(page - 1);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D) {
            setPage(page + 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return super.hasClickedOutside(mx, my, xo, yo) && !inBook(mx, my);
    }
}
