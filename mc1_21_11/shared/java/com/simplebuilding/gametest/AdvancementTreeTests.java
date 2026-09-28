package com.simplebuilding.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.advancement.FeatureTrigger;
import com.simplebuilding.advancement.ModTriggers;
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

    /**
     * Every advancement file of the tree loads, and every loaded one is complete: it has a display
     * that is not hidden, a translated title and description in English and German, an icon that is
     * a registered item, and a parent chain without cycles that ends in {@code simplebuilding:root}
     * (the only advancement without a parent) or, for the teasers, in one of the two allowed vanilla
     * advancements. Every feature name a criterion waits for is one {@link ModTriggers} knows, and
     * every feature {@link ModTriggers} reports is awaited by at least one advancement.
     *
     * <p>What breaks this test: a JSON that no longer parses (a renamed trigger or item, a trigger
     * type that was not registered on this loader), a missing lang key in either language, a hidden
     * flag, a second root, a parent that does not exist, a new feature nobody listens to.
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
        helper.assertTrue(files.size() >= 50, "only " + files.size() + " advancement files of the tree are on the data path");

        List<AdvancementHolder> tree = tree(manager);
        Set<String> loaded = new TreeSet<>();
        tree.forEach(holder -> loaded.add(holder.id().toString()));
        Set<String> notLoaded = new TreeSet<>(files);
        notLoaded.removeAll(loaded);
        helper.assertTrue(notLoaded.isEmpty(), "advancement files that did not load: " + notLoaded);

        JsonObject english = langFile(helper, "en_us");
        JsonObject german = langFile(helper, "de_de");
        Set<String> awaited = new HashSet<>();
        int roots = 0;
        for (AdvancementHolder holder : tree) {
            String id = holder.id().toString();
            JsonObject display = displayJson(helper, holder);
            helper.assertTrue(display != null, id + " has no display");
            helper.assertTrue(!display.has("hidden") || !display.get("hidden").getAsBoolean(),
                    id + " is hidden - only the easter chain may be");
            for (String part : List.of("title", "description")) {
                String key = display.getAsJsonObject(part).get("translate").getAsString();
                helper.assertTrue(key.equals("advancements." + MOD_ID + "." + holder.id().getPath().replace('/', '.') + "." + part),
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
                helper.assertTrue(id.equals(MOD_ID + ":root"), id + " has no parent but is not simplebuilding:root");
                helper.assertTrue(display.has("background"), "the SimpleBuilding tab has no background");
            } else {
                String top = topOf(helper, manager, holder);
                helper.assertTrue(top.equals(MOD_ID + ":root") || top.equals("minecraft:story/root") || top.equals("minecraft:nether/root"),
                        id + " does not lead to the SimpleBuilding tab or an allowed vanilla tab but to " + top);
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
        Assertions.valueEqual(helper, roots, 1, "advancements of the tree without a parent");
        List<String> unused = new ArrayList<>(ModTriggers.ALL);
        unused.removeAll(awaited);
        helper.assertTrue(unused.isEmpty(), "features no advancement waits for: " + unused);
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
    }

    /**
     * The item advancements follow the inventory: a crafting table opens the tab, any sledgehammer
     * earns the "Hammer Time" teaser under vanilla's "Getting an Upgrade", and "Void Walker" needs
     * all four enderite armor pieces, not just three.
     */
    public static void itemAdvancementsFollowTheInventory(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        ServerPlayer player = mockPlayer(helper);
        AdvancementHolder root = require(helper, manager, "root");
        AdvancementHolder hammerTime = require(helper, manager, "story/hammer_time");
        AdvancementHolder voidWalker = require(helper, manager, "enderite/void_walker");
        Assertions.valueEqual(helper, hammerTime.value().parent(), Optional.of(Identifier.withDefaultNamespace("story/upgrade_tools")),
                "parent of the Hammer Time teaser");

        helper.assertTrue(!done(player, root), "the tab root was done before a crafting table was in the inventory");
        give(player, new ItemStack(Items.CRAFTING_TABLE), 0);
        helper.assertTrue(done(player, root), "a crafting table did not open the SimpleBuilding tab");

        give(player, new ItemStack(ModItems.GOLD_SLEDGEHAMMER), 1);
        helper.assertTrue(done(player, hammerTime), "a gold sledgehammer did not earn Hammer Time");

        give(player, new ItemStack(ModItems.ENDERITE_HELMET), 2);
        give(player, new ItemStack(ModItems.ENDERITE_CHESTPLATE), 3);
        give(player, new ItemStack(ModItems.ENDERITE_LEGGINGS), 4);
        helper.assertTrue(!done(player, voidWalker), "three enderite armor pieces already earned Void Walker");
        give(player, new ItemStack(ModItems.ENDERITE_BOOTS), 5);
        helper.assertTrue(done(player, voidWalker), "the full enderite set did not earn Void Walker");
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
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
        TestCleanup.succeed(helper);
    }

    // -------------------------------------------------------------------------------------

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
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
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
