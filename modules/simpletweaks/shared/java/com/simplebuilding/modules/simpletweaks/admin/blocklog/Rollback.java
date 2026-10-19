package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.AdminCodecs;
import java.util.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Undoes logged actions, newest first: placed blocks are removed (only while the same block is still there), broken,
 * exploded and burned blocks come back (only onto air or replaceable blocks, with their block entity data), items taken
 * from a container go back in, deposited items are taken out again, edited signs get their old text back (only while the
 * new text is still there). Hopper moves (I/O) are never undone. A preview checks the same conditions and changes nothing.
 * The last real run is kept in memory, {@link #restore} plays it forward again. Server thread.
 */
public final class Rollback {
    public record Result(int undone, int skipped) {}
    /** One undone entry and, for removed placed blocks, their block entity data (so a restore brings the contents back). */
    private record Undone(BlockLog.Entry entry, String data) {}
    /** Placed/removed without drops and without the block entity's own clean-up (no item duplication). */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static List<Undone> last = List.of();

    public static Result apply(MinecraftServer server, List<BlockLog.Entry> newestFirst) { return apply(server, newestFirst, false); }

    /** {@code preview}: count what would be undone, change nothing (the restore buffer stays as it is). */
    public static Result apply(MinecraftServer server, List<BlockLog.Entry> newestFirst, boolean preview) {
        int done = 0, skipped = 0;
        var undone = new ArrayList<Undone>();
        for (var e : newestFirst) {
            ServerLevel level = level(server, e);
            if (level == null) { skipped++; continue; }
            String data = e.action() == 'P' && !preview ? blockEntityData(level, e) : null;
            boolean ok = switch (e.action()) {
                case 'P' -> unplace(level, e, preview);
                case 'B', 'X', 'F' -> unbreak(level, e, preview);
                case 'T' -> move(level, e, true, preview);
                case 'D' -> move(level, e, false, preview);
                case 'S' -> sign(level, e, true, preview);
                default -> false;
            };
            if (ok) { done++; if (!preview) undone.add(new Undone(e, data)); } else skipped++;
        }
        if (!preview) last = List.copyOf(undone);
        return new Result(done, skipped);
    }
    /** Server stop: a rollback is never restored into another world. */
    static void forget() { last = List.of(); }
    /** Changes the last rollback can bring back (0 = nothing to restore). */
    public static int restorable() { return last.size(); }

    /** Plays the last rollback forward again (oldest first) and forgets it. */
    public static Result restore(MinecraftServer server) {
        var list = new ArrayList<>(last);
        last = List.of();
        Collections.reverse(list);
        int done = 0, skipped = 0;
        for (var u : list) {
            var e = u.entry();
            ServerLevel level = level(server, e);
            boolean ok = level != null && switch (e.action()) {
                case 'P' -> replace(level, e, u.data());
                case 'B', 'X', 'F' -> unplace(level, e, false);
                case 'T' -> move(level, e, false, false);
                case 'D' -> move(level, e, true, false);
                case 'S' -> sign(level, e, false, false);
                default -> false;
            };
            if (ok) done++; else skipped++;
        }
        return new Result(done, skipped);
    }

    private static ServerLevel level(MinecraftServer server, BlockLog.Entry e) {
        var id = Identifier.tryParse(e.dimension());
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
    private static String blockEntityData(ServerLevel level, BlockLog.Entry e) {
        var be = level.getBlockEntity(e.pos());
        if (be == null) return null;
        try { return be.saveWithFullMetadata(level.registryAccess()).toString(); } catch (RuntimeException ex) { return null; }
    }
    private static boolean unplace(ServerLevel level, BlockLog.Entry e, boolean preview) {
        var state = level.getBlockState(e.pos());
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(e.id())) return false;
        return preview || level.setBlock(e.pos(), Blocks.AIR.defaultBlockState(), FLAGS | Block.UPDATE_NEIGHBORS);
    }
    private static boolean unbreak(ServerLevel level, BlockLog.Entry e, boolean preview) {
        var current = level.getBlockState(e.pos());
        if (!current.isAir() && !current.canBeReplaced()) return false;
        String extra = e.extra();
        int cut = extra.indexOf('\u0001');
        return setState(level, e, cut < 0 ? extra : extra.substring(0, cut), cut < 0 ? null : extra.substring(cut + 1), preview);
    }
    /** Restore of a removed placed block: its logged state plus the contents it had when the rollback removed it. */
    private static boolean replace(ServerLevel level, BlockLog.Entry e, String data) {
        var current = level.getBlockState(e.pos());
        if (!current.isAir() && !current.canBeReplaced()) return false;
        return setState(level, e, e.extra(), data, false);
    }
    private static boolean setState(ServerLevel level, BlockLog.Entry e, String stateText, String data, boolean preview) {
        try {
            var state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, stateText.isEmpty() ? e.id() : stateText, false).blockState();
            if (preview) return true;
            if (!level.setBlock(e.pos(), state, Block.UPDATE_ALL)) return false;
            if (data != null && !data.isEmpty()) {
                var be = level.getBlockEntity(e.pos());
                if (be != null) {
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), TagParser.parseCompoundFully(data)));
                    be.setChanged();
                    level.sendBlockUpdated(e.pos(), state, state, Block.UPDATE_CLIENTS);
                }
            }
            return true;
        } catch (Exception ex) { return false; }
    }
    /** {@code back}: old text again (rollback), else the new text again (restore); only while the other text is still there. */
    private static boolean sign(ServerLevel level, BlockLog.Entry e, boolean back, boolean preview) {
        if (!(level.getBlockEntity(e.pos()) instanceof SignBlockEntity sign)) return false;
        String[] f = e.extra().split("\u0001", -1);
        if (f.length != 3) return false;
        boolean front = f[0].equals("front");
        String want = back ? f[1] : f[2], expect = back ? f[2] : f[1];
        if (!BlockRecorder.signText(sign, front).equals(expect)) return false;
        if (preview) return true;
        String[] lines = want.split("\n", -1);
        sign.updateText(text -> {
            var m = text.asMutable();
            var old = text.getMessages(false);
            for (int i = 0; i < old.size(); i++) m.setLine(i, Component.literal(i < lines.length ? lines[i] : "").setStyle(old.get(i).getStyle()));
            return m.asImmutable();
        }, front ? SignTextSlot.FRONT : SignTextSlot.BACK);
        level.sendBlockUpdated(e.pos(), sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
    private static boolean move(ServerLevel level, BlockLog.Entry e, boolean putBack, boolean preview) {
        if (!(level.getBlockEntity(e.pos()) instanceof Container container)) return false;
        ItemStack proto = e.extra().isEmpty() ? new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(e.id()))) : AdminCodecs.fromSnbt(level.registryAccess(), e.extra());
        if (proto.isEmpty()) return false;
        if (preview) {
            if (putBack) return true;
            for (int i = 0; i < container.getContainerSize(); i++) if (ItemStack.isSameItemSameComponents(container.getItem(i), proto)) return true;
            return false;
        }
        int left = e.count();
        if (putBack) {
            for (int pass = 0; pass < 2 && left > 0; pass++) for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
                var in = container.getItem(i);
                if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, proto) && container.canPlaceItem(i, proto)) {
                    int n = Math.min(left, Math.min(in.getMaxStackSize(), container.getMaxStackSize()) - in.getCount());
                    if (n > 0) { in.grow(n); left -= n; }
                } else if (pass == 1 && in.isEmpty() && container.canPlaceItem(i, proto)) {
                    int n = Math.min(left, Math.min(proto.getMaxStackSize(), container.getMaxStackSize()));
                    container.setItem(i, proto.copyWithCount(n)); left -= n;
                }
            }
        } else {
            for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
                var in = container.getItem(i);
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, proto)) { int n = Math.min(left, in.getCount()); in.shrink(n); left -= n; if (in.isEmpty()) container.setItem(i, ItemStack.EMPTY); }
            }
        }
        container.setChanged();
        return left < e.count();
    }
    private Rollback() {}
}
