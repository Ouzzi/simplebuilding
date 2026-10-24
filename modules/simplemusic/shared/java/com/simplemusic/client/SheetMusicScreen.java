package com.simplemusic.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiCodeArea;
import com.simplelib.api.client.ui.UiPalette;
import com.simplemusic.SimpleMusic;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.NoteGrid;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The sheet music editor (owner decision N2 e1: text plus test listening; the grid followed with WB24-4). The code field
 * is SimpleLib's {@link UiCodeArea} (moved there from the blueprint editor, N1). Problems are coloured and listed with
 * line and text; "Listen" plays the code locally; "Import" lists {@code config/simplemusic/sheets/} (.txt, .nbs),
 * "Copy" puts the code on the clipboard; closing saves, "Sign" saves it read-only with a title. The server checks every
 * save again. "Upload" (owner 21.10.) opens the {@link BurnScreen}: the chosen Ogg file becomes this sheet's track
 * instead of a composition; an upload sheet shows its track read-only. Drawn in the upload window's brown box with light
 * text (owner 23.10., style audit variant C).
 *
 * <p>Grid (owner choice WB24-4: E2 A + E3 B): a composition sheet is split, the {@link NoteGrid} on the left and the code
 * on the right, live both ways. Choose the instrument with its button (click forward, shift-click back, or the wheel), a
 * left click on a cell sets the note or takes it away, a right click deletes it, the wheel over the grid scrolls by a
 * bar. A grid click writes the code anew (flat); typed code redraws the grid, code with problems leaves it standing.
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
    /** The upload window's box and inks (owner 23.10.: same style as the {@link BurnScreen}, jukebox brown, light text). */
    private static final UiPalette BOX = BurnScreen.PALETTE;
    /** Grid colours (preview aud7-music-editor.html): cells on the dark frame, notes in jukebox gold (>= 3:1 on a cell). */
    private static final int GRID_FRAME = 0xFF2A1C11, GRID_CELL = 0xFF523B27, GRID_BEAT = 0xFF614630,
            GRID_CELL_SHARP = 0xFF46321F, GRID_BEAT_SHARP = 0xFF533B29, GRID_NOTE = 0xFFE9B44C, GRID_GHOST = 0xFF9C7B57;
    private static final int GRID_ROWS = NoteGrid.NOTES;
    /** Instruments to choose: the note block ones, the heads, then the sounds the code names. */
    private static final List<NoteCode.Instrument> CHOICES = new ArrayList<>();

    static {
        NoteCode.NOTE_INSTRUMENTS.forEach((name, octave) -> CHOICES.add(new NoteCode.Instrument(NoteCode.Kind.NOTE_BLOCK, name, octave)));
        NoteCode.HEAD_INSTRUMENTS.forEach(name -> CHOICES.add(new NoteCode.Instrument(NoteCode.Kind.HEAD, name, 0)));
    }

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
    private NoteGrid grid = NoteGrid.empty();
    private NoteCode.Instrument instrument = CHOICES.get(0);
    private Button instrumentButton;
    /** Grid area in screen pixels (cells start at gridX + gridLabel), 0 width = no grid. */
    private int gridX, gridY, gridW, gridH, gridLabel, cellW, cellH, firstStep, codeLeft = 12;

    public SheetMusicScreen(int hand, SheetContent content, com.simplemusic.disc.TrackRef track) {
        super(Component.translatable("item.simplemusic.sheet_music"));
        this.hand = hand;
        this.content = content;
        this.track = track;
        this.readOnly = content.signed() || track != null;
        this.code = content.code();
        this.title = content.title();
        this.parsed = NoteCode.parse(code, SheetSounds.policy());
        if (parsed.ok()) {
            this.grid = NoteGrid.of(parsed.song());
            if (!grid.instruments().isEmpty()) this.instrument = grid.instruments().get(0);
        }
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
        gridW = 0;
        if (importing) {
            initImport(margin, top);
            return;
        }
        int codeX = margin;
        if (track == null && width >= 360) {
            // E3 B: grid left, code right
            gridX = margin;
            gridW = (width - margin * 2) * 11 / 20;
            codeX = gridX + gridW + 6;
            instrumentButton = Button.builder(instrumentLabel(), b -> cycleInstrument(minecraft.hasShiftDown() ? -1 : 1))
                    .bounds(gridX, top, Math.min(gridW, 150), 20)
                    .tooltip(Tooltip.create(Component.translatable("simplemusic.editor.grid.hint"))).build();
            addRenderableWidget(instrumentButton);
            gridY = top + 24;
            gridLabel = font.width("f#5") + 4;
            cellH = Math.max(3, (bottom - gridY - 2) / GRID_ROWS);
            gridH = cellH * GRID_ROWS + 2;
            cellW = Math.max(6, Math.min(12, (gridW - gridLabel - 2) / 16));
        }
        codeLeft = codeX;
        area = new UiCodeArea(font, codeX, top, width - margin - codeX, bottom - top, scale, readOnly, code, this::onCode,
                NoteCode.MAX_CODE_LENGTH, PALETTE, Component.translatable("simplemusic.editor.code"));
        addRenderableWidget(area);
        titleBox = new EditBox(font, width - margin - 140, 9, 140, 16, Component.translatable("simplemusic.editor.title"));
        titleBox.setMaxLength(NoteCode.MAX_TITLE_LENGTH);
        titleBox.setValue(title);
        titleBox.setEditable(!readOnly);
        titleBox.setResponder(v -> title = v);
        titleBox.setHint(Component.translatable("simplemusic.editor.title"));
        addRenderableWidget(titleBox);
        int y = height - 30;
        List<Button> buttons = new ArrayList<>();
        listen = Button.builder(listenLabel(), b -> toggleListen()).width(80).build();
        if (track == null) buttons.add(listen);
        if (!readOnly) buttons.add(Button.builder(Component.translatable("simplemusic.editor.import"), b -> {
            importing = true;
            rebuildWidgets();
        }).width(80).build());
        if (track == null) buttons.add(Button.builder(Component.translatable("simplemusic.editor.copy"),
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
        }).bounds(width / 2 - 40, height - 30, 80, 20).build());
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
        if (parsed.ok()) grid = NoteGrid.of(parsed.song()); // problems: the grid stays standing (E3 B)
    }

    // ---------------------------------------------------------------- grid (WB24-4)

    private Component instrumentLabel() {
        Component name = instrument.kind() == NoteCode.Kind.SOUND ? Component.literal(instrument.name())
                : Component.translatable("simplemusic.instrument." + instrument.name());
        return Component.translatable("simplemusic.editor.instrument", name);
    }

    private List<NoteCode.Instrument> choices() {
        List<NoteCode.Instrument> out = new ArrayList<>(CHOICES);
        for (NoteCode.Instrument i : grid.instruments()) if (!out.contains(i)) out.add(i);
        return out;
    }

    private void cycleInstrument(int by) {
        List<NoteCode.Instrument> list = choices();
        int i = Math.max(0, list.indexOf(instrument));
        instrument = list.get(Math.floorMod(i + by, list.size()));
        instrumentButton.setMessage(instrumentLabel());
    }

    private int visibleSteps() {
        return Math.max(1, (gridW - gridLabel - 2) / cellW);
    }

    /** Columns to scroll through: the song plus at least one empty bar to write on. */
    private int gridSteps() {
        return Math.max(16, (grid.length() + 4 + 3) / 4 * 4);
    }

    /** {step, note} under the mouse, or null. */
    private int[] cellAt(double mx, double my) {
        if (gridW == 0) return null;
        int cx = (int) Math.floor(mx) - gridX - gridLabel - 1, cy = (int) Math.floor(my) - gridY - 1;
        if (cx < 0 || cy < 0 || cx >= visibleSteps() * cellW || cy >= GRID_ROWS * cellH) return null;
        return new int[]{firstStep + cx / cellW, GRID_ROWS - 1 - cy / cellH};
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int[] cell = cellAt(event.x(), event.y());
        if (cell != null && !readOnly && (event.button() == 0 || event.button() == 1)) {
            boolean changed = event.button() == 0 ? grid.toggle(instrument, cell[0], cell[1]) != NoteGrid.Toggle.REFUSED
                    : grid.remove(instrument, cell[0], cell[1]);
            if (changed) area.replaceAll(grid.toCode());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (instrumentButton != null && gridW > 0 && instrumentButton.isMouseOver(mouseX, mouseY) && scrollY != 0) {
            cycleInstrument(scrollY > 0 ? -1 : 1);
            return true;
        }
        if (gridW > 0 && mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH && scrollY != 0) {
            firstStep = Math.max(0, Math.min(gridSteps() - visibleSteps(), firstStep + (scrollY > 0 ? -4 : 4)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawGrid(GuiGraphicsExtractor g) {
        firstStep = Math.max(0, Math.min(firstStep, gridSteps() - visibleSteps()));
        int visible = Math.min(visibleSteps(), gridSteps() - firstStep);
        int left = gridX + gridLabel;
        g.fill(left, gridY, left + visible * cellW + 2, gridY + gridH, GRID_FRAME);
        int ink = BurnScreen.ink(BurnScreen.INK);
        List<NoteCode.Instrument> others = grid.instruments();
        for (int row = 0; row < GRID_ROWS; row++) {
            int note = GRID_ROWS - 1 - row;
            String name = NoteCode.nameOf(note, instrument.octave());
            boolean sharp = name.contains("#");
            int y = gridY + 1 + row * cellH;
            if (name.startsWith("c") && !sharp) g.text(font, name, gridX, y + cellH / 2 - 4, ink, false);
            for (int c = 0; c < visible; c++) {
                int step = firstStep + c;
                int x = left + 1 + c * cellW;
                boolean beat = step % 4 == 0;
                int color = sharp ? beat ? GRID_BEAT_SHARP : GRID_CELL_SHARP : beat ? GRID_BEAT : GRID_CELL;
                if (grid.has(instrument, step, note)) {
                    color = GRID_NOTE;
                } else {
                    for (NoteCode.Instrument other : others) {
                        int n = note + 12 * (instrument.octave() - other.octave());
                        if (!other.equals(instrument) && other.tunable() && grid.has(other, step, n)) {
                            color = GRID_GHOST;
                            break;
                        }
                    }
                }
                g.fill(x, y, x + cellW - 1, y + cellH - (cellH > 3 ? 1 : 0), color);
            }
        }
        // first visible step, so the player knows where the window is
        g.text(font, Component.translatable("simplemusic.editor.grid.step", firstStep + 1),
                instrumentButton.getRight() + 6, instrumentButton.getY() + 6, ink, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (listen != null) listen.setMessage(listenLabel());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        UiBoxes.box(g, 4, 4, width - 8, height - 8, BOX);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int info = BurnScreen.ink(BurnScreen.INK), problem = BurnScreen.ink(BurnScreen.PROBLEM_INK);
        g.text(font, importing ? Component.translatable("simplemusic.editor.import.title", SheetFiles.sheets().toString()) : getTitle(),
                12, 12, info, false);
        if (importing) return;
        if (gridW > 0) drawGrid(g);
        int y = height - 48;
        if (track != null) {
            g.text(font, Component.translatable("simplemusic.editor.track", track.title(), track.seconds() / 60,
                    String.format(java.util.Locale.ROOT, "%02d", track.seconds() % 60), track.uploader()), codeLeft, y, info, false);
            return;
        }
        if (!parsed.problems().isEmpty()) {
            NoteCode.Problem p = parsed.problems().get(0);
            Component message = Component.translatable("simplemusic.code.line", p.line() + 1,
                    Component.translatable("simplemusic.code.error." + p.key(), p.args().toArray()));
            g.text(font, message, codeLeft, y, problem, false);
            if (parsed.problems().size() > 1) {
                g.text(font, Component.translatable("simplemusic.code.more", parsed.problems().size() - 1), codeLeft, y + 10, problem, false);
            }
        } else {
            NoteCode.Song song = parsed.song();
            g.text(font, Component.translatable("simplemusic.editor.info", song.steps(), song.instruments().size(),
                    song.ticks() / 20 / 60, String.format(java.util.Locale.ROOT, "%02d", song.ticks() / 20 % 60)), codeLeft, y, info, false);
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
