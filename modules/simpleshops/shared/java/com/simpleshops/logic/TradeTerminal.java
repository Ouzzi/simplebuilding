package com.simpleshops.logic;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.config.ShopsConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

/**
 * Server logic of the trade terminal (KONZ20 S5 k): the catalog over the ender shop data and the rules for reaching a shop from
 * afar. Nothing here loads a chunk: everything reads and writes {@link EnderShops} only. The client never sends a quantity, a
 * price or an item, only "this row", "this page", "this text".
 */
public final class TradeTerminal {
    /** Rows per page (two rows of nine) and the most offers one query looks at. */
    public static final int PAGE = 18, MAX_SCAN = 2000, QUERY_MAX = 32;
    /** Sort orders of the browse tab. */
    public static final int SORT_PRICE = 0, SORT_DISTANCE = 1, SORT_OWNER = 2, SORTS = 3;

    /** One line of a list: a shop (mine tab, {@code ware} -1) or one offer of a shop (browse tab). */
    public record Row(String shop, int ware, ItemStack icon, String sortName, int price, double distance) {}

    public enum Why { OK, OFF, FAR, DIMENSION, PRIVATE, NO_PRICE, NOT_OWNER, OWN, BUSY, GONE, NO_SPACE, EMPTY, RATE }

    private TradeTerminal() {}

    static String dim(ServerPlayer p) {
        return p.level().dimension().identifier().toString();
    }

    /** Same-dimension rule (config {@code remoteCrossDimension}). */
    public static boolean reachable(ServerPlayer player, EnderShops.Vault v) {
        return ShopsConfig.remoteCrossDimension || v.dimension().equals(dim(player));
    }

    private static double distance(ServerPlayer player, EnderShops.Vault v) {
        if (!v.dimension().equals(dim(player))) return Double.MAX_VALUE;
        BlockPos p = v.blockPos();
        return Math.sqrt(player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
    }

    /** All sales entries of {@code owner}, sorted by name then position. */
    public static List<Row> mine(EnderShops data, ServerPlayer player) {
        List<Row> out = new ArrayList<>();
        UUID me = player.getUUID();
        for (EnderShops.Vault v : data.all()) {
            if (v.kind != EnderShops.Kind.SALES || v.isServer() || !v.isOwner(me)) continue;
            if (out.size() >= ShopsConfig.MAX_SHOPS_MAX * 4) break;
            EnderShops.Vault box = data.caseOf(v);
            ItemStack icon = ItemStack.EMPTY;
            if (box != null) for (ItemStack s : box.items) if (!s.isEmpty()) {
                icon = s.copyWithCount(1);
                break;
            }
            if (icon.isEmpty()) icon = new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal(where(v)).withStyle(ChatFormatting.GRAY));
            int goods = 0;
            if (box != null) for (ItemStack s : box.items) goods += s.getCount();
            int earned = 0;
            for (NonNullList<ItemStack> list : data.earningsChain(v)) for (ItemStack s : list) earned += s.getCount();
            lore.add(Component.translatable("simpleshops.terminal.stock_line", goods, earned).withStyle(ChatFormatting.GRAY));
            lore.add(Component.translatable(status(data, v, box)).withStyle(ChatFormatting.GRAY));
            lore.add(Component.translatable(v.listed() ? "simpleshops.terminal.public" : "simpleshops.terminal.private").withStyle(ChatFormatting.GRAY));
            icon.set(DataComponents.CUSTOM_NAME, Component.literal(title(v)));
            icon.set(DataComponents.LORE, new ItemLore(lore));
            out.add(new Row(v.id, -1, icon, title(v).toLowerCase(Locale.ROOT), 0, distance(player, v)));
        }
        out.sort(Comparator.comparing(Row::sortName).thenComparing(Row::shop));
        return out;
    }

    private static String status(EnderShops data, EnderShops.Vault v, EnderShops.@Nullable Vault box) {
        if (PriceRow.filled(v.prices) == 0) return "simpleshops.terminal.status.no_price";
        boolean stocked = box != null && box.items.stream().anyMatch(s -> !s.isEmpty());
        if (!stocked) return "simpleshops.terminal.status.sold_out";
        if (!ShopTrade.fitsRemote(data, v)) return "simpleshops.terminal.status.earnings_full";
        return "simpleshops.terminal.status.active";
    }

    static String title(EnderShops.Vault v) {
        if (!v.name().isEmpty()) return v.name();
        if (v.isServer()) return "Server shop " + v.serverId();
        return v.ownerName().isEmpty() ? "?" : v.ownerName();
    }

    static String where(EnderShops.Vault v) {
        BlockPos p = v.blockPos();
        String d = v.dimension();
        int colon = d.indexOf(':');
        return p.getX() + " " + p.getY() + " " + p.getZ() + " (" + (colon >= 0 && d.startsWith("minecraft:") ? d.substring(colon + 1) : d) + ")";
    }

    /**
     * The browse catalog: one row per offered ware of every public shop (players and server) with a price and a display case,
     * reachable from this dimension. {@code query} matches the item's name, its id (mod id) or the shop's name/owner.
     */
    public static List<Row> browse(EnderShops data, ServerPlayer player, String query, boolean inStock, int sort) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).strip();
        List<Row> out = new ArrayList<>();
        int scanned = 0;
        for (EnderShops.Vault v : data.all()) {
            if (v.kind != EnderShops.Kind.SALES || !v.listed() || !reachable(player, v) || PriceRow.filled(v.prices) == 0) continue;
            if (v.owner() == null && !v.isServer()) continue;
            EnderShops.Vault box = data.caseOf(v);
            if (box == null || box.isServer() != v.isServer()) continue;
            int price = 0;
            for (ItemStack s : v.prices) price += s.getCount();
            boolean any = false;
            for (int i = 0; i < box.items.size(); i++) {
                ItemStack s = box.items.get(i);
                if (s.isEmpty()) continue;
                any = true;
                if (++scanned > MAX_SCAN) break;
                if (!q.isEmpty() && !matches(s, v, q)) continue;
                out.add(new Row(v.id, i, offerIcon(player, v, s, false), title(v).toLowerCase(Locale.ROOT), price, distance(player, v)));
            }
            if (!any && !inStock && (q.isEmpty() || title(v).toLowerCase(Locale.ROOT).contains(q))) {
                out.add(new Row(v.id, -1, offerIcon(player, v, new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM), true), title(v).toLowerCase(Locale.ROOT), price, distance(player, v)));
            }
            if (scanned > MAX_SCAN) break;
        }
        Comparator<Row> order = switch (sort) {
            case SORT_DISTANCE -> Comparator.comparingDouble(Row::distance);
            case SORT_OWNER -> Comparator.comparing(Row::sortName);
            default -> Comparator.comparingInt(Row::price);
        };
        out.sort(order.thenComparing(Row::shop).thenComparingInt(Row::ware));
        return out;
    }

    private static boolean matches(ItemStack s, EnderShops.Vault v, String q) {
        if (s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains(q)) return true;
        return title(v).toLowerCase(Locale.ROOT).contains(q);
    }

    private static ItemStack offerIcon(ServerPlayer player, EnderShops.Vault v, ItemStack goods, boolean soldOut) {
        ItemStack icon = goods.copyWithCount(1);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(title(v)).withStyle(ChatFormatting.GOLD));
        lore.add(Component.translatable("simpleshops.terminal.price_line", priceText(v.prices)).withStyle(ChatFormatting.GRAY));
        double d = distance(player, v);
        lore.add(Component.literal(where(v) + (d < Double.MAX_VALUE ? " · " + (int) d + " m" : "")).withStyle(ChatFormatting.GRAY));
        if (soldOut) lore.add(Component.translatable("simpleshops.terminal.status.sold_out").withStyle(ChatFormatting.GRAY));
        if (v.isOwner(player.getUUID())) lore.add(Component.translatable("simpleshops.terminal.own_shop").withStyle(ChatFormatting.GRAY));
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    static String priceText(NonNullList<ItemStack> prices) {
        StringBuilder b = new StringBuilder();
        for (ItemStack s : prices) {
            if (s.isEmpty()) continue;
            if (b.length() > 0) b.append(", ");
            b.append(s.getCount()).append("x ").append(s.getHoverName().getString());
        }
        return b.toString();
    }

    // --- rules ----------------------------------------------------------------------------------------------------------

    /** May {@code player} buy from {@code v} through a terminal (not own, public, reachable, priced)? */
    public static Why mayBuy(ServerPlayer player, EnderShops.Vault v) {
        if (v.deleted()) return Why.GONE;
        if (!reachable(player, v)) return Why.DIMENSION;
        if (v.isOwner(player.getUUID())) return Why.OWN;
        if (!v.listed()) return Why.PRIVATE;
        if (PriceRow.filled(v.prices) == 0) return Why.NO_PRICE;
        return Why.OK;
    }

    /** KONZ20 S2 W2: own goods from afar only out, only from a public shop with a price, only in reach (dimension rule). */
    public static Why mayTake(ServerPlayer player, EnderShops.Vault sales) {
        if (!ShopsConfig.remoteTake()) return Why.OFF;
        if (sales.deleted()) return Why.GONE;
        if (!sales.isOwner(player.getUUID())) return Why.NOT_OWNER;
        if (!reachable(player, sales)) return Why.DIMENSION;
        if (!sales.listed()) return Why.PRIVATE;
        if (PriceRow.filled(sales.prices) == 0) return Why.NO_PRICE;
        return Why.OK;
    }

    /** How many of {@code stack} the inventory takes (main + hotbar, never the armor or off hand slots). */
    public static int room(Inventory inv, ItemStack stack) {
        int room = 0, max = stack.getMaxStackSize();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack at = inv.getItem(i);
            if (at.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(at, stack)) room += Math.max(0, Math.min(max, at.getMaxStackSize()) - at.getCount());
            if (room >= stack.getCount()) return room;
        }
        return room;
    }

    /** Moves earnings into the inventory, only as much as fits (nothing drops, nothing ever goes the other way). Returns the moved count. */
    public static int collect(EnderShops data, ServerPlayer player, EnderShops.Vault sales) {
        int moved = 0;
        Inventory inv = player.getInventory();
        for (NonNullList<ItemStack> list : data.earningsChain(sales)) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack s = list.get(i);
                if (s.isEmpty()) continue;
                int fit = Math.min(s.getCount(), room(inv, s));
                if (fit <= 0) continue;
                ItemStack part = s.copyWithCount(fit);
                inv.add(part);
                int added = fit - part.getCount();
                // Whatever the inventory refused (it should not) goes back where it came from.
                s.shrink(added);
                if (s.isEmpty()) list.set(i, ItemStack.EMPTY);
                moved += added;
            }
        }
        return moved;
    }
}
