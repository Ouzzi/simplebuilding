package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedMap;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedTradePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The deed's buy tab (WH26-9, owner 08.11.: variant A, like a villager's trade screen): the server's offers on the left
 * ("16 iron -> +3 credits"), the chosen trade on the right with its "Trade" button, the player's inventory below and
 * the credits badge with the claim-credit coin at the top. Display only: the inventory is the client's copy, the server
 * takes the items ({@code DeedService.handleTrade}) and answers with a fresh snapshot, which reopens this tab.
 * One-way (DEED07-BACK = no): there is no trade from credits back to items.
 */
public final class DeedTradeScreen extends DeedCardScreen {
    /** The chosen offer per deed owner, so the answer of a trade keeps it. */
    private static final Map<UUID,Integer> CHOSEN = new HashMap<>();
    private int chosen, scroll;
    private Button trade;
    private Component lastTip;

    public DeedTradeScreen(DeedOpenPayload data) {
        super(Component.translatable("simpletweaks.deed.trade.title"), data);
        chosen = Math.max(0, Math.min(map.offers().size() - 1, CHOSEN.getOrDefault(data.ownerId(), 0)));
    }

    @Override int page() { return 1; }
    @Override void pickDimension(String dimension) { rememberTab(dimension); minecraft.gui.setScreen(new DeedMapScreen(data)); }

    /** How many of the offer's items the player carries (the client's copy of the inventory). */
    private int have(DeedMap.Offer offer) {
        if (minecraft == null || minecraft.player == null) return 0;
        var inv = minecraft.player.getInventory();
        var item = stackOf(offer.id(), 1).getItem();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    @Override
    protected void init() {
        super.init();
        var offers = map.offers();
        int rows = layout.offerRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, offers.size() - rows)));
        if (chosen < scroll) scroll = chosen;
        if (chosen >= scroll + rows) scroll = chosen - rows + 1;
        for (int row = 0; row < rows && scroll + row < offers.size(); row++) {
            int index = scroll + row;
            var o = offers.get(index);
            var r = layout.offer(row);
            var button = addRenderableWidget(new OfferRow(r, index, o));
            button.setTooltip(Tooltip.create(Component.translatable("simpletweaks.deed.trade.offer.tip", o.count(), itemName(o.id()), o.credits())));
        }
        if (offers.isEmpty()) return;
        Rect b = layout.tradeButton();
        lastTip = null;
        trade = addRenderableWidget(Button.builder(Component.translatable("simpletweaks.deed.trade.do"),
                x -> DeedNet.toServer.accept(new DeedTradePayload(data.hand(), chosen))).bounds(b.x(), b.y(), b.w(), b.h()).build());
        refreshTrade();
    }

    /** The Trade button follows the inventory (what the player carries) and the limit; the server re-checks both. */
    private void refreshTrade() {
        if (trade == null || map.offers().isEmpty()) return;
        var o = map.offers().get(chosen);
        Component why = map.buy() != DeedMap.BUY ? Component.translatable("simpletweaks.deed.trade.limit")
                : o.credits() > map.room() ? Component.translatable("simpletweaks.deed.trade.room", map.room())
                : have(o) < o.count() ? Component.translatable("simpletweaks.deed.trade.missing", o.count() - have(o), itemName(o.id())) : null;
        trade.active = why == null;
        Component tip = why != null ? why : Component.translatable("simpletweaks.deed.trade.do.tip", o.count(), itemName(o.id()), o.credits());
        if (!tip.equals(lastTip)) { lastTip = tip; trade.setTooltip(Tooltip.create(tip)); }
    }

    @Override
    public void tick() {
        super.tick();
        // the inventory changes under the open tab (pick-up, the trade itself): the button follows it
        refreshTrade();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    void drawCard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect t = layout.tradeTitle();
        g.text(font, fit(getTitle().getString(), t.w()), t.x(), t.y(), DeedStyle.P_INK, false);
        badge(g, layout.tradeBadge(), map.credits());
        var offers = map.offers();
        Rect d = layout.tradeDetail();
        if (offers.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.trade.none"), d.x(), d.y(), d.w(), DeedStyle.P_SOFT, false);
        } else {
            var o = offers.get(chosen);
            Component result = resultText();
            int have = have(o);
            Component head = result != null ? result : Component.translatable("simpletweaks.deed.trade.have", have);
            int color = result != null ? (resultOk() ? DeedStyle.P_OK : DeedStyle.P_ERROR) : have >= o.count() ? DeedStyle.P_OK : DeedStyle.P_ERROR;
            g.text(font, fit(head.getString(), d.w()), d.x(), d.y() + 2, color, false);
            Rect p = layout.priceSlot(), c = layout.creditSlot();
            slot(g, p);
            var stack = stackOf(o.id(), o.count());
            g.item(stack, p.x() + 1, p.y() + 1);
            g.itemDecorations(font, stack, p.x() + 1, p.y() + 1);
            int mid = (p.right() + c.x()) / 2;
            DeedIcons.arrow(g, mid - 4, p.y() + 6, DeedStyle.P_INK);
            slot(g, c);
            DeedIcons.coin(g, c.x() + 3, c.y() + 3);
            g.text(font, "+" + o.credits(), c.right() + 3, c.y() + 5, DeedStyle.P_INK, false);
        }
        drawInventory(g, mouseX, mouseY);
    }

    private static void slot(GuiGraphicsExtractor g, Rect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), DeedStyle.SLOT_EDGE);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, DeedStyle.SLOT);
    }

    /** The player's 36 slots, Vanilla grey, the offer's item marked; hovering shows the item's tooltip. */
    private void drawInventory(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (minecraft.player == null) return;
        var inv = minecraft.player.getInventory();
        var wanted = map.offers().isEmpty() ? null : stackOf(map.offers().get(chosen).id(), 1).getItem();
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) {
            Rect r = layout.inventorySlot(i);
            slot(g, r);
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (wanted != null && stack.is(wanted)) g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, 0xFFB7A35E);
            g.item(stack, r.x() + 1, r.y() + 1);
            g.itemDecorations(font, stack, r.x() + 1, r.y() + 1);
            if (r.contains(mouseX, mouseY)) hovered = stack;
        }
        if (!hovered.isEmpty()) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXDelta, double scrollYDelta) {
        int rows = layout.offerRows(), n = map.offers().size();
        Rect first = layout.offer(0);
        if (n > rows && mouseX >= first.x() && mouseX < first.right() && scrollYDelta != 0) {
            scroll = Math.max(0, Math.min(n - rows, scroll + (scrollYDelta > 0 ? -1 : 1)));
            if (chosen < scroll) chosen = scroll;
            if (chosen >= scroll + rows) chosen = scroll + rows - 1;
            CHOSEN.put(data.ownerId(), chosen);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollXDelta, scrollYDelta);
    }

    /** One offer of the list: item x count, arrow, coin +credits; the chosen one is brown with light text. */
    private final class OfferRow extends AbstractButton {
        private final int index;
        private final DeedMap.Offer offer;

        OfferRow(Rect r, int index, DeedMap.Offer offer) {
            super(r.x(), r.y(), r.w(), r.h(), Component.translatable("simpletweaks.deed.trade.offer", offer.count(), itemName(offer.id()), offer.credits()));
            this.index = index; this.offer = offer;
        }
        @Override public void onPress(InputWithModifiers input) { chosen = index; CHOSEN.put(data.ownerId(), index); rebuildWidgets(); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            boolean on = index == chosen;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int face = on ? DeedStyle.OFFER_ON : isHoveredOrFocused() ? com.simplelib.api.client.ui.UiPalette.mix(DeedStyle.PAPER_DARK, 0xFFFFFFFF, 0.3) : DeedStyle.PAPER_DARK;
            com.simplelib.api.client.ui.UiBoxes.stairFrame(g, x, y, w, h, new int[]{DeedStyle.EDGE, DeedStyle.CREASE}, face);
            int ink = on ? DeedStyle.OFFER_ON_TEXT : DeedStyle.P_INK;
            g.item(stackOf(offer.id(), 1), x + 2, y + 1);
            g.text(font, "x" + offer.count(), x + 20, y + 5, ink, false);
            int ax = x + w - 42;
            DeedIcons.arrow(g, ax, y + 6, ink);
            DeedIcons.coin(g, ax + 11, y + 3);
            g.text(font, "+" + offer.credits(), ax + 24, y + 5, ink, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
