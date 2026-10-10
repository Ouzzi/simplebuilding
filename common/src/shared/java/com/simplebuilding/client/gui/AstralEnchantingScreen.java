package com.simplebuilding.client.gui;

import com.simplebuilding.enchanting.AstralEnchanting;
import com.simplebuilding.screen.AstralEnchantingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Astral Enchanter (owner 2026-10-09, queue N27; N31: taller, glyphs, level ticks, a plain enchant button): item,
 * lapis and blaze powder on the left like Vanilla's table, on the right three random enchantments that fit the item, each
 * with its name, its name in the enchanting glyphs (Vanilla's {@code minecraft:alt} font) and a slider 0..max with a
 * small tick per level. Click or drag on a slider, or scroll over its row; levels the budget does not allow are greyed
 * out. The button under the slots (an enchanted book and the levels it uses up) enchants. Sliders and the button reach the
 * server as Vanilla menu button clicks.
 */
public class AstralEnchantingScreen extends AbstractContainerScreen<AstralEnchantingMenu> {
    /** Slider panel (relative to the screen's left/top). */
    public static final int ROW_H = 28, ROWS = 3;
    public static final int PANEL_X = 50, PANEL_Y = 17, PANEL_W = 119, PANEL_H = ROWS * ROW_H + 4;
    public static final int TRACK_X = PANEL_X + 5, TRACK_W = PANEL_W - 10, TRACK_H = 5;
    public static final int BUTTON_X = 7, BUTTON_Y = 68, BUTTON_W = 38, BUTTON_H = 20;
    /** Vanilla's enchanting glyph font (the Standard Galactic Alphabet of the enchanting table). */
    private static final Style GLYPHS = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt")));
    static final int PANEL = 0xFF1C1529, PANEL_RIM = 0xFF0D0914, PANEL_LIGHT = 0xFF3A2D52;
    static final int TRACK_OFF = 0xFF2B2238, TRACK_FREE = 0xFF5D4A7C, TRACK_SET = 0xFFE7A93C, TICK = 0xFF6A5A86, GLYPH = 0xFF8F7BB5;
    static final int KNOB = 0xFFF7F0FF, KNOB_RIM = 0xFF3C2A55;
    static final int TEXT = 0xFFEDE4FA, TEXT_OFF = 0xFF7A6F8C, GOLD = 0xFFF2C14E, GREEN = 0xFF80FF20, RED = 0xFFE05A4A;

    private static final ItemStack ENCHANTED_BOOK = new ItemStack(Items.ENCHANTED_BOOK);

    private int dragRow = -1;

    public AstralEnchantingScreen(AstralEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, AstralEnchantingMenu.IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
    }

    // ------------------------------------------------------------------ geometry

    static int rowY(int row) {
        return PANEL_Y + 3 + row * ROW_H;
    }

    static int glyphY(int row) {
        return rowY(row) + 10;
    }

    static int trackY(int row) {
        return rowY(row) + 20;
    }

    /** x of level {@code level} on a slider with {@code max} levels (0 at the left end). */
    static int levelX(int level, int max) {
        return TRACK_X + (max <= 0 ? 0 : Math.round(level * (TRACK_W - 1) / (float) max));
    }

    /** GUI position (screen coordinates) of level {@code level} on the slider of {@code row} - for the client test. */
    public int[] sliderPoint(int row, int level) {
        return new int[] {this.leftPos + levelX(level, this.menu.maxLevel(row)), this.topPos + trackY(row) + TRACK_H / 2};
    }

    /** The slider row a held mouse button is dragging, or -1 - for the client test. */
    public int draggingRow() {
        return this.dragRow;
    }

    private int rowAt(double mx, double my) {
        double x = mx - this.leftPos, y = my - this.topPos;
        if (x < PANEL_X || x >= PANEL_X + PANEL_W) return -1;
        for (int row = 0; row < ROWS; row++) {
            if (y >= rowY(row) - 2 && y < rowY(row) + ROW_H - 2 && this.menu.maxLevel(row) > 0) return row;
        }
        return -1;
    }

    private int levelAt(int row, double mx) {
        int max = this.menu.maxLevel(row);
        double x = mx - this.leftPos - TRACK_X;
        return Math.clamp(Math.round(x * max / (TRACK_W - 1)), 0, max);
    }

    private boolean onButton(double mx, double my) {
        double x = mx - this.leftPos, y = my - this.topPos;
        return x >= BUTTON_X && x < BUTTON_X + BUTTON_W && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_H;
    }

    // ------------------------------------------------------------------ input

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    /** Sets a slider here at once (the server checks the same click again and syncs its value back). */
    private void setLevel(int row, int level) {
        int target = Math.min(level, this.menu.affordable(row));
        if (target == this.menu.chosen(row)) return;
        this.menu.clickMenuButton(this.minecraft.player, row * AstralEnchantingMenu.LEVELS_PER_ROW + target);
        send(row * AstralEnchantingMenu.LEVELS_PER_ROW + target);
        this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 0.8F + 0.08F * target);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                this.dragRow = row;
                setLevel(row, levelAt(row, event.x()));
                return true;
            }
            if (onButton(event.x(), event.y()) && this.menu.canEnchant(this.minecraft.player)) {
                send(AstralEnchantingMenu.BUTTON_ENCHANT);
                this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.0F);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragRow >= 0) {
            setLevel(this.dragRow, levelAt(this.dragRow, event.x()));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragRow >= 0) {
            this.dragRow = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && verticalAmount != 0) {
            setLevel(row, Math.clamp(this.menu.chosen(row) + (verticalAmount > 0 ? 1 : -1), 0, this.menu.maxLevel(row)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int left = this.leftPos, top = this.topPos;
        if (!ModScreenStyle.astralEnchanting(g, this.menu, this.font, this.title, left, top, this.imageWidth)) {
            plainBackground(g, left, top);
        }
        // Slider panel.
        g.fill(left + PANEL_X - 1, top + PANEL_Y - 1, left + PANEL_X + PANEL_W + 1, top + PANEL_Y + PANEL_H + 1, PANEL_RIM);
        g.fill(left + PANEL_X, top + PANEL_Y, left + PANEL_X + PANEL_W, top + PANEL_Y + PANEL_H, PANEL);
        g.fill(left + PANEL_X, top + PANEL_Y + PANEL_H - 1, left + PANEL_X + PANEL_W, top + PANEL_Y + PANEL_H, PANEL_LIGHT);
        for (int row = 0; row < ROWS; row++) {
            drawRow(g, row, left, top);
        }
        drawButton(g, left, top, mouseX, mouseY);
    }

    /** Without the style (26.2 or switched off): a plain Vanilla-grey panel with dark slots. */
    private void plainBackground(GuiGraphicsExtractor g, int left, int top) {
        g.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF555555);
        g.fill(left + 1, top + 1, left + this.imageWidth - 1, top + this.imageHeight - 1, 0xFFC6C6C6);
        for (var slot : this.menu.slots) {
            g.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, 0xFF8B8B8B);
        }
    }

    private void drawRow(GuiGraphicsExtractor g, int row, int left, int top) {
        int max = this.menu.maxLevel(row);
        if (max <= 0) return;
        Holder<Enchantment> holder = this.minecraft.level == null ? null : this.menu.enchantment(this.minecraft.level, row);
        int chosen = this.menu.chosen(row);
        int affordable = this.menu.affordable(row);
        boolean off = affordable == 0 && chosen == 0;
        int y = top + rowY(row);
        if (holder != null) {
            Component name = holder.value().description();
            g.text(this.font, this.font.plainSubstrByWidth(name.getString(), 74), left + TRACK_X, y, off ? TEXT_OFF : TEXT, false);
            // The name once more in the enchanting glyphs, like the offers on Vanilla's table.
            String id = holder.getRegisteredName();
            String word = id.substring(id.indexOf(':') + 1).replace('_', ' ');
            FormattedText glyphs = this.font.getSplitter().headByWidth(Component.literal(word).withStyle(GLYPHS), TRACK_W, Style.EMPTY);
            g.textWithWordWrap(this.font, glyphs, left + TRACK_X, top + glyphY(row), TRACK_W, off ? TRACK_OFF : GLYPH, false);
        }
        String level = (chosen == 0 ? "-" : Component.translatable("enchantment.level." + chosen).getString())
                + " / " + Component.translatable("enchantment.level." + max).getString();
        g.text(this.font, level, left + TRACK_X + TRACK_W - this.font.width(level), y, chosen > 0 ? GOLD : TEXT_OFF, false);
        int ty = top + trackY(row);
        // One segment per level with a one pixel gap between them, and a small tick under every level step.
        for (int l = 1; l <= max; l++) {
            int x0 = left + levelX(l - 1, max) + (l > 1 ? 1 : 0), x1 = left + levelX(l, max) + (l == max ? 1 : 0);
            int color = l <= chosen ? TRACK_SET : l <= affordable ? TRACK_FREE : TRACK_OFF;
            g.fill(x0, ty, x1, ty + TRACK_H, color);
        }
        for (int l = 0; l <= max; l++) {
            int x = left + levelX(l, max);
            g.fill(x, ty + TRACK_H + 1, x + 1, ty + TRACK_H + 3, l == chosen ? GOLD : TICK);
        }
        int kx = left + levelX(chosen, max);
        g.fill(kx - 2, ty - 2, kx + 3, ty + TRACK_H + 2, KNOB_RIM);
        g.fill(kx - 1, ty - 1, kx + 2, ty + TRACK_H + 1, KNOB);
    }

    private void drawButton(GuiGraphicsExtractor g, int left, int top, int mouseX, int mouseY) {
        boolean ready = this.menu.canEnchant(this.minecraft.player);
        boolean hover = onButton(mouseX, mouseY);
        int x = left + BUTTON_X, y = top + BUTTON_Y;
        g.fill(x, y, x + BUTTON_W, y + BUTTON_H, PANEL_RIM);
        g.fill(x + 1, y + 1, x + BUTTON_W - 1, y + BUTTON_H - 1, ready ? (hover ? 0xFF8F64C4 : 0xFF6D4A99) : 0xFF3A3242);
        g.fill(x + 1, y + 1, x + BUTTON_W - 1, y + 2, ready ? 0xFFB592DD : 0xFF4D4458);
        // An enchanted book and the levels the enchanting uses up (N31: the old star read as a "+" button).
        g.item(ENCHANTED_BOOK, x + 2, y + 2);
        int cost = this.menu.levelCost();
        String text = cost > 0 ? String.valueOf(cost) : "-";
        g.text(this.font, text, x + 27 - this.font.width(text) / 2, y + 6, ready ? GREEN : cost > 0 ? RED : TEXT_OFF, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!ModScreenStyle.astralEnchantingLabels(g, this.font, this.title, this.titleLabelX, this.titleLabelY)) {
            g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        }
        int budget = this.menu.budget();
        String points = this.menu.spent() + "/" + (budget >= AstralEnchanting.UNLIMITED ? "∞" : String.valueOf(budget));
        g.text(this.font, points, PANEL_X + PANEL_W - this.font.width(points), this.titleLabelY, GOLD, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (onButton(mouseX, mouseY)) {
            g.setTooltipForNextFrame(this.font, buttonTooltip(), java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        int budgetX = this.leftPos + PANEL_X + PANEL_W - 40;
        if (mouseX >= budgetX && mouseX < this.leftPos + PANEL_X + PANEL_W && mouseY >= this.topPos + 4 && mouseY < this.topPos + 15) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.tier", this.menu.tier()));
            lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.tier_hint").withStyle(ChatFormatting.GRAY));
            g.setTooltipForNextFrame(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && this.minecraft.level != null) {
            Holder<Enchantment> holder = this.menu.enchantment(this.minecraft.level, row);
            if (holder != null) {
                List<Component> lines = new ArrayList<>();
                lines.add(holder.value().description().copy().withStyle(ChatFormatting.GOLD));
                lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.points_per_level",
                        this.menu.pointsPerLevel(row)).withStyle(ChatFormatting.GRAY));
                g.setTooltipForNextFrame(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private List<Component> buttonTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.enchant"));
        int cost = this.menu.levelCost();
        if (cost <= 0) {
            lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.choose").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        var player = this.minecraft.player;
        boolean infinite = player.hasInfiniteMaterials();
        int need = this.menu.requiredLevel();
        lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.levels", cost, need)
                .withStyle(infinite || player.experienceLevel >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
        int lapis = AstralEnchanting.lapisCost(cost), blaze = AstralEnchanting.blazePowderCost(cost);
        lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.lapis", lapis)
                .withStyle(infinite || this.menu.lapis() >= lapis ? ChatFormatting.GRAY : ChatFormatting.RED));
        lines.add(Component.translatable("container.simplebuilding.astral_enchanting_table.blaze_powder", blaze)
                .withStyle(infinite || this.menu.blazePowder() >= blaze ? ChatFormatting.GRAY : ChatFormatting.RED));
        return lines;
    }
}
