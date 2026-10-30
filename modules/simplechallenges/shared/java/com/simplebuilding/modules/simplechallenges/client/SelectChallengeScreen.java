package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.core.PresetRegistry;
import com.simplebuilding.modules.simplechallenges.preset.Currency;
import com.simplebuilding.modules.simplechallenges.preset.Difficulty;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.PresetDraft;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * "Select Challenge" (S3): the list of presets (shipped and the player's own) on the left, on the right what the chosen one is, a
 * picture of it and - for the player's own presets - its settings. "Play Challenge" fixes the choice for the world that "Create New
 * World" makes. The chosen entry has the white frame of the Vanilla selection lists. Own presets are files in
 * {@code config/simplechallenges/presets/}; the shipped ones can be duplicated and then changed.
 * Colours: panel text #404040 on #C6C6C6 = 5.7:1; list text white on #2B2B2B = 14:1, grey #A0A0A0 on #2B2B2B = 6.1:1.
 */
public final class SelectChallengeScreen extends Screen {
    private static final int PAD = 8, LIST_W = 150, ROW = 22, LABEL = 0xFF404040, SECONDARY = 0xFF4A4A4A, WELL = 0xFF2B2B2B;
    private final CreateWorldScreen parent;
    private int panelX, panelY, panelW, panelH, listY, listH, detailX, detailW, previewY;
    private PresetList list;
    private EditBox nameBox;
    private CycleButton<Integer> cellButton;
    private CycleButton<Currency> currencyButton;
    private CycleButton<Difficulty> difficultyButton;
    private Button areaButton, duplicateButton, deleteButton, playButton;
    private String selectedId;
    private Component caption = Component.empty();

    public SelectChallengeScreen(CreateWorldScreen parent) {
        super(Component.translatable("simplechallenges.ui.select.title"));
        this.parent = parent;
        selectedId = com.simplebuilding.modules.simplechallenges.core.PendingChallenge.peek();
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 12, 440);
        panelH = Math.min(height - 12, 292);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        listY = panelY + 22;
        listH = panelH - 22 - PAD - 26;
        detailX = panelX + PAD + LIST_W + PAD;
        detailW = panelX + panelW - PAD - detailX;
        int y = panelY + 22;
        nameBox = addRenderableWidget(new EditBox(font, detailX, y, detailW, 18, Component.translatable("simplechallenges.ui.name")));
        nameBox.setMaxLength(48);
        nameBox.setResponder(this::renamed);
        previewY = y + 38;
        int rowY = previewY + 74 + 14, half = detailW / 2 - 2;
        cellButton = addRenderableWidget(CycleButton.builder((Integer v) -> Component.translatable("simplechallenges.ui.cell_blocks", v), 16)
                .withValues(java.util.Arrays.stream(UnlockGrid.CELL_SIZES).boxed().toList())
                .create(detailX, rowY, half, 20, Component.translatable("simplechallenges.ui.cell"), (b, v) -> edit(d -> d.cell = v)));
        currencyButton = addRenderableWidget(CycleButton.builder((Currency c) -> Component.translatable("simplechallenges.ui.currency." + c.id()), Currency.ITEMS)
                .withValues(List.of(Currency.values()))
                .create(detailX + half + 4, rowY, half, 20, Component.translatable("simplechallenges.ui.currency"), (b, v) -> edit(d -> d.setCurrency(v))));
        difficultyButton = addRenderableWidget(CycleButton.builder((Difficulty c) -> Component.translatable("simplechallenges.ui.difficulty." + c.id()), Difficulty.MEDIUM)
                .withValues(List.of(Difficulty.values()))
                .create(detailX, rowY + 24, half, 20, Component.translatable("simplechallenges.ui.difficulty"), (b, v) -> edit(d -> d.difficulty = v)));
        areaButton = addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.area.edit"), b -> openEditor()).bounds(detailX + half + 4, rowY + 24, half, 20).build());
        duplicateButton = addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.duplicate"), b -> duplicate())
                .bounds(detailX, rowY + 48, half, 20).tooltip(Tooltip.create(Component.translatable("simplechallenges.ui.duplicate.tip"))).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.delete"), b -> delete()).bounds(detailX + half + 4, rowY + 48, half, 20).build());
        int by = panelY + panelH - PAD - 20;
        playButton = addRenderableWidget(Button.builder(Component.translatable("simplechallenges.ui.play"), b -> play()).bounds(detailX, by, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(detailX + half + 4, by, half, 20).build());
        list = addRenderableWidget(new PresetList(minecraft, LIST_W, listH, listY));
        list.setX(panelX + PAD);
        rebuildList(selectedId);
    }

    // --- data ---

    private Preset selected() {
        return selectedId == null ? null : PresetRegistry.get(selectedId);
    }

    private static boolean isUser(Preset p) {
        return p != null && p.id().startsWith("user/");
    }

    private void rebuildList(String select) {
        List<Entry> entries = new ArrayList<>();
        for (String id : PresetRegistry.SHIPPED) if (PresetRegistry.get(id) != null) entries.add(new Entry(id));
        for (String id : PresetRegistry.ids()) if (!PresetRegistry.SHIPPED.contains(id)) entries.add(new Entry(id));
        list.replaceEntries(entries);
        Entry pick = null;
        for (Entry e : entries) if (e.id.equals(select)) pick = e;
        if (pick == null && !entries.isEmpty()) pick = entries.get(0);
        if (pick != null) list.setSelected(pick);
        else onSelected(null);
    }

    /** The selection changed: the detail widgets show the preset and are active only for the player's own presets. */
    private void onSelected(Entry e) {
        selectedId = e == null ? null : e.id;
        refresh();
    }

    private void refresh() {
        Preset p = selected();
        boolean user = isUser(p);
        UnlockMode u = p == null ? null : p.unlock();
        nameBox.setEditable(user);
        nameBox.setValue(p == null ? "" : p.name());
        cellButton.active = user && u != null;
        currencyButton.active = user && u != null;
        difficultyButton.active = user && u != null;
        areaButton.active = user && u != null;
        if (u != null) {
            cellButton.setValue(u.cell());
            currencyButton.setValue(u.currency());
            difficultyButton.setValue(u.difficulty());
        }
        duplicateButton.active = p != null;
        deleteButton.active = user;
        playButton.active = p != null && p.runnable();
    }

    private void renamed(String value) {
        Preset p = selected();
        if (isUser(p) && !value.isBlank() && !value.equals(p.name())) edit(d -> d.name = value);
    }

    /** Changes the selected own preset: the draft is written to its file and read back by the normal parser. */
    private void edit(Consumer<PresetDraft> change) {
        Preset p = selected();
        if (!isUser(p)) return;
        PresetDraft d = PresetDraft.from(p);
        change.accept(d);
        try {
            PresetRegistry.saveUser(SimpleChallenges.configDir, p.id().substring(5), d.toJson());
        } catch (IOException e) {
            SimpleChallenges.LOG.warn("Could not save preset {}: {}", p.id(), e.toString());
        }
    }

    private void duplicate() {
        Preset p = selected();
        if (p == null) return;
        PresetDraft d = PresetDraft.from(p);
        d.name = p.name() + " 2";
        d.family = d.worldKind != null ? "skyblock" : "unlock";
        String base = p.id().replace("user/", "").replaceAll("[^a-z0-9_-]+", "_");
        String file = base + "_copy";
        for (int i = 2; PresetRegistry.get("user/" + file) != null; i++) file = base + "_copy" + i;
        try {
            String id = PresetRegistry.saveUser(SimpleChallenges.configDir, file, d.toJson());
            rebuildList(id);
        } catch (IOException e) {
            SimpleChallenges.LOG.warn("Could not save preset {}: {}", file, e.toString());
        }
    }

    private void delete() {
        Preset p = selected();
        if (isUser(p) && PresetRegistry.deleteUser(SimpleChallenges.configDir, p.id())) rebuildList(PresetRegistry.SHIPPED.get(0));
    }

    private void openEditor() {
        Preset p = selected();
        if (!isUser(p) || p.unlock() == null) return;
        minecraft.gui.setScreen(new StartAreaEditorScreen(this, p.unlock().cell(), p.unlock().start(), rects -> edit(d -> d.start = new ArrayList<>(rects))));
    }

    private void play() {
        if (selectedId != null && CreateChallenge.choose(parent, selectedId)) back();
    }

    private void back() {
        minecraft.gui.setScreen(parent);
        ((ChallengeScreenAccess) parent).simplechallenges$openWorldTab();
    }

    @Override
    public void onClose() {
        back();
    }

    // --- drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        UiBoxes.box(g, panelX, panelY, panelW, panelH, UiPalette.INVENTORY);
        g.text(font, title, panelX + PAD, panelY + 8, LABEL, false);
        UiBoxes.insetColored(g, panelX + PAD - 1, listY - 2, LIST_W + 2, listH + 4, UiPalette.INVENTORY, WELL);
        Preset p = selected();
        if (p != null) {
            Component family = Component.translatable("simplechallenges.ui.family." + p.family());
            g.text(font, family, detailX, panelY + 8, SECONDARY, false);
            caption = PresetPreview.draw(g, font, p, detailX, previewY, detailW, 74);
            g.text(font, caption, detailX, previewY + 77, SECONDARY, false);
            if (!p.runnable()) g.text(font, Component.translatable("simplechallenges.ui.not_runnable"), detailX, previewY + 88, 0xFF8B1A1A, false);
            else if (!p.warnings().isEmpty()) g.text(font, Component.translatable("simplechallenges.ui.warning", p.warnings().get(0)), detailX, previewY + 88, 0xFF8B1A1A, false);
        }
        if (!PresetRegistry.problems().isEmpty()) {
            g.text(font, Component.translatable("simplechallenges.ui.problems", PresetRegistry.problems().size()), panelX + PAD, panelY + panelH - PAD - 12, 0xFF8B1A1A, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    // --- the list ---

    private final class PresetList extends ObjectSelectionList<Entry> {
        PresetList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, ROW);
        }

        @Override
        public int getRowWidth() {
            return getWidth() - 12;
        }

        @Override
        public int getRowLeft() {
            return getX() + 2;
        }

        @Override
        protected int scrollBarX() {
            return getX() + getWidth() - 6;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor g) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor g) {
        }

        /** The Vanilla selection frame, white also while the list is not focused (the choice stays visible). */
        @Override
        protected void extractSelection(GuiGraphicsExtractor g, Entry entry, int outlineColor) {
            super.extractSelection(g, entry, 0xFFFFFFFF);
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            onSelected(entry);
        }
    }

    private final class Entry extends ObjectSelectionList.Entry<Entry> {
        final String id;

        Entry(String id) {
            this.id = id;
        }

        @Override
        public Component getNarration() {
            Preset p = PresetRegistry.get(id);
            return Component.literal(p == null ? id : p.name());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
            Preset p = PresetRegistry.get(id);
            if (p == null) return;
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(p.icon())));
            g.item(icon, getContentX() + 2, getContentY() + (getContentHeight() - 16) / 2);
            int color = p.runnable() ? 0xFFFFFFFF : 0xFFA0A0A0;
            g.text(font, Component.literal(p.name()), getContentX() + 24, getContentY() + (getContentHeight() - 8) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            SelectChallengeScreen.this.list.setSelected(this);
            if (doubleClick) play();
            return true;
        }
    }
}
