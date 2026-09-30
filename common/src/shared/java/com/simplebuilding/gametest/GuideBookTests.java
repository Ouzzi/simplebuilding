package com.simplebuilding.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.guide.GuideBooks;
import com.simplebuilding.guide.GuideContent;
import com.simplebuilding.items.ModItems;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

/**
 * Einsteiger-Handbuch und Themenbuecher ({@link GuideBooks}): Geschenk beim ersten Betreten, die
 * Rezepte an der Werkbank, der Inhalt jeder Seite und das Lesen.
 */
public final class GuideBookTests {

    private GuideBookTests() {
    }

    /**
     * Beim ersten Betreten gibt es das Handbuch genau einmal (Spieler-Tag); mit
     * {@code giveGuideBookOnFirstJoin} aus gibt es nichts, und es wird auch nichts gemerkt - der
     * naechste Beitritt mit eingeschaltetem Schalter bringt es dann doch. Der Standard ist an.
     *
     * <p>Was das bricht: der Tag fehlt (jedes Betreten ein neues Buch), der Schalter wird
     * uebergangen, oder der Beitritt ruft den Helfer nicht mehr (dann bleibt das Buch weg).
     */
    public static void theFirstJoinGivesTheGuideOnceAndHonoursTheConfig(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.MEGA_GUIDES) {
            var player = mockPlayer(helper);
            player.getInventory().clearContent();
            GuideBooks.onPlayerJoin(player);
            GuideBooks.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 0, "guides must be crafted");
            helper.assertTrue(!GuideBooks.giftEnabled(), "legacy config must not enable guide gifts");
            succeed(helper); return;
        }
        helper.assertTrue(new SimplebuildingConfig().giveGuideBookOnFirstJoin, "the guide gift is not on by default");
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null, "no config loaded, so the switch cannot be tested");
        boolean before = config.giveGuideBookOnFirstJoin;
        try {
            ServerPlayer player = mockPlayer(helper);
            // Der Mock ist schon ueber die PlayerList eingeloggt; von vorn anfangen.
            player.getInventory().clearContent();
            player.removeTag(GuideBooks.GIVEN_TAG);

            config.giveGuideBookOnFirstJoin = false;
            GuideBooks.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 0, "guides given with the switch off");
            helper.assertTrue(!player.entityTags().contains(GuideBooks.GIVEN_TAG), "the switch off still marked the player as served");

            config.giveGuideBookOnFirstJoin = true;
            GuideBooks.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 1, "guides given on the first join");
            helper.assertTrue(player.entityTags().contains(GuideBooks.GIVEN_TAG), "the first join did not mark the player");
            GuideBooks.onPlayerJoin(player);
            GuideBooks.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 1, "guides after joining three times");

            // Der gemeinsame Beitritts-Einstieg aller Loader ruft den Helfer.
            player.getInventory().clearContent();
            player.removeTag(GuideBooks.GIVEN_TAG);
            com.simplebuilding.tweaks.TweaksContent.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 1, "guides given through the loaders' join hook");
        } finally {
            config.giveGuideBookOnFirstJoin = before;
        }
        succeed(helper);
    }

    /**
     * Jedes Themenbuch entsteht formlos aus einem Buch oder dem Handbuch plus seinem
     * Schluesselitem, in beliebiger Reihenfolge; mit dem Handbuch bleibt das Handbuch im Raster
     * liegen (es ist sein eigener Handwerksrest), das Buch dagegen wird verbraucht. Das Handbuch
     * selbst: Buch + Werkbank. Ein Schluesselitem allein oder mit einem fremden Buch ergibt nichts.
     */
    public static void everyTopicBookRecipeTakesBookOrGuideAndTheGuideStays(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.MEGA_GUIDES) { megaGuideRecipes(helper); return; }
        List<String> problems = new ArrayList<>();
        for (GuideBooks.Shelf shelf : GuideBooks.Shelf.values()) {
            expect(helper, problems, List.of(new ItemStack(Items.BOOK), new ItemStack(GuideBooks.keyItem(shelf.hub()).asItem())),
                    GuideBooks.item(shelf.hub()), false);
        }
        for (GuideBooks.Book topic : GuideBooks.Book.topics()) {
            Item key = GuideBooks.keyItem(topic).asItem();
            Item result = GuideBooks.item(topic);
            Item hub = GuideBooks.item(topic.shelf().hub());
            expect(helper, problems, List.of(new ItemStack(Items.BOOK), new ItemStack(key)), result, false);
            expect(helper, problems, List.of(new ItemStack(key), new ItemStack(Items.BOOK)), result, false);
            expect(helper, problems, List.of(new ItemStack(hub), new ItemStack(key)), result, true);
            expect(helper, problems, List.of(new ItemStack(key), new ItemStack(hub)), result, true);
            if (find(helper, List.of(new ItemStack(key), new ItemStack(Items.WRITABLE_BOOK))).isPresent()) {
                problems.add("a book and quill + " + key + " crafts something");
            }
            if (find(helper, List.of(new ItemStack(key), new ItemStack(GuideBooks.item(GuideBooks.Book.TOOLS == topic
                    ? GuideBooks.Book.END : GuideBooks.Book.TOOLS)))).isPresent()) {
                problems.add("another topic book + " + key + " crafts something");
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " guide book recipe problems: " + problems);
        succeed(helper);
    }

    /**
     * Jedes Buch traegt seine Seiten fertig aufgeloest als Standardkomponente: Seite 1 ist ein
     * Inhaltsverzeichnis (ueber mehrere Seiten, wenn die Kapitel nicht auf eine passen) mit einem Sprung
     * je Kapitel (Handbuch: plus Themenbuecher), jede Kapitelseite springt zurueck auf Seite 1, kein
     * Sprung zeigt ins Leere. Jeder Mod-Schluessel
     * auf einer Seite steht in {@code en_us} und {@code de_de} (gleiche Platzhalterzahl), jeder
     * Vanilla-Schluessel (Namen der Schluesselitems) in Vanillas Sprachdatei; umgekehrt ist jeder
     * {@code book.simplebuilding.*}-Schluessel der Sprachdatei auf einer Seite in Gebrauch.
     */
    public static void everyGuidePageUsesTranslationKeysThatExistInEnglishAndGerman(GameTestHelper helper) {
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        List<String> problems = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (GuideBooks.Book book : GuideBooks.Book.values()) {
            ItemStack stack = new ItemStack(GuideBooks.item(book));
            WrittenBookContent content = com.simplebuilding.version.McVersion.MEGA_GUIDES ? GuideBooks.content(book) : stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content == null) {
                problems.add(book + " has no pages");
                continue;
            }
            if (!content.resolved()) {
                problems.add(book + " is not resolved, vanilla would rewrite it per reader");
            }
            List<Component> pages = content.getPages(false);
            int contentsPages = GuideBooks.contentsPages(book);
            int expectedPages = contentsPages + book.chapters() + GuideBooks.topicPages(book);
            if (pages.size() != expectedPages) {
                problems.add(book + " has " + pages.size() + " pages instead of " + expectedPages);
            }
            for (int p = 0; p < pages.size(); p++) {
                List<Integer> jumps = new ArrayList<>();
                Set<String> keys = new LinkedHashSet<>();
                walk(pages.get(p), keys, jumps);
                if (keys.isEmpty()) {
                    problems.add(book + " page " + (p + 1) + " has no translated text");
                }
                for (int jump : jumps) {
                    if (jump < 1 || jump > pages.size()) {
                        problems.add(book + " page " + (p + 1) + " jumps to page " + jump + " of " + pages.size());
                    }
                }
                int expectedJumps = p < contentsPages ? GuideBooks.contentsLinksOn(book, p) : 1;
                if (jumps.size() != expectedJumps) {
                    problems.add(book + " page " + (p + 1) + " has " + jumps.size() + " links instead of " + expectedJumps);
                }
                if (p >= contentsPages && !jumps.equals(List.of(1))) {
                    problems.add(book + " page " + (p + 1) + " does not lead back to the contents: " + jumps);
                }
                if (p < contentsPages) {
                    int before = 0;
                    for (int q = 0; q < p; q++) {
                        before += GuideBooks.contentsLinksOn(book, q);
                    }
                    for (int i = 0; i < jumps.size(); i++) {
                        int expected = GuideBooks.chapterPage(book, before + i + 1);
                        if (jumps.get(i) != expected) {
                            problems.add(book + " contents link " + (before + i + 1) + " jumps to page " + jumps.get(i) + " instead of " + expected);
                        }
                    }
                }
                for (String key : keys) {
                    used.add(key);
                    if (key.startsWith("item.minecraft.") || key.startsWith("block.minecraft.")) {
                        if (!Language.getInstance().has(key)) {
                            problems.add(key + " (" + book + " page " + (p + 1) + ") is not a vanilla key");
                        }
                        continue;
                    }
                    String english = en.has(key) ? en.get(key).getAsString() : null;
                    String german = de.has(key) ? de.get(key).getAsString() : null;
                    if (english == null || english.isBlank()) {
                        problems.add(key + " (" + book + " page " + (p + 1) + ") missing in en_us");
                    } else if (german == null || german.isBlank()) {
                        problems.add(key + " (" + book + " page " + (p + 1) + ") missing in de_de");
                    } else if (placeholders(english) != placeholders(german)) {
                        problems.add(key + " has " + placeholders(english) + " placeholders in en_us but " + placeholders(german) + " in de_de");
                    }
                }
            }
            String name = GuideBooks.item(book).getDescriptionId();
            if (!en.has(name) || !de.has(name)) {
                problems.add(name + " (item name) missing in en_us or de_de");
            }
        }
        for (String key : en.keySet()) {
            if (key.startsWith("book.simplebuilding.") && !used.contains(key)) {
                problems.add(key + " is in en_us but on no page");
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " guide page problems: " + problems);
        succeed(helper);
    }

    /**
     * Die Buecher lesen sich wie beschriebene Buecher: Benutzen gelingt und zaehlt als benutzt
     * (den Buchbildschirm oeffnet der Client selbst), sie passen aufs Lesepult und ins
     * gemeisselte Buecherregal, heissen wie ihr Item (kein leerer Buchtitel) und zeigen keine
     * Vanilla-Buchzeilen ("Original") im Tooltip.
     */
    public static void guideBooksReadLikeWrittenBooks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        List<String> problems = new ArrayList<>();
        for (GuideBooks.Book book : GuideBooks.Book.values()) {
            Item item = GuideBooks.item(book);
            ItemStack stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            int usedBefore = player.getStats().getValue(Stats.ITEM_USED.get(item));
            InteractionResult result = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                problems.add(book + ": use gave " + result);
            }
            if (player.getStats().getValue(Stats.ITEM_USED.get(item)) != usedBefore + 1) {
                problems.add(book + ": use did not count as used");
            }
            if (!stack.is(ItemTags.LECTERN_BOOKS) || !stack.is(ItemTags.BOOKSHELF_BOOKS)) {
                problems.add(book + " is missing from lectern_books or bookshelf_books");
            }
            TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
            if (display.shows(DataComponents.WRITTEN_BOOK_CONTENT)) {
                problems.add(book + " shows vanilla's book lines in its tooltip");
            }
            if (!(stack.getHoverName().getContents() instanceof TranslatableContents name) || !name.getKey().equals(item.getDescriptionId())) {
                problems.add(book + " is not called by its item name: " + stack.getHoverName());
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(problems.isEmpty(), problems.size() + " guide book problems: " + problems);
        succeed(helper);
    }

    /**
     * Der Buchbildschirm zeigt je Kapitel ein Symbol, Items und Rezeptkarten ({@link GuideContent}):
     * jedes Buch hat so viele Kapitel-Angaben wie Kapitel, jedes Symbol und Item existiert, jede
     * Rezeptangabe findet im Rezeptmanager ein zeichenbares Rezept mit genau diesem Ergebnis
     * (dieselbe Auswahl wie der Bildschirm im Einzelspieler), ebenso das Rezept jedes Themenbuchs
     * fuer die Themenseite des Handbuchs. Unterzeile, Mod-Zeile und alle Bildschirm-Texte stehen in
     * {@code en_us} und {@code de_de}; der Tooltip zeigt Name in Buchfarbe, Unterzeile, Mod-Name.
     */
    public static void everyGuideChapterIconAndRecipeResolves(GameTestHelper helper) {
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        List<String> problems = new ArrayList<>();
        java.util.Collection<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes = helper.getLevel().getServer().getRecipeManager().getRecipes();
        net.minecraft.util.context.ContextMap context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(helper.getLevel());
        List<String> keys = new ArrayList<>();
        keys.add(GuideContent.MOD_NAME_KEY);
        for (String key : GuideContent.GUI_KEYS) {
            keys.add(GuideContent.GUI + key);
        }
        for (GuideBooks.Book book : GuideBooks.Book.values()) {
            keys.add(GuideContent.taglineKey(book));
            GuideContent.BookStyle style = GuideContent.style(book);
            if (style == null) {
                problems.add(book + " has no screen content");
                continue;
            }
            if (style.chapters().size() != book.chapters()) {
                problems.add(book + " has " + style.chapters().size() + " screen chapters for " + book.chapters() + " chapters");
            }
            for (int i = 0; i < style.chapters().size(); i++) {
                GuideContent.Chapter chapter = style.chapters().get(i);
                String where = book + " chapter " + (i + 1);
                if (GuideContent.item(chapter.icon()) == Items.AIR) {
                    problems.add(where + ": icon " + chapter.icon() + " is no item");
                }
                for (String id : chapter.items()) {
                    if (GuideContent.item(id) == Items.AIR) {
                        problems.add(where + ": item " + id + " is no item");
                    }
                }
                for (String spec : chapter.recipes()) {
                    if (GuideContent.select(recipes, spec, context).isEmpty()) {
                        problems.add(where + ": no drawable recipe for " + spec);
                    }
                }
            }
            if (book.isTopic()) {
                String spec = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(GuideBooks.item(book)).toString();
                if (GuideContent.select(recipes, spec, context).isEmpty()) {
                    problems.add(book + ": the topic page finds no recipe for " + spec);
                }
            }

            ItemStack stack = new ItemStack(GuideBooks.item(book));
            net.minecraft.network.chat.TextColor colour = stack.getHoverName().getStyle().getColor();
            if (colour == null || colour.getValue() != GuideContent.style(com.simplebuilding.version.McVersion.MEGA_GUIDES ? book.shelf().hub() : book).colour()) {
                problems.add(book + ": the name is not in the book colour but " + colour);
            }
            List<Component> lines = new ArrayList<>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), TooltipDisplay.DEFAULT, lines::add,
                    net.minecraft.world.item.TooltipFlag.NORMAL);
            List<String> lineKeys = new ArrayList<>();
            for (Component line : lines) {
                lineKeys.add(line.getContents() instanceof TranslatableContents t ? t.getKey() : line.getString());
            }
            if (!lineKeys.equals((com.simplebuilding.version.McVersion.MEGA_GUIDES ? List.of(GuideContent.taglineKey(book.shelf().hub()), "tooltip.simplebuilding.guide_book.unlock", GuideContent.MOD_NAME_KEY) : List.of(GuideContent.taglineKey(book), GuideContent.MOD_NAME_KEY)))) {
                problems.add(book + ": tooltip lines are " + lineKeys);
            } else if (!lines.get(0).getStyle().isItalic()) {
                problems.add(book + ": the tagline is not italic");
            }
        }
        for (String key : keys) {
            String english = en.has(key) ? en.get(key).getAsString() : null;
            String german = de.has(key) ? de.get(key).getAsString() : null;
            if (english == null || english.isBlank() || german == null || german.isBlank()) {
                problems.add(key + " missing in en_us or de_de");
            } else if (placeholders(english) != placeholders(german)) {
                problems.add(key + " has different placeholders in en_us and de_de");
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " guide screen problems: " + problems);
        succeed(helper);
    }


    /**
     * Every enchantment of the mod is explained in the guides: its English name stands in the English
     * text of at least one chapter and its German name in the German text - Air Jump, Kinetic
     * Protection, Bridge, Cover and Range included. Every item the 2026-09-28 wave added to the books
     * (Velocity Gauge, Construction Light, the netherite and enderite apples and carrots, the Enderite
     * Spear, the Blaze Head, the seven quartz checkers) shows up on the screen of a chapter, as its
     * icon, an item or a recipe.
     */
    public static void theGuidesExplainEveryEnchantmentAndTheWaveItems(GameTestHelper helper) {
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        StringBuilder english = new StringBuilder();
        StringBuilder german = new StringBuilder();
        Set<Item> onScreen = new java.util.HashSet<>();
        for (GuideBooks.Book book : GuideBooks.Book.values()) {
            for (int i = 1; i <= book.chapters(); i++) {
                String key = book.key() + "." + i + ".text";
                english.append(en.has(key) ? en.get(key).getAsString() : "").append('\n');
                german.append(de.has(key) ? de.get(key).getAsString() : "").append('\n');
                GuideContent.Chapter chapter = GuideContent.chapter(book, i - 1);
                onScreen.add(GuideContent.item(chapter.icon()));
                chapter.items().forEach(id -> onScreen.add(GuideContent.item(id)));
                chapter.recipes().forEach(id -> onScreen.add(GuideContent.item(id)));
            }
        }
        List<String> problems = new ArrayList<>();
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        int mod = 0;
        for (var holder : enchantments.listElements().toList()) {
            net.minecraft.resources.Identifier id = holder.key().identifier();
            if (!id.getNamespace().equals(Simplebuilding.MOD_ID)) {
                continue;
            }
            mod++;
            String nameKey = "enchantment." + id.getNamespace() + "." + id.getPath();
            String englishName = en.has(nameKey) ? en.get(nameKey).getAsString() : null;
            String germanName = de.has(nameKey) ? de.get(nameKey).getAsString() : null;
            if (englishName == null || !english.toString().contains(englishName)) {
                problems.add(id + " (" + englishName + ") is named in no English guide chapter");
            }
            if (germanName == null || !german.toString().contains(germanName)) {
                problems.add(id + " (" + germanName + ") is named in no German guide chapter");
            }
        }
        helper.assertTrue(mod >= 19, "only " + mod + " mod enchantments are registered");
        for (Item item : List.of(ModItems.VELOCITY_GAUGE, ModItems.CONSTRUCTION_LIGHT, ModItems.NETHERITE_APPLE,
                ModItems.NETHERITE_CARROT, ModItems.ENDERITE_APPLE, ModItems.ENDERITE_CARROT, ModItems.ENDERITE_SPEAR,
                com.simplebuilding.tweaks.item.TweaksItems.BLAZE_HEAD, ModItems.PURPUR_QUARTZ_CHECKER, ModItems.LAPIS_QUARTZ_CHECKER,
                ModItems.BLACKSTONE_QUARTZ_CHECKER, ModItems.RESIN_QUARTZ_CHECKER, ModItems.ASTRALIT_QUARTZ_CHECKER,
                ModItems.NIHILITH_QUARTZ_CHECKER, ModItems.ENDER_QUARTZ_CHECKER, ModItems.GUIDE_BOOK_ADMIN)) {
            if (!onScreen.contains(item) && item != ModItems.GUIDE_BOOK_ADMIN) {
                problems.add(item + " is on no guide chapter's screen");
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " gaps in the guides: " + problems);
        succeed(helper);
    }

    /**
     * The Server Admin guide only names what exists: every command of {@link GuideBooks#ADMIN_COMMANDS}
     * is a path of literals in the server's command tree and stands as {@code /command} in the English
     * and the German text of the book; every option of {@link GuideBooks#ADMIN_OPTIONS} is a config
     * path ({@link com.simplebuilding.config.ConfigOptions#byPath}) and stands in both texts. The book
     * is crafted like the other topic books (Book + Redstone Comparator).
     *
     * <p>What breaks this test: a renamed or removed command or config option the book still names.
     */
    public static void theAdminGuideNamesOnlyCommandsAndOptionsThatExist(GameTestHelper helper) {
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        GuideBooks.Book admin = GuideBooks.Book.ADMIN;
        StringBuilder english = new StringBuilder();
        StringBuilder german = new StringBuilder();
        for (int i = 1; i <= admin.chapters(); i++) {
            String key = admin.key() + "." + i + ".text";
            english.append(en.get(key).getAsString()).append('\n');
            german.append(de.get(key).getAsString()).append('\n');
        }
        List<String> problems = new ArrayList<>();
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher =
                helper.getLevel().getServer().getCommands().getDispatcher();
        for (String command : GuideBooks.ADMIN_COMMANDS) {
            com.mojang.brigadier.tree.CommandNode<net.minecraft.commands.CommandSourceStack> node = dispatcher.getRoot();
            for (String literal : command.split(" ")) {
                node = node == null ? null : node.getChild(literal);
            }
            if (!(node instanceof com.mojang.brigadier.tree.LiteralCommandNode)) {
                problems.add("/" + command + " is no command of the server");
            }
            if (!english.toString().contains("/" + command) || !german.toString().contains("/" + command)) {
                problems.add("/" + command + " is not named in the English and German admin guide");
            }
        }
        for (String option : GuideBooks.ADMIN_OPTIONS) {
            if (com.simplebuilding.config.ConfigOptions.byPath(option) == null) {
                problems.add(option + " is no config option");
            }
            if (!english.toString().contains(option) || !german.toString().contains(option)) {
                problems.add(option + " is not named in the English and German admin guide");
            }
        }
        helper.assertTrue(GuideBooks.item(admin) == ModItems.GUIDE_BOOK_ADMIN && admin.isTopic() && GuideBooks.operatorOnly(admin),
                "the admin guide is not an operator-only topic book");
        if (!com.simplebuilding.version.McVersion.MEGA_GUIDES) expect(helper, problems, List.of(new ItemStack(ModItems.GUIDE_BOOK), new ItemStack(GuideBooks.keyItem(admin).asItem())),
                ModItems.GUIDE_BOOK_ADMIN, !com.simplebuilding.version.McVersion.MEGA_GUIDES);
        helper.assertTrue(problems.isEmpty(), problems.size() + " admin guide problems: " + problems);
        succeed(helper);
    }


    /**
     * Owner 2026-09-29: every page is properly formatted. Mirrors {@code tools/guide_book_pages.py} with
     * the vanilla ASCII glyph widths: every vanilla page (contents, chapters, topic pages) of every book
     * wraps to at most {@link GuideBooks#MAX_LINES} lines of 114 px in English and German, no ordinary
     * word is wider than a line (config paths and commands excepted), chapter titles take at most two
     * lines, and texts have no double spaces, empty paragraphs or other bullet signs than "- ".
     */
    public static void everyGuidePageFitsTheBookInEnglishAndGerman(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String locale : List.of("en_us", "de_de")) {
            JsonObject lang = langFile(helper, locale);
            java.util.function.Function<String, String> t = key -> lang.has(key) ? lang.get(key).getAsString() : key;
            for (GuideBooks.Book book : GuideBooks.Book.values()) {
                String base = book.key();
                for (int i = 1; i <= book.chapters(); i++) {
                    String title = t.apply(base + "." + i + ".title");
                    String icon = t.apply(nameKey(GuideContent.item(GuideContent.chapter(book, i - 1).icon())));
                    String text = t.apply(base + "." + i + ".text").replace("%1$s", "G").replace("%2$s", "B").replace("%3$s", icon);
                    String where = locale + " " + book.id() + " " + i;
                    int lines = PageMetrics.lines(title, true) + PageMetrics.lines(text, false) + 1;
                    if (lines > GuideBooks.MAX_LINES) {
                        problems.add(where + ": " + lines + " lines");
                    }
                    if (PageMetrics.lines(title, true) > 2) {
                        problems.add(where + ": title on more than two lines");
                    }
                    for (String word : PageMetrics.cutWords(title, true)) {
                        problems.add(where + ": title word cut: " + word);
                    }
                    for (String word : PageMetrics.cutWords(text, false)) {
                        problems.add(where + ": word cut: " + word);
                    }
                    if (text.contains("  ") || text.contains("\n\n") || text.contains(" \n") || !text.equals(text.strip())
                            || text.matches("(?s).*(^|\n)[*\u2022].*")) {
                        problems.add(where + ": stray spaces, empty paragraph or wrong bullet");
                    }
                }
                // Inhaltsseite 1
                StringBuilder contents = new StringBuilder();
                int links = GuideBooks.contentsLinksOn(book, 0);
                int lines = PageMetrics.lines(t.apply(base + ".title"), true) + 1;
                if (book.isTopic()) {
                    lines += PageMetrics.lines(t.apply(base + ".intro"), false) + 1;
                }
                for (int i = 1; i <= links; i++) {
                    lines += PageMetrics.lines(i <= book.chapters() ? t.apply(base + "." + i + ".title") : t.apply(GuideBooks.TOPICS_KEY + ".title"), false);
                }
                if (lines > GuideBooks.MAX_LINES) {
                    problems.add(locale + " " + book.id() + " contents: " + lines + " lines");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " formatting problems: " + problems);
        succeed(helper);
    }

    private static String nameKey(Item item) {
        return item.getDescriptionId();
    }

    /** Vanilla-Glyphbreiten (ASCII-Schrift, 26.3) fuer die Formatpruefung ohne Client. */
    static final class PageMetrics {
        static final int WIDTH = 114;

        static int width(char c, boolean bold) {
            int w;
            if (c == ' ') {
                return 4;
            } else if ("!',.:;i|".indexOf(c) >= 0) {
                w = 2;
            } else if ("`l".indexOf(c) >= 0) {
                w = 3;
            } else if ("\"()*I[]t{}".indexOf(c) >= 0) {
                w = 4;
            } else if ("<>fk".indexOf(c) >= 0) {
                w = 5;
            } else if ("@~".indexOf(c) >= 0) {
                w = 7;
            } else {
                w = 6;
            }
            return w + (bold ? 1 : 0);
        }

        static int wordWidth(String word, boolean bold) {
            int w = 0;
            for (char c : word.toCharArray()) {
                w += width(c, bold);
            }
            return w;
        }

        static int lines(String text, boolean bold) {
            int lines = 0;
            for (String paragraph : text.split("\n", -1)) {
                int x = 0;
                lines++;
                for (String word : paragraph.split(" ")) {
                    int w = wordWidth(word, bold);
                    if (x > 0 && x + 4 + w > WIDTH) {
                        lines++;
                        x = 0;
                    }
                    x += (x > 0 ? 4 : 0) + w;
                    while (x > WIDTH) {
                        lines++;
                        x -= WIDTH;
                    }
                }
            }
            return lines;
        }

        static List<String> cutWords(String text, boolean bold) {
            List<String> out = new ArrayList<>();
            for (String word : text.split("[ \n]")) {
                if (!word.matches(".*([a-z][A-Z]|[a-z]\\.[a-z]).*") && !word.startsWith("/") && wordWidth(word, bold) > WIDTH) {
                    out.add(word);
                }
            }
            return out;
        }
    }

    /**
     * Owner 2026-09-29: reading a guide does not pause the game, like the inventory. The book screen
     * returns {@link GuideContent#pausesGame} from {@code isPauseScreen}; the client class ships in the
     * jar and names the method (its constant pool holds "isPauseScreen" and the GuideContent field).
     */
    public static void readingTheGuideDoesNotPauseTheGame(GameTestHelper helper) {
        helper.assertTrue(!GuideContent.pausesGame(), "the guide screen pauses the game");
        String path = "com/simplebuilding/client/guide/GuideBookScreen.class";
        try (InputStream in = GuideBookTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not in the jar");
            String bytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            helper.assertTrue(bytes.contains("isPauseScreen") && bytes.contains("pausesGame"),
                    "GuideBookScreen does not override isPauseScreen with GuideContent.pausesGame");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        succeed(helper);
    }

    /**
     * Owner 2026-09-29: the Server Admin guide is crafted by operators only (permission level 2+).
     * A non-operator gets no result in the crafting grid and never gets the recipe unlocked; an
     * operator crafts it (the Beginner's Guide stays); a crafter never makes it.
     */
    public static void onlyOperatorsCraftTheAdminGuide(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.MEGA_GUIDES) {
            ServerPlayer player = mockPlayer(helper);
            var players = helper.getLevel().getServer().getPlayerList();
            boolean wasOp = players.isOp(player.nameAndId());
            var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            ItemStack guide = new ItemStack(ModItems.GUIDE_BOOK);
            ItemStack key = new ItemStack(GuideBooks.keyItem(GuideBooks.Book.ADMIN), 2);
            player.setItemInHand(hand, guide);
            player.getInventory().setItem(9, key);
            com.simplebuilding.guide.GuideUnlocks.open(player, hand);
            try {
                players.deop(player.nameAndId());
                helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, GuideBooks.Book.ADMIN.ordinal()), "non-operator unlock accepted");
                helper.assertValueEqual(key.getCount(), 2, "denied unlock consumed key");
                players.op(player.nameAndId(), Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
                helper.assertTrue(GuideBooks.isOperator(player), "op did not grant permission");
                helper.assertTrue(player.getItemInHand(hand) == guide, "op changed held stack");
                helper.assertTrue(player.containerMenu == player.inventoryMenu, "op left another menu open");
                helper.assertTrue(!GuideBooks.inserted(guide, GuideBooks.Book.ADMIN), "base book already has admin");
                helper.assertValueEqual(player.getInventory().countItem(GuideBooks.keyItem(GuideBooks.Book.ADMIN).asItem()), 2, "admin inventory fixture");
                helper.assertTrue(com.simplebuilding.guide.GuideUnlocks.unlock(player, GuideBooks.Book.ADMIN.ordinal()), "operator unlock refused");
                helper.assertValueEqual(key.getCount(), 1, "admin unlock cost");
                players.deop(player.nameAndId());
                helper.assertTrue(!GuideBooks.isOperator(player), "deop not respected");
                helper.assertTrue(GuideBooks.inserted(guide, GuideBooks.Book.ADMIN), "deop erased persistent chapter");
            } finally { if (wasOp) players.op(player.nameAndId()); else players.deop(player.nameAndId()); }
            succeed(helper);
            return;
        }
        ServerPlayer player = mockPlayer(helper);
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        List<String> problems = new ArrayList<>();
        List<ItemStack> grid = List.of(new ItemStack(ModItems.GUIDE_BOOK), new ItemStack(GuideBooks.keyItem(GuideBooks.Book.ADMIN).asItem()));
        Optional<RecipeHolder<CraftingRecipe>> recipe = find(helper, grid);
        try {
            helper.assertTrue(recipe.isPresent() && GuideBooks.isOperatorOnlyRecipe(recipe.get().id().identifier()),
                    "the admin guide recipe is missing or not marked operator-only");
            players.deop(player.nameAndId());
            if (GuideBooks.isOperator(player)) problems.add("a deopped player counts as operator");
            if (GuideBooks.mayCraft(player, recipe.get())) problems.add("a non-operator may craft the admin guide");
            player.awardRecipes(List.of(recipe.get()));
            if (player.getRecipeBook().contains(recipe.get().id())) problems.add("a non-operator got the admin recipe unlocked");
            // Stufe 1 (Moderator) reicht nicht, Stufe 2 (Spielleiter) schon - unabhaengig von op-permission-level des Testservers.
            players.op(player.nameAndId(), Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.MODERATOR), Optional.empty());
            if (GuideBooks.isOperator(player)) problems.add("a level 1 operator counts as operator");
            players.deop(player.nameAndId());
            players.op(player.nameAndId(), Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
            if (!GuideBooks.isOperator(player)) problems.add("a level 2 operator does not count as operator");
            if (!GuideBooks.mayCraft(player, recipe.get())) problems.add("an operator may not craft the admin guide");
            GuideBooks.syncOperatorRecipes(player);
            if (!player.getRecipeBook().contains(recipe.get().id())) problems.add("joining as operator does not unlock the admin recipe");
            players.deop(player.nameAndId());
            GuideBooks.syncOperatorRecipes(player);
            if (player.getRecipeBook().contains(recipe.get().id())) problems.add("joining after /deop keeps the admin recipe");
            if (net.minecraft.world.level.block.CrafterBlock.getPotentialResults(helper.getLevel(), CraftingInput.of(2, 1, grid)).isPresent()) {
                problems.add("a crafter makes the admin guide");
            }
            // Andere Buecher bleiben fuer alle offen.
            Optional<RecipeHolder<CraftingRecipe>> tools = find(helper, List.of(new ItemStack(com.simplebuilding.version.McVersion.MEGA_GUIDES ? ModItems.GUIDE_BOOK : Items.BOOK), new ItemStack(ModItems.STONE_CHISEL)));
            if (tools.isEmpty() || !GuideBooks.mayCraft(player, tools.get())) problems.add("non-operators cannot craft the tools guide");
        } finally {
            if (wasOp) players.op(player.nameAndId()); else players.deop(player.nameAndId());
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " operator problems: " + problems);
        succeed(helper);
    }

    /**
     * Owner 2026-09-29: the Enchantments guide explains every mod enchantment, one tool group per
     * page. Every page shows an enchanted book of exactly one mod enchantment (at its maximum level)
     * and at least one item that enchantment can go on; its text names the maximum level
     * ("Max level: V" / "Hoechststufe: V"); and every mod enchantment has at least one page.
     */
    public static void theEnchantmentsGuideCoversEveryModEnchantment(GameTestHelper helper) {
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        var registry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        GuideBooks.Book book = GuideBooks.Book.ENCHANTMENTS;
        Set<net.minecraft.resources.Identifier> covered = new java.util.HashSet<>();
        List<String> problems = new ArrayList<>();
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        for (int i = 0; i < book.chapters(); i++) {
            GuideContent.Chapter chapter = GuideContent.chapter(book, i);
            String where = "chapter " + (i + 1);
            List<net.minecraft.resources.Identifier> enchantments = new ArrayList<>();
            for (String spec : chapter.items()) {
                if (GuideContent.enchantment(spec) != null) enchantments.add(GuideContent.enchantment(spec));
            }
            if (enchantments.size() != 1) {
                problems.add(where + " shows " + enchantments.size() + " enchanted books");
                continue;
            }
            var holder = registry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchantments.get(0)));
            if (holder.isEmpty()) {
                problems.add(where + ": " + enchantments.get(0) + " is no enchantment");
                continue;
            }
            covered.add(enchantments.get(0));
            ItemStack shown = GuideContent.stack(chapter.items().get(0), helper.getLevel().registryAccess());
            var stored = shown.get(DataComponents.STORED_ENCHANTMENTS);
            int max = holder.get().value().getMaxLevel();
            if (stored == null || stored.getLevel(holder.get()) != max) problems.add(where + ": the book does not carry the enchantment at level " + max);
            boolean supported = false;
            for (String spec : chapter.items()) {
                if (GuideContent.enchantment(spec) == null && new ItemStack(GuideContent.item(spec)).is(holder.get().value().definition().supportedItems())) {
                    supported = true;
                }
            }
            if (!supported) problems.add(where + ": shows no item " + enchantments.get(0) + " can go on");
            String key = book.key() + "." + (i + 1) + ".text";
            if (!en.get(key).getAsString().contains("ax level: " + roman[max])) problems.add(where + ": English text does not say Max level: " + roman[max]);
            if (!de.get(key).getAsString().contains("chststufe: " + roman[max])) problems.add(where + ": German text does not say Höchststufe: " + roman[max]);
        }
        for (var holder : registry.listElements().toList()) {
            var id = holder.key().identifier();
            if (id.getNamespace().equals(Simplebuilding.MOD_ID) && !covered.contains(id)) problems.add(id + " has no page in the Enchantments guide");
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " enchantment guide problems: " + problems);
        succeed(helper);
    }

    // =====================================================================================

    private static void megaGuideRecipes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        ServerPlayer player = mockPlayer(helper);
        var main = net.minecraft.world.InteractionHand.MAIN_HAND;
        helper.assertValueEqual((int) net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof com.simplebuilding.items.custom.GuideBookItem).count(), 2, "registered guide items");
        for (GuideBooks.Shelf shelf : GuideBooks.Shelf.values()) {
            Item hub = GuideBooks.item(shelf.hub());
            var baseGrid = List.of(new ItemStack(Items.BOOK), new ItemStack(GuideBooks.keyItem(shelf.hub())));
            expect(helper, problems, baseGrid, hub, false);
            var baseRecipe = find(helper, baseGrid).orElseThrow();
            helper.assertTrue(baseRecipe.value().getClass() == net.minecraft.world.item.crafting.ShapelessRecipe.class, "base guide needs a normal auto-fill recipe");
            helper.assertTrue(!baseRecipe.value().display().isEmpty(), "base guide has no recipe-book/JEI display");
            helper.assertTrue(!baseRecipe.value().isSpecial(), "base guide hidden as special recipe");
            player.getInventory().clearContent();
            player.resetRecipes(List.of(baseRecipe));
            player.getInventory().setItem(9, new ItemStack(GuideBooks.keyItem(shelf.hub())));
            GuideBooks.onPlayerJoin(player);
            helper.assertTrue(player.getRecipeBook().contains(baseRecipe.id()), "base guide recipe cannot be unlocked");
            ItemStack book = new ItemStack(hub);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("My guide"));
            player.getInventory().clearContent();
            player.setItemInHand(main, book);
            var first = shelf.topics().getFirst();
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, first.ordinal()), "packet without reading session accepted");
            com.simplebuilding.guide.GuideUnlocks.open(player, main);
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, -1), "negative chapter accepted");
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, 1000), "invalid chapter accepted");
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, shelf.hub().ordinal()), "hub accepted");
            var other = (shelf == GuideBooks.Shelf.MOD ? GuideBooks.Shelf.VANILLA : GuideBooks.Shelf.MOD).topics().getFirst();
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, other.ordinal()), "cross-shelf chapter accepted");
            for (GuideBooks.Book topic : shelf.topics()) {
                ItemStack key = new ItemStack(GuideBooks.keyItem(topic), 2);
                helper.assertTrue(find(helper, List.of(book, key)).isEmpty(), "crafting extension still exists");
                helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, topic.ordinal()), "unlocked without key item");
                player.getInventory().setItem(9, key);
                if (GuideBooks.operatorOnly(topic)) {
                    helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, topic.ordinal()), "non-operator unlocked admin");
                    helper.assertValueEqual(key.getCount(), 2, "denied admin consumed item");
                    player.getInventory().setItem(9, ItemStack.EMPTY);
                    continue;
                }
                int before = GuideBooks.mask(book);
                // A poisoned key cannot inject a chapter mask.
                key.set(com.simplebuilding.component.ModDataComponentTypes.GUIDE_CHAPTERS, 1 << GuideBooks.Book.ADMIN.ordinal());
                helper.assertTrue(com.simplebuilding.guide.GuideUnlocks.unlock(player, topic.ordinal()), "unlock refused " + topic);
                helper.assertValueEqual(key.getCount(), 1, "unlock must consume exactly one item");
                helper.assertValueEqual(GuideBooks.mask(book), before | (1 << topic.ordinal()), "server accepted a spoofed mask");
                helper.assertValueEqual(book.get(DataComponents.CUSTOM_NAME), Component.literal("My guide"), "name erased");
                helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, topic.ordinal()), "duplicate unlock accepted");
                helper.assertValueEqual(key.getCount(), 1, "duplicate unlock consumed item");
                player.getInventory().setItem(9, ItemStack.EMPTY);
                var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, helper.getLevel().registryAccess());
                var decoded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, book).getOrThrow()).getOrThrow();
                helper.assertValueEqual(GuideBooks.mask(decoded), GuideBooks.mask(book), "save/load lost chapters from old or new books");
                var old = new net.minecraft.nbt.CompoundTag();
                old.putString("id", "simplebuilding:" + topic.itemName()); old.putInt("count", 1);
                com.simplebuilding.datafix.ModDataFixer.migrateGuide(old);
                helper.assertTrue((old.getCompound("components").orElseThrow().getIntOr("simplebuilding:guide_chapters", 0) & (1 << topic.ordinal())) != 0, "legacy migration lost chapter");
            }
            helper.assertTrue(find(helper, List.of(book, book.copy())).isEmpty(), "crafting combination still exists");
            // A moved/replaced book invalidates the exact-stack session.
            player.setItemInHand(main, new ItemStack(hub));
            player.getInventory().setItem(9, new ItemStack(GuideBooks.keyItem(first), 2));
            helper.assertTrue(!com.simplebuilding.guide.GuideUnlocks.unlock(player, first.ordinal()), "stale session changed a different book");
            com.simplebuilding.guide.GuideUnlocks.open(player, main);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(com.simplebuilding.guide.GuideUnlocks.unlock(player, first.ordinal()), "creative exchange refused");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "creative unlock must also cost one item");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var next = shelf.topics().get(1);
            ItemStack offhandKey = new ItemStack(GuideBooks.keyItem(next), 2);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhandKey);
            player.containerMenu = null;
            com.simplebuilding.networking.ModMessageHandlers.handleGuideUnlock(new com.simplebuilding.networking.GuideUnlockPayload(next.ordinal()), player);
            helper.assertTrue(!GuideBooks.inserted(player.getMainHandItem(), next), "packet accepted outside inventory menu");
            player.containerMenu = player.inventoryMenu;
            com.simplebuilding.networking.ModMessageHandlers.handleGuideUnlock(new com.simplebuilding.networking.GuideUnlockPayload(next.ordinal()), player);
            helper.assertTrue(GuideBooks.inserted(player.getMainHandItem(), next), "registered handler did not unlock from off hand");
            helper.assertValueEqual(offhandKey.getCount(), 1, "offhand exchange cost");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        helper.assertTrue(problems.isEmpty(), problems.toString());
        succeed(helper);
    }

    private static void expect(GameTestHelper helper, List<String> problems, List<ItemStack> grid, Item result, boolean guideStays) {
        CraftingInput input = CraftingInput.of(2, 1, grid);
        Optional<RecipeHolder<CraftingRecipe>> match = find(helper, grid);
        if (match.isEmpty()) {
            problems.add(grid + " crafts nothing, expected " + result);
            return;
        }
        ItemStack out = match.get().value().assemble(input);
        if (!out.is(result)) {
            problems.add(grid + " crafts " + out + " instead of " + result);
        }
        NonNullList<ItemStack> rest = match.get().value().getRemainingItems(input);
        for (int i = 0; i < grid.size(); i++) {
            boolean isGuide = grid.get(i).is(ModItems.GUIDE_BOOK) || grid.get(i).is(ModItems.GUIDE_BOOK_VANILLA_START);
            boolean stays = !rest.get(i).isEmpty() && rest.get(i).is(grid.get(i).getItem());
            if (isGuide && guideStays && !stays) {
                problems.add(grid + ": the guide does not stay in the grid (left " + rest.get(i) + ")");
            } else if (!isGuide && !rest.get(i).isEmpty()) {
                problems.add(grid + ": " + grid.get(i) + " leaves " + rest.get(i));
            }
        }
    }

    private static Optional<RecipeHolder<CraftingRecipe>> find(GameTestHelper helper, List<ItemStack> grid) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, grid), helper.getLevel());
    }

    /** Sammelt Uebersetzungsschluessel (auch in Argumenten) und Seitensprunge eines Seitenbaums. */
    private static void walk(Component component, Set<String> keys, List<Integer> jumps) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            keys.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component nested) {
                    walk(nested, keys, jumps);
                }
            }
        }
        if (component.getStyle().getClickEvent() instanceof ClickEvent.ChangePage change) {
            jumps.add(change.page());
        }
        for (Component sibling : component.getSiblings()) {
            walk(sibling, keys, jumps);
        }
    }

    private static JsonObject langFile(GameTestHelper helper, String locale) {
        String path = "assets/simplebuilding/lang/" + locale + ".json";
        try (InputStream in = GuideBookTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private static int placeholders(String text) {
        int count = 0;
        for (int i = text.indexOf('%'); i >= 0; i = text.indexOf('%', i + 1)) {
            count++;
        }
        return count;
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static void succeed(GameTestHelper helper) {
        helper.succeed();
    }
}
