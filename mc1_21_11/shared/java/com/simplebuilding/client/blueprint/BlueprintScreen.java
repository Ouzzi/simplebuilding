package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.networking.BlueprintEditPayload;
import com.simplebuilding.platform.ClientNetworking;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Der Blaupausen-Editor: ein Kartenblatt in drei Spalten.
 * <ul>
 *   <li>links, fest stehend: die Materialliste (Icon, Menge, Name; groesste Menge zuerst),</li>
 *   <li>Mitte: der Code mit Syntax-Einfaerbung, Zeilennummern, Bildlaufleiste und unterstrichenen
 *       Fehlern; die Meldung zum Fehler unter dem Cursor (sonst zum ersten) steht darunter,</li>
 *   <li>rechts: das Bauwerk in 3D, mit der Maus frei in jede Richtung drehbar, Mausrad zoomt.</li>
 * </ul>
 * Unten die Groessenanzeige (Bounding Box gegen die Baustab-Stufen) und die Knoepfe. Signieren
 * wie beim Buch: Titel eingeben, danach ist die Blaupause schreibgeschuetzt. Jede Aenderung geht
 * beim Schliessen per {@link BlueprintEditPayload} an den Server, der sie selbst prueft.
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

    private final int slot;
    private final BlueprintContent original;
    private final boolean readOnly;
    private String code;

    private BlueprintCodeArea codeArea;
    private EditBox titleBox;
    private Button doneButton;
    private Button signButton;
    private Button confirmSignButton;
    private Button cancelSignButton;
    private boolean signing;

    private BlueprintCode.ParseResult parsed;
    private List<BlueprintMaterials.Entry> materials = List.of();
    private boolean dirty;
    private long lastEdit;

    private int panelX, panelY, panelW, panelH;
    private int listX, listW, codeX, codeW, viewX, viewW, bodyY, bodyH;
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
        this.parsed = BlueprintCode.parse(code);
        this.materials = BlueprintMaterials.list(parsed.model());
    }

    /** Schriftgroesse im Code-Feld: ganzzahlige Pixel, bei grossem GUI-Massstab kleiner. */
    private float codeScale() {
        int guiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
        return guiScale >= 3 ? (guiScale - 1f) / guiScale : 1f;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 12, 560);
        panelH = Math.min(height - 12, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int inner = panelW - 16;
        listW = Math.max(84, Math.min(130, inner * 22 / 100));
        viewW = Math.max(100, inner * 32 / 100);
        codeW = inner - listW - viewW - 8;
        listX = panelX + 8;
        codeX = listX + listW + 4;
        viewX = codeX + codeW + 4;
        bodyY = panelY + 30;
        bodyH = panelH - 30 - 40;

        String current = codeArea != null ? codeArea.getValue() : code;
        codeArea = new BlueprintCodeArea(font, codeX, bodyY, codeW, bodyH, codeScale(), readOnly, current, this::onCodeChanged);
        codeArea.setHighlight(parsed.styles(), parsed.problems());
        addRenderableWidget(codeArea);

        int buttonY = panelY + panelH - 24;
        titleBox = new EditBox(font, panelX + 8, buttonY, Math.min(160, inner / 3), 18, Component.translatable("simplebuilding.blueprint.editor.title"));
        titleBox.setMaxLength(BlueprintCode.MAX_TITLE_LENGTH);
        titleBox.setHint(Component.translatable("simplebuilding.blueprint.editor.title_hint"));
        titleBox.setResponder(t -> updateButtons());
        addRenderableWidget(titleBox);

        int bw = 90;
        int right = panelX + panelW - 8;
        doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(right - bw, buttonY, bw, 18).build());
        signButton = addRenderableWidget(Button.builder(Component.translatable("book.signButton"), b -> {
            signing = true;
            updateButtons();
            setFocused(titleBox);
        }).bounds(right - 2 * bw - 4, buttonY, bw, 18).build());
        confirmSignButton = addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), b -> sign())
                .bounds(right - bw, buttonY, bw, 18).build());
        cancelSignButton = addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> {
            signing = false;
            updateButtons();
            setFocused(codeArea);
        }).bounds(right - 2 * bw - 4, buttonY, bw, 18).build());
        updateButtons();
        if (!signing) {
            setInitialFocus(codeArea);
        }
    }

    private void updateButtons() {
        doneButton.visible = !signing;
        signButton.visible = !signing && !readOnly;
        signButton.active = parsed.ok() && !parsed.model().isEmpty();
        titleBox.visible = signing;
        confirmSignButton.visible = signing;
        cancelSignButton.visible = signing;
        confirmSignButton.active = !titleBox.getValue().isBlank() && parsed.ok() && !parsed.model().isEmpty();
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
        if (dirty && Util.getMillis() - lastEdit > 150) {
            reparse();
        }
    }

    private void sign() {
        if (dirty) {
            reparse();
        }
        if (!parsed.ok() || titleBox.getValue().isBlank()) {
            return;
        }
        ClientNetworking.send(new BlueprintEditPayload(slot, code, true, titleBox.getValue().strip()));
        minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        if (!readOnly && !code.equals(original.code())) {
            ClientNetworking.send(new BlueprintEditPayload(slot, code, false, ""));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================================
    // ZEICHNEN
    // =====================================================================================

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.renderBackground(g, mouseX, mouseY, a);
        // Kartenblatt: Papier mit dunklerem Rand und leicht abgesetzten Ecken
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PAPER_EDGE);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PAPER);
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, PAPER_SHADE);
        g.fill(panelX + 2, panelY + panelH - 3, panelX + panelW - 2, panelY + panelH - 2, PAPER_SHADE);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.render(g, mouseX, mouseY, a);
        drawHeader(g);
        drawMaterials(g, mouseX, mouseY);
        drawView(g);
        drawFooter(g);
    }

    private void drawHeader(GuiGraphics g) {
        MutableComponent title = Component.empty();
        if (readOnly && !original.title().isBlank()) {
            title.append(Component.literal(original.title()).withStyle(ChatFormatting.BOLD));
            if (!original.author().isBlank()) {
                title.append(Component.literal("  ")).append(Component.translatable("book.byAuthor", original.author()));
            }
        } else {
            title.append(Component.translatable("item.simplebuilding.blueprint").withStyle(ChatFormatting.BOLD));
        }
        g.drawString(font, title, panelX + 8, panelY + 7, INK, false);
        Component chars = Component.translatable("simplebuilding.blueprint.editor.chars", code.length(), BlueprintCode.MAX_CODE_LENGTH);
        g.drawString(font, chars, panelX + panelW - 8 - font.width(chars), panelY + 7,
                code.length() > BlueprintCode.MAX_CODE_LENGTH * 9 / 10 ? ERROR : INK_SOFT, false);
        int labelY = bodyY - font.lineHeight - 1;
        g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.materials"), listX, labelY, INK_SOFT, false);
        g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.code"), codeX, labelY, INK_SOFT, false);
        g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.view"), viewX, labelY, INK_SOFT, false);
    }

    private void drawMaterials(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(listX, bodyY, listX + listW, bodyY + bodyH, PAPER_SHADE);
        if (materials.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.no_materials"), listX + 3, bodyY + 3, listW - 6, INK_SOFT, false);
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
                g.renderItem(new ItemStack(e.item()), listX + 2, ry);
            } else {
                g.fill(listX + 3, ry + 1, listX + 17, ry + 15, 0x40C62828);
            }
            String count = e.count() + "×";
            g.drawString(font, count, listX + 20, ry + 4, e.block() != null ? ERROR : INK, false);
            int nameX = listX + 22 + font.width(count);
            String shown = font.plainSubstrByWidth(name.getString(), listX + listW - 2 - nameX);
            g.drawString(font, shown, nameX, ry + 4, INK_SOFT, false);
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

    private void drawView(GuiGraphics g) {
        g.fill(viewX, bodyY, viewX + viewW, bodyY + bodyH, PAPER_EDGE);
        g.fill(viewX + 1, bodyY + 1, viewX + viewW - 1, bodyY + bodyH - 1, VIEW_BG);
        BlueprintModel model = parsed.model();
        if (model.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("simplebuilding.blueprint.editor.empty_view"), viewX + 6, bodyY + 6, viewW - 12, 0xFFB0C4DE, false);
            return;
        }
        BlueprintView.Mesh mesh = BlueprintView.mesh(model);
        BlueprintView.render(g, mesh, viewX + 2, bodyY + 2, viewW - 4, bodyH - 4, rotation, zoom);
        if (mesh.truncated()) {
            g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.truncated"), viewX + 4, bodyY + bodyH - 12, 0xFFFFB74D, false);
        }
        g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.view_hint"), viewX + 4, bodyY + 4, 0x90B0C4DE, false);
    }

    private void drawFooter(GuiGraphics g) {
        int y = bodyY + bodyH + 3;
        // Fehlerzeile: der Fehler unter dem Cursor, sonst der erste
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
        if (shown != null) {
            Component msg = Component.translatable("simplebuilding.blueprint.editor.error_line", shown.line() + 1, shown.message());
            String text = font.plainSubstrByWidth(msg.getString(), panelW - 16);
            g.drawString(font, text, panelX + 8, y, ERROR, false);
        } else if (!parsed.model().isEmpty()) {
            g.drawString(font, Component.translatable("simplebuilding.blueprint.editor.ok"), panelX + 8, y, OK, false);
        }
        if (!signing) {
            drawSizeIndicator(g, panelX + 8, panelY + panelH - 22, panelW - 16 - 2 * 94 - 8);
        }
    }

    /** Groessenanzeige: Bounding Box, Balken bis 128 mit den Stufen-Marken, benoetigter Baustab. */
    private void drawSizeIndicator(GuiGraphics g, int x, int y, int w) {
        BlueprintModel model = parsed.model();
        int edge = model.maxEdge();
        int tier = BlueprintTiers.tierIndexFor(edge);
        Component label;
        if (model.isEmpty()) {
            label = Component.translatable("simplebuilding.blueprint.editor.size_empty");
        } else {
            label = Component.translatable("simplebuilding.blueprint.editor.size", model.sizeX(), model.sizeY(), model.sizeZ(), model.size(),
                    tier >= 0 ? BlueprintTiers.wandName(tier) : Component.literal("-"), tier >= 0 ? BlueprintTiers.EDGES[tier] : BlueprintCode.GRID);
        }
        g.drawString(font, font.plainSubstrByWidth(label.getString(), w), x, y, INK, false);
        int barY = y + font.lineHeight + 2;
        int barW = Math.max(20, w);
        g.fill(x, barY, x + barW, barY + 5, PAPER_EDGE);
        g.fill(x + 1, barY + 1, x + barW - 1, barY + 4, PAPER_SHADE);
        int filled = (int) ((barW - 2) * Math.min(1.0, edge / (double) BlueprintCode.GRID));
        int colour = tier < 0 ? ERROR : TIER_COLOURS[tier];
        g.fill(x + 1, barY + 1, x + 1 + filled, barY + 4, colour);
        for (int i = 0; i < BlueprintTiers.EDGES.length; i++) {
            int mx = x + 1 + (int) ((barW - 2) * BlueprintTiers.EDGES[i] / (double) BlueprintCode.GRID);
            g.fill(mx - 1, barY - 1, mx, barY + 6, TIER_COLOURS[i]);
        }
    }

    private static final int[] TIER_COLOURS = {0xFFB87333, 0xFFA8A8A8, 0xFFE0B82E, 0xFF4FC3C7, 0xFF5A4A4A, 0xFF7B3FA0};

    // =====================================================================================
    // MAUS: 3D-Ansicht drehen/zoomen, Materialliste rollen
    // =====================================================================================

    private boolean overView(double mx, double my) {
        return mx >= viewX && mx < viewX + viewW && my >= bodyY && my < bodyY + bodyH;
    }

    private boolean overList(double mx, double my) {
        return mx >= listX && mx < listX + listW && my >= bodyY && my < bodyY + bodyH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (overView(event.x(), event.y())) {
            draggingView = true;
            if (doubleClick) {
                rotation = BlueprintView.defaultRotation();
                zoom = 1f;
            }
            return true;
        }
        draggingView = false;
        return super.mouseClicked(event, doubleClick);
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
            zoom = (float) Math.max(0.2, Math.min(12.0, zoom * Math.pow(1.15, scrollY)));
            return true;
        }
        if (overList(mouseX, mouseY)) {
            listScroll -= scrollY * ROW;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
