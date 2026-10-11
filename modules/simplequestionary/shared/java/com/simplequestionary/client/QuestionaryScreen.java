package com.simplequestionary.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The table's screen: a Vanilla-like box on the left (one item slot, the item's name in its rarity colour, the
 * inventory) and an open book on the right. The book has three pages - 1 Lore, 2 Use, 3 Secret - turned with the
 * bookmark tabs on its right edge, the Vanilla page arrows, the mouse wheel, A/D or the arrow keys. Text flows from the
 * left page onto the right one; the "Use" page ends with the automatic recipe line from the server. Flat colours only
 * (simplelib {@code UiBoxes}/{@code UiBookmarks}), no texture per size.
 */
public class QuestionaryScreen extends AbstractContainerScreen<QuestionaryMenu> {
    public static final int BOOK_X = QuestionaryMenu.LEFT_WIDTH + 4, BOOK_Y = 4, BOOK_W = 224, BOOK_H = 208;
    public static final int TAB_W = 15, TAB_OUT = 4, TAB_H = 26, TAB_GAP = 4, TAB_Y = BOOK_Y + 14;
    /** Space kept free at the bottom of both pages for the arrows and the page number. */
    private static final int BOTTOM = 24;
    private static final int PAGE_MARGIN = 8, TEXT_W = BOOK_W / 2 - 2 * PAGE_MARGIN - 2, LINE = 10;
    private static final int COVER = 0xFF2C4A86, COVER_DARK = 0xFF16264A, PAPER = 0xFFF4E9CF, PAPER_EDGE = 0xFFD9C79F;
    private static final int INK = 0xFF3B2A1A, HEADING = 0xFF6B4423, GOLD = 0xFFE8B923;
    private static final int[] TAB_COLORS = {0xFFB03A2E, 0xFF3A63B0, 0xFF7A3FA8};
    private static final Identifier FORWARD = Identifier.withDefaultNamespace("widget/page_forward"),
            FORWARD_HOVER = Identifier.withDefaultNamespace("widget/page_forward_highlighted"),
            BACKWARD = Identifier.withDefaultNamespace("widget/page_backward"),
            BACKWARD_HOVER = Identifier.withDefaultNamespace("widget/page_backward_highlighted");
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
        UiBoxes.box(g, x0, y0, QuestionaryMenu.LEFT_WIDTH, QuestionaryMenu.LEFT_HEIGHT, UiPalette.INVENTORY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i == QuestionaryMenu.ITEM_SLOT) UiBoxes.bigSlot(g, x0 + slot.x, y0 + slot.y, UiPalette.INVENTORY);
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
        int bx = x0 + BOOK_X, by = y0 + BOOK_Y, mid = bx + BOOK_W / 2;
        g.fill(bx, by, bx + BOOK_W, by + BOOK_H, COVER_DARK);
        g.fill(bx + 1, by + 1, bx + BOOK_W - 1, by + BOOK_H - 1, COVER);
        // Page stack below the pages, then the two pages and the spine's shadow.
        g.fill(bx + 5, by + 5, bx + BOOK_W - 5, by + BOOK_H - 3, PAPER_EDGE);
        g.fill(bx + 4, by + 4, mid - 1, by + BOOK_H - 6, PAPER);
        g.fill(mid + 1, by + 4, bx + BOOK_W - 4, by + BOOK_H - 6, PAPER);
        for (int i = 0; i < 4; i++) {
            int shade = UiPalette.scale(PAPER, 0.80 + 0.05 * i);
            g.fill(mid - 1 - (4 - i), by + 4, mid - (4 - i), by + BOOK_H - 6, shade);
            g.fill(mid + 1 + (3 - i), by + 4, mid + 2 + (3 - i), by + BOOK_H - 6, shade);
        }
        g.fill(mid - 1, by + 2, mid + 1, by + BOOK_H - 2, COVER_DARK);
        // Page arrows (Vanilla book sprites), greyed out at the ends.
        int ay = by + BOOK_H - 22;
        arrow(g, BACKWARD, BACKWARD_HOVER, bx + 8, ay, page > 0, mouseX, mouseY);
        arrow(g, FORWARD, FORWARD_HOVER, bx + BOOK_W - 8 - 23, ay, page < 2, mouseX, mouseY);
    }

    private void arrow(GuiGraphicsExtractor g, Identifier sprite, Identifier hover, int x, int y, boolean enabled, int mx, int my) {
        boolean over = enabled && mx >= x && mx < x + 23 && my >= y && my < y + 13;
        if (enabled) g.blitSprite(RenderPipelines.GUI_TEXTURED, over ? hover : sprite, x, y, 23, 13);
        else g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 23, 13, 0.3F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 8, 6, UiPalette.INVENTORY.label(), false);
        g.text(font, playerInventoryTitle, QuestionaryMenu.INVENTORY_X, QuestionaryMenu.INVENTORY_Y - 11, UiPalette.INVENTORY.label(), false);
        ItemStack stack = menu.shown();
        if (!stack.isEmpty()) {
            List<FormattedCharSequence> name = font.split(stack.getStyledHoverName(), QuestionaryMenu.LEFT_WIDTH - 20);
            for (int i = 0; i < Math.min(2, name.size()); i++) {
                g.centeredText(font, name.get(i), QuestionaryMenu.LEFT_WIDTH / 2, QuestionaryMenu.SLOT_Y + 26 + i * LINE, 0xFFFFFFFF);
            }
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
        int lx = BOOK_X + PAGE_MARGIN, rx = BOOK_X + BOOK_W / 2 + PAGE_MARGIN, top = BOOK_Y + PAGE_MARGIN;
        Page p = Page.values()[page];
        Component heading = stack.isEmpty() ? Component.translatable("screen.simplequestionary.welcome.title")
                : Component.translatable("screen.simplequestionary.page." + p.key);
        g.text(font, heading, lx, top, HEADING, false);
        boolean illustrated = !stack.isEmpty() && p == Page.LORE;
        g.fill(lx, top + LINE, lx + TEXT_W - (illustrated ? 36 : 0), top + LINE + 1, PAPER_EDGE);
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
        if (!stack.isEmpty() && p == Page.USE) facts(g, stack, rx, BOOK_Y + BOOK_H - BOTTOM - footerHeight());
        g.centeredText(font, (page + 1) + " / 3", BOOK_X + BOOK_W * 3 / 4, BOOK_Y + BOOK_H - 18, HEADING);
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
        g.fill(x, y - 3, x + TEXT_W, y - 2, PAPER_EDGE);
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
        int leftRows = (BOOK_H - BOTTOM - PAGE_MARGIN - firstLineOffset(stack)) / LINE;
        int rightRows = (BOOK_H - BOTTOM - PAGE_MARGIN - (!stack.isEmpty() && page == 1 ? footerHeight() + 4 : 0)) / LINE;
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
        int bx = leftPos + BOOK_X, ay = topPos + BOOK_Y + BOOK_H - 22;
        if (my >= ay && my < ay + 13) {
            if (mx >= bx + 8 && mx < bx + 31) {
                setPage(page - 1);
                return true;
            }
            if (mx >= bx + BOOK_W - 31 && mx < bx + BOOK_W - 8) {
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
