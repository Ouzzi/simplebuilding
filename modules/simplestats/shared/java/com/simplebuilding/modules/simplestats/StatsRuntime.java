package com.simplebuilding.modules.simplestats;

import com.simplebuilding.modules.simplestats.StatsPlayer.State;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatData.Delta;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side counting. Hooks (mixins) call the {@code on...} methods; everything here is event driven. Changes of one item
 * collect in a {@link Delta} and are written into the item component once at the end of the server tick (a hammer breaking 25
 * blocks makes one write); distances and wearing time are handed to the items every {@link #ACCRUE_TICKS} ticks.
 */
public final class StatsRuntime {
    public static final int ACCRUE_TICKS = 200;
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final IdentityHashMap<ItemStack, Delta> PENDING = new IdentityHashMap<>();
    /** Number of times an item component was written (for tests: bundling). */
    public static long writes;

    private StatsRuntime() {}

    // ------------------------------------------------------------------ gates

    /** Whether this player's actions count right now (switch, creative, spectator, machine players). */
    public static boolean counts(ServerPlayer player) {
        StatsConfig config = SimpleStats.config();
        if (!config.enabled || StatsComponents.ITEM_STATS == null) return false;
        if (player.getClass() != ServerPlayer.class && !config.countFakePlayers) return false;
        return config.countCreative || !(player.isCreative() || player.isSpectator());
    }

    // ------------------------------------------------------------------ buffer

    /** The pending changes of this exact stack object (flushed at the end of the tick). */
    public static Delta delta(ItemStack stack) {
        return PENDING.computeIfAbsent(stack, s -> new Delta());
    }

    /** Writes the changes into the item now (for stacks that may be copied before the tick ends: thrown tridents, fired bows). */
    public static void applyNow(ItemStack stack, Delta delta) {
        commit(stack, delta);
    }

    /** Writes all pending changes; cheap when nothing happened. */
    public static void flush() {
        if (PENDING.isEmpty()) return;
        List<Map.Entry<ItemStack, Delta>> all = new ArrayList<>(PENDING.entrySet());
        PENDING.clear();
        for (var e : all) commit(e.getKey(), e.getValue());
    }

    private static void commit(ItemStack stack, Delta delta) {
        if (stack.isEmpty() || delta.isEmpty() || StatsComponents.ITEM_STATS == null) return;
        StatData current = StatsComponents.of(stack);
        if (!current.writable()) return;
        if (current.uid() == 0L) current = current.withUid(ThreadLocalRandom.current().nextLong() | 1L);
        StatData next = current.apply(delta, SimpleStats.budget(), System.currentTimeMillis() / 86_400_000L);
        if (!next.equals(StatsComponents.of(stack))) {
            stack.set(StatsComponents.ITEM_STATS, next);
            writes++;
        }
    }

    // ------------------------------------------------------------------ server tick / lifecycle

    /** End of every server tick. */
    public static void tick(MinecraftServer server) {
        flush();
        if (server.getTickCount() % ACCRUE_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            accrue(player);
        }
        flush();
    }

    /** A player leaves: hand over what is collected, then write everything. */
    public static void onLeave(ServerPlayer player) {
        accrue(player);
        stopFlight(player);
        flush();
    }

    private static State state(ServerPlayer player) {
        return ((StatsPlayer) player).simplestats$state();
    }

    /** Hands distances and wearing time since the last hand-over to the items the player wears now. */
    public static void accrue(ServerPlayer player) {
        State st = state(player);
        long now = player.level().getGameTime();
        long ticks = st.lastAccrue < 0 ? 0 : Math.max(0, Math.min(now - st.lastAccrue, 4 * ACCRUE_TICKS));
        st.lastAccrue = now;
        StatsConfig config = SimpleStats.config();
        boolean any = st.walked + st.sprinted + st.sneaked + st.swum + st.flown > 0;
        if (!counts(player)) {
            st.walked = st.sprinted = st.sneaked = st.swum = st.flown = 0;
            return;
        }
        if (config.trackArmor && ticks > 0) {
            for (EquipmentSlot slot : WORN) {
                ItemStack piece = player.getItemBySlot(slot);
                if (StatItems.worn(piece, slot)) delta(piece).add(StatKeys.WORN_TIME, ticks);
            }
        }
        if (config.trackDistances && any) {
            ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
            if (StatItems.boots(feet)) {
                delta(feet).add(StatKeys.DISTANCE_WALKED, st.walked).add(StatKeys.DISTANCE_SPRINTED, st.sprinted)
                        .add(StatKeys.DISTANCE_SNEAKED, st.sneaked).add(StatKeys.DISTANCE_SWUM, st.swum);
            }
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (st.flown > 0 && StatItems.glider(chest)) delta(chest).add(StatKeys.DISTANCE_FLOWN, st.flown);
        }
        st.walked = st.sprinted = st.sneaked = st.swum = st.flown = 0;
    }

    // ------------------------------------------------------------------ distances and flights

    /** Called for every vanilla statistic the player earns; only movement distances matter. */
    public static void onStat(ServerPlayer player, Identifier stat, int amount) {
        if (amount <= 0 || !SimpleStats.config().trackDistances) return;
        State st = state(player);
        if (stat.equals(Stats.WALK_ONE_CM)) st.walked += amount;
        else if (stat.equals(Stats.SPRINT_ONE_CM)) st.sprinted += amount;
        else if (stat.equals(Stats.CROUCH_ONE_CM)) st.sneaked += amount;
        else if (stat.equals(Stats.SWIM_ONE_CM)) st.swum += amount;
        else if (stat.equals(Stats.AVIATE_ONE_CM)) {
            st.flown += amount;
            st.flightCm += amount;
        }
    }

    public static void startFlight(ServerPlayer player) {
        State st = state(player);
        st.flightCm = 0;
        st.flying = true;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (counts(player) && SimpleStats.config().trackDistances && StatItems.glider(chest)) delta(chest).add(StatKeys.FLIGHTS, 1);
    }

    public static void stopFlight(ServerPlayer player) {
        State st = state(player);
        if (!st.flying) return;
        st.flying = false;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (counts(player) && SimpleStats.config().trackDistances && StatItems.glider(chest)) {
            delta(chest).add(StatKeys.DISTANCE_FLOWN, st.flown).record(StatKeys.R_LONGEST_FLIGHT, st.flightCm / 100);
            st.flown = 0;
        }
        st.flightCm = 0;
    }

    public static void onRocket(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (counts(player) && SimpleStats.config().trackDistances && StatItems.glider(chest)) delta(chest).add(StatKeys.ROCKETS_USED, 1);
    }

    // ------------------------------------------------------------------ tools

    public static void onMine(ServerPlayer player, ItemStack tool, BlockState state) {
        if (!SimpleStats.config().trackMining || !StatItems.mines(tool) || !counts(player)) return;
        delta(tool).add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1);
    }

    /** An axe turned a log into its stripped form, a hoe tilled soil, flint and steel lit something. */
    public static void onUse(ServerPlayer player, ItemStack tool, String action) {
        if (!SimpleStats.config().trackTools || tool.isEmpty() || !counts(player)) return;
        delta(tool).add(action, 1);
    }

    public static void onShear(ServerPlayer player, ItemStack shears, Entity target) {
        if (!SimpleStats.config().trackTools || !StatItems.shears(shears) || !counts(player)) return;
        delta(shears).add(StatKeys.SHEARED, StatKeys.L_SHEARED, MobKeys.variant(target), 1);
    }

    public static void onBrushed(ServerPlayer player, ItemStack brush, BlockState state) {
        if (!SimpleStats.config().trackTools || !StatItems.brush(brush) || !counts(player)) return;
        delta(brush).add(StatKeys.BRUSHED, StatKeys.L_BRUSHED, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1);
    }

    public static void onCast(ServerPlayer player, ItemStack rod) {
        if (!SimpleStats.config().trackFishing || !StatItems.rod(rod) || !counts(player)) return;
        delta(rod).add(StatKeys.CASTS, 1);
    }

    /** Caught items; the fish count in the fish category, a short fixed list of vanilla treasure items in treasure, the rest is junk. */
    public static void onCatch(ServerPlayer player, ItemStack rod, Iterable<ItemStack> items) {
        if (!SimpleStats.config().trackFishing || !StatItems.rod(rod) || !counts(player)) return;
        Delta d = delta(rod);
        for (ItemStack caught : items) {
            if (caught.isEmpty()) continue;
            String kind = caught.is(net.minecraft.tags.ItemTags.FISHES) ? "fish" : isTreasure(caught) ? "treasure" : "junk";
            long count = caught.getCount();
            d.add(StatKeys.CATCHES, StatKeys.L_CATCHES, BuiltInRegistries.ITEM.getKey(caught.getItem()).toString(), count).entry(StatKeys.L_CATCH_KINDS, kind, count);
        }
    }

    private static boolean isTreasure(ItemStack s) {
        return s.is(Items.BOW) || s.is(Items.ENCHANTED_BOOK) || s.is(Items.NAME_TAG) || s.is(Items.NAUTILUS_SHELL) || s.is(Items.SADDLE) || s.is(Items.FISHING_ROD);
    }

    // ------------------------------------------------------------------ combat

    /** The item a damage source belongs to and how it has to be written. */
    public record Weapon(ItemStack stack, boolean ranged, boolean immediate) {}

    /** Finds the real item behind a damage source; arrows only carry a copy, which is matched through its statistic id. */
    public static Weapon resolve(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == player) {
            ItemStack held = player.getWeaponItem();
            return held != null && StatItems.fights(held) ? new Weapon(held, false, false) : null;
        }
        if (direct instanceof ThrownTrident trident) {
            ItemStack origin = trident.getPickupItemStackOrigin();
            return StatItems.fights(origin) ? new Weapon(origin, true, true) : null;
        }
        if (direct instanceof AbstractArrow arrow) {
            ItemStack copy = arrow.getWeaponItem();
            ItemStack real = copy == null ? null : findById(player, copy);
            return real != null && StatItems.fights(real) ? new Weapon(real, true, false) : null;
        }
        return null;
    }

    /** The stack in the player's inventory carrying the same statistic id and item as {@code copy}, or null. */
    public static ItemStack findById(ServerPlayer player, ItemStack copy) {
        long uid = StatsComponents.of(copy).uid();
        if (uid == 0L) return null;
        for (ItemStack hand : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (hand.getItem() == copy.getItem() && StatsComponents.of(hand).uid() == uid) return hand;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.getItem() == copy.getItem() && StatsComponents.of(s).uid() == uid) return s;
        }
        return null;
    }

    private static Delta target(Weapon weapon) {
        return weapon.immediate() ? new Delta() : delta(weapon.stack());
    }

    private static void done(Weapon weapon, Delta d) {
        if (weapon.immediate()) applyNow(weapon.stack(), d);
    }

    public static void onKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (!SimpleStats.config().trackCombat || !counts(player)) return;
        boolean human = victim instanceof net.minecraft.world.entity.player.Player;
        if (human && !SimpleStats.config().trackPlayerKills) return;
        Weapon weapon = resolve(player, source);
        if (weapon == null) return;
        Delta d = target(weapon);
        String kind = MobKeys.kind(victim);
        d.add(StatKeys.KILLS, StatKeys.L_KILLS, kind, 1).entry(StatKeys.L_KILL_VARIANTS, human ? kind : MobKeys.variant(victim), 1);
        done(weapon, d);
    }

    /**
     * A hit that did damage. {@code dealt} is the health the victim lost, in hundredths; {@code smash} is a mace hit that counts as a smash.
     */
    public static void onDamageDealt(ServerPlayer player, LivingEntity victim, DamageSource source, long dealt, boolean smash, boolean hit) {
        if (!SimpleStats.config().trackCombat || !counts(player)) return;
        if (dealt <= 0 && !hit) return;
        Weapon weapon = resolve(player, source);
        if (weapon == null) return;
        Delta d = target(weapon);
        d.add(StatKeys.DAMAGE_DEALT, dealt).record(StatKeys.R_BIGGEST_HIT, dealt);
        if (smash) d.record(StatKeys.R_BIGGEST_SMASH, dealt);
        if (weapon.ranged()) {
            if (hit) d.add(StatKeys.HITS, 1);
            d.record(StatKeys.R_LONGEST_SHOT, Math.round(player.distanceTo(victim)));
        }
        done(weapon, d);
    }

    /** A bow or crossbow fires projectiles; the id is written now because the arrows copy the weapon. */
    public static void onShoot(ServerPlayer player, ItemStack weapon, List<ItemStack> projectiles) {
        if (!SimpleStats.config().trackCombat || weapon.isEmpty() || !counts(player)) return;
        long shots = projectiles.stream().filter(s -> !s.isEmpty()).count();
        if (shots > 0) applyNow(weapon, new Delta().add(StatKeys.ARROWS_FIRED, shots));
    }

    public static void onThrow(ServerPlayer player, ItemStack trident) {
        if (!SimpleStats.config().trackCombat || trident.isEmpty() || !counts(player)) return;
        applyNow(trident, new Delta().add(StatKeys.THROWS, 1));
    }

    // ------------------------------------------------------------------ armor and shield

    /**
     * A hit reduced by armor and enchantments. {@code reduction} is the amount the worn pieces took away (approximation: split
     * by armor points; fall damage goes to the boots). Players use the four worn slots, other mobs only their body (mount) armor.
     */
    public static void onArmored(LivingEntity victim, DamageSource source, float reduction) {
        StatsConfig config = SimpleStats.config();
        if (!config.trackArmor) return;
        EquipmentSlot[] slots = WORN;
        if (victim instanceof ServerPlayer player) {
            if (!counts(player)) return;
        } else if (victim instanceof net.minecraft.world.entity.player.Player || !config.enabled || StatsComponents.ITEM_STATS == null) {
            return;
        } else {
            slots = new EquipmentSlot[] {EquipmentSlot.BODY};
        }
        boolean fall = source.is(DamageTypeTags.IS_FALL);
        List<ItemStack> pieces = new ArrayList<>();
        List<Double> points = new ArrayList<>();
        double total = 0;
        for (EquipmentSlot slot : slots) {
            if (fall && slot != EquipmentSlot.FEET) continue;
            ItemStack piece = victim.getItemBySlot(slot);
            double p = StatItems.armorPoints(piece, slot);
            if (fall && p <= 0 && StatItems.boots(piece)) p = 1;
            if (p > 0) {
                pieces.add(piece);
                points.add(p);
                total += p;
            }
        }
        long absorbed = reduction > 0 ? Math.round(reduction * 100.0) : 0;
        if (pieces.isEmpty() || (absorbed <= 0 && source.is(DamageTypeTags.BYPASSES_ARMOR))) return;
        long[] shares = split(absorbed, points, total);
        String type = source.typeHolder().getRegisteredName();
        for (int i = 0; i < pieces.size(); i++) {
            Delta d = delta(pieces.get(i));
            d.add(StatKeys.HITS_TAKEN, 1);
            if (shares[i] > 0) d.add(StatKeys.DAMAGE_ABSORBED, StatKeys.L_ABSORBED, type, shares[i]).record(StatKeys.R_BIGGEST_ABSORBED, shares[i]);
        }
    }

    /** Splits {@code amount} by weight so that the parts add up to exactly {@code amount} (the rest goes to the heaviest piece). */
    public static long[] split(long amount, List<Double> weights, double total) {
        long[] parts = new long[weights.size()];
        if (amount <= 0 || total <= 0) return parts;
        long used = 0;
        int heaviest = 0;
        for (int i = 0; i < parts.length; i++) {
            parts[i] = (long) Math.floor(amount * weights.get(i) / total);
            used += parts[i];
            if (weights.get(i) > weights.get(heaviest)) heaviest = i;
        }
        parts[heaviest] += amount - used;
        return parts;
    }

    public static void onBlocked(ServerPlayer player, ItemStack shield, DamageSource source, float blocked) {
        if (!SimpleStats.config().trackArmor || blocked <= 0 || !StatItems.shield(shield) || !counts(player)) return;
        Entity attacker = source.getEntity();
        String by = attacker != null ? MobKeys.kind(attacker) : source.typeHolder().getRegisteredName();
        long amount = Math.max(1, Math.round(blocked * 100.0));
        delta(shield).add(StatKeys.ATTACKS_BLOCKED, 1).add(StatKeys.DAMAGE_BLOCKED, StatKeys.L_BLOCKED_BY, by, amount);
    }

    /** Forget pending changes of a stack (used by the reset command so they cannot come back). */
    public static void discard(ItemStack stack) {
        PENDING.remove(stack);
    }

    /** For tests: drop everything pending and reset the write counter. */
    public static void reset() {
        PENDING.clear();
        writes = 0;
    }
}
