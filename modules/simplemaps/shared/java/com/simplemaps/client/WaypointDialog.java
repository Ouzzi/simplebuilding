package com.simplemaps.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplemaps.CubeNet;
import com.simplemaps.IconPickerLayout;
import com.simplemaps.Waypoint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Configure waypoint" (owner N65): a name, then an icon picked like in the creative search tab (search field and a grid of
 * blocks, items and mob heads), then how it is shown: the 3D block model, or one side picked on the unfolded cube. Items
 * without a block model have no side choice. The screen hosts the widgets and the buttons' actions.
 */
final class WaypointDialog {
    private static final String[] FACE_KEYS = {"model", "north", "east", "south", "west", "up", "down"};
    /** Operator-only items that no player should pick as an icon. */
    private static final Set<String> HIDDEN = Set.of("command_block", "chain_command_block", "repeating_command_block",
            "command_block_minecart", "structure_block", "jigsaw", "test_block", "test_instance_block", "debug_stick",
            "knowledge_book", "barrier", "light", "structure_void");

    private final Font font;
    private final UiPalette palette;
    private final Waypoint base;
    private final Consumer<AbstractWidget> add, remove;
    private final Consumer<Waypoint> save;
    private final Runnable cancel;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<Item> all = new ArrayList<>(), shown = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private IconPickerLayout layout;
    private EditBox nameBox, searchBox;
    private Identifier icon;
    private int face, scrollRow;

    WaypointDialog(Font font, UiPalette palette, Waypoint base, Consumer<AbstractWidget> add, Consumer<AbstractWidget> remove,
                   Consumer<Waypoint> save, Runnable cancel) {
        this.font = font;
        this.palette = palette;
        this.base = base;
        this.icon = base.icon();
        this.face = base.face();
        this.add = add;
        this.remove = remove;
        this.save = save;
        this.cancel = cancel;
        Set<Item> items = new LinkedHashSet<>();
        for (ItemStack stack : CreativeModeTabs.searchTab().getDisplayItems()) items.add(stack.getItem());
        if (items.isEmpty()) BuiltInRegistries.ITEM.forEach(items::add);
        for (Item item : items) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || HIDDEN.contains(id.getPath())) continue;
            all.add(item);
            names.add(new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT) + " " + id.getPath());
        }
        filter("");
    }

    /** (Re)creates the fields for a screen of this size, keeping what was typed. */
    void init(int screenW, int screenH) {
        String name = nameBox == null ? base.name() : nameBox.getValue(), search = searchBox == null ? "" : searchBox.getValue();
        widgets.forEach(remove);
        widgets.clear();
        layout = IconPickerLayout.of(screenW, screenH);
        nameBox = new EditBox(font, layout.nameX(), layout.nameY(), layout.fieldW(), IconPickerLayout.FIELD_H, Component.translatable("screen.simplemaps.name"));
        nameBox.setMaxLength(Waypoint.MAX_NAME);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("screen.simplemaps.name"));
        searchBox = new EditBox(font, layout.nameX(), layout.searchY(), layout.fieldW(), IconPickerLayout.FIELD_H, Component.translatable("screen.simplemaps.search"));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.translatable("screen.simplemaps.search"));
        searchBox.setResponder(this::filter);
        searchBox.setValue(search);
        for (EditBox box : List.of(nameBox, searchBox)) {
            widgets.add(box);
            add.accept(box);
        }
    }

    EditBox nameBox() {
        return nameBox;
    }

    void close() {
        widgets.forEach(remove);
        widgets.clear();
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (int i = 0; i < all.size(); i++) if (q.isEmpty() || names.get(i).contains(q)) shown.add(all.get(i));
        scrollRow = 0;
    }

    private int totalRows() {
        return (shown.size() + IconPickerLayout.GRID_COLS - 1) / IconPickerLayout.GRID_COLS;
    }

    private boolean block() {
        return WaypointArt.hasBlockModel(WaypointArt.item(icon));
    }

    private Waypoint result() {
        return new Waypoint(base.slot(), base.x(), base.z(), nameBox.getValue().trim(), base.color(), icon, block() ? face : CubeNet.FRONT);
    }

    void saveNow() {
        save.accept(result());
    }

    void render(GuiGraphicsExtractor g, int mouseX, int mouseY, int screenW, int screenH) {
        IconPickerLayout l = layout;
        g.fill(0, 0, screenW, screenH, 0x60000000);
        UiBoxes.box(g, l.x(), l.y(), l.w(), l.h(), palette);
        g.text(font, Component.translatable("screen.simplemaps.configure", base.slot() + 1), l.x() + IconPickerLayout.PAD, l.y() + IconPickerLayout.PAD, palette.label(), false);
        // Icon grid.
        for (int row = 0; row < l.gridRows(); row++) {
            for (int col = 0; col < IconPickerLayout.GRID_COLS; col++) {
                int index = (scrollRow + row) * IconPickerLayout.GRID_COLS + col, sx = l.gridX() + col * IconPickerLayout.SLOT + 1, sy = l.gridY() + row * IconPickerLayout.SLOT + 1;
                UiBoxes.slot(g, sx, sy, palette);
                if (index >= shown.size()) continue;
                Item item = shown.get(index);
                if (BuiltInRegistries.ITEM.getKey(item).equals(icon)) g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                WaypointArt.drawModel(g, new ItemStack(item), sx, sy, 16, true);
            }
        }
        int rows = totalRows();
        if (rows > l.gridRows()) {
            int barX = l.gridX() + l.gridW() + 1, barH = Math.max(6, l.gridH() * l.gridRows() / rows);
            int barY = l.gridY() + (l.gridH() - barH) * scrollRow / (rows - l.gridRows());
            g.fill(barX, l.gridY(), barX + 2, l.gridY() + l.gridH(), palette.slot());
            g.fill(barX, barY, barX + 2, barY + barH, palette.light());
        }
        // Display row: preview, 3D button, unfolded cube.
        UiBoxes.sunkRect(g, l.previewX(), l.previewY(), 32, 32, palette);
        WaypointArt.draw(g, icon, block() ? face : CubeNet.FRONT, l.previewX() + 4, l.previewY() + 4, 24);
        boolean block = block();
        button(g, l.modeButtonX(), l.modeButtonY(), l.modeButtonW(), 16, Component.translatable("screen.simplemaps.face.model"),
                block && face == CubeNet.MODEL, block, mouseX, mouseY);
        if (block) {
            g.text(font, Component.translatable("screen.simplemaps.face.side"), l.modeButtonX(), l.modeButtonY() + 22, palette.label(), false);
            for (int f = CubeNet.NORTH; f <= CubeNet.DOWN; f++) {
                int cx = l.netX() + CubeNet.column(f) * IconPickerLayout.NET_CELL, cy = l.netY() + CubeNet.row(f) * IconPickerLayout.NET_CELL;
                g.fill(cx, cy, cx + IconPickerLayout.NET_CELL, cy + IconPickerLayout.NET_CELL, UiBoxes.RIM);
                WaypointArt.draw(g, icon, f, cx + 1, cy + 1, IconPickerLayout.NET_CELL - 2, false);
                if (face == f) outline(g, cx, cy, IconPickerLayout.NET_CELL, 0xFFFFFFFF);
            }
        } else {
            g.text(font, Component.translatable("screen.simplemaps.face.none"), l.modeButtonX(), l.modeButtonY() + 22, palette.label(), false);
        }
        button(g, l.x() + IconPickerLayout.PAD, l.buttonY(), l.buttonW(), IconPickerLayout.BUTTON_H, Component.translatable("gui.done"), false, true, mouseX, mouseY);
        button(g, l.x() + l.w() - IconPickerLayout.PAD - l.buttonW(), l.buttonY(), l.buttonW(), IconPickerLayout.BUTTON_H, Component.translatable("gui.cancel"), false, true, mouseX, mouseY);
    }

    /** Tooltips come after the widgets so they lie on top. */
    void tooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int slot = layout.slotAt(mouseX, mouseY);
        if (slot >= 0) {
            int index = scrollRow * IconPickerLayout.GRID_COLS + slot;
            if (index < shown.size()) g.setTooltipForNextFrame(font, new ItemStack(shown.get(index)).getHoverName(), mouseX, mouseY);
        }
        int f = block() ? CubeNet.faceAt(layout.netX(), layout.netY(), IconPickerLayout.NET_CELL, mouseX, mouseY) : 0;
        if (f > 0) g.setTooltipForNextFrame(font, Component.translatable("screen.simplemaps.face." + FACE_KEYS[f]), mouseX, mouseY);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean on, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (on) UiBoxes.sunkRect(g, x, y, w, h, palette);
        else UiBoxes.raised(g, x, y, w, h, hover ? palette.light() : enabled ? palette.fill() : UiPalette.scale(palette.fill(), 0.8));
        int color = enabled ? palette.label() : UiPalette.mix(palette.label(), palette.fill(), 0.6);
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, color, false);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int s, int color) {
        g.fill(x, y, x + s, y + 1, color);
        g.fill(x, y + s - 1, x + s, y + s, color);
        g.fill(x, y, x + 1, y + s, color);
        g.fill(x + s - 1, y, x + s, y + s, color);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Returns whether the click was used (inside the dialog everything is). */
    boolean click(double mx, double my) {
        IconPickerLayout l = layout;
        int slot = l.slotAt(mx, my);
        if (slot >= 0) {
            int index = scrollRow * IconPickerLayout.GRID_COLS + slot;
            if (index < shown.size()) {
                icon = BuiltInRegistries.ITEM.getKey(shown.get(index));
                if (!block()) face = CubeNet.FRONT;
            }
            return true;
        }
        if (block() && inside(mx, my, l.modeButtonX(), l.modeButtonY(), l.modeButtonW(), 16)) {
            face = CubeNet.MODEL;
            return true;
        }
        int f = block() ? CubeNet.faceAt(l.netX(), l.netY(), IconPickerLayout.NET_CELL, mx, my) : 0;
        if (f > 0) {
            face = f;
            return true;
        }
        if (inside(mx, my, l.x() + IconPickerLayout.PAD, l.buttonY(), l.buttonW(), IconPickerLayout.BUTTON_H)) {
            saveNow();
            return true;
        }
        if (inside(mx, my, l.x() + l.w() - IconPickerLayout.PAD - l.buttonW(), l.buttonY(), l.buttonW(), IconPickerLayout.BUTTON_H)) {
            cancel.run();
            return true;
        }
        return false;
    }

    boolean scrolled(double mx, double my, double dy) {
        if (dy == 0 || !inside(mx, my, layout.x(), layout.gridY(), layout.w(), layout.gridH())) return false;
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(dy), 0, Math.max(0, totalRows() - layout.gridRows()));
        return true;
    }

    boolean enter(int key) {
        if (key == InputConstants.KEY_RETURN) {
            saveNow();
            return true;
        }
        return false;
    }
}
