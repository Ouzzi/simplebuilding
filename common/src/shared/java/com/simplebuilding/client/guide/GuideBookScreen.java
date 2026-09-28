package com.simplebuilding.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.guide.GuideBooks;
import com.simplebuilding.guide.GuideContent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

/**
 * Der Buchbildschirm der Handbuecher, nach dem Vorbild von Eidolons Codex: eine aufgeschlagene
 * Doppelseite in einem Ledereinband, links ein Lesezeichen "Inhalt", rechts je Buch ein farbiges
 * Lesezeichen mit dem Schluesselitem (gesperrt, solange das Buch nicht im Inventar liegt).
 *
 * <p>Aufbau eines Buchs: Inhaltsseite(n) mit Titel, Unterzeile, (Themenbuch:) Einleitung und je
 * Kapitel einer anklickbaren Zeile mit Symbol; danach jedes Kapitel ab einer neuen Doppelseite:
 * links Kopf mit Symbol im Rahmen, Text (dieselben Schluessel wie die Vanilla-Seiten,
 * {@link GuideBooks}) und die Items des Kapitels, rechts die Rezeptkarten ({@link GuideContent}) -
 * Werkbank, formlos, Schmiede, Ofen und Steinsaege, aus dem Rezeptmanager des Einzelspieler-Servers
 * oder, auf einem Server, aus dem Rezeptbuch des Spielers (nur freigeschaltete Rezepte). Ohne Rezept
 * zeigt die rechte Seite das Kapitelsymbol gross im Bildrahmen. Was nicht passt, fliesst weiter.
 *
 * <p>Bedienung: Pfeile unten, Pfeiltasten/A/D/Bild auf-ab, Mausrad; Rechtsklick oder Ruecktaste
 * springt zurueck (nach einem Sprung dorthin, sonst zum Inhalt), Pos1 zum Inhalt. Die zuletzt
 * gelesene Doppelseite merkt sich jedes Buch bis zum Neustart des Spiels.
 */
public class GuideBookScreen extends Screen {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplebuilding", "textures/gui/guide_book/book.png");
    static final int TEX_W = 512, TEX_H = 256;
    static final int BOOK_W = 292, BOOK_H = 180;
    /** Inhaltsflaeche je Seite, relativ zum Buch. */
    static final int LEFT_X = 17, RIGHT_X = 159, CONTENT_Y = 15, CONTENT_W = 116, CONTENT_H = 142;
    static final int INK = 0xFF3B2A1C, INK_SOFT = 0xFF7A6248, INK_FAINT = 0xFFA08A6A;
    /** Neun Lesezeichen passen mit 18 px Abstand an die Buchkante (20 px hoch, 2 px ueberlappend). */
    private static final int TAB_Y = 12, TAB_STEP = 18;

    private static final Map<GuideBooks.Book, Integer> LAST_SPREAD = new EnumMap<>(GuideBooks.Book.class);

    private final GuideBooks.Book opened;
    private GuideBooks.Book book;
    private List<List<Placed>> pages = List.of();
    private int[] chapterPage = new int[0];
    private int spread;
    private final Deque<Integer> history = new ArrayDeque<>();
    private int bx, by;
    private ContextMap context;
    ItemStack hovered = ItemStack.EMPTY;
    Component hoveredText;

    public GuideBookScreen(GuideBooks.Book book) {
        super(Component.translatable(GuideBooks.item(book).getDescriptionId()));
        this.opened = book;
        this.book = book;
    }

    @Override
    protected void init() {
        bx = (width - BOOK_W) / 2;
        by = Math.max(0, (height - BOOK_H) / 2);
        context = minecraft.level != null ? SlotDisplayContext.fromLevel(minecraft.level) : null;
        open(book, LAST_SPREAD.getOrDefault(book, 0));
    }

    private void open(GuideBooks.Book target, int wantedSpread) {
        book = target;
        history.clear();
        layout();
        spread = clampSpread(wantedSpread);
        LAST_SPREAD.put(book, spread);
    }

    private int clampSpread(int s) {
        int max = Math.max(0, (pages.size() - 1) & ~1);
        return Math.max(0, Math.min(max, s & ~1));
    }

    private void goTo(int newSpread, boolean remember) {
        newSpread = clampSpread(newSpread);
        if (newSpread == spread) {
            return;
        }
        if (remember) {
            history.push(spread);
        }
        spread = newSpread;
        LAST_SPREAD.put(book, spread);
    }

    private void back() {
        goTo(history.isEmpty() ? 0 : history.pop(), false);
    }

    boolean available(GuideBooks.Book b) {
        return b == opened || b == book || (minecraft.player != null && minecraft.player.getInventory().countItem(GuideBooks.item(b)) > 0);
    }

    static int ink(GuideBooks.Book b) {
        int c = GuideContent.style(b).colour();
        int r = ((c >> 16) & 0xFF) * 55 / 100, g = ((c >> 8) & 0xFF) * 55 / 100, bl = (c & 0xFF) * 55 / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // =====================================================================================
    // SEITENAUFBAU
    // =====================================================================================

    /** Ein Element mit seiner Hoehe; x/y sind Bildschirmkoordinaten der linken oberen Ecke. */
    interface Element {
        int height();

        void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my);

        default boolean click(GuideBookScreen s, int x, int y, double mx, double my) {
            return false;
        }
    }

    record Placed(Element element, int y) {
    }

    private final class Pager {
        final List<List<Placed>> out = new ArrayList<>();
        List<Placed> current = new ArrayList<>();
        int y;

        Pager() {
            out.add(current);
        }

        void add(Element e) {
            if (y > 0 && y + e.height() > CONTENT_H) {
                newPage();
            }
            current.add(new Placed(e, y));
            y += e.height();
        }

        boolean fits(int h) {
            return y + h <= CONTENT_H;
        }

        void newPage() {
            current = new ArrayList<>();
            out.add(current);
            y = 0;
        }

        void breakPage() {
            if (!current.isEmpty()) {
                newPage();
            }
        }

        /** Weiter auf einer leeren linken Seite (Anfang einer Doppelseite). */
        void startSpread() {
            breakPage();
            if (page() % 2 == 1) {
                newPage();
            }
        }

        /** Steht der Inhalt noch links, geht es rechts weiter; rechts bleibt alles, wie es ist. */
        void toRightPage() {
            if (page() % 2 == 0) {
                newPage();
            }
        }

        boolean onLeftPage() {
            return page() % 2 == 0;
        }

        int page() {
            return out.size() - 1;
        }
    }

    /**
     * Seitenaufbau im Stil des Codex: jedes Kapitel beginnt auf einer linken Seite (Kopf, Text,
     * Items), die Rezeptkarten stehen rechts daneben; ein Kapitel ohne Rezept zeigt rechts sein Symbol
     * gross. Was nicht passt, fliesst auf die naechste Seite weiter.
     */
    private void layout() {
        int chapters = book.chapters() + (book == GuideBooks.Book.GUIDE ? 1 : 0);
        chapterPage = new int[chapters];
        Pager p = new Pager();

        // Inhalt
        List<FormattedCharSequence> tagline = font.split(Component.translatable(GuideContent.taglineKey(book)).withStyle(st -> st.withItalic(true)), CONTENT_W);
        List<FormattedCharSequence> title = font.split(Component.translatable(book.key() + ".title").withStyle(st -> st.withBold(true)), CONTENT_W);
        p.add(new TitleBlock(title.subList(0, Math.min(2, title.size())), tagline.subList(0, Math.min(2, tagline.size())), book));
        if (book.isTopic()) {
            for (FormattedCharSequence line : font.split(Component.translatable(book.key() + ".intro"), CONTENT_W)) {
                p.add(new TextLine(line, INK_SOFT));
            }
            p.add(new Gap(4));
        }
        p.add(new Label(Component.translatable(GuideContent.GUI + "chapters")));
        for (int i = 0; i < chapters; i++) {
            boolean topics = i >= book.chapters();
            Component name = Component.translatable(topics ? GuideBooks.TOPICS_KEY + ".title" : book.key() + "." + (i + 1) + ".title");
            ItemStack icon = topics ? new ItemStack(Items.BOOKSHELF) : stack(GuideContent.chapter(book, i).icon());
            p.add(new IndexEntry(icon, name, i));
        }

        // Kapitel
        for (int i = 0; i < book.chapters(); i++) {
            p.startSpread();
            chapterPage[i] = p.page();
            GuideContent.Chapter chapter = GuideContent.chapter(book, i);
            p.add(new Header(stack(chapter.icon()), Component.translatable(book.key() + "." + (i + 1) + ".title"), ink(book)));
            for (FormattedCharSequence line : font.split(GuideBooks.chapterText(book, i + 1), CONTENT_W)) {
                p.add(new TextLine(line, INK));
            }
            if (!chapter.items().isEmpty()) {
                p.add(new Gap(4));
                p.add(new Label(Component.translatable(GuideContent.GUI + "in_chapter")));
                List<ItemStack> row = new ArrayList<>();
                for (String id : chapter.items()) {
                    ItemStack s = stack(id);
                    if (!s.isEmpty()) {
                        row.add(s);
                    }
                    if (row.size() == ItemRow.PER_ROW) {
                        p.add(new ItemRow(row));
                        row = new ArrayList<>();
                    }
                }
                if (!row.isEmpty()) {
                    p.add(new ItemRow(row));
                }
            }
            if (!chapter.recipes().isEmpty()) {
                p.toRightPage();
                for (String spec : chapter.recipes()) {
                    p.add(new Gap(p.y == 0 ? 6 : 8));
                    p.add(card(spec, null));
                }
            } else if (p.onLeftPage()) {
                p.newPage();
                p.add(new Gap((CONTENT_H - Illustration.HEIGHT) / 2 - 8));
                p.add(new Illustration(stack(chapter.icon())));
            } else if (p.fits(Illustration.HEIGHT + 6)) {
                p.add(new Gap(6));
                p.add(new Illustration(stack(chapter.icon())));
            }
        }
        if (book == GuideBooks.Book.GUIDE) {
            p.startSpread();
            chapterPage[book.chapters()] = p.page();
            p.add(new Header(new ItemStack(Items.BOOKSHELF), Component.translatable(GuideBooks.TOPICS_KEY + ".title"), ink(book)));
            for (FormattedCharSequence line : font.split(Component.translatable(GuideBooks.TOPICS_KEY + ".text"), CONTENT_W)) {
                p.add(new TextLine(line, INK));
            }
            for (GuideBooks.Book topic : GuideBooks.Book.topics()) {
                p.add(new Gap(5));
                p.add(card(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(GuideBooks.item(topic)).toString(), topic));
            }
        }
        pages = p.out;
    }

    private Element card(String spec, GuideBooks.Book topic) {
        Optional<RecipeDisplay> display = findRecipe(spec);
        Component caption = topic != null
                ? Component.translatable(topic.key() + ".title").withStyle(s -> s.withColor(TextColor.fromRgb(ink(topic) & 0xFFFFFF)))
                : null;
        if (display.isEmpty()) {
            return new MissingRecipe(stack(spec), caption);
        }
        return new RecipeCard(display.get(), caption, context);
    }

    /**
     * Einzelspieler: der Rezeptmanager des eingebauten Servers (alle Rezepte, dieselbe Auswahl wie im
     * Spieltest). Auf einem Server kennt der Client nur die freigeschalteten Rezepte seines Rezeptbuchs.
     */
    private Optional<RecipeDisplay> findRecipe(String spec) {
        if (context == null) {
            return Optional.empty();
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            Collection<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes();
            return GuideContent.select(recipes, spec, context);
        }
        if (minecraft.player == null) {
            return Optional.empty();
        }
        Item item = GuideContent.item(spec);
        for (RecipeCollection collection : minecraft.player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                if (GuideContent.drawable(entry.display()) && GuideContent.shows(entry.display(), item, context)) {
                    return Optional.of(entry.display());
                }
            }
        }
        return Optional.empty();
    }

    static ItemStack stack(String spec) {
        Item item = GuideContent.item(spec);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // =====================================================================================
    // EINGABE
    // =====================================================================================

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_A || key == InputConstants.KEY_PAGEUP) {
            goTo(spread - 2, false);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_D || key == InputConstants.KEY_PAGEDOWN) {
            goTo(spread + 2, false);
            return true;
        }
        if (key == InputConstants.KEY_BACKSPACE) {
            back();
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            goTo(0, true);
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
        if (scrollY > 0) {
            goTo(spread - 2, false);
        } else if (scrollY < 0) {
            goTo(spread + 2, false);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            back();
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        if (overPrev(mx, my) && spread > 0) {
            goTo(spread - 2, false);
            return true;
        }
        if (overNext(mx, my) && spread + 2 < pages.size()) {
            goTo(spread + 2, false);
            return true;
        }
        if (overContentsTab(mx, my)) {
            goTo(0, true);
            return true;
        }
        GuideBooks.Book[] books = GuideBooks.Book.values();
        for (int i = 0; i < books.length; i++) {
            if (overTab(i, mx, my)) {
                if (books[i] != book && available(books[i])) {
                    open(books[i], LAST_SPREAD.getOrDefault(books[i], 0));
                }
                return true;
            }
        }
        for (int side = 0; side < 2; side++) {
            int index = spread + side;
            if (index >= pages.size()) {
                continue;
            }
            int x = bx + (side == 0 ? LEFT_X : RIGHT_X);
            for (Placed placed : pages.get(index)) {
                if (placed.element().click(this, x, by + CONTENT_Y + placed.y(), mx, my)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    void jumpToChapter(int chapter) {
        if (chapter >= 0 && chapter < chapterPage.length) {
            goTo(chapterPage[chapter], true);
        }
    }

    private boolean overPrev(double mx, double my) {
        return in(mx, my, bx + 14, by + 162, 18, 10);
    }

    private boolean overNext(double mx, double my) {
        return in(mx, my, bx + BOOK_W - 32, by + 162, 18, 10);
    }

    private boolean overContentsTab(double mx, double my) {
        return in(mx, my, bx - 22, by + TAB_Y, 24, 20);
    }

    private boolean overTab(int i, double mx, double my) {
        return in(mx, my, bx + BOOK_W - 2, by + TAB_Y + i * TAB_STEP, 26, 19);
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // =====================================================================================
    // ZEICHNEN
    // =====================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        hovered = ItemStack.EMPTY;
        hoveredText = null;

        // Lesezeichen liegen hinter dem Einband
        GuideBooks.Book[] books = GuideBooks.Book.values();
        for (int i = 0; i < books.length; i++) {
            GuideBooks.Book b = books[i];
            int ty = by + TAB_Y + i * TAB_STEP;
            boolean selected = b == book;
            boolean open = available(b);
            int u = selected ? 32 : open ? 0 : 64;
            int shift = selected ? 3 : 0;
            int tx = bx + BOOK_W - 8 + shift;
            blit(g, tx, ty, u, 184, 30, 20);
            // Farbstreifen des Buchs auf dem Lesezeichen
            if (open) {
                g.fill(tx + 8, ty + 2, tx + 26, ty + 4, 0xFF000000 | GuideContent.style(b).colour());
            }
            ItemStack icon = b == GuideBooks.Book.GUIDE ? new ItemStack(GuideBooks.item(b)) : new ItemStack(GuideBooks.keyItem(b));
            g.item(icon, tx + 10, ty + 3);
            if (!open) {
                g.fill(tx + 10, ty + 3, tx + 26, ty + 19, 0x88402A18);
            }
            if (overTab(i, mouseX, mouseY)) {
                hoveredText = open ? Component.translatable(b.key() + ".title")
                        : Component.translatable(GuideContent.GUI + "locked", Component.translatable(GuideBooks.keyItem(b).asItem().getDescriptionId()));
            }
        }
        blit(g, bx - 24, by + TAB_Y, 96, 184, 30, 20);
        if (overContentsTab(mouseX, mouseY)) {
            hoveredText = Component.translatable(GuideContent.GUI + "contents");
        }

        blit(g, bx, by, 0, 0, BOOK_W, BOOK_H);

        for (int side = 0; side < 2; side++) {
            int index = spread + side;
            if (index >= pages.size()) {
                continue;
            }
            int x = bx + (side == 0 ? LEFT_X : RIGHT_X);
            for (Placed placed : pages.get(index)) {
                placed.element().draw(this, g, x, by + CONTENT_Y + placed.y(), mouseX, mouseY);
            }
            String number = String.valueOf(index + 1);
            g.text(font, number, x + CONTENT_W / 2 - font.width(number) / 2, by + 163, INK_FAINT, false);
        }

        if (spread > 0) {
            boolean hover = overPrev(mouseX, mouseY);
            blit(g, bx + 14, by + 162, 148, hover ? 196 : 184, 18, 10);
            if (hover) {
                hoveredText = Component.translatable(GuideContent.GUI + "previous");
            }
        }
        if (spread + 2 < pages.size()) {
            boolean hover = overNext(mouseX, mouseY);
            blit(g, bx + BOOK_W - 32, by + 162, 128, hover ? 196 : 184, 18, 10);
            if (hover) {
                hoveredText = Component.translatable(GuideContent.GUI + "next");
            }
        }

        if (!hovered.isEmpty()) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        } else if (hoveredText != null) {
            g.setTooltipForNextFrame(font, hoveredText, mouseX, mouseY);
        }
    }

    static void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, w, h, TEX_W, TEX_H);
    }

    /** Ein Item in einem Rahmen (18x18), mit Tooltip beim Ueberfahren; Leerstapel nur der Rahmen. */
    void slot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mx, int my) {
        blit(g, x, y, 168, 184, 18, 18);
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
            g.itemDecorations(font, stack, x + 1, y + 1);
            if (in(mx, my, x + 1, y + 1, 16, 16)) {
                hovered = stack;
            }
        }
    }

    void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mx, int my) {
        if (!stack.isEmpty()) {
            g.item(stack, x, y);
            if (in(mx, my, x, y, 16, 16)) {
                hovered = stack;
            }
        }
    }

    /** Wechselnde Anzeige mehrerer Moeglichkeiten (Tags, Varianten) im Sekundentakt. */
    static ItemStack cycle(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stacks.get((int) ((Util.getMillis() / 1000L) % stacks.size()));
    }

    // =====================================================================================
    // ELEMENTE
    // =====================================================================================

    record Gap(int height) implements Element {
        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        }
    }

    record TextLine(FormattedCharSequence line, int colour) implements Element {
        @Override
        public int height() {
            return 10;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            g.text(s.font, line, x, y, colour, false);
        }
    }

    record Label(Component text) implements Element {
        @Override
        public int height() {
            return 11;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            g.text(s.font, text, x, y + 1, INK_SOFT, false);
            int w = s.font.width(text);
            g.fill(x + w + 3, y + 5, x + CONTENT_W, y + 6, 0x40503A28);
        }
    }

    /** Titel der Inhaltsseite: Buchname in Buchfarbe, Unterzeile kursiv (bis zwei Zeilen), Zierlinie. */
    record TitleBlock(List<FormattedCharSequence> title, List<FormattedCharSequence> tagline, GuideBooks.Book book) implements Element {
        @Override
        public int height() {
            return 19 + 10 * title.size() + 9 * tagline.size();
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            int cx = x + CONTENT_W / 2;
            int ty = y + 3;
            for (FormattedCharSequence line : title) {
                g.text(s.font, line, cx - s.font.width(line) / 2, ty, ink(book), false);
                ty += 10;
            }
            ty += 3;
            for (FormattedCharSequence line : tagline) {
                g.text(s.font, line, cx - s.font.width(line) / 2, ty, INK_SOFT, false);
                ty += 9;
            }
            blit(g, x + (CONTENT_W - 100) / 2, ty + 3, 240, 184, 100, 7);
        }
    }

    /** Kapitelkopf: Symbol im Rahmen, Titel (bis zwei Zeilen), Zierlinie. */
    record Header(ItemStack icon, Component title, int colour) implements Element {
        @Override
        public int height() {
            return 31;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            blit(g, x, y, 344, 184, 22, 22);
            s.item(g, icon, x + 3, y + 3, mx, my);
            int avail = CONTENT_W - 26;
            // Erst fett, dann fett mit Umbruch nach Bindestrichen ("Hammer-" / "Aufwertung"), dann
            // normal; passt ein Wort auch so nicht, wird die Zeile verkleinert statt zerschnitten.
            String plain = title.getString();
            String hyphen = plain.replace("-", "- ");
            Style style = title.getStyle();
            Component shown = null;
            for (Component candidate : List.of(
                    Component.literal(plain).withStyle(style.withBold(true)),
                    Component.literal(hyphen).withStyle(style.withBold(true)),
                    Component.literal(plain).withStyle(style),
                    Component.literal(hyphen).withStyle(style))) {
                if (widestWord(s, candidate) <= avail) {
                    shown = candidate;
                    break;
                }
            }
            float scale = 1f;
            if (shown == null) {
                shown = Component.literal(hyphen).withStyle(style);
                scale = Math.max(0.6f, avail / (float) widestWord(s, shown));
            }
            List<FormattedCharSequence> lines = s.font.split(shown, (int) (avail / scale));
            lines = lines.subList(0, Math.min(2, lines.size()));
            int ty = lines.size() > 1 ? y + 2 : y + 7;
            g.pose().pushMatrix();
            g.pose().translate(x + 26, ty);
            g.pose().scale(scale, scale);
            int ly = scale < 1f ? Math.round((1f - scale) * 4f / scale) : 0;
            for (FormattedCharSequence line : lines) {
                g.text(s.font, line, 0, ly, colour, false);
                ly += 10;
            }
            g.pose().popMatrix();
            blit(g, x + (CONTENT_W - 100) / 2, y + 23, 240, 184, 100, 7);
        }

        /** Breite des laengsten Worts im Stil des Titels (ein Wort bricht der Font sonst mitten durch). */
        private static int widestWord(GuideBookScreen s, Component styled) {
            int widest = 0;
            for (String word : styled.getString().split(" ")) {
                widest = Math.max(widest, s.font.width(Component.literal(word).withStyle(styled.getStyle())));
            }
            return widest;
        }
    }

    /** Zeile im Inhaltsverzeichnis: Symbol + Kapitelname, anklickbar. */
    record IndexEntry(ItemStack icon, Component title, int chapter) implements Element {
        @Override
        public int height() {
            return 18;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            boolean hover = in(mx, my, x, y, CONTENT_W, 17);
            if (hover) {
                g.fill(x - 2, y, x + CONTENT_W + 2, y + 17, 0x30A0703A);
            }
            g.item(icon, x, y);
            FormattedCharSequence name = s.font.split(title, CONTENT_W - 22).get(0);
            g.text(s.font, name, x + 20, y + 5, hover ? ink(s.book) : INK, false);
        }

        @Override
        public boolean click(GuideBookScreen s, int x, int y, double mx, double my) {
            if (in(mx, my, x, y, CONTENT_W, 17)) {
                s.jumpToChapter(chapter);
                return true;
            }
            return false;
        }
    }

    record ItemRow(List<ItemStack> stacks) implements Element {
        static final int PER_ROW = 6;

        @Override
        public int height() {
            return 19;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            for (int i = 0; i < stacks.size(); i++) {
                s.slot(g, stacks.get(i), x + i * 19, y, mx, my);
            }
        }
    }

    /** Das Kapitelsymbol doppelt gross in einem Zierrahmen - fuellt Seiten ohne Rezept. */
    record Illustration(ItemStack stack) implements Element {
        static final int HEIGHT = 70;

        @Override
        public int height() {
            return HEIGHT;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            int fx = x + (CONTENT_W - 56) / 2;
            blit(g, fx, y, 368, 184, 56, 56);
            if (!stack.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(fx + 12, y + 12);
                g.pose().scale(2f, 2f);
                g.item(stack, 0, 0);
                g.pose().popMatrix();
                if (in(mx, my, fx + 12, y + 12, 32, 32)) {
                    s.hovered = stack;
                }
                FormattedCharSequence name = s.font.split(stack.getHoverName(), CONTENT_W).get(0);
                g.text(s.font, name, x + CONTENT_W / 2 - s.font.width(name) / 2, y + 60, INK_SOFT, false);
            }
        }
    }

    /** Platzhalter, wenn der Client das Rezept nicht kennt (Server: noch nicht freigeschaltet). */
    record MissingRecipe(ItemStack stack, Component caption) implements Element {
        @Override
        public int height() {
            return caption != null ? 32 : 22;
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            if (caption != null) {
                g.text(s.font, caption, x, y, INK, false);
                y += 10;
            }
            s.slot(g, stack, x, y, mx, my);
            List<FormattedCharSequence> lines = s.font.split(Component.translatable(GuideContent.GUI + "no_recipe"), CONTENT_W - 22);
            int ty = y + (lines.size() > 1 ? 0 : 5);
            for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
                g.text(s.font, line, x + 22, ty, INK_FAINT, false);
                ty += 9;
            }
        }
    }

    /** Rezeptkarte: Werkbank (geformt/formlos, 3x3), Schmiede, Ofen oder Steinsaege. */
    static final class RecipeCard implements Element {
        private final RecipeDisplay display;
        private final Component caption;
        private final List<List<ItemStack>> inputs = new ArrayList<>();
        private final ItemStack result;
        private final ItemStack station;
        private final int gridWidth;

        RecipeCard(RecipeDisplay display, Component caption, ContextMap context) {
            this.display = display;
            this.caption = caption;
            int w = 1;
            List<SlotDisplay> slots = List.of();
            if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                w = shaped.width();
                slots = shaped.ingredients();
            } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                w = shapeless.ingredients().size() <= 4 ? 2 : 3;
                slots = shapeless.ingredients();
            } else if (display instanceof SmithingRecipeDisplay smithing) {
                slots = List.of(smithing.template(), smithing.base(), smithing.addition());
            } else if (display instanceof FurnaceRecipeDisplay furnace) {
                slots = List.of(furnace.ingredient());
            } else if (display instanceof StonecutterRecipeDisplay cutter) {
                slots = List.of(cutter.input());
            }
            this.gridWidth = w;
            for (SlotDisplay slot : slots) {
                inputs.add(slot.resolveForStacks(context));
            }
            this.result = display.result().resolveForFirstStack(context);
            this.station = display.craftingStation().resolveForFirstStack(context);
        }

        /** 3x3-Raster: geformte Rezepte und formlose mit mehr als drei Zutaten; der Rest steht in einer Reihe. */
        private boolean crafting() {
            return display instanceof ShapedCraftingRecipeDisplay
                    || (display instanceof ShapelessCraftingRecipeDisplay && inputs.size() > 3);
        }

        @Override
        public int height() {
            return (caption != null ? 10 : 0) + (crafting() ? 58 : 30);
        }

        @Override
        public void draw(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            if (caption != null) {
                g.text(s.font, caption, x, y, INK, false);
                y += 10;
            }
            if (crafting()) {
                int gx = x + 2, gy = y + 2;
                for (int i = 0; i < 9; i++) {
                    int col = i % 3, row = i / 3;
                    int index = col < gridWidth ? row * gridWidth + col : -1;
                    ItemStack stack = index >= 0 && index < inputs.size() ? cycle(inputs.get(index)) : ItemStack.EMPTY;
                    s.slot(g, stack, gx + col * 18, gy + row * 18, mx, my);
                }
                blit(g, x + 60, y + 22, 216, 184, 22, 15);
                s.item(g, station, x + 63, y + 38, mx, my);
                result(s, g, x + 86, y + 16, mx, my);
                if (display instanceof ShapelessCraftingRecipeDisplay && in(mx, my, x + 60, y + 22, 22, 15)) {
                    s.hoveredText = Component.translatable(GuideContent.GUI + "shapeless");
                }
            } else if (display instanceof SmithingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay) {
                int n = display instanceof SmithingRecipeDisplay ? 3 : inputs.size();
                for (int i = 0; i < n; i++) {
                    s.slot(g, i < inputs.size() ? cycle(inputs.get(i)) : ItemStack.EMPTY, x + 2 + i * 18, y + 5, mx, my);
                }
                blit(g, x + 60, y + 7, 216, 184, 22, 15);
                if (display instanceof ShapelessCraftingRecipeDisplay) {
                    if (in(mx, my, x + 60, y + 7, 22, 15)) {
                        s.hoveredText = Component.translatable(GuideContent.GUI + "shapeless");
                    }
                }
                result(s, g, x + 86, y + 1, mx, my);
            } else {
                s.slot(g, inputs.isEmpty() ? ItemStack.EMPTY : cycle(inputs.get(0)), x + 2, y + 5, mx, my);
                if (display instanceof FurnaceRecipeDisplay) {
                    blit(g, x + 24, y + 7, 440, 184, 14, 14);
                }
                s.item(g, station, x + 40, y + 6, mx, my);
                blit(g, x + 60, y + 7, 216, 184, 22, 15);
                result(s, g, x + 86, y + 1, mx, my);
            }
        }

        private void result(GuideBookScreen s, GuiGraphicsExtractor g, int x, int y, int mx, int my) {
            blit(g, x, y, 188, 184, 26, 26);
            if (!result.isEmpty()) {
                g.item(result, x + 5, y + 5);
                g.itemDecorations(s.font, result, x + 5, y + 5);
                if (in(mx, my, x + 5, y + 5, 16, 16)) {
                    s.hovered = result;
                }
            }
        }
    }
}
