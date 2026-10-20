package com.simpleshops.client;

import com.simpleshops.menu.TradeTerminalMenu;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Screen of the trade terminal (Handelskontor) in SimpleLib's box style (palette BARREL like the other shop screens): two tabs
 * on top, the list (two rows of nine), the selected shop's two rows, the action buttons and the inventory panel. The screen only
 * sends menu buttons; the server decides everything. Labels are dark on the light wood (contrast see {@link #LABEL}).
 */
public final class TradeTerminalScreen extends ShopScreens.Base<TradeTerminalMenu> {
    private ShopScreens.BarButton tabMine, tabBrowse, prev, next, stock, sort, collect, publicToggle, buy;
    private EditBox search;
    private String sent = "";
    private int typedAt = -1, ticks;

    public TradeTerminalScreen(TradeTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TradeTerminalMenu.WIDTH, TradeTerminalMenu.HEIGHT, TradeTerminalMenu.INVENTORY_Y);
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos, y = this.topPos;
        this.tabMine = addRenderableWidget(new ShopScreens.BarButton(x + 8, y + 5, 79, 12, Component.translatable("simpleshops.terminal.tab.mine"),
                () -> send(TradeTerminalMenu.B_TAB_MINE)));
        this.tabBrowse = addRenderableWidget(new ShopScreens.BarButton(x + 89, y + 5, 79, 12, Component.translatable("simpleshops.terminal.tab.browse"),
                () -> send(TradeTerminalMenu.B_TAB_BROWSE)));
        this.search = new EditBox(this.font, x + 8, y + 20, 90, 12, Component.translatable("simpleshops.terminal.search")) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                Minecraft.getInstance().onTextInputFocusChange(this, focused);
            }
        };
        this.search.setMaxLength(32);
        this.search.setHint(Component.translatable("simpleshops.terminal.search"));
        this.search.setResponder(text -> this.typedAt = this.ticks);
        addRenderableWidget(this.search);
        this.stock = addRenderableWidget(new ShopScreens.BarButton(x + 100, y + 20, 34, 12, Component.empty(), () -> send(TradeTerminalMenu.B_STOCK)));
        this.sort = addRenderableWidget(new ShopScreens.BarButton(x + 136, y + 20, 32, 12, Component.empty(), () -> send(TradeTerminalMenu.B_SORT)));
        this.prev = addRenderableWidget(new ShopScreens.BarButton(x + 8, y + 74, 14, 12, Component.literal("<"), () -> send(TradeTerminalMenu.B_PREV)));
        this.next = addRenderableWidget(new ShopScreens.BarButton(x + 154, y + 74, 14, 12, Component.literal(">"), () -> send(TradeTerminalMenu.B_NEXT)));
        this.collect = addRenderableWidget(new ShopScreens.BarButton(x + 8, y + TradeTerminalMenu.BUTTON_Y, 80, 14, Component.translatable("simpleshops.terminal.collect"),
                () -> send(TradeTerminalMenu.B_COLLECT)));
        this.collect.setTooltip(Tooltip.create(Component.translatable("simpleshops.terminal.collect.tip")));
        this.publicToggle = addRenderableWidget(new ShopScreens.BarButton(x + 90, y + TradeTerminalMenu.BUTTON_Y, 78, 14, Component.empty(),
                () -> send(TradeTerminalMenu.B_PUBLIC)));
        this.buy = addRenderableWidget(new ShopScreens.BarButton(x + 48, y + TradeTerminalMenu.BUTTON_Y, 80, 14, Component.translatable("simpleshops.screen.trade"),
                () -> send((Minecraft.getInstance().hasShiftDown() ? TradeTerminalMenu.BUY_MAX : TradeTerminalMenu.BUY) | (this.menu.revisionSeen() & 0xFFFF))));
        this.buy.setTooltip(Tooltip.create(Component.translatable("simpleshops.screen.repeat")));
        update();
    }

    /** The box palette's label colour, at least 4.5:1 on the fill in every brightness mode (ShopLayoutTest checks the light one). */
    private static int label() {
        return UiPalette.BARREL.label();
    }

    private void send(int id) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    /** The search text goes to the server once typing paused (character by character, then "apply"; the server limits the rate). */
    private void sendQuery() {
        String text = this.search.getValue();
        if (text.equals(this.sent)) return;
        this.sent = text;
        send(TradeTerminalMenu.B_CLEAR);
        text.codePoints().limit(32).forEach(cp -> send(TradeTerminalMenu.CHAR + cp));
        send(TradeTerminalMenu.B_APPLY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
        if (this.typedAt >= 0 && this.ticks - this.typedAt >= 8) {
            this.typedAt = -1;
            sendQuery();
        }
        update();
    }

    private void update() {
        if (this.tabMine == null) return;
        boolean mine = this.menu.tab() == TradeTerminalMenu.TAB_MINE;
        int flags = this.menu.shopFlags();
        boolean selected = (flags & TradeTerminalMenu.F_SELECTED) != 0;
        this.tabMine.active = !mine;
        this.tabBrowse.active = mine;
        this.search.visible = !mine;
        this.stock.visible = !mine;
        this.sort.visible = !mine;
        this.stock.setMessage(Component.translatable(this.menu.inStockOnly() ? "simpleshops.terminal.stock_on" : "simpleshops.terminal.stock_off"));
        this.stock.setTooltip(Tooltip.create(Component.translatable("simpleshops.terminal.stock.tip")));
        this.sort.setMessage(Component.translatable("simpleshops.terminal.sort." + this.menu.sort()));
        this.sort.setTooltip(Tooltip.create(Component.translatable("simpleshops.terminal.sort.tip")));
        this.prev.active = this.menu.page() > 0;
        this.next.active = this.menu.page() + 1 < this.menu.pages();
        this.collect.visible = mine;
        this.publicToggle.visible = mine;
        this.collect.active = selected;
        this.publicToggle.active = selected;
        this.publicToggle.setMessage(Component.translatable((flags & TradeTerminalMenu.F_PUBLIC) != 0 ? "simpleshops.terminal.public" : "simpleshops.terminal.private"));
        this.publicToggle.setTooltip(Tooltip.create(Component.translatable("simpleshops.terminal.public.tip")));
        this.buy.visible = !mine;
        this.buy.active = this.menu.canBuyClient();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.search != null && this.search.isFocused() && this.search.visible) {
            if (event.isEscape()) return super.keyPressed(event);
            if (event.isConfirmation()) {
                this.typedAt = -1;
                sendQuery();
                return true;
            }
            return this.search.keyPressed(event) || this.search.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int sel = this.menu.selectedRow();
        if (sel >= 0 && sel < TradeTerminalMenu.LIST) {
            int x = this.leftPos + TradeTerminalMenu.LIST_X + (sel % 9) * 18 - 1, y = this.topPos + TradeTerminalMenu.LIST_Y + (sel / 9) * 18 - 1;
            graphics.fill(x, y, x + 18, y + 1, 0xFFE8B030);
            graphics.fill(x, y + 17, x + 18, y + 18, 0xFFE8B030);
            graphics.fill(x, y, x + 1, y + 18, 0xFFE8B030);
            graphics.fill(x + 17, y, x + 18, y + 18, 0xFFE8B030);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        boolean mine = this.menu.tab() == TradeTerminalMenu.TAB_MINE;
        boolean selected = (this.menu.shopFlags() & TradeTerminalMenu.F_SELECTED) != 0;
        if (mine) graphics.text(this.font, Component.translatable("simpleshops.terminal.mine_hint"), 8, 23, label(), false);
        String pageText = (this.menu.page() + 1) + "/" + this.menu.pages();
        graphics.text(this.font, pageText, 26, 76, label(), false);
        int status = this.menu.status();
        Component line = this.menu.pendingDeliveries() > 0
                ? Component.translatable("simpleshops.terminal.status.delivering", this.menu.pendingDeliveries(), this.menu.deliveryProgress())
                : status > 0 && status < TradeTerminalMenu.STATUS.length ? Component.translatable("simpleshops.terminal.status." + TradeTerminalMenu.STATUS[status])
                : Component.empty();
        int w = this.font.width(line);
        graphics.text(this.font, line, Math.max(50, 150 - w), 76, label(), false);
        if (selected) {
            graphics.text(this.font, Component.translatable(mine ? "simpleshops.screen.goods" : "simpleshops.screen.price"), 8, TradeTerminalMenu.ROW_A_Y + 5, label(), false);
            graphics.text(this.font, Component.translatable(mine ? "simpleshops.screen.price" : "simpleshops.screen.pay"), 8, TradeTerminalMenu.ROW_B_Y + 5, label(), false);
        } else {
            graphics.text(this.font, Component.translatable(mine ? "simpleshops.terminal.pick_mine" : "simpleshops.terminal.pick_offer"), 8, TradeTerminalMenu.ROW_A_Y + 5, label(), false);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
        // Lists and rows: only a plain click means something (an action on the server); no other click type is sent.
        if (slot != null && slotId >= 0 && slotId < TradeTerminalMenu.PAY && input != ContainerInput.PICKUP) return;
        super.slotClicked(slot, slotId, buttonNum, input);
    }
}
