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
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.net.FactsPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The table's screen (N37: in the mod's modern box style and in the guide book's look): on the left the lapis desk box
 * (one big item slot in a slowly turning gold ring, the item's name in its rarity colour, the mod it comes from) over
 * the light inventory box - SimpleLib's {@code UiBoxes}/{@code UiMotifs}; on the right an open book like the guide
 * books: lapis leather with gold stitching and corner caps, parchment pages with an inner frame line, a spine with gold
 * clasps. The book has three pages - 1 Lore, 2 Use, 3 Secret - turned with the bookmark tabs ({@code UiBookmarks}) on
 * its right edge, the page arrows, the mouse wheel, A/D or the arrow keys. Text flows from the left page onto the right
 * one; the "Use" page ends with the automatic recipe line from the server. Flat colours only, no texture.
 */
public class QuestionaryScreen extends AbstractContainerScreen<QuestionaryMenu> {
    public static final int BOOK_X = QuestionaryMenu.LEFT_WIDTH + 4, BOOK_Y = 0, BOOK_W = 224, BOOK_H = QuestionaryMenu.HEIGHT;
    public static final int TAB_W = 15, TAB_OUT = 4, TAB_H = 26, TAB_GAP = 4, TAB_Y = BOOK_Y + 16;
    /** Space kept free at the bottom of both pages for the arrows and the page number. */
    private static final int BOTTOM = 26;
    /** Page edge from the cover edge, text from the page edge; the text area of one page. */
    private static final int PAGE_IN = 5, TEXT_IN = 8, PAGE_MARGIN = PAGE_IN + TEXT_IN, TEXT_W = BOOK_W / 2 - 1 - PAGE_IN - 2 * TEXT_IN, LINE = 10;
    /** Top of the text below the page edge. */
    private static final int TEXT_TOP = 12;
    /** The dotted footer line of both pages (relative to the screen), text stays above it. */
    private static final int FOOTER_Y = BOOK_Y + BOOK_H - PAGE_IN - 2 - BOTTOM + 3;
    private static final int COVER = 0xFF2C4A86, COVER_LIGHT = 0xFF3F63A8, COVER_DARK = 0xFF16264A, PAPER = 0xFFF0E4C6,
            PAPER_DARK = 0xFFE4D4AE, PAPER_LIGHT = 0xFFF8F0DC, PAPER_EDGE = 0xFFD9C79F, FRAME_LINE = 0xFFDCC9A0;
    private static final int INK = 0xFF3B2A1C, INK_SOFT = 0xFF7A6248, INK_FAINT = 0xFFA08A6A, HEADING = 0xFF6B4423;
    private static final int GOLD = 0xFFE8B923, GOLD_LIGHT = 0xFFFFE27A, GOLD_DARK = 0xFFB5851A;
    private static final int[] TAB_COLORS = {0xFFB03A2E, 0xFF3A63B0, 0xFF7A3FA8};
    /** The desk box: deep lapis, its marks are runes (SimpleLib motif). */
    static final UiPalette DESK = UiPalette.derived(0xFF2B3F7C);
    /** Arrow size (guide book style, drawn). */
    private static final int ARROW_W = 18, ARROW_H = 10;
    /** The page last read, kept while the game runs (opening the table again shows the same page). */
    private static int lastPage = 0;

    private int page = lastPage;
    private String cacheKey = "";
    private List<FormattedCharSequence> leftLines = List.of(), rightLines = List.of();

    public QuestionaryScreen(QuestionaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, QuestionaryMenu.WIDTH, QuestionaryMenu.HEIGHT);
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

    /** Screen position of bookmark tab {@code i} (for the client smoke). */
    public int tabCenterX() {
        return leftPos + BOOK_X + BOOK_W + TAB_W / 2;
    }

    public int tabCenterY(int i) {
        return topPos + TAB_Y + i * (TAB_H + TAB_GAP) + TAB_H / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x0 = leftPos, y0 = topPos;
        desk(g, x0, y0);
        int invTop = y0 + QuestionaryMenu.DESK_HEIGHT + QuestionaryMenu.BOX_GAP;
        UiBoxes.box(g, x0, invTop, QuestionaryMenu.LEFT_WIDTH, QuestionaryMenu.INVENTORY_BOX, UiPalette.INVENTORY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i == QuestionaryMenu.ITEM_SLOT) UiBoxes.bigSlot(g, x0 + slot.x, y0 + slot.y, DESK);
            else UiBoxes.slot(g, x0 + slot.x, y0 + slot.y, UiPalette.INVENTORY);
        }
        // Tabs first: their inner end tucks under the cover.
        for (int i = 0; i < 3; i++) {
            boolean active = i == page;
            int tx = x0 + BOOK_X + BOOK_W - 3, ty = y0 + TAB_Y + i * (TAB_H + TAB_GAP), tw = TAB_W + (active ? TAB_OUT : 0);
            boolean hover = inTab(mouseX, mouseY) == i;
            UiBookmarks.tab(g, tx, ty, tw, TAB_H, false, UiPalette.derived(TAB_COLORS[i]), true, hover && !active);
            if (active) {
                g.fill(tx + 3, ty, tx + tw - 2, ty + 1, GOLD);
                g.fill(tx + 3, ty + TAB_H - 1, tx + tw - 2, ty + TAB_H, GOLD);
                g.fill(tx + tw - 1, ty + 2, tx + tw, ty + TAB_H - 2, GOLD);
            }
        }
        book(g, x0 + BOOK_X, y0 + BOOK_Y);
        int bx = x0 + BOOK_X, ay = y0 + BOOK_Y + BOOK_H - 20;
        arrow(g, bx + PAGE_MARGIN, ay, false, page > 0, mouseX, mouseY);
        arrow(g, bx + BOOK_W - PAGE_MARGIN - ARROW_W, ay, true, page < 2, mouseX, mouseY);
    }

    /** The desk box: lapis with rune marks, a gold ring of dots around the item slot that turns while an item lies in it. */
    private void desk(GuiGraphicsExtractor g, int x0, int y0) {
        int w = QuestionaryMenu.LEFT_WIDTH, h = QuestionaryMenu.DESK_HEIGHT;
        UiBoxes.box(g, x0, y0, w, h, DESK);
        int cx = x0 + QuestionaryMenu.SLOT_X + 8, cy = y0 + QuestionaryMenu.SLOT_Y + 8;
        List<int[]> avoid = new ArrayList<>();
        avoid.add(new int[] {x0 + 4, y0 + 4, x0 + 12 + font.width(title), y0 + 15});
        avoid.add(new int[] {cx - 24, cy - 24, cx + 25, cy + 25});
        avoid.add(new int[] {x0 + 8, y0 + NAME_Y - 2, x0 + w - 8, y0 + h - 7});
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

    private static final int RING_DOTS = 24, RING_R = 19, NAME_Y = 64;

    /** The open book in the guide book's look, drawn with fills. */
    private void book(GuiGraphicsExtractor g, int bx, int by) {
        int mid = bx + BOOK_W / 2, bottom = by + BOOK_H;
        // Cover: rim with cut corners, leather with a light top line, gold stitching all round.
        g.fill(bx + 1, by, bx + BOOK_W - 1, bottom, COVER_DARK);
        g.fill(bx, by + 1, bx + BOOK_W, bottom - 1, COVER_DARK);
        g.fill(bx + 1, by + 1, bx + BOOK_W - 1, bottom - 1, COVER);
        g.fill(bx + 2, by + 1, bx + BOOK_W - 2, by + 2, COVER_LIGHT);
        for (int x = bx + 3; x < bx + BOOK_W - 3; x += 3) {
            g.fill(x, by + 2, x + 2, by + 3, GOLD_DARK);
            g.fill(x, bottom - 3, x + 2, bottom - 2, GOLD_DARK);
        }
        for (int y = by + 4; y < bottom - 4; y += 3) {
            g.fill(bx + 2, y, bx + 3, y + 2, GOLD_DARK);
            g.fill(bx + BOOK_W - 3, y, bx + BOOK_W - 2, y + 2, GOLD_DARK);
        }
        // Page block: two layers of page edges below, then the two pages.
        int top = by + PAGE_IN - 1, pageBottom = bottom - PAGE_IN - 2;
        g.fill(bx + PAGE_IN, top + 1, bx + BOOK_W - PAGE_IN, pageBottom + 2, PAPER_EDGE);
        g.fill(bx + PAGE_IN + 1, pageBottom + 1, bx + BOOK_W - PAGE_IN - 1, pageBottom + 2, PAPER_DARK);
        page(g, bx + PAGE_IN, top, mid - 1, pageBottom, 11);
        page(g, mid + 1, top, bx + BOOK_W - PAGE_IN, pageBottom, 29);
        // Spine: shadow towards the fold on both pages, the fold, gold clasps top and bottom.
        for (int i = 0; i < 4; i++) {
            int shade = UiPalette.scale(PAPER, 0.80 + 0.05 * i);
            g.fill(mid - 1 - (4 - i), top, mid - (4 - i), pageBottom, shade);
            g.fill(mid + 1 + (3 - i), top, mid + 2 + (3 - i), pageBottom, shade);
        }
        g.fill(mid - 1, by + 1, mid + 1, bottom - 1, COVER_DARK);
        clasp(g, mid - 2, by);
        clasp(g, mid - 2, bottom - 3);
        // Gold corner caps.
        cap(g, bx, by, 1, 1);
        cap(g, bx + BOOK_W - 1, by, -1, 1);
        cap(g, bx, bottom - 1, 1, -1);
        cap(g, bx + BOOK_W - 1, bottom - 1, -1, -1);
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
        int footer = topPos + FOOTER_Y;
        for (int x = x1 + TEXT_IN; x < x2 - TEXT_IN; x += 2) g.fill(x, footer, x + 1, footer + 1, INK_FAINT);
    }

    /** A gold clasp (4 x 3) over the spine. */
    private static void clasp(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 4, y + 3, GOLD_DARK);
        g.fill(x + 1, y, x + 3, y + 2, GOLD);
        g.fill(x + 1, y, x + 2, y + 1, GOLD_LIGHT);
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
        g.text(font, title, 8, 6, DESK.label(), false);
        g.text(font, playerInventoryTitle, QuestionaryMenu.INVENTORY_X, QuestionaryMenu.INVENTORY_Y - 11, UiPalette.INVENTORY.label(), false);
        ItemStack stack = menu.shown();
        int w = QuestionaryMenu.LEFT_WIDTH;
        if (!stack.isEmpty()) {
            List<FormattedCharSequence> name = font.split(stack.getStyledHoverName(), w - 20);
            int lines = Math.min(2, name.size());
            for (int i = 0; i < lines; i++) g.centeredText(font, name.get(i), w / 2, NAME_Y + i * LINE, 0xFFFFFFFF);
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Component mod = Component.literal(SimpleQuestionary.modName.apply(id.getNamespace())).withStyle(ChatFormatting.ITALIC);
            g.centeredText(font, mod, w / 2, NAME_Y + lines * LINE + 4, UiPalette.mix(DESK.light(), 0xFFFFFFFF, 0.35));
        } else {
            g.centeredText(font, Component.translatable("screen.simplequestionary.welcome.title"), w / 2, NAME_Y + 6,
                    UiPalette.mix(DESK.light(), 0xFFFFFFFF, 0.35));
        }
        for (int i = 0; i < 3; i++) {
            int ty = TAB_Y + i * (TAB_H + TAB_GAP), tx = BOOK_X + BOOK_W - 3 + (i == page ? TAB_OUT : 0);
            g.centeredText(font, String.valueOf(i + 1), tx + 8, ty + 6, 0xFFFFFFFF);
            if (i == 2) {
                // The seal on the secret tab.
                g.fill(tx + 6, ty + 17, tx + 10, ty + 21, GOLD);
                g.fill(tx + 7, ty + 16, tx + 9, ty + 22, GOLD);
            }
        }
        layout(stack);
        int lx = BOOK_X + PAGE_MARGIN, rx = BOOK_X + BOOK_W / 2 + 1 + TEXT_IN, top = BOOK_Y + TEXT_TOP;
        Page p = Page.values()[page];
        Component heading = stack.isEmpty() ? Component.translatable("screen.simplequestionary.welcome.title")
                : Component.translatable("screen.simplequestionary.page." + p.key);
        g.text(font, heading, lx, top, HEADING, false);
        boolean illustrated = !stack.isEmpty() && p == Page.LORE;
        divider(g, lx, top + LINE + 1, TEXT_W - (illustrated ? 36 : 0));
        if (illustrated) {
            g.pose().pushMatrix();
            g.pose().translate(lx + TEXT_W - 32, top + 2);
            g.pose().scale(2.0F, 2.0F);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
        }
        int y = top + firstLineOffset(stack);
        for (FormattedCharSequence line : leftLines) {
            g.text(font, line, lx, y, INK, false);
            y += LINE;
        }
        y = top;
        for (FormattedCharSequence line : rightLines) {
            g.text(font, line, rx, y, INK, false);
            y += LINE;
        }
        if (!stack.isEmpty() && p == Page.USE) facts(g, stack, rx, FOOTER_Y - 2 - footerHeight());
        g.centeredText(font, (page + 1) + " / 3", BOOK_X + BOOK_W * 3 / 4, BOOK_Y + BOOK_H - 19, HEADING);
    }

    /** The guide book's heading line: a thin ink line with a small gold diamond in its middle. */
    private static void divider(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, INK_FAINT);
        int c = x + w / 2;
        g.fill(c - 1, y - 1, c + 2, y + 2, GOLD_DARK);
        g.fill(c, y - 2, c + 1, y + 3, GOLD_DARK);
        g.fill(c, y - 1, c + 1, y + 2, GOLD);
    }

    private int firstLineOffset(ItemStack stack) {
        return !stack.isEmpty() && page == 0 ? 40 : LINE + 6;
    }

    private int footerHeight() {
        return 3 * LINE + 14;
    }

    /** The automatic recipe line: ingredient icons of the first recipe, how many recipes make it and use it. */
    private void facts(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        QuestionaryFacts.Facts facts = FactsPayload.latest;
        g.fill(x, y - 3, x + TEXT_W, y - 2, FRAME_LINE);
        if (!facts.item().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            wrapped(g, Component.translatable("screen.simplequestionary.facts.none"), x, y);
            return;
        }
        if (!facts.madeFrom().isEmpty()) {
            g.text(font, Component.translatable("screen.simplequestionary.facts.made_from"), x, y, HEADING, false);
            int ix = x;
            for (Identifier id : facts.madeFrom()) {
                if (ix + 12 > x + TEXT_W) break;
                Item item = BuiltInRegistries.ITEM.getValue(id);
                g.pose().pushMatrix();
                g.pose().translate(ix, y + LINE);
                g.pose().scale(0.75F, 0.75F);
                g.item(new ItemStack(item), 0, 0);
                g.pose().popMatrix();
                ix += 12;
            }
        } else {
            wrapped(g, Component.translatable("screen.simplequestionary.facts.not_crafted"), x, y);
        }
        wrapped(g, Component.translatable("screen.simplequestionary.facts.recipes", facts.makes(), facts.usedIn()), x, y + LINE + 14);
    }

    private void wrapped(GuiGraphicsExtractor g, Component text, int x, int y) {
        for (FormattedCharSequence line : font.split(text, TEXT_W)) {
            g.text(font, line, x, y, HEADING, false);
            y += LINE;
        }
    }

    /** Wraps the current page into left and right page lines (cached until item, page, language or data change). */
    private void layout(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        String lang = mc.getLanguageManager().getSelected();
        QuestionaryData.Library library = QuestionaryData.current;
        Item item = stack.getItem();
        String key = BuiltInRegistries.ITEM.getKey(item) + "|" + page + "|" + lang + "|" + System.identityHashCode(library) + "|" + stack.isEmpty();
        if (key.equals(cacheKey)) return;
        cacheKey = key;
        List<String> paragraphs = new ArrayList<>();
        if (stack.isEmpty()) {
            paragraphs.add(Component.translatable("screen.simplequestionary.welcome.1").getString());
            paragraphs.add(Component.translatable("screen.simplequestionary.welcome.2").getString());
        } else {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            Predicate<Identifier> inTag = tag -> item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
            QuestionaryData.Resolved resolved = library.resolve(id, QuestionaryData.kindOf(item), inTag, lang);
            String name = stack.getHoverName().getString(), mod = SimpleQuestionary.modName.apply(id.getNamespace());
            for (String paragraph : resolved.page(Page.values()[page])) {
                paragraphs.add(QuestionaryData.format(paragraph, name, mod,
                        other -> BuiltInRegistries.ITEM.getOptional(other).map(i -> new ItemStack(i).getHoverName().getString()).orElse(other.toString())));
            }
            if (paragraphs.isEmpty()) paragraphs.add(Component.translatable("screen.simplequestionary.empty").getString());
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (!lines.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            lines.addAll(font.split(Component.literal(paragraph), TEXT_W));
        }
        int leftRows = (FOOTER_Y - TEXT_TOP - BOOK_Y - firstLineOffset(stack) + 1) / LINE;
        int rightBottom = !stack.isEmpty() && page == 1 ? FOOTER_Y - 2 - footerHeight() - 4 : FOOTER_Y;
        int rightRows = (rightBottom - TEXT_TOP - BOOK_Y + 1) / LINE;
        // A blank line never starts the right page.
        int split = Math.min(leftRows, lines.size());
        leftLines = List.copyOf(lines.subList(0, split));
        List<FormattedCharSequence> rest = new ArrayList<>(lines.subList(split, lines.size()));
        if (!rest.isEmpty() && rest.get(0) == FormattedCharSequence.EMPTY) rest.remove(0);
        if (rest.size() > rightRows) {
            rest = new ArrayList<>(rest.subList(0, rightRows));
            rest.set(rightRows - 1, Component.literal("…").getVisualOrderText());
        }
        rightLines = List.copyOf(rest);
    }

    private int inTab(double mx, double my) {
        for (int i = 0; i < 3; i++) {
            int tx = leftPos + BOOK_X + BOOK_W - 3, ty = topPos + TAB_Y + i * (TAB_H + TAB_GAP);
            if (mx >= tx && mx < tx + TAB_W + TAB_OUT && my >= ty && my < ty + TAB_H) return i;
        }
        return -1;
    }

    private boolean inBook(double mx, double my) {
        return mx >= leftPos + BOOK_X && mx < leftPos + BOOK_X + BOOK_W + TAB_W + TAB_OUT && my >= topPos + BOOK_Y && my < topPos + BOOK_Y + BOOK_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int tab = inTab(mx, my);
        if (tab >= 0) {
            setPage(tab);
            return true;
        }
        int bx = leftPos + BOOK_X, ay = topPos + BOOK_Y + BOOK_H - 20;
        if (my >= ay - 2 && my < ay + ARROW_H + 2) {
            if (mx >= bx + PAGE_MARGIN - 2 && mx < bx + PAGE_MARGIN + ARROW_W + 2) {
                setPage(page - 1);
                return true;
            }
            if (mx >= bx + BOOK_W - PAGE_MARGIN - ARROW_W - 2 && mx < bx + BOOK_W - PAGE_MARGIN + 2) {
                setPage(page + 1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inBook(mouseX, mouseY) && scrollY != 0) {
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
