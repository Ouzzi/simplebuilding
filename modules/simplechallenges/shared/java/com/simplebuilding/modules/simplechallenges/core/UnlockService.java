package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import com.simplebuilding.modules.simplechallenges.preset.Currency;
import com.simplebuilding.modules.simplechallenges.price.CellInfo;
import com.simplebuilding.modules.simplechallenges.price.Price;
import com.simplebuilding.modules.simplechallenges.price.PriceEngine;
import com.simplebuilding.modules.simplechallenges.price.Solvability;
import com.simplebuilding.modules.simplechallenges.price.WeightedItem;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Prices, payment and unlocking of cells (S2). All server side; the price of a cell is computed once and kept in the state. */
public final class UnlockService {
    public enum Outcome { OK, INACTIVE, ALREADY, NOT_ADJACENT, CANNOT_PAY }

    private UnlockService() {}

    private static String key(String dim, int cx, int cz) {
        return dim + "|" + cx + "|" + cz;
    }

    /** The price of a locked cell (cached; computed from the probe of the cell and the unlocked area). */
    public static Price priceOf(Active a, String dim, int cx, int cz) {
        String key = key(dim, cx, cz);
        String stored = a.state.prices.get(key);
        Price p = stored == null ? null : Price.decode(stored);
        if (p != null) return p;
        ServerLevel level = a.level(dim);
        CellInfo own = level == null ? CellInfo.EMPTY : LevelProbe.cell(level, a.mode.cell(), cx, cz);
        p = a.engine.price(dim, cx, cz, own, a.region(), a.grid(dim).size());
        a.state.prices.put(key, p.encode());
        a.state.setDirty();
        return p;
    }

    public static int count(ServerPlayer player, String itemId) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) n += s.getCount();
        }
        return n;
    }

    public static boolean canPay(Active a, ServerPlayer player, Price p) {
        return switch (p.currency()) {
            case ITEMS -> count(player, p.item()) >= p.count();
            case CREDITS -> Credits.of(a, player) >= p.count();
            case XP -> player.experienceLevel >= p.count();
        };
    }

    private static void pay(Active a, ServerPlayer player, Price p) {
        switch (p.currency()) {
            case ITEMS -> {
                int left = p.count();
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.isEmpty() || !BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(p.item())) continue;
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
                inv.setChanged();
            }
            case CREDITS -> Credits.add(a, player, -p.count());
            case XP -> player.giveExperienceLevels(-p.count());
        }
    }

    public static Outcome tryUnlock(Active a, ServerPlayer player, String dim, int cx, int cz) {
        UnlockGrid g = a.grid(dim);
        if (g == null) return Outcome.INACTIVE;
        if (g.isFree(cx, cz)) return Outcome.ALREADY;
        if (!g.touchesFree(cx, cz)) return Outcome.NOT_ADJACENT;
        Price p = priceOf(a, dim, cx, cz);
        if (!canPay(a, player, p)) return Outcome.CANNOT_PAY;
        pay(a, player, p);
        unlock(a, dim, cx, cz);
        return Outcome.OK;
    }

    /** Unlocks without payment (admin command, emergency unlock). */
    public static boolean unlock(Active a, String dim, int cx, int cz) {
        UnlockGrid g = a.grid(dim);
        if (g == null || !g.unlock(cx, cz)) return false;
        ChallengeState s = a.state;
        s.unlockCount++;
        s.lastUnlockTick = a.server.overworld().getGameTime();
        s.prices.remove(key(dim, cx, cz));
        ServerLevel level = a.level(dim);
        if (level != null) a.addAvailable(LevelProbe.cell(level, a.mode.cell(), cx, cz).reachable());
        s.setDirty();
        ChallengeSync.sendDelta(a, dim, cx, cz);
        ensureSolvable(a);
        return true;
    }

    /** Looks along (lookX, lookZ) from a block position and returns the first locked cell that touches the free area, or {@link UnlockGrid#NONE}. */
    public static long lookedCell(UnlockGrid g, double x, double z, double lookX, double lookZ, double maxDistance) {
        double len = Math.hypot(lookX, lookZ);
        if (len < 1.0E-6) return UnlockGrid.NONE;
        double dx = lookX / len, dz = lookZ / len;
        for (double d = 0; d <= maxDistance; d += 0.5) {
            int cx = g.cellOf(x + dx * d), cz = g.cellOf(z + dz * d);
            if (!g.isFree(cx, cz) && g.touchesFree(cx, cz)) return UnlockGrid.key(cx, cz);
        }
        return UnlockGrid.NONE;
    }

    /**
     * Deadlock protection (concept 3): at least one frontier cell must be payable from the unlocked area. If none is, the first
     * frontier cell gets a replacement price from the area itself (its most common hand-reachable item), or one level when the
     * area offers nothing at all.
     */
    public static void ensureSolvable(Active a) {
        for (String dim : a.dimensions()) {
            UnlockGrid g = a.grid(dim);
            if (g == null) continue;
            if (!Solvability.payableFrontier(g, k -> priceOf(a, dim, UnlockGrid.keyX(k), UnlockGrid.keyZ(k)), a::obtainable).isEmpty()) continue;
            long[] frontier = g.frontier();
            if (frontier.length == 0) continue;
            long k = frontier[0];
            List<WeightedItem> region = a.region();
            Price p = Price.xp(1);
            if (a.mode.currency() == Currency.ITEMS && !region.isEmpty()) {
                WeightedItem best = region.get(0);
                for (WeightedItem w : region) if (w.weight() > best.weight()) best = w;
                int base = (int) Math.max(1, Math.ceil(Math.sqrt((double) a.mode.cell() * a.mode.cell()) * 0.5));
                p = Price.items(best.id(), Math.min(base, 2 * best.maxStack()));
            } else if (a.mode.currency() == Currency.CREDITS) {
                p = Price.credits(1);
            }
            a.state.prices.put(key(dim, UnlockGrid.keyX(k), UnlockGrid.keyZ(k)), p.encode());
            a.state.setDirty();
        }
    }

    /** True when someone can still pay a credit (the team pot, or any online player's own pot). */
    private static boolean anyCredit(Active a) {
        if (!ChallengeServerConfig.perPlayerCredits) return a.state.credits >= 1;
        for (ServerPlayer p : a.server.getPlayerList().getPlayers()) if (Credits.of(a, p) >= 1) return true;
        return false;
    }

    /** Every 20 ticks: the emergency unlock when nothing was unlocked for pityMinutes and nothing can be paid (or no credit is left). */
    public static void pity(Active a) {
        pity(a, !a.server.getPlayerList().getPlayers().isEmpty());
    }

    /** @param playersOnline the timer only runs while someone plays (tests pass true) */
    public static void pity(Active a, boolean playersOnline) {
        if (a.mode == null || !ChallengeServerConfig.pityEnabled || !playersOnline) return;
        long now = a.server.overworld().getGameTime();
        long pityTicks = a.mode.pityMinutes() * 60L * 20L;
        if (now - a.state.lastUnlockTick < pityTicks) return;
        for (String dim : a.dimensions()) {
            UnlockGrid g = a.grid(dim);
            if (g == null) continue;
            boolean stuck = a.mode.currency() == Currency.CREDITS ? !anyCredit(a)
                    : Solvability.payableFrontier(g, k -> priceOf(a, dim, UnlockGrid.keyX(k), UnlockGrid.keyZ(k)), a::obtainable).isEmpty();
            if (!Solvability.due(true, now - a.state.lastUnlockTick, pityTicks, stuck)) continue;
            long[] frontier = g.frontier();
            if (frontier.length == 0) continue;
            long best = frontier[0];
            double bestD = Double.MAX_VALUE;
            for (long k : frontier) {
                double d = Math.hypot(UnlockGrid.keyX(k), UnlockGrid.keyZ(k));
                if (d < bestD) {
                    bestD = d;
                    best = k;
                }
            }
            if (unlock(a, dim, UnlockGrid.keyX(best), UnlockGrid.keyZ(best))) {
                for (ServerPlayer p : a.server.getPlayerList().getPlayers()) p.sendSystemMessage(Component.translatable("simplechallenges.msg.pity"));
            }
            return;
        }
    }
}
