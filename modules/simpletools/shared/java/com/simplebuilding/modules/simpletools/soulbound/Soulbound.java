package com.simplebuilding.modules.simpletools.soulbound;

import com.simplebuilding.modules.simpletools.SimpleTools;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Soulbound (owner 2026-10-12): an item with it never drops on death. While the inventory empties ({@code Inventory#dropAll}
 * inside {@code Player#dropEquipment}) its stacks are taken out and put back into the same slots of the dead player, which is
 * the only store (saved with the player while the death screen is open). Nachtrag 60: in a Hardcore world it does nothing
 * (the enchantment can still be applied; the item tooltip says so). The respawn ({@code restoreFrom(old, false)})
 * moves them slot by slot into the new player and caps the remaining durability at {@code deathDurabilityCap} (50).
 */
public final class Soulbound {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, "soulbound"));

    /** The player whose death drops are running right now (server thread), else null. */
    private static Player dying;

    /** Test hook: forces the Hardcore answer (null: ask the world). */
    public static Boolean hardcoreOverride;

    private Soulbound() {}

    public static boolean active() {
        return SimpleTools.active() && SimpleTools.config().enableSoulbound;
    }

    /** Like the bed and the respawn anchor, Soulbound does nothing in a Hardcore world (items stay enchantable). */
    public static boolean hardcore(Player player) {
        if (hardcoreOverride != null) return hardcoreOverride;
        return player != null && player.level().getLevelData().isHardcore();
    }

    static boolean is(Holder<Enchantment> holder) {
        return holder.is(KEY);
    }

    /** True when the stack itself carries Soulbound (a book that only stores it is not bound). */
    public static boolean has(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) if (is(holder)) return true;
        return false;
    }

    /** The stack carries or stores Soulbound (anvil input). */
    public static boolean carries(ItemStack stack) {
        if (has(stack)) return true;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return false;
        for (Holder<Enchantment> holder : stored.keySet()) if (is(holder)) return true;
        return false;
    }

    public static void beginDeath(Player player) {
        dying = player;
    }

    public static void endDeath(Player player) {
        if (dying == player) dying = null;
    }

    /** True while {@code player}'s death drops run. */
    public static boolean dying(Player player) {
        return player != null && dying == player;
    }

    /** One Soulbound stack taken out of a slot before the drop. */
    public record Kept(int slot, ItemStack stack) {}

    /** Takes every Soulbound stack out of the inventory (before {@code dropAll}). */
    public static List<Kept> takeOut(Inventory inventory) {
        List<Kept> kept = new ArrayList<>();
        if (!active() || hardcore(inventory.player)) return kept;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (has(stack)) {
                kept.add(new Kept(i, stack));
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        return kept;
    }

    /** Puts the stacks back into the same slots of the (dead) player after {@code dropAll}. */
    public static void putBack(Inventory inventory, List<Kept> kept) {
        for (Kept k : kept) {
            if (inventory.getItem(k.slot()).isEmpty()) inventory.setItem(k.slot(), k.stack());
            else inventory.placeItemBackInInventory(k.stack(), false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /** A Soulbound enchanted book, or empty when the enchantment is missing (data pack removed it). */
    public static ItemStack book(net.minecraft.core.HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(KEY)
                .map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(new net.minecraft.world.item.enchantment.EnchantmentInstance(h, 1)))
                .orElse(ItemStack.EMPTY);
    }

    /** Wandering trader: with {@code soulboundTraderChance} one extra offer, 24-32 diamonds for the book, once. */
    public static boolean maybeOffer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.trading.MerchantOffers offers, net.minecraft.util.RandomSource random) {
        if (!active() || random.nextDouble() >= SimpleTools.config().soulboundTraderChance) return false;
        ItemStack book = book(level.registryAccess());
        if (book.isEmpty()) return false;
        offers.add(traderOffer(book, 24 + random.nextInt(9)));
        return true;
    }

    public static net.minecraft.world.item.trading.MerchantOffer traderOffer(ItemStack book, int diamonds) {
        return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.DIAMOND, diamonds),
                book, 1, 30, 0);
    }

    /** Remaining durability capped: damage = max - min(max - damage, cap); items without durability unchanged. */
    public static ItemStack capDurability(ItemStack stack) {
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            int cap = SimpleTools.config().deathDurabilityCap;
            if (left > cap) stack.setDamageValue(max - cap);
        }
        return stack;
    }

    /**
     * Respawn after a death: Soulbound stacks of the dead player move into the same slots of the new one (the old slot is
     * emptied, so nothing exists twice). Not on End return or with keepInventory (Vanilla copies everything then).
     */
    public static void restore(ServerPlayer fresh, ServerPlayer old, boolean restoreAll, boolean vanillaKeeps) {
        if (restoreAll || vanillaKeeps || hardcore(old)) return;
        Inventory from = old.getInventory();
        Inventory to = fresh.getInventory();
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack stack = from.getItem(i);
            if (!has(stack)) continue;
            from.setItem(i, ItemStack.EMPTY);
            ItemStack moved = capDurability(stack);
            if (i < to.getContainerSize() && to.getItem(i).isEmpty()) to.setItem(i, moved);
            else to.placeItemBackInInventory(moved, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
}
