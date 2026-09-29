package com.simplebuilding.guide;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.mixin.ItemCraftRemainderAccessor;
import com.simplebuilding.version.McVersion;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Die Handbuecher: zwei Regale mit je einem Einstiegsbuch und seinen Themenbuechern.
 *
 * <ul>
 *   <li><b>Mod-Regal</b>: Einsteiger-Handbuch ({@link Book#GUIDE}) und zehn Themenbuecher zur Mod
 *       (Werkzeuge, Verzauberungen, Bauen, Lagerung, Maschinen, Ende, Pads, Geraete, Besaetze,
 *       Server-Admin).</li>
 *   <li><b>Vanilla-Regal</b> (2026-09-29): "Erste Schritte" ({@link Book#VANILLA_START}) und acht
 *       Themenbuecher zu Vanilla-Minecraft 26.3 (Oberwelt, Hoehlen, Ozean, Nether, Ende, Redstone,
 *       Ausruestung, Landwirtschaft &amp; Tiere).</li>
 * </ul>
 *
 * <p>Jedes Buch ist ein eigenes Item ({@link com.simplebuilding.items.custom.GuideBookItem}), das
 * seine Seiten als Standardkomponente {@code WRITTEN_BOOK_CONTENT} traegt: fertig aufgeloest
 * ({@code resolved = true}), ohne Titel und Autor (der Itemname bleibt der uebersetzte Name) und nur
 * aus Uebersetzungsschluesseln gebaut, damit der Client jede Seite in seiner eigenen Sprache zeigt.
 * Ein Rechtsklick oeffnet den eigenen Buchbildschirm ({@code client.guide.GuideBookScreen}, gleiche
 * Texte plus Symbole und Rezeptkarten aus {@link GuideContent}); die Vanilla-Seiten zeigt nur noch
 * das Lesepult (Tag {@code minecraft:lectern_books}).
 *
 * <p>Aufbau jedes Buchs: vorn ein anklickbares Inhaltsverzeichnis ({@link ClickEvent.ChangePage}),
 * danach je Kapitel genau eine Seite (Titel, Text, Ruecksprung). Ein Einstiegsbuch endet mit Seiten,
 * die die Themenbuecher seines Regals samt Rezept nennen ({@link #topicPages}). Schluessel:
 * {@code book.simplebuilding.<buch>.title}, {@code .intro} (nur Themenbuecher), {@code .<n>.title},
 * {@code .<n>.text}. Die Kapitelzahl steht nicht hier, sondern ergibt sich aus
 * {@link GuideContent} (eine Angabe je Kapitel). Jeder Kapiteltext bekommt drei Argumente: die Tasten
 * "Werkzeug-Einstellungen" ({@code %1$s}) und "Rucksack oeffnen" ({@code %2$s}) sowie den Namen des
 * Kapitelsymbols ({@code %3$s}) - so bleibt ein Text richtig, wenn ein Item umbenannt wird.
 *
 * <p>Textregeln: ein Gedanke je Kapitel, kurze Absaetze ({@code \n}), Aufzaehlungen mit
 * {@code "- "}. Rezepte stehen nicht im Text, sondern kommen als Rezeptkarten aus dem Rezeptmanager.
 *
 * <p>Seitenbudget: der Vanilla-Bildschirm zeichnet hoechstens 14 Zeilen zu 114 px und schneidet den
 * Rest stumm ab. Alle Seiten sind mit den Vanilla-Glyphbreiten auf hoechstens {@link #MAX_LINES}
 * Zeilen geprueft, auf Englisch und Deutsch: {@code python tools/guide_book_pages.py} nach jeder
 * Textaenderung; {@code GuideBookTests} prueft grob dasselbe im Spiel.
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
    /** Hoechstens so viele Zeilen je Vanilla-Seite (14 zeichnet der Bildschirm, eine Zeile Reserve). */
    public static final int MAX_LINES = 13;
    /** So viele Themenbuecher nennt die erste Themenseite eines Einstiegsbuchs, der Rest steht auf weiteren. */
    private static final int TOPICS_ON_FIRST_PAGE = 2;
    /** So viele Themenbuecher nennt jede weitere Themenseite. */
    private static final int TOPICS_PER_PAGE = 4;
    /** Kapitel-Links auf Seite 1 eines Themenbuchs (unter Titel und Einleitung) bzw. eines Einstiegsbuchs (ohne Einleitung). */
    public static final int CONTENTS_FIRST_TOPIC = 6, CONTENTS_FIRST_GUIDE = 11;
    /** Kapitel-Links auf jeder weiteren Inhaltsseite. */
    public static final int CONTENTS_MORE = 11;

    /**
     * Befehle, die das Admin-Buch nennt (ohne Schraegstrich, nur die woertlichen Teile); jeder steht als
     * {@code /befehl} im Text und {@code GuideBookTests} prueft ihn gegen den Befehlsbaum des Servers.
     */
    public static final List<String> ADMIN_COMMANDS = List.of("simplebuilding config list", "simplebuilding config get",
            "simplebuilding config set", "simplebuilding config reset", "simplebuilding tweaks pads", "simplebuilding chunkloaders list",
            "simplebuilding tweaks worldspawn setspawn1", "killboats", "killcarts");
    /** Config-Optionen, die das Admin-Buch nennt; jede steht im Text und ist ein Pfad aus {@code ConfigOptions}. */
    public static final List<String> ADMIN_OPTIONS = List.of("tweaks.pads.enableFlypads", "tweaks.commands.killCommandRadius",
            "tweaks.commands.enableKillCartsCommand", "giveGuideBookOnFirstJoin", "worldGen.enableLootTableChanges",
            "worldGen.enableVillagerTrades", "worldGen.enableWanderingTrades", "trimBenefitBaseMultiplier",
            "airJumpCooldownTicks", "breakerPistonsLoseDurability", "tools.buildingWandHungerCost");

    private GuideBooks() {
    }

    /** Die zwei Regale; auf dem Buchbildschirm je eine Reihe Lesezeichen. */
    public enum Shelf {
        MOD, VANILLA;

        /** Das Einstiegsbuch des Regals: es nennt die Themenbuecher und ist ihr wiederverwendbarer Zutatenersatz. */
        public Book hub() {
            return this == MOD ? Book.GUIDE : Book.VANILLA_START;
        }

        public List<Book> books() {
            List<Book> out = new ArrayList<>();
            for (Book book : Book.values()) {
                if (book.shelf() == this) {
                    out.add(book);
                }
            }
            return List.copyOf(out);
        }

        /** Die Themenbuecher des Regals (ohne Einstiegsbuch). */
        public List<Book> topics() {
            List<Book> out = new ArrayList<>(books());
            out.remove(hub());
            return List.copyOf(out);
        }
    }

    /** Alle Buecher in Regal- und Lesezeichen-Reihenfolge. */
    public enum Book {
        GUIDE("guide", "guide_book", Shelf.MOD),
        TOOLS("tools", "guide_book_tools", Shelf.MOD),
        /** Jeder Mod-Zauber, eine Seite je Zauber und Werkzeuggruppe (Besitzer 2026-09-29). */
        ENCHANTMENTS("enchantments", "guide_book_enchantments", Shelf.MOD),
        BUILDING("building", "guide_book_building", Shelf.MOD),
        STORAGE("storage", "guide_book_storage", Shelf.MOD),
        MACHINES("machines", "guide_book_machines", Shelf.MOD),
        END("end", "guide_book_end", Shelf.MOD),
        /** Pads & Druckplatten. Die Item-ID bleibt {@code guide_book_tweaks} (frueher "Pads & Geraete"), damit Spielstaende sie behalten. */
        PADS("pads", "guide_book_tweaks", Shelf.MOD),
        /** Geraete: Attractor, Rotator, Erzdetektor, Echolot, Linse, Tachometer (2026-09-29 aus dem Pad-Buch geloest). */
        GADGETS("gadgets", "guide_book_gadgets", Shelf.MOD),
        TRIMS("trims", "guide_book_trims", Shelf.MOD),
        /** Befehle und Config-Schalter fuer Server-Betreiber; nur Operatoren koennen es herstellen ({@link #operatorOnly}). */
        ADMIN("admin", "guide_book_admin", Shelf.MOD),
        VANILLA_START("vanilla_start", "guide_book_vanilla_start", Shelf.VANILLA),
        VANILLA_OVERWORLD("vanilla_overworld", "guide_book_vanilla_overworld", Shelf.VANILLA),
        VANILLA_CAVES("vanilla_caves", "guide_book_vanilla_caves", Shelf.VANILLA),
        VANILLA_OCEAN("vanilla_ocean", "guide_book_vanilla_ocean", Shelf.VANILLA),
        VANILLA_NETHER("vanilla_nether", "guide_book_vanilla_nether", Shelf.VANILLA),
        VANILLA_END("vanilla_end", "guide_book_vanilla_end", Shelf.VANILLA),
        VANILLA_REDSTONE("vanilla_redstone", "guide_book_vanilla_redstone", Shelf.VANILLA),
        VANILLA_GEAR("vanilla_gear", "guide_book_vanilla_gear", Shelf.VANILLA),
        VANILLA_FARMING("vanilla_farming", "guide_book_vanilla_farming", Shelf.VANILLA);

        private final String id;
        private final String itemName;
        private final Shelf shelf;

        Book(String id, String itemName, Shelf shelf) {
            this.id = id;
            this.itemName = itemName;
            this.shelf = shelf;
        }

        public String id() {
            return id;
        }

        public Shelf shelf() {
            return shelf;
        }

        /** Kapitelseiten nach dem Inhaltsverzeichnis: so viele, wie {@link GuideContent} Kapitel nennt. */
        public int chapters() {
            return GuideContent.style(this).chapters().size();
        }

        public boolean isHub() {
            return shelf.hub() == this;
        }

        public boolean isTopic() {
            return !isHub();
        }

        /** Registry-Pfad des Items. */
        public String itemName() {
            return itemName;
        }

        public String key() {
            return "book.simplebuilding." + id;
        }

        /** Alle Themenbuecher beider Regale. */
        public static List<Book> topics() {
            List<Book> topics = new ArrayList<>();
            for (Book book : values()) {
                if (book.isTopic()) {
                    topics.add(book);
                }
            }
            return List.copyOf(topics);
        }
    }

    /** Das Item eines Buchs. */
    public static Item item(Book book) {
        return switch (book) {
            case GUIDE -> ModItems.GUIDE_BOOK;
            case TOOLS -> ModItems.GUIDE_BOOK_TOOLS;
            case ENCHANTMENTS -> ModItems.GUIDE_BOOK_ENCHANTMENTS;
            case BUILDING -> ModItems.GUIDE_BOOK_BUILDING;
            case STORAGE -> ModItems.GUIDE_BOOK_STORAGE;
            case MACHINES -> ModItems.GUIDE_BOOK_MACHINES;
            case END -> ModItems.GUIDE_BOOK_END;
            case PADS -> ModItems.GUIDE_BOOK_TWEAKS;
            case GADGETS -> ModItems.GUIDE_BOOK_GADGETS;
            case TRIMS -> ModItems.GUIDE_BOOK_TRIMS;
            case ADMIN -> ModItems.GUIDE_BOOK_ADMIN;
            case VANILLA_START -> ModItems.GUIDE_BOOK_VANILLA_START;
            case VANILLA_OVERWORLD -> ModItems.GUIDE_BOOK_VANILLA_OVERWORLD;
            case VANILLA_CAVES -> ModItems.GUIDE_BOOK_VANILLA_CAVES;
            case VANILLA_OCEAN -> ModItems.GUIDE_BOOK_VANILLA_OCEAN;
            case VANILLA_NETHER -> ModItems.GUIDE_BOOK_VANILLA_NETHER;
            case VANILLA_END -> ModItems.GUIDE_BOOK_VANILLA_END;
            case VANILLA_REDSTONE -> ModItems.GUIDE_BOOK_VANILLA_REDSTONE;
            case VANILLA_GEAR -> ModItems.GUIDE_BOOK_VANILLA_GEAR;
            case VANILLA_FARMING -> ModItems.GUIDE_BOOK_VANILLA_FARMING;
        };
    }

    /**
     * Die zweite Zutat eines Buchs (formlos: Buch oder Einstiegsbuch des Regals + dieses Item). Billig
     * und frueh zu haben, und das Item verraet das Thema. Die Einstiegsbuecher selbst: Buch + dieses Item.
     */
    public static ItemLike keyItem(Book book) {
        return switch (book) {
            case GUIDE -> Items.CRAFTING_TABLE;
            case TOOLS -> ModItems.STONE_CHISEL;
            case ENCHANTMENTS -> Items.LAPIS_LAZULI;
            case BUILDING -> Items.BRICK;
            case STORAGE -> Items.CHEST;
            case MACHINES -> Items.PISTON;
            case END -> Items.ENDER_PEARL;
            case PADS -> Items.STONE_PRESSURE_PLATE;
            case GADGETS -> Items.COPPER_INGOT;
            case TRIMS -> Items.AMETHYST_SHARD;
            case ADMIN -> Items.COMPARATOR;
            case VANILLA_START -> Items.WOODEN_PICKAXE;
            case VANILLA_OVERWORLD -> Items.OAK_SAPLING;
            case VANILLA_CAVES -> Items.TORCH;
            case VANILLA_OCEAN -> Items.OAK_BOAT;
            case VANILLA_NETHER -> Items.FLINT_AND_STEEL;
            case VANILLA_END -> Items.ENDER_EYE;
            case VANILLA_REDSTONE -> Items.REDSTONE;
            case VANILLA_GEAR -> Items.STONE_SWORD;
            case VANILLA_FARMING -> Items.WHEAT_SEEDS;
        };
    }

    /** Eigenschaften eines Buch-Items: bis 16 stapelbar wie ein beschriebenes Buch, Seiten als Standardkomponente. */
    public static Item.Properties properties(Item.Properties settings, Book book) {
        return settings.stacksTo(16)
                .component(DataComponents.WRITTEN_BOOK_CONTENT, content(book))
                // Vanillas Buchzeilen ("Original", "von ...") gehoeren zu keinem dieser Buecher.
                .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.WRITTEN_BOOK_CONTENT, true));
    }

    /** Macht ein Einstiegsbuch zu seinem eigenen Handwerksrest (nach der Registrierung aufzurufen). */
    public static void makeSelfRemainder(Item hub) {
        ((ItemCraftRemainderAccessor) hub).simplebuilding$setCraftingRemainingItem(new ItemStackTemplate(hub));
    }

    // =====================================================================================
    // Nur fuer Operatoren: das Admin-Buch
    // =====================================================================================

    /** Buecher, die nur Operatoren (Berechtigungsstufe 2 und hoeher) herstellen koennen. */
    public static boolean operatorOnly(Book book) {
        return book == Book.ADMIN;
    }

    /** Ob dieses Item ein Buch ist, das nur Operatoren herstellen duerfen. */
    public static boolean isOperatorOnly(ItemStack stack) {
        for (Book book : Book.values()) {
            if (operatorOnly(book) && stack.is(item(book))) {
                return true;
            }
        }
        return false;
    }

    /** Das Rezept eines nur-Operator-Buchs erkennt man an seiner ID ({@code simplebuilding:guide_book_admin}). */
    public static boolean isOperatorOnlyRecipe(Identifier recipe) {
        if (!Simplebuilding.MOD_ID.equals(recipe.getNamespace())) {
            return false;
        }
        for (Book book : Book.values()) {
            if (operatorOnly(book) && recipe.getPath().equals(book.itemName())) {
                return true;
            }
        }
        return false;
    }

    /** Operator im Sinne der Buecher: Berechtigungsstufe mindestens 2 ("Spielleiter", wie /gamemode). */
    public static boolean isOperator(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Frisch aus der Operatorliste (wie bei Befehlen), nicht der beim Einloggen gemerkte Stand.
            return Commands.LEVEL_GAMEMASTERS.check(serverPlayer.createCommandSourceStack().permissions());
        }
        return player != null && Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    /** Ob dieser Spieler mit diesem Rezept etwas herstellen darf (Werkbank, Inventar-Raster). */
    public static boolean mayCraft(Player player, RecipeHolder<?> recipe) {
        return !isOperatorOnlyRecipe(recipe.id().identifier()) || isOperator(player);
    }

    /**
     * Rezepte, die ein Spieler freigeschaltet bekommen darf: ohne Operatorrechte fallen die
     * nur-Operator-Rezepte weg (Rezeptbuch, {@code /recipe give}, Freischalt-Advancements).
     */
    public static Collection<RecipeHolder<?>> filterUnlocks(Player player, Collection<RecipeHolder<?>> recipes) {
        if (isOperator(player) || recipes.stream().noneMatch(h -> isOperatorOnlyRecipe(h.id().identifier()))) {
            return recipes;
        }
        List<RecipeHolder<?>> allowed = new ArrayList<>(recipes.size());
        for (RecipeHolder<?> holder : recipes) {
            if (!isOperatorOnlyRecipe(holder.id().identifier())) {
                allowed.add(holder);
            }
        }
        return allowed;
    }

    /**
     * Beim Betreten das Rezeptbuch an den aktuellen Rang anpassen: Operatoren bekommen die
     * nur-Operator-Rezepte, allen anderen werden sie wieder genommen (z. B. nach {@code /deop}).
     */
    public static void syncOperatorRecipes(ServerPlayer player) {
        var manager = player.level().getServer().getRecipeManager();
        List<RecipeHolder<?>> operatorRecipes = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (isOperatorOnlyRecipe(holder.id().identifier())) {
                operatorRecipes.add(holder);
            }
        }
        if (operatorRecipes.isEmpty()) {
            return;
        }
        if (isOperator(player)) {
            player.awardRecipes(operatorRecipes);
        } else {
            player.resetRecipes(operatorRecipes);
        }
    }

    // =====================================================================================
    // Seiten
    // =====================================================================================

    /** Anklickbare Eintraege im Inhalt: die Kapitel, beim Einstiegsbuch zusaetzlich die Themenbuecher. */
    public static int contentsLinks(Book book) {
        return book.chapters() + (book.isHub() ? 1 : 0);
    }

    /** Wie viele Kapitel-Links Inhaltsseite {@code page} (ab 0) traegt. */
    public static int contentsLinksOn(Book book, int page) {
        int first = book.isTopic() ? CONTENTS_FIRST_TOPIC : CONTENTS_FIRST_GUIDE;
        int links = contentsLinks(book);
        if (page == 0) {
            return Math.min(links, first);
        }
        return Math.max(0, Math.min(CONTENTS_MORE, links - first - (page - 1) * CONTENTS_MORE));
    }

    /** Seiten des Inhaltsverzeichnisses (mindestens eine). */
    public static int contentsPages(Book book) {
        int pages = 1;
        while (contentsLinksOn(book, pages) > 0) {
            pages++;
        }
        return pages;
    }

    /** Seiten eines Einstiegsbuchs, die die Themenbuecher seines Regals samt Rezept nennen (0 fuer Themenbuecher). */
    public static int topicPages(Book book) {
        if (!book.isHub()) {
            return 0;
        }
        int rest = book.shelf().topics().size() - TOPICS_ON_FIRST_PAGE;
        return 1 + Math.max(0, (rest + TOPICS_PER_PAGE - 1) / TOPICS_PER_PAGE);
    }

    /** Buchseite (ab 1) von Eintrag {@code n} (ab 1) des Inhalts. */
    public static int chapterPage(Book book, int n) {
        return contentsPages(book) + n;
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
        int entry = 1;
        for (int page = 0; page < contentsPages(book); page++) {
            if (page > 0) {
                contents = Component.empty();
            }
            for (int j = 0; j < contentsLinksOn(book, page); j++, entry++) {
                if (j > 0) {
                    contents.append("\n");
                }
                String title = entry <= book.chapters() ? base + "." + entry + ".title" : TOPICS_KEY + ".title";
                contents.append(Component.translatable(title).withStyle(link(chapterPage(book, entry))));
            }
            pages.add(contents);
        }

        for (int i = 1; i <= book.chapters(); i++) {
            // Fetter Titel direkt ueber dem Text (keine Leerzeile: jede Zeile zaehlt im 13-Zeilen-Budget).
            pages.add(Component.empty()
                    .append(Component.translatable(base + "." + i + ".title").withStyle(ChatFormatting.BOLD))
                    .append("\n")
                    .append(chapterText(book, i))
                    .append("\n")
                    .append(back()));
        }

        if (book.isHub()) {
            List<Book> topics = book.shelf().topics();
            MutableComponent first = Component.empty()
                    .append(Component.translatable(TOPICS_KEY + ".title").withStyle(ChatFormatting.BOLD))
                    .append("\n\n")
                    .append(Component.translatable(TOPICS_KEY + ".text"))
                    .append("\n\n");
            appendTopics(first, topics.subList(0, Math.min(TOPICS_ON_FIRST_PAGE, topics.size())));
            pages.add(first.append("\n").append(back()));
            for (int from = TOPICS_ON_FIRST_PAGE; from < topics.size(); from += TOPICS_PER_PAGE) {
                MutableComponent more = Component.empty();
                appendTopics(more, topics.subList(from, Math.min(topics.size(), from + TOPICS_PER_PAGE)));
                pages.add(more.append("\n").append(back()));
            }
        }
        return pages;
    }

    /**
     * Text von Kapitel {@code n} (ab 1) mit seinen drei Argumenten (zwei Tasten, Name des
     * Kapitelsymbols); auch der Buchbildschirm nutzt ihn.
     */
    public static MutableComponent chapterText(Book book, int n) {
        Item icon = GuideContent.item(GuideContent.chapter(book, n - 1).icon());
        return Component.translatable(book.key() + "." + n + ".text", Component.keybind(SETTINGS_KEYBIND), Component.keybind(BACKPACK_KEYBIND),
                Component.translatable(icon.getDescriptionId()));
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

    /** Registry-ID des Items eines Buchs als Text ({@code simplebuilding:guide_book_tools}). */
    public static String itemId(Book book) {
        return BuiltInRegistries.ITEM.getKey(item(book)).toString();
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
     * Beim Betreten: einmal je Spieler das Einsteiger-Handbuch, und das Rezeptbuch passt sich dem
     * Operator-Rang an ({@link #syncOperatorRecipes}). Abgeschaltet wird nichts verschenkt und nichts
     * gemerkt; wird der Schalter spaeter eingeschaltet, bekommt es jeder Spieler einmal beim naechsten
     * Betreten. Volles Inventar: das Buch faellt vor die Fuesse.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        syncOperatorRecipes(player);
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
