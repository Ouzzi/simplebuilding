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
            helper.assertTrue(!player.getTags().contains(GuideBooks.GIVEN_TAG), "the switch off still marked the player as served");

            config.giveGuideBookOnFirstJoin = true;
            GuideBooks.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(ModItems.GUIDE_BOOK), 1, "guides given on the first join");
            helper.assertTrue(player.getTags().contains(GuideBooks.GIVEN_TAG), "the first join did not mark the player");
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
        List<String> problems = new ArrayList<>();
        expect(helper, problems, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.CRAFTING_TABLE)), ModItems.GUIDE_BOOK, false);
        for (GuideBooks.Book topic : GuideBooks.Book.topics()) {
            Item key = GuideBooks.keyItem(topic).asItem();
            Item result = GuideBooks.item(topic);
            expect(helper, problems, List.of(new ItemStack(Items.BOOK), new ItemStack(key)), result, false);
            expect(helper, problems, List.of(new ItemStack(key), new ItemStack(Items.BOOK)), result, false);
            expect(helper, problems, List.of(new ItemStack(ModItems.GUIDE_BOOK), new ItemStack(key)), result, true);
            expect(helper, problems, List.of(new ItemStack(key), new ItemStack(ModItems.GUIDE_BOOK)), result, true);
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
            WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content == null) {
                problems.add(book + " has no pages");
                continue;
            }
            if (!content.resolved()) {
                problems.add(book + " is not resolved, vanilla would rewrite it per reader");
            }
            List<Component> pages = content.getPages(false);
            int contentsPages = GuideBooks.contentsPages(book);
            int expectedPages = contentsPages + book.chapters() + (book == GuideBooks.Book.GUIDE ? GuideBooks.topicPages() : 0);
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
            if (colour == null || colour.getValue() != style.colour()) {
                problems.add(book + ": the name is not in the book colour but " + colour);
            }
            List<Component> lines = new ArrayList<>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), TooltipDisplay.DEFAULT, lines::add,
                    net.minecraft.world.item.TooltipFlag.NORMAL);
            List<String> lineKeys = new ArrayList<>();
            for (Component line : lines) {
                lineKeys.add(line.getContents() instanceof TranslatableContents t ? t.getKey() : line.getString());
            }
            if (!lineKeys.equals(List.of(GuideContent.taglineKey(book), GuideContent.MOD_NAME_KEY))) {
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
        helper.assertTrue(GuideBooks.item(admin) == ModItems.GUIDE_BOOK_ADMIN && admin.isTopic(), "the admin guide is not a topic book");
        expect(helper, problems, List.of(new ItemStack(ModItems.GUIDE_BOOK), new ItemStack(GuideBooks.keyItem(admin).asItem())),
                ModItems.GUIDE_BOOK_ADMIN, true);
        helper.assertTrue(problems.isEmpty(), problems.size() + " admin guide problems: " + problems);
        succeed(helper);
    }

    // =====================================================================================

    private static void expect(GameTestHelper helper, List<String> problems, List<ItemStack> grid, Item result, boolean guideStays) {
        CraftingInput input = CraftingInput.of(2, 1, grid);
        Optional<RecipeHolder<CraftingRecipe>> match = find(helper, grid);
        if (match.isEmpty()) {
            problems.add(grid + " crafts nothing, expected " + result);
            return;
        }
        ItemStack out = match.get().value().assemble(input, helper.getLevel().registryAccess());
        if (!out.is(result)) {
            problems.add(grid + " crafts " + out + " instead of " + result);
        }
        NonNullList<ItemStack> rest = match.get().value().getRemainingItems(input);
        for (int i = 0; i < grid.size(); i++) {
            boolean isGuide = grid.get(i).is(ModItems.GUIDE_BOOK);
            boolean stays = rest.get(i).is(ModItems.GUIDE_BOOK);
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
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static void succeed(GameTestHelper helper) {
        TestCleanup.succeed(helper);
    }
}
