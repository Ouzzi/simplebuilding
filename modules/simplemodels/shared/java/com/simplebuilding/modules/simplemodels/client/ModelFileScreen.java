package com.simplebuilding.modules.simplemodels.client;

import com.simplebuilding.modules.simplemodels.Models;
import com.simplebuilding.modules.simplemodels.editor.ModelExport;
import com.simplelib.api.client.ui.UiBoxes;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * File dialog of the model editor (b-models14, concept §5): name and base item, then
 * <ul>
 *   <li><b>Save</b> - workspace {@code config/simplemodels/workspace/<name>.json};</li>
 *   <li><b>Use as model</b> - resource pack {@code resourcepacks/simplemodels-editor} + catalogue entry
 *       {@code config/simplemodels/catalog/<name>.json}, pack switched on and resources reloaded; in single player the
 *       integrated server reloads its catalogue, so the anvil offers {@code @model:mymodels:<name>} right away;</li>
 *   <li><b>Zip</b> - the pack files as {@code simplemodels/exports/<name>.zip};</li>
 *   <li>saved workspaces listed to open again.</li>
 * </ul>
 * All file work (names, path safety, formats) is {@link ModelExport}; this screen only collects input and reports.
 */
public final class ModelFileScreen extends Screen {
    static final int PANEL_W = 300;

    private final ModelEditorScreen editor;
    private EditBox name, base;
    private Component message = Component.empty();
    private boolean error;
    private List<String> saved = List.of();

    public ModelFileScreen(ModelEditorScreen editor) {
        super(Component.translatable("simplemodels.editor.file.title"));
        this.editor = editor;
    }

    static Path workspaceDir() { return Models.root.resolve("workspace"); }
    static Path catalogDir() { return Models.root.resolve("catalog"); }
    static Path exportDir() { return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("simplemodels").resolve("exports"); }
    static Path packDir() { return net.minecraft.client.Minecraft.getInstance().getResourcePackDirectory().resolve(ModelExport.PACK_FOLDER); }

    public Component message() { return message; }
    public boolean error() { return error; }
    public void setName(String value) { if (name != null) name.setValue(value); }

    private int left() { return (width - Math.min(PANEL_W, width - 16)) / 2; }
    private int panelW() { return Math.min(PANEL_W, width - 16); }

    @Override
    protected void init() {
        int x = left(), w = panelW(), y = 30;
        name = new EditBox(font, x + 90, y, w - 96, 16, Component.translatable("simplemodels.editor.file.name"));
        name.setMaxLength(ModelExport.MAX_NAME);
        name.setValue(editor.fileName());
        addRenderableWidget(name);
        base = new EditBox(font, x + 90, y + 22, w - 96, 16, Component.translatable("simplemodels.editor.file.base"));
        base.setMaxLength(128);
        base.setValue(editor.baseItem());
        addRenderableWidget(base);
        int bw = (w - 12 - 6) / 3;
        button("save", x + 6, y + 46, bw, this::save);
        button("use", x + 9 + bw, y + 46, bw, this::use);
        button("zip", x + 12 + 2 * bw, y + 46, bw, this::zip);
        try {
            saved = ModelExport.workspaces(workspaceDir());
        } catch (java.io.IOException e) {
            saved = List.of();
        }
        int listY = y + 100, rows = Math.max(0, (height - 40 - listY) / 22);
        for (int i = 0; i < Math.min(rows, saved.size()); i++) {
            String n = saved.get(i);
            addRenderableWidget(Button.builder(Component.translatable("simplemodels.editor.file.open", n), b -> open(n))
                    .bounds(x + 6, listY + i * 22, w - 12, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(x + w - 86, height - 28, 80, 20).build());
    }

    private void button(String key, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable("simplemodels.editor.file." + key), b -> action.run()).bounds(x, y, w, 20)
                .tooltip(Tooltip.create(Component.translatable("simplemodels.editor.file." + key + ".hint"))).build());
    }

    /** The checked name, or null with an error message. */
    private String checkedName() {
        String n = ModelExport.safeName(name.getValue());
        if (n == null) report(true, Component.translatable("simplemodels.editor.file.bad_name", ModelExport.MAX_NAME));
        else { name.setValue(n); editor.fileName(n); }
        return n;
    }

    private void report(boolean bad, Component text) {
        error = bad;
        message = text;
    }

    public void save() {
        String n = checkedName();
        if (n == null) return;
        try {
            ModelExport.saveWorkspace(workspaceDir(), n, editor.state().committed());
            report(false, Component.translatable("simplemodels.editor.file.saved", "config/simplemodels/workspace/" + n + ".json"));
            rebuildWidgets();
        } catch (java.io.IOException | RuntimeException e) {
            report(true, Component.translatable("simplemodels.editor.file.failed", String.valueOf(e.getMessage())));
        }
    }

    /** Pack + catalogue entry + pack on + reload (single player: catalogue reload on the integrated server). */
    public void use() {
        String n = checkedName();
        if (n == null) return;
        Identifier item = Identifier.tryParse(base.getValue().trim());
        if (item == null || !BuiltInRegistries.ITEM.containsKey(item) || BuiltInRegistries.ITEM.getValue(item) == Items.AIR) {
            report(true, Component.translatable("simplemodels.editor.file.bad_base", base.getValue()));
            return;
        }
        editor.baseItem(item.toString());
        var mc = minecraft;
        try {
            ModelExport.writeFiles(packDir(), ModelExport.packFiles(editor.state().committed(), n, net.minecraft.SharedConstants.RESOURCE_PACK_FORMAT_MAJOR));
            ModelExport.writeFiles(catalogDir(), Map.of(n + ".json", ModelExport.catalogueEntry(n, item.toString(), mc.getUser().getName())));
        } catch (java.io.IOException | RuntimeException e) {
            report(true, Component.translatable("simplemodels.editor.file.failed", String.valueOf(e.getMessage())));
            return;
        }
        var repo = mc.getResourcePackRepository();
        repo.reload();
        if (repo.addPack(ModelExport.PACK_ID)) mc.options.updateResourcePacks(repo);
        else mc.reloadResourcePacks();
        var server = mc.getSingleplayerServer();
        if (server != null) server.execute(() -> { Models.reload(); server.getPlayerList().getPlayers().forEach(Models.send); });
        report(false, Component.translatable(server != null ? "simplemodels.editor.file.used" : "simplemodels.editor.file.used_remote",
                "@model:" + ModelExport.modelId(n), item.toString()));
    }

    public void zip() {
        String n = checkedName();
        if (n == null) return;
        try {
            Path zip = ModelExport.writeZip(exportDir(), n, ModelExport.packFiles(editor.state().committed(), n, net.minecraft.SharedConstants.RESOURCE_PACK_FORMAT_MAJOR));
            report(false, Component.translatable("simplemodels.editor.file.zipped", "simplemodels/exports/" + zip.getFileName()));
        } catch (java.io.IOException | RuntimeException e) {
            report(true, Component.translatable("simplemodels.editor.file.failed", String.valueOf(e.getMessage())));
        }
    }

    private void open(String n) {
        try {
            var result = ModelExport.loadWorkspace(workspaceDir(), n);
            if (!result.ok()) {
                var p = result.problems().getFirst();
                report(true, Component.translatable("simplemodels.editor.file.broken", n, p.path().isEmpty() ? "JSON" : p.path()));
                return;
            }
            editor.fileName(n);
            editor.state().editModel(result.model());
            onClose();
        } catch (java.io.IOException e) {
            report(true, Component.translatable("simplemodels.editor.file.failed", String.valueOf(e.getMessage())));
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(editor);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        UiBoxes.box(g, 0, 0, width, height, ModelEditorScreen.PALETTE, false);
        int x = left(), w = panelW(), y = 30;
        g.text(font, title, x + 6, 12, ModelEditorScreen.LABEL, false);
        g.text(font, Component.translatable("simplemodels.editor.file.name"), x + 6, y + 4, ModelEditorScreen.LABEL, false);
        g.text(font, Component.translatable("simplemodels.editor.file.base"), x + 6, y + 26, ModelEditorScreen.LABEL, false);
        g.textWithWordWrap(font, message, x + 6, y + 70, w - 12, error ? FileColours.ERROR_TEXT : ModelEditorScreen.LABEL, false);
        g.text(font, Component.translatable(saved.isEmpty() ? "simplemodels.editor.file.none_saved" : "simplemodels.editor.file.saved_list"),
                x + 6, y + 88, ModelEditorScreen.LABEL, false);
    }

    /** Message colours on the panel grey #C6C6C6: error red #8E1B1B = 5.3:1. */
    static final class FileColours {
        static final int ERROR_TEXT = 0xFF8E1B1B, PANEL_FACE = 0xFFC6C6C6;
    }
}
