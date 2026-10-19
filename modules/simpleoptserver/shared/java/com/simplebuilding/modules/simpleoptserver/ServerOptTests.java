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
import net.minecraft.world.entity.EntityTypes;

/** Simple Opt Server checks (Fabric wrapper ModuleGameTest). */
public final class ServerOptTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("compat_modes", ServerOptTests::compatModes);
        ALL.put("s5a_bounds", ServerOptTests::s5aBounds);
        ALL.put("s5a_controller", ServerOptTests::s5aController);
        ALL.put("config_file", ServerOptTests::configFile);
        ALL.put("s5b_rule", ServerOptTests::s5bRule);
    }

    private ServerOptTests() {}

    private static OptSettings settings(String json) {
        return OptSettings.of(com.google.gson.JsonParser.parseString(json).getAsJsonObject(), ServerOpt.defaults());
    }

    public static void compatModes(GameTestHelper h) {
        List<OptCompat.Decision> live = OptCompat.decisions(ServerOpt.MOD_ID);
        var p = ServerOpt.params();
        var a = ServerOpt.activation();
        h.assertTrue(!ServerOpt.active(ServerOpt.ACTIVATION_RANGE), "S5b off in a default start (changes gameplay)");
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
            ServerOpt.apply(settings("{}"), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(!OptCompat.active(ServerOpt.MOD_ID, "activation_range"), "S5b default mode off");
            ServerOpt.apply(settings("{\"areas\":{\"activation_range\":{\"mode\":\"auto\",\"range\":500,\"interval\":1}}}"), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(OptCompat.active(ServerOpt.MOD_ID, "activation_range"), "S5b on when chosen");
            h.assertTrue(ServerOpt.activation().range() == ActivationRange.MAX_RANGE && ServerOpt.activation().interval() == 2, "range/interval clamped");
            ServerOpt.apply(settings("{\"areas\":{\"activation_range\":{\"mode\":\"auto\"}}}"), true, Set.of("servercore")::contains, Map.of());
            h.assertTrue(!OptCompat.active(ServerOpt.MOD_ID, "activation_range"), "ServerCore covers S5b too");
        } finally {
            ServerOpt.restore(live, p, a);
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
            var written = com.google.gson.JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("areas").getAsJsonObject("activation_range");
            h.assertTrue("off".equals(written.get("mode").getAsString()), "S5b written as off");
            for (String key : List.of("dynamic_distance", "targetMspt", "minView", "minSimulation", "stepSeconds", "adjustSimulation", "range", "interval"))
                h.assertTrue(text.contains(key), "file has " + key);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }

    /** S5b rule on real mobs: idle far mobs are skipped off-beat, everything that would drift stays active. */
    public static void s5bRule(GameTestHelper h) {
        var p = ActivationRange.Params.DEFAULTS;
        int skipped = 0;
        for (int t = 0; t < 40; t++) if (ActivationRange.offBeat(t, 7, p.interval())) skipped++;
        h.assertTrue(skipped == 30, "3 of 4 ticks skipped: " + skipped);
        h.assertTrue(!ActivationRange.offBeat(1, 7, 4) && ActivationRange.offBeat(2, 7, 4), "spread by id");
        var level = h.getLevel();
        var zombie = h.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
        zombie.setBaby(false);
        zombie.setOnGround(true);
        h.assertTrue(!ActivationRange.exempt(zombie), "idle zombie on the ground: may sleep");
        var noPlayers = level.players().isEmpty();
        int idle = 0;
        for (int t = 0; t < 4; t++) {
            zombie.tickCount = t;
            if (ActivationRange.skip(zombie, level, p)) idle++;
        }
        h.assertTrue(!noPlayers || idle == 3, "no player near: runs 1 of 4 ticks (" + idle + ")");
        zombie.setOnGround(false);
        h.assertTrue(ActivationRange.exempt(zombie), "airborne stays active");
        zombie.setOnGround(true);
        zombie.hurtTime = 5;
        h.assertTrue(ActivationRange.exempt(zombie), "hurt stays active");
        zombie.hurtTime = 0;
        zombie.setRemainingFireTicks(40);
        h.assertTrue(ActivationRange.exempt(zombie), "burning stays active");
        var calf = h.spawn(EntityTypes.COW, 2, 2, 1);
        calf.setAge(-24000);
        calf.setOnGround(true);
        h.assertTrue(ActivationRange.exempt(calf), "babies keep growing");
        var cow = h.spawn(EntityTypes.COW, 3, 2, 1);
        cow.setOnGround(true);
        h.assertTrue(!ActivationRange.exempt(cow), "adult cow may sleep");
        cow.setInLove(null);
        h.assertTrue(ActivationRange.exempt(cow), "cow in love stays active");
        h.assertTrue(ActivationRange.exempt(h.spawn(EntityTypes.VILLAGER, 1, 2, 3)), "villagers stay active");
        h.assertTrue(ActivationRange.exempt(h.spawn(EntityTypes.ARMOR_STAND, 2, 2, 3)), "non-mobs are never touched");
        h.succeed();
    }
}
