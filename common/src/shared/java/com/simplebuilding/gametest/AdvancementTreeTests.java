package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.advancement.FeatureTrigger;
import com.simplebuilding.advancement.ModTriggers;
import com.simplebuilding.compat.FtbQuestsDefaults;
import com.simplebuilding.items.ModItems;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The SimpleBuilding advancement tree (datagen: {@code ModAdvancementProvider}) and the mod's own
 * trigger {@code simplebuilding:feature_used} ({@link ModTriggers}).
 *
 * <p>"The tree" is every advancement in the {@code simplebuilding} namespace except the recipe
 * unlocks ({@code recipes/}) and the hidden easter chain ({@code easter/}, tested in
 * {@code TweaksEasterTests}).
 *
 * <p><b>Not covered here:</b> that each hook sits at the right moment of its feature. The finished
 * machine upgrade is checked on the real path in {@code SledgehammerUpgradeTests}; the octant
 * corner below drives {@code OctantItem#useOn}; every other feature is fired directly.
 */
public final class AdvancementTreeTests {

    private AdvancementTreeTests() {
    }

    private static final String MOD_ID = "simplebuilding";

    /** Vanilla advancements the tree may hang teasers under - and nothing else of vanilla's. */
    private static final Set<String> VANILLA_PARENTS = Set.of("minecraft:story/upgrade_tools", "minecraft:nether/root");

    /** The mod's own tabs, like vanilla's Story/Nether/End/Adventure: the only entries without a parent. */
    private static final Set<String> TAB_ROOTS = Set.of(MOD_ID + ":root", MOD_ID + ":building/root", MOD_ID + ":tweaks/root",
            MOD_ID + ":end/root", MOD_ID + ":guides/root");

    /**
     * Every advancement file of the tree loads, and every loaded one is complete: it has a display
     * that is not hidden, a translated title and description in English and German, an icon that is
     * a registered item, and a parent chain without cycles that ends in one of the four tab roots
     * (the only advancements without a parent, each with a background) or, for the teasers, in one
     * of the two allowed vanilla advancements. Every tab holds at least five advancements. Every feature name a criterion waits for is one {@link ModTriggers} knows, and
     * every feature {@link ModTriggers} reports is awaited by at least one advancement.
     *
     * <p>What breaks this test: a JSON that no longer parses (a renamed trigger or item, a trigger
     * type that was not registered on this loader), a missing lang key in either language, a hidden
     * flag, a fifth root, a parent that does not exist, a new feature nobody listens to.
     */
    public static void theTreeLoadsCompletelyAndEveryEntryIsTranslated(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        Set<String> files = new TreeSet<>();
        helper.getLevel().getServer().getResourceManager()
                .listResources("advancement", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json"))
                .keySet()
                .forEach(id -> {
                    String path = id.getPath().substring("advancement/".length(), id.getPath().length() - ".json".length());
                    if (inTree(path)) {
                        files.add(MOD_ID + ":" + path);
                    }
                });
        helper.assertTrue(files.size() >= 80, "only " + files.size() + " advancement files of the tree are on the data path");

        List<AdvancementHolder> tree = tree(manager);
        Set<String> loaded = new TreeSet<>();
        tree.forEach(holder -> loaded.add(holder.id().toString()));
        Set<String> notLoaded = new TreeSet<>(files);
        notLoaded.removeAll(loaded);
        helper.assertTrue(notLoaded.isEmpty(), "advancement files that did not load: " + notLoaded);

        JsonObject english = langFile(helper, "en_us");
        JsonObject german = langFile(helper, "de_de");
        Set<String> awaited = new HashSet<>();
        java.util.Map<String, Integer> perTab = new java.util.HashMap<>();
        int roots = 0;
        for (AdvancementHolder holder : tree) {
            String id = holder.id().toString();
            JsonObject display = displayJson(helper, holder);
            helper.assertTrue(display != null, id + " has no display");
            helper.assertTrue(!display.has("hidden") || !display.get("hidden").getAsBoolean(),
                    id + " is hidden - only the easter chain may be");
            for (String part : List.of("title", "description")) {
                String key = display.getAsJsonObject(part).get("translate").getAsString();
                // Guide advancements reuse the guide's own texts: the title is the book title, the description
                // one of the shared "advancements.<mod>.guides.*" lines.
                boolean guideReuse = holder.id().getPath().startsWith("guides/")
                        && (key.startsWith("book." + MOD_ID + ".") || key.startsWith("advancements." + MOD_ID + ".guides."));
                helper.assertTrue(guideReuse || key.equals("advancements." + MOD_ID + "." + holder.id().getPath().replace('/', '.') + "." + part),
                        id + " uses the " + part + " key " + key);
                helper.assertTrue(english.has(key) && !english.get(key).getAsString().isBlank(), key + " is missing in en_us.json");
                helper.assertTrue(german.has(key) && !german.get(key).getAsString().isBlank(), key + " is missing in de_de.json");
            }
            Identifier icon = Identifier.parse(display.getAsJsonObject("icon").get("id").getAsString());
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(icon) && !icon.equals(Identifier.withDefaultNamespace("air")),
                    id + " shows the icon " + icon + ", which is no registered item");

            Optional<Identifier> parent = holder.value().parent();
            if (parent.isEmpty()) {
                roots++;
                helper.assertTrue(TAB_ROOTS.contains(id), id + " has no parent but is none of the tab roots " + TAB_ROOTS);
                helper.assertTrue(display.has("background"), "the tab " + id + " has no background");
            } else {
                String top = topOf(helper, manager, holder);
                helper.assertTrue(TAB_ROOTS.contains(top) || top.equals("minecraft:story/root") || top.equals("minecraft:nether/root"),
                        id + " does not lead to a SimpleBuilding tab or an allowed vanilla tab but to " + top);
                perTab.merge(top, 1, Integer::sum);
                if (parent.get().getNamespace().equals("minecraft")) {
                    helper.assertTrue(VANILLA_PARENTS.contains(parent.get().toString()),
                            id + " hangs under the vanilla advancement " + parent.get());
                }
            }
            holder.value().criteria().values().forEach(criterion -> {
                if (criterion.triggerInstance() instanceof FeatureTrigger.TriggerInstance instance) {
                    helper.assertTrue(ModTriggers.ALL.contains(instance.feature()), id + " waits for the unknown feature " + instance.feature());
                    awaited.add(instance.feature());
                }
            });
        }
        helper.assertValueEqual(roots, TAB_ROOTS.size(), "advancements of the tree without a parent");
        for (String tab : TAB_ROOTS) {
            helper.assertTrue(perTab.getOrDefault(tab, 0) >= 5, "the tab " + tab + " holds only " + perTab.getOrDefault(tab, 0) + " advancements");
        }
        List<String> unused = new ArrayList<>(ModTriggers.ALL);
        unused.removeAll(awaited);
        helper.assertTrue(unused.isEmpty(), "features no advancement waits for: " + unused);
        helper.succeed();
    }

    /**
     * Firing each feature grants exactly the advancements that wait for it: before its own feature
     * fires, no advancement of a later feature is done yet - so no earlier feature granted it by
     * mistake - and afterwards all of them are.
     *
     * <p>What breaks this test: a trigger that is not registered or does not fire listeners, a
     * criterion that compares the wrong field, an advancement waiting for a misspelt feature.
     */
    public static void everyFeatureGrantsTheAdvancementsThatWaitForIt(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        ServerPlayer player = mockPlayer(helper);
        List<AdvancementHolder> tree = tree(manager);
        for (String feature : ModTriggers.ALL) {
            List<AdvancementHolder> waiting = tree.stream().filter(holder -> waitsFor(holder, feature)).toList();
            helper.assertTrue(!waiting.isEmpty(), "no advancement waits for " + feature);
            for (AdvancementHolder holder : waiting) {
                helper.assertTrue(!done(player, holder), holder.id() + " was done before its feature " + feature + " fired");
            }
            ModTriggers.feature(player, feature);
            for (AdvancementHolder holder : waiting) {
                helper.assertTrue(done(player, holder), "firing " + feature + " did not grant " + holder.id());
            }
        }
        helper.succeed();
    }

    /**
     * The item advancements follow the inventory: a crafting table opens the first tab, a chisel the
     * Building tab and end stone the End tab; any sledgehammer earns the "Hammer Time" teaser under
     * vanilla's "Getting an Upgrade", and "Void Walker" needs all four enderite armor pieces, not
     * just three.
     */
    public static void itemAdvancementsFollowTheInventory(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        ServerPlayer player = mockPlayer(helper);
        AdvancementHolder root = require(helper, manager, "root");
        AdvancementHolder hammerTime = require(helper, manager, "story/hammer_time");
        AdvancementHolder voidWalker = require(helper, manager, "enderite/void_walker");
        helper.assertValueEqual(hammerTime.value().parent(), Optional.of(Identifier.withDefaultNamespace("story/upgrade_tools")),
                "parent of the Hammer Time teaser");

        helper.assertTrue(!done(player, root), "the tab root was done before a crafting table was in the inventory");
        give(player, new ItemStack(Items.CRAFTING_TABLE), 0);
        helper.assertTrue(done(player, root), "a crafting table did not open the SimpleBuilding tab");

        give(player, new ItemStack(ModItems.GOLD_SLEDGEHAMMER), 1);
        helper.assertTrue(done(player, hammerTime), "a gold sledgehammer did not earn Hammer Time");

        AdvancementHolder buildingTab = require(helper, manager, "building/root");
        AdvancementHolder endTab = require(helper, manager, "end/root");
        helper.assertTrue(!done(player, buildingTab) && !done(player, endTab), "a tab opened before its item was in the inventory");
        give(player, new ItemStack(ModItems.STONE_CHISEL), 6);
        helper.assertTrue(done(player, buildingTab), "a stone chisel did not open the Building & Blueprints tab");
        helper.assertTrue(!done(player, endTab), "a stone chisel opened the End tab");
        give(player, new ItemStack(Items.END_STONE), 7);
        helper.assertTrue(done(player, endTab), "end stone did not open the Beyond the End tab");

        give(player, new ItemStack(ModItems.ENDERITE_HELMET), 2);
        give(player, new ItemStack(ModItems.ENDERITE_CHESTPLATE), 3);
        give(player, new ItemStack(ModItems.ENDERITE_LEGGINGS), 4);
        helper.assertTrue(!done(player, voidWalker), "three enderite armor pieces already earned Void Walker");
        give(player, new ItemStack(ModItems.ENDERITE_BOOTS), 5);
        helper.assertTrue(done(player, voidWalker), "the full enderite set did not earn Void Walker");
        helper.succeed();
    }

    /**
     * The real path of one hook: an Octant used on a block ({@code OctantItem#useOn}, the same call
     * a click makes) marks its corner and earns "Measure Twice".
     */
    public static void markingOneCornerWithTheOctantEarnsMeasureTwice(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        ServerPlayer player = mockPlayer(helper);
        AdvancementHolder measure = require(helper, manager, "octant/measure_twice");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.STONE);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);
        player.setShiftKeyDown(false);
        helper.assertTrue(!done(player, measure), "Measure Twice was done before the octant was used");
        BlockPos absolute = helper.absolutePos(pos);
        octant.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.assertTrue(done(player, measure), "marking a corner with the Octant did not earn Measure Twice");
        helper.succeed();
    }

    /**
     * Every recipe unlock of the mod ({@code recipes/...}) hands out at least one recipe, and every
     * recipe it hands out is loaded. Until 2026-09-28 the 34 {@code upgrade_*} unlocks of
     * {@code ModRecipeProvider#createUpgradeRecipe} only waited for the template and granted
     * nothing; a new provider method that forgets the reward turns this red.
     */
    public static void everyRecipeUnlockHandsOutAnExistingRecipe(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int unlocks = 0;
        List<String> problems = new ArrayList<>();
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (!holder.id().getNamespace().equals(MOD_ID) || !holder.id().getPath().startsWith("recipes/")) {
                continue;
            }
            unlocks++;
            var recipes = holder.value().rewards().recipes();
            if (recipes.isEmpty()) {
                problems.add(holder.id() + " unlocks no recipe");
            }
            for (var key : recipes) {
                if (server.getRecipeManager().byKey(key).isEmpty()) {
                    problems.add(holder.id() + " hands out the missing recipe " + key);
                }
            }
        }
        helper.assertTrue(unlocks >= 300, "only " + unlocks + " recipe unlocks are loaded");
        helper.assertTrue(problems.isEmpty(), problems.size() + " broken recipe unlocks: " + problems);
        helper.succeed();
    }

    /**
     * The FTB Quests book the mod ships ({@code data/simplebuilding/ftbquests/}, written by
     * {@code tools/quests/generate_quests.py}, installed by {@link FtbQuestsDefaults}) is complete
     * without FTB Quests being present: every file the install list names parses in this line's
     * format (JSON5 on 26.x, SNBT on 1.21.11), every chapter belongs to the SimpleBuilding group,
     * every id is a unique positive 16-digit hex long, every dependency names a quest of the book,
     * every item task names a registered item and every advancement task a loaded advancement, and
     * every chapter and quest has a title and description whose translation key exists - the mod's
     * own keys in English and German, vanilla's keys in the server language. The stages hold: each
     * stage chapter ends in one gear-shaped capstone that waits for every required quest of the
     * chapter, and every quest of stage n+1 waits (through its dependencies) for the capstone of
     * stage n, with no dependency cycle anywhere.
     *
     * <p>What breaks this test: a renamed item or advancement, a hand edit that broke the syntax,
     * a quest text without a lang key, a stage quest that forgot its dependency, a cycle.
     */
    public static void theFtbQuestsBookIsCompleteAndFormsStages(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        JsonObject english = langFile(helper, "en_us");
        JsonObject german = langFile(helper, "de_de");
        String format = questFormat(helper);
        List<String> problems = new ArrayList<>();

        String groupId = null;
        java.util.Map<String, JsonElement> lang = new java.util.HashMap<>();
        java.util.Map<String, JsonObject> chapters = new java.util.TreeMap<>();
        for (String file : shippedQuestFiles(helper)) {
            JsonObject json = parseQuestFile(helper, format, file);
            if (file.startsWith("chapter_groups.")) {
                JsonArray groups = json.getAsJsonArray("chapter_groups");
                helper.assertTrue(groups != null && groups.size() == 1, "chapter_groups ships " + groups + " instead of one group");
                groupId = groups.get(0).getAsJsonObject().get("id").getAsString();
            } else if (file.startsWith("chapters/")) {
                chapters.put(file.substring("chapters/".length(), file.lastIndexOf('.')), json);
            } else if (file.startsWith("lang/")) {
                json.entrySet().forEach(e -> lang.put(e.getKey(), e.getValue()));
            }
        }
        helper.assertTrue(groupId != null, "the book ships no chapter group");
        helper.assertTrue(chapters.size() >= 6, "only " + chapters.size() + " chapters are shipped");
        checkId(groupId, "the group", problems);
        checkTranslated("chapter_group." + groupId + ".title", lang, english, german, problems);

        java.util.Map<String, JsonObject> quests = new java.util.HashMap<>();
        java.util.Map<String, String> chapterOf = new java.util.HashMap<>();
        Set<String> ids = new HashSet<>();
        ids.add(groupId);
        for (var chapter : chapters.entrySet()) {
            JsonObject json = chapter.getValue();
            String id = json.get("id").getAsString();
            checkId(id, chapter.getKey(), problems);
            if (!ids.add(id)) {
                problems.add("duplicate id " + id);
            }
            if (!groupId.equals(json.get("group").getAsString())) {
                problems.add(chapter.getKey() + " is not in the SimpleBuilding group");
            }
            if (!chapter.getKey().equals(json.get("filename").getAsString())) {
                problems.add(chapter.getKey() + " names the file " + json.get("filename"));
            }
            checkTranslated("chapter." + id + ".title", lang, english, german, problems);
            checkTranslated("chapter." + id + ".chapter_subtitle", lang, english, german, problems);
            for (JsonElement element : json.getAsJsonArray("quests")) {
                JsonObject quest = element.getAsJsonObject();
                String questId = quest.get("id").getAsString();
                checkId(questId, chapter.getKey() + " quest", problems);
                if (!ids.add(questId)) {
                    problems.add("duplicate id " + questId);
                }
                quests.put(questId, quest);
                chapterOf.put(questId, chapter.getKey());
                if (!quest.has("x") || !quest.has("y")) {
                    problems.add("quest " + questId + " has no position");
                }
                checkTranslated("quest." + questId + ".title", lang, english, german, problems);
                checkTranslated("quest." + questId + ".quest_desc", lang, english, german, problems);
                JsonArray tasks = quest.getAsJsonArray("tasks");
                if (tasks == null || tasks.isEmpty()) {
                    problems.add("quest " + questId + " has no task");
                    continue;
                }
                for (JsonElement taskElement : tasks) {
                    JsonObject task = taskElement.getAsJsonObject();
                    String taskId = task.get("id").getAsString();
                    checkId(taskId, "task of " + questId, problems);
                    if (!ids.add(taskId)) {
                        problems.add("duplicate id " + taskId);
                    }
                    switch (task.get("type").getAsString()) {
                        case "item" -> {
                            Identifier item = Identifier.parse(task.getAsJsonObject("item").get("id").getAsString());
                            if (!BuiltInRegistries.ITEM.containsKey(item)) {
                                problems.add("quest " + questId + " wants the unknown item " + item);
                            }
                        }
                        case "advancement" -> {
                            Identifier advancement = Identifier.parse(task.get("advancement").getAsString());
                            if (manager.get(advancement) == null) {
                                problems.add("quest " + questId + " waits for the unknown advancement " + advancement);
                            }
                        }
                        default -> problems.add("quest " + questId + " has the task type " + task.get("type"));
                    }
                }
            }
        }
        java.util.Map<String, List<String>> deps = new java.util.HashMap<>();
        quests.forEach((id, quest) -> {
            List<String> list = new ArrayList<>();
            JsonArray array = quest.getAsJsonArray("dependencies");
            if (array != null) {
                array.forEach(dep -> {
                    list.add(dep.getAsString());
                    if (!quests.containsKey(dep.getAsString())) {
                        problems.add("quest " + id + " depends on the unknown quest " + dep.getAsString());
                    }
                });
            }
            deps.put(id, list);
        });
        helper.assertTrue(problems.isEmpty(), problems.size() + " problems in the FTB Quests book: " + problems);

        // Stages: simplebuilding_stage_<n>, each with one gear-shaped capstone.
        String previousCapstone = null;
        for (int stage = 1; chapters.containsKey("simplebuilding_stage_" + stage); stage++) {
            String chapter = "simplebuilding_stage_" + stage;
            List<String> members = quests.keySet().stream().filter(id -> chapter.equals(chapterOf.get(id))).toList();
            List<String> capstones = members.stream()
                    .filter(id -> quests.get(id).has("shape") && quests.get(id).get("shape").getAsString().equals("gear")).toList();
            helper.assertTrue(capstones.size() == 1, chapter + " has " + capstones.size() + " capstones instead of one");
            String capstone = capstones.get(0);
            for (String member : members) {
                boolean optional = quests.get(member).has("optional") && isTrue(quests.get(member).get("optional"));
                if (!member.equals(capstone) && !optional) {
                    helper.assertTrue(deps.get(capstone).contains(member), "the capstone of " + chapter + " does not wait for " + member);
                }
                Set<String> ancestors = ancestors(helper, deps, member);
                if (previousCapstone != null) {
                    helper.assertTrue(ancestors.contains(previousCapstone),
                            member + " in " + chapter + " does not wait for the end of stage " + (stage - 1));
                }
            }
            previousCapstone = capstone;
        }
        helper.assertTrue(previousCapstone != null && chapters.containsKey("simplebuilding_stage_4"), "the book has fewer than four stages");
        helper.succeed();
    }

    /**
     * Installing the book into a quest folder ({@link FtbQuestsDefaults#install}, run against a
     * temporary folder): a new book gets every shipped file and a marker, a second install changes
     * nothing, and in an existing book (a modpack's own group, data file and, on 1.21.11, lang file)
     * nothing is overwritten - the SimpleBuilding group and texts are inserted next to the pack's
     * entries and the files still parse.
     */
    public static void installingTheQuestBookAddsButNeverOverwrites(GameTestHelper helper) {
        String format = questFormat(helper);
        java.nio.file.Path fresh = null;
        java.nio.file.Path existing = null;
        try {
            fresh = java.nio.file.Files.createTempDirectory("sb-ftbquests-fresh");
            int written = FtbQuestsDefaults.install(fresh);
            List<String> shipped = shippedQuestFiles(helper);
            helper.assertTrue(written == shipped.size(), "a new book got " + written + " of " + shipped.size() + " files");
            for (String file : shipped) {
                helper.assertTrue(java.nio.file.Files.exists(fresh.resolve(file)), file + " was not installed");
            }
            helper.assertTrue(java.nio.file.Files.exists(fresh.resolve(FtbQuestsDefaults.MARKER)), "no install marker was written");
            helper.assertTrue(FtbQuestsDefaults.install(fresh) == 0, "a second install changed the book again");

            existing = java.nio.file.Files.createTempDirectory("sb-ftbquests-existing");
            String ext = "." + format;
            String foreignGroup = "0123456789ABCDEF";
            boolean json5 = format.equals("json5");
            java.nio.file.Files.writeString(existing.resolve("data" + ext), "KEEP");
            java.nio.file.Files.writeString(existing.resolve("chapter_groups" + ext), json5
                    ? "{\n\t\"chapter_groups\": [\n\t\t{ \"id\": \"" + foreignGroup + "\" }\n\t]\n}\n"
                    : "{\n\tchapter_groups: [\n\t\t{ id: \"" + foreignGroup + "\" }\n\t]\n}\n");
            if (!json5) {
                java.nio.file.Files.createDirectories(existing.resolve("lang"));
                java.nio.file.Files.writeString(existing.resolve("lang/en_us.snbt"), "{\n\t\"chapter." + foreignGroup + ".title\": \"Pack\"\n}\n");
            }
            FtbQuestsDefaults.install(existing);
            helper.assertTrue(java.nio.file.Files.readString(existing.resolve("data" + ext)).equals("KEEP"), "the pack's data file was overwritten");
            JsonObject groups = parseQuestText(format, java.nio.file.Files.readString(existing.resolve("chapter_groups" + ext)));
            List<String> groupIds = new ArrayList<>();
            groups.getAsJsonArray("chapter_groups").forEach(g -> groupIds.add(g.getAsJsonObject().get("id").getAsString()));
            helper.assertTrue(groupIds.size() == 2 && groupIds.contains(foreignGroup), "after the install the groups are " + groupIds);
            if (!json5) {
                JsonObject langTable = parseQuestText(format, java.nio.file.Files.readString(existing.resolve("lang/en_us.snbt")));
                helper.assertTrue(langTable.has("chapter." + foreignGroup + ".title") && langTable.size() > 20,
                        "the pack's lang file lost its entry or got none of ours: " + langTable.size() + " entries");
            }
            helper.assertTrue(FtbQuestsDefaults.install(existing) == 0, "a second install changed the existing book again");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            deleteTree(fresh);
            deleteTree(existing);
        }
        helper.succeed();
    }

    // -------------------------------------------------------------------------------------

    // ---- FTB Quests book ----------------------------------------------------------------

    private static String questFormat(GameTestHelper helper) {
        try {
            return FtbQuestsDefaults.readResource("install.txt").lines().map(String::strip)
                    .filter(line -> line.startsWith("format ")).map(line -> line.substring("format ".length()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("install.txt names no format"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("the FTB Quests book is not on the classpath", e);
        }
    }

    private static List<String> shippedQuestFiles(GameTestHelper helper) {
        try {
            return FtbQuestsDefaults.shippedFiles();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("the FTB Quests book is not on the classpath", e);
        }
    }

    private static JsonObject parseQuestFile(GameTestHelper helper, String format, String file) {
        try {
            return parseQuestText(format, FtbQuestsDefaults.readResource(file));
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalStateException("the shipped quest file " + file + " does not parse: " + e.getMessage(), e);
        }
    }

    /** JSON5 files are written as strict JSON; SNBT is read by vanilla's parser and turned into JSON. */
    private static JsonObject parseQuestText(String format, String text) {
        if (format.equals("json5")) {
            return JsonParser.parseString(text).getAsJsonObject();
        }
        try {
            return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, TagParser.parseCompoundFully(text)).getAsJsonObject();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void checkId(String id, String what, List<String> problems) {
        if (id == null || !id.matches("[0-9A-F]{16}") || Long.parseUnsignedLong(id, 16) < 2 || id.charAt(0) > '7') {
            problems.add(what + " has the malformed id " + id);
        }
    }

    /** The FTB translation entry {@code key} exists and every text of it is a translate component with a known key. */
    private static void checkTranslated(String key, java.util.Map<String, JsonElement> lang, JsonObject english, JsonObject german,
                                        List<String> problems) {
        JsonElement value = lang.get(key);
        if (value == null) {
            problems.add("no text for " + key);
            return;
        }
        List<String> texts = new ArrayList<>();
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(e -> texts.add(e.getAsString()));
        } else {
            texts.add(value.getAsString());
        }
        if (texts.isEmpty()) {
            problems.add("empty text for " + key);
        }
        for (String text : texts) {
            String translate;
            try {
                translate = JsonParser.parseString(text).getAsJsonObject().get("translate").getAsString();
            } catch (RuntimeException e) {
                problems.add(key + " is no translate component: " + text);
                continue;
            }
            if (translate.startsWith("quests." + MOD_ID + ".") || translate.startsWith("advancements." + MOD_ID + ".")) {
                if (!english.has(translate) || !german.has(translate)) {
                    problems.add(key + " uses " + translate + ", which is missing in en_us.json or de_de.json");
                }
            } else if (!Language.getInstance().has(translate)) {
                problems.add(key + " uses the unknown vanilla key " + translate);
            }
        }
    }

    private static boolean isTrue(JsonElement element) {
        return element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : element.getAsInt() != 0;
    }

    private static Set<String> ancestors(GameTestHelper helper, java.util.Map<String, List<String>> deps, String start) {
        Set<String> seen = new HashSet<>();
        java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<>(deps.getOrDefault(start, List.of()));
        while (!stack.isEmpty()) {
            String node = stack.pop();
            helper.assertTrue(!node.equals(start), start + " sits in a dependency cycle");
            if (seen.add(node)) {
                stack.addAll(deps.getOrDefault(node, List.of()));
            }
        }
        return seen;
    }

    private static void deleteTree(java.nio.file.Path root) {
        if (root == null) {
            return;
        }
        try (var walk = java.nio.file.Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (java.io.IOException ignored) {
            // a temp folder left behind is harmless
        }
    }

    private static boolean inTree(String path) {
        return !path.startsWith("recipes/") && !path.startsWith("easter/");
    }

    private static List<AdvancementHolder> tree(ServerAdvancementManager manager) {
        List<AdvancementHolder> out = new ArrayList<>();
        for (AdvancementHolder holder : manager.getAllAdvancements()) {
            if (holder.id().getNamespace().equals(MOD_ID) && inTree(holder.id().getPath())) {
                out.add(holder);
            }
        }
        out.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));
        return out;
    }

    /** Walks the parent chain to its top; fails on a missing parent or a cycle. */
    private static String topOf(GameTestHelper helper, ServerAdvancementManager manager, AdvancementHolder start) {
        Set<Identifier> seen = new HashSet<>();
        AdvancementHolder current = start;
        while (current.value().parent().isPresent()) {
            Identifier parentId = current.value().parent().get();
            helper.assertTrue(seen.add(parentId), start.id() + " sits in a parent cycle through " + parentId);
            AdvancementHolder parent = manager.get(parentId);
            helper.assertTrue(parent != null, current.id() + " names the parent " + parentId + ", which is not loaded");
            current = parent;
        }
        return current.id().toString();
    }

    private static boolean waitsFor(AdvancementHolder holder, String feature) {
        return holder.value().criteria().values().stream().anyMatch(criterion ->
                criterion.triggerInstance() instanceof FeatureTrigger.TriggerInstance instance && instance.feature().equals(feature));
    }

    private static AdvancementHolder require(GameTestHelper helper, ServerAdvancementManager manager, String path) {
        AdvancementHolder holder = manager.get(Identifier.fromNamespaceAndPath(MOD_ID, path));
        helper.assertTrue(holder != null, "simplebuilding:" + path + " is not loaded");
        return holder;
    }

    private static boolean done(ServerPlayer player, AdvancementHolder holder) {
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void give(ServerPlayer player, ItemStack stack, int slot) {
        player.getInventory().setItem(9 + slot, stack);
        player.inventoryMenu.broadcastChanges();
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static JsonObject displayJson(GameTestHelper helper, AdvancementHolder holder) {
        Optional<DisplayInfo> display = holder.value().display();
        if (display.isEmpty()) {
            return null;
        }
        return DisplayInfo.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), display.get())
                .getOrThrow().getAsJsonObject();
    }

    private static JsonObject langFile(GameTestHelper helper, String locale) {
        String path = "assets/simplebuilding/lang/" + locale + ".json";
        try (InputStream in = AdvancementTreeTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
