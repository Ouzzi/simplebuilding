package com.simplequestionary.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplequestionary.QuestionaryConfig;
import com.simplequestionary.QuestionaryData;
import com.simplequestionary.QuestionaryData.Kind;
import com.simplequestionary.QuestionaryData.Page;
import com.simplequestionary.QuestionaryData.Source;
import com.simplequestionary.QuestionaryFacts;
import com.simplequestionary.QuestionaryMenu;
import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.QuestionarySheet;
import com.simplequestionary.SimpleQuestionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server tests of Simple Questionary; the loader adapters register them as {@code simplequestionary:module_game_test_<name>}. */
public final class QuestionaryTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    private static final Identifier ENDERITE_INGOT = Identifier.fromNamespaceAndPath("simplebuilding", "enderite_ingot");

    static {
        ALL.put("registered", QuestionaryTests::registered);
        ALL.put("recipe", QuestionaryTests::recipe);
        ALL.put("menu_opens", QuestionaryTests::menuOpens);
        ALL.put("slot_takes_item", QuestionaryTests::slotTakesItem);
        ALL.put("data_loaded", QuestionaryTests::dataLoaded);
        ALL.put("fallback_deterministic", QuestionaryTests::fallbackDeterministic);
        ALL.put("families", QuestionaryTests::families);
        ALL.put("facts", QuestionaryTests::facts);
        ALL.put("coverage", QuestionaryTests::coverage);
        ALL.put("host_switch", QuestionaryTests::hostSwitch);
        ALL.put("sheet_every_entry", QuestionaryTests::sheetEveryEntry);
        ALL.put("sheet_spans", QuestionaryTests::sheetSpans);
        ALL.put("sheet_layout", QuestionaryTests::sheetLayout);
    }

    private QuestionaryTests() {}

    /**
     * The questionary files are client resources; the server reads them here straight from its own packs (every mod
     * jar is one), opened as client resources - the same loader code the client's reload listener runs.
     */
    public static QuestionaryData.Library loadFromPacks(MinecraftServer server) {
        List<PackResources> packs = server.getPackRepository().getSelectedPacks().stream().flatMap(Pack::open).toList();
        try (MultiPackResourceManager manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, packs)) {
            return QuestionaryData.load(manager);
        }
    }

    private static Predicate<Identifier> tagsOf(Identifier item) {
        Item value = BuiltInRegistries.ITEM.getValue(item);
        return tag -> value.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
    }

    private static boolean sbLoaded() {
        return BuiltInRegistries.ITEM.containsKey(ENDERITE_INGOT);
    }

    private static void registered(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(QuestionaryRegistry.TABLE_ID) == QuestionaryRegistry.TABLE, "block registered");
        Item item = BuiltInRegistries.ITEM.getValue(QuestionaryRegistry.TABLE_ID);
        h.assertTrue(item instanceof BlockItem b && b.getBlock() == QuestionaryRegistry.TABLE, "block item registered");
        h.assertTrue(BuiltInRegistries.MENU.getValue(QuestionaryRegistry.TABLE_ID) == QuestionaryRegistry.MENU, "menu registered");
        h.assertTrue(QuestionaryRegistry.TABLE.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "mined with an axe");
        h.succeed();
    }

    /** Shaped (N50): lapis, book, lapis / three planks / plank, obsidian, plank = 2 lapis + 1 book + 5 planks + 1 obsidian. */
    private static void recipe(GameTestHelper h) {
        var holder = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, QuestionaryRegistry.TABLE_ID));
        h.assertTrue(holder.isPresent(), "recipe missing");
        List<Ingredient> ingredients = holder.get().value().placementInfo().ingredients();
        int lapis = 0, book = 0, planks = 0, obsidian = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(new ItemStack(Items.LAPIS_LAZULI))) lapis++;
            else if (ingredient.test(new ItemStack(Items.BOOK))) book++;
            else if (ingredient.test(new ItemStack(Items.OAK_PLANKS)) && ingredient.test(new ItemStack(Items.CHERRY_PLANKS))) planks++;
            else if (ingredient.test(new ItemStack(Items.OBSIDIAN))) obsidian++;
        }
        h.assertTrue(ingredients.size() == 9 && lapis == 2 && book == 1 && planks == 5 && obsidian == 1,
                "2 lapis + 1 book + 5 planks + 1 obsidian, got " + ingredients.size() + ": " + lapis + "/" + book + "/" + planks + "/" + obsidian);
        ItemStack made = QuestionaryFacts.compute(h.getLevel(), new ItemStack(QuestionaryRegistry.TABLE_ITEM)).makes() > 0 ? new ItemStack(QuestionaryRegistry.TABLE_ITEM) : ItemStack.EMPTY;
        h.assertTrue(!made.isEmpty(), "the recipe makes the table");
        h.succeed();
    }

    private static void menuOpens(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, QuestionaryRegistry.TABLE.defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        h.assertTrue(player.containerMenu instanceof QuestionaryMenu, "menu open, got " + player.containerMenu);
        h.assertTrue(player.containerMenu.stillValid(player), "menu valid next to the table");
        h.succeed();
    }

    /** Shift-click puts one piece in (never more), closing gives it back. */
    private static void slotTakesItem(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, QuestionaryRegistry.TABLE.defaultBlockState());
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, new ItemStack(Items.BOOK, 3));
        QuestionaryMenu menu = new QuestionaryMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(rel)));
        int hotbar0 = QuestionaryMenu.INVENTORY_START + 27;
        menu.quickMoveStack(player, hotbar0);
        h.assertTrue(menu.shown().is(Items.BOOK) && menu.shown().getCount() == 1, "one book in the slot: " + menu.shown());
        h.assertTrue(player.getInventory().getItem(0).getCount() == 2, "two books left");
        menu.quickMoveStack(player, hotbar0);
        h.assertTrue(menu.shown().getCount() == 1 && player.getInventory().getItem(0).getCount() == 2, "slot holds one piece only");
        h.assertTrue(menu.getSlot(QuestionaryMenu.ITEM_SLOT).mayPlace(new ItemStack(Items.DIAMOND_SWORD)), "any item fits");
        menu.removed(player);
        int books = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.BOOK)) books += player.getInventory().getItem(i).getCount();
        }
        h.assertTrue(books == 3 && menu.shown().isEmpty(), "book returned on close: " + books);
        h.succeed();
    }

    private static void dataLoaded(GameTestHelper h) {
        QuestionaryData.Library library = loadFromPacks(h.getLevel().getServer());
        for (String lang : List.of(QuestionaryData.ENGLISH, QuestionaryData.GERMAN)) {
            QuestionaryData.Language language = library.language(lang);
            h.assertTrue(language != null, "no questionary data for " + lang + " (namespaces " + library.namespaces() + ")");
            for (Kind kind : Kind.values()) {
                for (Page page : Page.values()) {
                    h.assertTrue(language.fallback(kind, page).size() == QuestionaryData.VARIANTS,
                            lang + " fallback " + kind.key() + "." + page.key + ": " + language.fallback(kind, page).size() + " variants");
                }
            }
        }
        if (sbLoaded()) {
            for (String lang : List.of(QuestionaryData.ENGLISH, QuestionaryData.GERMAN)) {
                QuestionaryData.Resolved r = library.resolveStrict(ENDERITE_INGOT, tag -> false, lang);
                for (Page page : Page.values()) {
                    h.assertTrue(r.source(page) == Source.ENTRY, "enderite ingot " + lang + " " + page.key + " from " + r.source(page));
                    h.assertTrue(QuestionaryData.problems(r.page(page)).isEmpty(), "enderite ingot " + lang + " " + page.key + ": " + QuestionaryData.problems(r.page(page)));
                }
            }
        } else {
            SimpleQuestionary.LOG.info("[standalone] SimpleBuilding absent: enderite example not checked");
        }
        h.succeed();
    }

    private static void fallbackDeterministic(GameTestHelper h) {
        h.assertTrue(QuestionaryData.fnv1a("") == 0x811C9DC5 && QuestionaryData.fnv1a("a") == 0xE40C292C, "FNV-1a reference values");
        QuestionaryData.Library library = loadFromPacks(h.getLevel().getServer());
        Identifier foreign = Identifier.fromNamespaceAndPath("othermod", "void_pearl_shard");
        for (String lang : List.of(QuestionaryData.ENGLISH, QuestionaryData.GERMAN, "fr_fr")) {
            QuestionaryData.Resolved a = library.resolve(foreign, Kind.MISC, tag -> false, lang);
            QuestionaryData.Resolved b = library.resolve(foreign, Kind.MISC, tag -> false, lang);
            for (Page page : Page.values()) {
                h.assertTrue(a.source(page) == Source.FALLBACK && !a.page(page).isEmpty(), lang + " " + page.key + " falls back");
                h.assertTrue(a.page(page).equals(b.page(page)), lang + " " + page.key + " stable");
            }
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 64; i++) {
            seen.add(library.resolve(Identifier.fromNamespaceAndPath("othermod", "thing_" + i), Kind.BLOCK, tag -> false, QuestionaryData.ENGLISH).page(Page.LORE).get(0));
        }
        h.assertTrue(seen.size() >= 5, "the hash spreads over the variants: " + seen.size());
        for (String lang : List.of(QuestionaryData.ENGLISH, QuestionaryData.GERMAN)) {
            for (Kind kind : Kind.values()) {
                for (Page page : Page.values()) {
                    for (String variant : library.language(lang).fallback(kind, page)) {
                        h.assertTrue(QuestionaryData.problems(List.of(variant)).isEmpty(), lang + " " + kind.key() + "." + page.key + ": " + variant);
                    }
                }
            }
        }
        h.assertTrue(QuestionaryData.kindOf(Items.DIAMOND_PICKAXE) == Kind.TOOL && QuestionaryData.kindOf(Items.IRON_CHESTPLATE) == Kind.ARMOR
                && QuestionaryData.kindOf(Items.BREAD) == Kind.FOOD && QuestionaryData.kindOf(Items.STONE) == Kind.BLOCK
                && QuestionaryData.kindOf(Items.STICK) == Kind.MISC, "item classes");
        h.succeed();
    }

    /** Families by regex and by tag; an own entry wins; a later layer replaces a key; placeholders. */
    private static void families(GameTestHelper h) {
        JsonObject bottom = JsonParser.parseString("""
                {"family:stairs": {"items": ["regex:.*_stairs"], "lore": ["{name} climbs."]},
                 "family:planks": {"items": ["#minecraft:planks"], "use": ["Wood."]},
                 "oak_stairs": {"lore": ["Old text."]},
                 "_facts": {"note": "ignored"}}""").getAsJsonObject();
        JsonObject top = JsonParser.parseString("{\"oak_stairs\": {\"lore\": [\"Own text.\"]}}").getAsJsonObject();
        QuestionaryData.Library library = QuestionaryData.parse(Map.of(Identifier.fromNamespaceAndPath("minecraft", "en_us"), List.of(bottom, top)));
        Identifier stone = Identifier.withDefaultNamespace("stone_stairs"), oak = Identifier.withDefaultNamespace("oak_stairs"),
                birch = Identifier.withDefaultNamespace("birch_planks"), dirt = Identifier.withDefaultNamespace("dirt");
        h.assertTrue(library.resolve(stone, Kind.BLOCK, tagsOf(stone), "en_us").source(Page.LORE) == Source.FAMILY, "regex family");
        h.assertTrue(library.resolve(oak, Kind.BLOCK, tagsOf(oak), "de_de").page(Page.LORE).equals(List.of("Own text.")), "own entry wins, top layer wins, English fallback");
        h.assertTrue(library.resolve(birch, Kind.BLOCK, tagsOf(birch), "en_us").source(Page.USE) == Source.FAMILY, "tag family");
        h.assertTrue(library.resolve(dirt, Kind.BLOCK, tagsOf(dirt), "en_us").source(Page.LORE) == Source.NONE, "no family for dirt");
        String text = QuestionaryData.format("{name} by {mod}, see {item:minecraft:book}.", "Stairs", "Minecraft", id -> id.getPath().toUpperCase());
        h.assertTrue(text.equals("Stairs by Minecraft, see BOOK."), "placeholders: " + text);
        h.assertTrue(!QuestionaryData.problems(List.of("{nme} TODO")).isEmpty(), "problems found");
        h.succeed();
    }

    private static void facts(GameTestHelper h) {
        QuestionaryFacts.Facts book = QuestionaryFacts.compute(h.getLevel(), new ItemStack(Items.BOOK));
        h.assertTrue(book.makes() >= 1 && !book.madeFrom().isEmpty(), "book is crafted: " + book);
        h.assertTrue(book.usedIn() >= 3, "book is used in bookshelf, enchanting table, questionary table ...: " + book.usedIn());
        QuestionaryFacts.Facts table = QuestionaryFacts.compute(h.getLevel(), new ItemStack(QuestionaryRegistry.TABLE_ITEM));
        Identifier lapis = BuiltInRegistries.ITEM.getKey(Items.LAPIS_LAZULI), bookId = BuiltInRegistries.ITEM.getKey(Items.BOOK);
        h.assertTrue(table.makes() == 1 && table.madeFrom().contains(lapis) && table.madeFrom().contains(bookId), "table made from lapis and book: " + table);
        // QT23: the grid keeps the recipe's shape (lapis, book, lapis / planks / plank, obsidian, plank), made at the crafting table.
        Identifier obsidian = BuiltInRegistries.ITEM.getKey(Items.OBSIDIAN);
        h.assertTrue(table.madeFrom().size() == QuestionaryFacts.MAX_INGREDIENTS && table.madeFrom().get(0).equals(lapis) && table.madeFrom().get(1).equals(bookId)
                && table.madeFrom().get(2).equals(lapis) && table.madeFrom().get(7).equals(obsidian), "shaped grid: " + table.madeFrom());
        h.assertTrue(table.station().equals(BuiltInRegistries.ITEM.getKey(Items.CRAFTING_TABLE)), "station: " + table.station());
        h.assertTrue(QuestionarySheet.origin(table, QuestionaryRegistry.TABLE_ITEM) == QuestionarySheet.Origin.CRAFT, "the table is crafted");
        QuestionaryFacts.Facts ingot = QuestionaryFacts.compute(h.getLevel(), new ItemStack(Items.IRON_INGOT));
        h.assertTrue(ingot.makes() >= 2 && !ingot.madeFrom().isEmpty(), "iron ingot has recipes: " + ingot);
        h.assertTrue(QuestionaryFacts.compute(h.getLevel(), ItemStack.EMPTY) == QuestionaryFacts.Facts.NONE, "empty slot: no facts");
        h.succeed();
    }

    /** Counts items without own text (warning only while the texts are being written); text problems are errors. */
    private static void coverage(GameTestHelper h) {
        QuestionaryData.Library library = loadFromPacks(h.getLevel().getServer());
        Set<String> namespaces = QuestionaryData.describedNamespaces(library.namespaces());
        namespaces.remove(SimpleQuestionary.MOD_ID);
        QuestionaryData.Coverage coverage = QuestionaryData.coverage(library, BuiltInRegistries.ITEM.keySet(), namespaces, QuestionaryTests::tagsOf);
        coverage.items().forEach((ns, total) -> SimpleQuestionary.LOG.warn("[coverage] {}: {} of {} items without questionary text (EN+DE)",
                ns, coverage.missing().getOrDefault(ns, 0), total));
        h.assertTrue(coverage.problems().isEmpty(), "text problems: " + coverage.problems());
        h.assertTrue(coverage.items().containsKey("minecraft"), "Vanilla items counted");
        if (sbLoaded()) h.assertTrue(!coverage.missingIds().contains(ENDERITE_INGOT), "the example is complete");
        h.succeed();
    }

    private static void hostSwitch(GameTestHelper h) {
        boolean before = QuestionaryConfig.enabled;
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplequestionary-host");
            h.assertTrue(QuestionaryConfig.hostAllows(dir), "no host file: on");
            java.nio.file.Files.writeString(dir.resolve(QuestionaryConfig.HOST_FILE), "{\"enableSimpleQuestionary\": false}");
            h.assertTrue(!QuestionaryConfig.hostAllows(dir), "host says off");
            java.nio.file.Files.writeString(dir.resolve(QuestionaryConfig.HOST_FILE), "{\"enableSimpleQuestionary\": true}");
            h.assertTrue(QuestionaryConfig.hostAllows(dir), "host says on");
            java.nio.file.Files.writeString(dir.resolve(QuestionaryConfig.HOST_FILE), "not json");
            h.assertTrue(QuestionaryConfig.hostAllows(dir), "unreadable host file: on");
            QuestionaryConfig.enabled = false;
            h.assertTrue(QuestionaryRegistry.tabStacks().isEmpty(), "disabled: not offered");
            QuestionaryConfig.enabled = true;
            h.assertTrue(QuestionaryRegistry.tabStacks().size() == 1, "enabled again");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            QuestionaryConfig.enabled = before;
        }
        h.succeed();
    }

    /**
     * QT23: every questionary entry (own entries and family members, EN and DE) has text on all three pages for the
     * left side of the book, and something for the right side: the item for the picture plate, a find hint - a recipe
     * grid when a recipe makes it, else mined/found - and an item model for the magnifier.
     */
    private static void sheetEveryEntry(GameTestHelper h) {
        QuestionaryData.Library library = loadFromPacks(h.getLevel().getServer());
        ContextMap context = SlotDisplayContext.fromLevel(h.getLevel());
        // One pass over all recipes: which items a recipe with a showable grid makes.
        Set<Identifier> withGrid = new HashSet<>();
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            try {
                List<Ingredient> ingredients = recipe.placementInfo().ingredients();
                for (RecipeDisplay display : recipe.display()) {
                    if (QuestionaryFacts.distinct(QuestionaryFacts.grid(display, ingredients, context)) == 0) continue;
                    for (ItemStack result : display.result().resolveForStacks(context)) withGrid.add(BuiltInRegistries.ITEM.getKey(result.getItem()));
                }
            } catch (RuntimeException e) {
                // special recipes without placement info show no grid
            }
        }
        List<String> problems = new java.util.ArrayList<>();
        Map<QuestionarySheet.Origin, Integer> origins = new java.util.EnumMap<>(QuestionarySheet.Origin.class);
        int checked = 0, absent = 0;
        for (String lang : List.of(QuestionaryData.ENGLISH, QuestionaryData.GERMAN)) {
            QuestionaryData.Language language = library.language(lang);
            Set<Identifier> items = new java.util.LinkedHashSet<>(language.entries().keySet());
            for (QuestionaryData.Family family : language.families()) {
                for (Identifier id : BuiltInRegistries.ITEM.keySet()) if (family.matches(id, tagsOf(id))) items.add(id);
            }
            for (Identifier id : items) {
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null || item == Items.AIR) {
                    absent++;
                    continue;
                }
                checked++;
                QuestionaryData.Resolved resolved = library.resolve(id, QuestionaryData.kindOf(item), tagsOf(id), lang);
                for (Page page : Page.values()) {
                    if (resolved.page(page).isEmpty()) problems.add(id + " " + lang + " " + page.key + ": no text");
                    for (String paragraph : resolved.page(page)) {
                        if (QuestionarySheet.spans(paragraph, "Name", "Mod", Identifier::getPath).isEmpty()) problems.add(id + " " + lang + " " + page.key + ": empty paragraph");
                    }
                }
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) problems.add(id + ": no picture for the plate");
                if (stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL) == null) problems.add(id + ": no item model for the magnifier");
                QuestionaryFacts.Facts facts = new QuestionaryFacts.Facts(id, withGrid.contains(id) ? List.of(id) : List.of(), id, 0, 0);
                origins.merge(QuestionarySheet.origin(facts, item), 1, Integer::sum);
            }
        }
        SimpleQuestionary.LOG.info("[sheet] {} entries checked (EN+DE), {} absent items, origins {}", checked, absent, origins);
        h.assertTrue(problems.isEmpty(), problems.size() + " sheet problems, first: " + problems.subList(0, Math.min(10, problems.size())));
        h.assertTrue(checked >= 300, "Vanilla entries alone are more than 300 per language: " + checked);
        h.assertTrue(origins.getOrDefault(QuestionarySheet.Origin.CRAFT, 0) > 0 && origins.getOrDefault(QuestionarySheet.Origin.MINE, 0) > 0
                && origins.getOrDefault(QuestionarySheet.Origin.FIND, 0) > 0, "all three find hints occur: " + origins);
        h.succeed();
    }

    /** Keywords: placeholders, **marked** words, the item's own name and numbers are bold; the rest is plain. */
    private static void sheetSpans(GameTestHelper h) {
        List<QuestionarySheet.Span> spans = QuestionarySheet.spans("{name} burns 120 s, **much** longer than {item:minecraft:coal}; a Rod stays.", "Rod", "Mod",
                id -> id.getPath().toUpperCase(java.util.Locale.ROOT));
        List<QuestionarySheet.Span> expected = List.of(new QuestionarySheet.Span("Rod", true), new QuestionarySheet.Span(" burns ", false),
                new QuestionarySheet.Span("120", true), new QuestionarySheet.Span(" s, ", false), new QuestionarySheet.Span("much", true),
                new QuestionarySheet.Span(" longer than ", false), new QuestionarySheet.Span("COAL", true), new QuestionarySheet.Span("; a ", false),
                new QuestionarySheet.Span("Rod", true), new QuestionarySheet.Span(" stays.", false));
        h.assertTrue(spans.equals(expected), "spans: " + spans);
        h.assertTrue(QuestionarySheet.spans("Plain text.", "", "Mod", Identifier::getPath).equals(List.of(new QuestionarySheet.Span("Plain text.", false))), "plain");
        h.assertTrue(QuestionarySheet.spans("{mod} 1.5", "a.b", "Simple", Identifier::getPath)
                .equals(List.of(new QuestionarySheet.Span("Simple", true), new QuestionarySheet.Span(" ", false), new QuestionarySheet.Span("1.5", true))), "mod and decimal");
        Identifier stone = BuiltInRegistries.ITEM.getKey(Items.STONE);
        h.assertTrue(QuestionarySheet.origin(new QuestionaryFacts.Facts(stone, List.of(), stone, 0, 0), Items.STONE) == QuestionarySheet.Origin.MINE, "a block is mined");
        h.assertTrue(QuestionarySheet.origin(new QuestionaryFacts.Facts(stone, List.of(), stone, 0, 0), Items.BLAZE_ROD) == QuestionarySheet.Origin.FIND, "an item is found");
        h.succeed();
    }

    /** The window fits 427 x 240 with the column in the middle; every ink of the pages reads on the paper (4.5:1). */
    private static void sheetLayout(GameTestHelper h) {
        h.assertTrue(QuestionaryMenu.WIDTH <= 427 && QuestionaryMenu.HEIGHT <= 240, "window " + QuestionaryMenu.WIDTH + " x " + QuestionaryMenu.HEIGHT);
        h.assertTrue(QuestionaryMenu.COLUMN_X * 2 + QuestionaryMenu.COLUMN_WIDTH == QuestionaryMenu.WIDTH, "column in the middle");
        h.assertTrue(QuestionaryMenu.COLUMN_Y + QuestionaryMenu.DESK_HEIGHT + QuestionaryMenu.BOX_GAP + QuestionaryMenu.INVENTORY_BOX <= QuestionaryMenu.HEIGHT, "column fits the height");
        h.assertTrue(QuestionaryMenu.INVENTORY_X >= QuestionaryMenu.COLUMN_X && QuestionaryMenu.INVENTORY_X + 9 * 18 <= QuestionaryMenu.COLUMN_X + QuestionaryMenu.COLUMN_WIDTH
                && QuestionaryMenu.SLOT_X > QuestionaryMenu.COLUMN_X && QuestionaryMenu.SLOT_X + 16 < QuestionaryMenu.COLUMN_X + QuestionaryMenu.COLUMN_WIDTH, "slots inside the column");
        for (int ink : QuestionarySheet.INKS) {
            for (int paper : new int[] {QuestionarySheet.PAPER, QuestionarySheet.PAPER_DARK}) {
                double ratio = com.simplelib.api.client.ui.UiContrast.ratio(ink, paper);
                h.assertTrue(ratio >= com.simplelib.api.client.ui.UiContrast.MIN, Integer.toHexString(ink) + " on " + Integer.toHexString(paper) + ": " + ratio);
            }
        }
        h.succeed();
    }

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplequestionary.discover.QuestionaryDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplequestionary.discover.QuestionaryDiscover::behaviourHints);
    }
}
