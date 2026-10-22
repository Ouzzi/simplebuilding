package com.simplebuilding.modules.simpletools.client;

import com.simplebuilding.modules.simpletools.vault.DeathVault;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import com.simplebuilding.modules.simpletools.vault.Fate;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Death Vault screen in SimpleLib's box style (SP22-3): a violet vault box on the left with one field per drop of the last death (frame and tint in the
 * colour of its {@link Fate}; a drop still lying gets a despawn bar like a durability bar, and a pause mark while its chunk
 * is not loaded and the timer stands), a legend with counts on the right, the death age in the band below the grid (the
 * future offering slot and button take that place when the redeem option is on), the light inventory box below; the
 * legend box on the right holds its list in a light field with dark ink.
 */
public class DeathVaultScreen extends AbstractContainerScreen<DeathVaultMenu> {
    /** Box colours from the violet of the vault's block texture (SP22-3); the label comes out light (>= 4.5:1). */
    static final UiPalette PALETTE = UiPalette.derived(0xFF3C2D57);
    private static final int PAUSE = 0xFF6E86B0, WARN = 0xFF8A3A00;
    /** First inventory row (as {@link DeathVaultMenu}), the vault box above it with the mod chests' 2 px gap, the inventory box. */
    private static final int GAP = 2, INVENTORY_Y = DeathVaultMenu.GRID_Y + DeathVaultMenu.ROWS * 18 + 13;
    private static final int VAULT_HEIGHT = INVENTORY_Y - UiBoxes.FRAME - GAP;
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int PANEL_X = DeathVaultMenu.WIDTH + GAP, PANEL_W = 130;
    /** The light list field inside the legend box. */
    private static final int LIST_X = PANEL_X + 7, LIST_Y = 17, LIST_W = PANEL_W - 14, LIST_H = DeathVaultMenu.HEIGHT - LIST_Y - UiBoxes.FRAME_BOTTOM - 3;
    private Button offer;

    public DeathVaultScreen(DeathVaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_X + PANEL_W, DeathVaultMenu.HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
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
        return menu.data(DeathVaultMenu.D_ROWS + 3 * index);
    }

    private int age(int index) {
        return menu.data(DeathVaultMenu.D_ROWS + 3 * index + 1);
    }

    private int lifespan(int index) {
        return menu.data(DeathVaultMenu.D_ROWS + 3 * index + 2);
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
        // Vault box (grid + band) in the vault's violet with faint runes, the light inventory box below it (as the mod chests).
        UiBoxes.box(g, leftPos, topPos, DeathVaultMenu.WIDTH, VAULT_HEIGHT, PALETTE);
        UiBoxes.box(g, leftPos, topPos + INVENTORY_Y - UiBoxes.FRAME, DeathVaultMenu.WIDTH, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.isActive() && slot.y < INVENTORY_Y) avoid.add(new int[] {slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17});
        }
        avoid.add(new int[] {4, 4, 12 + font.width(title), 15});
        avoid.add(new int[] {UiBoxes.FRAME, DeathVaultMenu.BAND_Y - 1, DeathVaultMenu.WIDTH - UiBoxes.FRAME, DeathVaultMenu.BAND_Y + 18});
        UiMotifs.draw(g, leftPos, topPos, UiMotifs.Kind.RUNE, UiBoxes.FRAME, UiBoxes.FRAME, DeathVaultMenu.WIDTH - 2 * UiBoxes.FRAME,
                VAULT_HEIGHT - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, PALETTE, avoid, 5);
        // Only real fields get a sunk slot: rows without a drop stay plain, the offering slot only with the redeem option.
        for (Slot slot : menu.slots) {
            if (slot.isActive()) UiBoxes.slot(g, leftPos + slot.x, topPos + slot.y, slot.y >= INVENTORY_Y ? UiPalette.INVENTORY : PALETTE);
        }
        panel(g);
    }

    /** Legend box right of the vault in the same violet; the list itself sits in a light field with dark ink (UI-BAUSTEINE). */
    private void panel(GuiGraphicsExtractor g) {
        UiBoxes.box(g, leftPos + PANEL_X, topPos, PANEL_W, DeathVaultMenu.HEIGHT, PALETTE);
        UiBoxes.insetColored(g, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H, PALETTE, UiPalette.INVENTORY.fill());
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
        float left = never(index) ? 1.0F : Mth.clamp(1.0F - age(index) / (float) lifespan(index), 0.0F, 1.0F);
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
            else if (running(i)) lines.add(Component.translatable("screen.simpletools.death_vault.timer", clock(lifespan(i) - age(i))).withColor(0x55D05A));
            else lines.add(Component.translatable("screen.simpletools.death_vault.paused.tip", clock(lifespan(i) - age(i))).withColor(0xE08A30));
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
        // Only the title, in the box's light label colour; no inventory label (as the mod chests and shops, owner N23).
        g.text(font, title, titleLabelX, titleLabelY, PALETTE.label(), false);
        if (menu.offerEnabled()) g.text(font, Component.translatable("screen.simpletools.death_vault.offering"), 8, DeathVaultMenu.OFFER_Y + 5, PALETTE.label(), false);
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
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.text(font, lines.get(i), 8, y + i * 9, PALETTE.label(), false);
    }

    /** Legend: colour key, fate and how many of the shown drops have it; below it the state of the despawn timers. */
    private void legend(GuiGraphicsExtractor g) {
        int x = LIST_X + 3, width = LIST_W - 6;
        g.text(font, Component.translatable("screen.simpletools.death_vault.legend"), PANEL_X + 8, 6, PALETTE.label(), false);
        // Ink on the light field, every colour at least 4.5:1 (follows the inventory brightness like the field itself).
        int field = UiPalette.INVENTORY.fill(), text = UiPalette.INVENTORY.label();
        int dim = UiContrast.ensure(UiPalette.mix(text, field, 0.3), field), warn = UiContrast.ensure(WARN, field);
        int[] count = new int[16];
        int lyingRunning = 0, lyingStalled = 0, next = Integer.MAX_VALUE;
        for (int i = 0; i < menu.data(DeathVaultMenu.D_COUNT); i++) {
            count[fate(i).id]++;
            if (lying(i) && !never(i)) {
                if (running(i)) {
                    lyingRunning++;
                    next = Math.min(next, lifespan(i) - age(i));
                } else lyingStalled++;
            }
        }
        int y = LIST_Y + 4;
        for (Fate fate : Fate.DISPLAY_ORDER) {
            if (fate == Fate.REDEEMED && !menu.offerEnabled() && count[fate.id] == 0) continue;
            int n = count[fate.id];
            g.fill(x, y + 1, x + 8, y + 9, 0xFF000000);
            g.fill(x + 1, y + 2, x + 7, y + 8, fate.color);
            List<FormattedCharSequence> lines = font.split(fate.label().copy().append(" (" + n + ")"), width - 12);
            for (FormattedCharSequence line : lines) {
                g.text(font, line, x + 12, y, n == 0 ? dim : text, false);
                y += 9;
            }
            y += 2;
        }
        int sy = LIST_Y + LIST_H - 3;
        List<FormattedCharSequence> status = new ArrayList<>();
        int color = text;
        if (lyingStalled > 0) {
            status = font.split(Component.translatable("screen.simpletools.death_vault.paused"), width - 12);
            color = warn;
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
