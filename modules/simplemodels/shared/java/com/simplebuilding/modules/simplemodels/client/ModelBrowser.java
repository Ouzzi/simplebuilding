package com.simplebuilding.modules.simplemodels.client;

import com.simplebuilding.modules.simplemodels.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;

/**
 * Searchable server catalogue. Previews are copies; assignment uses the vanilla anvil packet. Drawn as a box in the
 * container style of the anvil it opens from (SimpleLib style blocks, N43; docs/ai/PLAN-UI-KONZEPTE-2026-10-12.md).
 */
public final class ModelBrowser extends Screen {
    /** The anvil's palette (simpleinterfaces {@code StationStyles.ANVIL}). */
    static final UiPalette PALETTE = UiPalette.derived(0xFF666666);
    /** Bottom row of buttons: 3 px above the 7 px bottom frame. */
    private static final int BOTTOM = 30;
    public static ModelCatalogue.Snapshot catalogue = new ModelCatalogue.Snapshot(new ModelPolicy(), List.of());
    private final Screen parent;
    private final AnvilMenu anvil;
    private EditBox search;
    private String query = "", tab = "models", baseFilter = ModelQuery.ALL;
    private ModelQuery.Sort sort = ModelQuery.Sort.NAME_ASC;
    private int page;
    /** Row buttons of the current page and the model each shows (tooltip preview, SM21 d). */
    private final List<Button> rowButtons = new ArrayList<>();
    private final List<ModelDefinition> rowModels = new ArrayList<>();
    private ModelDefinition selected;
    private int x, y, panelWidth, panelHeight;
    public ModelBrowser(Screen parent) {
        super(Component.translatable("simplemodels.browser.title"));
        this.parent = parent;
        this.anvil = parent instanceof AnvilScreen a ? a.getMenu() : null;
    }
    public static void accept(CataloguePayload payload) {
        try { catalogue = ModelCatalogue.decode(payload.json()); }
        catch (RuntimeException e) { clear(); }
        if (Minecraft.getInstance().gui.screen() instanceof ModelBrowser screen) {
            if (screen.selected != null && !catalogue.models().contains(screen.selected)) screen.selected = null;
            screen.rebuildWidgets();
        }
    }
    public static void clear() {
        var p = new ModelPolicy(); p.enabled = false; catalogue = new ModelCatalogue.Snapshot(p, List.of());
    }
    public static ItemStack displayStack(ItemStack input) {
        if ((com.simplebuilding.modules.simplemodels.Models.on() && catalogue.policy().enabled) || input.isEmpty()) return input;
        var model = input.get(DataComponents.ITEM_MODEL);
        var base = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if (model == null || catalogue.models().stream().noneMatch(d -> d.base_item().equals(base) && d.model().equals(model.toString()))) return input;
        var output = input.copy(); var original = new ItemStack(input.getItem()).get(DataComponents.ITEM_MODEL);
        if (original == null) output.remove(DataComponents.ITEM_MODEL); else output.set(DataComponents.ITEM_MODEL, original);
        return output;
    }
    /** The listed models: search, base-item filter and sort order applied (SM21 c). */
    public List<ModelDefinition> filtered() { return ModelQuery.view(catalogue.models(), query, baseFilter, sort); }
    public int page() { return page; }
    public int pages() { return ModelQuery.pages(filtered().size(), rows()); }
    public String baseFilter() { return baseFilter; }
    public void setBaseFilter(String baseItem) { baseFilter = baseItem; page = 0; refreshRows(); }
    public ModelQuery.Sort sort() { return sort; }
    public void setSort(ModelQuery.Sort order) { sort = order; page = 0; refreshRows(); }
    private static Component itemName(String id) {
        return id.equals(ModelQuery.ALL) ? Component.translatable("simplemodels.browser.filter.all")
                : new ItemStack(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(id))).getHoverName();
    }
    /** Tooltip of a row: the name, the preview image (base item and the model, enlarged), then the details. */
    public List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip(ModelDefinition def) {
        var lines = new ArrayList<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent>();
        java.util.function.Consumer<Component> line = c -> lines.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(c.getVisualOrderText()));
        line.accept(Component.literal(def.match_name()));
        lines.add(new ModelPreviewTooltip(new ItemStack(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(def.base_item()))), preview(def)));
        // Grey (0xAAAAAA) on the tooltip's near-black: contrast about 8:1.
        line.accept(itemName(def.base_item()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        line.accept(Component.literal(def.id()).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (!def.tags().isEmpty()) line.accept(Component.literal(String.join(", ", def.tags())).withStyle(net.minecraft.ChatFormatting.GRAY));
        line.accept(Component.translatable("simplemodels.browser.author", def.author()).withStyle(net.minecraft.ChatFormatting.GRAY));
        return lines;
    }
    public ItemStack preview(ModelDefinition def) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(def.base_item())));
        stack.set(DataComponents.ITEM_MODEL, ModelDefinition.safeId(def.model())); return stack;
    }
    private Button button(String key, int bx, int by, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.translatable("simplemodels.browser." + key), b -> action.run()).bounds(bx, by, w, 20).build());
    }
    @Override protected void init() {
        panelWidth = Math.min(420, width - 12); panelHeight = Math.min(300, height - 12);
        x = (width - panelWidth) / 2; y = (height - panelHeight) / 2;
        // The current tab is greyed out, so it is clear where you are.
        button("models", x + 6, y + 20, 94, () -> { tab = "models"; rebuildWidgets(); }).active = !tab.equals("models");
        button("policy", x + 102, y + 20, 94, () -> { tab = "policy"; rebuildWidgets(); }).active = !tab.equals("policy");
        button("help", x + 198, y + 20, 94, () -> { tab = "help"; rebuildWidgets(); }).active = !tab.equals("help");
        button("back", x + panelWidth - 76, y + panelHeight - BOTTOM, 70, this::onClose);
        if (tab.equals("help")) {
            button("folder", x + 6, y + panelHeight - BOTTOM, 124, () -> {
                var folder = com.simplebuilding.modules.simplemodels.Models.root.resolve("catalogue").toAbsolutePath();
                try { java.nio.file.Files.createDirectories(folder); com.mojang.blaze3d.Blaze3D.openPath(folder); }
                catch (java.io.IOException e) { org.slf4j.LoggerFactory.getLogger("simplemodels").warn("Cannot open import folder", e); }
            });
        }
        if (!tab.equals("models")) return;
        // Filter by base item and sort order right of the search (SM21 c); the wheel over them cycles like Vanilla.
        var items = ModelQuery.baseItems(catalogue.models());
        if (!items.contains(baseFilter)) baseFilter = ModelQuery.ALL;
        int choice = Math.min(102, (panelWidth - 16) / 4);
        addRenderableWidget(net.minecraft.client.gui.components.CycleButton.builder(ModelBrowser::itemName, baseFilter).withValues(items)
                .create(x + panelWidth - 6 - 2 * choice - 2, y + 43, choice, 20, Component.translatable("simplemodels.browser.filter"),
                        (b, value) -> setBaseFilter(value)));
        addRenderableWidget(net.minecraft.client.gui.components.CycleButton.<ModelQuery.Sort>builder(
                        s -> Component.translatable("simplemodels.browser.sort." + s.key()), sort).withValues(ModelQuery.Sort.values())
                .create(x + panelWidth - 6 - choice, y + 43, choice, 20, Component.translatable("simplemodels.browser.sort"),
                        (b, value) -> setSort(value)));
        search = new EditBox(font, x + 6, y + 44, panelWidth - 16 - 2 * choice - 2, 18, Component.translatable("simplemodels.browser.search")) {
            @Override public void setFocused(boolean focused) {
                super.setFocused(focused); Minecraft.getInstance().onTextInputFocusChange(this, focused);
            }
        };
        search.setMaxLength(128); search.setHint(Component.translatable("simplemodels.browser.search")); search.setValue(query);
        search.setResponder(q -> { query = q; page = 0; refreshRows(); }); addRenderableWidget(search);
        refreshRows();
    }
    // Rebuild on a completed query using tick; never modify children inside a widget callback.
    private boolean dirty;
    private void refreshRows() { dirty = true; }
    @Override public void tick() {
        super.tick();
        if (dirty) { dirty = false; String value = query; boolean focus = search != null && search.isFocused();
            rebuildWidgets(); query = value; if (focus && search != null) setFocused(search); dirty = false; }
    }
    @Override protected void rebuildWidgets() { super.rebuildWidgets(); if (tab.equals("models")) addRows(); }
    /** The wheel turns the pages of the model list (SM21 b); over a widget (filter, sort) that widget gets it first. */
    @Override public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (super.mouseScrolled(mx, my, scrollX, scrollY)) return true;
        if (!tab.equals("models")) return false;
        int next = ModelQuery.scroll(page, scrollY, pages());
        if (next != page) { page = next; rebuildWidgets(); }
        return true;
    }
    private int rows() { return Math.max(1, (panelHeight - 154) / 24); }
    private void addRows() {
        var list = filtered(); page = Math.clamp(page, 0, ModelQuery.pages(list.size(), rows()) - 1);
        rowButtons.clear(); rowModels.clear();
        for (int i = page * rows(); i < Math.min(list.size(), (page + 1) * rows()); i++) {
            var def = list.get(i); int row = i - page * rows();
            rowButtons.add(addRenderableWidget(Button.builder(Component.literal(def.match_name()), widget -> { selected = def; rebuildWidgets(); })
                    .bounds(x + 28, y + 68 + row * 24, panelWidth - 34, 20).build()));
            rowModels.add(def);
        }
        button("previous", x + 6, y + panelHeight - 78, 70, () -> { page--; rebuildWidgets(); }).active = page > 0;
        button("next", x + 78, y + panelHeight - 78, 70, () -> { page++; rebuildWidgets(); }).active = (page + 1) * rows() < list.size();
        button("refresh", x + 150, y + panelHeight - 78, 90, () -> { minecraft.reloadResourcePacks(); rebuildWidgets(); });
        var reload = button("reload", x + 242, y + panelHeight - 78, Math.max(70, panelWidth - 248), () -> {
            if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand("simplemodels reload");
        });
        reload.active = minecraft.getConnection() != null && minecraft.getConnection().getCommands().getRoot().getChild("simplemodels") != null;
        var apply = button("assign", x + 6, y + panelHeight - BOTTOM, 94, () -> assign(selected == null ? null : Models.PREFIX + selected.id()));
        apply.active = selected != null && anvil != null && catalogue.policy().enabled
                && anvil.getSlot(0).getItem().is(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(selected.base_item())));
        apply.setTooltip(Tooltip.create(Component.translatable("simplemodels.browser.assign_hint")));
        button("reset", x + 102, y + panelHeight - BOTTOM, 90, () -> assign(Models.RESET)).active = anvil != null;
    }
    public void assign(String request) {
        if (request == null || anvil == null || minecraft.player == null || minecraft.player.containerMenu != anvil) return;
        minecraft.getConnection().send(new ServerboundRenameItemPacket(request)); onClose();
    }
    @Override public void onClose() { if (search != null) search.setFocused(false); minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        if (com.simplelib.api.client.ui.UiStyleToggle.isEnabled()) UiBoxes.box(g, x, y, panelWidth, panelHeight, PALETTE);
        else { // comparison toggle off: the earlier dark panel
            g.fill(x, y, x + panelWidth, y + panelHeight, 0xFF272B32);
            g.fill(x + 2, y + 2, x + panelWidth - 2, y + 18, 0xFF414854);
        }
    }
    /** A read-only switch, 20x10: knob right on a green track = on, knob left on a dark track = off. */
    static void toggle(GuiGraphicsExtractor g, int sx, int sy, boolean on) {
        g.fill(sx, sy, sx + 20, sy + 10, UiBoxes.RIM);
        g.fill(sx + 1, sy + 1, sx + 19, sy + 9, on ? 0xFF3F8F2F : 0xFF3A3A3A);
        int kx = on ? sx + 10 : sx + 1;
        g.fill(kx, sy + 1, kx + 9, sy + 9, 0xFFC6C6C6);
        g.fill(kx, sy + 1, kx + 9, sy + 2, 0xFFFFFFFF);
        g.fill(kx, sy + 8, kx + 9, sy + 9, 0xFF8B8B8B);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        g.text(font, title, x + 8, y + 6, PALETTE.label(), false);
        if (tab.equals("models")) {
            var list = filtered();
            for (int i = page * rows(); i < Math.min(list.size(), (page + 1) * rows()); i++)
                g.item(preview(list.get(i)), x + 7, y + 70 + (i - page * rows()) * 24);
            if (selected != null) {
                g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(selected.base_item()))), x + 6, y + panelHeight - 52);
                g.text(font, "→", x + 28, y + panelHeight - 48, PALETTE.label(), false);
                g.item(preview(selected), x + 44, y + panelHeight - 52);
                g.text(font, selected.id(), x + 70, y + panelHeight - 48, PALETTE.label(), false);
            }
            int pages = ModelQuery.pages(list.size(), rows());
            if (pages > 1) g.text(font, (page + 1) + "/" + pages, x + panelWidth - 8 - font.width((page + 1) + "/" + pages), y + 6, PALETTE.label(), false);
            if (list.isEmpty()) g.textWithWordWrap(font, Component.translatable("simplemodels.browser.empty"), x + 8, y + 72, panelWidth - 16, PALETTE.label(), false);
            for (int i = 0; i < rowButtons.size(); i++) if (rowButtons.get(i).isHovered())
                ((com.simplebuilding.modules.simplemodels.mixin.ModelTooltipInvoker)g).simplemodels$tooltip(font, tooltip(rowModels.get(i)), mx, my,
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null, true);
        } else if (tab.equals("policy")) {
            // Read only (SM21 a): switches for on/off values, numbers right-aligned; admins edit the file.
            var fields = java.util.Arrays.stream(ModelPolicy.class.getFields()).filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers())).toList();
            int step = Math.clamp((panelHeight - 48 - BOTTOM - 4) / Math.max(1, fields.size()), 12, 23), right = x + panelWidth - 8;
            for (int row = 0; row < fields.size(); row++) {
                var field = fields.get(row); int ry = y + 48 + row * step;
                try {
                    g.text(font, Component.translatable("text.autoconfig.simplemodels.option." + field.getName()), x + 8, ry, PALETTE.label(), false);
                    if (field.get(catalogue.policy()) instanceof Boolean on) toggle(g, right - 20, ry - 1, on);
                    else { String value = String.valueOf(field.get(catalogue.policy())); g.text(font, value, right - font.width(value), ry, PALETTE.label(), false); }
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                if (mx >= x && mx < right && my >= ry - 2 && my < ry - 2 + step)
                    g.setTooltipForNextFrame(font, font.split(Component.translatable("text.autoconfig.simplemodels.option." + field.getName() + ".@Tooltip"), Math.min(240, width - 16)), mx, my);
            }
            g.textWithWordWrap(font, Component.translatable("simplemodels.browser.readonly"), x + 8, y + panelHeight - BOTTOM + 1, panelWidth - 92, PALETTE.label(), false);
        } else g.textWithWordWrap(font, Component.translatable("simplemodels.browser.import"), x + 8, y + 48, panelWidth - 16, PALETTE.label(), false);
    }
}
