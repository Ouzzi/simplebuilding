package com.simplebuilding.guide;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.mixin.ItemCraftRemainderAccessor;
import com.simplebuilding.version.McVersion;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Einsteiger-Handbuch und die Themenbuecher.
 *
 * <p>Jedes Buch ist ein eigenes Item ({@link com.simplebuilding.items.custom.GuideBookItem}), das
 * seine Seiten als Standardkomponente {@code WRITTEN_BOOK_CONTENT} traegt: fertig aufgeloest
 * ({@code resolved = true}, Vanillas {@code resolveForItem} greift also nie), ohne Titel und Autor
 * (der Itemname bleibt der uebersetzte Name) und nur aus Uebersetzungsschluesseln gebaut, damit der
 * Client jede Seite in seiner eigenen Sprache zeigt. Ein Rechtsklick oeffnet Vanillas
 * Buchbildschirm, ein Lesepult nimmt die Buecher auch (Tag {@code minecraft:lectern_books}).
 *
 * <p>Aufbau jedes Buchs: Seite 1 ist ein anklickbares Inhaltsverzeichnis
 * ({@link ClickEvent.ChangePage}), danach je Kapitel eine Seite mit Titel, Text und einem
 * Ruecksprung zum Inhalt. Das Handbuch endet mit zwei Seiten, die alle Themenbuecher samt Rezept
 * nennen (Buch oder Handbuch + {@link #keyItem}). Schluessel:
 * {@code book.simplebuilding.<buch>.title}, {@code .intro} (nur Themenbuecher),
 * {@code .<n>.title}, {@code .<n>.text}. Jeder Kapiteltext bekommt zwei Argumente: die Tasten
 * "Werkzeug-Einstellungen" ({@code %1$s}) und "Rucksack oeffnen" ({@code %2$s}) als
 * Tastenbelegungs-Komponente, also die Taste, die der Spieler wirklich belegt hat.
 *
 * <p>Seitenbudget: der Vanilla-Bildschirm zeichnet hoechstens 14 Zeilen zu 114 px und schneidet
 * den Rest stumm ab. Alle Seiten sind mit den Vanilla-Glyphbreiten auf hoechstens 13 Zeilen
 * geprueft, auf Englisch und Deutsch: {@code python tools/guide_book_pages.py} nach jeder Textaenderung.
 */
public final class GuideBooks {

    /**
     * Spieler-Tag: das Handbuch wurde verschenkt. Entity-Tags ueberleben Tod und Wiedereinstieg
     * ({@code PlayerList#respawn} kopiert sie), also genau einmal je Spieler.
     */
    public static final String GIVEN_TAG = "simplebuilding.guide_book_given";

    public static final String BACK_KEY = "book.simplebuilding.back";
    public static final String TOPICS_KEY = "book.simplebuilding.guide.topics";
    /** Tasten, die jeder Kapiteltext als {@code %1$s} und {@code %2$s} bekommt. */
    public static final String SETTINGS_KEYBIND = "key.simplebuilding.simple_settings";
    public static final String BACKPACK_KEYBIND = "key.simplebuilding.open_backpack";
    /** So viele Themenbuecher nennt die erste Themenseite des Handbuchs, der Rest steht auf der zweiten. */
    private static final int TOPICS_ON_FIRST_PAGE = 2;

    private GuideBooks() {
    }

    /** Die acht Buecher; {@code chapters} = Kapitelseiten nach dem Inhaltsverzeichnis. */
    public enum Book {
        GUIDE("guide", 10),
        TOOLS("tools", 6),
        BUILDING("building", 6),
        STORAGE("storage", 4),
        MACHINES("machines", 5),
        END("end", 4),
        TWEAKS("tweaks", 5),
        TRIMS("trims", 5);

        private final String id;
        private final int chapters;

        Book(String id, int chapters) {
            this.id = id;
            this.chapters = chapters;
        }

        public String id() {
            return id;
        }

        public int chapters() {
            return chapters;
        }

        public boolean isTopic() {
            return this != GUIDE;
        }

        /** Registry-Pfad des Items: {@code guide_book} bzw. {@code guide_book_<thema>}. */
        public String itemName() {
            return isTopic() ? "guide_book_" + id : "guide_book";
        }

        public String key() {
            return "book.simplebuilding." + id;
        }

        public static List<Book> topics() {
            List<Book> topics = new ArrayList<>(List.of(values()));
            topics.remove(GUIDE);
            return List.copyOf(topics);
        }
    }

    /** Das Item eines Buchs. */
    public static Item item(Book book) {
        return switch (book) {
            case GUIDE -> ModItems.GUIDE_BOOK;
            case TOOLS -> ModItems.GUIDE_BOOK_TOOLS;
            case BUILDING -> ModItems.GUIDE_BOOK_BUILDING;
            case STORAGE -> ModItems.GUIDE_BOOK_STORAGE;
            case MACHINES -> ModItems.GUIDE_BOOK_MACHINES;
            case END -> ModItems.GUIDE_BOOK_END;
            case TWEAKS -> ModItems.GUIDE_BOOK_TWEAKS;
            case TRIMS -> ModItems.GUIDE_BOOK_TRIMS;
        };
    }

    /**
     * Die zweite Zutat eines Themenbuchs (formlos: Buch oder Handbuch + dieses Item). Billig und
     * frueh zu haben, und das Item verraet das Thema. Das Handbuch selbst: Buch + Werkbank.
     */
    public static ItemLike keyItem(Book book) {
        return switch (book) {
            case GUIDE -> Items.CRAFTING_TABLE;
            case TOOLS -> ModItems.STONE_CHISEL;
            case BUILDING -> Items.BRICK;
            case STORAGE -> Items.CHEST;
            case MACHINES -> Items.PISTON;
            case END -> Items.ENDER_PEARL;
            case TWEAKS -> Items.STONE_PRESSURE_PLATE;
            case TRIMS -> Items.AMETHYST_SHARD;
        };
    }

    /** Eigenschaften eines Buch-Items: bis 16 stapelbar wie ein beschriebenes Buch, Seiten als Standardkomponente. */
    public static Item.Properties properties(Item.Properties settings, Book book) {
        return settings.stacksTo(16)
                .component(DataComponents.WRITTEN_BOOK_CONTENT, content(book))
                // Vanillas Buchzeilen ("Original", "von ...") gehoeren zu keinem dieser Buecher.
                .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.WRITTEN_BOOK_CONTENT, true));
    }

    /** Macht das Handbuch zu seinem eigenen Handwerksrest (nach der Registrierung aufzurufen). */
    public static void makeSelfRemainder(Item guide) {
        ((ItemCraftRemainderAccessor) guide).simplebuilding$setCraftingRemainingItem(new ItemStackTemplate(guide));
    }

    public static WrittenBookContent content(Book book) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (Component page : pages(book)) {
            pages.add(Filterable.passThrough(page));
        }
        return new WrittenBookContent(Filterable.passThrough(""), "", 0, pages, true);
    }

    /** Alle Seiten eines Buchs in Lesereihenfolge. */
    public static List<Component> pages(Book book) {
        List<Component> pages = new ArrayList<>();
        String base = book.key();

        MutableComponent contents = Component.empty()
                .append(Component.translatable(base + ".title").withStyle(ChatFormatting.BOLD))
                .append("\n\n");
        if (book.isTopic()) {
            contents.append(Component.translatable(base + ".intro")).append("\n\n");
        }
        int links = book.chapters() + (book == Book.GUIDE ? 1 : 0);
        for (int i = 1; i <= links; i++) {
            if (i > 1) {
                contents.append("\n");
            }
            String title = i <= book.chapters() ? base + "." + i + ".title" : TOPICS_KEY + ".title";
            // Seite 1 ist dieser Inhalt, Kapitel i steht auf Seite i + 1.
            contents.append(Component.translatable(title).withStyle(link(i + 1)));
        }
        pages.add(contents);

        for (int i = 1; i <= book.chapters(); i++) {
            pages.add(Component.empty()
                    .append(Component.translatable(base + "." + i + ".title").withStyle(ChatFormatting.BOLD))
                    .append("\n\n")
                    .append(Component.translatable(base + "." + i + ".text",
                            Component.keybind(SETTINGS_KEYBIND), Component.keybind(BACKPACK_KEYBIND)))
                    .append("\n")
                    .append(back()));
        }

        if (book == Book.GUIDE) {
            List<Book> topics = Book.topics();
            MutableComponent first = Component.empty()
                    .append(Component.translatable(TOPICS_KEY + ".title").withStyle(ChatFormatting.BOLD))
                    .append("\n\n")
                    .append(Component.translatable(TOPICS_KEY + ".text"))
                    .append("\n\n");
            appendTopics(first, topics.subList(0, TOPICS_ON_FIRST_PAGE));
            pages.add(first.append("\n").append(back()));
            MutableComponent second = Component.empty();
            appendTopics(second, topics.subList(TOPICS_ON_FIRST_PAGE, topics.size()));
            pages.add(second.append("\n").append(back()));
        }
        return pages;
    }

    private static void appendTopics(MutableComponent page, List<Book> topics) {
        for (int i = 0; i < topics.size(); i++) {
            Book topic = topics.get(i);
            if (i > 0) {
                page.append("\n");
            }
            page.append(Component.translatable(topic.key() + ".title").withStyle(ChatFormatting.DARK_GREEN))
                    .append("\n")
                    .append(Component.translatable(TOPICS_KEY + ".recipe",
                            Component.translatable(keyItem(topic).asItem().getDescriptionId())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Style link(int page) {
        return Style.EMPTY.withColor(ChatFormatting.DARK_BLUE).withUnderlined(true).withClickEvent(new ClickEvent.ChangePage(page));
    }

    private static Component back() {
        return Component.translatable(BACK_KEY).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withClickEvent(new ClickEvent.ChangePage(1)));
    }

    // =====================================================================================
    // Erstbeitritt
    // =====================================================================================

    /** Ob das Handbuch beim ersten Betreten verschenkt wird (Config {@code giveGuideBookOnFirstJoin}). */
    public static boolean giftEnabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.giveGuideBookOnFirstJoin;
    }

    /**
     * Beim Betreten: einmal je Spieler das Einsteiger-Handbuch. Abgeschaltet wird nichts verschenkt
     * und nichts gemerkt; wird der Schalter spaeter eingeschaltet, bekommt es jeder Spieler einmal
     * beim naechsten Betreten. Volles Inventar: das Buch faellt vor die Fuesse.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (player.entityTags().contains(GIVEN_TAG) || !giftEnabled()) {
            return;
        }
        ItemStack guide = new ItemStack(ModItems.GUIDE_BOOK);
        if (!player.getInventory().add(guide)) {
            McVersion.drop(player, guide, false, false);
        }
        player.addTag(GIVEN_TAG);
    }
}
