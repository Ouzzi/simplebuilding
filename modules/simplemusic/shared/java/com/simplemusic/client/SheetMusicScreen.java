package com.simplemusic.client;

import com.simplelib.api.client.ui.UiCodeArea;
import com.simplemusic.SimpleMusic;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetMusicItem;
import com.simplemusic.sheet.SheetSounds;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The sheet music editor (owner decision N2 e1: text plus test listening; a grid editor comes later). The code field
 * is SimpleLib's {@link UiCodeArea} (moved there from the blueprint editor, N1). Problems are coloured and listed with
 * line and text; "Listen" plays the code locally; "Import" lists {@code config/simplemusic/sheets/} (.txt, .nbs),
 * "Copy" puts the code on the clipboard; closing saves, "Sign" saves it read-only with a title. The server checks every
 * save again. "Upload" (owner 21.10.) opens the {@link BurnScreen}: the chosen Ogg file becomes this sheet's track
 * instead of a composition; an upload sheet shows its track read-only.
 */
public class SheetMusicScreen extends Screen {
    /** Style colours by {@code NoteCode.STYLE_*}; each at least 4.5:1 on the field's paper (#F3EBD2). */
    public static final int[] PALETTE = {
            0xFF3B2F20, // text
            0xFF6B6850, // comment
            0xFF1F4E9A, // instrument
            0xFF256B29, // note
            0xFF7A3E9D, // keyword
            0xFFA0421D, // number
            0xFF6B5B45, // operator
            0xFFC62828, // error
            0xFF7A6355, // rest
    };
    /** Problem text on the dark screen background. */
    private static final int PROBLEM_INK = 0xFFFF8A80;
    private static final int INFO_INK = 0xFFE0E0E0;

    private final int hand;
    private final SheetContent content;
    private final com.simplemusic.disc.TrackRef track;
    private final boolean readOnly;
    private String code;
    private String title;
    private UiCodeArea area;
    private EditBox titleBox;
    private Button listen;
    private NoteCode.Result parsed;
    private boolean importing;
    private boolean saved;

    public SheetMusicScreen(int hand, SheetContent content, com.simplemusic.disc.TrackRef track) {
        super(Component.translatable("item.simplemusic.sheet_music"));
        this.hand = hand;
        this.content = content;
        this.track = track;
        this.readOnly = content.signed() || track != null;
        this.code = content.code();
        this.title = content.title();
        this.parsed = NoteCode.parse(code, SheetSounds.policy());
    }

    /** Opens the editor for the sheet in a hand (0 main, 1 off); called from the item through SimpleMusic.openSheet. */
    public static void open(int hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof SheetMusicItem) mc.gui.setScreen(new SheetMusicScreen(hand, SheetMusicItem.content(stack), SheetMusicItem.track(stack)));
    }

    @Override
    protected void init() {
        int margin = 12;
        int top = 30;
        int bottom = height - 54;
        float scale = minecraft.getWindow().getGuiScale() >= 4 ? 0.75F : minecraft.getWindow().getGuiScale() >= 3 ? 2F / 3F : 1F;
        if (importing) {
            initImport(margin, top);
            return;
        }
        area = new UiCodeArea(font, margin, top, width - margin * 2, bottom - top, scale, readOnly, code, this::onCode,
                NoteCode.MAX_CODE_LENGTH, PALETTE, Component.translatable("simplemusic.editor.code"));
        addRenderableWidget(area);
        titleBox = new EditBox(font, width - margin - 140, 8, 140, 16, Component.translatable("simplemusic.editor.title"));
        titleBox.setMaxLength(NoteCode.MAX_TITLE_LENGTH);
        titleBox.setValue(title);
        titleBox.setEditable(!readOnly);
        titleBox.setResponder(v -> title = v);
        titleBox.setHint(Component.translatable("simplemusic.editor.title"));
        addRenderableWidget(titleBox);
        int y = height - 26;
        List<Button> buttons = new ArrayList<>();
        listen = Button.builder(listenLabel(), b -> toggleListen()).width(80).build();
        if (track == null) buttons.add(listen);
        if (!readOnly) buttons.add(Button.builder(Component.translatable("simplemusic.editor.import"), b -> {
            importing = true;
            rebuildWidgets();
        }).width(80).build());
        buttons.add(Button.builder(Component.translatable("simplemusic.editor.copy"),
                b -> minecraft.keyboardHandler.setClipboard(code)).width(80).build());
        if (!readOnly) buttons.add(Button.builder(Component.translatable("simplemusic.editor.sign"), b -> sign()).width(80).build());
        if (!readOnly) buttons.add(Button.builder(Component.translatable("simplemusic.editor.upload"), b -> upload()).width(80).build());
        buttons.add(Button.builder(Component.translatable("gui.done"), b -> onClose()).width(80).build());
        int w = Math.min(80, (width - 24) / buttons.size() - 4);
        int total = buttons.size() * (w + 4) - 4;
        int x = (width - total) / 2;
        for (Button b : buttons) {
            b.setWidth(w);
            b.setPosition(x, y);
            x += w + 4;
            addRenderableWidget(b);
        }
        onCode(code);
        setInitialFocus(area);
    }

    private void initImport(int margin, int top) {
        List<Path> files = SheetFiles.list(SheetFiles.sheets(), ".txt", ".nbs");
        int y = top;
        for (Path file : files) {
            if (y > height - 60) break;
            addRenderableWidget(Button.builder(Component.literal(file.getFileName().toString()), b -> {
                String imported = SheetFiles.importCode(file);
                if (imported != null) code = imported;
                importing = false;
                rebuildWidgets();
            }).bounds(width / 2 - 120, y, 240, 18).build());
            y += 20;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            importing = false;
            rebuildWidgets();
        }).bounds(width / 2 - 40, height - 26, 80, 20).build());
    }

    private Component listenLabel() {
        return Component.translatable(SheetPreview.playing() ? "simplemusic.editor.stop" : "simplemusic.editor.listen");
    }

    private void toggleListen() {
        if (SheetPreview.playing()) SheetPreview.stop();
        else if (parsed.ok()) SheetPreview.play(parsed.song());
        listen.setMessage(listenLabel());
    }

    private void onCode(String value) {
        code = value;
        parsed = NoteCode.parse(value, SheetSounds.policy());
        List<UiCodeArea.Range> ranges = new ArrayList<>();
        for (NoteCode.Problem p : parsed.problems()) ranges.add(new UiCodeArea.Range(p.start(), p.end()));
        area.setProblems(parsed.styles(), ranges);
    }

    @Override
    public void tick() {
        super.tick();
        if (listen != null) listen.setMessage(listenLabel());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.text(font, importing ? Component.translatable("simplemusic.editor.import.title", SheetFiles.sheets().toString()) : getTitle(),
                12, 12, INFO_INK, true);
        if (importing) return;
        int y = height - 48;
        if (track != null) {
            g.text(font, Component.translatable("simplemusic.editor.track", track.title(), track.seconds() / 60,
                    String.format(java.util.Locale.ROOT, "%02d", track.seconds() % 60), track.uploader()), 12, y, INFO_INK, true);
            return;
        }
        if (!parsed.problems().isEmpty()) {
            NoteCode.Problem p = parsed.problems().get(0);
            Component message = Component.translatable("simplemusic.code.line", p.line() + 1,
                    Component.translatable("simplemusic.code.error." + p.key(), p.args().toArray()));
            g.text(font, message, 12, y, PROBLEM_INK, true);
            if (parsed.problems().size() > 1) {
                g.text(font, Component.translatable("simplemusic.code.more", parsed.problems().size() - 1), 12, y + 10, PROBLEM_INK, true);
            }
        } else {
            NoteCode.Song song = parsed.song();
            g.text(font, Component.translatable("simplemusic.editor.info", song.steps(), song.instruments().size(),
                    song.ticks() / 20 / 60, String.format(java.util.Locale.ROOT, "%02d", song.ticks() / 20 % 60)), 12, y, INFO_INK, true);
        }
    }

    private void sign() {
        if (title.isBlank() || code.isBlank() || !parsed.ok()) return;
        SimpleMusic.toServer.accept(new SheetEditPayload(hand, code, title.strip(), true));
        saved = true;
        minecraft.gui.setScreen(null);
    }

    /** Saves the draft, then opens the upload window for this sheet (the server puts the track on it). */
    private void upload() {
        save();
        minecraft.gui.setScreen(new BurnScreen(hand));
    }

    @Override
    public void onClose() {
        if (importing) {
            importing = false;
            rebuildWidgets();
            return;
        }
        save();
        super.onClose();
    }

    @Override
    public void removed() {
        SheetPreview.stop();
        save();
        super.removed();
    }

    private void save() {
        if (saved || readOnly) return;
        saved = true;
        if (code.equals(content.code()) && title.equals(content.title())) return;
        SimpleMusic.toServer.accept(new SheetEditPayload(hand, code, title.strip(), false));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
