package com.simplebuilding.client.blueprint;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blueprint.BlueprintBlockSearch;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintEditorLayout;
import com.simplebuilding.blueprint.BlueprintEditorLayout.Rect;
import com.simplebuilding.blueprint.BlueprintExamples;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.blueprint.BlueprintViewControls;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.networking.BlueprintEditPayload;
import com.simplebuilding.platform.ClientNetworking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Der Blaupausen-Editor (Umbau Queue N23): ein Kartenblatt mit Ueberschrift und drei Bereichen,
 * Geometrie aus {@link BlueprintEditorLayout}.
 * <ul>
 *   <li><b>Materials</b>: Raster aus Icon + Zahl (groesste Menge zuerst, Name im Tooltip); darunter
 *       "X×Y×Z", darunter "= n Bloecke", darunter der noetige Baustab als Icon mit Fortschrittsbalken
 *       zur Stufengrenze.</li>
 *   <li><b>Code</b>: Syntax-Farben, Zeilennummern, unterstrichene Fehler; darunter der Status und die
 *       Zeichenzahl, rechtsbuendig der Buch-Knopf (Hilfe).</li>
 *   <li><b>Preview</b>: das Bauwerk in 3D (Ziehen dreht, Strg+Ziehen verschiebt, Mausrad zoomt,
 *       Reset-Icon oben rechts im Fenster); darunter Signieren und Fertig. Das Hilfe-Buch nimmt
 *       dieselbe Flaeche ein: Reiter <i>Blocks</i> (Standard; Textfeld + Einfuegen, beim Oeffnen
 *       fokussiert) und <i>Guide</i> (vollstaendige Anleitung, unten "Text kopieren").</li>
 * </ul>
 * Geoeffnet aus der Hand oder - Rechtsklick mit leerer Hand - fuer eine abgelegte Blaupause.
 * Speichern: jede Aenderung geht 1,5 s nach dem letzten Tastendruck und beim Schliessen (auch ueber
 * {@link #removed()}) per {@link BlueprintEditPayload} an den Server.
 */
public class BlueprintScreen extends Screen {
    private static final int PAPER = 0xFFEADFBF;
    private static final int PAPER_EDGE = 0xFF8B7355;
    private static final int PAPER_SHADE = 0xFFD8C99E;
    private static final int INK = 0xFF3B2F20;
    private static final int INK_SOFT = 0xFF7A6A50;
    private static final int VIEW_BG = 0xFF26384F;
    private static final int ERROR = 0xFFC62828;
    private static final int OK = 0xFF2E7D32;
    private static final int SELECT = 0xFF3A6EA5;
    /** Autospeichern so lange nach der letzten Aenderung. */
    public static final long AUTOSAVE_MS = 1500;
    private static final int[] TIER_COLOURS = {0xFFB87333, 0xFFA8A8A8, 0xFFE0B82E, 0xFF4FC3C7, 0xFF5A4A4A, 0xFF7B3FA0};
    /** Groesster Fall der Materials-Fusszeilen: so breit wird die Spalte. */
    private static final String WIDEST_DIMS = "256×256×256";
    private static final int WIDEST_BLOCKS = 4_194_304;

    /**
     * Die Anleitung im Reiter Guide, in Lesereihenfolge: {@code h<n>} ist eine Ueberschrift
     * ({@code simplebuilding.blueprint.help.guide.head.<n>}), eine Zahl ein Absatz
     * ({@code simplebuilding.blueprint.help.guide.<n>}).
     */
    public static final String[] GUIDE = {
            "h1", "1",
            "h2", "2", "3", "4",
            "h3", "5", "6", "7", "9", "8",
            "h4", "11", "12",
            "h5", "13",
            "h6", "10", "14", "15", "16", "17", "18"};

    private final int slot;
    private final @Nullable BlockPos placedPos;
    private final BlueprintContent original;
    private final boolean readOnly;
    private String code;
    private String lastSent;

    private BlueprintEditorLayout layout;
    private BlueprintCodeArea codeArea;
    private EditBox insertBox;
    private EditBox titleBox;
    private Button doneButton;
    private Button signButton;
    private Button confirmSignButton;
    private Button cancelSignButton;
    private Button insertButton;
    private Button exampleButton;
    // Hilfe-Reiter und Kopieren als Pixel-Symbole mit Namen im Tooltip (Queue N29).
    private IconButton copyButton;
    private IconButton helpButton;
    private IconButton resetViewButton;
    private IconButton guideTab;
    private IconButton blocksTab;
    private boolean signing;
    private boolean helpOpen;
    /** Reiter der Hilfe: true = Blocks (Standard), false = Guide. */
    private boolean helpBlocks = true;
    private long copiedUntil;

    private BlueprintCode.ParseResult parsed;
    private List<BlueprintMaterials.Entry> materials = List.of();
    private boolean dirty;
    private long lastEdit;

    private List<Block> helpResults = List.of();
    private @Nullable Block selected;
    private double helpScroll;

    // Von BlueprintViewClientTest per Reflexion gelesen: Namen nicht aendern.
    private int viewX, viewW, bodyY, bodyH;
    private double listScroll;
    private final BlueprintViewControls controls = new BlueprintViewControls();
    private boolean draggingView;
    /** Ob der laufende Ziehvorgang Verschieben (Strg beim Tastendruck) oder Drehen ist. */
    private boolean draggingPan;

    /** Die Blaupause in der Hand. */
    public BlueprintScreen(Player player, InteractionHand hand) {
        this(player.getItemInHand(hand), hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND, null);
    }

    /** Die abgelegte Blaupause an {@code pos} (Rechtsklick mit leerer Hand). */
    public BlueprintScreen(ItemStack stack, BlockPos pos) {
        this(stack, -1, pos);
    }

    private BlueprintScreen(ItemStack stack, int slot, @Nullable BlockPos placedPos) {
        super(Component.translatable(itemKey(stack)));
        this.itemName = Component.translatable(itemKey(stack));
        this.slot = slot;
        this.placedPos = placedPos;
        this.original = BlueprintItem.content(stack);
        this.readOnly = original.signed();
        this.code = original.code();
        this.lastSent = code;
        this.parsed = BlueprintCode.parse(code);
        this.materials = BlueprintMaterials.list(parsed.model());
    }

    /** Name der Blaupause im Kopf: normale oder Kreativ-Blaupause (Queue N29). */
    private final Component itemName;

    private static String itemKey(ItemStack stack) {
        return BlueprintItem.isCreative(stack) ? "item.simplebuilding.creative_blueprint" : "item.simplebuilding.blueprint";
    }

    /** Schriftgroesse im Code-Feld: ganzzahlige Pixel, bei grossem GUI-Massstab kleiner. */
    private float codeScale() {
        int guiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
        return guiScale >= 3 ? (guiScale - 1f) / guiScale : 1f;
    }

    private static String displayName(Block block) {
        return block.getName().getString();
    }

    private static Button button(Component label, Rect r, Button.OnPress press) {
        return Button.builder(label, press).bounds(r.x(), r.y(), r.w(), r.h()).build();
    }

    @Override
    protected void init() {
        int statsW = Math.max(font.width(WIDEST_DIMS), font.width(Component.translatable("simplebuilding.blueprint.editor.blocks_total", WIDEST_BLOCKS)));
        layout = BlueprintEditorLayout.of(width, height, statsW + 2);
        viewX = layout.view.x();
        viewW = layout.view.w();
        bodyY = layout.view.y();
        bodyH = layout.view.h();

        String current = codeArea != null ? codeArea.getValue() : code;
        Rect c = layout.code;
        codeArea = new BlueprintCodeArea(font, c.x(), c.y(), c.w(), c.h(), codeScale(), readOnly, current, this::onCodeChanged);
        codeArea.setHighlight(parsed.styles(), parsed.problems());
        addRenderableWidget(codeArea);

        // Hilfe-Buch, Reiter Blocks: das Textfeld ist zugleich Suche und Einfuege-Feld (erstes EditBox-Kind).
        String query = insertBox != null ? insertBox.getValue() : "";
        Rect f = layout.insertField;
        insertBox = new EditBox(font, f.x(), f.y(), f.w(), f.h(), Component.translatable("simplebuilding.blueprint.editor.insert_search"));
        insertBox.setHint(Component.translatable("simplebuilding.blueprint.editor.insert_hint"));
        insertBox.setValue(query);
        insertBox.setResponder(q -> {
            helpResults = BlueprintBlockSearch.search(q, BlueprintScreen::displayName, 400);
            helpScroll = 0;
            if (selected != null && !helpResults.contains(selected)) {
                selected = null;
            }
            updateButtons();
        });
        helpResults = BlueprintBlockSearch.search(query, BlueprintScreen::displayName, 400);
        addRenderableWidget(insertBox);
        insertButton = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.editor.insert"), layout.insertButton, b -> insertTarget()));

        helpButton = addRenderableWidget(new IconButton(layout.book, Component.translatable("simplebuilding.blueprint.help.title"),
                Component.translatable("simplebuilding.blueprint.help.open"), null, this::toggleHelp));
        resetViewButton = addRenderableWidget(new IconButton(layout.reset, Component.translatable("simplebuilding.blueprint.editor.view_reset"),
                Component.translatable("simplebuilding.blueprint.editor.view_reset_tip"), RESET_ICON, controls::reset));
        exampleButton = addRenderableWidget(button(Component.translatable("simplebuilding.blueprint.editor.example"), layout.example, b -> insertExample()));
        Component blocksName = Component.translatable("simplebuilding.blueprint.help.blocks_tab");
        Component guideName = Component.translatable("simplebuilding.blueprint.help.guide_tab");
        blocksTab = addRenderableWidget(new IconButton(layout.blocksTab, blocksName, blocksName, BLOCKS_ICON, () -> showTab(true)));
        guideTab = addRenderableWidget(new IconButton(layout.guideTab, guideName, guideName, GUIDE_ICON, () -> showTab(false)));
        copyButton = addRenderableWidget(new IconButton(layout.copy, Component.translatable("simplebuilding.blueprint.help.copy"),
                copyTip(), COPY_ICON, this::copyGuide));

        doneButton = addRenderableWidget(button(Component.translatable("gui.done"), readOnly ? layout.doneWide : layout.done, b -> onClose()));
        signButton = addRenderableWidget(button(Component.translatable("book.signButton"), layout.sign, b -> {
            signing = true;
            updateButtons();
            setFocused(titleBox);
        }));
        Rect t = layout.titleBox;
        String title = titleBox != null ? titleBox.getValue() : "";
        titleBox = new EditBox(font, t.x(), t.y(), t.w(), t.h(), Component.translatable("simplebuilding.blueprint.editor.title"));
        titleBox.setMaxLength(BlueprintCode.MAX_TITLE_LENGTH);
        titleBox.setHint(Component.translatable("simplebuilding.blueprint.editor.title_hint"));
        titleBox.setValue(title);
        titleBox.setResponder(v -> updateButtons());
        addRenderableWidget(titleBox);
        confirmSignButton = addRenderableWidget(button(Component.translatable("book.finalizeButton"), layout.confirm, b -> sign()));
        cancelSignButton = addRenderableWidget(button(Component.translatable("gui.cancel"), layout.cancel, b -> {
            signing = false;
            updateButtons();
            setFocused(codeArea);
        }));
        updateButtons();
        if (signing) {
            setFocused(titleBox);
        } else if (helpOpen && helpBlocks) {
            setInitialFocus(insertBox);
        } else {
            setInitialFocus(codeArea);
        }
    }

    private void updateButtons() {
        boolean valid = parsed.ok() && !parsed.model().isEmpty();
        doneButton.visible = !signing;
        signButton.visible = !signing && !readOnly;
        signButton.active = valid;
        titleBox.visible = signing;
        confirmSignButton.visible = signing;
        cancelSignButton.visible = signing;
        confirmSignButton.active = !titleBox.getValue().isBlank() && valid;
        boolean blocks = helpOpen && helpBlocks;
        insertBox.visible = blocks;
        insertButton.visible = blocks && !readOnly;
        Block target = target();
        insertButton.active = target != null;
        insertButton.setTooltip(target == null ? null : Tooltip.create(
                Component.translatable("simplebuilding.blueprint.editor.insert_tip", target.getName(), BlueprintBlockSearch.codeName(target))));
        copyButton.visible = helpOpen && !helpBlocks;
        exampleButton.visible = !readOnly && !helpOpen && code.isBlank();
        resetViewButton.visible = !helpOpen && !parsed.model().isEmpty();
        blocksTab.visible = helpOpen;
        guideTab.visible = helpOpen;
        blocksTab.active = !helpBlocks;
        guideTab.active = helpBlocks;
    }

    /**
     * Buch-Knopf: oeffnet die Hilfe im Reiter Blocks mit fokussiertem Einfuege-Feld ("Insert
     * vorausgewaehlt") oder schliesst sie wieder. Oeffentlich fuer den Client-Test.
     */
    public void toggleHelp() {
        helpOpen = !helpOpen;
        draggingView = false;
        if (helpOpen) {
            helpBlocks = true;
            helpScroll = 0;
            updateButtons();
            setFocused(insertBox);
        } else {
            updateButtons();
            setFocused(codeArea);
        }
    }

    /** Reiter der Hilfe waehlen (true = Blocks); oeffentlich fuer den Client-Test. */
    public void showTab(boolean blocks) {
        helpBlocks = blocks;
        helpScroll = 0;
        updateButtons();
        setFocused(blocks ? insertBox : null);
    }

    private void onCodeChanged(String value) {
        code = value;
        dirty = true;
        lastEdit = Util.getMillis();
    }

    private void reparse() {
        parsed = BlueprintCode.parse(code);
        materials = BlueprintMaterials.list(parsed.model());
        codeArea.setHighlight(parsed.styles(), parsed.problems());
        dirty = false;
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        long now = Util.getMillis();
        if (dirty && now - lastEdit > 150) {
            reparse();
        }
        // Entprelltes Autospeichern: ein Abbruch ohne sauberes Schliessen kostet hoechstens die letzten Sekunden.
        if (!readOnly && !code.equals(lastSent) && now - lastEdit > AUTOSAVE_MS) {
            save();
        }
        if (copiedUntil != 0 && now > copiedUntil) {
            copiedUntil = 0;
            copyButton.setBitmap(COPY_ICON);
            copyButton.setTooltip(Tooltip.create(copyTip()));
        }
    }

    private BlueprintEditPayload payload(boolean sign, String title) {
        return placedPos != null ? BlueprintEditPayload.placed(placedPos, code, sign, title) : new BlueprintEditPayload(slot, code, sign, title);
    }

    private void save() {
        if (!readOnly && !code.equals(lastSent)) {
            ClientNetworking.send(payload(false, ""));
            lastSent = code;
        }
    }

    /** Der Block, den "Einfuegen" setzt: der gewaehlte, sonst der erste Treffer einer Suche. */
    private @Nullable Block target() {
        if (selected != null) {
            return selected;
        }
        return insertBox != null && !insertBox.getValue().isBlank() && !helpResults.isEmpty() ? helpResults.get(0) : null;
    }

    private void insertTarget() {
        Block target = target();
        if (target != null && !readOnly) {
            codeArea.insert(BlueprintBlockSearch.codeName(target));
        }
    }

    private void insertExample() {
        if (readOnly || !code.isBlank() || minecraft.player == null || minecraft.level == null) {
            return;
        }
        String group = BlueprintExamples.groupAt(minecraft.level, minecraft.player.blockPosition());
        String name = BlueprintExamples.pick(group, n -> minecraft.level.getRandom().nextInt(n));
        codeArea.replaceAll(BlueprintExamples.code(name));
        setFocused(codeArea);
    }

    /** Die ganze Anleitung als Text: Ueberschriften mit "## ", Absaetze durch Leerzeilen getrennt. */
    public static String guideText() {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(Component.translatable("simplebuilding.blueprint.help.guide.title").getString()).append("\n\n");
        for (String part : GUIDE) {
            if (part.startsWith("h")) {
                out.append("## ").append(Component.translatable("simplebuilding.blueprint.help.guide.head." + part.substring(1)).getString());
            } else {
                out.append(Component.translatable("simplebuilding.blueprint.help.guide." + part).getString());
            }
            out.append("\n\n");
        }
        return out.toString().strip() + "\n";
    }

    /** Tooltip des Kopier-Symbols: Name, darunter was es tut. */
    private static Component copyTip() {
        return Component.translatable("simplebuilding.blueprint.help.copy").withStyle(ChatFormatting.BOLD)
                .append(Component.literal("\n")).append(Component.translatable("simplebuilding.blueprint.help.copy_tip").withStyle(s -> s.withBold(false)));
    }

    private void copyGuide() {
        minecraft.keyboardHandler.setClipboard(guideText());
        copyButton.setBitmap(CHECK_ICON);
        copyButton.setTooltip(Tooltip.create(Component.translatable("simplebuilding.blueprint.help.copied")));
        copiedUntil = Util.getMillis() + 2000;
    }

    private void sign() {
        if (dirty) {
            reparse();
        }
        if (!parsed.ok() || titleBox.getValue().isBlank()) {
            return;
        }
        ClientNetworking.send(payload(true, titleBox.getValue().strip()));
        lastSent = code;
        minecraft.gui.setScreen(null);
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    /** Auch beim Verlassen der Welt, beim Rauswurf oder wenn ein anderer Bildschirm uebernimmt. */
    @Override
    public void removed() {
        save();
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (insertBox.isFocused() && insertBox.visible
                && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
            insertTarget();
            return true;
        }
        boolean typing = codeArea.isFocused() || insertBox.isFocused() || titleBox.isFocused();
        if (!typing && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================================
    // ZEICHNEN
    // =====================================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Rect p = layout.panel;
        g.fill(p.x() - 1, p.y() - 1, p.right() + 1, p.bottom() + 1, PAPER_EDGE);
        g.fill(p.x(), p.y(), p.right(), p.bottom(), PAPER);
        g.fill(p.x() + 2, p.y() + 2, p.right() - 2, p.y() + 3, PAPER_SHADE);
        g.fill(p.x() + 2, p.bottom() - 3, p.right() - 2, p.bottom() - 2, PAPER_SHADE);
        // Hintergrund der Vorschau vor den Knoepfen, damit die Knoepfe darauf liegen
        Rect v = layout.view;
        g.fill(v.x(), v.y(), v.right(), v.bottom(), PAPER_EDGE);
        g.fill(v.x() + 1, v.y() + 1, v.right() - 1, v.bottom() - 1, helpOpen ? PAPER_SHADE : VIEW_BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (helpOpen) {
            drawHelp(g, mouseX, mouseY);
        } else {
            drawView(g);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        drawHeader(g);
        drawMaterials(g, mouseX, mouseY);
        drawStats(g, mouseX, mouseY);
        drawStatus(g);
    }

    /** Text, der in {@code w} passt: wird bei Bedarf kleiner gezeichnet statt abgeschnitten. */
    private void fitted(GuiGraphicsExtractor g, Component text, int x, int y, int w, int colour) {
        float scale = BlueprintEditorLayout.fitScale(font.width(text), w);
        if (scale >= 1f) {
            g.text(font, text, x, y, colour, false);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y + (font.lineHeight * (1f - scale)) / 2f);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, colour, false);
        g.pose().popMatrix();
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        MutableComponent title = Component.empty();
        if (readOnly && !original.title().isBlank()) {
            title.append(Component.literal(original.title()).withStyle(ChatFormatting.BOLD));
            if (!original.author().isBlank()) {
                title.append(Component.literal("  ")).append(Component.translatable("book.byAuthor", original.author()));
            }
        } else {
            title.append(itemName.copy().withStyle(ChatFormatting.BOLD));
        }
        Rect p = layout.panel;
        fitted(g, title, p.x() + 8, p.y() + 7, p.w() - 16, INK);
        g.fill(p.x() + 8, p.y() + 18, p.right() - 8, p.y() + 19, PAPER_SHADE);
        int labelY = bodyY - font.lineHeight - 1;
        g.text(font, Component.translatable("simplebuilding.blueprint.editor.materials"), layout.list.x(), labelY, INK_SOFT, false);
        g.text(font, Component.translatable("simplebuilding.blueprint.editor.code"), layout.code.x(), labelY, INK_SOFT, false);
        g.text(font, Component.translatable(helpOpen ? "simplebuilding.blueprint.help.title" : "simplebuilding.blueprint.editor.view"),
                viewX, labelY, INK_SOFT, false);
    }

    /** Materialraster: je Zelle Icon + Menge; Name, genaue Zahl und Stapel im Tooltip. */
    private void drawMaterials(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Rect l = layout.list;
        g.fill(l.x(), l.y(), l.right(), l.bottom(), PAPER_SHADE);
        if (materials.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.no_materials"), l.x() + 3, l.y() + 3, l.w() - 6, INK_SOFT, false);
            return;
        }
        int cols = layout.materialColumns();
        int cellW = l.w() / cols;
        int rows = (materials.size() + cols - 1) / cols;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, rows * BlueprintEditorLayout.CELL_H - l.h() + 2)));
        g.enableScissor(l.x(), l.y(), l.right(), l.bottom());
        int y0 = l.y() + 1 - (int) listScroll;
        Component hovered = null;
        for (int i = 0; i < materials.size(); i++) {
            int x = l.x() + (i % cols) * cellW;
            int y = y0 + (i / cols) * BlueprintEditorLayout.CELL_H;
            if (y + BlueprintEditorLayout.CELL_H < l.y() || y > l.bottom()) {
                continue;
            }
            BlueprintMaterials.Entry e = materials.get(i);
            if (e.block() == null) {
                g.item(new ItemStack(e.item()), x + 2, y);
            } else {
                g.fill(x + 3, y + 1, x + 17, y + 15, 0x40C62828);
            }
            g.text(font, BlueprintEditorLayout.compactCount(e.count()), x + 20, y + 4, e.block() != null ? ERROR : INK, false);
            if (mouseX >= x && mouseX < x + cellW && mouseY >= Math.max(y, l.y()) && mouseY < Math.min(y + BlueprintEditorLayout.CELL_H, l.bottom())) {
                Component name = e.block() != null ? e.block().getName() : new ItemStack(e.item()).getHoverName();
                MutableComponent tip = Component.literal(e.count() + "× ").append(name);
                if (e.block() == null && e.count() >= 64) {
                    tip.append(Component.literal(" (" + (e.count() / 64) + "×64 + " + (e.count() % 64) + ")").withStyle(ChatFormatting.GRAY));
                }
                if (e.block() != null) {
                    tip.append(Component.translatable("simplebuilding.blueprint.materials.creative_only"));
                }
                hovered = tip;
            }
        }
        g.disableScissor();
        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    private static Item wandFor(int tier) {
        return switch (tier) {
            case 0 -> ModItems.COPPER_BUILDING_WAND;
            case 1 -> ModItems.IRON_BUILDING_WAND;
            case 2 -> ModItems.GOLD_BUILDING_WAND;
            case 3 -> ModItems.DIAMOND_BUILDING_WAND;
            case 4 -> ModItems.NETHERITE_BUILDING_WAND;
            default -> ModItems.ENDERITE_BUILDING_WAND;
        };
    }

    /** Unter dem Raster: "X×Y×Z", darunter "= n Bloecke", darunter Stab-Icon + Balken zur Stufengrenze. */
    private void drawStats(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        BlueprintModel model = parsed.model();
        Rect dims = layout.dimsLine;
        if (model.isEmpty()) {
            fitted(g, Component.translatable("simplebuilding.blueprint.editor.size_empty"), dims.x(), dims.y(), dims.w(), INK_SOFT);
            return;
        }
        fitted(g, Component.literal(model.sizeX() + "×" + model.sizeY() + "×" + model.sizeZ()), dims.x(), dims.y(), dims.w(), INK);
        Rect blocks = layout.blocksLine;
        fitted(g, Component.translatable("simplebuilding.blueprint.editor.blocks_total", model.size()), blocks.x(), blocks.y(), blocks.w(), INK_SOFT);

        int edge = model.maxEdge();
        int tier = BlueprintTiers.tierIndexFor(edge);
        Rect icon = layout.wandIcon;
        Rect bar = layout.wandBar;
        if (tier < 0) {
            g.item(new ItemStack(Items.BARRIER), icon.x(), icon.y());
            fitted(g, Component.translatable("simplebuilding.blueprint.editor.too_big", BlueprintCode.GRID), bar.x(), icon.y() + 4, bar.w(), ERROR);
            return;
        }
        g.item(new ItemStack(wandFor(tier)), icon.x(), icon.y());
        g.fill(bar.x(), bar.y(), bar.right(), bar.bottom(), PAPER_EDGE);
        g.fill(bar.x() + 1, bar.y() + 1, bar.right() - 1, bar.bottom() - 1, PAPER_SHADE);
        int filled = (int) ((bar.w() - 2) * Math.min(1.0, edge / (double) BlueprintTiers.EDGES[tier]));
        g.fill(bar.x() + 1, bar.y() + 1, bar.x() + 1 + filled, bar.bottom() - 1, TIER_COLOURS[tier]);
        if (mouseX >= icon.x() && mouseX < bar.right() && mouseY >= icon.y() && mouseY < icon.bottom()) {
            List<FormattedCharSequence> tip = new ArrayList<>();
            tip.add(Component.translatable("simplebuilding.blueprint.tooltip.needs", BlueprintTiers.wandName(tier)).getVisualOrderText());
            tip.add(Component.translatable("simplebuilding.blueprint.editor.usage", edge, BlueprintTiers.EDGES[tier] - edge)
                    .withStyle(ChatFormatting.GRAY).getVisualOrderText());
            g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    /** Unter dem Code: Status, darunter die Zeichenzahl; rechts daneben der Buch-Knopf. */
    private void drawStatus(GuiGraphicsExtractor g) {
        BlueprintCode.Problem shown = null;
        int cursor = codeArea.cursor();
        for (BlueprintCode.Problem p : parsed.problems()) {
            if (cursor >= p.start() && cursor <= p.end()) {
                shown = p;
                break;
            }
        }
        if (shown == null && !parsed.problems().isEmpty()) {
            shown = parsed.problems().get(0);
        }
        String text;
        int colour;
        if (shown != null) {
            text = Component.translatable("simplebuilding.blueprint.editor.invalid",
                    Component.translatable("simplebuilding.blueprint.editor.error_line", shown.line() + 1, shown.message())).getString();
            colour = ERROR;
        } else if (parsed.model().isEmpty()) {
            text = Component.translatable("simplebuilding.blueprint.editor.status_empty").getString();
            colour = INK_SOFT;
        } else {
            text = Component.translatable("simplebuilding.blueprint.editor.ok").getString();
            colour = OK;
        }
        Rect status = layout.status;
        g.text(font, font.plainSubstrByWidth(text, status.w()), status.x(), status.y(), colour, false);
        Rect chars = layout.chars;
        fitted(g, Component.translatable("simplebuilding.blueprint.editor.chars", code.length(), BlueprintCode.MAX_CODE_LENGTH),
                chars.x(), chars.y(), chars.w(), code.length() > BlueprintCode.MAX_CODE_LENGTH * 9 / 10 ? ERROR : INK_SOFT);
    }

    private void drawView(GuiGraphicsExtractor g) {
        BlueprintModel model = parsed.model();
        if (model.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.empty_view"), viewX + 6, bodyY + 6, viewW - 12, 0xFFB0C4DE, false);
            return;
        }
        BlueprintView.Mesh mesh = BlueprintView.mesh(model);
        BlueprintView.render(g, mesh, viewX + 2, bodyY + 2, viewW - 4, bodyH - 4, controls.rotation(),
                controls.zoom(), controls.panX(), controls.panY());
        if (mesh.truncated()) {
            g.text(font, Component.translatable("simplebuilding.blueprint.editor.truncated"), viewX + 4, bodyY + bodyH - 12, 0xFFFFB74D, false);
        }
        // Hinweis oben links in Normalgroesse, umbrochen statt verkleinert, nicht unter das Reset-Icon
        g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.view_hint"), viewX + 4, bodyY + 4,
                layout.reset.x() - 4 - (viewX + 4), 0x90B0C4DE, false);
    }

    /** Hilfe statt 3D-Ansicht: Blockliste (Reiter Blocks) oder Anleitung (Reiter Guide). */
    private void drawHelp(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!helpBlocks) {
            Rect r = layout.guideText;
            List<FormattedCharSequence> lines = guideLines(r.w() - 8);
            helpScroll = Math.max(0, Math.min(helpScroll, Math.max(0, lines.size() * 10 - r.h() + 4)));
            g.enableScissor(r.x(), r.y(), r.right(), r.bottom());
            int y = r.y() + 2 - (int) helpScroll;
            for (FormattedCharSequence line : lines) {
                if (y > r.y() - 10 && y < r.bottom()) {
                    g.text(font, line, r.x() + 4, y, INK, false);
                }
                y += 10;
            }
            g.disableScissor();
            return;
        }
        Rect r = layout.helpList;
        int row = BlueprintEditorLayout.HELP_ROW;
        if (helpResults.isEmpty()) {
            g.text(font, Component.translatable("simplebuilding.blueprint.editor.no_hits"), r.x() + 4, r.y() + 4, INK_SOFT, false);
            return;
        }
        helpScroll = Math.max(0, Math.min(helpScroll, Math.max(0, helpResults.size() * row - r.h())));
        g.enableScissor(r.x(), r.y(), r.right(), r.bottom());
        int y = r.y() - (int) helpScroll;
        Component hover = null;
        for (Block block : helpResults) {
            if (y > r.y() - row && y < r.bottom()) {
                if (block == selected) {
                    g.fill(r.x() + 1, y, r.right() - 1, y + row, 0x50000000 | (SELECT & 0xFFFFFF));
                }
                Item item = block.asItem();
                if (item != Items.AIR) {
                    g.item(new ItemStack(item), r.x() + 2, y + 1);
                }
                g.text(font, font.plainSubstrByWidth(displayName(block), r.w() - 24), r.x() + 21, y + 1, INK, false);
                String id = font.plainSubstrByWidth(BlueprintBlockSearch.codeName(block), (int) ((r.w() - 24) / 0.75f));
                g.pose().pushMatrix();
                g.pose().translate(r.x() + 21, y + 10);
                g.pose().scale(0.75f, 0.75f);
                g.text(font, id, 0, 0, INK_SOFT, false);
                g.pose().popMatrix();
                if (r.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + row) {
                    hover = Component.translatable(readOnly ? "simplebuilding.blueprint.help.pick_readonly" : "simplebuilding.blueprint.help.pick");
                }
            }
            y += row;
        }
        g.disableScissor();
        if (hover != null) {
            g.setTooltipForNextFrame(font, hover, mouseX, mouseY);
        }
    }

    private List<FormattedCharSequence> guideLines(int w) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String part : GUIDE) {
            if (part.startsWith("h")) {
                if (!lines.isEmpty()) {
                    lines.add(FormattedCharSequence.EMPTY);
                }
                lines.addAll(font.split(Component.translatable("simplebuilding.blueprint.help.guide.head." + part.substring(1))
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE), w));
            } else {
                lines.addAll(font.split(Component.translatable("simplebuilding.blueprint.help.guide." + part), w));
                lines.add(FormattedCharSequence.EMPTY);
            }
        }
        return lines;
    }

    // =====================================================================================
    // MAUS
    // =====================================================================================

    private boolean overView(double mx, double my) {
        return layout.view.contains(mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (helpOpen && helpBlocks && layout.helpList.contains(mx, my)) {
            int i = (int) ((my - layout.helpList.y() + helpScroll) / BlueprintEditorLayout.HELP_ROW);
            if (i >= 0 && i < helpResults.size()) {
                selected = helpResults.get(i);
                updateButtons();
                if (doubleClick) {
                    insertTarget();
                }
                return true;
            }
        }
        // Freie Vorschau: der Drag startet vor der Verteilung an die Widgets, damit kein Widget ihn
        // abfangen kann - nur Knoepfe im Fenster (Reset, Beispiel) gehen vor.
        if (!helpOpen && overView(mx, my) && getChildAt(mx, my).isEmpty()) {
            draggingView = true;
            // Strg beim Tastendruck entscheidet fuer den ganzen Ziehvorgang: Verschieben statt Drehen.
            draggingPan = event.hasControlDown();
            if (doubleClick) {
                controls.reset();
            }
            return true;
        }
        draggingView = false;
        draggingPan = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingView = false;
        draggingPan = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingView) {
            if (draggingPan) {
                controls.pan(dx, dy);
            } else {
                controls.drag(dx, dy);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overView(mouseX, mouseY)) {
            if (helpOpen) {
                helpScroll -= scrollY * (helpBlocks ? BlueprintEditorLayout.HELP_ROW : 20);
            } else {
                controls.zoom(scrollY);
            }
            return true;
        }
        if (layout.list.contains(mouseX, mouseY)) {
            listScroll -= scrollY * BlueprintEditorLayout.CELL_H;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Kreispfeil "Ansicht zuruecksetzen", 9×8, im Stil der eingravierten Symbole. */
    private static final String[] RESET_ICON = {
            "..####.#.",
            ".#....##.",
            "#....###.",
            "#........",
            "#.......#",
            "#.......#",
            ".#.....#.",
            "..#####..",
    };

    /** Reiter "Blocks": ein Wuerfel von schraeg oben, 9×9. */
    private static final String[] BLOCKS_ICON = {
            "....#....",
            "..##.##..",
            "##.....##",
            "#.##.##.#",
            "#...#...#",
            "#...#...#",
            "#...#...#",
            ".##.#.##.",
            "...###...",
    };

    /** Reiter "Guide": ein aufgeschlagenes Buch mit Zeilen, 11×8. */
    private static final String[] GUIDE_ICON = {
            "..##...##..",
            "##..#.#..##",
            "#.##.#.##.#",
            "#....#....#",
            "#.##.#.##.#",
            "#....#....#",
            "##..###..##",
            "..##...##..",
    };

    /** "Text kopieren": zwei versetzte Blaetter, 9×9. */
    private static final String[] COPY_ICON = {
            "######...",
            "#....#...",
            "#..######",
            "#..#....#",
            "#..#.##.#",
            "####....#",
            "...#.##.#",
            "...#....#",
            "...######",
    };

    /** Nach dem Kopieren kurz ein Haken, 9×7. */
    private static final String[] CHECK_ICON = {
            "........#",
            ".......##",
            "#.....##.",
            "##...##..",
            ".##.##...",
            "..###....",
            "...#.....",
    };

    /**
     * Kleiner 16×16-Knopf ohne Text: entweder das Wissensbuch (Hilfe, wie Vanillas Rezeptbuch-Knopf)
     * oder ein gezeichnetes Pixel-Symbol auf dunklem Grund (Reset im Vorschaufenster).
     */
    private static final class IconButton extends net.minecraft.client.gui.components.AbstractButton {
        private final Runnable action;
        private String[] bitmap;

        IconButton(Rect r, Component label, Component tooltip, String[] bitmap, Runnable action) {
            super(r.x(), r.y(), r.w(), r.h(), label);
            this.action = action;
            this.bitmap = bitmap;
            setTooltip(Tooltip.create(tooltip));
        }

        void setBitmap(String[] bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            int x = getX(), y = getY();
            if (bitmap == null) {
                if (isHoveredOrFocused()) {
                    g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x40FFFFFF);
                }
                g.item(new ItemStack(Items.KNOWLEDGE_BOOK), x, y);
                return;
            }
            // Ein inaktiver Reiter ist der gewaehlte: hell hinterlegt mit Unterstrich (Queue N29).
            boolean selected = !active;
            g.fill(x, y, x + width, y + height, selected ? 0xC0405A78 : isHoveredOrFocused() ? 0xA0405A78 : 0x70000000);
            if (selected) {
                g.fill(x + 1, y + height - 1, x + width - 1, y + height, 0xFFFFFFFF);
            }
            int ox = x + (width - bitmap[0].length()) / 2;
            int oy = y + (height - bitmap.length) / 2;
            int colour = selected || isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFC8D6E8;
            for (int row = 0; row < bitmap.length; row++) {
                for (int col = 0; col < bitmap[row].length(); col++) {
                    if (bitmap[row].charAt(col) == '#') {
                        g.fill(ox + col, oy + row, ox + col + 1, oy + row + 1, colour);
                    }
                }
            }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
