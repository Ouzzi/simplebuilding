package com.simplebuilding.modules.simplechallenges.skyline;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.core.Gate;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The server side of the Skyline Block mode (S6/S7). All work is event driven: a chunk gets its blocks (and tells the eggs) when it
 * is generated, a broken line block is queued by the block-change hook and put back at the end of the same tick, a player who changes
 * dimension is put on the first block of the new line, and the seal opens when it is hit with the right tool. There is no scan: while
 * nothing happens, the only per-tick work is looking at two empty queues. Every entry starts with the same check as the unlock
 * hooks: no running skyline, no effect.
 */
public final class SkylineService {
    /** Flags for block changes of the line: tell the clients, but no neighbour updates (so nothing next to a line block pops off). */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Test seam: the overworld and nether line start here, the End line too (null = the real origins, see {@link SkylinePlans#build}). */
    public static volatile BlockPos originOverride;

    private record Regrow(ServerLevel level, BlockPos pos) {}

    private record EggJob(ServerLevel level, SkylinePlans.Dim dim, Eggs.Site site) {}

    private static final Queue<Regrow> REGROW = new ConcurrentLinkedQueue<>();
    private static final Queue<EggJob> EGG_JOBS = new ConcurrentLinkedQueue<>();

    private SkylineService() {}

    private static Active running() {
        Active a = ChallengeRuntime.server();
        return a == null || a.skyline == null || !SimpleChallenges.on() ? null : a;
    }

    /** A challenge ended or the server stopped: forget queued work. */
    public static void clear() {
        REGROW.clear();
        EGG_JOBS.clear();
    }

    /** Puts a block with its second half and support into a loaded level (flags 2|16). Parts that already stand are left alone. */
    public static void place(ServerLevel level, BlockPos pos, Block block) {
        for (Support.Part p : Support.parts(block)) {
            BlockPos at = pos.offset(p.dx(), p.dy(), p.dz());
            if (level.getBlockState(at).getBlock() != p.state().getBlock()) level.setBlock(at, p.state(), FLAGS);
        }
    }

    /**
     * Puts the line blocks of one chunk into a freshly generated chunk (no updates: the chunk is being built). Walks the 8 x 8 cells of
     * the chunk and asks the layout, so it costs 64 index calculations (linear: 8) however long the line is.
     * @return how many line blocks were placed
     */
    public static int fillChunk(SkylinePlans.Dim d, LevelChunk chunk) {
        if (d.oy < chunk.getMinY() || d.oy > chunk.getMaxY()) return 0;
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int cx0 = Math.floorDiv(minX - d.ox + 1, 2), cx1 = Math.floorDiv(minX + 15 - d.ox, 2);
        int cz0 = Math.floorDiv(minZ - d.oz + 1, 2), cz1 = Math.floorDiv(minZ + 15 - d.oz, 2);
        int placed = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                int idx = d.layout.index(cx, cz);
                if (idx < 0 || idx >= d.size()) continue;
                if (d.isSeal(idx)) {
                    Active a = ChallengeRuntime.server();
                    if (a != null && a.state.sealOpened) continue;
                }
                int x = d.ox + 2 * cx, z = d.oz + 2 * cz;
                for (Support.Part p : Support.parts(d.blockAt(idx))) {
                    at.set(x + p.dx(), d.oy + p.dy(), z + p.dz());
                    if (at.getY() >= chunk.getMinY() && at.getY() <= chunk.getMaxY()) chunk.setBlockState(at, p.state(), 0);
                }
                placed++;
            }
        }
        return placed;
    }

    /** A new skyline challenge starts: the chunks round the origin that exist already (spawn chunks) get their blocks now. */
    public static void start(Active a) {
        if (a.skyline == null || originOverride != null) return;
        ServerLevel ow = a.overworld();
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                LevelChunk c = ow.getChunkSource().getChunkNow(cx, cz);
                if (c != null) fillChunk(a.skyline.overworld(), c);
            }
        }
    }

    /** CHUNK_GENERATE: the line blocks of the chunk, and a note for an egg whose anchor lies in it. */
    public static void chunkGenerated(ServerLevel level, LevelChunk chunk) {
        Active a = running();
        if (a == null) return;
        SkylinePlans.Dim d = a.skyline.of(level.dimension());
        if (d == null) return;
        fillChunk(d, chunk);
        if (!a.skyline.mode.hasEggs()) return;
        for (Eggs.Site s : Eggs.sites(d, true)) {
            if (Eggs.inChunk(s, chunk.getPos().x(), chunk.getPos().z()) && !a.state.eggs.contains(Eggs.key(d, s))) EGG_JOBS.add(new EggJob(level, d, s));
        }
    }

    /** Level.setBlock hook (after the change): a line block that is gone or replaced is queued to grow back. */
    public static void changed(Level level, BlockPos pos, BlockState now) {
        Active a = ChallengeRuntime.server();
        if (a == null || a.skyline == null || level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        SkylinePlans.Dim d = a.skyline.of(sl.dimension());
        if (d == null || pos.getY() != d.oy) return;
        int idx = d.indexAt(pos.getX(), pos.getY(), pos.getZ());
        if (idx < 0 || (d.isSeal(idx) && a.state.sealOpened)) return;
        if (now.getBlock() != d.blockAt(idx)) REGROW.add(new Regrow(sl, pos.immutable()));
    }

    /** Every tick: grows back what was broken (at once, before the tick ends), builds one egg site, and every two seconds refills Elytra and keys. */
    public static void tick(Active a) {
        if (a.skyline == null) return;
        Regrow r;
        while ((r = REGROW.poll()) != null) {
            SkylinePlans.Dim d = a.skyline.of(r.level().dimension());
            if (d == null || !r.level().hasChunkAt(r.pos())) continue;
            int idx = d.indexAt(r.pos().getX(), r.pos().getY(), r.pos().getZ());
            if (idx < 0 || (d.isSeal(idx) && a.state.sealOpened)) continue;
            place(r.level(), r.pos(), d.blockAt(idx));
        }
        EggJob job = EGG_JOBS.poll();
        if (job != null && a.state.eggs.add(Eggs.key(job.dim(), job.site()))) {
            Eggs.build(job.level(), job.site());
            a.state.setDirty();
        }
        if (a.server.getTickCount() % 40 == 15 && !a.state.eggs.isEmpty()) {
            for (SkylinePlans.Dim d : new SkylinePlans.Dim[]{a.skyline.overworld(), a.skyline.nether(), a.skyline.end()}) {
                ServerLevel level = a.server.getLevel(d.key);
                if (level == null) continue;
                for (Eggs.Site s : Eggs.sites(d, a.skyline.mode.hasEggs())) if (a.state.eggs.contains(Eggs.key(d, s))) Eggs.keep(level, s);
            }
        }
    }

    /**
     * AttackBlockCallback: hitting the seal at the end of the Nether line with a Netherite pickaxe while holding an Eye of Ender in the
     * other hand opens it (the eye is used up, the pickaxe takes damage) and an End portal stands next to it.
     * @return true when the hit was handled
     */
    public static boolean attack(Player player, Level level, BlockPos pos) {
        Active a = running();
        if (a == null || level.isClientSide() || !(level instanceof ServerLevel sl)) return false;
        SkylinePlans.Dim d = a.skyline.of(sl.dimension());
        if (d == null || !d.seal || pos.getY() != d.oy) return false;
        int idx = d.indexAt(pos.getX(), pos.getY(), pos.getZ());
        if (!d.isSeal(idx) || a.state.sealOpened) return false;
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
        if (!main.is(Items.NETHERITE_PICKAXE) || !off.is(Items.ENDER_EYE)) {
            if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("simplechallenges.msg.seal_hint"));
            return false;
        }
        a.state.sealOpened = true; // first: the block change below must not grow the seal back
        a.state.setDirty();
        sl.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Eggs.buildPortal(sl, pos);
        if (!Gate.creativeCheck.test(player)) {
            off.shrink(1);
            main.hurtAndBreak(8, player, InteractionHand.MAIN_HAND);
        }
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("simplechallenges.msg.seal_opened"));
        return true;
    }

    /** AFTER_PLAYER_CHANGE_WORLD: a player who arrives in a dimension with a line stands on its first block (no platform, no portal destination of its own). */
    public static void arrived(ServerPlayer player, ServerLevel to) {
        Active a = running();
        if (a == null) return;
        SkylinePlans.Dim d = a.skyline.of(to.dimension());
        if (d == null) return;
        if (to.dimension() == Level.END) clearPlatform(to, d, new BlockPos(100, 48, 0));
        BlockPos start = d.start();
        place(to, start, d.blockAt(0));
        player.connection.teleport(start.getX() + 0.5, start.getY() + 1, start.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** Takes Vanilla's 5 x 5 obsidian arrival platform away (it lies at floor level of the End line) and puts the line blocks that stood there back. */
    public static void clearPlatform(ServerLevel end, SkylinePlans.Dim d, BlockPos floorCenter) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos at = floorCenter.offset(dx, 0, dz);
                if (end.getBlockState(at).is(Blocks.OBSIDIAN)) end.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos at = floorCenter.offset(dx, 0, dz);
                int idx = d.indexAt(at.getX(), at.getY(), at.getZ());
                if (idx >= 0) place(end, at, d.blockAt(idx));
            }
        }
    }
}
