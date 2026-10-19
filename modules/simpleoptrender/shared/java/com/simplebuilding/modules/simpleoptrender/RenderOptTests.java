package com.simplebuilding.modules.simpleoptrender;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Simple Opt Render checks (Fabric wrapper ModuleGameTest). Runs on the dedicated test server: rules, not pixels. */
public final class RenderOptTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("compat_modes", RenderOptTests::compatModes);
        ALL.put("mixin_plugin", RenderOptTests::mixinPlugin);
        ALL.put("leaf_culling_rule", RenderOptTests::leafCullingRule);
        ALL.put("lightmap_cache", RenderOptTests::lightmapCache);
        ALL.put("fast_ip_ping", RenderOptTests::fastIpPing);
        ALL.put("config_file", RenderOptTests::configFile);
        ALL.put("more_culling_rules", RenderOptTests::moreCullingRules);
        ALL.put("details_rules", RenderOptTests::detailsRules);
        ALL.put("target_check", RenderOptTests::targetCheck);
    }

    private RenderOptTests() {}

    private static OptSettings settings(String leafMode, int depth) {
        var read = com.google.gson.JsonParser.parseString("{\"areas\":{\"leaf_culling\":{\"mode\":\"" + leafMode + "\",\"depth\":" + depth + "}}}").getAsJsonObject();
        return OptSettings.of(read, RenderOpt.defaults());
    }

    /** Runs {@code body} with fake decisions and puts the real ones (and the depth) back afterwards. */
    private static void withFake(Runnable body) {
        List<OptCompat.Decision> live = OptCompat.decisions(RenderOpt.MOD_ID);
        int depth = RenderOpt.leafDepth();
        var parts = RenderOpt.culling();
        var switches = RenderOpt.details();
        try {
            body.run();
        } finally {
            RenderOpt.restore(live, depth, parts, switches);
        }
    }

    /** Mode resolution with and without a fake "other mod" in the loaded set. */
    public static void compatModes(GameTestHelper h) {
        withFake(() -> {
            RenderOpt.apply(settings("auto", 0), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(RenderOpt.active(RenderOpt.LEAF_CULLING), "auto without other mod: ours on");
            h.assertTrue(RenderOpt.active(RenderOpt.NO_TELEMETRY), "other areas on too");
            RenderOpt.apply(settings("auto", 0), true, Set.of("cullleaves")::contains, Map.of());
            h.assertTrue(!RenderOpt.active(RenderOpt.LEAF_CULLING), "auto with Cull Leaves: theirs wins");
            h.assertTrue(RenderOpt.active(RenderOpt.LIGHTMAP_CACHE), "only the covered area steps back");
            RenderOpt.apply(settings("ours", 0), true, Set.of("moreculling")::contains, Map.of());
            h.assertTrue(!RenderOpt.active(RenderOpt.LEAF_CULLING), "ours next to More Culling (same target): falls back");
            RenderOpt.apply(settings("auto", 0), true, Set.of("cullleaves")::contains, Map.of("leaf_culling", OptCompat.Winner.OURS));
            var d = OptCompat.decisions(RenderOpt.MOD_ID).get(0);
            h.assertTrue(!d.active() && d.hint().contains("cullleaves"), "measured win only gives a hint");
            RenderOpt.apply(settings("off", 0), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(!RenderOpt.active(RenderOpt.LEAF_CULLING), "off is off");
            RenderOpt.apply(settings("auto", 0), false, Set.<String>of()::contains, Map.of());
            h.assertTrue(!RenderOpt.active(RenderOpt.LEAF_CULLING) && !RenderOpt.active(RenderOpt.FAST_IP_PING), "super-mod switch off");
            RenderOpt.apply(settings("auto", 0), true, Set.of("moreculling", "sodium-extra")::contains, Map.of());
            h.assertTrue(!RenderOpt.live(RenderOpt.MORE_CULLING) && !RenderOpt.live(RenderOpt.DETAILS), "More Culling covers O3, Sodium Extra O9");
            h.assertTrue(RenderOpt.live(RenderOpt.LIGHTMAP_CACHE), "other areas stay");
            RenderOpt.apply(settings("auto", 0), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(RenderOpt.live(RenderOpt.MORE_CULLING) && RenderOpt.live(RenderOpt.DETAILS), "O3/O9 on without rivals");
        });
        for (OptCompat.Area a : RenderOpt.AREAS)
            h.assertTrue(RenderOpt.priorities().get(a.id()) == OptCompat.Winner.THEIRS, "shipped table: theirs for " + a.id());
        h.succeed();
    }

    /** shouldApplyMixin follows the area of the mixin's package. */
    public static void mixinPlugin(GameTestHelper h) {
        String leaf = "com.simplebuilding.modules.simpleoptrender.mixin.leaf_culling.ModelBlockRendererMixin";
        String light = "com.simplebuilding.modules.simpleoptrender.mixin.lightmap_cache.LightmapMixin";
        withFake(() -> {
            RenderOpt.apply(settings("auto", 0), true, Set.of("cullleaves")::contains, Map.of());
            h.assertTrue(!RenderOpt.mixinActive(leaf), "leaf mixin skipped when Cull Leaves wins");
            h.assertTrue(RenderOpt.mixinActive(light), "lightmap mixin still applied");
            RenderOpt.apply(settings("auto", 0), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(RenderOpt.mixinActive(leaf), "leaf mixin applied without other mod");
            h.assertTrue(!RenderOpt.mixinActive("com.simplebuilding.modules.simpleoptrender.mixin.unknown_area.X"), "unknown area off");
        });
        h.succeed();
    }

    /** No holes: a face next to air or a non-leaf block always stays; enclosed faces go; depth keeps outer layers. */
    public static void leafCullingRule(GameTestHelper h) {
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        // 3x3x3 leaf cube at (1..3, 1..3, 1..3), stone at (4, 2, 2) next to its middle +x face.
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) for (int z = 1; z <= 3; z++) h.setBlock(x, y, z, leaf);
        h.setBlock(4, 2, 2, Blocks.STONE.defaultBlockState());
        h.setBlock(2, 2, 4, Blocks.AIR.defaultBlockState());
        var level = h.getLevel();
        BlockPos center = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos edge = h.absolutePos(new BlockPos(3, 2, 2));
        BlockPos top = h.absolutePos(new BlockPos(2, 3, 2));
        for (Direction d : Direction.values())
            h.assertTrue(LeafCulling.cull(level, leaf, d, center.relative(d), 0), "enclosed face culled at depth 0: " + d);
        h.assertTrue(!LeafCulling.cull(level, leaf, Direction.EAST, edge.east(), 0), "face next to stone stays");
        h.assertTrue(!LeafCulling.cull(level, leaf, Direction.UP, top.above(), 0), "face next to air stays");
        for (int depth = 0; depth <= RenderOpt.MAX_DEPTH; depth++)
            for (Direction d : Direction.values()) {
                BlockPos outer = center.relative(d);
                h.assertTrue(!LeafCulling.cull(level, leaf, d, outer.relative(d), depth), "outer face never culled (depth " + depth + ", " + d + ")");
            }
        h.assertTrue(!LeafCulling.cull(level, leaf, Direction.EAST, edge, 1), "depth 1: face seen through one outer layer stays");
        h.assertTrue(!LeafCulling.cull(level, Blocks.STONE.defaultBlockState(), Direction.EAST, edge, 0), "non-leaf block untouched");
        h.succeed();
    }

    public static void lightmapCache(GameTestHelper h) {
        float[] last = new float[LightmapCache.SIZE], now = new float[LightmapCache.SIZE];
        now[1] = 1.4f;
        h.assertTrue(!LightmapCache.unchanged(last, now, false), "first upload always");
        now[1] = 1.4f + LightmapCache.EPSILON * 0.5f;
        h.assertTrue(LightmapCache.unchanged(last, now, true), "below half a step: skipped");
        now[1] = 1.4f + LightmapCache.EPSILON * 0.9f;
        h.assertTrue(LightmapCache.unchanged(last, now, true), "still compared with the uploaded value");
        now[1] = 1.4f + LightmapCache.EPSILON * 1.5f;
        h.assertTrue(!LightmapCache.unchanged(last, now, true), "drift beyond half a step uploads");
        now[17] = 0.5f;
        h.assertTrue(!LightmapCache.unchanged(last, now, true), "any input counts");
        h.succeed();
    }

    public static void fastIpPing(GameTestHelper h) {
        InetAddress v4 = FastIpPing.literal("127.0.0.1"), v6 = FastIpPing.literal("[::1]");
        h.assertTrue(v4 != null && "127.0.0.1".equals(v4.getHostName()), "IPv4 literal keeps its text as host name (no reverse DNS)");
        h.assertTrue(v6 != null && v6.isLoopbackAddress(), "IPv6 literal in brackets");
        h.assertTrue(FastIpPing.literal("example.org") == null && FastIpPing.literal("") == null && FastIpPing.literal(null) == null, "names stay with Vanilla");
        h.succeed();
    }

    public static void configFile(GameTestHelper h) {
        try {
            var dir = Files.createTempDirectory("simpleoptrender-test");
            OptSettings.load(dir, RenderOpt.FILE, RenderOpt.defaults(), s -> {});
            String text = Files.readString(dir.resolve(RenderOpt.FILE));
            for (OptCompat.Area a : RenderOpt.AREAS) h.assertTrue(text.contains(a.id()), "file lists " + a.id());
            h.assertTrue(text.contains("\"depth\"") && text.contains("conflictScan"), "parameters written");
            for (String key : List.of("signText", "itemFrames", "beaconBeams", "particles", "textureAnimations", "fog"))
                h.assertTrue(text.contains("\"" + key + "\": true"), "default on: " + key);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }

    /** O3 geometry: text side and frame back culling only well behind the board or wall, never at the edge. */
    public static void moreCullingRules(GameTestHelper h) {
        // Sign facing south (yRot 0): front normal +z.
        h.assertTrue(MoreCulling.signSides(0, 3, 0) == MoreCulling.SignSides.FRONT, "in front: back text dropped");
        h.assertTrue(MoreCulling.signSides(0, -3, 0) == MoreCulling.SignSides.BACK, "behind: front text dropped");
        h.assertTrue(MoreCulling.signSides(5, 0.4, 0) == MoreCulling.SignSides.BOTH, "beside the board plane: both kept");
        h.assertTrue(MoreCulling.signSides(-3, 0, 90) == MoreCulling.SignSides.FRONT, "yRot 90 faces west (-x)");
        h.assertTrue(MoreCulling.signSides(2, 2, 45) == MoreCulling.SignSides.BOTH, "on the 45 degree plane: both kept");
        h.assertTrue(MoreCulling.signSides(-2, 2, 45) == MoreCulling.SignSides.FRONT, "yRot 45 faces (-x, +z)");
        // Agrees with Vanilla's side rule (getSlotPlayerIsFacing) wherever we drop a side.
        for (int yaw = 0; yaw < 360; yaw += 15)
            for (int a = 0; a < 360; a += 10) {
                double dx = 4 * Math.cos(Math.toRadians(a)), dz = 4 * Math.sin(Math.toRadians(a));
                float playerYRot = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
                boolean vanillaFront = net.minecraft.util.Mth.degreesDifferenceAbs(yaw, playerYRot) <= 90;
                var sides = MoreCulling.signSides(dx, dz, yaw);
                h.assertTrue(sides == MoreCulling.SignSides.BOTH || (sides == MoreCulling.SignSides.FRONT) == vanillaFront,
                        "matches Vanilla front/back at yaw " + yaw + ", angle " + a);
            }
        h.assertTrue(MoreCulling.behindFrame(0, 0, -1.5, 0, 0, 1), "frame facing south, camera north of its wall: hidden");
        h.assertTrue(!MoreCulling.behindFrame(0, 0, -0.9, 0, 0, 1), "camera inside the wall depth: kept");
        h.assertTrue(!MoreCulling.behindFrame(3, 0, 0.2, 0, 0, 1), "camera beside the frame: kept");
        h.assertTrue(MoreCulling.behindFrame(0, 2, 0, 0, -1, 0), "ceiling frame facing down, camera above: hidden");
        h.succeed();
    }

    public static void detailsRules(GameTestHelper h) {
        var off = new Details.Switches(true, true, false);
        h.assertTrue(Details.liftFog(off, false, false), "fog false lifts the air fog");
        h.assertTrue(!Details.liftFog(off, true, false), "fluid or snow fog stays");
        h.assertTrue(!Details.liftFog(off, false, true), "blindness/darkness fog stays");
        h.assertTrue(!Details.liftFog(Details.Switches.DEFAULTS, false, false), "default: Vanilla fog");
        withFake(() -> {
            var read = com.google.gson.JsonParser.parseString("{\"areas\":{\"details\":{\"particles\":false,\"fog\":false},\"more_culling\":{\"beaconBeams\":false}}}").getAsJsonObject();
            RenderOpt.apply(OptSettings.of(read, RenderOpt.defaults()), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(!RenderOpt.details().particles() && RenderOpt.details().textureAnimations() && !RenderOpt.details().fog(), "switches read");
            h.assertTrue(!RenderOpt.culling().beaconBeams() && RenderOpt.culling().signText() && RenderOpt.culling().itemFrames(), "parts read");
        });
        h.succeed();
    }

    /** Concept §4.5: another mod hooking a class of an active area switches that area off at runtime, never ours forced. */
    public static void targetCheck(GameTestHelper h) {
        withFake(() -> {
            RenderOpt.apply(settings("auto", 0), true, Set.<String>of()::contains, Map.of());
            RenderOpt.RUNTIME.shouldApply("net.minecraft.client.renderer.block.ModelBlockRenderer",
                    "com.simplebuilding.modules.simpleoptrender.mixin.leaf_culling.ModelBlockRendererMixin");
            RenderOpt.RUNTIME.scan(Map.of("somemod", Set.of("net/minecraft/client/renderer/block/ModelBlockRenderer")), m -> false, s -> {});
            var d = OptCompat.decisions(RenderOpt.MOD_ID).get(0);
            h.assertTrue(!RenderOpt.live(RenderOpt.LEAF_CULLING) && d.reason() == OptCompat.Reason.CONFLICT && "somemod".equals(d.rival()),
                    "leaf culling off: possible conflict with somemod");
            h.assertTrue(RenderOpt.live(RenderOpt.LIGHTMAP_CACHE), "areas on other classes stay");
            RenderOpt.apply(settings("ours", 0), true, Set.<String>of()::contains, Map.of());
            RenderOpt.RUNTIME.scan(Map.of("somemod", Set.of("net.minecraft.client.renderer.block.ModelBlockRenderer")), m -> false, s -> {});
            h.assertTrue(RenderOpt.live(RenderOpt.LEAF_CULLING), "mode ours keeps it");
        });
        h.assertTrue(OptCompat.decisions(RenderOpt.MOD_ID).stream().noneMatch(d -> d.reason() == OptCompat.Reason.CONFLICT),
                "no conflict in this test run (family and Fabric API ignored)");
        h.succeed();
    }
}
