package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.keys.KeyConflicts.Binding;
import com.simplelib.api.client.keys.KeyboardLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * One key of the keyboard picture (IG31-31, replaces the self-drawn popup): the actions on that key, each with "remove" and -
 * on a shared key - "move to the nearest free key", plus "assign an action here" (back to the list, which then assigns the
 * key to the clicked action). Vanilla widgets only; back returns to the key binding page.
 */
public final class KeyCellScreen extends Screen {
    private static final String K = "simpleinterfaces.keys.";
    private final KeysScreen parent;
    private final int code;
    private CellList list;
    final Map<String, AbstractWidget> probe = new LinkedHashMap<>();

    KeyCellScreen(KeysScreen parent, int code) {
        super(Component.translatable(K + "cell.title", KeyboardLayout.label(code, KeysScreen.german())));
        this.parent = parent;
        this.code = code;
    }

    public int code() {
        return code;
    }

    @Override
    protected void init() {
        probe.clear();
        list = addRenderableWidget(new CellList(minecraft, width, height - 33 - 32, 32));
        fill();
        int fy = height - 27, cx = width / 2;
        probe.put("assign", addRenderableWidget(Button.builder(Component.translatable(K + "popup.assign"), b -> parent.startPick(code))
                .bounds(cx - 154, fy, 150, 20).build()));
        probe.put("back", addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(cx + 4, fy, 150, 20).build()));
    }

    private void fill() {
        List<Binding> on = parent.byKey().getOrDefault(KeysScreen.nameOf(code), List.of());
        List<CellRow> rows = new ArrayList<>();
        for (int i = 0; i < on.size(); i++) rows.add(new CellRow(on.get(i), i > 0 && on.size() > 1));
        if (on.isEmpty()) rows.add(new CellRow(null, false));
        list.replaceEntries(rows);
        probe.keySet().removeIf(k -> k.startsWith("remove_") || k.startsWith("move_"));
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).remove != null) probe.put("remove_" + i, rows.get(i).remove);
            if (rows.get(i).move != null) probe.put("move_" + i, rows.get(i).move);
        }
    }

    private void assign(KeyMapping m, InputConstants.Key key) {
        parent.assign(m, key);
        fill();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        g.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    /** Rectangles (x, y, w, h, GUI coordinates) of the named widgets for the real-mouse probe. */
    public Map<String, int[]> probeRects() {
        Map<String, int[]> out = new LinkedHashMap<>();
        probe.forEach((name, w) -> out.put(name, KeysScreen.rect(w)));
        return out;
    }

    final class CellList extends ContainerObjectSelectionList<CellRow> {
        CellList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, 24);
        }

        @Override
        public int getRowWidth() {
            return Math.min(320, width - 20);
        }
    }

    /** One action on the key: its name, remove, and (shared key, not the first) move to the nearest free key. */
    final class CellRow extends ContainerObjectSelectionList.Entry<CellRow> {
        private final Binding binding;
        final Button remove, move;

        CellRow(Binding binding, boolean offerMove) {
            this.binding = binding;
            KeyMapping m = binding == null ? null : parent.mapping(binding);
            remove = m == null ? null : Button.builder(Component.translatable(K + "remove"), b -> assign(m, InputConstants.UNKNOWN))
                    .bounds(0, 0, 70, 20).tooltip(Tooltip.create(Component.translatable(K + "remove.tip"))).build();
            Optional<KeyboardLayout.Cell> free = !offerMove || m == null ? Optional.empty()
                    : KeyboardLayout.nearestFree(KeyboardLayout.cells(KeyboardLayout.Size.FULL, KeysScreen.german()), parent.usedCodes(), code);
            move = free.map(c -> Button.builder(Component.translatable(K + "move", c.label()), b -> assign(m, KeysScreen.keyOf(c.code())))
                    .bounds(0, 0, 70, 20).build()).orElse(null);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
            var font = Minecraft.getInstance().font;
            if (binding == null) {
                g.centeredText(font, Component.translatable(K + "tip.free"), getContentXMiddle(), getContentYMiddle() - 4, 0xFFFFFFFF);
                return;
            }
            int right = getContentRight();
            remove.setPosition(right - remove.getWidth(), getContentY());
            remove.extractRenderState(g, mouseX, mouseY, delta);
            int textRight = remove.getX() - 4;
            if (move != null) {
                move.setPosition(remove.getX() - 4 - move.getWidth(), getContentY());
                move.extractRenderState(g, mouseX, mouseY, delta);
                textRight = move.getX() - 4;
            }
            String label = binding.action() + " (" + binding.group() + ")";
            g.text(font, KeysScreen.fit(font, label, textRight - getContentX() - 4), getContentX(), getContentY() + 6, 0xFFFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            List<Button> out = new ArrayList<>();
            if (move != null) out.add(move);
            if (remove != null) out.add(remove);
            return out;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            List<Button> out = new ArrayList<>();
            if (move != null) out.add(move);
            if (remove != null) out.add(remove);
            return out;
        }
    }
}
