package com.simplebuilding.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.guide.GuideBooks;
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
     * Inhaltsverzeichnis mit einem Sprung je Kapitel (Handbuch: plus Themenbuecher), jede
     * Kapitelseite springt zurueck auf Seite 1, kein Sprung zeigt ins Leere. Jeder Mod-Schluessel
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
            int expectedPages = 1 + book.chapters() + (book == GuideBooks.Book.GUIDE ? 2 : 0);
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
                int expectedJumps = p == 0 ? book.chapters() + (book == GuideBooks.Book.GUIDE ? 1 : 0) : 1;
                if (jumps.size() != expectedJumps) {
                    problems.add(book + " page " + (p + 1) + " has " + jumps.size() + " links instead of " + expectedJumps);
                }
                if (p > 0 && !jumps.equals(List.of(1))) {
                    problems.add(book + " page " + (p + 1) + " does not lead back to the contents: " + jumps);
                }
                if (p == 0) {
                    for (int i = 0; i < jumps.size(); i++) {
                        if (jumps.get(i) != i + 2) {
                            problems.add(book + " contents link " + (i + 1) + " jumps to page " + jumps.get(i) + " instead of " + (i + 2));
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
     * (der Server schickt dabei das Buch-oeffnen-Paket), sie passen aufs Lesepult und ins
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

    // =====================================================================================

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
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static void succeed(GameTestHelper helper) {
        helper.succeed();
    }
}
