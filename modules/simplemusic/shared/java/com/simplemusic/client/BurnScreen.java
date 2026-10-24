package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.UploadBeginPayload;
import com.simplemusic.net.UploadChunkPayload;
import com.simplemusic.net.UploadResultPayload;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.upload.OggCheck;
import com.simplemusic.upload.UploadSession;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Upload window of the sheet of music (upload concept §2.2, owner decision U5 o; opened from the sheet editor since the
 * disc press, owner 21.10.): lists the {@code .ogg} files of
 * {@code config/simplemusic/upload/}, checks the chosen one locally with the same {@link OggCheck} as the server
 * (format, mono, length, size), takes a title and sends the file in chunks, paced below the server's rate. The server
 * checks everything again and answers with {@link UploadResultPayload}.
 */
public class BurnScreen extends Screen {
    /**
     * Panel in SimpleLib's box style, jukebox brown with light text (style audit, owner 23.10. variant C); the sheet
     * editor uses the same palette and inks.
     */
    static final int PANEL_FACE = 0xFF6B4A35; // fill of PALETTE, named for tools/check_ui_contrast.py
    static final UiPalette PALETTE = UiPalette.derived(PANEL_FACE);
    static final int INK = 0xFFF2EEE8;         // = PALETTE.label(), 6.84:1 on the panel
    static final int PROBLEM_INK = 0xFFFFB8B0; // 4.79:1
    static final int OK_INK = 0xFFB8F0B6;      // 6.09:1
    private static final int PANEL_WIDTH = 260;
    private static BurnScreen current;

    private final int hand;
    private Path chosen;
    private byte[] data;
    private Component status = Component.translatable("simplemusic.burn.choose");
    private int statusInk = INK;
    private EditBox titleBox;
    private Button burn;
    private boolean sending;
    private int nextChunk = -1;
    private long startedMillis;
    private long bytesPerSecond = 128 * 1024;

    public BurnScreen(int hand) {
        super(Component.translatable("simplemusic.burn.title"));
        this.hand = hand;
    }

    public static void open(int hand) {
        Minecraft.getInstance().gui.setScreen(new BurnScreen(hand));
    }

    @Override
    protected void init() {
        current = this;
        List<Path> files = SheetFiles.list(SheetFiles.uploads(), ".ogg");
        int y = 40;
        for (Path file : files) {
            if (y > height - 90) break;
            addRenderableWidget(Button.builder(Component.literal(file.getFileName().toString()), b -> choose(file))
                    .bounds(width / 2 - 120, y, 240, 18).build());
            y += 20;
        }
        titleBox = new EditBox(font, width / 2 - 120, height - 76, 240, 18, Component.translatable("simplemusic.editor.title"));
        titleBox.setMaxLength(NoteCode.MAX_TITLE_LENGTH);
        titleBox.setHint(Component.translatable("simplemusic.editor.title"));
        addRenderableWidget(titleBox);
        burn = Button.builder(Component.translatable("simplemusic.burn.burn"), b -> start()).bounds(width / 2 - 122, height - 30, 120, 20).build();
        burn.active = false;
        addRenderableWidget(burn);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 2, height - 30, 120, 20).build());
    }

    private void choose(Path file) {
        if (sending) return;
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            data = null;
            status(Component.translatable("simplemusic.upload.error.unreadable"), PROBLEM_INK);
            return;
        }
        chosen = file;
        OggCheck.Result check = OggCheck.check(data, OggCheck.Limits.DEFAULT);
        if (!check.ok()) {
            status(Component.translatable("simplemusic.upload.error." + check.key(), check.args().toArray()), PROBLEM_INK);
            burn.active = false;
            return;
        }
        if (titleBox.getValue().isBlank()) {
            String name = file.getFileName().toString();
            titleBox.setValue(name.substring(0, Math.min(NoteCode.MAX_TITLE_LENGTH, name.length() - 4)));
        }
        status(Component.translatable("simplemusic.burn.ready", check.info().seconds() / 60,
                String.format(java.util.Locale.ROOT, "%02d", check.info().seconds() % 60), data.length / 1024), OK_INK);
        burn.active = true;
    }

    private void status(Component text, int ink) {
        status = text;
        statusInk = ink;
    }

    private void start() {
        if (data == null || titleBox.getValue().isBlank() || sending) return;
        sending = true;
        burn.active = false;
        status(Component.translatable("simplemusic.burn.sending", 0), INK);
        com.simplemusic.config.MusicClientConfig.own(UploadSession.sha256(data));
        SimpleMusic.toServer.accept(new UploadBeginPayload(hand, titleBox.getValue().strip(), data.length));
    }

    /** Server answers (network thread already handed over to the client thread). */
    public static void receive(UploadResultPayload result) {
        BurnScreen screen = current;
        if (screen == null || Minecraft.getInstance().gui.screen() != screen) return;
        screen.onResult(result);
    }

    private void onResult(UploadResultPayload result) {
        switch (result.key()) {
            case "next" -> {
                int index = Integer.parseInt(result.args().get(0));
                if (result.args().size() > 1) bytesPerSecond = Long.parseLong(result.args().get(1)) * 1024 * 9 / 10;
                if (index == 0) startedMillis = System.currentTimeMillis();
                nextChunk = index;
            }
            case "accepted", "pending" -> {
                sending = false;
                status(Component.translatable("simplemusic.upload." + result.key(), result.args().toArray()), OK_INK);
            }
            default -> {
                sending = false;
                nextChunk = -1;
                status(Component.translatable("simplemusic.upload.error." + result.key(), result.args().toArray()), PROBLEM_INK);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!sending || nextChunk < 0 || data == null) return;
        long from = (long) nextChunk * UploadSession.MAX_CHUNK;
        // pace: chunk n may leave n * 32 KiB / rate after the start
        long due = startedMillis + from * 1000 / Math.max(1, bytesPerSecond);
        if (System.currentTimeMillis() < due) return;
        int to = (int) Math.min(data.length, from + UploadSession.MAX_CHUNK);
        SimpleMusic.toServer.accept(new UploadChunkPayload(nextChunk, Arrays.copyOfRange(data, (int) from, to)));
        status(Component.translatable("simplemusic.burn.sending", (int) (100L * to / data.length)), INK);
        nextChunk = -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        UiBoxes.box(g, width / 2 - PANEL_WIDTH / 2, 4, PANEL_WIDTH, height - 8, PALETTE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int left = width / 2 - 120;
        int ink = ink(INK);
        g.text(font, getTitle(), width / 2 - font.width(getTitle()) / 2, 12, ink, false);
        Component folder = Component.translatable("simplemusic.burn.folder", SheetFiles.uploads().toString());
        g.text(font, folder, width / 2 - Math.min(font.width(folder), 240) / 2, 24, ink, false);
        g.text(font, status, left, height - 52, ink(statusInk), false);
        g.text(font, Component.translatable("simplemusic.burn.rights"), left, height - 42, ink, false);
        if (chosen != null) g.text(font, Component.literal(chosen.getFileName().toString()), left, height - 88, ink, false);
    }

    /** {@code color} at least 4.5:1 on the panel as currently toned (inventory brightness). */
    static int ink(int color) {
        return UiContrast.ensure(color, PALETTE.fill());
    }

    @Override
    public void removed() {
        if (current == this) current = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
