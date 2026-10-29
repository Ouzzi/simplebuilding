package com.simplelib.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplelib.api.config.ConfigCommands;
import com.simplelib.api.config.ConfigCommands.Kind;
import com.simplelib.api.config.ConfigCommands.Status;
import com.simplelib.config.LibConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

/**
 * GameTests of the config command (CMD24, docs/ai/PLAN-B-CMDCFG24-2026-10-24.md): a synthetic config through every kind of
 * value, SimpleLib's own config through the server's dispatcher, and every announced option of every loaded mod reachable
 * by {@code /<modid> config get <option>}. {@link #roundTrip} is the shared check the modules call for their own configs.
 */
public final class ConfigCommandTests {
    private ConfigCommandTests() {}

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    private static CommandSourceStack source(GameTestHelper h) {
        return h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
    }

    private static int run(CommandDispatcher<CommandSourceStack> d, String command, CommandSourceStack source) {
        try {
            return d.execute(command, source);
        } catch (CommandSyntaxException e) {
            return -1;
        }
    }

    /** Lists, numbers, text, a group, a choice: read, set, wrong type, clamp, reset, saved file, untouched defaults. */
    static void types(GameTestHelper h) {
        JsonObject[] state = {null};
        Consumer<JsonObject> apply = j -> {
            JsonObject o = new JsonObject();
            o.addProperty("flag", !j.has("flag") || j.get("flag").getAsBoolean());
            o.addProperty("count", Math.max(0, Math.min(10, j.has("count") ? j.get("count").getAsInt() : 5)));
            o.addProperty("rate", Math.max(0.0, Math.min(1.0, j.has("rate") ? j.get("rate").getAsDouble() : 0.5)));
            o.addProperty("name", j.has("name") ? j.get("name").getAsString() : "abc");
            JsonObject group = new JsonObject();
            JsonObject g = j.has("group") && j.get("group").isJsonObject() ? j.getAsJsonObject("group") : new JsonObject();
            group.addProperty("depth", g.has("depth") ? g.get("depth").getAsInt() : 3);
            o.add("group", group);
            JsonArray ids = new JsonArray();
            if (j.has("ids")) j.getAsJsonArray("ids").forEach(ids::add);
            else { ids.add("a"); ids.add("b"); }
            o.add("ids", ids);
            o.addProperty("mode", j.has("mode") ? j.get("mode").getAsString() : "light");
            state[0] = o;
        };
        apply.accept(new JsonObject());
        Path dir;
        try {
            dir = Files.createTempDirectory("simplelib-cmd24");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Path file = dir.resolve("test-server.json");
        ConfigCommands.Builder builder = ConfigCommands.server("simplelibtest").json(() -> state[0], apply).file(file)
                .choices("mode", "light", "dark").restart("group");
        ConfigCommands.Spec spec = builder.build();

        List<String> paths = spec.options().stream().map(ConfigCommands.Option::path).toList();
        check(h, paths.equals(List.of("flag", "count", "rate", "name", "group.depth", "ids", "mode")), "options " + paths);
        check(h, spec.option("flag").kind() == Kind.BOOL && spec.option("count").kind() == Kind.INT && spec.option("rate").kind() == Kind.DECIMAL
                && spec.option("name").kind() == Kind.TEXT && spec.option("ids").kind() == Kind.LIST && spec.option("group.depth").kind() == Kind.INT,
                "kinds");

        check(h, spec.set("count", "7").status() == Status.SET && state[0].get("count").getAsInt() == 7, "set count 7");
        var clamped = spec.set("count", "99");
        check(h, clamped.status() == Status.CLAMPED && clamped.value().equals("10"), "count 99 clamped to 10: " + clamped);
        check(h, spec.set("count", "abc").status() == Status.INVALID && state[0].get("count").getAsInt() == 10, "count abc refused");
        check(h, spec.set("flag", "maybe").status() == Status.INVALID, "flag maybe refused");
        check(h, spec.set("flag", "FALSE").ok() && !state[0].get("flag").getAsBoolean(), "flag FALSE");
        check(h, spec.set("rate", "0.25").status() == Status.SET && state[0].get("rate").getAsDouble() == 0.25, "rate 0.25");
        check(h, spec.set("rate", "NaN").status() == Status.INVALID, "rate NaN refused");
        check(h, spec.set("name", "\"hello world\"").ok() && state[0].get("name").getAsString().equals("hello world"), "quoted text");
        check(h, spec.set("group.depth", "4").ok() && state[0].getAsJsonObject("group").get("depth").getAsInt() == 4, "group.depth 4");
        check(h, spec.restart("group.depth") && !spec.restart("count"), "restart hint follows the group prefix");
        check(h, spec.set("ids", "x, y").ok() && state[0].getAsJsonArray("ids").toString().equals("[\"x\",\"y\"]"), "list x, y: " + state[0].get("ids"));
        check(h, spec.set("ids", "[]").ok() && state[0].getAsJsonArray("ids").isEmpty(), "empty list");
        check(h, spec.set("mode", "DARK").ok() && state[0].get("mode").getAsString().equals("dark"), "choice DARK -> dark");
        check(h, spec.set("mode", "blue").status() == Status.INVALID, "choice blue refused");
        check(h, spec.set("nope", "1").status() == Status.UNKNOWN, "unknown option");

        // A fresh spec takes its defaults from a snapshot and leaves the live values alone.
        ConfigCommands.Spec fresh = builder.build();
        check(h, fresh.defaults().get("count").getAsInt() == 5 && state[0].get("count").getAsInt() == 10, "defaults snapshot keeps live values");
        check(h, spec.reset("count").ok() && state[0].get("count").getAsInt() == 5, "reset count");

        // Saved file = live config.
        try {
            JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            check(h, saved.equals(state[0]), "file " + saved + " vs " + state[0]);
        } catch (Exception e) {
            check(h, false, "file not written: " + e);
        }

        // The command tree: permission 2 for the server, list/get/set/reset/help.
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        d.register(LiteralArgumentBuilder.<CommandSourceStack>literal("simplelibtest")
                .then(ConfigCommands.tree(spec, Commands.hasPermission(Commands.LEVEL_GAMEMASTERS), ConfigCommands.SERVER_IO)));
        CommandSourceStack op = source(h);
        check(h, run(d, "simplelibtest config set count 3", op) == 1 && state[0].get("count").getAsInt() == 3, "command set");
        check(h, run(d, "simplelibtest config set count three", op) == 0 && state[0].get("count").getAsInt() == 3, "command invalid value");
        check(h, run(d, "simplelibtest config get group.depth", op) == 1, "command get");
        check(h, run(d, "simplelibtest config list", op) == 7 && run(d, "simplelibtest config", op) == 7, "command list");
        check(h, run(d, "simplelibtest config list group", op) == 1, "command list group");
        check(h, run(d, "simplelibtest config reset count", op) == 1 && state[0].get("count").getAsInt() == 5, "command reset");
        check(h, run(d, "simplelibtest config help", op) == 1, "command help");
        CommandSourceStack player = op.withPermission(LevelBasedPermissionSet.ALL);
        check(h, run(d, "simplelibtest config set count 4", player) == -1 && state[0].get("count").getAsInt() == 5, "level 0 may not change it");
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        } catch (Exception ignored) {
        }
        h.succeed();
    }

    /** SimpleLib's own file through the real dispatcher: only its own keys, bounds of LibConfig, reset to the default. */
    static void lib(GameTestHelper h) {
        JsonObject before = LibConfig.toJson();
        try {
            var spec = ConfigCommands.spec("simplelib", false);
            check(h, spec != null, "simplelib config not announced");
            check(h, spec.options().stream().map(ConfigCommands.Option::path).toList().equals(LibConfig.KEYS), "simplelib options " + spec.options());
            var d = h.getLevel().getServer().getCommands().getDispatcher();
            CommandSourceStack op = source(h);
            check(h, run(d, "simplelib config set eatSpeedBonus 0.2", op) == 1 && LibConfig.eatSpeedBonus == 0.2, "set eatSpeedBonus");
            check(h, run(d, "simplelib config set eatSpeedBonus 5", op) == 1 && LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_MAX, "clamped to max");
            check(h, run(d, "simplelib config set warmBaseTicks lots", op) == 0, "wrong type refused");
            check(h, run(d, "simplelib config reset eatSpeedBonus", op) == 1 && LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_DEFAULT, "reset");
            check(h, run(d, "simplelib config set legendaryAdvancements minecraft:story/root, minecraft:end/root", op) == 1
                    && LibConfig.legendaryAdvancements.size() == 2, "id list " + LibConfig.legendaryAdvancements);
            check(h, run(d, "simplelib config get eatSpeedBonus", op.withPermission(LevelBasedPermissionSet.ALL)) == -1, "level 0 may not read it");
        } finally {
            LibConfig.apply(before);
            LibConfig.save();
        }
        h.succeed();
    }

    /** Every option of every announced server config answers {@code /<modid> config get <option>} (all modules in the integration run). */
    static void everyOption(GameTestHelper h) {
        var d = h.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack op = source(h);
        var specs = ConfigCommands.specs(false);
        check(h, specs.stream().anyMatch(s -> s.modId().equals("simplelib")), "simplelib missing in " + specs.size() + " configs");
        int count = 0;
        for (var spec : specs) {
            var options = spec.options();
            check(h, !options.isEmpty(), spec.modId() + " has no options");
            for (var option : options) {
                String command = spec.modId() + " config get " + option.path();
                check(h, run(d, command, op) == 1, "/" + command + " does not answer");
                check(h, option.defaultValue() != null, spec.modId() + " " + option.path() + " has no default");
                count++;
            }
        }
        com.simplelib.SimpleLib.LOG.info("Config commands: {} options of {} mods reachable", count, specs.size());
        h.succeed();
    }

    /**
     * For module tests: {@code /<modid> config set <path> <value>} stores {@code expected} (file too, when the spec writes
     * one), {@code invalid} is refused without a change, reset brings the default back; the old value is restored at the end.
     */
    public static void roundTrip(GameTestHelper h, String modId, String path, String value, String expected, String invalid) {
        var spec = ConfigCommands.spec(modId, false);
        check(h, spec != null, modId + " config not announced");
        check(h, spec.option(path) != null, modId + " has no option " + path);
        String before = ConfigCommands.show(spec.value(path));
        var d = h.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack op = source(h);
        try {
            check(h, run(d, modId + " config set " + path + " " + value, op) == 1, "set " + path + " " + value);
            String stored = ConfigCommands.show(spec.value(path));
            check(h, stored.equals(expected), path + " = " + stored + ", expected " + expected);
            if (spec.file() != null) {
                try {
                    var saved = JsonParser.parseString(Files.readString(spec.file())).getAsJsonObject();
                    JsonObject cur = saved;
                    String[] parts = path.split("\\.");
                    for (int i = 0; i < parts.length - 1; i++) cur = cur.getAsJsonObject(parts[i]);
                    check(h, ConfigCommands.show(cur.get(parts[parts.length - 1])).equals(expected), "file has " + cur.get(parts[parts.length - 1]));
                } catch (Exception e) {
                    check(h, false, modId + " file not readable: " + e);
                }
            }
            check(h, run(d, modId + " config set " + path + " " + invalid, op) == 0
                    && ConfigCommands.show(spec.value(path)).equals(expected), "invalid " + invalid + " was taken");
            check(h, run(d, modId + " config reset " + path, op) == 1
                    && ConfigCommands.show(spec.value(path)).equals(ConfigCommands.show(spec.option(path).defaultValue())), "reset " + path);
            check(h, run(d, modId + " config set " + path + " " + value, op.withPermission(LevelBasedPermissionSet.ALL)) == -1, "level 0 may set " + path);
        } finally {
            spec.set(path, before);
        }
    }

    /**
     * For the modules (CMD24-2/3): the config of {@code modId} is announced with options and a default for each; a server
     * config is also run through {@link #roundTrip} with its first switch (set, wrong type refused, reset, level 0 refused).
     */
    public static void module(GameTestHelper h, String modId, boolean client) {
        var spec = ConfigCommands.spec(modId, client);
        check(h, spec != null, modId + " config not announced");
        var options = spec.options();
        check(h, !options.isEmpty(), modId + " has no options");
        for (var option : options) check(h, option.defaultValue() != null, modId + " " + option.path() + " has no default");
        if (!client) {
            var flag = options.stream().filter(o -> o.kind() == Kind.BOOL).findFirst();
            check(h, flag.isPresent(), modId + " has no switch to try");
            roundTrip(h, modId, flag.get().path(), "true", "true", "abc");
        }
        h.succeed();
    }
}
