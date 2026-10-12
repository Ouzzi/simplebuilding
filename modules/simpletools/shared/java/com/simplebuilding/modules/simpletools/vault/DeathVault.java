package com.simplebuilding.modules.simpletools.vault;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Death Vault rules (owner 2026-10-12): which lost stacks are shown, how an offering turns into returns, and the weights
 * (newer deaths and more valuable items are more likely). Pure server logic; the menu only calls it.
 */
public final class DeathVault {
    /** Items accepted in the offering slot, one point each (diamond; Simple Money bill only when that mod is loaded). */
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, "death_vault_offerings"));

    /** Death drops being collected right now (between {@code Player#dropEquipment} head and return). */
    private static List<ItemEntity> collecting;

    private DeathVault() {}

    public static boolean active() {
        return SimpleTools.active() && SimpleTools.config().enableDeathVault;
    }

    public static void beginDeath(Player player) {
        collecting = active() && player instanceof ServerPlayer ? new ArrayList<>() : null;
    }

    /** Called for every item entity the dying player creates while the death drops run. */
    public static void collect(ItemEntity entity) {
        if (collecting != null && entity != null) collecting.add(entity);
    }

    /** Stores the collected drops as one death of the player. */
    public static void endDeath(Player player) {
        List<ItemEntity> drops = collecting;
        collecting = null;
        if (drops == null || drops.isEmpty() || !(player instanceof ServerPlayer server)) return;
        Optional<UUID> killer = Optional.empty();
        if (player.getKillCredit() instanceof Player other && other != player) killer = Optional.of(other.getUUID());
        DeathLedger.get(server.level().getServer()).record(player.getUUID(), server.level().getGameTime(), killer, drops);
    }

    /** A redeemable stack with its weight. */
    public record Lost(DeathLedger.Death death, DeathLedger.Entry entry, double weight) {}

    /** Lost (redeemable) stacks of the player, newest death first, with their weights. */
    public static List<Lost> lost(DeathLedger ledger, UUID player) {
        List<DeathLedger.Death> deaths = ledger.deaths(player);
        List<Lost> out = new ArrayList<>();
        for (int rank = 0; rank < deaths.size(); rank++) {
            DeathLedger.Death death = deaths.get(deaths.size() - 1 - rank);
            for (DeathLedger.Entry entry : death.entries) {
                if (entry.state == DeathLedger.LOST) out.add(new Lost(death, entry, weight(entry.stack, rank)));
            }
        }
        return out;
    }

    /** Stacks still lying in the world (shown, not redeemable), newest death first. */
    public static List<ItemStack> lying(DeathLedger ledger, UUID player) {
        List<DeathLedger.Death> deaths = ledger.deaths(player);
        List<ItemStack> out = new ArrayList<>();
        for (int i = deaths.size() - 1; i >= 0; i--) {
            for (DeathLedger.Entry entry : deaths.get(i).entries) if (entry.state == DeathLedger.LYING) out.add(entry.stack);
        }
        return out;
    }

    /** Recency factor 1/(1 + rank), rank 0 = newest death. */
    public static double recency(int rank) {
        return 1.0 / (1 + rank);
    }

    /** Item value: rarity (1/2/4/8) + enchantment levels + max durability / 200. */
    public static double value(ItemStack stack) {
        double v = switch (stack.getRarity()) {
            case UNCOMMON -> 2;
            case RARE -> 4;
            case EPIC -> 8;
            default -> 1;
        };
        v += levels(stack.getEnchantments()) + levels(stack.get(DataComponents.STORED_ENCHANTMENTS));
        if (stack.isDamageableItem()) v += stack.getMaxDamage() / 200.0;
        return v;
    }

    private static int levels(ItemEnchantments enchantments) {
        if (enchantments == null) return 0;
        int sum = 0;
        for (var e : enchantments.entrySet()) sum += e.getIntValue();
        return sum;
    }

    public static double weight(ItemStack stack, int rank) {
        return value(stack) * recency(rank);
    }

    /** Points an offering stack is worth (one per item in {@link #OFFERINGS}). */
    public static int points(ItemStack offering) {
        return offering.is(OFFERINGS) ? offering.getCount() : 0;
    }

    /**
     * One offering: as many returns as the points pay for (cost per item), at most the per-offering limit and the number
     * of lost stacks; the chosen stacks leave the ledger, the paid items leave the offering stack. Nothing is consumed when
     * nothing can be returned. Returns the stacks to give (durability already capped).
     */
    public static List<ItemStack> offer(DeathLedger ledger, UUID player, ItemStack offering, RandomSource random) {
        if (!active()) return List.of();
        List<Lost> lost = new ArrayList<>(lost(ledger, player));
        int cost = SimpleTools.config().deathVaultCostPerItem;
        int picks = Math.min(Math.min(points(offering) / cost, SimpleTools.config().deathVaultMaxPerOffering), lost.size());
        List<ItemStack> out = new ArrayList<>();
        if (picks <= 0) return out;
        offering.shrink(picks * cost);
        for (int n = 0; n < picks; n++) {
            Lost chosen = pick(lost, random);
            lost.remove(chosen);
            ledger.take(player, chosen.death(), chosen.entry());
            out.add(Soulbound.capDurability(chosen.entry().stack.copy()));
        }
        return out;
    }

    /** Weighted choice (weights > 0). */
    public static Lost pick(List<Lost> lost, RandomSource random) {
        double total = 0;
        for (Lost l : lost) total += l.weight();
        double r = random.nextDouble() * total;
        for (Lost l : lost) {
            r -= l.weight();
            if (r < 0) return l;
        }
        return lost.getLast();
    }
}
