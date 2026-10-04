package com.simplebuilding.modules.simplemodels;
import com.google.gson.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public final class ModelTests {
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("launch", ModelTests::launch); ALL.put("assignment", ModelTests::assignment);
        ALL.put("permissions", ModelTests::permissions); ALL.put("request_bounds", ModelTests::requestBounds);
        ALL.put("config_bounds", ModelTests::configBounds); ALL.put("definition_bounds", ModelTests::definitionBounds);
        ALL.put("folder_scan", ModelTests::folderScan); ALL.put("reload_fail_closed", ModelTests::reloadFailClosed);
        ALL.put("catalogue_sync", ModelTests::catalogueSync); ALL.put("search", ModelTests::search);
        ALL.put("anvil_cost_and_take", ModelTests::anvilCostAndTake); ALL.put("stale_result", ModelTests::staleResult);
        ALL.put("normal_rename", ModelTests::normalRename); ALL.put("cross_mod", ModelTests::crossMod);
        ALL.put("legacy_names", ModelTests::legacyNames); ALL.put("config_and_lang", ModelTests::configAndLang);
    }
    public static ModelDefinition sample(String base) { return new ModelDefinition("renamed:tomato", base, "Tomato", "renamed:tomato", List.of("food", "red"), "Example"); }
    private static void fixture(String base, Runnable body) {
        var original = Models.server; var policy = new ModelPolicy(); policy.operatorsOnly = false;
        Models.server = new ModelCatalogue.Snapshot(policy, List.of(sample(base)));
        try { body.run(); } finally { Models.server = original; }
    }
    private static void rejects(Runnable body) {
        try { body.run(); } catch (IllegalArgumentException e) { return; }
        throw new AssertionError("Unsafe definition was accepted");
    }
    private static void ioRejects(Path file, int cap) {
        try { ModelCatalogue.read(file, cap); } catch (java.io.IOException e) { return; }
        throw new AssertionError("Unsafe file was accepted");
    }
    public static void launch(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getNamespace().equals("simplemodels")), "No scaffold token or invented registry items");
        if (isModLoaded("simplebuilding")) h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:diamond_building_wand")), "SimpleBuilding content visible when loaded");
        h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("simplemodels") != null, "Admin reload command registered");
        h.assertTrue(CataloguePayload.current().json().length() <= ModelCatalogue.MAX_SNAPSHOT_CHARS, "Bounded snapshot");
        h.succeed();
    }
    public static void assignment(GameTestHelper h) {
        fixture("minecraft:diamond_sword", () -> {
            var in = new ItemStack(Items.DIAMOND_SWORD); in.setDamageValue(23); in.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("My Sword"));
            var out = Models.assign(in, "@model:renamed:tomato", false);
            h.assertTrue(out.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("renamed:tomato")), "Approved item_model");
            var expected = in.copy(); expected.set(DataComponents.ITEM_MODEL, Identifier.parse("renamed:tomato"));
            h.assertTrue(ItemStack.matches(out, expected), "Only item_model changed, preserving all other components");
            h.assertTrue(!Objects.equals(in.get(DataComponents.ITEM_MODEL), out.get(DataComponents.ITEM_MODEL)), "Input was not mutated");
            h.assertTrue(Models.assign(out, "@model:renamed:tomato", false).isEmpty(), "Repeated assignment is a no-op");
            h.assertTrue(ItemStack.matches(Models.assign(out, Models.RESET, false), in), "Reset restores base rendering and preserves custom name/damage");
        }); h.succeed();
    }
    public static void permissions(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            var p = Models.server.policy(); var in = new ItemStack(Items.APPLE);
            p.operatorsOnly = true;
            h.assertTrue(Models.assign(in, "@model:renamed:tomato", false).isEmpty(), "Unprivileged request refused");
            var out = Models.assign(in, "@model:renamed:tomato", true); h.assertTrue(!out.isEmpty(), "Operator allowed");
            h.assertTrue(Models.assign(out, Models.RESET, false).isEmpty(), "Removal also requires permission");
            p.enabled = false;
            h.assertTrue(Models.assign(in, "@model:renamed:tomato", true).isEmpty(), "Disabled server refuses operator");
            h.assertTrue(Models.assign(out, Models.RESET, true).isEmpty(), "Disabled server refuses removal");
        }); h.succeed();
    }
    public static void requestBounds(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            for (String name : List.of("@model:minecraft:diamond", "@model:../tomato", "@model:renamed:tomato[uuid]", "@model:" + "x".repeat(4096)))
                h.assertTrue(Models.assign(new ItemStack(Items.APPLE), name, true).isEmpty(), "Forged model refused: " + name.substring(0, Math.min(32, name.length())));
            h.assertTrue(Models.assign(new ItemStack(Items.DIAMOND), "@model:renamed:tomato", true).isEmpty(), "Wrong base item refused");
            var player = h.makeMockServerPlayerInLevel();
            var menu = new AnvilMenu(1, player.getInventory()); player.containerMenu = menu;
            menu.getSlot(0).set(new ItemStack(Items.APPLE));
            for (String request : List.of("@model:minecraft:diamond", "@model:renamed:tomato[uuid]", "@model:" + "x".repeat(4096))) {
                player.connection.handleRenameItem(new net.minecraft.network.protocol.game.ServerboundRenameItemPacket(request));
                h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Actual forged rename packet refused");
            }
        }); h.succeed();
    }
    public static void configBounds(GameTestHelper h) {
        var p = new ModelPolicy(); p.levelCost = Integer.MAX_VALUE; p.maxModels = Integer.MAX_VALUE; p.maxFileBytes = Integer.MAX_VALUE; p.clamp();
        h.assertTrue(p.levelCost == 10 && p.maxModels == 64 && p.maxFileBytes == 16384, "Hard upper bounds");
        p.levelCost = p.maxModels = p.maxFileBytes = Integer.MIN_VALUE; p.clamp();
        h.assertTrue(p.levelCost == 1 && p.maxModels == 1 && p.maxFileBytes == 256, "Lower bounds prevent free changes/negative allocation"); h.succeed();
    }
    public static void definitionBounds(GameTestHelper h) {
        var good = ModelCatalogue.GSON.toJsonTree(sample("minecraft:apple")).getAsJsonObject();
        h.assertTrue(ModelDefinition.parse(good).equals(sample("minecraft:apple")), "Original CIT fields parse");
        for (String bad : List.of("../bad", "renamed:../bad", "renamed:/absolute", "C:\\private", "renamed:x".repeat(100)))
            rejects(() -> ModelDefinition.safeId(bad));
        for (var pair : List.of(Map.entry("id", "x".repeat(41)), Map.entry("base_item", "absent:no_item"), Map.entry("base_item", "minecraft:air"), Map.entry("match_name", "x".repeat(49)), Map.entry("author", "x".repeat(33)), Map.entry("model", "../unsafe"))) {
            var json = good.deepCopy(); json.addProperty(pair.getKey(), pair.getValue()); rejects(() -> ModelDefinition.parse(json));
        }
        var json = good.deepCopy(); json.add("tags", JsonParser.parseString("[\"" + "x".repeat(25) + "\"]")); rejects(() -> ModelDefinition.parse(json));
        h.succeed();
    }
    public static void folderScan(GameTestHelper h) {
        try {
            Path root = Files.createTempDirectory("simplemodels-scan-"); ModelCatalogue.load(root); Path folder = root.resolve("catalogue");
            Files.writeString(folder.resolve("a.json"), ModelCatalogue.GSON.toJson(sample("minecraft:apple")));
            Files.writeString(folder.resolve("b.json"), ModelCatalogue.GSON.toJson(sample("minecraft:apple")));
            Files.writeString(folder.resolve("bad.json"), "not json");
            Files.writeString(folder.resolve("oversized.json"), "x".repeat(20000));
            Files.createDirectory(folder.resolve("directory.json"));
            Files.writeString(folder.resolve("deep.json"), "[".repeat(200) + "]".repeat(200));
            h.assertTrue(ModelCatalogue.load(root).models().size() == 1, "Duplicate/malformed/oversized/directory/deep JSON rejected without crashing");
            ioRejects(folder.resolve("deep.json"), 16384); ioRejects(folder.resolve("directory.json"), 16384); ioRejects(folder.resolve("oversized.json"), 16384);
            h.assertTrue(ModelCatalogue.load(root).models().getFirst().matches("FOOD"), "Case-insensitive tags");
            Path overflow = Files.createTempDirectory("simplemodels-overflow-");
            ModelCatalogue.load(overflow);
            for (int i = 0; i < 257; i++) Files.writeString(overflow.resolve("catalogue/" + i + ".json"), "{}");
            boolean refused = false;
            try { ModelCatalogue.load(overflow); } catch (java.io.IOException expected) { refused = true; }
            h.assertTrue(refused, "Too many files refuse the catalogue instead of selecting filesystem-dependent entries");
        } catch (java.io.IOException e) { throw new AssertionError(e); } h.succeed();
    }
    public static void reloadFailClosed(GameTestHelper h) {
        try {
            var root = Files.createTempDirectory("simplemodels-policy-"); ModelCatalogue.load(root);
            Files.writeString(root.resolve("config.json"), "{malformed");
            h.assertTrue(!ModelCatalogue.load(root).policy().enabled, "Bad config disables assignment");
            Files.writeString(root.resolve("config.json"), "null");
            h.assertTrue(!ModelCatalogue.load(root).policy().enabled, "Null config disables assignment");
            Files.writeString(root.resolve("config.json"), "{\"levelCost\":500,\"maxModels\":-1}");
            var snapshot = ModelCatalogue.load(root); h.assertTrue(snapshot.policy().levelCost == 10 && snapshot.policy().maxModels == 1, "Loaded config clamped");
            Files.writeString(root.resolve("catalogue/one.json"), ModelCatalogue.GSON.toJson(sample("minecraft:apple")));
            h.assertTrue(ModelCatalogue.load(root).models().size() == 1, "Folder addition visible after reload");
            Files.move(root.resolve("catalogue/one.json"), root.resolve("one.removed"));
            h.assertTrue(ModelCatalogue.load(root).models().isEmpty(), "Folder removal visible after reload");
        } catch (java.io.IOException e) { throw new AssertionError(e); } h.succeed();
    }
    public static void catalogueSync(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            var packet = CataloguePayload.current(); var decoded = ModelCatalogue.decode(packet.json());
            h.assertTrue(decoded.models().equals(Models.server.models()), "Server definitions round-trip without loader state");
            h.assertTrue(decoded.policy().levelCost == 1, "Server cost sync");
            rejects(() -> ModelCatalogue.decode("x".repeat(28001)));
            rejects(() -> ModelCatalogue.decode("[".repeat(200) + "]".repeat(200)));
        }); h.succeed();
    }
    public static void search(GameTestHelper h) {
        var d = sample("minecraft:apple");
        for (String q : List.of("TOMATO", "apple", "renamed:", "FOOD", "red", "example", "")) h.assertTrue(d.matches(q), "Search matches " + q);
        h.assertTrue(!d.matches("nonexistent"), "Search excludes nonmatches"); h.succeed();
    }
    public static void anvilCostAndTake(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setExperienceLevels(5);
            var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
            menu.getSlot(0).set(new ItemStack(Items.APPLE, 3)); menu.setItemName("@model:renamed:tomato");
            h.assertTrue(menu.getCost() == 1 && menu.getSlot(2).mayPickup(player), "Vanilla cost and pickup permission");
            var out = menu.getSlot(2).remove(3); menu.getSlot(2).onTake(player, out);
            h.assertTrue(out.getCount() == 3 && out.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("renamed:tomato")), "Exact stack retained");
            h.assertTrue(menu.getSlot(0).getItem().isEmpty() && player.experienceLevel == 4, "Input consumed once and XP charged once");
            h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "No replay/duplication output");
            var poor = new AnvilMenu(2, player.getInventory()); poor.getSlot(0).set(new ItemStack(Items.APPLE)); poor.setItemName("@model:renamed:tomato");
            player.setExperienceLevels(0); h.assertTrue(!poor.getSlot(2).mayPickup(player), "Insufficient XP refused");
            poor.getSlot(1).set(new ItemStack(Items.DIAMOND)); h.assertTrue(poor.getSlot(2).getItem().isEmpty(), "Second slot must be empty");
        }); h.succeed();
    }
    public static void staleResult(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var menu = new AnvilMenu(1, player.getInventory()); menu.getSlot(0).set(new ItemStack(Items.APPLE)); menu.setItemName("@model:renamed:tomato");
            h.assertTrue(menu.getSlot(2).mayPickup(player), "Initial approved output");
            Models.server.policy().enabled = false; h.assertTrue(!menu.getSlot(2).mayPickup(player), "Disabling server policy revokes stale output");
            Models.server.policy().enabled = true; Models.server.policy().levelCost = 2;
            h.assertTrue(!menu.getSlot(2).mayPickup(player), "Cost reload cannot preserve old cheap result");
            Models.server = new ModelCatalogue.Snapshot(Models.server.policy(), List.of());
            h.assertTrue(!menu.getSlot(2).mayPickup(player), "Catalogue removal revokes stale output");
        }); h.succeed();
    }
    public static void normalRename(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel(); var menu = new AnvilMenu(1, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.APPLE)); menu.setItemName("Lunch[my-id]");
        h.assertTrue(menu.getSlot(2).getItem().getHoverName().getString().equals("Lunch[my-id]"), "Vanilla names and brackets stay intact"); h.succeed();
    }
    public static void crossMod(GameTestHelper h) {
        if (partnerMissing(h, "simplebuilding", "mod item protection with a SimpleBuilding wand")) return;
        String base = "simplebuilding:diamond_building_wand";
        fixture(base, () -> {
            var input = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(base)));
            h.assertTrue(Models.assign(input, "@model:renamed:tomato", true).isEmpty(), "Mod items protected by default");
            Models.server.policy().allowModItems = true;
            var out = Models.assign(input, "@model:renamed:tomato", true); h.assertTrue(!out.isEmpty(), "Explicit approved mod item opt-in");
            var expected = input.copy(); expected.set(DataComponents.ITEM_MODEL, Identifier.parse("renamed:tomato"));
            h.assertTrue(ItemStack.matches(expected, out), "Foreign gameplay components intact");
            var player = h.makeMockServerPlayerInLevel(); var menu = new AnvilMenu(1, player.getInventory());
            menu.getSlot(0).set(input); menu.setItemName("Owner Tool");
            h.assertTrue(!menu.getSlot(2).getItem().isEmpty(), "SimpleBuilding normal anvil path still works");
        }); h.succeed();
    }
    public static void legacyNames(GameTestHelper h) {
        fixture("minecraft:apple", () -> {
            var input = new ItemStack(Items.APPLE);
            h.assertTrue(Models.assign(input, "Tomato", true).isEmpty(), "Legacy disabled by default");
            Models.server.policy().legacyNameMatching = true;
            h.assertTrue(!Models.assign(input, "Tomato", true).isEmpty(), "Exact legacy name allowed by policy");
            h.assertTrue(Models.assign(input, "Tomato[hidden]", true).isEmpty(), "Unsafe legacy suffix never accepted");
            var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var menu = new AnvilMenu(1, player.getInventory()); menu.getSlot(0).set(input); menu.setItemName("Tomato");
            h.assertTrue(menu.getSlot(2).mayPickup(player), "Legacy approved result initially available");
            Models.server.policy().legacyNameMatching = false;
            h.assertTrue(!menu.getSlot(2).mayPickup(player), "Turning off legacy matching revokes already computed result");
        }); h.succeed();
    }
    public static void configAndLang(GameTestHelper h) {
        try {
            Map<String, String> en, de;
            try (var a = ModelTests.class.getResourceAsStream("/assets/simplemodels/lang/en_us.json"); var b = ModelTests.class.getResourceAsStream("/assets/simplemodels/lang/de_de.json")) {
                var type = new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType();
                en = ModelCatalogue.GSON.fromJson(new java.io.InputStreamReader(a, java.nio.charset.StandardCharsets.UTF_8), type);
                de = ModelCatalogue.GSON.fromJson(new java.io.InputStreamReader(b, java.nio.charset.StandardCharsets.UTF_8), type);
            }
            h.assertTrue(en.keySet().equals(de.keySet()), "EN/DE complete"); var defaults = new ModelPolicy();
            for (var f : ModelPolicy.class.getFields()) if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                String key = "simplemodels.config." + f.getName();
                for (var lang : List.of(en, de)) for (String suffix : List.of("", ".tooltip", ".tab")) h.assertTrue(lang.containsKey(key + suffix), "Config metadata " + key + suffix);
                h.assertTrue(en.get(key + ".tooltip").contains("Default: " + f.get(defaults)) && de.get(key + ".tooltip").contains("Standard: " + f.get(defaults)), "Default in every tooltip");
            }
        } catch (Exception e) { throw new AssertionError(e); } h.succeed();
    }
    private ModelTests() {}
}
