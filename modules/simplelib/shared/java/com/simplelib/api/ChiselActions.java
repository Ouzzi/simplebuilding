package com.simplelib.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The chisel contract (WE25-7, docs/ai/PLAN-B-CHISELS07-2026-11-07.md): Simple Chisels tells partners what its chisels did
 * and publishes their transformation tables, partners plug their own chisel ways in - nobody imports the chisel mod. Without
 * a partner nothing is registered and the chisels only reshape along their tables. Content-free: only Vanilla types.
 *
 * <ul>
 *   <li>{@link #registerUse}/{@link #use}: a partner's right-click way before the table (SimpleBuilding: carving stripped logs
 *       with a pottery sherd in the off hand).</li>
 *   <li>{@link #listen}/{@link #fire}: what a chisel did ({@link #CHISEL}: one block reshaped; SimpleBuilding's statistic,
 *       advancement trigger and counter).</li>
 *   <li>{@link #publishTools}/{@link #tools}: the chisels with their effective maps, for a partner's wiki export, recipe viewer
 *       and test centre.</li>
 * </ul>
 * Rights are asked through framework {@code Protection} and {@link HammerActions#mayChange} (the partner permission every
 * reshaping tool shares).
 */
public final class ChiselActions {
    /** A chisel reshaped one block along its table. */
    public static final String CHISEL = "chisel";

    /** A partner's right-click way: {@code null} if it does not apply (the chisel carries on with its table). */
    @FunctionalInterface
    public interface Use {
        @Nullable InteractionResult use(UseOnContext context, int cooldownTicks, int damage);
    }

    /**
     * One chisel as the partner sees it: the maps of its tier (chisel forward/back, with Constructor's Touch forward/back),
     * its cooldown before Fast Chiseling, whether it only works backwards, and the durability per step forward/back.
     */
    public record Tool(Item item, Map<Block, Block> forward, Map<Block, Block> backward, Map<Block, Block> touchForward,
                       Map<Block, Block> touchBackward, int cooldownTicks, boolean spatula, int damage, int reverseDamage) {}

    private static final List<Use> USES = new CopyOnWriteArrayList<>();
    private static final Map<String, List<Consumer<ServerPlayer>>> LISTENERS = new ConcurrentHashMap<>();
    private static volatile Supplier<List<Tool>> tools = List::of;

    private ChiselActions() {}

    public static void registerUse(Use use) {
        USES.add(use);
    }

    /** The first registered way that handles the click, or {@code null}. */
    public static @Nullable InteractionResult use(UseOnContext context, int cooldownTicks, int damage) {
        for (Use use : USES) {
            InteractionResult result = use.use(context, cooldownTicks, damage);
            if (result != null) return result;
        }
        return null;
    }

    public static void listen(String event, Consumer<ServerPlayer> listener) {
        LISTENERS.computeIfAbsent(event, e -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Tells the listeners of {@code event}; only server players count. */
    public static void fire(String event, @Nullable Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        for (Consumer<ServerPlayer> listener : LISTENERS.getOrDefault(event, List.of())) listener.accept(server);
    }

    /** The chisel mod hands over its chisels (asked each time, so the tables are the current ones). */
    public static void publishTools(Supplier<List<Tool>> supplier) {
        tools = supplier;
    }

    /** The published chisels, weakest first; empty without a chisel mod. */
    public static List<Tool> tools() {
        return tools.get();
    }
}
