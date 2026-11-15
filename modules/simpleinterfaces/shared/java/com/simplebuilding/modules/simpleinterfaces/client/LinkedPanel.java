package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Layout;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedLayout.Rect;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedMenu;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedSlot;
import com.simplebuilding.modules.simpleinterfaces.mixin.SlotPositionAccessor;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Client side of the linked GUIs: the marked containers' slots appended to the open menu and arranged by
 * {@link LinkedLayout} (N65): below the station with a hotbar strip and the inventory behind a tab, beside the screen,
 * scrolling, or side tabs for two double chests. Only client slot positions change (the server knows slots by index);
 * every moved slot remembers its home ({@link #homeX}/{@link #homeY}) so the screen's own style keeps measuring it there.
 */
public final class LinkedPanel {
    /** Matt bronze like the Octant screen and the HUD tiles (owner N65 16.10.: no more bright orange). */
    private static final UiPalette PALETTE = UiPalette.derived(0xFF6F5F4B);
    private static final int HIDDEN = -10000;
    /** Where the panel found each slot it moved (client slots of open menus; weak, closed menus drop out). */
    private static final Map<Slot, int[]> HOME = new WeakHashMap<>();

    private final List<LinkedOpenPayload.Entry> entries;
    private final int[] starts;
    private int tab;
    private int scroll;
    private @Nullable Layout layout;
    private LinkedLayout.@Nullable Input input;

    private LinkedPanel(List<LinkedOpenPayload.Entry> entries) {
        this.entries = List.copyOf(entries);
        this.starts = new int[entries.size()];
        int at = 0;
        for (int i = 0; i < entries.size(); i++) {
            this.starts[i] = at;
            at += entries.get(i).size();
        }
    }

    /** Item x of {@code slot} where its screen put it (before the panel moved it). */
    public static int homeX(Slot slot) {
        int[] home = HOME.get(slot);
        return home == null ? slot.x : home[0];
    }

    /** Item y of {@code slot} where its screen put it. */
    public static int homeY(Slot slot) {
        int[] home = HOME.get(slot);
        return home == null ? slot.y : home[1];
    }

    /** Payload handler (client thread, before the menu's first content packet). */
    public static void receive(LinkedOpenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (payload.containerId() == LinkedOpenPayload.HUD) {
            LinkedHud.set(payload.entries());
            return;
        }
        AbstractContainerMenu own = minecraft.player.inventoryMenu;
        AbstractContainerMenu menu = payload.containerId() == own.containerId ? own : minecraft.player.containerMenu;
        if (menu.containerId != payload.containerId()) return;
        boolean linked = LinkedContainers.hasLinked(menu);
        if (linked && menu != own) return;
        if (linked) {
            int first = 0;
            while (!(menu.slots.get(first) instanceof LinkedSlot)) first++;
            ((LinkedMenu) menu).qol$truncate(first);
            ((LinkedMenu) menu).qol$panel(null);
        }
        int size = payload.size();
        if (payload.entries().isEmpty() || size <= 0 || size > LinkedContainers.MAX_SLOTS) {
            if (linked) relayout(minecraft, menu);
            return;
        }
        for (LinkedOpenPayload.Entry entry : payload.entries()) {
            SimpleContainer mirror = new SimpleContainer(entry.size());
            for (int i = 0; i < entry.size(); i++) ((LinkedMenu) menu).qol$addSlot(new LinkedSlot(mirror, i, null));
        }
        ((LinkedMenu) menu).qol$panel(new LinkedPanel(payload.entries()));
        relayout(minecraft, menu);
    }

    private static void relayout(Minecraft minecraft, AbstractContainerMenu menu) {
        if (minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() == menu && screen instanceof LinkedScreen l) l.qol$layout();
    }

    /** Whether {@code screen} may be moved for C (its background and widgets follow {@code leftPos}). */
    public static boolean clean(AbstractContainerScreen<?> screen) {
        return LinkedLayout.clean(screen.getClass().getName(), StyledScreens.style(screen) != null);
    }

    public @Nullable Layout current() {
        return this.layout;
    }

    /**
     * Arranges the panel for the screen as its own {@code init} left it ({@code leftPos/topPos} unshifted) and moves the
     * slots; returns the layout, whose {@code dx/dy} the screen applies.
     */
    public Layout layout(AbstractContainerMenu menu, int leftPos, int topPos, int imageWidth, int imageHeight, int width, int height,
            boolean clean, boolean inventoryScreen) {
        int inventoryTop = Integer.MAX_VALUE;
        int own = 0;
        for (Slot slot : menu.slots) {
            if (slot instanceof LinkedSlot) continue;
            int x = homeX(slot);
            if (x < 0 || x + 16 > imageWidth) continue;
            if (player(slot)) inventoryTop = Math.min(inventoryTop, homeY(slot));
            else if (!(slot.container instanceof Inventory)) own++;
        }
        List<Integer> sizes = new ArrayList<>();
        for (LinkedOpenPayload.Entry e : this.entries) sizes.add(e.size());
        this.input = new LinkedLayout.Input(width, height, imageWidth, imageHeight, leftPos, topPos,
                inventoryTop == Integer.MAX_VALUE ? -1 : inventoryTop, own >= 27 && own % 9 == 0 ? own / 9 : 0, inventoryScreen, clean, sizes, this.tab);
        this.layout = LinkedLayout.compute(this.input);
        this.tab = this.layout.tab();
        this.place(menu);
        return this.layout;
    }

    /** The player's own inventory and hotbar. */
    private static boolean player(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }

    private static void move(Slot slot, int x, int y) {
        HOME.computeIfAbsent(slot, s -> new int[] {s.x, s.y});
        ((SlotPositionAccessor) slot).qol$setX(x);
        ((SlotPositionAccessor) slot).qol$setY(y);
    }

    private static void home(Slot slot) {
        int[] home = HOME.get(slot);
        if (home == null) return;
        ((SlotPositionAccessor) slot).qol$setX(home[0]);
        ((SlotPositionAccessor) slot).qol$setY(home[1]);
    }

    /** Content rows of the open tab (for scrolling). */
    private int contentRows() {
        Layout l = this.layout;
        if (l == null || l.box() == null) return 0;
        return l.shows() == LinkedLayout.Tab.INVENTORY ? LinkedLayout.INVENTORY_ROWS : LinkedLayout.rows(this.entries.get(l.part()).size());
    }

    private void place(AbstractContainerMenu menu) {
        Layout l = this.layout;
        if (l == null) return;
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, this.contentRows() - l.rows()));
        boolean strip = l.hotbar() != null;
        boolean inventoryTab = l.shows() == LinkedLayout.Tab.INVENTORY;
        boolean tabsMarked = l.mode() == LinkedLayout.Mode.TABS && l.part() >= 0;
        int index = 0;
        for (Slot slot : menu.slots) {
            if (slot instanceof LinkedSlot linked) {
                int part = this.partOf(index);
                int i = index - this.starts[part];
                index++;
                int row = i / 9 - this.scroll;
                linked.shown = part == l.part() && row >= 0 && row < l.rows();
                if (linked.shown) {
                    ((SlotPositionAccessor) slot).qol$setX(l.slotX(i % 9));
                    ((SlotPositionAccessor) slot).qol$setY(l.slotY(row));
                } else {
                    ((SlotPositionAccessor) slot).qol$setY(HIDDEN);
                }
                continue;
            }
            if (player(slot) && homeX(slot) >= 0) {
                int c = slot.getContainerSlot();
                if (!strip) {
                    home(slot);
                } else if (c < 9) {
                    move(slot, l.hotbarX(c), l.hotbarY());
                } else {
                    int row = (c - 9) / 9 - this.scroll;
                    if (inventoryTab && row >= 0 && row < l.rows()) move(slot, l.slotX((c - 9) % 9), l.slotY(row));
                    else move(slot, homeX(slot), HIDDEN);
                }
            } else if (!(slot.container instanceof Inventory) && l.mode() == LinkedLayout.Mode.TABS) {
                if (tabsMarked) move(slot, homeX(slot), HIDDEN);
                else home(slot);
            }
        }
    }

    private int partOf(int index) {
        int part = 0;
        while (part + 1 < this.starts.length && index >= this.starts[part + 1]) part++;
        return part;
    }

    private static boolean inside(@Nullable Rect r, double x, double y, int left, int top) {
        return r != null && x >= left + r.x() && x < left + r.right() && y >= top + r.y() && y < top + r.bottom();
    }

    /** Inside the box, the hotbar strip or a tab, in screen coordinates ({@code left/top} = shifted image origin). */
    public boolean contains(double x, double y, int left, int top) {
        Layout l = this.layout;
        if (l == null) return false;
        if (inside(l.box(), x, y, left, top) || inside(l.hotbar(), x, y, left, top)) return true;
        for (Rect r : l.tabs()) if (inside(r, x, y, left, top)) return true;
        return false;
    }

    /** A click on a tab: opens it (relayout with the same shift, the group never jumps). True when a tab was hit. */
    public boolean clickTab(AbstractContainerMenu menu, double x, double y, int left, int top) {
        Layout l = this.layout;
        if (l == null || this.input == null) return false;
        for (int i = 0; i < l.tabs().size(); i++) {
            if (!inside(l.tabs().get(i), x, y, left, top)) continue;
            if (i != this.tab) {
                this.tab = i;
                this.scroll = 0;
                this.input = this.input.withTab(i);
                this.layout = LinkedLayout.compute(this.input);
                this.place(menu);
            }
            return true;
        }
        return false;
    }

    /** Mouse wheel over the box; true when it scrolled. */
    public boolean scroll(AbstractContainerMenu menu, double delta) {
        Layout l = this.layout;
        if (l == null || !l.scroll() || delta == 0) return false;
        this.scroll = Math.clamp(this.scroll - (int) Math.signum(delta), 0, this.contentRows() - l.rows());
        this.place(menu);
        return true;
    }

    /**
     * Draws the panel in the container style (SimpleLib's style blocks; owner images 3/4; margins after A7): the box of the
     * open tab (bronze for a marked container, light for the inventory) with its title and sunk slots, the scroll bar in its
     * own column, the hotbar strip, the tabs (block item, the closed ones dimmed). The pose is already translated to the
     * screen's {@code leftPos/topPos}.
     */
    public void render(GuiGraphicsExtractor graphics, Font font, Component inventoryTitle) {
        Layout l = this.layout;
        if (l == null) return;
        if (l.box() != null) {
            Rect b = l.box();
            boolean inventory = l.shows() == LinkedLayout.Tab.INVENTORY;
            UiPalette p = inventory ? UiPalette.INVENTORY : PALETTE;
            UiBoxes.box(graphics, b.x(), b.y(), b.width(), b.height(), p);
            int size = inventory ? 27 : this.entries.get(l.part()).size();
            for (int i = 0; i < size; i++) {
                int row = i / 9 - this.scroll;
                if (row >= 0 && row < l.rows()) UiBoxes.slot(graphics, l.slotX(i % 9), l.slotY(row), p);
            }
            Component title = inventory ? inventoryTitle : Component.translatable("container.simpleinterfaces.linked", this.entries.get(l.part()).title());
            graphics.text(font, title, b.x() + LinkedLayout.EDGE, b.y() + 6, p.label(), false);
            if (l.scroll()) {
                int rows = this.contentRows();
                int track = l.rows() * LinkedLayout.SLOT - 1;
                int thumb = Math.max(6, track * l.rows() / rows);
                int x = b.x() + LinkedLayout.SCROLL_X;
                int top = l.slotY(0);
                int y = top + (track - thumb) * this.scroll / Math.max(1, rows - l.rows());
                graphics.fill(x, top, x + LinkedLayout.SCROLL_BAR, top + track, p.slotTop());
                graphics.fill(x, y, x + LinkedLayout.SCROLL_BAR, y + thumb, p.light());
            }
        }
        if (l.hotbar() != null) {
            Rect h = l.hotbar();
            UiBoxes.box(graphics, h.x(), h.y(), h.width(), h.height(), UiPalette.INVENTORY);
            for (int i = 0; i < 9; i++) UiBoxes.slot(graphics, l.hotbarX(i), l.hotbarY(), UiPalette.INVENTORY);
        }
        int marked = 0;
        for (int i = 0; i < l.tabs().size(); i++) {
            Rect t = l.tabs().get(i);
            LinkedLayout.Tab kind = l.tabKinds().get(i);
            UiPalette p = kind == LinkedLayout.Tab.MARKED ? PALETTE : UiPalette.INVENTORY;
            UiBoxes.box(graphics, t.x(), t.y(), t.width(), t.height(), p, false);
            if (i != l.tab()) graphics.fill(t.x() + UiBoxes.FRAME, t.y() + UiBoxes.FRAME, t.right() - UiBoxes.FRAME, t.bottom() - UiBoxes.FRAME, 0x60000000);
            ItemStack icon = switch (kind) {
                case MARKED -> this.entries.get(marked++).icon();
                case INVENTORY -> new ItemStack(Items.BUNDLE);
                case OPENED -> new ItemStack(Items.CHEST);
            };
            graphics.item(icon, t.x() + 4, t.y() + 4);
            if (kind == LinkedLayout.Tab.MARKED) graphics.fill(t.right() - 5, t.bottom() - 5, t.right() - 3, t.bottom() - 3, LinkedHud.DOT);
        }
    }
}
