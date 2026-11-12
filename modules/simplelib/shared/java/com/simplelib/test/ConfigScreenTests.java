package com.simplelib.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.api.config.ConfigCommands;
import com.simplelib.api.config.ConfigCommands.Status;
import com.simplelib.api.config.ConfigScreenModel;
import com.simplelib.api.config.ConfigScreenModel.Access;
import com.simplelib.api.config.ConfigScreenModel.Category;
import com.simplelib.api.config.ConfigScreenModel.Field;
import com.simplelib.api.config.ConfigScreenModel.Widget;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTests of the generic config screen's model (L13, docs/ai/PLAN-B-CFGSCREEN31-2026-11-11.md): a config spec becomes
 * categories and widgets with defaults and ranges, saving writes only changed values through the config command's checks,
 * the rights table, and every announced config of every loaded mod gives a screen model.
 */
public final class ConfigScreenTests {
    private ConfigScreenTests() {}

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    /** A config with every kind: flag, count (0..10), rate, name, group.depth, ids list, mode choice, token secret. */
    private static ConfigCommands.Spec spec(JsonObject[] state, Path file) {
        Consumer<JsonObject> apply = j -> {
            JsonObject o = new JsonObject();
            o.addProperty("flag", !j.has("flag") || j.get("flag").getAsBoolean());
            o.addProperty("count", Math.max(0, Math.min(10, j.has("count") ? j.get("count").getAsInt() : 5)));
            o.addProperty("rate", j.has("rate") ? j.get("rate").getAsDouble() : 0.5);
            o.addProperty("name", j.has("name") ? j.get("name").getAsString() : "abc");
            JsonObject group = new JsonObject();
            JsonObject g = j.has("group") && j.get("group").isJsonObject() ? j.getAsJsonObject("group") : new JsonObject();
            group.addProperty("depth", g.has("depth") ? g.get("depth").getAsInt() : 3);
            o.add("group", group);
            JsonArray ids = new JsonArray();
            if (j.has("ids")) j.getAsJsonArray("ids").forEach(ids::add);
            else { ids.add(1); ids.add(2); }
            o.add("ids", ids);
            o.addProperty("mode", j.has("mode") ? j.get("mode").getAsString() : "light");
            o.addProperty("token", j.has("token") ? j.get("token").getAsString() : "");
            state[0] = o;
        };
        apply.accept(new JsonObject());
        return ConfigCommands.server("simplelibtest").json(() -> state[0], apply).file(file)
                .choices("mode", "light", "dark").restart("group").secret("token").range("count", 0, 10).build();
    }

    private static Field field(List<Category> categories, String path) {
        for (Category c : categories) for (Field f : c.fields()) if (f.path().equals(path)) return f;
        return null;
    }

    /** Categories in file order (flat options in "general"), widget per kind, default text, range, restart, secret. */
    static void model(GameTestHelper h) {
        JsonObject[] state = {null};
        List<Category> categories = ConfigScreenModel.categories(spec(state, null));
        check(h, categories.stream().map(Category::group).toList().equals(List.of(ConfigScreenModel.GENERAL, "group")), "groups " + categories);
        check(h, field(categories, "flag").widget() == Widget.TOGGLE && field(categories, "count").widget() == Widget.INTEGER
                && field(categories, "rate").widget() == Widget.DECIMAL && field(categories, "name").widget() == Widget.TEXT
                && field(categories, "ids").widget() == Widget.LIST && field(categories, "mode").widget() == Widget.CHOICE, "widgets");
        Field depth = field(categories, "group.depth");
        check(h, depth.name().equals("depth") && depth.restart() && depth.defaultText().equals("3"), "group.depth " + depth);
        Field count = field(categories, "count");
        check(h, count.range() != null && count.range()[0] == 0 && count.range()[1] == 10 && field(categories, "rate").range() == null, "range");
        check(h, field(categories, "ids").text().equals("[1,2]") && ConfigScreenModel.items(field(categories, "ids").value()).equals(List.of("1", "2")), "list text");
        check(h, field(categories, "mode").choices().equals(List.of("light", "dark")), "choices");
        check(h, field(categories, "token").secret() && field(categories, "token").text().equals("***"), "secret hidden");
        check(h, ConfigScreenModel.humanize("upgradeSeconds").equals("Upgrade seconds")
                && ConfigScreenModel.humanize("padTuning.teleporterTier1WarmupTicks").equals("Teleporter tier 1 warmup ticks"),
                "humanize " + ConfigScreenModel.humanize("padTuning.teleporterTier1WarmupTicks"));
        check(h, ConfigScreenModel.namesDefault("Range 1-5. Default: 3.") && ConfigScreenModel.namesDefault("Standard: 3")
                && !ConfigScreenModel.namesDefault("How long it takes."), "default named in tooltip");
        check(h, ConfigScreenModel.number(1.0).equals("1") && ConfigScreenModel.number(0.5).equals("0.5"), "range numbers");
        h.succeed();
    }

    /** Only changed values are written (clamped like the command), wrong text is refused, the file follows. */
    static void save(GameTestHelper h) {
        JsonObject[] state = {null};
        Path dir;
        try {
            dir = Files.createTempDirectory("simplelib-cfgscreen");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Path file = dir.resolve("screen-server.json");
        ConfigCommands.Spec spec = spec(state, file);
        // Unchanged values (as the widgets hand them back) write nothing.
        Map<String, String> same = new LinkedHashMap<>();
        for (Category c : ConfigScreenModel.categories(spec)) for (Field f : c.fields()) same.put(f.path(), f.text());
        same.put("ids", ConfigScreenModel.listText(List.of("1", "2"), spec.option("ids").defaultValue()));
        same.put("rate", Double.toString(0.5));
        same.put("count", Long.toString(5L));
        check(h, ConfigScreenModel.save(spec, same).isEmpty() && !Files.exists(file), "unchanged values write nothing");

        Map<String, String> edits = new LinkedHashMap<>();
        edits.put("flag", "false");
        edits.put("count", "99");
        edits.put("group.depth", "4");
        edits.put("ids", ConfigScreenModel.listText(List.of("7", " ", "8"), spec.option("ids").defaultValue()));
        edits.put("mode", "DARK");
        edits.put("name", "hello world");
        edits.put("token", "leaked");
        var results = ConfigScreenModel.save(spec, edits);
        check(h, results.size() == 6 && results.stream().allMatch(ConfigCommands.Result::ok), "six writes " + results);
        check(h, results.stream().anyMatch(r -> r.path().equals("count") && r.status() == Status.CLAMPED) && state[0].get("count").getAsInt() == 10, "count clamped");
        check(h, !state[0].get("flag").getAsBoolean() && state[0].getAsJsonObject("group").get("depth").getAsInt() == 4
                && state[0].getAsJsonArray("ids").toString().equals("[7,8]") && state[0].get("mode").getAsString().equals("dark")
                && state[0].get("name").getAsString().equals("hello world"), "values " + state[0]);
        check(h, state[0].get("token").getAsString().isEmpty(), "a secret is never written from the screen");
        var bad = ConfigScreenModel.save(spec, Map.of("count", "many", "mode", "blue", "nope", "1"));
        check(h, bad.stream().noneMatch(ConfigCommands.Result::ok) && state[0].get("count").getAsInt() == 10, "wrong text refused " + bad);
        try {
            JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            check(h, saved.equals(state[0]), "file " + saved);
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        } catch (Exception e) {
            check(h, false, "file not written: " + e);
        }
        h.succeed();
    }

    /** Client configs always; server configs: title screen yes, own world only for operators, remote server never. */
    static void access(GameTestHelper h) {
        check(h, ConfigScreenModel.access(true, true, true, false) == Access.EDIT, "client config on a remote server");
        check(h, ConfigScreenModel.access(false, false, false, false) == Access.EDIT, "server config from the title screen");
        check(h, ConfigScreenModel.access(false, true, false, true) == Access.EDIT, "own world, operator");
        check(h, ConfigScreenModel.access(false, true, false, false) == Access.READ_ONLY, "own world without cheats");
        check(h, ConfigScreenModel.access(false, true, true, true) == Access.READ_ONLY, "remote server, even as operator");
        h.succeed();
    }

    /** Every announced config (both sides) gives a model with every option, a widget and a default. */
    static void everyMod(GameTestHelper h) {
        int mods = 0, fields = 0;
        for (boolean client : new boolean[] {false, true}) {
            for (ConfigCommands.Spec spec : ConfigCommands.specs(client)) {
                List<Category> categories = ConfigScreenModel.categories(spec);
                long count = categories.stream().mapToLong(c -> c.fields().size()).sum();
                check(h, count == spec.options().size(), spec.modId() + ": " + count + " fields for " + spec.options().size() + " options");
                for (Category c : categories) {
                    for (Field f : c.fields()) {
                        check(h, f.widget() != null && f.defaultValue() != null, spec.modId() + " " + f.path());
                        if (f.widget() == Widget.TOGGLE) f.value().getAsBoolean();
                        if (f.widget() == Widget.INTEGER) f.value().getAsLong();
                        if (f.widget() == Widget.DECIMAL) f.value().getAsDouble();
                        fields++;
                    }
                }
                mods++;
            }
        }
        check(h, mods > 0, "no config announced");
        com.simplelib.SimpleLib.LOG.info("Config screens: {} fields of {} configs", fields, mods);
        h.succeed();
    }
}
