package com.simplebuilding.advancement;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player counts behind the counter advancements ({@link CounterTrigger}, {@code simplebuilding:counter}).
 *
 * <p>The counts sit in the player's own save data ({@link Holder}, implemented by
 * {@code AdvancementHooksMixin} on {@code ServerPlayer}, NBT key {@value #NBT_KEY}); they survive
 * death, dimension changes and relogs, and they are independent of vanilla statistics. Every
 * {@link #add} fires the trigger with the new total, so a criterion is met the moment the count
 * reaches its threshold. Client and non-server players count nothing.
 */
public final class ModCounters {
    private ModCounters() {
    }

    /** Blocks the building wand placed (area build, octant fill, blueprint build). */
    public static final String WAND_BLOCKS = "wand_blocks";
    /** Blocks a sledgehammer broke around the block that was hit. */
    public static final String HAMMER_BLOCKS = "hammer_blocks";
    /** Steps a chisel moved a block along its chain. */
    public static final String CHISEL_STEPS = "chisel_steps";

    public static final List<String> ALL = List.of(WAND_BLOCKS, HAMMER_BLOCKS, CHISEL_STEPS);

    /** Key of the compound in the player's save data. */
    public static final String NBT_KEY = "SimpleBuildingCounters";

    /** Implemented by the {@code ServerPlayer} mixin: the player's mutable counts. */
    public interface Holder {
        Map<String, Long> simplebuilding$counters();
    }

    /** Adds {@code amount} (ignored if not positive) to the player's {@code counter} and fires the trigger. */
    public static void add(Player player, String counter, long amount) {
        if (amount <= 0 || !(player instanceof ServerPlayer serverPlayer) || !(player instanceof Holder holder)) {
            return;
        }
        long total = holder.simplebuilding$counters().merge(counter, amount, Long::sum);
        ModTriggers.COUNTER.trigger(serverPlayer, counter, total);
    }

    /** The player's current count; 0 for an unknown counter or a player without counts. */
    public static long get(Player player, String counter) {
        return player instanceof Holder holder ? holder.simplebuilding$counters().getOrDefault(counter, 0L) : 0L;
    }

    /** Writes the counts (only positive ones) to a compound. */
    public static CompoundTag write(Map<String, Long> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach((name, value) -> {
            if (value > 0) {
                tag.putLong(name, value);
            }
        });
        return tag;
    }

    /** Replaces {@code counts} with the counts stored in {@code tag}. */
    public static void read(CompoundTag tag, Map<String, Long> counts) {
        counts.clear();
        for (String name : tag.keySet()) {
            long value = tag.getLongOr(name, 0L);
            if (value > 0) {
                counts.put(name, value);
            }
        }
    }
}
