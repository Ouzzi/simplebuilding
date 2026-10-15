package com.simpleshops.client;

import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * The shop screens in SimpleLib's box style: one wooden-bronze box with the light inventory panel (seam) below it, like the other
 * mod screens. Slots are drawn under the menu's slot positions; block slots in the box colour, inventory slots light.
 */
public final class ShopScreens {
    private static final UiPalette PALETTE = UiPalette.BARREL;
    private static final UiPalette INVENTORY = UiPalette.INVENTORY;
    /** The light panel starts this many pixels above the first inventory row (same as the other screens). */
    private static final int SEAM = 3;

    /** Base: draws the box, the inventory panel and every active slot; subclasses add labels. */
    abstract static class Base<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
        private final int inventoryY;

        Base(M menu, Inventory inventory, Component title, int w, int h, int inventoryY) {
            super(menu, inventory, title, w, h);
            this.inventoryY = inventoryY;
            this.titleLabelX = 8;
            this.titleLabelY = 6;
            this.inventoryLabelY = -1000;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            UiBoxes.box(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, PALETTE);
            int seam = this.inventoryY - SEAM;
            UiBoxes.seam(graphics, this.leftPos + UiBoxes.FRAME, this.topPos + seam, this.imageWidth - 2 * UiBoxes.FRAME,
                    this.imageHeight - UiBoxes.FRAME_BOTTOM - seam, PALETTE);
            for (Slot slot : this.menu.slots) {
                if (slot.isActive()) UiBoxes.slot(graphics, this.leftPos + slot.x, this.topPos + slot.y, slot.y >= this.inventoryY ? INVENTORY : PALETTE);
            }
        }

        protected void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
            graphics.text(this.font, Component.translatable(key), x, y, UiPalette.DARK_LABEL, false);
        }
    }

    /** A plain bar button in the style of the box (the Vanilla sprite). */
    static final class BarButton extends AbstractButton {
        private final Runnable action;

        BarButton(int x, int y, int w, int h, Component text, Runnable action) {
            super(x, y, w, h, text);
            this.action = action;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            extractDefaultSprite(graphics);
            extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    /** Owner screen of the sales block (alone: price + earnings) and of the whole shop (goods, price, earnings in double-chest height). */
    public static final class Owner extends Base<SalesOwnerMenu> {
        public Owner(SalesOwnerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, SalesOwnerMenu.WIDTH, menu.isCombined() ? SalesOwnerMenu.C_HEIGHT : SalesOwnerMenu.HEIGHT,
                    menu.isCombined() ? SalesOwnerMenu.C_INVENTORY_Y : SalesOwnerMenu.INVENTORY_Y);
        }

        @Override
        protected void init() {
            super.init();
            if (this.menu.isCombined()) {
                // The free strip between the earnings and the inventory panel.
                Component text = Component.translatable("simpleshops.screen.dev_buyer");
                BarButton b = this.addRenderableWidget(new BarButton(this.leftPos + 8, this.topPos + SalesOwnerMenu.C_EARNINGS_Y + 54 + 4, 160, 14, text,
                        () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, SalesOwnerMenu.DEV_BUYER)));
                b.setTooltip(Tooltip.create(Component.translatable("simpleshops.screen.dev_buyer.tip")));
            }
        }

        @Override
        protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
            super.extractLabels(graphics, xm, ym);
            if (this.menu.isCombined()) {
                label(graphics, "simpleshops.screen.goods", 8, SalesOwnerMenu.C_GOODS_Y + 5);
                label(graphics, "simpleshops.screen.price", 8, SalesOwnerMenu.C_PRICE_Y + 5);
            } else {
                label(graphics, "simpleshops.screen.price", 8, SalesOwnerMenu.PRICE_Y + 5);
            }
        }

        @Override
        protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            super.extractTooltip(graphics, mouseX, mouseY);
            Slot hovered = this.hoveredSlot;
            int start = this.menu.priceStart();
            if (hovered != null && hovered.index >= start && hovered.index < start + 5 && this.menu.getCarried().isEmpty() && !hovered.hasItem()) {
                graphics.setTooltipForNextFrame(Component.translatable("simpleshops.screen.template"), mouseX, mouseY);
            }
        }
    }

    /** Owner screen of a sales storage. */
    public static final class Storage extends Base<StorageMenu> {
        public Storage(StorageMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, StorageMenu.WIDTH, StorageMenu.HEIGHT, StorageMenu.INVENTORY_Y);
        }
    }

    /** Owner menu of the display case. */
    public static final class Stock extends Base<StockMenu> {
        public Stock(StockMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, StockMenu.WIDTH, StockMenu.HEIGHT, StockMenu.INVENTORY_Y);
        }

        @Override
        protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
            super.extractLabels(graphics, xm, ym);
            label(graphics, "simpleshops.screen.goods", 8, StockMenu.ROW_Y + 5);
        }
    }

    /** Buyer menu: pick goods, pay, trade. */
    public static final class Buyer extends Base<ShopBuyerMenu> {
        private TradeButton trade;
        private BarButton back;

        public Buyer(ShopBuyerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, ShopBuyerMenu.WIDTH, ShopBuyerMenu.HEIGHT, ShopBuyerMenu.INVENTORY_Y);
        }

        @Override
        protected void init() {
            super.init();
            this.trade = this.addRenderableWidget(new TradeButton(this.leftPos + 48, this.topPos + 84, 80, 18));
            // Shown once the synced "owner" flag arrives (it comes after the screen opens).
            this.back = this.addRenderableWidget(new BarButton(this.leftPos + 82, this.topPos + 3, 86, 12,
                    Component.translatable("simpleshops.screen.dev_owner"), () -> send(ShopBuyerMenu.TO_OWNER)));
            this.back.setTooltip(Tooltip.create(Component.translatable("simpleshops.screen.dev_owner.tip")));
            this.back.visible = false;
        }

        @Override
        protected void containerTick() {
            super.containerTick();
            if (this.back != null) this.back.visible = this.menu.canManage();
            if (this.trade != null) {
                this.trade.setMessage(Component.translatable(this.menu.earningsFull() ? "simpleshops.screen.earnings_full_short" : "simpleshops.screen.trade"));
                this.trade.active = this.menu.canTradeClient();
                this.trade.setTooltip(Tooltip.create(Component.translatable(this.trade.active ? "simpleshops.screen.repeat"
                        : this.menu.earningsFull() ? "simpleshops.screen.earnings_full"
                        : hasGoods() ? "simpleshops.screen.pay_exactly" : "simpleshops.screen.sold_out")));
            }
        }

        private boolean hasGoods() {
            for (int i = 0; i < 5; i++) if (!this.menu.goodsView().getItem(i).isEmpty()) return true;
            return false;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            // The chosen goods slot gets a gold frame.
            int sel = this.menu.selectedWare();
            if (sel >= 0 && sel < 5 && !this.menu.goodsView().getItem(sel).isEmpty()) {
                int x = this.leftPos + ShopBuyerMenu.ROW_X + sel * 18 - 1, y = this.topPos + ShopBuyerMenu.GOODS_Y - 1;
                graphics.fill(x, y, x + 18, y + 1, 0xFFE8B030);
                graphics.fill(x, y + 17, x + 18, y + 18, 0xFFE8B030);
                graphics.fill(x, y, x + 1, y + 18, 0xFFE8B030);
                graphics.fill(x + 17, y, x + 18, y + 18, 0xFFE8B030);
            }
        }

        @Override
        protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
            super.extractLabels(graphics, xm, ym);
            label(graphics, "simpleshops.screen.goods", 8, ShopBuyerMenu.GOODS_Y + 5);
            label(graphics, "simpleshops.screen.price", 8, ShopBuyerMenu.PRICE_Y + 5);
            label(graphics, "simpleshops.screen.pay", 8, ShopBuyerMenu.PAY_Y + 5);
        }

        @Override
        protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
            // A click on a goods slot picks the ware (a menu button, no item moves); the price row is display only.
            if (slot != null && slotId >= ShopBuyerMenu.GOODS_START && slotId < ShopBuyerMenu.PRICE_START) {
                send(ShopBuyerMenu.SELECT + slotId);
                return;
            }
            if (slot != null && slotId >= ShopBuyerMenu.PRICE_START && slotId < ShopBuyerMenu.PAY_START) return;
            super.slotClicked(slot, slotId, buttonNum, input);
        }

        private void send(int action) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, ShopBuyerMenu.button(action, this.menu.revisionSeen()));
        }

        private final class TradeButton extends AbstractButton {
            TradeButton(int x, int y, int w, int h) {
                super(x, y, w, h, Component.translatable("simpleshops.screen.trade"));
            }

            @Override
            public void onPress(InputWithModifiers input) {
                send(input.hasShiftDown() ? ShopBuyerMenu.TRADE_MAX : ShopBuyerMenu.TRADE);
            }

            @Override
            protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                extractDefaultSprite(graphics);
                extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {
                this.defaultButtonNarrationText(output);
            }
        }
    }

    private ShopScreens() {}
}
