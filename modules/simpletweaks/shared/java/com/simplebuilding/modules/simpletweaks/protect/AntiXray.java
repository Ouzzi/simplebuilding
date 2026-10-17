package com.simplebuilding.modules.simpletweaks.protect;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Anti-X-Ray (plan docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md, like Paper's engine modes 1 and 2): the chunk data sent to a
 * player shows every hidden block (ores) that touches no non-solid block as its host stone; mode 2 also turns enclosed host
 * blocks into fake ores. When a block changes, its neighbours are re-sent with their real state ({@link #revealAround}).
 * The client never receives an enclosed ore, so x-ray texture packs, x-ray mods and noclip cameras see stone.
 */
public final class AntiXray {
    /** Receiver of the chunk packet being built on this thread (set around {@code PlayerChunkSender.sendChunk}). */
    public static final ThreadLocal<ServerPlayer> RECEIVER = new ThreadLocal<>();
    /** How a hidden neighbour is re-sent; tests replace it to observe the reveal. */
    public static volatile BiConsumer<ServerLevel, BlockPos> reveal = (level, pos) -> level.getChunkSource().blockChanged(pos);

    /** Resolved lists of one dimension. */
    record Rules(Set<Block> hidden, Set<Block> hosts, BlockState stone, BlockState deepslate, int maxY, BlockState[] fakes, Set<Block> deepOres) {
        boolean concealable(BlockState s) { return hidden.contains(s.getBlock()) || hosts.contains(s.getBlock()); }
    }
    private static final Map<ResourceKey<Level>, Optional<Rules>> RULES = new HashMap<>();
    private static ProtectConfig resolvedFor;

    /** Drops the resolved lists (config change, reload, server start); they are rebuilt on first use. */
    public static synchronized void invalidate() { RULES.clear(); resolvedFor = null; }

    static synchronized Rules rules(Level level) {
        var c = Protect.config();
        if (c != resolvedFor) { RULES.clear(); resolvedFor = c; }
        return RULES.computeIfAbsent(level.dimension(), k -> Optional.ofNullable(resolve(c.antiXray(), k.identifier().toString()))).orElse(null);
    }
    private static Rules resolve(ProtectConfig.AntiXray c, String dimension) {
        var d = c.dimensions().get(dimension);
        if (d == null) d = c.dimensions().get("*");
        if (d == null) return null;
        var hidden = blocks(d.hidden());
        if (hidden.isEmpty()) return null;
        var stone = block(d.replacement());
        var deep = d.deepslateReplacement().isEmpty() ? null : block(d.deepslateReplacement());
        if (stone == null) return null;
        var hosts = new ReferenceOpenHashSet<Block>();
        if (c.engineMode() == 2) { hosts.add(stone.getBlock()); if (deep != null) hosts.add(deep.getBlock()); }
        var fakes = blocks(d.fakeOres().isEmpty() ? d.hidden() : d.fakeOres()).stream().map(Block::defaultBlockState).toArray(BlockState[]::new);
        hidden.remove(stone.getBlock());
        if (deep != null) hidden.remove(deep.getBlock());
        var deepOres = new ReferenceOpenHashSet<Block>();
        for (var b : hidden) if (BuiltInRegistries.BLOCK.getKey(b).getPath().contains("deepslate")) deepOres.add(b);
        return new Rules(hidden, hosts, stone, deep, d.maxY(), fakes, deepOres);
    }
    private static Set<Block> blocks(List<String> entries) {
        var out = new ReferenceOpenHashSet<Block>();
        for (var e : entries) {
            if (e.startsWith("#")) {
                var tag = TagKey.create(Registries.BLOCK, Identifier.parse(e.substring(1)));
                for (var b : BuiltInRegistries.BLOCK) if (b.defaultBlockState().is(tag)) out.add(b);
            } else BuiltInRegistries.BLOCK.getOptional(Identifier.parse(e)).ifPresent(out::add);
        }
        out.remove(net.minecraft.world.level.block.Blocks.AIR);
        return out;
    }
    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).map(Block::defaultBlockState).orElse(null);
    }

    /** Does the chunk data for {@code player} get obfuscated? (feature on, dimension covered, player not exempt) */
    public static boolean applies(ServerPlayer player, Level level) {
        if (player == null || !Protect.on()) return false;
        var c = Protect.config().antiXray();
        if (!c.enabled() || exempt(player, c)) return false;
        return rules(level) != null;
    }
    public static boolean exempt(ServerPlayer player, ProtectConfig.AntiXray c) {
        var mode = player.gameMode.getGameModeForPlayer();
        if (c.exemptCreative() && mode == GameType.CREATIVE) return true;
        if (c.exemptSpectator() && mode == GameType.SPECTATOR) return true;
        return c.exemptPermission() >= 0 && ProtectPerms.has(player, c.exemptPermission());
    }

    /** The serialized sections of {@code chunk} as the client should see them (same layout as Vanilla's buffer). */
    public static byte[] obfuscate(LevelChunk chunk) {
        var rules = rules(chunk.getLevel());
        var sections = chunk.getSections();
        var out = sections;
        if (rules != null) {
            for (int i = 0; i < sections.length; i++) {
                var changed = section(chunk, sections, i, rules);
                if (changed != null) {
                    if (out == sections) out = sections.clone();
                    out[i] = changed;
                }
            }
        }
        int size = 0;
        for (var s : out) size += s.getSerializedSize();
        var bytes = new byte[size];
        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        buf.writerIndex(0);
        for (var s : out) s.write(buf);
        return bytes;
    }

    /** Copy of section {@code index} with enclosed hidden blocks replaced, or null when nothing changes. */
    private static LevelChunkSection section(LevelChunk chunk, LevelChunkSection[] sections, int index, Rules rules) {
        var section = sections[index];
        int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
        if (section.hasOnlyAir() || baseY > rules.maxY()) return null;
        if (!section.maybeHas(rules::concealable)) return null;
        LevelChunkSection copy = null;
        double chance = rules.hosts().isEmpty() || rules.fakes().length == 0 ? -1 : Protect.config().antiXray().fakeOreChance();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        for (int y = 0; y < 16; y++) {
            int wy = baseY + y;
            if (wy > rules.maxY()) break;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                var state = section.getBlockState(x, y, z);
                boolean hidden = rules.hidden().contains(state.getBlock());
                if (!hidden && !rules.hosts().contains(state.getBlock())) continue;
                if (!enclosed(chunk, sections, index, x, y, z)) continue;
                BlockState shown;
                long hash = chance < 0 ? 0 : mix(baseX + x, wy, baseZ + z);
                if (chance > 0 && (hash >>> 11) * 0x1.0p-53 < chance)
                    shown = rules.fakes()[(int) Long.remainderUnsigned(hash, rules.fakes().length)];
                else if (!hidden) continue;
                else shown = host(rules, state, wy);
                if (shown == state) continue;
                if (copy == null) copy = section.copy();
                copy.setBlockState(x, y, z, shown, false);
            }
        }
        return copy;
    }
    static BlockState host(Rules rules, BlockState ore, int y) {
        if (rules.deepslate() == null) return rules.stone();
        return y < 0 || rules.deepOres().contains(ore.getBlock()) ? rules.deepslate() : rules.stone();
    }
    private static long mix(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33;
        return h;
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    /** True when all six neighbours are solid render blocks (sections of this chunk first, loaded neighbour chunks otherwise). */
    private static boolean enclosed(LevelChunk chunk, LevelChunkSection[] sections, int index, int x, int y, int z) {
        for (var d : DIRECTIONS) {
            var n = neighbour(chunk, sections, index, x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
            if (n == null || !n.isSolidRender()) return false;
        }
        return true;
    }
    private static BlockState neighbour(LevelChunk chunk, LevelChunkSection[] sections, int index, int x, int y, int z) {
        if (y < 0) { if (--index < 0) return null; y += 16; }
        else if (y > 15) { if (++index >= sections.length) return null; y -= 16; }
        if (x >= 0 && x < 16 && z >= 0 && z < 16) return sections[index].getBlockState(x, y, z);
        if (!(chunk.getLevel() instanceof ServerLevel level)) return null;
        var pos = chunk.getPos();
        var other = level.getChunkSource().getChunkNow(pos.x() + (x >> 4), pos.z() + (z >> 4));
        if (other == null) return null;
        var s = other.getSections();
        return index < s.length ? s[index].getBlockState(x & 15, y, z & 15) : null;
    }

    /** After a block change: neighbours that may be shown concealed are re-sent with their real state. */
    public static void revealAround(ServerLevel level, BlockPos pos) {
        if (!Protect.on() || !Protect.config().antiXray().enabled()) return;
        var rules = rules(level);
        if (rules == null || pos.getY() > rules.maxY() + 1) return;
        var m = new BlockPos.MutableBlockPos();
        for (var d : DIRECTIONS) {
            m.setWithOffset(pos, d);
            if (m.getY() > rules.maxY() || level.getChunkSource().getChunkNow(m.getX() >> 4, m.getZ() >> 4) == null) continue;
            if (rules.concealable(level.getBlockState(m))) reveal.accept(level, m.immutable());
        }
    }

    /** The chunk data Vanilla's own sender would build for {@code player} (tests and bench). */
    public static net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData packetFor(ServerPlayer player, LevelChunk chunk) {
        var old = RECEIVER.get();
        RECEIVER.set(player);
        try { return new net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData(chunk); }
        finally { if (old == null) RECEIVER.remove(); else RECEIVER.set(old); }
    }

    /** Cost of obfuscating {@code chunk} against Vanilla's own serialization, in nanoseconds {vanilla, obfuscated} over {@code rounds}. */
    public static long[] bench(LevelChunk chunk, int rounds) {
        long v = 0, o = 0;
        for (int i = 0; i < rounds; i++) {
            long t = System.nanoTime();
            new net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData(chunk);
            long t2 = System.nanoTime();
            obfuscate(chunk);
            long t3 = System.nanoTime();
            v += t2 - t; o += t3 - t2;
        }
        return new long[] { v, o };
    }
    private AntiXray() {}
}
