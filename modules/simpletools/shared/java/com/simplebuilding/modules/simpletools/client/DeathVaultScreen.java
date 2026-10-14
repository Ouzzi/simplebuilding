package com.simplebuilding.modules.simpletools.client;

import com.simplebuilding.modules.simpletools.vault.DeathLedger;
import com.simplebuilding.modules.simpletools.vault.DeathVault;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import com.simplebuilding.modules.simpletools.vault.Fate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Death Vault screen: the Vanilla large chest on the left with one field per drop of the last death (frame and tint in the
 * colour of its {@link Fate}; a drop still lying gets a despawn bar like a durability bar, and a pause mark while its chunk
 * is not loaded and the timer stands), a legend with counts on the right, the death age in the band below the grid (the
 * future offering slot and button take that place when the redeem option is on), the inventory below.
 */
public class DeathVaultScreen extends AbstractContainerScreen<DeathVaultMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int FILL = 0xFFC6C6C6, TEXT = 0xFF404040, DIM_TEXT = 0xFF8B8B8B, PAUSE = 0xFF6E86B0, WARN = 0xFFB85A10;
    private static final int PANEL_X = DeathVaultMenu.WIDTH + 4, PANEL_W = 130;
    private Button offer;

    public DeathVaultScreen(DeathVaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_X + PANEL_W, DeathVaultMenu.HEIGHT);
        this.inventoryLabelY = DeathVaultMenu.HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        offer = addRenderableWidget(Button.builder(Component.translatable("screen.simpletools.death_vault.offer"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DeathVaultMenu.BUTTON_OFFER);
        }).bounds(leftPos + DeathVaultMenu.BUTTON_X, topPos + DeathVaultMenu.OFFER_Y - 1, DeathVaultMenu.BUTTON_W, 18).build());
    }

    // --- data of one field -------------------------------------------------------------------------------------------

    private int flags(int index) {
        return menu.data(DeathVaultMenu.D_ROWS + 2 * index);
    }

    private int age(int index) {
        return menu.data(DeathVaultMenu.D_ROWS + 2 * index + 1);
    }

    private boolean shownField(int index) {
        return index >= 0 && index < menu.data(DeathVaultMenu.D_COUNT);
    }

    private Fate fate(int index) {
        return Fate.of(flags(index) & DeathVault.F_FATE_MASK);
    }

    private boolean lying(int index) {
        return shownField(index) && fate(index) == Fate.LYING;
    }

    private boolean running(int index) {
        return (flags(index) & DeathVault.F_RUNNING) != 0;
    }

    private boolean never(int index) {
        return (flags(index) & DeathVault.F_NEVER) != 0;
    }

    private static String clock(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    // --- background ----------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int top = DeathVaultMenu.ROWS * 18 + 17;
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, DeathVaultMenu.WIDTH, top, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + top, 0.0F, 126.0F, DeathVaultMenu.WIDTH, 96, 256, 256);
        // Band below the grid: only the offering slot is real (and only with the redeem option); cover the other frames.
        int rowY = topPos + DeathVaultMenu.BAND_Y - 1;
        if (menu.offerEnabled()) {
            g.fill(leftPos + 7, rowY, leftPos + DeathVaultMenu.OFFER_X - 1, rowY + 18, FILL);
            g.fill(leftPos + DeathVaultMenu.OFFER_X + 17, rowY, leftPos + 169, rowY + 18, FILL);
        } else {
            g.fill(leftPos + 7, rowY, leftPos + 169, rowY + 18, FILL);
        }
        panel(g);
    }

    /** Right panel in the chest colours: black rim, light top/left, dark bottom/right edge. */
    private void panel(GuiGraphicsExtractor g) {
        int x0 = leftPos + PANEL_X, y0 = topPos, x1 = x0 + PANEL_W, y1 = y0 + DeathVaultMenu.HEIGHT - 1;
        g.fill(x0, y0, x1, y1, 0xFF000000);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFFFFFFF);
        g.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
        g.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, FILL);
    }

    // --- slots ---------------------------------------------------------------------------------------------------------

    @Override
    public void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        int i = slot.index;
        if (i < DeathVaultMenu.SHOWN && shownField(i) && slot.hasItem()) {
            int color = fate(i).color;
            g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, ARGB.color(110, color));
            g.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y, color);
            g.fill(slot.x - 1, slot.y + 16, slot.x + 17, slot.y + 17, color);
            g.fill(slot.x - 1, slot.y, slot.x, slot.y + 16, color);
            g.fill(slot.x + 16, slot.y, slot.x + 17, slot.y + 16, color);
        }
        super.extractSlot(g, slot, mouseX, mouseY);
        if (i < DeathVaultMenu.SHOWN && lying(i) && slot.hasItem()) {
            timerBar(g, slot.x, slot.y, i);
            if (!running(i) && !never(i)) pauseMark(g, slot.x + 9, slot.y);
        }
    }

    /** Despawn bar over the lower edge of the item (like a durability bar): full = just dropped, empty = about to vanish. */
    private void timerBar(GuiGraphicsExtractor g, int x, int y, int index) {
        float left = never(index) ? 1.0F : Mth.clamp(1.0F - age(index) / (float) DeathLedger.LIFETIME, 0.0F, 1.0F);
        int color = never(index) ? 0xFFB8B8FF : running(index) ? ARGB.opaque(Mth.hsvToRgb(left / 3.0F, 1.0F, 1.0F)) : PAUSE;
        g.fill(x + 2, y + 13, x + 15, y + 16, 0xFF000000);
        g.fill(x + 2, y + 14, x + 2 + Math.round(13 * left), y + 16, color);
    }

    /** Pause symbol (two bars) in the upper right corner of a field: the chunk is not loaded, the timer stands. */
    private void pauseMark(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 7, y + 7, 0xFF000000);
        g.fill(x + 1, y + 1, x + 3, y + 6, 0xFFFFFFFF);
        g.fill(x + 4, y + 1, x + 6, y + 6, 0xFFFFFFFF);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < DeathVaultMenu.SHOWN && lying(slot.index)) {
            int i = slot.index;
            if (never(i)) lines.add(Component.translatable("screen.simpletools.death_vault.timer.never").withColor(0xB8B8FF));
            else if (running(i)) lines.add(Component.translatable("screen.simpletools.death_vault.timer", clock(DeathLedger.LIFETIME - age(i))).withColor(0x55D05A));
            else lines.add(Component.translatable("screen.simpletools.death_vault.paused.tip", clock(DeathLedger.LIFETIME - age(i))).withColor(0xE08A30));
        }
        return lines;
    }

    // --- text and widgets ----------------------------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (offer != null) {
            offer.visible = menu.offerEnabled();
            int cost = Math.max(1, menu.data(DeathVaultMenu.D_COST));
            offer.active = menu.data(DeathVaultMenu.D_LOST) > 0 && menu.offering().getCount() >= cost;
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        if (offer != null && offer.visible && offer.isHovered()) {
            g.setTooltipForNextFrame(font, Component.translatable("screen.simpletools.death_vault.offer.tooltip",
                    menu.data(DeathVaultMenu.D_COST), menu.data(DeathVaultMenu.D_MAX)), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        if (menu.offerEnabled()) g.text(font, Component.translatable("screen.simpletools.death_vault.offering"), 8, DeathVaultMenu.OFFER_Y + 5, TEXT, false);
        else band(g);
        legend(g);
    }

    /** Age of the last death (and drops that did not fit) in the band below the grid. */
    private void band(GuiGraphicsExtractor g) {
        int since = menu.data(DeathVaultMenu.D_SINCE), more = menu.data(DeathVaultMenu.D_MORE);
        Component text;
        if (since < 0) text = Component.translatable("screen.simpletools.death_vault.none");
        else if (menu.data(DeathVaultMenu.D_COUNT) == 0) text = Component.translatable("screen.simpletools.death_vault.empty");
        else if (since == 0) text = Component.translatable("screen.simpletools.death_vault.last.now");
        else text = Component.translatable("screen.simpletools.death_vault.last", since);
        if (more > 0) text = Component.empty().append(text).append(Component.translatable("screen.simpletools.death_vault.more", more));
        List<FormattedCharSequence> lines = font.split(text, 160);
        int y = DeathVaultMenu.BAND_Y + (lines.size() > 1 ? 1 : 5);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.text(font, lines.get(i), 8, y + i * 9, TEXT, false);
    }

    /** Legend: colour key, fate and how many of the shown drops have it; below it the state of the despawn timers. */
    private void legend(GuiGraphicsExtractor g) {
        int x = PANEL_X + 8, width = PANEL_W - 16;
        g.text(font, Component.translatable("screen.simpletools.death_vault.legend"), x, 6, TEXT, false);
        int[] count = new int[16];
        int lyingRunning = 0, lyingStalled = 0, next = Integer.MAX_VALUE;
        for (int i = 0; i < menu.data(DeathVaultMenu.D_COUNT); i++) {
            count[fate(i).id]++;
            if (lying(i) && !never(i)) {
                if (running(i)) {
                    lyingRunning++;
                    next = Math.min(next, DeathLedger.LIFETIME - age(i));
                } else lyingStalled++;
            }
        }
        int y = 18;
        for (Fate fate : Fate.DISPLAY_ORDER) {
            if (fate == Fate.REDEEMED && !menu.offerEnabled() && count[fate.id] == 0) continue;
            int n = count[fate.id];
            g.fill(x, y + 1, x + 8, y + 9, 0xFF000000);
            g.fill(x + 1, y + 2, x + 7, y + 8, fate.color);
            List<FormattedCharSequence> lines = font.split(fate.label().copy().append(" (" + n + ")"), width - 12);
            for (FormattedCharSequence line : lines) {
                g.text(font, line, x + 12, y, n == 0 ? DIM_TEXT : TEXT, false);
                y += 9;
            }
            y += 2;
        }
        int sy = DeathVaultMenu.HEIGHT - 8;
        List<FormattedCharSequence> status = new ArrayList<>();
        int color = TEXT;
        if (lyingStalled > 0) {
            status = font.split(Component.translatable("screen.simpletools.death_vault.paused"), width - 12);
            color = WARN;
        } else if (lyingRunning > 0) {
            status = font.split(Component.translatable("screen.simpletools.death_vault.next", clock(next)), width);
        }
        if (!status.isEmpty()) {
            int h = status.size() * 9;
            int top = sy - h;
            if (lyingStalled > 0) pauseMark(g, x, top);
            for (int i = 0; i < status.size(); i++) g.text(font, status.get(i), x + (lyingStalled > 0 ? 12 : 0), top + i * 9, color, false);
        }
    }
}
