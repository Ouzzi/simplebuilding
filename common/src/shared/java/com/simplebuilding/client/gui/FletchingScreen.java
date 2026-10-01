package com.simplebuilding.client.gui;

import com.simplebuilding.fletching.ArrowParts;
import com.simplebuilding.fletching.FletchingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bildschirm des Befiederungstischs: Teile diagonal wie ein Pfeil, rechts das Ergebnis, links ein Material-Panel im
 * Stil des Vanilla-Rezeptbuchs (Spitze, Schaft, Befiederung). Ein Klick auf ein Material legt es aus dem Inventar in
 * seinen Slot; fehlende Materialien sind abgedunkelt, der Tooltip nennt Zweck und Wirkung.
 */
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final int PANEL_W = 104;
    private static final int CELL = 18;
    private static final int PER_ROW = 5;
    private static final int LABEL = 0xFF404040;
    private static final int ARROW = 0xFF8B8B8B;
    private static final int MISSING = 0xA0373737;
    private static final int HOVER = 0x80FFFFFF;
    private static final int TOGGLE_X = 152;
    private static final int TOGGLE_Y = 58;
    /** Wie das Vanilla-Rezeptbuch: der offene Zustand gilt fuer alle Tische dieser Sitzung. */
    private static boolean panelOpen = true;

    private final List<Entry> entries = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();

    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private record Entry(Item item, int button, String effectKey, int x, int y) {
    }

    private record Header(Component text, int x, int y) {
    }

    @Override
    protected void init() {
        super.init();
        if (panelOpen && this.width >= this.imageWidth + 2 * (PANEL_W + 2)) {
            this.leftPos = (this.width - this.imageWidth + PANEL_W + 2) / 2;
        }
        layoutPanel();
    }

    private boolean panelShown() {
        return panelOpen && this.leftPos - PANEL_W - 2 >= 0;
    }

    private int panelLeft() {
        return this.leftPos - PANEL_W - 2;
    }

    private void layoutPanel() {
        this.entries.clear();
        this.headers.clear();
        int x = panelLeft() + 7;
        int y = this.topPos + 7;
        y = section(Component.translatable("container.simplebuilding.fletching.tips"), x, y,
                partsOf(ArrowParts.Tip.values()), FletchingMenu.TIP_BUTTON, "tip");
        y = section(Component.translatable("container.simplebuilding.fletching.shafts"), x, y,
                partsOf(ArrowParts.Shaft.values()), FletchingMenu.SHAFT_BUTTON, "shaft");
        section(Component.translatable("container.simplebuilding.fletching.fletchings"), x, y,
                partsOf(ArrowParts.Fletching.values()), FletchingMenu.FLETCHING_BUTTON, "fletching");
    }

    private static List<Object[]> partsOf(Enum<?>[] values) {
        List<Object[]> out = new ArrayList<>();
        for (Enum<?> value : values) {
            Item item = value instanceof ArrowParts.Tip t ? t.input()
                    : value instanceof ArrowParts.Shaft s ? s.input()
                    : ((ArrowParts.Fletching) value).input();
            String id = ((net.minecraft.util.StringRepresentable) value).getSerializedName();
            out.add(new Object[] {item, id});
        }
        return out;
    }

    private int section(Component title, int x, int y, List<Object[]> parts, int firstButton, String kind) {
        this.headers.add(new Header(title, x, y));
        y += 10;
        for (int i = 0; i < parts.size(); i++) {
            int cx = x + (i % PER_ROW) * CELL;
            int cy = y + (i / PER_ROW) * CELL;
            this.entries.add(new Entry((Item) parts.get(i)[0], firstButton + i,
                    "container.simplebuilding.fletching.effect." + kind + "." + parts.get(i)[1], cx, cy));
        }
        return y + ((parts.size() + PER_ROW - 1) / PER_ROW) * CELL + 4;
    }

    private boolean playerHas(Item item) {
        Inventory inventory = this.minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) return true;
        }
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        TieredChestScreen.panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            TieredChestScreen.slot(graphics, this.leftPos + slot.x, this.topPos + slot.y);
        }
        // Pfeil von den Teilen zum Ergebnis.
        int ax = this.leftPos + 90;
        int ay = this.topPos + 42;
        graphics.fill(ax, ay - 1, ax + 16, ay + 2, ARROW);
        for (int i = 0; i < 6; i++) {
            graphics.fill(ax + 16 + i, ay - 5 + i, ax + 17 + i, ay + 6 - i, ARROW);
        }
        TieredChestScreen.slot(graphics, this.leftPos + TOGGLE_X, this.topPos + TOGGLE_Y);
        graphics.item(new ItemStack(Items.KNOWLEDGE_BOOK), this.leftPos + TOGGLE_X, this.topPos + TOGGLE_Y);
        if (panelShown()) {
            TieredChestScreen.panel(graphics, panelLeft(), this.topPos, PANEL_W, this.imageHeight);
            for (Header header : this.headers) {
                graphics.text(this.font, header.text(), header.x(), header.y(), LABEL, false);
            }
            for (Entry entry : this.entries) {
                TieredChestScreen.slot(graphics, entry.x(), entry.y());
                graphics.item(new ItemStack(entry.item()), entry.x(), entry.y());
                if (!playerHas(entry.item())) {
                    graphics.fill(entry.x(), entry.y(), entry.x() + 16, entry.y() + 16, MISSING);
                } else if (inside(entry, mouseX, mouseY)) {
                    graphics.fill(entry.x(), entry.y(), entry.x() + 16, entry.y() + 16, HOVER);
                }
            }
        }
    }

    private static boolean inside(Entry entry, double x, double y) {
        return x >= entry.x() - 1 && y >= entry.y() - 1 && x < entry.x() + 17 && y < entry.y() + 17;
    }

    private boolean onToggle(double x, double y) {
        return x >= this.leftPos + TOGGLE_X - 1 && y >= this.topPos + TOGGLE_Y - 1
                && x < this.leftPos + TOGGLE_X + 17 && y < this.topPos + TOGGLE_Y + 17;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (onToggle(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("container.simplebuilding.fletching.toggle"), mouseX, mouseY);
            return;
        }
        if (!panelShown()) {
            return;
        }
        for (Entry entry : this.entries) {
            if (inside(entry, mouseX, mouseY)) {
                List<Component> lines = new ArrayList<>();
                lines.add(entry.item().getName(new ItemStack(entry.item())));
                lines.add(Component.translatable(entry.effectKey()).withStyle(ChatFormatting.GRAY));
                if (!playerHas(entry.item())) {
                    lines.add(Component.translatable("container.simplebuilding.fletching.missing").withStyle(ChatFormatting.RED));
                }
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (onToggle(event.x(), event.y())) {
            panelOpen = !panelOpen;
            this.init(this.width, this.height);
            return true;
        }
        if (panelShown()) {
            for (Entry entry : this.entries) {
                if (inside(entry, event.x(), event.y())) {
                    if (playerHas(entry.item())) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, entry.button());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean inPanel = panelShown() && mx >= panelLeft() && mx < this.leftPos && my >= this.topPos && my < this.topPos + this.imageHeight;
        return !inPanel && super.hasClickedOutside(mx, my, xo, yo);
    }
}
