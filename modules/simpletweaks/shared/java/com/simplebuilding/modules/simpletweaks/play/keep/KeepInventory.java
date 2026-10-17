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
 * part + {@code keepXpPercent}). The kept slots are taken out before {@code Inventory.dropAll}, put back right after
 * (the dead player still holds them, so a relog before respawning keeps them too) and move to the new player in
 * {@code restoreFrom}. Curse of vanishing is applied first, as in Vanilla.
 */
public final class KeepInventory {
    private static final ThreadLocal<Map<Integer, ItemStack>> HELD = new ThreadLocal<>();

    public static boolean active(ServerLevel level) {
        return Play.on() && Play.server() != null && Play.config().keepInventory().enabled() && !level.getGameRules().get(GameRules.KEEP_INVENTORY);
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

    /** Start of {@code Player.dropEquipment}: curse of vanishing first, then the kept stacks leave the inventory. */
    public static void beforeDrops(Player p, ServerLevel level) {
        if (!(p instanceof ServerPlayer) || !active(level)) return;
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
    /** End of {@code Player.dropEquipment}: the kept stacks go back into the (dead) player's slots. */
    public static void afterDrops(Player p) {
        var held = HELD.get();
        HELD.remove();
        if (held == null) return;
        held.forEach((slot, st) -> p.getInventory().setItem(slot, st));
    }
    /** Experience orbs dropped on death, reduced by the kept share. */
    public static int droppedXp(Player p, int vanilla) {
        if (!(p.level() instanceof ServerLevel level) || !active(level)) return vanilla;
        return vanilla * (100 - xpPercent()) / 100;
    }
    /** {@code ServerPlayer.restoreFrom} after a death (not after leaving the End). */
    public static void restore(ServerPlayer now, ServerPlayer old) {
        if (!active(now.level()) || old.isSpectator()) return;
        var from = old.getInventory();
        for (int i = 0; i < from.getContainerSize(); i++) {
            var st = from.getItem(i);
            if (!st.isEmpty() && keepsSlot(i)) now.getInventory().setItem(i, st.copy());
        }
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
