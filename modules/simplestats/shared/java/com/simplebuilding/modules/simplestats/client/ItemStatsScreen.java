package com.simplebuilding.modules.simplestats.client;

import com.simplebuilding.modules.simplestats.StatNames;
import com.simplebuilding.modules.simplestats.StatsComponents;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatFormat;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import com.simplebuilding.modules.simplestats.data.StatRows;
import com.simplebuilding.modules.simplestats.data.StatRows.Row;
import com.simplebuilding.modules.simplestats.data.StatsLayout;
import com.simplebuilding.modules.simplestats.data.StatsLayout.Rect;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The statistics of one item in the style of the vanilla statistics screen: a scrolling list with the totals and records, then
 * every counted block, mob, catch and damage type with its amount and share. Geometry and rows come from pure classes
 * ({@link StatsLayout}, {@link StatRows}); this class only draws. It shows a snapshot of the item taken when it opened.
 */
public final class ItemStatsScreen extends Screen {
    private static final int LIST_BG = 0xFF373737, SECTION = 0xFFFFD75E, TEXT = 0xFFFFFFFF, VALUE = 0xFFB8E8B8,
            SOFT = 0xFFA0A0A0, BAR = 0x4455AA55, ROW_ALT = 0x18FFFFFF, THUMB = 0xFFE0E0E0, TRACK = 0xFF202020, TITLE = 0xFF404040;

    private final ItemStack stack;
    private final StatData data;
    private final List<Row> rows;
    private final Screen parent;
    private final Locale locale;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private StatsLayout layout;
    private int scroll;
    private boolean dragging;

    public ItemStatsScreen(ItemStack stack, Screen parent) {
        super(stack.getHoverName());
        this.stack = stack.copy();
        this.data = StatsComponents.of(stack);
        this.rows = StatRows.build(data);
        this.parent = parent;
        this.locale = StatsClient.locale();
    }

    @Override
    protected void init() {
        layout = StatsLayout.of(width, height);
        scroll = StatsLayout.clampScroll(scroll, rows.size(), layout.visibleRows);
        Rect d = layout.done;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(d.x(), d.y(), d.w(), d.h()).build());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect p = layout.panel;
        UiBoxes.box(g, p.x(), p.y(), p.w(), p.h(), UiPalette.INVENTORY, false);
        Rect l = layout.list;
        UiBoxes.insetColored(g, l.x(), l.y(), l.w(), l.h(), UiPalette.INVENTORY, LIST_BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.item(stack, layout.icon.x(), layout.icon.y());
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), layout.title.w()), layout.title.x(), layout.title.y() + 4, TITLE, false);
        Rect l = layout.list;
        if (rows.isEmpty()) {
            Component none = Component.translatable("stat.simplestats.empty");
            g.text(font, none.getVisualOrderText(), l.x() + (l.w() - font.width(none)) / 2, l.y() + l.h() / 2 - 4, SOFT, false);
        }
        g.enableScissor(l.x(), l.y(), l.right(), l.bottom());
        int end = Math.min(rows.size(), scroll + layout.visibleRows);
        for (int i = scroll; i < end; i++) drawRow(g, rows.get(i), l.x(), l.y() + (i - scroll) * StatsLayout.ROW_H, l.w(), (i & 1) == 1);
        g.disableScissor();
        drawScrollbar(g);
    }

    private void drawRow(GuiGraphicsExtractor g, Row row, int x, int y, int w, boolean alt) {
        int textY = y + 5, right = x + w - 4;
        switch (row.kind()) {
            case SECTION -> {
                Component title = row.stat().equals("overview") ? Component.translatable("stat.simplestats.overview") : StatNames.list(row.stat());
                g.fill(x, y + StatsLayout.ROW_H - 2, x + w, y + StatsLayout.ROW_H - 1, 0x66FFFFFF);
                g.text(font, font.plainSubstrByWidth(title.getString(), w - 8), x + 4, textY, SECTION, false);
            }
            case TOTAL, RECORD, SINCE -> {
                if (alt) g.fill(x, y, x + w, y + StatsLayout.ROW_H, ROW_ALT);
                Component label = row.kind() == StatRows.Kind.TOTAL ? StatNames.total(row.stat())
                        : row.kind() == StatRows.Kind.RECORD ? StatNames.record(row.stat()) : Component.translatable("stat.simplestats.since");
                String value = row.kind() == StatRows.Kind.SINCE ? StatFormat.date(row.value()) : StatFormat.value(StatKeys.unit(row.stat()), row.value(), locale, false);
                drawPair(g, x + 4, textY, right, label.getString(), value, TEXT);
            }
            case ENTRY, OTHER -> {
                if (row.whole() > 0) g.fill(x, y, x + (int) Math.max(1, (long) w * row.value() / row.whole()), y + StatsLayout.ROW_H, BAR);
                ItemStack icon = row.kind() == StatRows.Kind.ENTRY ? icon(row.stat(), row.key()) : ItemStack.EMPTY;
                if (!icon.isEmpty()) g.item(icon, x + 2, y + 1);
                String label = row.kind() == StatRows.Kind.ENTRY ? StatNames.entry(row.stat(), row.key()).getString() : Component.translatable("stat.simplestats.other").getString();
                String value = StatFormat.value(StatKeys.listUnit(row.stat()), row.value(), locale, false);
                drawPair(g, x + 22, textY, right, label, value, row.kind() == StatRows.Kind.OTHER ? SOFT : TEXT);
            }
        }
    }

    private void drawPair(GuiGraphicsExtractor g, int leftX, int y, int rightX, String label, String value, int color) {
        int valueW = font.width(value);
        g.text(font, value, rightX - valueW, y, VALUE, false);
        g.text(font, font.plainSubstrByWidth(label, Math.max(8, rightX - valueW - 6 - leftX)), leftX, y, color, false);
    }

    private void drawScrollbar(GuiGraphicsExtractor g) {
        Rect b = layout.scrollbar;
        g.fill(b.x(), b.y(), b.right(), b.bottom(), TRACK);
        int thumbH = StatsLayout.thumbHeight(b.h(), rows.size(), layout.visibleRows);
        int top = b.y() + StatsLayout.thumbTop(b.h(), rows.size(), layout.visibleRows, scroll);
        g.fill(b.x(), top, b.right(), top + thumbH, THUMB);
    }

    /** Icon of a list entry (block, spawn egg of the mob, caught item); empty for entries without one. Looked up once per entry. */
    private ItemStack icon(String list, String key) {
        return icons.computeIfAbsent(list + "|" + key, k -> {
            Identifier id = Identifier.tryParse(key.contains("#") ? key.substring(0, key.indexOf('#')) : key);
            if (id == null) return ItemStack.EMPTY;
            return switch (list) {
                case StatKeys.L_BLOCKS, StatKeys.L_BRUSHED -> BuiltInRegistries.BLOCK.getOptional(id).map(b -> new ItemStack(b.asItem())).orElse(ItemStack.EMPTY);
                case StatKeys.L_CATCHES -> BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
                case StatKeys.L_KILLS, StatKeys.L_KILL_VARIANTS, StatKeys.L_SHEARED, StatKeys.L_BLOCKED_BY ->
                        BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg")).map(ItemStack::new).orElse(ItemStack.EMPTY);
                default -> ItemStack.EMPTY;
            };
        });
    }

    // ---------------------------------------------------------------- input

    private void scrollTo(int value) {
        scroll = StatsLayout.clampScroll(value, rows.size(), layout.visibleRows);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(x, y, scrollX, scrollY);
        scrollTo(scroll - (int) Math.signum(scrollY) * 3);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (StatsClient.openKey != null && StatsClient.openKey.matches(event)) {
            onClose();
            return true;
        }
        int page = Math.max(1, layout.visibleRows - 1);
        if (event.isUp()) scrollTo(scroll - 1);
        else if (event.isDown()) scrollTo(scroll + 1);
        else if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP) scrollTo(scroll - page);
        else if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN) scrollTo(scroll + page);
        else if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_HOME) scrollTo(0);
        else if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_END) scrollTo(Integer.MAX_VALUE);
        else return super.keyPressed(event);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && layout.scrollbar.contains(event.x(), event.y())) {
            dragging = true;
            dragTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    private void dragTo(double mouseY) {
        scrollTo(StatsLayout.scrollForThumb(layout.scrollbar.h(), rows.size(), layout.visibleRows, (int) mouseY - layout.scrollbar.y()));
    }
}
