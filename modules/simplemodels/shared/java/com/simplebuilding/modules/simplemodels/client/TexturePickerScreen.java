package com.simplebuilding.modules.simplemodels.client;

import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorState;
import com.simplebuilding.modules.simplemodels.editor.TextureChoice;
import com.simplebuilding.modules.simplemodels.editor.UvEdit;
import com.simplelib.api.client.ui.UiBoxes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

/**
 * Texture choice for the model editor (b-models14, concept §3 "Texturwahl"): left the model's texture variables (click =
 * the selected face uses that variable, "+" adds one), right a search over every texture in the block atlas (vanilla and
 * mods) as an icon grid; clicking an icon sets the chosen variable to that texture (one undo step in the editor). Only
 * atlas references, no image files. Logic in {@link TextureChoice}.
 */
public final class TexturePickerScreen extends Screen {
    static final int CELL = 18, LIST_W = 124, MARGIN = 8;

    private final ModelEditorScreen editor;
    private final ModelEditorState state;
    private final List<String> atlas;
    private List<String> results = List.of();
    private String variable;
    private EditBox search;
    private int scroll;
    private String hovered;

    public TexturePickerScreen(ModelEditorScreen editor) {
        super(Component.translatable("simplemodels.editor.textures.title"));
        this.editor = editor;
        this.state = editor.state();
        this.atlas = atlasIds();
        EditableModel.Element e = state.selectedElement();
        EditableModel.Face face = e == null ? null : e.faces().get(state.face());
        if (face != null && face.texture().startsWith("#") && state.model().textures().containsKey(face.texture().substring(1))) variable = face.texture().substring(1);
        else variable = state.model().textures().keySet().stream().filter(k -> !k.equals("particle")).findFirst().orElse(null);
    }

    /**
     * Block textures of all loaded packs ({@code <ns>:textures/block/...png} -> {@code <ns>:block/...}) that are stitched
     * into the block atlas (the atlas has no public id list on Fabric, so the resource list is checked against it).
     */
    static List<String> atlasIds() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var t = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (!(t instanceof TextureAtlas a)) return List.of();
        var missing = a.missingSprite();
        return mc.getResourceManager().listResources("textures/block", id -> id.getPath().endsWith(".png")).keySet().stream()
                .map(id -> id.withPath(p -> p.substring("textures/".length(), p.length() - 4)))
                .filter(id -> a.getSprite(id) != missing)
                .map(Object::toString).distinct().sorted().toList();
    }

    public String variable() { return variable; }
    public List<String> results() { return results; }
    public void search(String query) { if (search != null) search.setValue(query); update(query); }

    @Override
    protected void init() {
        int top = MARGIN + 14;
        search = new EditBox(font, MARGIN + LIST_W + 6, top, width - 2 * MARGIN - LIST_W - 6, 16, Component.translatable("simplemodels.editor.textures.search"));
        search.setHint(Component.translatable("simplemodels.editor.textures.search"));
        search.setResponder(this::update);
        addRenderableWidget(search);
        setInitialFocus(search);
        addRenderableWidget(Button.builder(Component.literal("+ ").append(Component.translatable("simplemodels.editor.textures.new")), b -> addVariable())
                .bounds(MARGIN + 3, height - MARGIN - 40, LIST_W - 6, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width - MARGIN - 80, height - MARGIN - 20, 80, 20).build());
        update(search.getValue());
    }

    private void update(String query) {
        results = TextureChoice.search(atlas, query, TextureChoice.MAX_RESULTS);
        scroll = 0;
    }

    private void addVariable() {
        String name = TextureChoice.freeVariable(state.model(), "texture");
        String value = variable == null ? "minecraft:block/stone" : String.valueOf(TextureChoice.resolve(state.model(), "#" + variable));
        state.editModel(TextureChoice.assign(state.model(), name, value.equals("null") ? "minecraft:block/stone" : value));
        useVariable(name);
    }

    /** The selected face now draws {@code #name}. */
    private void useVariable(String name) {
        variable = name;
        EditableModel.Element e = state.selectedElement();
        if (e != null && e.faces().containsKey(state.face())) state.editElement(el -> UvEdit.setTexture(el, state.face(), "#" + name));
    }

    /** Clicking an icon: the chosen variable gets that texture. */
    public void choose(String id) {
        if (variable == null) {
            variable = TextureChoice.freeVariable(state.model(), "texture");
            state.editModel(TextureChoice.assign(state.model(), variable, id));
            useVariable(variable);
        } else {
            state.editModel(TextureChoice.assign(state.model(), variable, id));
        }
    }

    private int columns() {
        return Math.max(1, (width - 2 * MARGIN - LIST_W - 6) / CELL);
    }

    private int gridTop() {
        return MARGIN + 34;
    }

    private int rows() {
        return Math.max(1, (height - MARGIN - 26 - gridTop()) / CELL);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        // Variable rows.
        var variables = TextureChoice.variables(state.model());
        int rowY = MARGIN + 16;
        if (mx >= MARGIN && mx < MARGIN + LIST_W && my >= rowY) {
            int index = (int) ((my - rowY) / 12);
            if (index >= 0 && index < variables.size() && my < height - MARGIN - 44) {
                useVariable(variables.get(index).name());
                return true;
            }
        }
        // Icon grid.
        int gx = MARGIN + LIST_W + 6, cols = columns();
        if (mx >= gx && mx < gx + cols * CELL && my >= gridTop() && my < gridTop() + rows() * CELL) {
            int index = ((int) ((my - gridTop()) / CELL) + scroll) * cols + (int) ((mx - gx) / CELL);
            if (index >= 0 && index < results.size()) {
                choose(results.get(index));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, (results.size() + columns() - 1) / columns() - rows());
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
        return true;
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
        g.text(font, title, MARGIN, MARGIN, ModelEditorScreen.LABEL, false);
        // Variables list.
        int listBottom = height - MARGIN - 44;
        UiBoxes.insetColored(g, MARGIN, MARGIN + 14, LIST_W, listBottom - MARGIN - 14, ModelEditorScreen.PALETTE, ModelEditorScreen.LIST_FACE);
        int y = MARGIN + 16;
        for (var v : TextureChoice.variables(state.model())) {
            if (y + 12 > listBottom) break;
            boolean selected = v.name().equals(variable);
            if (selected) g.fill(MARGIN + 1, y, MARGIN + LIST_W - 1, y + 12, ModelEditorScreen.ROW_SELECTED);
            TextureAtlasSprite sprite = ModelViewport.sprite(state.model(), "#" + v.name());
            if (sprite != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, MARGIN + 3, y + 1, 10, 10);
            String text = font.plainSubstrByWidth("#" + v.name() + " = " + (v.resolved() == null ? v.value() : shortId(v.resolved())), LIST_W - 20);
            g.text(font, text, MARGIN + 16, y + 2, selected ? ModelEditorScreen.ROW_SELECTED_TEXT : ModelEditorScreen.LABEL, false);
            y += 12;
        }
        // Hint which face uses what.
        EditableModel.Element e = state.selectedElement();
        EditableModel.Face face = e == null ? null : e.faces().get(state.face());
        Component status = face == null ? Component.translatable("simplemodels.editor.textures.no_face")
                : Component.translatable("simplemodels.editor.textures.status", Component.translatable("simplemodels.editor.face." + state.face()), face.texture());
        g.textWithWordWrap(font, status, MARGIN, height - MARGIN - 22, width - 2 * MARGIN - 90, ModelEditorScreen.LABEL, false);
        // Icon grid.
        int gx = MARGIN + LIST_W + 6, cols = columns(), rows = rows();
        g.fill(gx - 1, gridTop() - 1, gx + cols * CELL + 1, gridTop() + rows * CELL + 1, UiBoxes.RIM);
        g.fill(gx, gridTop(), gx + cols * CELL, gridTop() + rows * CELL, ModelEditorScreen.UV_BACK);
        var t = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        hovered = null;
        if (t instanceof TextureAtlas a) {
            for (int i = 0; i < rows * cols; i++) {
                int index = scroll * cols + i;
                if (index >= results.size()) break;
                int cx = gx + (i % cols) * CELL, cy = gridTop() + (i / cols) * CELL;
                String id = results.get(index);
                var sprite = a.getSprite(net.minecraft.resources.Identifier.parse(id));
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, cx + 1, cy + 1, 16, 16);
                if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                    hovered = id;
                    g.fill(cx, cy, cx + CELL, cy + 1, ModelViewport.SELECTION);
                    g.fill(cx, cy + CELL - 1, cx + CELL, cy + CELL, ModelViewport.SELECTION);
                    g.fill(cx, cy, cx + 1, cy + CELL, ModelViewport.SELECTION);
                    g.fill(cx + CELL - 1, cy, cx + CELL, cy + CELL, ModelViewport.SELECTION);
                }
            }
        }
        Component count = Component.translatable("simplemodels.editor.textures.count", results.size(), atlas.size());
        g.text(font, count, width - MARGIN - font.width(count), MARGIN, ModelEditorScreen.LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        if (hovered != null) g.setTooltipForNextFrame(Component.literal(hovered), mx, my);
    }

    /** {@code minecraft:block/oak_planks} -> {@code oak_planks}; other namespaces keep their namespace. */
    static String shortId(String id) {
        String s = id.startsWith("minecraft:") ? id.substring(10) : id;
        return s.replace("block/", "");
    }
}
