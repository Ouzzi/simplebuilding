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
import net.minecraft.world.item.crafting.Ingredient;
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

    /** Shaped: lapis, book, lapis / three planks / plank, gap, plank = 2 lapis + 1 book + 5 planks. */
    private static void recipe(GameTestHelper h) {
        var holder = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, QuestionaryRegistry.TABLE_ID));
        h.assertTrue(holder.isPresent(), "recipe missing");
        List<Ingredient> ingredients = holder.get().value().placementInfo().ingredients();
        int lapis = 0, book = 0, planks = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(new ItemStack(Items.LAPIS_LAZULI))) lapis++;
            else if (ingredient.test(new ItemStack(Items.BOOK))) book++;
            else if (ingredient.test(new ItemStack(Items.OAK_PLANKS)) && ingredient.test(new ItemStack(Items.CHERRY_PLANKS))) planks++;
        }
        h.assertTrue(ingredients.size() == 8 && lapis == 2 && book == 1 && planks == 5,
                "2 lapis + 1 book + 5 planks, got " + ingredients.size() + ": " + lapis + "/" + book + "/" + planks);
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

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplequestionary.discover.QuestionaryDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplequestionary.discover.QuestionaryDiscover::behaviourHints);
    }
}
