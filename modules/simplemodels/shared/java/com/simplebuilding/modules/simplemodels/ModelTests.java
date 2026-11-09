package com.simplebuilding.modules.simplemodels;
import static com.simplelib.test.PartnerChecks.isModLoaded;
import static com.simplelib.test.PartnerChecks.partnerMissing;
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
import com.simplebuilding.modules.simplemodels.editor.EditHistory;
import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.ModelJson;

public final class ModelTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("guide_book", com.simplebuilding.modules.simplemodels.guide.ModelsGuide::gameTest);
        ALL.put("sub_mod_switch", ModelTests::subModSwitch); ALL.put("launch", ModelTests::launch); ALL.put("assignment", ModelTests::assignment);
        ALL.put("permissions", ModelTests::permissions); ALL.put("request_bounds", ModelTests::requestBounds);
        ALL.put("config_bounds", ModelTests::configBounds); ALL.put("definition_bounds", ModelTests::definitionBounds);
        ALL.put("folder_scan", ModelTests::folderScan); ALL.put("reload_fail_closed", ModelTests::reloadFailClosed);
        ALL.put("catalogue_sync", ModelTests::catalogueSync); ALL.put("search", ModelTests::search);
        ALL.put("anvil_cost_and_take", ModelTests::anvilCostAndTake); ALL.put("stale_result", ModelTests::staleResult);
        ALL.put("normal_rename", ModelTests::normalRename); ALL.put("cross_mod", ModelTests::crossMod);
        ALL.put("legacy_names", ModelTests::legacyNames); ALL.put("config_and_lang", ModelTests::configAndLang);
        ALL.put("browser_query", ModelTests::browserQuery);
        ALL.put("examples", ModelTests::examples);
        ALL.put("editable_round_trip", ModelTests::editableRoundTrip); ALL.put("editable_bounds", ModelTests::editableBounds);
        ALL.put("edit_history", ModelTests::editHistory);
        ALL.put("editor_layout", EditorTests::layout); ALL.put("editor_geometry", EditorTests::geometry);
        ALL.put("editor_gizmo", EditorTests::gizmo); ALL.put("editor_state", EditorTests::state);
        ALL.put("editor_snap_help", EditorTests::snapHelp);
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
    /** Super-mod switch off: registry (guide book, command) stays, anvil requests are not intercepted and assign refuses. */
    public static void subModSwitch(GameTestHelper h) {
        var sw = com.simplebuilding.framework.api.SubMods.enabled("simplemodels");
        try {
            com.simplebuilding.framework.api.SubMods.set("simplemodels", false);
            h.assertTrue(!Models.on(), "Switch off reaches Simple Models");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplemodels:guide_book")), "Registry stays");
            h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("simplemodels") != null, "Command stays");
            h.assertTrue(!Models.request(Models.PREFIX + "renamed:tomato") && !Models.request(Models.RESET), "Anvil request not intercepted while off");
            fixture("minecraft:diamond_sword", () -> h.assertTrue(Models.assign(new ItemStack(Items.DIAMOND_SWORD), "@model:renamed:tomato", true).isEmpty(), "No assignment while off"));
        } finally { com.simplebuilding.framework.api.SubMods.set("simplemodels", sw); }
        h.assertTrue(Models.on() == sw, "Switch restored");
        h.assertTrue(Models.request(Models.RESET) == sw, "Anvil request works again once restored");
        h.succeed();
    }
    public static void launch(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getNamespace().equals("simplemodels") && !id.getPath().equals("guide_book")), "No scaffold token or invented registry items (only the guide)");
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
            Path root = Files.createTempDirectory("simplemodels-scan-"); ModelCatalogue.load(root); Path folder = root.resolve("catalog");
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
            for (int i = 0; i < 257; i++) Files.writeString(overflow.resolve("catalog/" + i + ".json"), "{}");
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
            Files.writeString(root.resolve("catalog/one.json"), ModelCatalogue.GSON.toJson(sample("minecraft:apple")));
            h.assertTrue(ModelCatalogue.load(root).models().size() == 1, "Folder addition visible after reload");
            Files.move(root.resolve("catalog/one.json"), root.resolve("one.removed"));
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
    /** SM21 b/c: base-item filter, sort orders, filter choices and wheel paging of the model browser. */
    public static void browserQuery(GameTestHelper h) {
        var tomato = sample("minecraft:apple");
        var wand = new ModelDefinition("renamed:wand", "minecraft:stick", "wand", "minecraft:blaze_rod", List.of("rod"), "B");
        var axe = new ModelDefinition("renamed:axe", "minecraft:stick", "Axe", "minecraft:iron_axe", List.of(), "C");
        var all = List.of(tomato, wand, axe);
        var names = (java.util.function.Function<List<ModelDefinition>, List<String>>)l -> l.stream().map(ModelDefinition::match_name).toList();
        h.assertTrue(names.apply(ModelQuery.view(all, "", ModelQuery.ALL, ModelQuery.Sort.NAME_ASC)).equals(List.of("Axe", "Tomato", "wand")), "Name A-Z ignores case");
        h.assertTrue(names.apply(ModelQuery.view(all, "", ModelQuery.ALL, ModelQuery.Sort.NAME_DESC)).equals(List.of("wand", "Tomato", "Axe")), "Name Z-A");
        h.assertTrue(names.apply(ModelQuery.view(all, "", ModelQuery.ALL, ModelQuery.Sort.BASE_ITEM)).equals(List.of("Tomato", "Axe", "wand")), "Base item, then name");
        h.assertTrue(names.apply(ModelQuery.view(all, "", "minecraft:stick", ModelQuery.Sort.NAME_ASC)).equals(List.of("Axe", "wand")), "Base-item filter");
        h.assertTrue(names.apply(ModelQuery.view(all, "rod", "minecraft:stick", ModelQuery.Sort.NAME_ASC)).equals(List.of("wand")), "Filter and search combine");
        h.assertTrue(ModelQuery.view(all, "", "minecraft:bread", ModelQuery.Sort.NAME_ASC).isEmpty(), "Unknown filter lists nothing");
        h.assertTrue(ModelQuery.baseItems(all).equals(List.of(ModelQuery.ALL, "minecraft:apple", "minecraft:stick")), "Filter choices: all, then each base item once");
        h.assertTrue(ModelQuery.pages(0, 3) == 1 && ModelQuery.pages(3, 3) == 1 && ModelQuery.pages(7, 3) == 3, "Page count");
        h.assertTrue(ModelQuery.scroll(0, -1, 3) == 1 && ModelQuery.scroll(1, 1, 3) == 0, "Wheel down = next, up = previous");
        h.assertTrue(ModelQuery.scroll(0, 1, 3) == 0 && ModelQuery.scroll(2, -2.5, 3) == 2 && ModelQuery.scroll(1, 0, 3) == 1, "Wheel clamps at the ends");
        h.succeed();
    }
    /** b-models12 stage 0: six built-in example models load, render data ships in the jar, each one assigns at the anvil. */
    public static void examples(GameTestHelper h) {
        var examples = ModelExamples.all();
        h.assertTrue(examples.size() == 6 && examples.stream().map(ModelDefinition::id).distinct().count() == 6, "Six distinct built-in examples");
        for (var d : examples) {
            String name = Identifier.parse(d.id()).getPath();
            h.assertTrue(d.base_item().startsWith("minecraft:") && d.model().equals("simplemodels:example/" + name), "Vanilla base item and own model id: " + name);
            try {
                var item = JsonParser.parseString(ModelExamples.resource("items/example/" + name + ".json")).getAsJsonObject();
                h.assertTrue(item.getAsJsonObject("model").get("model").getAsString().equals("simplemodels:item/example/" + name), "Item definition points at the model: " + name);
                var model = JsonParser.parseString(ModelExamples.resource("models/item/example/" + name + ".json")).getAsJsonObject();
                for (var texture : model.getAsJsonObject("textures").entrySet())
                    h.assertTrue(texture.getValue().getAsString().startsWith("minecraft:block/"), "Only vanilla texture references (A8): " + name);
                for (var element : model.getAsJsonArray("elements"))
                    for (String key : List.of("from", "to")) for (var v : element.getAsJsonObject().getAsJsonArray(key))
                        h.assertTrue(v.getAsFloat() >= -16 && v.getAsFloat() <= 32, "Vanilla element bounds: " + name);
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            var policy = new ModelPolicy(); policy.operatorsOnly = false;
            var original = Models.server;
            try {
                Models.server = ModelExamples.merge(new ModelCatalogue.Snapshot(policy, List.of()));
                var base = BuiltInRegistries.ITEM.getValue(Identifier.parse(d.base_item()));
                var out = Models.assign(new ItemStack(base), Models.PREFIX + d.id(), false);
                h.assertTrue(Identifier.parse(d.model()).equals(out.get(DataComponents.ITEM_MODEL)), "Anvil assigns example " + name);
            } finally { Models.server = original; }
        }
        var policy = new ModelPolicy();
        var tomato = sample("minecraft:apple");
        var merged = ModelExamples.merge(new ModelCatalogue.Snapshot(policy, List.of(tomato)));
        h.assertTrue(merged.models().size() == 7 && merged.models().getFirst().equals(tomato), "Folder entries first, examples appended");
        var own = new ModelDefinition("simplemodels:stool", "minecraft:oak_slab", "My Stool", "renamed:stool", List.of(), "Me");
        h.assertTrue(ModelExamples.merge(new ModelCatalogue.Snapshot(policy, List.of(own))).models().stream().filter(d -> d.id().equals("simplemodels:stool")).toList().equals(List.of(own)), "Folder entry with the same id wins");
        policy.maxModels = 3;
        h.assertTrue(ModelExamples.merge(new ModelCatalogue.Snapshot(policy, List.of(tomato))).models().size() == 3, "Catalog limit still caps");
        var off = new ModelPolicy(); off.builtinExamples = false;
        h.assertTrue(ModelExamples.merge(new ModelCatalogue.Snapshot(off, List.of(tomato))).models().equals(List.of(tomato)), "Switch off: no examples");
        h.assertTrue(new Gson().toJson(merged).length() <= ModelCatalogue.MAX_SNAPSHOT_CHARS, "Examples fit the catalogue sync");
        h.succeed();
    }
    /** b-models12 stage 1: vanilla JSON -> EditableModel -> JSON keeps every field (single axis and Euler rotation), examples included. */
    public static void editableRoundTrip(GameTestHelper h) {
        String full = """
            {"parent":"minecraft:block/block","ambientocclusion":false,"gui_light":"front",
             "textures":{"particle":"#side","side":"minecraft:block/oak_planks"},
             "elements":[
              {"name":"a","from":[-16,0,0.5],"to":[32,16,16],"rotation":{"origin":[8,8,8],"axis":"y","angle":22.5,"rescale":true},
               "shade_direction_override":"up","light_emission":15,
               "faces":{"east":{"uv":[16,0,0,16],"texture":"#side","rotation":270,"cullface":"west","tintindex":2},"down":{"texture":"#side"}}},
              {"from":[0,0,0],"to":[1,1,1],"rotation":{"origin":[0,0,0],"x":10,"y":-37.5,"z":90},"faces":{"up":{"texture":"minecraft:block/stone"}}}],
             "display":{"gui":{"rotation":[30,225,0],"translation":[0,-80,1.5],"scale":[0.625,0.625,0.625],"right_rotation":[0,0,5]},"head":{}}}
            """;
        var read = ModelJson.read(full);
        h.assertTrue(read.ok(), "Full vanilla model reads: " + read.problems());
        var m = read.model();
        var a = m.elements().get(0);
        h.assertTrue(a.name().equals("a") && a.from().equals(new EditableModel.Vec(-16, 0, 0.5f)) && a.lightEmission() == 15 && "up".equals(a.shadeDirection()), "Element fields");
        h.assertTrue(!a.rotation().isEuler() && a.rotation().axis().equals("y") && a.rotation().angle() == 22.5f && a.rotation().rescale(), "Single-axis rotation");
        h.assertTrue(a.faces().keySet().stream().toList().equals(List.of("down", "east")), "Faces in vanilla direction order");
        var east = a.faces().get("east");
        h.assertTrue(east.uv().equals(new EditableModel.Uv(16, 0, 0, 16)) && east.rotation() == 270 && "west".equals(east.cullface()) && east.tintIndex() == 2, "Face fields (mirrored uv allowed)");
        h.assertTrue(a.faces().get("down").uv() == null && a.faces().get("down").tintIndex() == -1, "Auto-UV and no tint by default");
        var b = m.elements().get(1);
        h.assertTrue(b.name() == null && b.rotation().isEuler() && b.rotation().euler().equals(new EditableModel.Vec(10, -37.5f, 90)), "Euler rotation");
        h.assertTrue(m.display().get("head").equals(EditableModel.Transform.IDENTITY) && m.display().get("gui").translation().y() == -80, "Display transforms");
        h.assertTrue(Boolean.FALSE.equals(m.ambientOcclusion()) && "front".equals(m.guiLight()) && m.textures().get("particle").equals("#side"), "Model fields");
        String written = ModelJson.write(m);
        h.assertTrue(ModelJson.read(written).model().equals(m), "JSON -> model -> JSON -> model is stable");
        h.assertTrue(JsonParser.parseString(written).equals(JsonParser.parseString(full)), "Written JSON equals the input (defaults left out, whole numbers without .0)");
        for (String name : ModelExamples.NAMES) {
            try {
                String json = ModelExamples.resource("models/item/example/" + name + ".json");
                var example = ModelJson.read(json);
                h.assertTrue(example.ok(), "Example reads: " + name + " " + example.problems());
                h.assertTrue(JsonParser.parseString(ModelJson.write(example.model())).equals(JsonParser.parseString(json)), "Example round-trips unchanged: " + name);
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        h.assertTrue(ModelJson.read("{}").model().equals(new EditableModel(null, Map.of(), List.of(), Map.of(), null, null)), "Empty model");
        h.succeed();
    }
    /** b-models12 stage 1: vanilla and editor bounds are refused with a JSON path and a translated message, never half-read. */
    public static void editableBounds(GameTestHelper h) {
        Map<String, String> en;
        try (var in = ModelTests.class.getResourceAsStream("/assets/simplemodels/lang/en_us.json")) {
            en = ModelCatalogue.GSON.fromJson(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        String cube = "{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"up\":{\"texture\":\"#a\"}}}";
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("{\"elements\":[{\"from\":[-16.5,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].from out_of_range");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,32.5,1],\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].to out_of_range");
        cases.put("{\"elements\":[{\"from\":[0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].from bad_count");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{}}]}", "elements[0].faces no_faces");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"top\":{\"texture\":\"#a\"}}}]}", "elements[0].faces.top bad_direction");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"light_emission\":16,\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].light_emission bad_light");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\",\"uv\":[0,0,17,16]}}}]}", "elements[0].faces.up.uv[2] out_of_range");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\",\"rotation\":45}}}]}", "elements[0].faces.up.rotation bad_face_rotation");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\",\"tintindex\":-2}}}]}", "elements[0].faces.up.tintindex bad_tint");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"../x\"}}}]}", "elements[0].faces.up.texture bad_texture");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"faces\":{\"up\":{}}}]}", "elements[0].faces.up.texture missing");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"rotation\":{\"origin\":[8,8,8],\"axis\":\"w\",\"angle\":5},\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].rotation.axis bad_axis");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"rotation\":{\"origin\":[8,8,8],\"axis\":\"x\",\"angle\":5,\"y\":3},\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].rotation rotation_mixed");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"rotation\":{\"origin\":[8,8,8]},\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].rotation rotation_missing");
        cases.put("{\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,1],\"shade_direction_override\":\"sideways\",\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].shade_direction_override bad_direction");
        cases.put("{\"elements\":[{\"from\":[0,\"x\",0],\"to\":[1,1,1],\"faces\":{\"up\":{\"texture\":\"#a\"}}}]}", "elements[0].from[1] not_number");
        cases.put("{\"display\":{\"gui\":{\"translation\":[0,81,0]}}}", "display.gui.translation out_of_range");
        cases.put("{\"display\":{\"gui\":{\"scale\":[4.5,1,1]}}}", "display.gui.scale out_of_range");
        cases.put("{\"display\":{\"hat\":{}}}", "display.hat bad_display_slot");
        cases.put("{\"gui_light\":\"top\"}", "gui_light bad_gui_light");
        cases.put("{\"textures\":{\"bad var\":\"minecraft:block/stone\"}}", "textures.bad var bad_variable");
        cases.put("{\"parent\":\"Bad Parent\"}", "parent bad_id");
        cases.put("{\"elements\":[" + String.join(",", java.util.Collections.nCopies(257, cube)) + "]}", "elements too_many_elements");
        cases.put("{\"elements\":" + "[".repeat(20) + "]".repeat(20) + "}", " too_deep");
        cases.put("{\"a\":\"" + "x".repeat(ModelJson.MAX_CHARS) + "\"}", " too_long");
        cases.put("{\"elements\":[", " syntax");
        cases.put("[]", " not_object");
        for (var c : cases.entrySet()) {
            var result = ModelJson.read(c.getKey());
            h.assertTrue(!result.ok() && result.model() == null, "Refused, nothing half-read: " + c.getValue());
            var first = result.problems().getFirst();
            h.assertTrue((first.path() + " " + first.key()).equals(c.getValue()), "Problem " + c.getValue() + " got " + first);
            for (var p : result.problems()) h.assertTrue(en.containsKey("simplemodels.editor.problem." + p.key()), "Translated problem text: " + p.key());
        }
        h.assertTrue(ModelJson.read("{\"elements\":[" + String.join(",", java.util.Collections.nCopies(256, cube)) + "]}").ok(), "256 cubes are fine");
        h.assertTrue(ModelJson.read("{\"elements\":[{\"from\":[-16,-16,-16],\"to\":[32,32,32],\"faces\":{\"up\":{\"texture\":\"#a\",\"uv\":[0,0,16,16],\"tintindex\":-1,\"rotation\":180}}}],\"display\":{\"gui\":{\"translation\":[80,-80,0],\"scale\":[-4,4,0]}}}").ok(), "Exact vanilla limits are allowed");
        h.succeed();
    }
    /** b-models12 stage 1: undo/redo stack: one push per action, equal states skipped, redo cleared, limit drops the oldest. */
    public static void editHistory(GameTestHelper h) {
        var base = EditableModel.EMPTY;
        var history = new EditHistory<>(base);
        h.assertTrue(!history.canUndo() && !history.canRedo() && history.undo() == base, "Fresh history has nothing to undo");
        var one = base.addElement(EditableModel.Element.cube("seat", new EditableModel.Vec(2, 10, 2), new EditableModel.Vec(14, 12, 14), "#seat"));
        h.assertTrue(history.push(one) && !history.push(base.addElement(one.elements().getFirst())), "Equal state is not a second step");
        var moved = one.withElement(0, one.elements().getFirst().withBounds(new EditableModel.Vec(2, 11, 2), new EditableModel.Vec(14, 13, 14)));
        history.push(moved);
        h.assertTrue(history.undoSteps() == 2 && history.undo().equals(one) && history.undo().equals(base) && history.undo().equals(base), "Undo walks back and stops at the start");
        h.assertTrue(history.redo().equals(one) && history.redo().equals(moved) && !history.canRedo(), "Redo walks forward again");
        history.undo();
        var renamed = one.withElement(0, one.elements().getFirst().withName("sitz"));
        history.push(renamed);
        h.assertTrue(!history.canRedo() && history.current().equals(renamed), "A new step clears redo");
        var small = new EditHistory<>(0, 3);
        for (int i = 1; i <= 5; i++) small.push(i);
        h.assertTrue(small.undoSteps() == 3 && small.undo() == 4 && small.undo() == 3 && small.undo() == 2 && !small.canUndo(), "Limit keeps the newest steps");
        h.assertTrue(EditHistory.DEFAULT_LIMIT == 200 && one.removeElement(0).equals(base) && base.withTexture("seat", "minecraft:block/oak_planks").withTexture("seat", null).equals(base), "Default limit 200, edit helpers are pure");
        h.succeed();
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
        // The wand moved to simplebuildingtools on 26.3 (bundle 131); the old id still resolves through IdMoves,
        // and the catalogue entry has to name the item's real registry id.
        var wand = BuiltInRegistries.ITEM.getOptional(Identifier.parse("simplebuildingtools:diamond_building_wand"))
                .orElseGet(() -> BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:diamond_building_wand")));
        h.assertTrue(wand != Items.AIR, "No diamond building wand registered");
        String base = BuiltInRegistries.ITEM.getKey(wand).toString();
        fixture(base, () -> {
            var input = new ItemStack(wand);
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
                String key = "text.autoconfig.simplemodels.option." + f.getName();
                for (var lang : List.of(en, de)) for (String suffix : List.of("", ".@Tooltip")) h.assertTrue(lang.containsKey(key + suffix), "Config metadata " + key + suffix);
                h.assertTrue(en.get(key + ".@Tooltip").contains("Default: " + f.get(defaults)) && de.get(key + ".@Tooltip").contains("Standard: " + f.get(defaults)), "Default in every tooltip");
            }
        } catch (Exception e) { throw new AssertionError(e); } h.succeed();
    }
    private ModelTests() {}

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplebuilding.modules.simplemodels.discover.ModelsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebuilding.modules.simplemodels.discover.ModelsDiscover::behaviourHints);
    }
}
