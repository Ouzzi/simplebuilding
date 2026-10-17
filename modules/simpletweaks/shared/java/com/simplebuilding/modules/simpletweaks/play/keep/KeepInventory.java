package com.simplebuilding.modules.simpletweaks.play.keep;

import com.simplebuilding.modules.simpletweaks.play.Play;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * T27 partial keep inventory, only while the Vanilla rule {@code keep_inventory} is off. Modes: {@code hotbar_armor}
 * (hotbar, armor and offhand stay), {@code xp} (experience stays, everything else drops), {@code custom} (switches per
 * part + {@code keepXpPercent}). Off in Hardcore worlds (as Vanilla's own rules there).
 *
 * <p>Order with other death handling (no class imports, only Vanilla hook points): Curse of Vanishing runs first (Vanilla
 * {@code destroyVanishingCursedItems}), then every {@code Inventory.dropAll} HEAD hook of another mod (Simple Tools Soulbound
 * takes its stacks there), and only then T27 takes what is left of its slots (injected at the first read of the slot list,
 * which is after all HEAD hooks). The rest drops and is what a death record (Simple Tools Death Vault) sees. The kept stacks
 * go back into the dead player (so a relog on the death screen, e.g. Simple Respawn's logout death, keeps them in the saved
 * file) and the slots are written down as an entity tag; {@code restoreFrom} moves exactly those slots to the new player and
 * empties them in the old one, so neither another mod's restore nor a second respawn can copy them.
 */
public final class KeepInventory {
    private static final ThreadLocal<Map<Integer, ItemStack>> HELD = new ThreadLocal<>();
    /** Entity tag on the dead player: {@code simpletweaks_kept:<slot>,<slot>...}. */
    static final String TAG = "simpletweaks_kept:";
    /** The player whose {@code Player.dropEquipment} runs (only a death empties the inventory through T27). */
    private static final ThreadLocal<Player> DYING = new ThreadLocal<>();

    public static void deathStart(Player p) { DYING.set(p); }
    public static void deathEnd(Player p) { if (DYING.get() == p) DYING.remove(); }
    static void untag(Player p) { for (var t : List.copyOf(p.entityTags())) if (t.startsWith(TAG)) p.removeTag(t); }

    public static boolean active(ServerLevel level) {
        return Play.on() && Play.server() != null && Play.config().keepInventory().enabled() && !level.getGameRules().get(GameRules.KEEP_INVENTORY)
                && !level.getLevelData().isHardcore();
    }
    public static boolean keepsSlot(int slot) {
        var c = Play.config().keepInventory();
        return switch (c.mode()) {
            case "xp" -> false;
            case "custom" -> slot < Inventory.SELECTION_SIZE ? c.keepHotbar() : slot < Inventory.INVENTORY_SIZE ? c.keepMain()
                    : slot < Inventory.SLOT_OFFHAND ? c.keepArmor() : slot == Inventory.SLOT_OFFHAND && c.keepOffhand();
            default -> slot < Inventory.SELECTION_SIZE || (slot >= Inventory.INVENTORY_SIZE && slot <= Inventory.SLOT_OFFHAND);
        };
    }
    /** Percent of the experience kept. */
    public static int xpPercent() {
        var c = Play.config().keepInventory();
        return switch (c.mode()) { case "xp" -> 100; case "custom" -> c.keepXpPercent(); default -> 0; };
    }

    /** Inside {@code Inventory.dropAll}, after the HEAD hooks of other mods: the kept stacks leave the inventory. */
    public static void beforeDrops(Player p) {
        HELD.remove();
        if (!(p instanceof ServerPlayer) || DYING.get() != p || !(p.level() instanceof ServerLevel level) || !active(level)) return;
        var inv = p.getInventory();
        var held = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var st = inv.getItem(i);
            if (st.isEmpty() || !keepsSlot(i)) continue;
            if (net.minecraft.world.item.enchantment.EnchantmentHelper.has(st, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;
            held.put(i, inv.removeItemNoUpdate(i));
        }
        HELD.set(held);
    }
    /** End of {@code Inventory.dropAll}: the kept stacks go back into the (dead) player's slots, the slots are noted. */
    public static void afterDrops(Player p) {
        var held = HELD.get();
        HELD.remove();
        if (held == null) return;
        untag(p);
        if (held.isEmpty()) return;
        var slots = new StringJoiner(",");
        new TreeMap<>(held).forEach((slot, st) -> {
            var inv = p.getInventory();
            if (inv.getItem(slot).isEmpty()) inv.setItem(slot, st);
            else inv.placeItemBackInInventory(st, false, net.minecraft.util.Prediction.SERVER_ONLY);
            slots.add(String.valueOf(slot));
        });
        p.addTag(TAG + slots);
    }
    /** Slots T27 kept on the dead player (from its tag). */
    static List<Integer> keptSlots(Player p) {
        var out = new ArrayList<Integer>();
        for (var t : p.entityTags()) if (t.startsWith(TAG)) for (var n : t.substring(TAG.length()).split(",")) {
            try { out.add(Integer.parseInt(n)); } catch (NumberFormatException ignored) {}
        }
        return out;
    }
    /** Experience orbs dropped on death, reduced by the kept share. */
    public static int droppedXp(Player p, int vanilla) {
        if (!(p.level() instanceof ServerLevel level) || !active(level)) return vanilla;
        return vanilla * (100 - xpPercent()) / 100;
    }
    /**
     * {@code ServerPlayer.restoreFrom} after a death (not after leaving the End): the slots T27 kept move (not copy) to the
     * new player, even if T27 was switched off meanwhile; with Vanilla keep inventory Vanilla already copied everything.
     */
    public static void restore(ServerPlayer now, ServerPlayer old) {
        var slots = keptSlots(old);
        untag(old);
        untag(now);
        if (now.level().getGameRules().get(GameRules.KEEP_INVENTORY) || old.isSpectator()) return;
        var from = old.getInventory();
        var to = now.getInventory();
        for (int i : slots) {
            if (i < 0 || i >= from.getContainerSize()) continue;
            var st = from.removeItemNoUpdate(i);
            if (st.isEmpty()) continue;
            if (i < to.getContainerSize() && to.getItem(i).isEmpty()) to.setItem(i, st);
            else to.placeItemBackInInventory(st, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        if (!active(now.level())) return;
        int pct = xpPercent();
        if (pct >= 100) { now.experienceLevel = old.experienceLevel; now.experienceProgress = old.experienceProgress; now.totalExperience = old.totalExperience; }
        else if (pct > 0) now.giveExperiencePoints((int) (points(old.experienceLevel, old.experienceProgress) * pct / 100));
    }
    /** Experience points of a level and progress (Vanilla formula of {@code getXpNeededForNextLevel}). */
    public static long points(int level, float progress) {
        long sum = 0;
        for (int l = 0; l < level; l++) sum += need(l);
        return sum + Math.round(progress * need(level));
    }
    static int need(int l) { return l >= 30 ? 112 + (l - 30) * 9 : l >= 15 ? 37 + (l - 15) * 5 : 7 + l * 2; }
    private KeepInventory() {}
}
