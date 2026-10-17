package com.simplebuilding.modules.simpletweaks.admin.invsee.client;

import com.simplebuilding.modules.simpletweaks.admin.invsee.InvseeMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The admin inventory screen in SimpleLib's box style (principle 6a): steel box ({@link UiPalette#IRON}) for the target,
 * the light inventory panel for the viewer below the seam. Text is {@link UiPalette#DARK_LABEL} on the steel fill and on
 * the light panel (rule 15, pairs in {@link InvseeColors#PAIRS}). Death backups show an info line and three buttons.
 */
public final class InvseeScreen extends AbstractContainerScreen<InvseeMenu> {
    private static final UiPalette BOX = InvseeColors.BOX, INVENTORY = InvseeColors.INVENTORY;
    private static final int SEAM = 3;
    private String info = "", cause;
    private int index = 1, count = 1;

    public InvseeScreen(InvseeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, heading(menu, title), InvseeMenu.WIDTH, InvseeMenu.height(menu.kind));
        this.titleLabelX = 8; this.titleLabelY = 6; this.inventoryLabelY = -1000;
        if (menu.kind == InvseeMenu.Kind.DEATHS && title.getContents() instanceof TranslatableContents t && t.getArgs().length >= 7) {
            var a = t.getArgs();
            index = number(a[1], 1); count = number(a[2], 1);
            info = Component.translatable("simpletweaks.admin.deaths.info", index, count, text(a[3]), text(a[5])).getString();
            cause = text(a[4]) + "\n" + text(a[6]);
        }
    }
    private static Component heading(InvseeMenu menu, Component title) {
        if (menu.kind == InvseeMenu.Kind.DEATHS && title.getContents() instanceof TranslatableContents t && t.getArgs().length >= 1)
            return Component.translatable("simpletweaks.admin.deaths.heading", text(t.getArgs()[0]));
        return title;
    }
    private static String text(Object o) { return o instanceof Component c ? c.getString() : String.valueOf(o); }
    private static int number(Object o, int fallback) {
        try { return Integer.parseInt(text(o)); } catch (NumberFormatException e) { return fallback; }
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind != InvseeMenu.Kind.DEATHS) return;
        // own row inside the panel (no overlap with the info line or the slots): < > on the left, Restore on the right,
        // sized by the translated text, so long translations (DE "Wiederherstellen") still fit
        int y = topPos + InvseeMenu.BUTTONS_Y, h = InvseeMenu.BUTTON_H;
        var prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(InvseeMenu.PREV)).bounds(leftPos + 8, y, 20, h).build());
        var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(InvseeMenu.NEXT)).bounds(leftPos + 30, y, 20, h).build());
        prev.active = index > 1; next.active = index < count;
        // index 1 is the newest backup
        prev.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simpletweaks.admin.deaths.newer")));
        next.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simpletweaks.admin.deaths.older")));
        Component label = Component.translatable("simpletweaks.admin.deaths.restore");
        int max = imageWidth - 8 - 56, w = Math.min(max, Math.max(60, font.width(label) + 12));
        var restore = addRenderableWidget(Button.builder(label, b -> click(InvseeMenu.RESTORE)).bounds(leftPos + imageWidth - 8 - w, y, w, h).build());
        restore.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simpletweaks.admin.deaths.restore.tip")));
    }
    private void click(int id) {
        var mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        UiBoxes.box(g, leftPos, topPos, imageWidth, imageHeight, BOX);
        int seam = InvseeMenu.ownY(menu.kind) - SEAM;
        UiBoxes.seam(g, leftPos + UiBoxes.FRAME, topPos + seam, imageWidth - 2 * UiBoxes.FRAME, imageHeight - UiBoxes.FRAME_BOTTOM - seam, BOX);
        for (Slot slot : menu.slots) if (slot.isActive()) UiBoxes.slot(g, leftPos + slot.x, topPos + slot.y, slot.y >= seam ? INVENTORY : BOX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        if (menu.kind == InvseeMenu.Kind.DEATHS && !info.isEmpty())
            g.text(font, font.plainSubstrByWidth(info, imageWidth - 16), 8, InvseeMenu.INFO_Y, InvseeColors.LABEL, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (cause != null && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8 && mouseY >= topPos + InvseeMenu.INFO_Y - 1 && mouseY < topPos + InvseeMenu.INFO_Y + 9)
            g.setTooltipForNextFrame(font.split(Component.literal(cause), 200), mouseX, mouseY);
    }
}
