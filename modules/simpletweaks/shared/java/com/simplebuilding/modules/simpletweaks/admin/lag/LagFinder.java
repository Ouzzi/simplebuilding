package com.simplebuilding.modules.simpletweaks.admin.lag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * T34 lag finder (off by default): /lag [seconds] samples tick time, time per block entity type and entity type,
 * the most expensive chunks and redstone state changes per chunk (a possible redstone clock above
 * {@code clockThreshold} changes per second). Outside a sample the hooks cost one boolean check.
 */
public final class LagFinder {
    /** Read by the mixins on every call: keep it a plain field. */
    public static volatile boolean sampling;
    private static int ticksLeft, window;
    private static CommandSourceStack requester;
    static final Map<String, long[]> BLOCK_ENTITIES = new HashMap<>(), ENTITIES = new HashMap<>();
    /** key "dimension|chunkX|chunkZ" -> {nanos, redstone changes}. */
    static final Map<String, long[]> CHUNKS = new HashMap<>();

    public static boolean active() { return Admin.on() && Admin.config().lagFinder().enabled(); }

    public static boolean begin(CommandSourceStack source, int seconds) {
        if (sampling) return false;
        BLOCK_ENTITIES.clear(); ENTITIES.clear(); CHUNKS.clear();
        window = ticksLeft = seconds * 20;
        requester = source;
        sampling = true;
        return true;
    }
    public static void tick(MinecraftServer server) {
        if (!sampling || --ticksLeft > 0) return;
        sampling = false;
        var report = report(server, window / 20.0);
        if (requester != null) report.forEach(line -> requester.sendSuccess(() -> line, false));
        requester = null;
    }

    // ---- hooks (server thread)
    public static void blockEntity(Level level, BlockPos pos, String type, long nanos) {
        add(BLOCK_ENTITIES, type, nanos);
        chunk(level, pos.getX() >> 4, pos.getZ() >> 4)[0] += nanos;
    }
    public static void entity(Entity e, long nanos) {
        add(ENTITIES, net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), nanos);
        chunk(e.level(), e.getBlockX() >> 4, e.getBlockZ() >> 4)[0] += nanos;
    }
    public static void changed(Level level, BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWERED) || state.hasProperty(BlockStateProperties.POWER) || state.hasProperty(BlockStateProperties.LIT) && state.isSignalSource())
            chunk(level, pos.getX() >> 4, pos.getZ() >> 4)[1]++;
    }
    private static void add(Map<String, long[]> map, String key, long nanos) { var v = map.computeIfAbsent(key, k -> new long[2]); v[0] += nanos; v[1]++; }
    private static long[] chunk(Level level, int cx, int cz) { return CHUNKS.computeIfAbsent(level.dimension().identifier() + "|" + cx + "|" + cz, k -> new long[2]); }

    /** Tests and reports: what the last sample saw. */
    public static Set<String> entityTypes() { return Set.copyOf(ENTITIES.keySet()); }
    public static Set<String> blockEntityTypes() { return Set.copyOf(BLOCK_ENTITIES.keySet()); }
    /** Chunks ("dimension|x|z") above the clock threshold in a sample of {@code seconds}. */
    public static Set<String> clocks(double seconds) {
        var out = new HashSet<String>();
        int threshold = Admin.config().lagFinder().clockThreshold();
        CHUNKS.forEach((k, v) -> { if (v[1] / seconds >= threshold) out.add(k); });
        return out;
    }
    /** The report lines (also used by tests). */
    public static List<Component> report(MinecraftServer server, double seconds) {
        var out = new ArrayList<Component>();
        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        out.add(Component.translatable("simpletweaks.admin.lag.header", String.format(Locale.ROOT, "%.1f", seconds), String.format(Locale.ROOT, "%.2f", mspt),
                String.format(Locale.ROOT, "%.1f", Math.min(20.0, 1000.0 / Math.max(mspt, 0.001)))).withStyle(ChatFormatting.GOLD));
        top(out, "simpletweaks.admin.lag.block_entities", BLOCK_ENTITIES, seconds);
        top(out, "simpletweaks.admin.lag.entities", ENTITIES, seconds);
        var chunks = new ArrayList<>(CHUNKS.entrySet());
        chunks.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        out.add(Component.translatable("simpletweaks.admin.lag.chunks").withStyle(ChatFormatting.YELLOW));
        for (var e : chunks.subList(0, Math.min(5, chunks.size()))) {
            var k = e.getKey().split("\\|");
            int x = Integer.parseInt(k[1]) * 16 + 8, z = Integer.parseInt(k[2]) * 16 + 8;
            out.add(Component.translatable("simpletweaks.admin.lag.chunk", k[0], x, z, ms(e.getValue()[0], seconds), counts(server, k[0], x >> 4, z >> 4)));
        }
        int threshold = Admin.config().lagFinder().clockThreshold();
        for (var e : CHUNKS.entrySet()) {
            double perSecond = e.getValue()[1] / seconds;
            if (perSecond < threshold) continue;
            var k = e.getKey().split("\\|");
            out.add(Component.translatable("simpletweaks.admin.lag.clock", k[0], Integer.parseInt(k[1]) * 16 + 8, Integer.parseInt(k[2]) * 16 + 8,
                    String.format(Locale.ROOT, "%.0f", perSecond)).withStyle(ChatFormatting.RED));
        }
        return out;
    }
    private static void top(List<Component> out, String key, Map<String, long[]> map, double seconds) {
        out.add(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
        var list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        for (var e : list.subList(0, Math.min(5, list.size())))
            out.add(Component.translatable("simpletweaks.admin.lag.entry", e.getKey(), ms(e.getValue()[0], seconds), e.getValue()[1]));
    }
    /** ms per second of the window. */
    private static String ms(long nanos, double seconds) { return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0 / seconds); }
    private static String counts(MinecraftServer server, String dim, int cx, int cz) {
        for (var level : server.getAllLevels()) if (level.dimension().identifier().toString().equals(dim)) {
            var box = new net.minecraft.world.phys.AABB(cx * 16, level.getMinY(), cz * 16, cx * 16 + 16, level.getMaxY() + 1, cz * 16 + 16);
            int entities = level.getEntities((Entity) null, box, e -> true).size();
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            int blockEntities = chunk == null ? 0 : chunk.getBlockEntities().size();
            return entities + "/" + blockEntities;
        }
        return "?";
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.lagFinder().enabled()) return;
        d.register(Commands.literal("lag").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().lagFinder().permission()))
                .executes(c -> start(c.getSource(), Admin.config().lagFinder().defaultSeconds()))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 30)).executes(c -> start(c.getSource(), IntegerArgumentType.getInteger(c, "seconds")))));
    }
    static int start(CommandSourceStack s, int seconds) {
        if (!begin(s, seconds)) { s.sendFailure(Component.translatable("simpletweaks.admin.lag.busy")); return 0; }
        s.sendSuccess(() -> Component.translatable("simpletweaks.admin.lag.started", seconds), false);
        return 1;
    }
    private LagFinder() {}
}
