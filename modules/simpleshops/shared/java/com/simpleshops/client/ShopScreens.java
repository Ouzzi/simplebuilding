package com.simpleshops.client;

import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
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

/** The three shop screens in SimpleLib's box style: slots are drawn under the menu's slot positions. */
public final class ShopScreens {
    private static final UiPalette PALETTE = UiPalette.INVENTORY;

    /** Base: draws the box, every active slot and the player inventory block; subclasses add labels. */
    abstract static class Base<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
        Base(M menu, Inventory inventory, Component title, int w, int h) {
            super(menu, inventory, title, w, h);
            this.titleLabelX = 8;
            this.titleLabelY = 6;
            this.inventoryLabelY = h - 94;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            UiBoxes.box(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, PALETTE);
            for (Slot slot : this.menu.slots) {
                if (slot.isActive()) UiBoxes.slot(graphics, this.leftPos + slot.x, this.topPos + slot.y, PALETTE);
            }
        }

        protected void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
            graphics.text(this.font, Component.translatable(key), x, y, UiPalette.DARK_LABEL, false);
        }
    }

    /** Owner menu of the sales block: price row and earnings. */
    public static final class Owner extends Base<SalesOwnerMenu> {
        public Owner(SalesOwnerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, SalesOwnerMenu.WIDTH, SalesOwnerMenu.HEIGHT);
        }

        @Override
        protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
            super.extractLabels(graphics, xm, ym);
            label(graphics, "simpleshops.screen.price", 8, SalesOwnerMenu.PRICE_Y + 5);
            label(graphics, "simpleshops.screen.earnings", 8, SalesOwnerMenu.EARNINGS_Y - 10);
        }

        @Override
        protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            super.extractTooltip(graphics, mouseX, mouseY);
            Slot hovered = this.hoveredSlot;
            if (hovered != null && hovered.index < SalesOwnerMenu.EARNINGS_START && this.menu.getCarried().isEmpty() && !hovered.hasItem()) {
                graphics.setTooltipForNextFrame(Component.translatable("simpleshops.screen.template"), mouseX, mouseY);
            }
        }
    }

    /** Owner menu of the display case. */
    public static final class Stock extends Base<StockMenu> {
        public Stock(StockMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, StockMenu.WIDTH, StockMenu.HEIGHT);
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

        public Buyer(ShopBuyerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, ShopBuyerMenu.WIDTH, ShopBuyerMenu.HEIGHT);
        }

        @Override
        protected void init() {
            super.init();
            this.trade = this.addRenderableWidget(new TradeButton(this.leftPos + 48, this.topPos + 84, 80, 18));
        }

        @Override
        protected void containerTick() {
            super.containerTick();
            if (this.trade != null) {
                this.trade.active = this.menu.canTradeClient();
                this.trade.setTooltip(Tooltip.create(Component.translatable(this.trade.active ? "simpleshops.screen.repeat"
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
