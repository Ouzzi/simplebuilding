package com.simplebuilding.modules.simpletools.client;

import com.simplebuilding.modules.simpletools.vault.DeathVault;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import com.simplebuilding.modules.simpletools.vault.Fate;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 * legend box on the right holds one card per group (owner 23.10.: status icons in group cards framed like the UI boxes).
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
    /**
     * Legend cards (owner 23.10.: variant A's status icons in variant C's group cards, the card frame in the style of the
     * other UI parts - outline and light line with stepped corners - and "Lost" red instead of orange): left edge, width,
     * first card, head strip height, line pitch, gap between cards.
     */
    private static final int CARD_X = PANEL_X + 6, CARD_W = PANEL_W - 12, CARD_Y = 17, HEAD = 11, ROW = 12, CARD_GAP = 3;
    /** Legend groups (heading key suffix + fates) and the short legend names. */
    private static final Fate[][] GROUPS = {{Fate.LYING}, {Fate.PICKED_OWNER, Fate.PICKED_OTHER, Fate.REDEEMED},
            {Fate.BURNED, Fate.DESPAWNED, Fate.VOID, Fate.OTHER}};
    private static final String[] GROUP_NAMES = {"here", "picked", "lost"};
    /** Head strip of each group card; white text on them reads 7.8:1 (green), 8.5:1 (blue), 7.3:1 (red). */
    private static final int[] GROUP_HEADS = {0xFF1F5E22, 0xFF1D4C8C, 0xFFA12A22};
    private static final int HEAD_TEXT = 0xFFFFFFFF;
    private static final String GROUP_KEY = "screen.simpletools.death_vault.group.", LABEL_KEY = "screen.simpletools.death_vault.legend.";
    /** Outline of the status icons (10:1 on the light card body). */
    private static final int ICON_OUTLINE = 0xFF1E1A26;
    /**
     * 9x9 status icon per fate ('#' fate colour, 'o' outline): hourglass, figure, two figures, emerald (bought back),
     * flame, puff, fall into the hole, question mark - readable without the colours too (variant A, 22.10.).
     */
    private static final Map<Fate, String[]> ICONS = new EnumMap<>(Map.of(
            Fate.LYING, new String[] {"ooooooooo", "o#######o", ".o#####o.", "..o###o..", "...o#o...", "..o#.#o..", ".o#...#o.", "o###.###o", "ooooooooo"},
            Fate.PICKED_OWNER, new String[] {"...ooo...", "..o###o..", "..o###o..", "...ooo...", "..o###o..", ".o#####o.", ".o#####o.", ".o#####o.", ".ooooooo."},
            Fate.PICKED_OTHER, new String[] {"..oo..oo.", ".o##oo##o", ".o##oo##o", "..oo..oo.", ".o##oo##o", "o###oo###", "o###oo###", "o###oo###", "ooooooooo"},
            Fate.REDEEMED, new String[] {"....o....", "...o#o...", "..o###o..", ".o#####o.", "o#######o", ".o#####o.", "..o###o..", "...o#o...", "....o...."},
            Fate.BURNED, new String[] {"....o....", "...o#o...", "..o##o...", "..o###o..", ".o##o##o.", ".o#o.o#o.", "o##o.o##o", "o###o###o", ".ooooooo."},
            Fate.DESPAWNED, new String[] {"..o...o..", "o#o.o.o#o", ".o.o#o.o.", "..o###o..", "oo#####oo", "..o###o..", ".o.o#o.o.", "o#o.o.o#o", "..o...o.."},
            Fate.VOID, new String[] {"...o#o...", "...o#o...", "...o#o...", ".oo###oo.", "..o###o..", "...o#o...", "ooooooooo", "o#######o", "ooooooooo"},
            Fate.OTHER, new String[] {"..ooooo..", ".o#####o.", ".o#ooo#o.", "..o..o#o.", "....o#o..", "...o#o...", "...ooo...", "...o#o...", "...ooo..."}));
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

    /** Legend box right of the vault in the same violet; its group cards are drawn with the labels ({@link #legend}). */
    private void panel(GuiGraphicsExtractor g) {
        UiBoxes.box(g, leftPos + PANEL_X, topPos, PANEL_W, DeathVaultMenu.HEIGHT, PALETTE);
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

    /**
     * Legend (SP22-3, owner 23.10. "take A and combine it with C"): one card per group - what still lies at the death
     * spot, what was picked up (or bought back), what is lost - with a coloured head strip (name and group total), and
     * one line per fate with its status icon, a short name and the count right aligned; fates without a drop dimmed.
     * The timer state sits in a small card of its own at the bottom.
     */
    private void legend(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("screen.simpletools.death_vault.legend"), PANEL_X + 8, 6, PALETTE.label(), false);
        // Ink on the light card body, every colour at least 4.5:1 (follows the inventory brightness like the body itself).
        int body = UiPalette.INVENTORY.fill(), text = UiPalette.INVENTORY.label();
        int dim = UiContrast.ensure(UiPalette.mix(text, body, 0.12), body), warn = UiContrast.ensure(WARN, body);
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
        int x = CARD_X + 4, right = CARD_X + CARD_W - 4, y = CARD_Y;
        for (int group = 0; group < GROUPS.length; group++) {
            List<Fate> shown = new ArrayList<>();
            int total = 0;
            for (Fate fate : GROUPS[group]) {
                if (fate == Fate.REDEEMED && !menu.offerEnabled() && count[fate.id] == 0) continue;
                shown.add(fate);
                total += count[fate.id];
            }
            int height = 2 + HEAD + 3 + shown.size() * ROW + 2;
            card(g, y, height, GROUP_HEADS[group], body);
            int head = UiContrast.ensure(HEAD_TEXT, GROUP_HEADS[group]);
            String sum = Integer.toString(total);
            List<FormattedCharSequence> title = font.split(Component.translatable(GROUP_KEY + GROUP_NAMES[group]), right - font.width(sum) - 4 - x);
            if (!title.isEmpty()) g.text(font, title.get(0), x, y + 4, head, false);
            g.text(font, sum, right - font.width(sum), y + 4, head, false);
            int line = y + 2 + HEAD + 4;
            for (Fate fate : shown) {
                int n = count[fate.id], ink = n == 0 ? dim : text;
                String number = Integer.toString(n);
                int numberX = right - font.width(number);
                icon(g, x, line - 1, fate);
                List<FormattedCharSequence> name = font.split(Component.translatable(LABEL_KEY + fate.key), numberX - 4 - (x + 13));
                if (!name.isEmpty()) g.text(font, name.get(0), x + 13, line, ink, false);
                g.text(font, number, numberX, line, ink, false);
                line += ROW;
            }
            y += height + CARD_GAP;
        }
        List<FormattedCharSequence> status = new ArrayList<>();
        int color = text;
        if (lyingStalled > 0) {
            status = font.split(Component.translatable("screen.simpletools.death_vault.paused"), right - x - 12);
            color = warn;
        } else if (lyingRunning > 0) {
            status = font.split(Component.translatable("screen.simpletools.death_vault.next", clock(next)), right - x);
        }
        if (!status.isEmpty()) {
            int height = 2 + 3 + status.size() * 9 + 3, top = DeathVaultMenu.HEIGHT - UiBoxes.FRAME_BOTTOM - 2 - height;
            card(g, top, height, 0, body);
            if (lyingStalled > 0) pauseMark(g, x, top + 5);
            for (int i = 0; i < status.size(); i++) g.text(font, status.get(i), x + (lyingStalled > 0 ? 12 : 0), top + 5 + i * 9, color, false);
        }
    }

    /**
     * One legend card at {@code y}: outline and light line with the stepped 2-1 corners of the UI boxes around a light
     * body, and a head strip of {@link #HEAD} px in {@code headColor} (none when 0) with a darker line below it.
     */
    private static void card(GuiGraphicsExtractor g, int y, int height, int headColor, int body) {
        UiBoxes.stairFrame(g, CARD_X, y, CARD_W, height, new int[] {UiBoxes.RIM, UiPalette.INVENTORY.light()}, body);
        if (headColor != 0) {
            g.fill(CARD_X + 2, y + 2, CARD_X + CARD_W - 2, y + 2 + HEAD, headColor);
            g.fill(CARD_X + 2, y + 2 + HEAD, CARD_X + CARD_W - 2, y + 3 + HEAD, UiPalette.scale(headColor, 0.6));
        }
    }

    /** The 9x9 status icon of {@code fate} (variant A): fate colour with a dark outline, so it reads on the light body. */
    private static void icon(GuiGraphicsExtractor g, int x, int y, Fate fate) {
        String[] rows = ICONS.get(fate);
        if (rows == null) return;
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                char ch = rows[r].charAt(c);
                if (ch == '#') g.fill(x + c, y + r, x + c + 1, y + r + 1, fate.color);
                else if (ch == 'o') g.fill(x + c, y + r, x + c + 1, y + r + 1, ICON_OUTLINE);
            }
        }
    }
}
