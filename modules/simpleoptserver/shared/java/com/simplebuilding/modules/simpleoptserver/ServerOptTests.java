package com.simplebuilding.modules.simpleoptserver;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Simple Opt Server checks (Fabric wrapper ModuleGameTest). */
public final class ServerOptTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("compat_modes", ServerOptTests::compatModes);
        ALL.put("s5a_bounds", ServerOptTests::s5aBounds);
        ALL.put("s5a_controller", ServerOptTests::s5aController);
        ALL.put("config_file", ServerOptTests::configFile);
    }

    private ServerOptTests() {}

    private static OptSettings settings(String json) {
        return OptSettings.of(com.google.gson.JsonParser.parseString(json).getAsJsonObject(), ServerOpt.defaults());
    }

    public static void compatModes(GameTestHelper h) {
        List<OptCompat.Decision> live = OptCompat.decisions(ServerOpt.MOD_ID);
        var p = ServerOpt.params();
        try {
            ServerOpt.apply(settings("{}"), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(OptCompat.active(ServerOpt.MOD_ID, "dynamic_distance"), "auto without ServerCore: ours on");
            h.assertTrue(!ServerOpt.params().adjustSimulation(), "simulation distance stays by default (gameplay)");
            ServerOpt.apply(settings("{}"), true, Set.of("servercore")::contains, Map.of());
            h.assertTrue(!OptCompat.active(ServerOpt.MOD_ID, "dynamic_distance"), "ServerCore present: theirs wins");
            ServerOpt.apply(settings("{\"areas\":{\"dynamic_distance\":{\"mode\":\"ours\"}}}"), true, Set.of("servercore")::contains, Map.of());
            h.assertTrue(!OptCompat.active(ServerOpt.MOD_ID, "dynamic_distance"), "ours next to ServerCore (same target): falls back");
            ServerOpt.apply(settings("{\"areas\":{\"dynamic_distance\":{\"mode\":\"off\"}}}"), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(!OptCompat.active(ServerOpt.MOD_ID, "dynamic_distance"), "off");
        } finally {
            ServerOpt.restore(live, p);
        }
        h.succeed();
    }

    /** Never above server.properties, never below 3 (or minView), simulation untouched unless allowed. */
    public static void s5aBounds(GameTestHelper h) {
        var p = DynamicDistance.Params.DEFAULTS;
        var d = new DynamicDistance.Distances(12, 10);
        for (int i = 0; i < 40; i++) d = DynamicDistance.step(d, 200, p, 12, 10);
        h.assertTrue(d.view() == 6 && d.simulation() == 10, "overload: view down to minView, simulation fixed: " + d);
        for (int i = 0; i < 40; i++) d = DynamicDistance.step(d, 1, p, 12, 10);
        h.assertTrue(d.view() == 12 && d.simulation() == 10, "free: back up to server.properties, never above: " + d);
        var hold = DynamicDistance.step(new DynamicDistance.Distances(9, 10), p.targetMspt() * 0.9, p, 12, 10);
        h.assertTrue(hold.view() == 9, "between 80 % and target: hold");
        var low = new DynamicDistance.Params(45, 1, 1, 15, true);
        d = new DynamicDistance.Distances(12, 10);
        for (int i = 0; i < 40; i++) d = DynamicDistance.step(d, 200, low, 12, 10);
        h.assertTrue(d.view() == DynamicDistance.FLOOR && d.simulation() == DynamicDistance.FLOOR, "floor 3 even when config says 1: " + d);
        for (int i = 0; i < 40; i++) d = DynamicDistance.step(d, 1, low, 12, 10);
        h.assertTrue(d.view() == 12 && d.simulation() == 10, "adjusted simulation also returns to its maximum: " + d);
        var small = DynamicDistance.step(new DynamicDistance.Distances(4, 4), 200, p, 4, 4);
        h.assertTrue(small.view() == 4, "server.properties below minView: stays at server.properties");
        h.succeed();
    }

    public static void s5aController(GameTestHelper h) {
        var p = new DynamicDistance.Params(45, 6, 5, 1, false);
        var c = new DynamicDistance.Controller(10, 8);
        var now = new DynamicDistance.Distances(10, 8);
        int changes = 0;
        for (int t = 0; t < 19; t++) if (c.onTick(1, 100, now, p) != null) changes++;
        h.assertTrue(changes == 0, "no step before the interval");
        var next = c.onTick(1, 100, now, p);
        h.assertTrue(next != null && next.view() == 9, "one step after stepSeconds");
        for (int t = 0; t < 19; t++) c.onTick(0, 100, now, p);
        h.assertTrue(c.onTick(0, 100, now, p) == null, "no players: nothing changes");
        h.succeed();
    }

    public static void configFile(GameTestHelper h) {
        try {
            var dir = Files.createTempDirectory("simpleoptserver-test");
            OptSettings.load(dir, ServerOpt.FILE, ServerOpt.defaults(), s -> {});
            String text = Files.readString(dir.resolve(ServerOpt.FILE));
            for (String key : List.of("dynamic_distance", "targetMspt", "minView", "minSimulation", "stepSeconds", "adjustSimulation"))
                h.assertTrue(text.contains(key), "file has " + key);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }
}
