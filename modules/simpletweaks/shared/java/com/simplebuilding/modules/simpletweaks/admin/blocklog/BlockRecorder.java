package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.AdminCodecs;
import java.util.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What the mixins hand to the block log: breaks, placements and container differences. Server thread only. */
public final class BlockRecorder {
    /** Block entity data larger than this is not kept (the block itself still is). */
    static final int MAX_BLOCK_ENTITY_SNBT = 32768;

    /** Before a player breaks a block: its state and block entity data (or null when the log is off). */
    public static String[] before(ServerLevel level, BlockPos pos) {
        if (!BlockLog.active()) return null;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return null;
        String data = "";
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            try {
                String snbt = be.saveWithFullMetadata(level.registryAccess()).toString();
                if (snbt.length() <= MAX_BLOCK_ENTITY_SNBT) data = snbt;
            } catch (RuntimeException ignored) { /* a broken block entity is not worth losing the entry */ }
        }
        return new String[]{BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), BlockStateParser.serialize(state) + (data.isEmpty() ? "" : "\u0001" + data)};
    }
    public static void broke(ServerPlayer player, BlockPos pos, String[] before) {
        if (before == null) return;
        BlockLog.record(player.level(), player, 'B', pos, before[0], 1, before[1]);
    }
    public static void placed(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!BlockLog.active() || player == null) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        BlockLog.record(level, player, 'P', pos, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1, BlockStateParser.serialize(state));
    }

    // ---- containers: snapshot when a menu opens, difference when it closes ----
    private record Ref(BlockEntity be, Container container, int slot) {}
    private record Snapshot(List<Ref> refs, List<ItemStack> before) {}
    private static final Map<ServerPlayer, Snapshot> OPEN = new WeakHashMap<>();

    public static void opened(ServerPlayer player) {
        OPEN.remove(player);
        if (!BlockLog.active() || player.containerMenu == player.inventoryMenu) return;
        var refs = new ArrayList<Ref>();
        var seen = new HashSet<String>();
        for (var slot : player.containerMenu.slots) {
            var ref = resolve(slot.container, slot.getContainerSlot());
            if (ref != null && seen.add(System.identityHashCode(ref.container) + ":" + ref.slot)) refs.add(ref);
        }
        if (refs.isEmpty()) return;
        var before = new ArrayList<ItemStack>(refs.size());
        for (var r : refs) before.add(r.container.getItem(r.slot).copy());
        OPEN.put(player, new Snapshot(refs, before));
    }
    private static Ref resolve(Container container, int slot) {
        if (container instanceof CompoundContainer compound) {
            var a = (com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimCompoundAccessor) compound;
            int size = a.claims$first().getContainerSize();
            return slot < size ? resolve(a.claims$first(), slot) : resolve(a.claims$second(), slot - size);
        }
        if (container instanceof BlockEntity be && be.getLevel() instanceof ServerLevel && slot >= 0 && slot < container.getContainerSize()) return new Ref(be, container, slot);
        return null;
    }

    public static void closed(ServerPlayer player) {
        var snap = OPEN.remove(player);
        if (snap == null || !BlockLog.active()) return;
        // per block entity: item prototype -> count before minus after
        var byBlock = new LinkedHashMap<BlockEntity, List<Object[]>>();
        for (int i = 0; i < snap.refs.size(); i++) {
            var r = snap.refs.get(i);
            if (r.be.isRemoved()) continue;
            var list = byBlock.computeIfAbsent(r.be, k -> new ArrayList<>());
            add(list, snap.before.get(i), 1);
            add(list, r.container.getItem(r.slot), -1);
        }
        for (var e : byBlock.entrySet()) {
            var level = (ServerLevel) e.getKey().getLevel();
            for (Object[] item : e.getValue()) {
                int delta = (int) item[1];
                if (delta == 0) continue;
                ItemStack proto = (ItemStack) item[0];
                String extra = proto.getComponentsPatch().isEmpty() ? "" : AdminCodecs.snbt(level.registryAccess(), proto);
                BlockLog.record(level, player, delta > 0 ? 'T' : 'D', e.getKey().getBlockPos(), BuiltInRegistries.ITEM.getKey(proto.getItem()).toString(), Math.abs(delta), extra);
            }
        }
    }
    private static void add(List<Object[]> list, ItemStack stack, int sign) {
        if (stack.isEmpty()) return;
        for (Object[] item : list) if (ItemStack.isSameItemSameComponents((ItemStack) item[0], stack)) { item[1] = (int) item[1] + sign * stack.getCount(); return; }
        list.add(new Object[]{stack.copyWithCount(1), sign * stack.getCount()});
    }
    private BlockRecorder() {}
}
