package com.simplebuilding.client.blueprint;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;
import com.simplebuilding.blueprint.BlueprintIoLayout;
import com.simplebuilding.blueprint.BlueprintPicture;
import com.simplebuilding.config.ServerTuning;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Bild-Fenster der Blaupause (Plan docs/ai/PLAN-B-PICTURE-2026-10-19.md), aus dem Import-Fenster
 * ({@link BlueprintIoScreen}, Knopf "Bild ..."), im selben Kartenblatt-Stil und mit der Geometrie von
 * {@link BlueprintIoLayout}: links die Einstellungen (Datei aus {@code config/simplebuilding/images/}, Groesse,
 * Dithering, Wand/Boden, Bloecke alle/Inventar), rechts die Vorschau in Blockfarben, darunter Groesse, Bloecke,
 * Farben und Codelaenge. "Uebernehmen" setzt den fertigen Code in den Editor; das Bild selbst verlaesst den Client
 * nie, der Server sieht nur den Code ({@link BlueprintPicture#refusal}). Gelesen wird nur PNG ({@link NativeImage},
 * kein AWT).
 */
public class BlueprintPictureScreen extends Screen {
    private static final int CONTROL_H = 16, CONTROL_GAP = 2, CONTROL_W = 150;
    /** Groesstes Quellbild (Seite), groessere Dateien werden nicht gelesen. */
    private static final int MAX_SOURCE_SIDE = 4096;

    private final BlueprintScreen parent;
    private BlueprintIoLayout layout;
    private Button apply;
    private final List<Path> files = new ArrayList<>();
    private int fileIndex;
    private int side = 64;
    private BlueprintPicture.Dither dither = BlueprintPicture.Dither.FLOYD_STEINBERG;
    private BlueprintPicture.Facing facing = BlueprintPicture.Facing.WALL;
    private boolean inventoryOnly;
    private @Nullable Path loadedFile;
    private int[] argb = new int[0];
    private int sourceW, sourceH;
    private @Nullable Component readError;
    private @Nullable BlueprintPicture.Result result;

    public BlueprintPictureScreen(BlueprintScreen parent) {
        super(Component.translatable("simplebuilding.blueprint.picture.title"));
        this.parent = parent;
    }

    /** {@code <Spielordner>/config/simplebuilding/images}. */
    private Path folder() {
        return minecraft.gameDirectory.toPath().resolve("config").resolve("simplebuilding").resolve("images");
    }

    private int maxSide() {
        return Math.min(BlueprintPicture.MAX_SIDE, ServerTuning.pictureMaxSide());
    }

    @Override
    protected void init() {
        layout = BlueprintIoLayout.of(width, height);
        if (files.isEmpty()) {
            scan();
        }
        side = Math.min(side, maxSide());
        Rect t = layout.text;
        int x = t.x(), y = t.y(), w = Math.min(CONTROL_W, t.w() / 2 - 4);
        addRenderableWidget(control(fileLabel(), x, y, w, b -> {
            fileIndex = files.isEmpty() ? 0 : (fileIndex + 1) % files.size();
            refresh(b, fileLabel());
        }));
        y += CONTROL_H + CONTROL_GAP;
        addRenderableWidget(control(sizeLabel(), x, y, w, b -> {
            side = nextSide();
            refresh(b, sizeLabel());
        }));
        y += CONTROL_H + CONTROL_GAP;
        addRenderableWidget(control(ditherLabel(), x, y, w, b -> {
            dither = BlueprintPicture.Dither.values()[(dither.ordinal() + 1) % BlueprintPicture.Dither.values().length];
            refresh(b, ditherLabel());
        }));
        y += CONTROL_H + CONTROL_GAP;
        addRenderableWidget(control(facingLabel(), x, y, w, b -> {
            facing = facing == BlueprintPicture.Facing.WALL ? BlueprintPicture.Facing.FLOOR : BlueprintPicture.Facing.WALL;
            refresh(b, facingLabel());
        }));
        y += CONTROL_H + CONTROL_GAP;
        addRenderableWidget(control(paletteLabel(), x, y, w, b -> {
            inventoryOnly = !inventoryOnly;
            refresh(b, paletteLabel());
        }));
        addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.picture.reload"), layout.button(0, 3), b -> {
            scan();
            loadedFile = null;
            rebuildWidgets();
        }));
        apply = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.io.apply"), layout.button(1, 3), b -> apply()));
        addRenderableWidget(button(Component.translatable("gui.cancel"), layout.button(2, 3), b -> onClose()));
        convert();
    }

    private static Button control(Component label, int x, int y, int w, Button.OnPress press) {
        return Button.builder(label, press).bounds(x, y, w, CONTROL_H).build();
    }

    private static Button button(Component label, Rect r, Button.OnPress press) {
        return Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build();
    }

    private void refresh(Button button, Component label) {
        button.setMessage(label);
        convert();
    }

    private Component fileLabel() {
        String name = files.isEmpty() ? "-" : files.get(fileIndex).getFileName().toString();
        return Component.translatable("simplebuilding.blueprint.picture.file", name);
    }

    private Component sizeLabel() {
        return Component.translatable("simplebuilding.blueprint.picture.size", side);
    }

    private Component ditherLabel() {
        return Component.translatable("simplebuilding.blueprint.picture.dither", Component.translatable(dither.langKey()));
    }

    private Component facingLabel() {
        return Component.translatable("simplebuilding.blueprint.picture.facing", Component.translatable(facing.langKey()));
    }

    private Component paletteLabel() {
        return Component.translatable("simplebuilding.blueprint.picture.palette",
                Component.translatable("simplebuilding.blueprint.picture.palette." + (inventoryOnly ? "inventory" : "all")));
    }

    /** Naechste Groessen-Stufe bis zur Server-Grenze, danach wieder die kleinste. */
    private int nextSide() {
        int max = maxSide();
        for (int s : BlueprintPicture.SIDES) {
            if (s > side && s <= max) {
                return s;
            }
        }
        return BlueprintPicture.SIDES[0];
    }

    /** PNG-Dateien im Ordner, nach Namen; legt den Ordner an, damit man ihn findet. */
    private void scan() {
        Path previous = files.isEmpty() ? null : files.get(fileIndex);
        files.clear();
        Path dir = folder();
        try {
            Files.createDirectories(dir);
            try (Stream<Path> list = Files.list(dir)) {
                list.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted().forEach(files::add);
            }
        } catch (IOException | RuntimeException ignored) {
            // kein Ordner, keine Dateien: der Status sagt, wohin sie gehoeren
        }
        fileIndex = Math.max(0, previous == null ? 0 : files.indexOf(previous));
    }

    private void load(Path file) {
        loadedFile = file;
        argb = new int[0];
        readError = null;
        try (InputStream in = Files.newInputStream(file); NativeImage image = NativeImage.read(in)) {
            sourceW = image.getWidth();
            sourceH = image.getHeight();
            if (sourceW > MAX_SOURCE_SIDE || sourceH > MAX_SOURCE_SIDE) {
                throw new IOException("too large");
            }
            argb = new int[sourceW * sourceH];
            for (int y = 0; y < sourceH; y++) {
                for (int x = 0; x < sourceW; x++) {
                    argb[y * sourceW + x] = image.getPixel(x, y);
                }
            }
        } catch (IOException | RuntimeException e) {
            argb = new int[0];
            readError = Component.translatable("simplebuilding.blueprint.picture.error.read", file.getFileName().toString());
        }
    }

    private void convert() {
        result = null;
        if (files.isEmpty()) {
            updateApply();
            return;
        }
        Path file = files.get(fileIndex);
        if (!file.equals(loadedFile)) {
            load(file);
        }
        if (argb.length > 0) {
            String name = file.getFileName().toString().replaceFirst("(?i)\\.png$", "");
            result = BlueprintPicture.convert(argb, sourceW, sourceH, side, BlueprintPicture.palette(inventoryOnly ? this::carried : null),
                    dither, facing, name);
        }
        updateApply();
    }

    /** Liegt das Item im Inventar? Im Kreativmodus gilt alles als vorhanden. */
    private boolean carried(Item item) {
        var player = minecraft.player;
        if (player == null) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private void updateApply() {
        if (apply != null) {
            apply.active = ServerTuning.pictureImport() && result != null && result.ok();
        }
    }

    private void apply() {
        if (result == null || !result.ok() || !ServerTuning.pictureImport()) {
            return;
        }
        parent.importCode(result.code());
        minecraft.gui.setScreen(parent);
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
        g.text(font, font.plainSubstrByWidth(getTitle().getString(), layout.modal.w() - 2 * BlueprintIoLayout.MARGIN), t.x(), t.y() + 4,
                BlueprintScreen.INK, false);
        g.fill(t.x(), layout.modal.y() + BlueprintIoLayout.TITLE_H, layout.modal.right() - BlueprintIoLayout.MARGIN,
                layout.modal.y() + BlueprintIoLayout.TITLE_H + 1, BlueprintScreen.PAPER_SHADE);
        drawPreview(g);
        drawStatus(g);
    }

    /** Die Pixel in Blockfarben, ganzzahlig vergroessert, rechts neben den Einstellungen; gleiche Nachbarn als ein Streifen. */
    private void drawPreview(GuiGraphicsExtractor g) {
        if (result == null || result.width() == 0) {
            return;
        }
        Rect t = layout.text;
        int left = t.x() + Math.min(CONTROL_W, t.w() / 2 - 4) + 8;
        int boxW = t.right() - left, boxH = t.h();
        int w = result.width(), h = result.height();
        int cell = Math.max(1, Math.min(boxW / w, boxH / h));
        if (boxW < w || boxH < h) {
            return;
        }
        int x0 = left + (boxW - w * cell) / 2, y0 = t.y() + (boxH - h * cell) / 2;
        g.fill(x0 - 1, y0 - 1, x0 + w * cell + 1, y0 + h * cell + 1, BlueprintScreen.PAPER_EDGE);
        g.fill(x0, y0, x0 + w * cell, y0 + h * cell, BlueprintScreen.PAPER_SHADE);
        int[] indices = result.indices();
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                int index = indices[y * w + x];
                int run = 1;
                while (x + run < w && indices[y * w + x + run] == index) {
                    run++;
                }
                if (index >= 0) {
                    g.fill(x0 + x * cell, y0 + y * cell, x0 + (x + run) * cell, y0 + (y + 1) * cell, 0xFF000000 | result.palette().get(index).rgb());
                }
                x += run;
            }
        }
    }

    private void line(GuiGraphicsExtractor g, Rect r, Component text, int colour) {
        g.text(font, font.plainSubstrByWidth(text.getString(), r.w()), r.x(), r.y(), colour, false);
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        if (!ServerTuning.pictureImport()) {
            line(g, layout.status1, Component.translatable("simplebuilding.blueprint.picture.error.off"), BlueprintScreen.ERROR);
            return;
        }
        if (files.isEmpty()) {
            line(g, layout.status1, Component.translatable("simplebuilding.blueprint.picture.no_files", "config/simplebuilding/images"),
                    BlueprintScreen.ERROR);
            line(g, layout.status2, Component.translatable("simplebuilding.blueprint.io.picture_hint"), BlueprintScreen.INK_SOFT);
            return;
        }
        if (readError != null) {
            line(g, layout.status1, readError, BlueprintScreen.ERROR);
            return;
        }
        if (result == null) {
            return;
        }
        if (!result.ok()) {
            line(g, layout.status1, Component.translatable("simplebuilding.blueprint.picture.error." + result.error()), BlueprintScreen.ERROR);
            return;
        }
        line(g, layout.status1, Component.translatable("simplebuilding.blueprint.picture.summary", result.width(), result.height(),
                result.blocks(), result.colors()), BlueprintScreen.OK);
        line(g, layout.status2, Component.translatable("simplebuilding.blueprint.picture.code_length", result.code().length(),
                BlueprintCode.MAX_CODE_LENGTH), BlueprintScreen.INK_SOFT);
    }
}
