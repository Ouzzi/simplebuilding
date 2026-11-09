package com.simplelib.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.ModpackGuide;
import com.simplelib.api.client.ModpackGuideBar;
import com.simplelib.api.client.ModpackGuideScreens;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * SimpleLib's book screen of the Simple Modpack guide (b-guide01, owner 03.11.): the top bookmarks pick the section
 * (super-mod, {@link ModpackGuideBar}), the side bookmarks its chapters - eight on the right, six on the left under
 * "Contents", the rest through the contents pages. A locked chapter keeps a grey bookmark and names what opens it; its pages
 * are left out until then. Each chapter starts on a new spread; its pages are the mod's guide pages (title + text).
 *
 * <p>Same book sheet, sizes and controls as SimpleBuilding's guide (arrows, wheel, A/D, Home, right click back to the
 * contents), so switching to a section with its own screen does not move the book.
 */
public final class ModpackGuideScreen extends Screen implements GuideProbe.Target {
    static final int BOOK_W = ModpackGuideBar.BOOK_W, BOOK_H = ModpackGuideBar.BOOK_H;
    static final int LEFT_X = 17, RIGHT_X = 159, CONTENT_Y = 15, CONTENT_W = 116, CONTENT_H = 142, LINE = 10, ENTRY = 18;
    static final int TAB_Y = 12, TAB_STEP = 20, RIGHT_TABS = 8, LEFT_TABS = 6, SHELF_TAB_Y = TAB_Y + 24;
    /** Ink on the parchment: text 11:1, soft (locked entries) 4.5:1, faint only for page numbers. */
    static final int INK = 0xFF3B2A1C, INK_SOFT = 0xFF7A6248, INK_FAINT = 0xFFA08A6A;

    private final @Nullable String sectionId;
    private final @Nullable InteractionHand hand;
    /**
     * Chapter to open at (b-guidechap14), then null. b-guide15: kept until the chapter is open - the server's unlock answer
     * comes after the first {@link #init} - or the reader turned a page, or {@link #START_WAIT} ticks passed (locked: contents).
     */
    private @Nullable Identifier startChapter;
    private int startWait;
    static final int START_WAIT = 60;
    private ModpackGuide.@Nullable Section section;
    private List<ModpackGuide.Section> sections = List.of();
    /** Side bookmarks: -1 = the section's own screen, otherwise a chapter index. */
    private final List<Integer> tabs = new ArrayList<>();
    private final List<Page> pages = new ArrayList<>();
    private int[] chapterStart = new int[0];
    private int contentsPages;
    private int spread;
    private int seenVersion = -1;
    private int bx, by;
    private @Nullable List<Component> tooltip;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    /** Where the last click arrived (GUI pixels) and what it hit, for the real-mouse probe. */
    private double[] lastClick = {-1, -1};
    private String lastHit = "none";

    /** One row of a page: optional icon, text lines, a chapter to jump to (-1 none). */
    record Row(int y, ItemStack icon, List<FormattedCharSequence> lines, int colour, int chapter) {
    }

    record Page(List<Row> rows) {
    }

    public ModpackGuideScreen(@Nullable String section, @Nullable InteractionHand hand) {
        this(section, hand, null);
    }

    /** b-guidechap14: {@code chapter} = open at the first page of that chapter (once; locked or unknown: the contents). */
    public ModpackGuideScreen(@Nullable String section, @Nullable InteractionHand hand, @Nullable Identifier chapter) {
        super(Component.translatable("item.simplelib.modpack_guide"));
        this.sectionId = section;
        this.hand = hand;
        this.startChapter = chapter;
    }

    @Override
    protected void init() {
        bx = ModpackGuideBar.bookX(width);
        by = ModpackGuideBar.bookY(height);
        sections = ModpackGuideScreens.sections();
        section = ModpackGuideScreens.section(sections, sectionId);
        if (section == null && sectionId == null && !sections.isEmpty()) section = sections.getFirst();
        ModpackGuideScreens.query(sections);
        layout();
        tryStart();
    }

    /** Jumps to {@link #startChapter} once it is open; drops it when it is not in this section. */
    private void tryStart() {
        if (startChapter == null) return;
        int index = -1;
        if (section != null) for (int i = 0; i < section.chapters().size(); i++) if (section.chapters().get(i).id().equals(startChapter)) index = i;
        if (index < 0) {
            startChapter = null;
        } else if (chapterStart[index] >= 0) {
            jump(index);
            startChapter = null;
        }
    }

    @Override
    public void removed() {
        ModpackGuideScreens.remember(spread < contentsPages || currentChapter() < 0 || section == null ? null
                : section.chapters().get(currentChapter()).id());
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (hand != null && minecraft.player != null) {
            ItemStack held = minecraft.player.getItemInHand(hand);
            if (!ModpackGuide.ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(held.getItem()))) {
                onClose();
                return;
            }
        }
        if (ModpackGuideScreens.version() != seenVersion) {
            int keep = spread;
            layout();
            spread = clamp(keep);
        }
        if (startChapter != null) {
            tryStart();
            // still locked after the server had time to answer: stay on the contents
            if (startChapter != null && ++startWait > START_WAIT) startChapter = null;
        }
    }

    private boolean ownScreen() {
        return section != null && section.screen() && ModpackGuideScreens.hasScreen(section.id());
    }

    // =====================================================================================
    // Layout
    // =====================================================================================

    private void layout() {
        seenVersion = ModpackGuideScreens.version();
        pages.clear();
        tabs.clear();
        List<ModpackGuide.Chapter> chapters = section == null ? List.of() : section.chapters();
        if (ownScreen()) tabs.add(-1);
        for (int i = 0; i < chapters.size(); i++) tabs.add(i);
        chapterStart = new int[chapters.size()];

        // Contents: title, then one entry per chapter (icon, name; locked ones softer), seven to a page.
        List<Row> rows = new ArrayList<>();
        int y = 0;
        Component title = section == null ? Component.translatable("item.simplelib.modpack_guide")
                : Component.translatable(section.name());
        for (FormattedCharSequence line : font.split(title.copy().withStyle(ChatFormatting.BOLD), CONTENT_W)) {
            rows.add(new Row(y, ItemStack.EMPTY, List.of(line), INK, -1));
            y += LINE;
        }
        List<FormattedCharSequence> tagline = font.split(Component.translatable("tooltip.simplelib.modpack_guide")
                .withStyle(ChatFormatting.ITALIC), CONTENT_W);
        rows.add(new Row(y, ItemStack.EMPTY, tagline, INK_SOFT, -1));
        y += LINE * tagline.size() + 6;
        if (chapters.isEmpty() && !ownScreen()) {
            rows.add(new Row(y, ItemStack.EMPTY, font.split(Component.translatable("gui.simplelib.modpack_guide.empty"), CONTENT_W), INK, -1));
        }
        for (int i = 0; i < chapters.size(); i++) {
            if (y + ENTRY > CONTENT_H) {
                pages.add(new Page(rows));
                rows = new ArrayList<>();
                y = 0;
            }
            ModpackGuide.Chapter chapter = chapters.get(i);
            boolean open = ModpackGuideScreens.open(chapter);
            String name = font.plainSubstrByWidth(Component.translatable(chapter.title()).getString(), CONTENT_W - 20);
            rows.add(new Row(y, ModpackGuideBar.icon(chapter.icon()), List.of(Component.literal(name).getVisualOrderText()),
                    open ? INK : INK_SOFT, i));
            y += ENTRY;
        }
        pages.add(new Page(rows));
        if (pages.size() % 2 == 1) pages.add(new Page(List.of()));
        contentsPages = pages.size();

        // Chapters: each open one from a new spread, one guide page per book page.
        for (int i = 0; i < chapters.size(); i++) {
            ModpackGuide.Chapter chapter = chapters.get(i);
            if (!ModpackGuideScreens.open(chapter) || chapter.pages() == 0) {
                chapterStart[i] = -1;
                continue;
            }
            chapterStart[i] = pages.size();
            for (int n = 1; n <= chapter.pages(); n++) pages.add(page(chapter, n));
            if (pages.size() % 2 == 1) pages.add(new Page(List.of()));
        }
    }

    private Page page(ModpackGuide.Chapter chapter, int number) {
        String key = chapter.pageKey() + "." + number;
        List<Row> rows = new ArrayList<>();
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.translatable(key + ".title").withStyle(ChatFormatting.BOLD), CONTENT_W));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.translatable(key), CONTENT_W));
        int max = CONTENT_H / LINE;
        rows.add(new Row(0, ItemStack.EMPTY, lines.subList(0, Math.min(max, lines.size())), INK, -1));
        return new Page(rows);
    }

    private int clamp(int s) {
        int max = Math.max(0, (pages.size() - 1) & ~1);
        return Math.max(0, Math.min(max, s & ~1));
    }

    /** The chapter whose pages the open spread shows; -1 on the contents. */
    private int currentChapter() {
        int best = -1;
        for (int i = 0; i < chapterStart.length; i++) if (chapterStart[i] >= 0 && chapterStart[i] <= spread) best = i;
        return best;
    }

    private void jump(int chapter) {
        if (chapter >= 0 && chapter < chapterStart.length && chapterStart[chapter] >= 0) spread = clamp(chapterStart[chapter]);
    }

    // =====================================================================================
    // Bookmarks
    // =====================================================================================

    static boolean leftTab(int i) {
        return i >= RIGHT_TABS;
    }

    static boolean shown(int i) {
        return i < RIGHT_TABS + LEFT_TABS;
    }

    static int tabY(int i) {
        return leftTab(i) ? SHELF_TAB_Y + (i - RIGHT_TABS) * TAB_STEP : TAB_Y + i * TAB_STEP;
    }

    private int[] tabRect(int i) {
        return leftTab(i) ? new int[]{bx - 22, by + tabY(i), 24, 19} : new int[]{bx + BOOK_W - 2, by + tabY(i), 26, 19};
    }

    private int[] contentsRect() {
        return new int[]{bx - 22, by + TAB_Y, 24, 20};
    }

    private int[] prevRect() {
        return new int[]{bx + 14, by + 162, 18, 10};
    }

    private int[] nextRect() {
        return new int[]{bx + BOOK_W - 32, by + 162, 18, 10};
    }

    private boolean tabOpen(int tab) {
        return tab < 0 || ModpackGuideScreens.open(section.chapters().get(tab));
    }

    private List<Component> tabTooltip(int tab) {
        if (tab < 0) return List.of(Component.translatable(section.name()));
        ModpackGuide.Chapter chapter = section.chapters().get(tab);
        Component name = Component.translatable(chapter.title());
        if (ModpackGuideScreens.open(chapter)) return List.of(name);
        Component what = chapter.hint() == null ? Component.translatable("gui.simplelib.modpack_guide.locked_short")
                : Component.translatable("gui.simplelib.modpack_guide.locked",
                Component.translatable(BuiltInRegistries.ITEM.getValue(chapter.hint()).getDescriptionId()));
        return List.of(name, what.copy().withStyle(ChatFormatting.GRAY));
    }

    // =====================================================================================
    // Input
    // =====================================================================================

    @Override
    public boolean keyPressed(KeyEvent event) {
        startChapter = null;
        int key = event.key();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A || key == InputConstants.KEY_PAGEUP) {
            spread = clamp(spread - 2);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D || key == InputConstants.KEY_PAGEDOWN) {
            spread = clamp(spread + 2);
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            spread = 0;
            return true;
        }
        if (minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        startChapter = null;
        if (scrollY != 0) spread = clamp(spread + (scrollY > 0 ? -2 : 2));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        startChapter = null;
        double mx = event.x(), my = event.y();
        lastClick = new double[]{mx, my};
        lastHit = "miss";
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            spread = 0;
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        if (ModpackGuideBar.click(mx, my, section == null ? null : section.id(), hand)) return true;
        if (ModpackGuideBar.in(mx, my, prevRect()) && spread > 0) {
            spread = clamp(spread - 2);
            return true;
        }
        if (ModpackGuideBar.in(mx, my, nextRect()) && spread + 2 < pages.size()) {
            lastHit = "next";
            spread = clamp(spread + 2);
            return true;
        }
        if (ModpackGuideBar.in(mx, my, contentsRect())) {
            lastHit = "contents";
            spread = 0;
            return true;
        }
        for (int i = 0; i < tabs.size() && shown(i); i++) {
            if (!ModpackGuideBar.in(mx, my, tabRect(i))) continue;
            int tab = tabs.get(i);
            lastHit = "tab " + tab;
            if (tab < 0) ModpackGuideScreens.open(section.id(), hand);
            else jump(tab); // a locked chapter has no pages: nothing happens, its tooltip says what opens it
            return true;
        }
        for (int side = 0; side < 2; side++) {
            int index = spread + side;
            if (index >= pages.size()) continue;
            int x = bx + (side == 0 ? LEFT_X : RIGHT_X);
            for (Row row : pages.get(index).rows()) {
                if (row.chapter() >= 0 && ModpackGuideBar.in(mx, my, new int[]{x, by + CONTENT_Y + row.y(), CONTENT_W, ENTRY - 1})) {
                    jump(row.chapter());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // =====================================================================================
    // Drawing
    // =====================================================================================

    static void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, ModpackGuideBar.TEXTURE, x, y, u, v, w, h, ModpackGuideBar.TEX_W, ModpackGuideBar.TEX_H);
    }

    /** Mirrored by swapped u (a negative scale flips the winding and gets culled since 26.x). */
    static void blitMirrored(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        float tw = ModpackGuideBar.TEX_W, th = ModpackGuideBar.TEX_H;
        g.blit(ModpackGuideBar.TEXTURE, x, y, x + w, y + h, (u + w) / tw, u / tw, v / th, (v + h) / th);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        tooltip = null;
        hoveredStack = ItemStack.EMPTY;
        Component bar = ModpackGuideBar.render(g, bx, by, section == null ? null : section.id(), mouseX, mouseY);
        if (bar != null) tooltip = List.of(bar);

        int current = currentChapter();
        for (int i = 0; i < tabs.size() && shown(i); i++) {
            int tab = tabs.get(i);
            boolean selected = tab >= 0 && tab == current && spread >= contentsPages;
            boolean open = tabOpen(tab);
            int u = selected ? 32 : open ? 0 : 64, shift = selected ? 3 : 0;
            int ty = by + tabY(i);
            int tx = leftTab(i) ? bx - 24 - shift : bx + BOOK_W - 8 + shift;
            if (leftTab(i)) blitMirrored(g, tx, ty, u, 184, 30, 20);
            else blit(g, tx, ty, u, 184, 30, 20);
            ItemStack icon = tab < 0 ? ModpackGuideBar.icon(section.icon()) : ModpackGuideBar.icon(section.chapters().get(tab).icon());
            g.item(icon, leftTab(i) ? tx + 4 : tx + 10, ty + 2);
            if (ModpackGuideBar.in(mouseX, mouseY, tabRect(i))) tooltip = tabTooltip(tab);
        }
        blit(g, bx - 24, by + TAB_Y, 96, 184, 30, 20);
        if (ModpackGuideBar.in(mouseX, mouseY, contentsRect())) tooltip = List.of(Component.translatable("gui.simplelib.modpack_guide.contents"));
        blit(g, bx, by, 0, 0, BOOK_W, BOOK_H);

        for (int side = 0; side < 2; side++) {
            int index = spread + side;
            if (index >= pages.size()) continue;
            int x = bx + (side == 0 ? LEFT_X : RIGHT_X);
            for (Row row : pages.get(index).rows()) {
                int y = by + CONTENT_Y + row.y();
                int textX = x;
                if (!row.icon().isEmpty()) {
                    g.item(row.icon(), x, y);
                    textX = x + 20;
                    boolean hover = ModpackGuideBar.in(mouseX, mouseY, new int[]{x, y, CONTENT_W, ENTRY - 1});
                    if (hover && row.chapter() >= 0) {
                        g.fill(x - 1, y + 16, x + CONTENT_W, y + 17, 0x40503A28);
                        tooltip = tabTooltip(row.chapter());
                    }
                }
                int ly = row.icon().isEmpty() ? y : y + 4;
                for (FormattedCharSequence line : row.lines()) {
                    g.text(font, line, textX, ly, row.colour(), false);
                    ly += LINE;
                }
            }
            String number = String.valueOf(index + 1);
            g.text(font, number, x + CONTENT_W / 2 - font.width(number) / 2, by + 163, INK_SOFT, false);
        }
        if (spread > 0) {
            boolean hover = ModpackGuideBar.in(mouseX, mouseY, prevRect());
            blit(g, bx + 14, by + 162, 148, hover ? 196 : 184, 18, 10);
            if (hover) tooltip = List.of(Component.translatable("gui.simplelib.modpack_guide.previous"));
        }
        if (spread + 2 < pages.size()) {
            boolean hover = ModpackGuideBar.in(mouseX, mouseY, nextRect());
            blit(g, bx + BOOK_W - 32, by + 162, 128, hover ? 196 : 184, 18, 10);
            if (hover) tooltip = List.of(Component.translatable("gui.simplelib.modpack_guide.next"));
        }
        if (tooltip != null) g.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    // =====================================================================================
    // Test probe (real mouse, GuideProbe)
    // =====================================================================================

    @Override
    public @Nullable String probeSection() {
        return section == null ? null : section.id();
    }

    @Override
    public int probeChapter() {
        return spread < contentsPages ? -1 : currentChapter();
    }

    @Override
    public int probeSpread() {
        return spread;
    }

    @Override
    public List<int[]> probeTabs() {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < tabs.size() && shown(i); i++) out.add(tabRect(i));
        return out;
    }

    @Override
    public List<Integer> probeTabChapters() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < tabs.size() && shown(i); i++) out.add(tabs.get(i));
        return out;
    }

    @Override
    public double[] probeLastClick() {
        return lastClick;
    }

    @Override
    public String probeLastHit() {
        return lastHit;
    }

    @Override
    public int[] probeNext() {
        return nextRect();
    }
}
