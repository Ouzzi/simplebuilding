package com.simplelib.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The gadget contract (OW24-2 G2, docs/ai/PLAN-B-GADGETS24-2026-10-24.md): Simple Gadgets (Transposer, Resonance Rod) tells
 * partners what its gadgets did and how they look, partners tell it how they decide - nobody imports the gadget mod. Without a
 * partner nothing is registered and the gadgets use their own answers. Content-free: only Vanilla types.
 *
 * <ul>
 *   <li>{@link #listen}/{@link #fire}: what a gadget did (advancements of SimpleBuilding).</li>
 *   <li>{@link #setFact}/{@link #fact}: numbers a gadget publishes for a partner's catalogue (charges, costs).</li>
 *   <li>{@link #registerItemHint}/{@link #itemHint}: a gadget item says "this click would transform the block" for the partner's
 *       hand-hint question, asked where the partner would ask its own tool.</li>
 *   <li>{@link #registerHandHint}/{@link #handHint}: client, the partner's full answer to "would this hand transform the aimed
 *       block" (the gadget's model property asks it; without a partner the gadget uses its own check).</li>
 * </ul>
 */
public final class GadgetActions {
    /** The Transposer turned or swapped a block. */
    public static final String ROTATE = "rotate";
    /** The Resonance Rod beam changed a block (melted, lit, dried, set alight). */
    public static final String LENS_BEAM = "lens_beam";
    /** The Resonance Rod beam primed TNT. */
    public static final String LASER_TNT = "laser_tnt";

    /** The partner's full hand-hint answer (client). */
    @FunctionalInterface
    public interface HandHint {
        boolean wouldTransform(net.minecraft.world.level.Level level, BlockHitResult hit, Player player, net.minecraft.world.InteractionHand hand);
    }

    private static final Map<String, List<Consumer<ServerPlayer>>> LISTENERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FACTS = new ConcurrentHashMap<>();
    private static final Map<Item, Predicate<UseOnContext>> ITEM_HINTS = new ConcurrentHashMap<>();
    private static final List<HandHint> HAND_HINTS = new CopyOnWriteArrayList<>();

    private GadgetActions() {}

    public static void listen(String event, Consumer<ServerPlayer> listener) {
        LISTENERS.computeIfAbsent(event, e -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Tells the listeners of {@code event}; only server players count. */
    public static void fire(String event, @Nullable Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        for (Consumer<ServerPlayer> listener : LISTENERS.getOrDefault(event, List.of())) listener.accept(server);
    }

    public static void setFact(String key, int value) {
        FACTS.put(key, value);
    }

    /** A published number, or {@code fallback} while no gadget published it. */
    public static int fact(String key, int fallback) {
        return FACTS.getOrDefault(key, fallback);
    }

    /** Registers the hint of {@code item}: whether a right click with this context would transform the aimed block now. */
    public static void registerItemHint(Item item, Predicate<UseOnContext> hint) {
        ITEM_HINTS.put(item, hint);
    }

    /** Whether the registered hint of {@code item} says yes; false for an item without one. */
    public static boolean itemHint(Item item, UseOnContext context) {
        Predicate<UseOnContext> hint = ITEM_HINTS.get(item);
        return hint != null && hint.test(context);
    }

    public static void registerHandHint(HandHint hint) {
        HAND_HINTS.add(hint);
    }

    /** The first partner answer, or {@code null} when no partner registered one. */
    public static @Nullable Boolean handHint(net.minecraft.world.level.Level level, BlockHitResult hit, Player player, net.minecraft.world.InteractionHand hand) {
        for (HandHint hint : HAND_HINTS) return hint.wouldTransform(level, hit, player, hand);
        return null;
    }
}
