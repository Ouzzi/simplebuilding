package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights;
import com.simplebuilding.modules.simpletweaks.deed.DeedLayout;
import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.DeedText;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * The claim deed sheet: a rights-code editor (like the blueprint's code editor), a read-only view of what the
 * holder may do, or the small "claim here" confirmation. Geometry comes from {@link DeedLayout}. The screen only
 * previews: the code is checked again by the server on "Done" and any refusal comes back as error lines.
 */
public final class DeedScreen extends Screen {
    // N67: inks from SimpleLib's UiContrast - at least 4.5:1 on the inventory grey and on the paper (they were 3.0-4.4)
    private static final int PAPER = 0xFFE2D3AA, INK_OK = UiContrast.INK_OK, INK_ERROR = UiContrast.INK_ERROR, INK_SOFT = UiContrast.INK_SOFT;
    private static final int TEXT = 0xFF404040, ROW_ERROR = 0x40C62828, ROW_H = 9, PAD = 4;
    private static final long COPIED_MS = 2000, PENDING_MS = 5000;
    private static final String[] LETTER_KEYS = {"B", "D", "U", "C", "R", "A", "M"};

    private final DeedOpenPayload data;
    private final String original;
    private String text;
    private int revision;
    private DeedLayout layout;
    private MultiLineEditBox box;
    private Button done, copy, next;
    private boolean helpOpen, pending;
    private long copiedUntil, pendingUntil;
    private int chipOffset, shown;
    private ClaimCode.Parsed parsed;
    private DeedText.Status status;
    private List<DeedResultPayload.Err> serverErrors = List.of();
    private String shadowText = null;
    private int shadowWidth = -1;
    private Rows shadowField;

    /** Reproduces the wrapping of the field's rows so an error line can be marked and scrolled to. */
    private static final class Rows extends MultilineTextField {
        Rows(net.minecraft.client.gui.Font font, int width) { super(font, width); }
        int rowStart(int row) { return getLineView(row).beginIndex(); }
    }

    /** The map view this sheet was opened from (SP22-8: rights are the deed's second page); null = none. */
    private Screen parent;

    public DeedScreen(DeedOpenPayload data) { this(data, null); }

    public DeedScreen(DeedOpenPayload data, Screen parent) {
        super(Component.translatable(data.mode() == DeedOpenPayload.CLAIM ? "simpletweaks.deed.title.claim" : "simpletweaks.deed.title.owner", data.owner()));
        this.data = data;
        this.parent = parent;
        this.original = data.code();
        this.text = data.code();
        this.revision = data.revision();
        recheck();
    }

    private boolean editing() { return data.mode() == DeedOpenPayload.EDIT; }
    private boolean dirty() { return !text.equals(original); }
    private UiPalette palette() { return UiPalette.derived(PAPER); }

    private void recheck() {
        if (!editing()) return;
        List<String> known = new ArrayList<>(data.online());
        known.addAll(data.known());
        parsed = DeedText.preview(text, data.ownerId(), data.owner(), data.dimension(), known, data.maxPeople());
        status = DeedText.status(parsed);
    }

    private Button button(Component label, Rect r, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build());
    }

    @Override
    protected void init() {
        layout = editing() ? DeedLayout.editor(width, height) : DeedLayout.claim(width, height);
        if (data.mode() == DeedOpenPayload.CLAIM) {
            button(Component.translatable("simpletweaks.deed.claim"), layout.right(0, 2), b -> claimHere());
            button(CommonComponents.GUI_CANCEL, layout.right(1, 2), b -> onClose());
            return;
        }
        if (!editing()) {
            button(CommonComponents.GUI_DONE, layout.right(0, 1), b -> onClose());
            return;
        }
        Rect f = layout.field;
        box = MultiLineEditBox.builder().setX(f.x()).setY(f.y()).setTextColor(0xFF000000 | TEXT).setTextShadow(false)
                .setCursorColor(0xFF000000 | TEXT).setShowBackground(false)
                .build(font, f.w(), f.h(), Component.translatable("simpletweaks.deed.editor"));
        box.setCharacterLimit(ClaimCode.MAX_CHARS);
        box.setValue(text);
        box.setValueListener(v -> { text = v; serverErrors = List.of(); recheck(); updateDone(); });
        box.visible = !helpOpen;
        addRenderableWidget(box);
        button(Component.translatable("simpletweaks.deed.help"), layout.help(), b -> toggleHelp())
                .setTooltip(Tooltip.create(Component.translatable("simpletweaks.deed.help.tip")));
        copy = button(Component.translatable("simpletweaks.deed.copy"), layout.left(0), b -> copyCode());
        button(Component.translatable("simpletweaks.deed.paste"), layout.left(1), b -> pasteCode())
                .setTooltip(Tooltip.create(Component.translatable("simpletweaks.deed.paste.tip")));
        button(CommonComponents.GUI_CANCEL, layout.right(0, 2), b -> onClose());
        done = button(CommonComponents.GUI_DONE, layout.right(1, 2), b -> save());
        chips();
        updateDone();
        setInitialFocus(box);
    }

    private void chips() {
        List<String> names = data.online();
        if (names.isEmpty()) return;
        int labelW = font.width(Component.translatable("simpletweaks.deed.online")) + 2;
        int[] widths = new int[names.size()];
        for (int i = 0; i < widths.length; i++) widths[i] = font.width(names.get(i) + " +") + 10;
        if (chipOffset >= widths.length) chipOffset = 0;
        List<Rect> rects = layout.chips(labelW, widths, chipOffset);
        shown = rects.size();
        for (int i = 0; i < rects.size(); i++) {
            String name = names.get(chipOffset + i);
            button(Component.literal(name + " +"), rects.get(i), b -> addPerson(name))
                    .setTooltip(Tooltip.create(Component.translatable("simpletweaks.deed.add.tip", name)));
        }
        if (chipOffset > 0 || shown < widths.length - chipOffset) {
            Rect n = layout.nextChips();
            next = button(Component.literal(">"), n, b -> { chipOffset += Math.max(1, shown); if (chipOffset >= widths.length) chipOffset = 0; rebuildWidgets(); });
        }
    }

    private void addPerson(String name) {
        String changed = DeedText.addPerson(text, name, "U");
        if (!changed.equals(text)) { box.setValue(changed); }
        setFocused(box);
    }

    private void toggleHelp() {
        helpOpen = !helpOpen;
        box.visible = !helpOpen;
        if (!helpOpen) setFocused(box);
    }

    private void copyCode() {
        minecraft.keyboardHandler.setClipboard(text);
        copy.setMessage(Component.translatable("simpletweaks.deed.copied"));
        copiedUntil = Util.getMillis() + COPIED_MS;
    }

    private void pasteCode() {
        String clip = minecraft.keyboardHandler.getClipboard();
        if (clip == null) return;
        clip = clip.replace("\r", "");
        box.setValue(clip.length() > ClaimCode.MAX_CHARS ? clip.substring(0, ClaimCode.MAX_CHARS) : clip);
        setFocused(box);
    }

    private void updateDone() {
        if (done != null) done.active = !pending && status != null && status.ok();
    }

    private void save() {
        if (!dirty()) { onClose(); return; }
        if (pending || !status.ok()) return;
        pending = true;
        pendingUntil = Util.getMillis() + PENDING_MS;
        serverErrors = List.of();
        updateDone();
        DeedNet.toServer.accept(new DeedEditPayload(data.hand(), revision, text));
    }

    private void claimHere() {
        DeedNet.toServer.accept(new DeedClaimPayload(data.hand()));
        onClose();
    }

    /** The server's answer to a save. */
    public void onResult(DeedResultPayload result) {
        pending = false;
        // saved: the map view's snapshot is stale now, close the deed altogether
        if (result.ok()) { parent = null; onClose(); return; }
        serverErrors = result.errors();
        updateDone();
        int line = firstLine(serverErrors);
        if (line > 0) scrollToLine(line);
    }

    private static int firstLine(List<DeedResultPayload.Err> errors) {
        for (var e : errors) if (e.line() > 0) return e.line();
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        long now = Util.getMillis();
        if (copiedUntil != 0 && now > copiedUntil) { copiedUntil = 0; if (copy != null) copy.setMessage(Component.translatable("simpletweaks.deed.copy")); }
        if (pending && now > pendingUntil) { pending = false; updateDone(); }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Cancel / Done without changes go back to the map view it came from. */
    @Override
    public void onClose() {
        if (parent != null) minecraft.gui.setScreen(parent);
        else super.onClose();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect p = layout.panel;
        UiPalette palette = palette();
        UiBoxes.box(g, p.x(), p.y(), p.w(), p.h(), palette);
        Rect c = layout.code;
        UiBoxes.insetColored(g, c.x(), c.y(), c.w(), c.h(), palette, UiPalette.INVENTORY.fill());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int label = palette().label();
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), layout.title.w()), layout.title.x(), layout.title.y(), label, false);
        if (data.mode() != DeedOpenPayload.CLAIM) {
            Component land = Component.translatable("simpletweaks.deed.land", data.landChunks(), data.maxChunks());
            g.text(font, font.plainSubstrByWidth(land.getString(), layout.info.w()), layout.info.x(), layout.info.y(), INK_SOFT, false);
        }
        Rect c = layout.code;
        int w = c.w() - 2 * PAD;
        switch (data.mode()) {
            case DeedOpenPayload.CLAIM -> g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.claim.text", data.chunkX(), data.chunkZ(), Math.max(0, data.maxChunks() - data.landChunks())), c.x() + PAD, c.y() + PAD, w, TEXT, false);
            case DeedOpenPayload.READ -> drawRead(g, c, w);
            default -> {
                if (helpOpen) g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.help.text"), c.x() + PAD, c.y() + PAD, w, TEXT, false);
                else drawErrorRows(g);
                drawStatus(g);
                if (!data.online().isEmpty()) g.text(font, Component.translatable("simpletweaks.deed.online"), layout.chipRow.x(), layout.chipRow.y() + 5, label, false);
            }
        }
    }

    private void drawRead(GuiGraphicsExtractor g, Rect c, int w) {
        int y = c.y() + PAD;
        if (!data.note().isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simpletweaks.deed.note." + data.note()), c.x() + PAD, y, w, INK_ERROR, false);
            return;
        }
        Component rights = data.rights() == 0 ? Component.translatable("simpletweaks.deed.read.none") : Component.translatable("simpletweaks.deed.read.rights", rightsText(data.rights()));
        g.textWithWordWrap(font, rights, c.x() + PAD, y, w, TEXT, false);
    }

    private static Component rightsText(int rights) {
        var out = Component.empty();
        boolean first = true;
        for (int i = 0; i < ClaimRights.LETTERS.length(); i++) {
            if ((rights & (1 << i)) == 0) continue;
            if (!first) out.append(", ");
            out.append(Component.translatable("simpletweaks.deed.right." + LETTER_KEYS[i]));
            first = false;
        }
        return out;
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        Rect s = layout.status;
        Component line;
        int color;
        if (pending) { line = Component.translatable("simpletweaks.deed.saving"); color = INK_SOFT; }
        else if (!serverErrors.isEmpty()) { line = errorText(serverErrors.get(0).line(), serverErrors.get(0).key(), serverErrors.get(0).arg()); color = INK_ERROR; }
        else if (!status.ok()) { var e = parsed.errors().get(0); line = errorText(e.line(), e.key(), e.arg()); color = INK_ERROR; }
        else { line = Component.translatable("simpletweaks.deed.ok", status.people(), status.sections()); color = INK_OK; }
        g.text(font, font.plainSubstrByWidth(line.getString(), s.w()), s.x(), s.y() + 1, color, false);
    }

    static Component errorText(int line, String key, String arg) {
        if (key.equals("cooldown")) {
            // the server names ticks; the sheet says seconds
            try { arg = Long.toString(Math.max(1, (Long.parseLong(arg) + 19) / 20)); } catch (NumberFormatException e) { arg = "1"; }
        }
        Component reason = Component.translatable("simpletweaks.deed.error." + key, arg);
        return line > 0 ? Component.translatable("simpletweaks.deed.error_line", line, reason) : reason;
    }

    // Marks the first row of every error line (client preview and server answer) over the field.
    private void drawErrorRows(GuiGraphicsExtractor g) {
        var lines = new java.util.LinkedHashSet<Integer>();
        for (var e : serverErrors) if (e.line() > 0) lines.add(e.line());
        if (parsed != null) for (var e : parsed.errors()) if (e.line() > 0) lines.add(e.line());
        if (lines.isEmpty() || box == null) return;
        rewrap();
        Rect f = layout.field;
        g.enableScissor(f.x() + 1, f.y() + 1, f.right() - 1, f.bottom() - 1);
        for (int line : lines) {
            int row = rowOf(line);
            if (row < 0) continue;
            int y = f.y() + PAD + row * ROW_H - (int) box.scrollAmount();
            g.fill(f.x() + 1, y - 1, f.right() - 1, y + ROW_H - 1, ROW_ERROR);
        }
        g.disableScissor();
    }

    private void rewrap() {
        int w = layout.field.w() - 2 * PAD;
        if (shadowField == null || shadowWidth != w) { shadowField = new Rows(font, w); shadowWidth = w; shadowText = null; }
        if (!text.equals(shadowText)) { shadowField.setValue(text, true); shadowText = text; }
    }

    /** Row (wrapped line) in which source line {@code line} starts, or -1. */
    private int rowOf(int line) {
        int offset = DeedText.lineStart(text, line);
        if (offset < 0) return -1;
        int found = -1;
        for (int r = 0; r < shadowField.getLineCount(); r++) {
            if (rowStart(r) <= offset) found = r; else break;
        }
        return found;
    }

    private int rowStart(int row) { return shadowField.rowStart(row); }

    private void scrollToLine(int line) {
        if (box == null) return;
        rewrap();
        int row = rowOf(line);
        if (row >= 0) box.setScrollAmount(row * ROW_H);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (editing() && layout.status.contains(event.x(), event.y()) && !helpOpen) {
            int line = !serverErrors.isEmpty() ? firstLine(serverErrors) : status != null ? status.firstLine() : 0;
            if (line > 0) { scrollToLine(line); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
