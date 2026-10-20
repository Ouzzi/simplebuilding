package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.logic.EnderShops;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopTrade;
import com.simpleshops.logic.TradeTerminal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The trade terminal (Handelskontor, KONZ20 S5 k): two tabs over the ender shop data, no own network packets. Lists are read-only
 * slots whose item carries the row's text as name and lore; everything else is menu buttons and data slots.
 * <ul>
 * <li>Tab "My shops": pick one of your shops; collect its earnings (only out, only what fits), edit its price row (same template
 * clicks as on the sales block), switch it public/private, and take goods out (KONZ20 S2 W2: only from a public shop with a
 * price, with a delivery time per stack; the goods are reserved at once and go back if the delivery fails).</li>
 * <li>Tab "Browse shops": every public shop with a price (players and server), search (item name, mod id, shop), filter
 * "in stock", sort by price/distance/owner, pages of 18. Buying is the buyer menu's path ({@link ShopTrade#executeRemote}).</li>
 * </ul>
 * A selected shop is held with the same one-player lock as on site. Only the terminal's dimension is reachable unless the
 * server allows more (KONZ20 S3). Catalog queries are rate-limited (4 per second).
 */
public class TradeTerminalMenu extends AbstractContainerMenu implements EnderShops.RemoteHolder {
    public static final int WIDTH = 176, HEIGHT = 236, LIST_X = 8, LIST_Y = 36, ROW_X = 60, ROW_A_Y = 90, ROW_B_Y = 110, BUTTON_Y = 130, INVENTORY_Y = 154;
    public static final int LIST = TradeTerminal.PAGE, ROW_A = LIST, ROW_B = ROW_A + 5, PAY = ROW_B + 5, INVENTORY_START = PAY + 5;
    public static final int TAB_MINE = 0, TAB_BROWSE = 1;
    /** Menu buttons. Characters of the search text: {@link #CHAR} + code point; buy: {@link #BUY}/{@link #BUY_MAX} | revision. */
    public static final int B_TAB_MINE = 20, B_TAB_BROWSE = 21, B_PREV = 22, B_NEXT = 23, B_STOCK = 24, B_SORT = 25, B_CLEAR = 26, B_APPLY = 27,
            B_COLLECT = 28, B_PUBLIC = 29, CHAR = 0x10000, BUY = 0x100000, BUY_MAX = 0x200000;
    /** Status texts (data slot value = index). */
    public static final String[] STATUS = {"", "done", "busy", "far", "private", "no_price", "own", "no_space", "sold_out", "pay_exactly",
            "earnings_full", "stale", "slow", "off", "gone", "not_owner", "delivering"};
    public static final int S_NONE = 0, S_DONE = 1, S_BUSY = 2, S_FAR = 3, S_PRIVATE = 4, S_NO_PRICE = 5, S_OWN = 6, S_NO_SPACE = 7, S_SOLD_OUT = 8,
            S_PAY = 9, S_FULL = 10, S_STALE = 11, S_SLOW = 12, S_OFF = 13, S_GONE = 14, S_NOT_OWNER = 15, S_DELIVERING = 16;
    /** Shop flags (data slot bits). */
    public static final int F_PUBLIC = 1, F_FULL = 2, F_TAKE = 4, F_BUY = 8, F_SELECTED = 16;
    public static final int RATE_TICKS = 5, MAX_DELIVERIES = 5;

    private final ContainerLevelAccess access;
    private final @Nullable ServerPlayer player;
    private final SimpleContainer listView = new SimpleContainer(LIST);
    private final SimpleContainer rowA = new SimpleContainer(5);
    private final SimpleContainer rowB = new SimpleContainer(5);
    private final SimpleContainer pay = new SimpleContainer(PriceRow.SIZE);
    private final DataSlot tab = DataSlot.standalone(), page = DataSlot.standalone(), pages = DataSlot.standalone(), selected = DataSlot.standalone(),
            options = DataSlot.standalone(), status = DataSlot.standalone(), revision = DataSlot.standalone(), pending = DataSlot.standalone(),
            progress = DataSlot.standalone(), flags = DataSlot.standalone();
    // --- server state ---
    private List<TradeTerminal.Row> rows = List.of();
    private final StringBuilder query = new StringBuilder();
    private String appliedQuery = "";
    private @Nullable String shopId;
    private int ware = -1;
    private @Nullable String lockedId;
    private long lastQuery = Long.MIN_VALUE / 2, lastAction, lastRefresh;
    private boolean closed;
    private final List<Delivery> deliveries = new ArrayList<>();
    /** Last outcome for tests. */
    public int lastStatus;

    /** Goods reserved in a display case entry on their way to this terminal's player. */
    private record Delivery(String caseId, ItemStack stack, long due) {}

    /** Client side (menu type factory). */
    public TradeTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
    }

    public TradeTerminalMenu(int containerId, Inventory inventory, ContainerLevelAccess access, @Nullable ServerPlayer player) {
        super(ShopsRegistry.TERMINAL_MENU, containerId);
        this.access = access;
        this.player = player;
        for (int i = 0; i < LIST; i++) addSlot(new ShopMenu.ViewSlot(this.listView, i, LIST_X + (i % 9) * 18, LIST_Y + (i / 9) * 18, x -> true));
        for (int i = 0; i < 5; i++) addSlot(new ShopMenu.ViewSlot(this.rowA, i, ROW_X + i * 18, ROW_A_Y, x -> this.flags.get() != 0));
        for (int i = 0; i < 5; i++) {
            addSlot(new ShopMenu.ViewSlot(this.rowB, i, ROW_X + i * 18, ROW_B_Y, x -> this.tab.get() == TAB_MINE && (this.flags.get() & F_SELECTED) != 0));
        }
        for (int p = 0; p < PriceRow.SIZE; p++) {
            final int position = p;
            addSlot(new Slot(this.pay, p, ROW_X + p * 18, ROW_B_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack price = TradeTerminalMenu.this.rowA.getItem(position);
                    return TradeTerminalMenu.this.tab.get() == TAB_BROWSE && !price.isEmpty() && ItemStack.isSameItemSameComponents(price, stack);
                }

                @Override
                public boolean isActive() {
                    return TradeTerminalMenu.this.tab.get() == TAB_BROWSE && !TradeTerminalMenu.this.rowA.getItem(position).isEmpty();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        for (DataSlot d : new DataSlot[] {this.tab, this.page, this.pages, this.selected, this.options, this.status, this.revision, this.pending, this.progress, this.flags}) {
            addDataSlot(d);
        }
        this.selected.set(-1);
        this.pages.set(1);
        if (player != null) {
            this.lastAction = player.level().getGameTime();
            rebuild();
        }
    }

    // --- client view ---------------------------------------------------------------------------------------------------

    public int tab() {
        return this.tab.get();
    }

    public int page() {
        return this.page.get();
    }

    public int pages() {
        return Math.max(1, this.pages.get());
    }

    public int selectedRow() {
        return this.selected.get();
    }

    public boolean inStockOnly() {
        return (this.options.get() & 1) != 0;
    }

    public int sort() {
        return (this.options.get() >> 1) & 3;
    }

    public int status() {
        return this.status.get();
    }

    public int revisionSeen() {
        return this.revision.get();
    }

    public int pendingDeliveries() {
        return this.pending.get();
    }

    public int deliveryProgress() {
        return this.progress.get();
    }

    public int shopFlags() {
        return this.flags.get();
    }

    public SimpleContainer pay() {
        return this.pay;
    }

    public net.minecraft.world.Container listView() {
        return this.listView;
    }

    public net.minecraft.world.Container rowA() {
        return this.rowA;
    }

    /** Client: the pay slots hold every price exactly (the buy button is on). */
    public boolean canBuyClient() {
        if (this.tab.get() != TAB_BROWSE || (this.flags.get() & F_BUY) == 0 || (this.flags.get() & F_FULL) != 0) return false;
        boolean any = false;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack price = this.rowA.getItem(p);
            if (price.isEmpty()) continue;
            any = true;
            ItemStack paid = this.pay.getItem(p);
            if (paid.isEmpty() || !ItemStack.isSameItemSameComponents(price, paid) || paid.getCount() < price.getCount()) return false;
        }
        return any;
    }

    // --- server --------------------------------------------------------------------------------------------------------

    @Override
    public boolean holdsShop(String vaultId) {
        return !this.closed && vaultId.equals(this.lockedId);
    }

    private @Nullable EnderShops data() {
        return this.player == null ? null : EnderShops.get(this.player.level().getServer());
    }

    private EnderShops.@Nullable Vault shop() {
        EnderShops data = data();
        return data == null || this.shopId == null ? null : data.find(this.shopId);
    }

    private void setStatus(int s) {
        this.status.set(s);
        this.lastStatus = s;
    }

    /** Runs a catalog query now, if the rate limit allows it ({@code force}: the server's own refresh). */
    private boolean query(boolean force) {
        if (this.player == null) return false;
        long now = this.player.level().getGameTime();
        if (!force && now - this.lastQuery < RATE_TICKS) {
            setStatus(S_SLOW);
            return false;
        }
        if (!force) this.lastQuery = now;
        this.lastRefresh = now;
        rebuild();
        return true;
    }

    /** Recomputes the list, the selected rows and the flags from the world data. */
    private void rebuild() {
        EnderShops data = data();
        if (data == null || this.player == null) return;
        this.rows = this.tab.get() == TAB_MINE ? TradeTerminal.mine(data, this.player)
                : TradeTerminal.browse(data, this.player, this.appliedQuery, inStockOnly(), sort());
        int count = Math.max(1, (this.rows.size() + LIST - 1) / LIST);
        this.pages.set(count);
        if (this.page.get() >= count) this.page.set(count - 1);
        if (this.page.get() < 0) this.page.set(0);
        int start = this.page.get() * LIST;
        int sel = -1;
        for (int i = 0; i < LIST; i++) {
            TradeTerminal.Row r = start + i < this.rows.size() ? this.rows.get(start + i) : null;
            ItemStack icon = r == null ? ItemStack.EMPTY : r.icon();
            if (!ItemStack.matches(this.listView.getItem(i), icon)) this.listView.setItem(i, icon);
            if (r != null && r.shop().equals(this.shopId) && (this.tab.get() == TAB_MINE || r.ware() == this.ware)) sel = i;
        }
        this.selected.set(sel);
        refreshShop();
    }

    /** Rows A/B and the flags of the selected shop. */
    private void refreshShop() {
        EnderShops data = data();
        EnderShops.Vault v = shop();
        if (this.shopId != null && (v == null || v.deleted() || !TradeTerminal.reachable(this.player, v))) {
            select(null, -1);
            v = null;
        }
        int f = 0;
        ItemStack[] a = new ItemStack[5], b = new ItemStack[5];
        java.util.Arrays.fill(a, ItemStack.EMPTY);
        java.util.Arrays.fill(b, ItemStack.EMPTY);
        if (v != null && data != null) {
            f |= F_SELECTED;
            EnderShops.Vault box = data.caseOf(v);
            if (v.listed()) f |= F_PUBLIC;
            if (!ShopTrade.fitsRemote(data, v)) f |= F_FULL;
            if (this.tab.get() == TAB_MINE) {
                if (box != null) for (int i = 0; i < 5; i++) a[i] = box.items.get(i).copy();
                for (int i = 0; i < 5; i++) b[i] = v.prices.get(i).copy();
                if (TradeTerminal.mayTake(this.player, v) == TradeTerminal.Why.OK) f |= F_TAKE;
            } else {
                for (int i = 0; i < 5; i++) a[i] = v.prices.get(i).copy();
                boolean stocked = box != null && this.ware >= 0 && !box.items.get(this.ware).isEmpty();
                if (TradeTerminal.mayBuy(this.player, v) == TradeTerminal.Why.OK && stocked && this.lockedId != null) f |= F_BUY;
            }
            this.revision.set(v.revision() & 0xFFFF);
        }
        for (int i = 0; i < 5; i++) {
            if (!ItemStack.matches(this.rowA.getItem(i), a[i])) this.rowA.setItem(i, a[i]);
            if (!ItemStack.matches(this.rowB.getItem(i), b[i])) this.rowB.setItem(i, b[i]);
        }
        this.flags.set(f);
    }

    /** Selects a shop (and ware), taking its lock; the old lock is released. A busy shop is not selected. */
    private boolean select(@Nullable String id, int newWare) {
        EnderShops data = data();
        if (this.player == null || data == null) return false;
        EnderShops.Vault next = id == null ? null : data.find(id);
        boolean needLock = next != null && (this.tab.get() == TAB_MINE || !next.isOwner(this.player.getUUID()));
        if (next != null && needLock && !id.equals(this.lockedId) && next.lockHolder() != null && next.lockHolder() != this.player) {
            setStatus(S_BUSY);
            return false;
        }
        if (this.lockedId != null && !this.lockedId.equals(id)) {
            EnderShops.Vault old = data.find(this.lockedId);
            if (old != null) old.unlock(this);
            this.lockedId = null;
        }
        if (!java.util.Objects.equals(this.shopId, id)) clearContainer(this.player, this.pay);
        this.shopId = id;
        this.ware = newWare;
        if (next != null && needLock) {
            next.lock(this.player, this, true);
            this.lockedId = id;
        }
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player who) {
        touch(who);
        if (slotIndex >= 0 && slotIndex < PAY) {
            // Lists and rows are display only: no click type ever moves an item there; a plain click is an action (server).
            if (input == ContainerInput.PICKUP && who instanceof ServerPlayer sp && sp == this.player && stillValid(sp)) {
                if (slotIndex < LIST) clickRow(slotIndex);
                else if (slotIndex < ROW_B && this.tab.get() == TAB_MINE && getCarried().isEmpty()) take(slotIndex - ROW_A);
                else if (slotIndex >= ROW_B && this.tab.get() == TAB_MINE) editPrice(slotIndex - ROW_B, button == 1);
                broadcastChanges();
            }
            return;
        }
        if (slotIndex >= PAY && slotIndex < INVENTORY_START && this.tab.get() != TAB_BROWSE) return;
        super.clicked(slotIndex, button, input, who);
    }

    private void clickRow(int i) {
        int index = this.page.get() * LIST + i;
        if (index >= this.rows.size()) return;
        TradeTerminal.Row r = this.rows.get(index);
        EnderShops data = data();
        EnderShops.Vault v = data == null ? null : data.find(r.shop());
        if (v == null) {
            setStatus(S_GONE);
            return;
        }
        if (!TradeTerminal.reachable(this.player, v)) {
            setStatus(S_FAR);
            return;
        }
        if (select(r.shop(), r.ware())) {
            setStatus(this.tab.get() == TAB_BROWSE && v.isOwner(this.player.getUUID()) ? S_OWN : S_NONE);
            rebuild();
        }
    }

    private void editPrice(int position, boolean right) {
        EnderShops.Vault v = shop();
        if (v == null || !v.isOwner(this.player.getUUID()) || !holdsShop(v.id)) return;
        if (PriceRow.click(v.prices, position, getCarried(), right)) {
            v.bumpRevision();
            EnderShops.touch(this.player.level().getServer(), v);
            refreshShop();
        }
    }

    /** KONZ20 S2 W2: one stack out of the own shop's case, delivered after the configured time; reserved at once. */
    private void take(int slot) {
        EnderShops data = data();
        EnderShops.Vault v = shop();
        if (data == null || v == null || !holdsShop(v.id)) return;
        TradeTerminal.Why why = TradeTerminal.mayTake(this.player, v);
        if (why != TradeTerminal.Why.OK) {
            setStatus(code(why));
            return;
        }
        EnderShops.Vault box = data.caseOf(v);
        if (box == null || box.items.get(slot).isEmpty()) {
            setStatus(S_SOLD_OUT);
            return;
        }
        if (this.deliveries.size() >= MAX_DELIVERIES || box.transit().size() >= EnderShops.MAX_TRANSIT) {
            setStatus(S_SLOW);
            return;
        }
        ItemStack stack = box.items.get(slot);
        // No room = no delivery (nothing ever drops at the terminal); the room of earlier deliveries counts too.
        int need = stack.getCount();
        for (Delivery d : this.deliveries) if (ItemStack.isSameItemSameComponents(d.stack(), stack)) need += d.stack().getCount();
        if (TradeTerminal.room(this.player.getInventory(), stack) < need) {
            setStatus(S_NO_SPACE);
            return;
        }
        box.items.set(slot, ItemStack.EMPTY);
        box.transit().add(stack);
        long start = this.deliveries.isEmpty() ? this.player.level().getGameTime() : this.deliveries.get(this.deliveries.size() - 1).due();
        this.deliveries.add(new Delivery(box.id, stack, start + ShopsConfig.remoteDelaySeconds * 20L));
        v.bumpRevision();
        EnderShops.touch(this.player.level().getServer(), box);
        setStatus(S_DELIVERING);
        refreshShop();
    }

    private static int code(TradeTerminal.Why why) {
        return switch (why) {
            case OK -> S_DONE;
            case OFF -> S_OFF;
            case FAR, DIMENSION -> S_FAR;
            case PRIVATE -> S_PRIVATE;
            case NO_PRICE -> S_NO_PRICE;
            case NOT_OWNER -> S_NOT_OWNER;
            case OWN -> S_OWN;
            case BUSY -> S_BUSY;
            case GONE -> S_GONE;
            case NO_SPACE -> S_NO_SPACE;
            case EMPTY -> S_SOLD_OUT;
            case RATE -> S_SLOW;
        };
    }

    private static int code(ShopTrade.Result r) {
        return switch (r) {
            case OK -> S_DONE;
            case STALE -> S_STALE;
            case NO_STOCK -> S_SOLD_OUT;
            case NO_PRICE -> S_NO_PRICE;
            case WRONG_PAYMENT -> S_PAY;
            case EARNINGS_FULL -> S_FULL;
            case INVALID -> S_GONE;
            case NO_SPACE -> S_NO_SPACE;
            case BUSY -> S_BUSY;
            case FAR -> S_FAR;
        };
    }

    /** Ends due deliveries: into the inventory if it fits, otherwise back into the shop (never dropped, never doubled). */
    private void deliver(boolean all) {
        EnderShops data = data();
        if (data == null || this.deliveries.isEmpty()) {
            this.pending.set(0);
            this.progress.set(0);
            return;
        }
        long now = this.player.level().getGameTime();
        while (!this.deliveries.isEmpty()) {
            Delivery d = this.deliveries.get(0);
            if (!all && d.due() > now) break;
            this.deliveries.remove(0);
            EnderShops.Vault box = data.find(d.caseId());
            // Identity: only the very stack that was reserved (a broken case already dropped it, a restart put it back).
            if (box == null || !removeIdentity(box.transit(), d.stack())) continue;
            ItemStack stack = d.stack();
            if (!all && !this.closed) {
                int fit = Math.min(stack.getCount(), TradeTerminal.room(this.player.getInventory(), stack));
                if (fit > 0) {
                    ItemStack part = stack.copyWithCount(fit);
                    this.player.getInventory().add(part);
                    stack.shrink(fit - part.getCount());
                }
                if (fit > 0) this.player.level().playSound(null, this.player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);
            }
            if (!stack.isEmpty()) {
                box.transit().add(stack);
                box.settle();
            }
            EnderShops.touch(this.player.level().getServer(), box);
        }
        this.pending.set(this.deliveries.size());
        if (this.deliveries.isEmpty()) {
            this.progress.set(0);
            if (this.status.get() == S_DELIVERING) setStatus(S_DONE);
        } else {
            Delivery d = this.deliveries.get(0);
            long total = Math.max(1, ShopsConfig.remoteDelaySeconds * 20L);
            this.progress.set((int) Math.max(0, Math.min(100, 100 - (d.due() - now) * 100 / total)));
        }
    }

    private static boolean removeIdentity(List<ItemStack> list, ItemStack stack) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == stack) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp) || sp != this.player || id < 0 || !stillValid(sp)) return false;
        touch(sp);
        if (id >= BUY) return buy(id >= BUY_MAX ? 64 : 1, id & 0xFFFF);
        if (id >= CHAR) {
            int cp = id - CHAR;
            if (this.query.length() < TradeTerminal.QUERY_MAX && cp >= 32 && Character.isValidCodePoint(cp) && !Character.isISOControl(cp)) this.query.appendCodePoint(cp);
            return true;
        }
        switch (id) {
            case B_TAB_MINE, B_TAB_BROWSE -> {
                int t = id == B_TAB_MINE ? TAB_MINE : TAB_BROWSE;
                if (t != this.tab.get() && query(false)) {
                    select(null, -1);
                    this.tab.set(t);
                    this.page.set(0);
                    rebuild();
                }
            }
            case B_PREV, B_NEXT -> {
                int p = this.page.get() + (id == B_NEXT ? 1 : -1);
                if (p >= 0 && p < pages() && query(false)) {
                    this.page.set(p);
                    rebuild();
                }
            }
            case B_STOCK -> {
                if (query(false)) {
                    this.options.set(this.options.get() ^ 1);
                    rebuild();
                }
            }
            case B_SORT -> {
                if (query(false)) {
                    this.options.set((this.options.get() & 1) | (((sort() + 1) % TradeTerminal.SORTS) << 1));
                    rebuild();
                }
            }
            case B_CLEAR -> this.query.setLength(0);
            case B_APPLY -> {
                if (query(false)) {
                    this.appliedQuery = this.query.toString();
                    this.page.set(0);
                    rebuild();
                }
            }
            case B_COLLECT -> collect();
            case B_PUBLIC -> togglePublic();
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    private void collect() {
        EnderShops data = data();
        EnderShops.Vault v = shop();
        if (data == null || v == null || this.tab.get() != TAB_MINE || !v.isOwner(this.player.getUUID()) || !holdsShop(v.id)) return;
        int moved = TradeTerminal.collect(data, this.player, v);
        if (moved > 0) {
            v.bumpRevision();
            EnderShops.touch(this.player.level().getServer(), v);
            for (String s : v.storages()) {
                EnderShops.Vault st = data.find(s);
                if (st != null) EnderShops.touch(this.player.level().getServer(), st);
            }
        }
        setStatus(moved > 0 ? S_DONE : S_NO_SPACE);
        rebuild();
    }

    private void togglePublic() {
        EnderShops.Vault v = shop();
        if (v == null || this.tab.get() != TAB_MINE || !v.isOwner(this.player.getUUID()) || !holdsShop(v.id)) return;
        v.setListed(!v.listed());
        EnderShops.get(this.player.level().getServer()).setDirty();
        setStatus(S_DONE);
        rebuild();
    }

    private boolean buy(int times, int seen) {
        EnderShops.Vault v = shop();
        if (this.tab.get() != TAB_BROWSE || v == null || this.ware < 0) return false;
        TradeTerminal.Why why = TradeTerminal.mayBuy(this.player, v);
        if (why != TradeTerminal.Why.OK) {
            setStatus(code(why));
            broadcastChanges();
            return true;
        }
        for (int i = 0; i < times; i++) {
            int expected = i == 0 ? seen : v.revision() & 0xFFFF;
            ShopTrade.Result r = ShopTrade.executeRemote(this.player, v, this.ware, expected, this.pay);
            if (r != ShopTrade.Result.OK) {
                if (i == 0) setStatus(code(r));
                break;
            }
            setStatus(S_DONE);
        }
        this.player.level().playSound(null, this.player.blockPosition(), this.lastStatus == S_DONE ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO,
                SoundSource.BLOCKS, 0.5F, 1.1F);
        rebuild();
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (this.player != null && !this.closed) {
            deliver(false);
            long now = this.player.level().getGameTime();
            // The server's own refresh (not rate limited): the world data changes under an open terminal.
            if (now - this.lastRefresh >= 40) {
                this.lastRefresh = now;
                rebuild();
            } else {
                refreshShop();
            }
        }
        super.broadcastChanges();
    }

    private void touch(Player who) {
        if (who instanceof ServerPlayer sp) this.lastAction = sp.level().getGameTime();
    }

    /** Tests: pretends the last click was {@code ticks} ago. */
    public void ageForTest(long ticks) {
        this.lastAction -= ticks;
    }

    /** Tests: the rate limit is behind us. */
    public void coolDownForTest() {
        this.lastQuery = Long.MIN_VALUE / 2;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        if (index >= PAY && index < INVENTORY_START) {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, INVENTORY_START, this.slots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        if (index >= INVENTORY_START && this.tab.get() == TAB_BROWSE) {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, PAY, INVENTORY_START, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player who) {
        if (!stillValid(this.access, who, ShopsRegistry.TRADE_TERMINAL)) return false;
        if (this.player == null) return true;
        return this.player.level().getGameTime() - this.lastAction <= ShopsConfig.idleTimeoutSeconds * 20L;
    }

    @Override
    public void removed(Player who) {
        super.removed(who);
        if (!(who instanceof ServerPlayer) || this.player == null) return;
        // Deliveries still on their way go back into the shop; the pay slots back to the player; the lock is released.
        this.closed = true;
        deliver(true);
        clearContainer(who, this.pay);
        if (this.lockedId != null) {
            EnderShops.Vault v = EnderShops.get(this.player.level().getServer()).find(this.lockedId);
            if (v != null) v.unlock(this);
            this.lockedId = null;
        }
    }

    // --- tests ---------------------------------------------------------------------------------------------------------

    public List<TradeTerminal.Row> rowsForTest() {
        return this.rows;
    }

    public @Nullable String selectedShopForTest() {
        return this.shopId;
    }
}
