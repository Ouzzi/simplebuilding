package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintBlockSearch;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintExamples;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintTiers;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;

/**
 * Der Blaupausen-Editor: ein Kartenblatt in drei Spalten.
 * <ul>
 *   <li>links die Materialliste (Icon, Menge, Name; groesste Menge zuerst), darunter die Masse mit
 *       Blockzahl, der noetige Baustab (Icon + Stufe) und die Leiste "genutzt / frei" zur Stufengrenze,</li>
 *   <li>Mitte der Code (Syntax-Farben, Zeilennummern, unterstrichene Fehler), darunter nur der
 *       Code-Status und die Einfuege-Leiste (Blocksuche, Auswahl per Klick, "Einfuegen" bestaetigt),</li>
 *   <li>rechts das Bauwerk in 3D (frei drehbar, Mausrad zoomt) oder, per Buch-Knopf, die Hilfe
 *       (Code-Anleitung und durchsuchbare Blockliste); bei leerem Code "Beispiel einfuegen";
 *       darunter buendig "Signieren" und "Fertig".</li>
 * </ul>
 * Speichern: jede Aenderung geht 1,5 s nach dem letzten Tastendruck und beim Schliessen (auch
 * ueber {@link #removed()}, also bei Verbindungsabbruch/Weltverlassen) per {@link BlueprintEditPayload}
 * an den Server, der sie sofort am Item ablegt.
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
    private static final int ROW = 17;
    private static final int FOOTER = 56;
    /** Autospeichern so lange nach der letzten Aenderung. */
    public static final long AUTOSAVE_MS = 1500;
    private static final int[] TIER_COLOURS = {0xFFB87333, 0xFFA8A8A8, 0xFFE0B82E, 0xFF4FC3C7, 0xFF5A4A4A, 0xFF7B3FA0};

    private final int slot;
    private final BlueprintContent original;
    private final boolean readOnly;
    private String code;
    private String lastSent;

    private BlueprintCodeArea codeArea;
    private EditBox titleBox;
    private EditBox insertBox;
    private EditBox helpSearch;
    private Button doneButton;
    private Button signButton;
    private Button confirmSignButton;
    private Button cancelSignButton;
    private Button insertButton;
    private Button exampleButton;
    private BookButton helpButton;
    private Button guideTab;
    private Button blocksTab;
    private boolean signing;
    private boolean helpOpen;
    private boolean helpBlocks;

    private BlueprintCode.ParseResult parsed;
    private List<BlueprintMaterials.Entry> materials = List.of();
    private boolean dirty;
    private long lastEdit;

    private List<Block> insertResults = List.of();
    private Block selected;
    private List<Block> helpResults = List.of();
    private double helpScroll;

    private int panelX, panelY, panelW, panelH;
    private int listX, listW, codeX, codeW, viewX, viewW, bodyY, bodyH, footerY;
    private double listScroll;
    private Quaternionf rotation = BlueprintView.defaultRotation();
    private float zoom = 1f;
    private boolean draggingView;

    public BlueprintScreen(Player player, InteractionHand hand) {
        super(Component.translatable("item.simplebuilding.blueprint"));
        ItemStack stack = player.getItemInHand(hand);
        this.slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        this.original = BlueprintItem.content(stack);
        this.readOnly = original.signed();
        this.code = original.code();
        this.lastSent = code;
        this.parsed = BlueprintCode.parse(code);
        this.materials = BlueprintMaterials.list(parsed.model());
    }

    /** Schriftgroesse im Code-Feld: ganzzahlige Pixel, bei grossem GUI-Massstab kleiner. */
    private float codeScale() {
        int guiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
        return guiScale >= 3 ? (guiScale - 1f) / guiScale : 1f;
    }

    private static String displayName(Block block) {
        return block.getName().getString();
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 12, 560);
        panelH = Math.min(height - 12, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int inner = panelW - 16;
        listW = Math.max(90, Math.min(130, inner * 22 / 100));
        viewW = Math.max(120, inner * 32 / 100);
        codeW = inner - listW - viewW - 8;
        listX = panelX + 8;
        codeX = listX + listW + 4;
        viewX = codeX + codeW + 4;
        bodyY = panelY + 30;
        bodyH = panelH - 30 - FOOTER - 6;
        footerY = bodyY + bodyH + 3;

        String current = codeArea != null ? codeArea.getValue() : code;
        codeArea = new BlueprintCodeArea(font, codeX, bodyY, codeW, bodyH, codeScale(), readOnly, current, this::onCodeChanged);
        codeArea.setHighlight(parsed.styles(), parsed.problems());
        addRenderableWidget(codeArea);

        // Einfuege-Leiste unter dem Code: Suche, Ergebnis-Symbole (gezeichnet), "Einfuegen"
        insertBox = new EditBox(font, codeX, footerY + 12, codeW - 62, 16, Component.translatable("simplebuilding.blueprint.editor.insert_search"));
        insertBox.setHint(Component.translatable("simplebuilding.blueprint.editor.insert_hint"));
        insertBox.setResponder(q -> {
            insertResults = q.isBlank() ? List.of() : BlueprintBlockSearch.search(q, BlueprintScreen::displayName, 64);
            if (selected != null && !insertResults.contains(selected)) {
                selected = null;
            }
            updateButtons();
        });
        insertBox.visible = !readOnly;
        addRenderableWidget(insertBox);
        insertButton = addRenderableWidget(Button.builder(Component.translatable("simplebuilding.blueprint.editor.insert"), b -> insertSelected())
                .bounds(codeX + codeW - 58, footerY + 12, 58, 16).build());

        // Rechte Spalte: Buch-Knopf (Hilfe), Beispiel, Signieren/Fertig
        helpButton = addRenderableWidget(new BookButton(viewX + viewW - 16, bodyY - 16, () -> {
            helpOpen = !helpOpen;
            updateButtons();
        }));
        exampleButton = addRenderableWidget(Button.builder(Component.translatable("simplebuilding.blueprint.editor.example"), b -> insertExample())
                .bounds(viewX + 6, bodyY + bodyH - 24, viewW - 12, 18).build());
        guideTab = addRenderableWidget(Button.builder(Component.translatable("simplebuilding.blueprint.help.guide_tab"), b -> {
            helpBlocks = false;
            helpScroll = 0;
            updateButtons();
        }).bounds(viewX + 2, bodyY + 2, viewW / 2 - 3, 14).build());
        blocksTab = addRenderableWidget(Button.builder(Component.translatable("simplebuilding.blueprint.help.blocks_tab"), b -> {
            helpBlocks = true;
            helpScroll = 0;
            updateButtons();
        }).bounds(viewX + viewW / 2 + 1, bodyY + 2, viewW / 2 - 3, 14).build());
        helpSearch = new EditBox(font, viewX + 3, bodyY + 19, viewW - 6, 14, Component.translatable("simplebuilding.blueprint.help.search"));
        helpSearch.setHint(Component.translatable("simplebuilding.blueprint.editor.insert_hint"));
        helpSearch.setResponder(q -> {
            helpResults = BlueprintBlockSearch.search(q, BlueprintScreen::displayName, 400);
            helpScroll = 0;
        });
        helpResults = BlueprintBlockSearch.search("", BlueprintScreen::displayName, 400);
        addRenderableWidget(helpSearch);

        int bw = (viewW - 4) / 2;
        doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(readOnly ? viewX : viewX + viewW - bw, footerY, readOnly ? viewW : bw, 20).build());
        signButton = addRenderableWidget(Button.builder(Component.translatable("book.signButton"), b -> {
            signing = true;
            updateButtons();
            setFocused(titleBox);
        }).bounds(viewX, footerY, bw, 20).build());
        titleBox = new EditBox(font, viewX, footerY, viewW, 18, Component.translatable("simplebuilding.blueprint.editor.title"));
        titleBox.setMaxLength(BlueprintCode.MAX_TITLE_LENGTH);
        titleBox.setHint(Component.translatable("simplebuilding.blueprint.editor.title_hint"));
        titleBox.setResponder(t -> updateButtons());
        addRenderableWidget(titleBox);
        confirmSignButton = addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), b -> sign())
                .bounds(viewX, footerY + 22, bw, 20).build());
        cancelSignButton = addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> {
            signing = false;
            updateButtons();
            setFocused(codeArea);
        }).bounds(viewX + viewW - bw, footerY + 22, bw, 20).build());
        updateButtons();
        if (!signing) {
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
        insertButton.visible = !readOnly;
        insertButton.active = selected != null;
        insertButton.setTooltip(selected == null ? null : net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("simplebuilding.blueprint.editor.insert_tip", selected.getName(), BlueprintBlockSearch.codeName(selected))));
        exampleButton.visible = !readOnly && !helpOpen && code.isBlank();
        guideTab.visible = helpOpen;
        blocksTab.visible = helpOpen;
        guideTab.active = helpBlocks;
        blocksTab.active = !helpBlocks;
        helpSearch.visible = helpOpen && helpBlocks;
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
    }

    private void save() {
        if (!readOnly && !code.equals(lastSent)) {
            ClientNetworking.send(new BlueprintEditPayload(slot, code, false, ""));
            lastSent = code;
        }
    }

    private void insertSelected() {
        if (selected != null && !readOnly) {
            codeArea.insert(BlueprintBlockSearch.codeName(selected));
            setFocused(codeArea);
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

    private void sign() {
        if (dirty) {
            reparse();
        }
        if (!parsed.ok() || titleBox.getValue().isBlank()) {
            return;
        }
        ClientNetworking.send(new BlueprintEditPayload(slot, code, true, titleBox.getValue().strip()));
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
        boolean typing = codeArea.isFocused() || insertBox.isFocused() || titleBox.isFocused() || helpSearch.isFocused();
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
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PAPER_EDGE);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PAPER);
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, PAPER_SHADE);
        g.fill(panelX + 2, panelY + panelH - 3, panelX + panelW - 2, panelY + panelH - 2, PAPER_SHADE);
        // Hintergrund der rechten Spalte vor den Knoepfen, damit die Knoepfe darauf liegen
        g.fill(viewX, bodyY, viewX + viewW, bodyY + bodyH, PAPER_EDGE);
        g.fill(viewX + 1, bodyY + 1, viewX + viewW - 1, bodyY + bodyH - 1, helpOpen ? PAPER_SHADE : VIEW_BG);
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
        if (!readOnly) {
            drawInsertResults(g, mouseX, mouseY);
        }
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        MutableComponent title = Component.empty();
        if (readOnly && !original.title().isBlank()) {
            title.append(Component.literal(original.title()).withStyle(ChatFormatting.BOLD));
            if (!original.author().isBlank()) {
                title.append(Component.literal("  ")).append(Component.translatable("book.byAuthor", original.author()));
            }
        } else {
            title.append(Component.translatable("item.simplebuilding.blueprint").withStyle(ChatFormatting.BOLD));
        }
        g.text(font, title, panelX + 8, panelY + 7, INK, false);
        Component chars = Component.translatable("simplebuilding.blueprint.editor.chars", code.length(), BlueprintCode.MAX_CODE_LENGTH);
        g.text(font, chars, panelX + panelW - 8 - font.width(chars), panelY + 7,
                code.length() > BlueprintCode.MAX_CODE_LENGTH * 9 / 10 ? ERROR : INK_SOFT, false);
        int labelY = bodyY - font.lineHeight - 1;
        g.text(font, Component.translatable("simplebuilding.blueprint.editor.materials"), listX, labelY, INK_SOFT, false);
        g.text(font, Component.translatable("simplebuilding.blueprint.editor.code"), codeX, labelY, INK_SOFT, false);
        g.text(font, Component.translatable(helpOpen ? "simplebuilding.blueprint.help.title" : "simplebuilding.blueprint.editor.view"),
                viewX, labelY, INK_SOFT, false);
    }

    private void drawMaterials(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(listX, bodyY, listX + listW, bodyY + bodyH, PAPER_SHADE);
        if (materials.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.no_materials"), listX + 3, bodyY + 3, listW - 6, INK_SOFT, false);
            return;
        }
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, materials.size() * ROW - bodyH + 2)));
        g.enableScissor(listX, bodyY, listX + listW, bodyY + bodyH);
        int y0 = bodyY + 1 - (int) listScroll;
        Component hovered = null;
        for (int i = 0; i < materials.size(); i++) {
            int ry = y0 + i * ROW;
            if (ry + ROW < bodyY || ry > bodyY + bodyH) {
                continue;
            }
            BlueprintMaterials.Entry e = materials.get(i);
            Component name = e.block() != null ? e.block().getName() : new ItemStack(e.item()).getHoverName();
            if (e.block() == null) {
                g.item(new ItemStack(e.item()), listX + 2, ry);
            } else {
                g.fill(listX + 3, ry + 1, listX + 17, ry + 15, 0x40C62828);
            }
            String count = e.count() + "×";
            g.text(font, count, listX + 20, ry + 4, e.block() != null ? ERROR : INK, false);
            int nameX = listX + 22 + font.width(count);
            g.text(font, font.plainSubstrByWidth(name.getString(), listX + listW - 2 - nameX), nameX, ry + 4, INK_SOFT, false);
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= Math.max(ry, bodyY) && mouseY < Math.min(ry + ROW, bodyY + bodyH)) {
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

    /** Unter der Materialliste: Masse + Blockzahl, der noetige Baustab, die Leiste genutzt/frei. */
    private void drawStats(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        BlueprintModel model = parsed.model();
        int x = listX;
        int y = footerY;
        if (model.isEmpty()) {
            g.text(font, Component.translatable("simplebuilding.blueprint.editor.size_empty"), x, y, INK_SOFT, false);
            return;
        }
        String dims = model.sizeX() + " × " + model.sizeY() + " × " + model.sizeZ();
        g.text(font, dims, x, y, INK, false);
        Component blocks = Component.translatable("simplebuilding.blueprint.editor.blocks", model.size());
        g.text(font, blocks, x + listW - font.width(blocks), y, INK_SOFT, false);

        int edge = model.maxEdge();
        int tier = BlueprintTiers.tierIndexFor(edge);
        if (tier < 0) {
            g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.too_big", BlueprintCode.GRID), x, y + 12, listW, ERROR, false);
            return;
        }
        g.item(new ItemStack(wandFor(tier)), x, y + 11);
        Component tierName = Component.translatable("simplebuilding.blueprint.tier." + BlueprintTiers.NAMES[tier]);
        g.text(font, tierName, x + 19, y + 15, INK, false);
        String limit = edge + " / " + BlueprintTiers.EDGES[tier];
        g.text(font, limit, x + listW - font.width(limit), y + 15, INK_SOFT, false);
        if (mouseX >= x && mouseX < x + listW && mouseY >= y + 11 && mouseY < y + 27) {
            g.setTooltipForNextFrame(font, Component.translatable("simplebuilding.blueprint.tooltip.needs", BlueprintTiers.wandName(tier)), mouseX, mouseY);
        }
        // Leiste genutzt / frei bezogen auf die Stufengrenze
        int barY = y + 30;
        g.fill(x, barY, x + listW, barY + 6, PAPER_EDGE);
        g.fill(x + 1, barY + 1, x + listW - 1, barY + 5, PAPER_SHADE);
        int filled = (int) ((listW - 2) * Math.min(1.0, edge / (double) BlueprintTiers.EDGES[tier]));
        g.fill(x + 1, barY + 1, x + 1 + filled, barY + 5, TIER_COLOURS[tier]);
        Component usage = Component.translatable("simplebuilding.blueprint.editor.usage", edge, BlueprintTiers.EDGES[tier] - edge);
        g.text(font, font.plainSubstrByWidth(usage.getString(), listW), x, barY + 9, INK_SOFT, false);
    }

    /** Direkt unter dem Code: nur der Status, in Code-Breite. */
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
        g.text(font, font.plainSubstrByWidth(text, codeW), codeX, footerY, colour, false);
    }

    /** Die Ergebnis-Symbole der Einfuege-Suche; der erste Klick waehlt, "Einfuegen" bestaetigt. */
    private void drawInsertResults(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = footerY + 32;
        int max = Math.max(1, codeW / 18);
        Component hover = null;
        if (insertResults.isEmpty() && !insertBox.getValue().isBlank()) {
            g.text(font, Component.translatable("simplebuilding.blueprint.editor.no_hits"), codeX, y + 4, INK_SOFT, false);
        }
        for (int i = 0; i < insertResults.size() && i < max; i++) {
            Block block = insertResults.get(i);
            int x = codeX + i * 18;
            if (block == selected) {
                g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3A6EA5);
            }
            g.fill(x, y, x + 16, y + 16, 0x30000000);
            Item item = block.asItem();
            if (item != Items.AIR) {
                g.item(new ItemStack(item), x, y);
            } else {
                g.text(font, "?", x + 5, y + 4, INK, false);
            }
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                hover = Component.literal("").append(block.getName()).append(Component.literal("  " + BlueprintBlockSearch.codeName(block)).withStyle(ChatFormatting.GRAY));
            }
        }
        if (hover != null) {
            g.setTooltipForNextFrame(font, hover, mouseX, mouseY);
        }
    }

    private void drawView(GuiGraphicsExtractor g) {
        BlueprintModel model = parsed.model();
        if (model.isEmpty()) {
            g.textWithWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.empty_view"), viewX + 6, bodyY + 6, viewW - 12, 0xFFB0C4DE, false);
            return;
        }
        BlueprintView.Mesh mesh = BlueprintView.mesh(model);
        BlueprintView.render(g, mesh, viewX + 2, bodyY + 2, viewW - 4, bodyH - 4, rotation, zoom);
        if (mesh.truncated()) {
            g.text(font, Component.translatable("simplebuilding.blueprint.editor.truncated"), viewX + 4, bodyY + bodyH - 12, 0xFFFFB74D, false);
        }
        g.text(font, Component.translatable("simplebuilding.blueprint.editor.view_hint"), viewX + 4, bodyY + 4, 0x90B0C4DE, false);
    }

    /** Hilfe statt 3D-Ansicht: Code-Anleitung oder durchsuchbare Blockliste. */
    private void drawHelp(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int top = bodyY + (helpBlocks ? 36 : 19);
        int bottom = bodyY + bodyH - 2;
        g.enableScissor(viewX + 1, top, viewX + viewW - 1, bottom);
        if (!helpBlocks) {
            List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
            for (int i = 1; i <= GUIDE_LINES; i++) {
                lines.addAll(font.split(Component.translatable("simplebuilding.blueprint.help.guide." + i), viewW - 10));
                lines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
            }
            helpScroll = Math.max(0, Math.min(helpScroll, Math.max(0, lines.size() * 10 - (bottom - top))));
            int y = top - (int) helpScroll;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                if (y > top - 10 && y < bottom) {
                    g.text(font, line, viewX + 5, y, INK, false);
                }
                y += 10;
            }
        } else {
            helpScroll = Math.max(0, Math.min(helpScroll, Math.max(0, helpResults.size() * 18 - (bottom - top))));
            int y = top - (int) helpScroll;
            Component hover = null;
            for (Block block : helpResults) {
                if (y > top - 18 && y < bottom) {
                    if (block == selected) {
                        g.fill(viewX + 2, y, viewX + viewW - 2, y + 18, 0x503A6EA5);
                    }
                    Item item = block.asItem();
                    if (item != Items.AIR) {
                        g.item(new ItemStack(item), viewX + 3, y + 1);
                    }
                    String name = font.plainSubstrByWidth(displayName(block), viewW - 26);
                    g.text(font, name, viewX + 22, y + 1, INK, false);
                    String id = font.plainSubstrByWidth(BlueprintBlockSearch.codeName(block), viewW - 26);
                    g.pose().pushMatrix();
                    g.pose().translate(viewX + 22, y + 10);
                    g.pose().scale(0.75f, 0.75f);
                    g.text(font, id, 0, 0, INK_SOFT, false);
                    g.pose().popMatrix();
                    if (mouseX >= viewX && mouseX < viewX + viewW && mouseY >= Math.max(y, top) && mouseY < Math.min(y + 18, bottom)) {
                        hover = Component.translatable("simplebuilding.blueprint.help.pick");
                    }
                }
                y += 18;
            }
            if (hover != null) {
                g.setTooltipForNextFrame(font, hover, mouseX, mouseY);
            }
        }
        g.disableScissor();
    }

    private static final int GUIDE_LINES = 10;

    // =====================================================================================
    // MAUS
    // =====================================================================================

    private boolean overView(double mx, double my) {
        return mx >= viewX && mx < viewX + viewW && my >= bodyY && my < bodyY + bodyH;
    }

    private boolean overList(double mx, double my) {
        return mx >= listX && mx < listX + listW && my >= bodyY && my < bodyY + bodyH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        // Einfuege-Ergebnisse: erster Klick waehlt (ein zweiter auf "Einfuegen" fuegt ein)
        if (!readOnly && my >= footerY + 32 && my < footerY + 48 && mx >= codeX && mx < codeX + codeW) {
            int i = (int) ((mx - codeX) / 18);
            if (i >= 0 && i < insertResults.size() && i < Math.max(1, codeW / 18)) {
                selected = insertResults.get(i);
                updateButtons();
                return true;
            }
        }
        if (helpOpen && helpBlocks && overView(mx, my) && my >= bodyY + 36) {
            int i = (int) ((my - (bodyY + 36) + helpScroll) / 18);
            if (i >= 0 && i < helpResults.size()) {
                selected = helpResults.get(i);
                insertBox.setValue(BlueprintBlockSearch.codeName(selected));
                selected = helpResults.get(i);
                updateButtons();
                return true;
            }
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (!helpOpen && overView(mx, my)) {
            draggingView = true;
            if (doubleClick) {
                rotation = BlueprintView.defaultRotation();
                zoom = 1f;
            }
            return true;
        }
        draggingView = false;
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingView = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingView) {
            // Trackball: waagerecht um die Bild-Hochachse, senkrecht um die Bild-Querachse -
            // vorne angesetzt, damit jede Richtung frei erreichbar ist (auch kopfueber).
            float k = 0.012f;
            rotation = new Quaternionf().rotateX((float) (dy * k)).rotateY((float) (dx * k)).mul(rotation);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overView(mouseX, mouseY)) {
            if (helpOpen) {
                helpScroll -= scrollY * 18;
            } else {
                zoom = (float) Math.max(0.2, Math.min(12.0, zoom * Math.pow(1.15, scrollY)));
            }
            return true;
        }
        if (overList(mouseX, mouseY)) {
            listScroll -= scrollY * ROW;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Buch-Symbol wie Vanillas Rezeptbuch-Knopf: oeffnet und schliesst die Hilfe. */
    private static final class BookButton extends net.minecraft.client.gui.components.AbstractButton {
        private final Runnable action;

        BookButton(int x, int y, Runnable action) {
            super(x, y, 16, 16, Component.translatable("simplebuilding.blueprint.help.title"));
            this.action = action;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("simplebuilding.blueprint.help.open")));
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            if (isHoveredOrFocused()) {
                g.fill(getX() - 1, getY() - 1, getX() + 17, getY() + 17, 0x40FFFFFF);
            }
            g.item(new ItemStack(Items.KNOWLEDGE_BOOK), getX(), getY());
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
