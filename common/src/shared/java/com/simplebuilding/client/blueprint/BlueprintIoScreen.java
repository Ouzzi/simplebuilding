package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintFormats;
import com.simplebuilding.blueprint.BlueprintFormats.Format;
import com.simplebuilding.blueprint.BlueprintFormats.Exported;
import com.simplebuilding.blueprint.BlueprintFormats.Imported;
import com.simplebuilding.blueprint.BlueprintIoLayout;
import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

/**
 * Import-/Export-Fenster der Blaupause (Queue N55), ueber dem Editor ({@link BlueprintScreen}) im selben
 * Kartenblatt-Stil, Geometrie aus {@link BlueprintIoLayout}.
 * <ul>
 *   <li><b>Export</b>: der Bau als Text in einem schreibgeschuetzten Feld, Format wechselbar (Bau-Code oder
 *       Befehle), Knopf "Kopieren" legt ihn in die Zwischenablage.</li>
 *   <li><b>Import</b>: Feld zum Einfuegen (Strg+V oder Knopf "Einfuegen" aus der Zwischenablage), Format
 *       "Automatisch" oder fest; unter dem Feld das Ergebnis der Pruefung ({@link BlueprintFormats}); "Uebernehmen"
 *       ersetzt den Code im Editor und ist nur bei fehlerfreiem Text aktiv. Es entsteht nur der Plan - nichts wird
 *       gebaut oder ausgegeben; der Editor speichert danach wie gewohnt.</li>
 * </ul>
 */
public class BlueprintIoScreen extends Screen {
    private static final long COPIED_MS = 2000;
    private static final long DEBOUNCE_MS = 250;

    private final BlueprintScreen parent;
    private final boolean export;
    private final boolean canApply;
    private final String code;

    private BlueprintIoLayout layout;
    private BlueprintCodeArea area;
    private Button formatButton;
    private Button primary;
    private Button apply;
    /** Export: CODE oder COMMANDS; Import: null = automatisch. */
    private @Nullable Format format;
    private String exportText = "";
    private @Nullable Exported exported;
    private @Nullable Imported imported;
    private boolean dirty;
    private long lastEdit;
    private long copiedUntil;
    private String pendingText = "";

    /** Export des Bau-Codes {@code code}. */
    public static BlueprintIoScreen forExport(BlueprintScreen parent, String code) {
        return new BlueprintIoScreen(parent, true, code, false);
    }

    /** Import; {@code canApply} false bei signierter (schreibgeschuetzter) Blaupause. */
    public static BlueprintIoScreen forImport(BlueprintScreen parent, boolean canApply) {
        return new BlueprintIoScreen(parent, false, "", canApply);
    }

    private BlueprintIoScreen(BlueprintScreen parent, boolean export, String code, boolean canApply) {
        super(Component.translatable(export ? "simplebuilding.blueprint.io.export_title" : "simplebuilding.blueprint.io.import_title"));
        this.parent = parent;
        this.export = export;
        this.code = code;
        this.canApply = canApply;
        this.format = export ? Format.CODE : null;
    }

    private float codeScale() {
        int guiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
        return guiScale >= 3 ? (guiScale - 1f) / guiScale : 1f;
    }

    private static Button button(Component label, Rect r, Button.OnPress press) {
        return Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build();
    }

    @Override
    protected void init() {
        layout = BlueprintIoLayout.of(width, height);
        Rect t = layout.text;
        if (export) {
            exported = BlueprintFormats.export(format, code);
            exportText = exported.text();
            area = new BlueprintCodeArea(font, t.x(), t.y(), t.w(), t.h(), codeScale(), true, exportText, v -> { },
                    Math.max(exportText.length(), 1));
            if (format == Format.CODE) {
                area.setHighlight(BlueprintCode.parseCached(exportText).styles(), List.of());
            }
        } else {
            String current = area != null ? area.getValue() : pendingText;
            area = new BlueprintCodeArea(font, t.x(), t.y(), t.w(), t.h(), codeScale(), false, current, this::onTextChanged,
                    BlueprintFormats.MAX_IMPORT_CHARS + 1);
            recheck();
        }
        addRenderableWidget(area);
        Rect f = layout.format;
        formatButton = addRenderableWidget(button(formatLabel(), f, b -> cycleFormat()));
        if (export) {
            primary = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.io.copy"), layout.button(0, 2), b -> copy()));
            addRenderableWidget(button(Component.translatable("gui.done"), layout.button(1, 2), b -> onClose()));
            primary.active = !exportText.isEmpty();
        } else {
            primary = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.io.paste"), layout.button(0, 3), b -> paste()));
            apply = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.io.apply"), layout.button(1, 3), b -> apply()));
            addRenderableWidget(button(Component.translatable("gui.cancel"), layout.button(2, 3), b -> onClose()));
            apply.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simplebuilding.blueprint.io.apply_tip")));
            updateApply();
        }
        setInitialFocus(area);
    }

    private Component formatLabel() {
        Component name = format == null ? Component.translatable("simplebuilding.blueprint.io.format.auto") : format.label();
        return Component.translatable("simplebuilding.blueprint.io.format", name);
    }

    private void cycleFormat() {
        if (export) {
            format = format == Format.CODE ? Format.COMMANDS : Format.CODE;
        } else if (format == null) {
            format = Format.CODE;
        } else if (format == Format.CODE) {
            format = Format.COMMANDS;
        } else {
            format = null;
        }
        if (!export) {
            pendingText = area.getValue();
        }
        rebuildWidgets();
    }

    private void onTextChanged(String value) {
        pendingText = value;
        dirty = true;
        lastEdit = Util.getMillis();
        imported = null;
        if (apply != null) {
            updateApply();
        }
    }

    private void recheck() {
        imported = BlueprintFormats.importText(area.getValue(), format);
        dirty = false;
    }

    private void updateApply() {
        apply.active = canApply && !dirty && imported != null && imported.ok();
    }

    private void copy() {
        minecraft.keyboardHandler.setClipboard(exportText);
        primary.setMessage(Component.translatable("simplebuilding.blueprint.io.copied"));
        copiedUntil = Util.getMillis() + COPIED_MS;
    }

    private void paste() {
        String clip = minecraft.keyboardHandler.getClipboard();
        area.replaceAll(clip == null ? "" : clip);
        setFocused(area);
    }

    private void apply() {
        if (dirty) {
            recheck();
        }
        if (!canApply || imported == null || !imported.ok()) {
            return;
        }
        parent.importCode(imported.code());
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        long now = Util.getMillis();
        if (!export && dirty && now - lastEdit > DEBOUNCE_MS) {
            recheck();
            updateApply();
        }
        if (copiedUntil != 0 && now > copiedUntil) {
            copiedUntil = 0;
            primary.setMessage(Component.translatable("simplebuilding.blueprint.io.copy"));
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect m = layout.modal;
        g.fill(m.x() - 1, m.y() - 1, m.right() + 1, m.bottom() + 1, BlueprintScreen.PAPER_EDGE);
        g.fill(m.x(), m.y(), m.right(), m.bottom(), BlueprintScreen.PAPER);
        g.fill(m.x() + 2, m.y() + 2, m.right() - 2, m.y() + 3, BlueprintScreen.PAPER_SHADE);
        g.fill(m.x() + 2, m.bottom() - 3, m.right() - 2, m.bottom() - 2, BlueprintScreen.PAPER_SHADE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        Rect t = layout.title;
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), t.w()), t.x(), t.y() + 4, BlueprintScreen.INK, false);
        g.fill(t.x(), layout.modal.y() + BlueprintIoLayout.TITLE_H, layout.modal.right() - BlueprintIoLayout.MARGIN,
                layout.modal.y() + BlueprintIoLayout.TITLE_H + 1, BlueprintScreen.PAPER_SHADE);
        drawStatus(g);
    }

    private void line(GuiGraphicsExtractor g, Rect r, Component text, int colour) {
        g.text(font, font.plainSubstrByWidth(text.getString(), r.w()), r.x(), r.y(), colour, false);
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        if (export) {
            BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(code.replace("\r", ""));
            if (exported != null && exported.tooLarge()) {
                line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.too_large", BlueprintFormats.MAX_EXPORT_LINES), BlueprintScreen.ERROR);
            } else if (exportText.isEmpty()) {
                line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.export_empty"), BlueprintScreen.INK_SOFT);
            } else if (!parsed.ok()) {
                line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.export_errors"), BlueprintScreen.ERROR);
            } else {
                line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.summary", parsed.model().size(),
                        parsed.model().sizeX(), parsed.model().sizeY(), parsed.model().sizeZ()), BlueprintScreen.OK);
            }
            return;
        }
        if (dirty || imported == null) {
            line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.checking"), BlueprintScreen.INK_SOFT);
            return;
        }
        if (imported.ok()) {
            line(g, layout.status1, Component.translatable("simplebuilding.blueprint.io.summary", imported.model().size(),
                    imported.model().sizeX(), imported.model().sizeY(), imported.model().sizeZ()), BlueprintScreen.OK);
            if (!canApply) {
                line(g, layout.status2, Component.translatable("simplebuilding.blueprint.io.read_only"), BlueprintScreen.ERROR);
            } else {
                line(g, layout.status2, Component.translatable("simplebuilding.blueprint.io.detected", imported.format().label()), BlueprintScreen.INK_SOFT);
            }
            return;
        }
        List<BlueprintFormats.Issue> issues = imported.issues();
        line(g, layout.status1, issueText(issues.get(0)), BlueprintScreen.ERROR);
        if (issues.size() > 1) {
            line(g, layout.status2, Component.translatable("simplebuilding.blueprint.io.more", issues.size() - 1), BlueprintScreen.INK_SOFT);
        }
    }

    private static Component issueText(BlueprintFormats.Issue issue) {
        return issue.line() > 0
                ? Component.translatable("simplebuilding.blueprint.editor.error_line", issue.line(), issue.message())
                : issue.message();
    }
}
