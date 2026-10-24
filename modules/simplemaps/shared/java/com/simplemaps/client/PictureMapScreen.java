package com.simplemaps.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplelib.api.image.PixelArt;
import com.simplemaps.PictureMaps;
import com.simplemaps.SimpleMaps;
import com.simplemaps.net.PictureMapPayload;
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
import org.jetbrains.annotations.Nullable;

/**
 * "Picture map" (AUD-6, opened by {@code /simplemaps picture}): a PNG from {@code config/simplemaps/images/}, the size in
 * maps (longer edge), dithering and a live preview in map colours; "Make maps" sends one {@link PictureMapPayload} per
 * map. The picture is converted here, the server only sees (and checks) the map colour bytes. Only PNG is read
 * ({@link NativeImage}, no AWT). The server's limits answer as chat messages, so the size list goes to 4 maps.
 */
public final class PictureMapScreen extends Screen {
    private static final int CONTROL_H = 20, CONTROL_W = 150, GAP = 4, MAX_SOURCE_SIDE = 4096;
    private static final int INK = 0xFFFFFFFF, SOFT = 0xFFC0C0C0, ERROR = 0xFFFF8080, OK = 0xFF90FF90;

    private final List<Path> files = new ArrayList<>();
    private int fileIndex, maps = 2;
    private PixelArt.Dither dither = PixelArt.Dither.FLOYD_STEINBERG;
    private @Nullable Path loadedFile;
    private int[] argb = new int[0];
    private int sourceW, sourceH;
    private boolean readError;
    private PictureMaps.@Nullable Result result;
    private Button make;

    public PictureMapScreen() {
        super(Component.translatable("screen.simplemaps.picture.title"));
    }

    private Path folder() {
        return minecraft.gameDirectory.toPath().resolve("config").resolve("simplemaps").resolve("images");
    }

    @Override
    protected void init() {
        if (files.isEmpty()) scan();
        int x = 20, y = 40;
        addRenderableWidget(Button.builder(fileLabel(), b -> {
            fileIndex = files.isEmpty() ? 0 : (fileIndex + 1) % files.size();
            b.setMessage(fileLabel());
            convert();
        }).bounds(x, y, CONTROL_W, CONTROL_H).build());
        y += CONTROL_H + GAP;
        addRenderableWidget(Button.builder(sizeLabel(), b -> {
            maps = maps % 4 + 1;
            b.setMessage(sizeLabel());
            convert();
        }).bounds(x, y, CONTROL_W, CONTROL_H).build());
        y += CONTROL_H + GAP;
        addRenderableWidget(Button.builder(ditherLabel(), b -> {
            dither = PixelArt.Dither.values()[(dither.ordinal() + 1) % PixelArt.Dither.values().length];
            b.setMessage(ditherLabel());
            convert();
        }).bounds(x, y, CONTROL_W, CONTROL_H).build());
        y += CONTROL_H + GAP;
        addRenderableWidget(Button.builder(Component.translatable("screen.simplemaps.picture.reload"), b -> {
            scan();
            loadedFile = null;
            rebuildWidgets();
        }).bounds(x, y, CONTROL_W, CONTROL_H).build());
        make = addRenderableWidget(Button.builder(Component.translatable("screen.simplemaps.picture.make"), b -> make())
                .bounds(x, height - 52, CONTROL_W, CONTROL_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, height - 28, CONTROL_W, CONTROL_H).build());
        convert();
    }

    private Component fileLabel() {
        return Component.translatable("screen.simplemaps.picture.file", files.isEmpty() ? "-" : files.get(fileIndex).getFileName().toString());
    }

    private Component sizeLabel() {
        return Component.translatable("screen.simplemaps.picture.size", maps);
    }

    private Component ditherLabel() {
        return Component.translatable("screen.simplemaps.picture.dither", Component.translatable("screen.simplemaps.picture.dither." + dither.name().toLowerCase(Locale.ROOT)));
    }

    private void scan() {
        Path previous = files.isEmpty() ? null : files.get(fileIndex);
        files.clear();
        try {
            Files.createDirectories(folder());
            try (Stream<Path> list = Files.list(folder())) {
                list.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).sorted().forEach(files::add);
            }
        } catch (IOException | RuntimeException ignored) {
            // no folder, no files: the status line says where they belong
        }
        fileIndex = Math.max(0, previous == null ? 0 : files.indexOf(previous));
    }

    private void load(Path file) {
        loadedFile = file;
        argb = new int[0];
        readError = false;
        try (InputStream in = Files.newInputStream(file); NativeImage image = NativeImage.read(in)) {
            sourceW = image.getWidth();
            sourceH = image.getHeight();
            if (sourceW > MAX_SOURCE_SIDE || sourceH > MAX_SOURCE_SIDE) throw new IOException("too large");
            argb = new int[sourceW * sourceH];
            for (int y = 0; y < sourceH; y++) for (int x = 0; x < sourceW; x++) argb[y * sourceW + x] = image.getPixel(x, y);
        } catch (IOException | RuntimeException e) {
            argb = new int[0];
            readError = true;
        }
    }

    private void convert() {
        result = null;
        if (!files.isEmpty()) {
            Path file = files.get(fileIndex);
            if (!file.equals(loadedFile)) load(file);
            if (argb.length > 0) result = PictureMaps.convert(argb, sourceW, sourceH, maps, dither);
        }
        if (make != null) make.active = result != null;
    }

    private void make() {
        if (result == null) return;
        for (int row = 0; row < result.rows(); row++) {
            for (int col = 0; col < result.cols(); col++) {
                SimpleMaps.toServer.accept(new PictureMapPayload(result.cols(), result.rows(), col, row, result.tile(col, row)));
            }
        }
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.text(font, getTitle(), 20, 20, INK, false);
        int left = 20 + CONTROL_W + 16, top = 40, boxW = width - left - 20, boxH = height - top - 40;
        if (result != null) {
            int w = result.cols() * PictureMaps.MAP, h = result.rows() * PictureMaps.MAP;
            // Whole pixels per preview cell, sampling every step-th map pixel so large walls stay cheap to draw.
            int step = Math.max(1, (int) Math.ceil(Math.max((double) w / boxW, (double) h / boxH)));
            int pw = w / step, ph = h / step, x0 = left + (boxW - pw) / 2, y0 = top + (boxH - ph) / 2;
            g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFF404040);
            g.fill(x0, y0, x0 + pw, y0 + ph, 0xFFC8B48C);
            byte[] colors = result.colors();
            for (int y = 0; y < ph; y++) {
                for (int x = 0; x < pw; x++) {
                    int rgb = PictureMaps.rgb(colors[y * step * w + x * step]);
                    if (rgb >= 0) g.fill(x0 + x, y0 + y, x0 + x + 1, y0 + y + 1, 0xFF000000 | rgb);
                }
            }
        }
        int sy = height - 52, sx = left;
        if (files.isEmpty()) {
            g.text(font, Component.translatable("screen.simplemaps.picture.no_files", "config/simplemaps/images"), sx, sy, ERROR, false);
        } else if (readError) {
            g.text(font, Component.translatable("screen.simplemaps.picture.error.read"), sx, sy, ERROR, false);
        } else if (result != null) {
            g.text(font, Component.translatable("screen.simplemaps.picture.summary", result.cols(), result.rows(), result.cols() * result.rows()), sx, sy, OK, false);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }
}
