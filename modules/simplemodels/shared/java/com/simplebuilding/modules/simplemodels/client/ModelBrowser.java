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
import java.util.List;

/** Searchable server catalogue. Previews are copies; assignment uses the vanilla anvil packet. */
public final class ModelBrowser extends Screen {
    public static ModelCatalogue.Snapshot catalogue = new ModelCatalogue.Snapshot(new ModelPolicy(), List.of());
    private final Screen parent;
    private final AnvilMenu anvil;
    private EditBox search;
    private String query = "", tab = "models";
    private int page;
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
        if (catalogue.policy().enabled || input.isEmpty()) return input;
        var model = input.get(DataComponents.ITEM_MODEL);
        var base = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if (model == null || catalogue.models().stream().noneMatch(d -> d.base_item().equals(base) && d.model().equals(model.toString()))) return input;
        var output = input.copy(); var original = new ItemStack(input.getItem()).get(DataComponents.ITEM_MODEL);
        if (original == null) output.remove(DataComponents.ITEM_MODEL); else output.set(DataComponents.ITEM_MODEL, original);
        return output;
    }
    public List<ModelDefinition> filtered() { return catalogue.models().stream().filter(d -> d.matches(query)).toList(); }
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
        button("models", x + 6, y + 20, 94, () -> { tab = "models"; rebuildWidgets(); });
        button("policy", x + 102, y + 20, 94, () -> { tab = "policy"; rebuildWidgets(); });
        button("help", x + 198, y + 20, 94, () -> { tab = "help"; rebuildWidgets(); });
        button("back", x + panelWidth - 76, y + panelHeight - 26, 70, this::onClose);
        if (tab.equals("help")) {
            button("folder", x + 6, y + panelHeight - 26, 124, () -> {
                var folder = minecraft.gameDirectory.toPath().resolve("config/simplemodels/catalogue");
                try { java.nio.file.Files.createDirectories(folder); com.mojang.blaze3d.Blaze3D.openPath(folder); }
                catch (java.io.IOException e) { org.slf4j.LoggerFactory.getLogger("simplemodels").warn("Cannot open import folder", e); }
            });
        }
        if (!tab.equals("models")) return;
        search = new EditBox(font, x + 6, y + 44, panelWidth - 12, 18, Component.translatable("simplemodels.browser.search")) {
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
    private int rows() { return Math.max(1, (panelHeight - 154) / 24); }
    private void addRows() {
        var list = filtered(); page = Math.clamp(page, 0, Math.max(0, (list.size() - 1) / rows()));
        for (int i = page * rows(); i < Math.min(list.size(), (page + 1) * rows()); i++) {
            var def = list.get(i); int row = i - page * rows();
            var b = addRenderableWidget(Button.builder(Component.literal(def.match_name()), widget -> { selected = def; rebuildWidgets(); })
                    .bounds(x + 28, y + 68 + row * 24, panelWidth - 34, 20).build());
            b.setTooltip(Tooltip.create(Component.literal(def.base_item() + " | " + def.id() + " | " + String.join(", ", def.tags()))));
        }
        button("previous", x + 6, y + panelHeight - 78, 70, () -> { page--; rebuildWidgets(); });
        button("next", x + 78, y + panelHeight - 78, 70, () -> { page++; rebuildWidgets(); });
        button("refresh", x + 150, y + panelHeight - 78, 90, () -> { minecraft.reloadResourcePacks(); rebuildWidgets(); });
        var reload = button("reload", x + 242, y + panelHeight - 78, Math.max(70, panelWidth - 248), () -> {
            if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand("simplemodels reload");
        });
        reload.active = minecraft.getConnection() != null && minecraft.getConnection().getCommands().getRoot().getChild("simplemodels") != null;
        var apply = button("assign", x + 6, y + panelHeight - 26, 94, () -> assign(selected == null ? null : Models.PREFIX + selected.id()));
        apply.active = selected != null && anvil != null && catalogue.policy().enabled
                && anvil.getSlot(0).getItem().is(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(selected.base_item())));
        apply.setTooltip(Tooltip.create(Component.translatable("simplemodels.browser.assign_hint")));
        button("reset", x + 102, y + panelHeight - 26, 90, () -> assign(Models.RESET)).active = anvil != null;
    }
    public void assign(String request) {
        if (request == null || anvil == null || minecraft.player == null || minecraft.player.containerMenu != anvil) return;
        minecraft.getConnection().send(new ServerboundRenameItemPacket(request)); onClose();
    }
    @Override public void onClose() { if (search != null) search.setFocused(false); minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        g.fill(x, y, x + panelWidth, y + panelHeight, 0xFF272B32);
        g.fill(x + 2, y + 2, x + panelWidth - 2, y + 18, 0xFF414854);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        g.text(font, title, x + 6, y + 5, 0xFFFFFFFF, false);
        if (tab.equals("models")) {
            var list = filtered();
            for (int i = page * rows(); i < Math.min(list.size(), (page + 1) * rows()); i++)
                g.item(preview(list.get(i)), x + 7, y + 70 + (i - page * rows()) * 24);
            if (selected != null) {
                g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(ModelDefinition.safeId(selected.base_item()))), x + 6, y + panelHeight - 52);
                g.text(font, "→", x + 28, y + panelHeight - 48, 0xFFFFFFFF, false);
                g.item(preview(selected), x + 44, y + panelHeight - 52);
                g.text(font, selected.id(), x + 70, y + panelHeight - 48, 0xFFFFFFFF, false);
            }
            if (list.isEmpty()) g.textWithWordWrap(font, Component.translatable("simplemodels.browser.empty"), x + 6, y + 72, panelWidth - 12, 0xFFE3E3E3, false);
        } else if (tab.equals("policy")) {
            int row = 0;
            for (var field : ModelPolicy.class.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    var label = Component.translatable("simplemodels.config." + field.getName()).append(": " + field.get(catalogue.policy()));
                    g.text(font, label, x + 6, y + 48 + row * 23, 0xFFFFFFFF, false);
                    if (my >= y + 48 + row * 23 && my < y + 69 + row * 23)
                        g.setTooltipForNextFrame(font, font.split(Component.translatable("simplemodels.config." + field.getName() + ".tooltip"), Math.min(240, width - 16)), mx, my);
                    row++;
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            }
        } else g.textWithWordWrap(font, Component.translatable("simplemodels.browser.import"), x + 6, y + 48, panelWidth - 12, 0xFFE3E3E3, false);
    }
}
