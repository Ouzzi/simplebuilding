package com.simplebuilding.client.gui;

import com.mojang.blaze3d.platform.InputConstants;

import com.simplebuilding.client.ClientState;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantConfigurePayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private static final Identifier PANEL_SPRITE = Identifier.withDefaultNamespace("popup/background");
    /** Vanillas Beschriftungsfarbe auf hellem Grund (Container-Titel, CommonColors.DARK_GRAY). */
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int SECONDARY_COLOR = 0xFF707070;

    private final ItemStack stack;

    private EditBox x1Field, y1Field, z1Field;
    private EditBox x2Field, y2Field, z2Field;
    private EditBox wField, hField, dField;

    private Button shapeButton;
    private Button lockButton;
    private Button orientationButton;
    private Button figureToggleButton;
    private Button pageButton;
    private Button doneButton;

    // Fill Settings Buttons
    private Button hollowButton;
    private Button layerModeButton;
    private Button fillOrderButton;

    private final List<AbstractWidget> pageOneWidgets = new ArrayList<>();
    private final List<AbstractWidget> pageTwoWidgets = new ArrayList<>();

    private OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
    private OrientationMode currentOrientation = OrientationMode.POS_Y;
    private boolean isHollow = false;
    private boolean isLayerMode = false;
    private OctantItem.FillOrder currentOrder = OctantItem.FillOrder.DEFAULT;

    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private boolean isLocked = false;
    private boolean isUpdating = false;

    private int columnCenterX;
    private int startY;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int rowStartX;
    private int hudSummaryY;
    private int yDoneSelection;
    private int yDoneSettings;

    private static final int PANEL_PADDING = 8;
    private static final int TITLE_HEIGHT = 20;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int GAP = 4;
    private static final int LABEL_WIDTH = 20;
    private static final int STEP_BUTTON_WIDTH = 12;
    private static final int FIELD_WIDTH = 36;
    private static final int GROUP_WIDTH = STEP_BUTTON_WIDTH + 1 + FIELD_WIDTH + 1 + STEP_BUTTON_WIDTH;
    private static final int CONTENT_WIDTH = LABEL_WIDTH + GROUP_WIDTH * 3 + GAP * 2;
    private static final int SUMMARY_HEIGHT = 44;

    private enum MenuPage {
        SELECTION,
        SETTINGS
    }

    private MenuPage currentPage = MenuPage.SELECTION;

    private enum OrientationMode {
        POS_X(0, Direction.EAST, "simplebuilding.axis.pos_x"),
        POS_Y(1, Direction.UP, "simplebuilding.axis.pos_y"),
        POS_Z(2, Direction.SOUTH, "simplebuilding.axis.pos_z"),
        NEG_X(3, Direction.WEST, "simplebuilding.axis.neg_x"),
        NEG_Y(4, Direction.DOWN, "simplebuilding.axis.neg_y"),
        NEG_Z(5, Direction.NORTH, "simplebuilding.axis.neg_z");

        final int nbtIndex;
        final Direction direction;
        final String labelKey;

        OrientationMode(int nbtIndex, Direction direction, String labelKey) {
            this.nbtIndex = nbtIndex;
            this.direction = direction;
            this.labelKey = labelKey;
        }

        static OrientationMode fromNbtIndex(int index) {
            for (OrientationMode mode : values()) {
                if (mode.nbtIndex == index) return mode;
            }
            if (index == 0) return POS_X;
            if (index == 2) return POS_Z;
            return POS_Y;
        }

        OrientationMode next() {
            OrientationMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public OctantScreen(ItemStack stack) {
        super(Component.translatable("simplebuilding.gui.title"));
        this.stack = stack;
        loadDataFromStack();
    }

    private void loadDataFromStack() {
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        if (nbt.contains("Pos1")) nbt.getIntArray("Pos1").ifPresent(p -> { if(p.length==3) pos1 = new BlockPos(p[0], p[1], p[2]); });
        if (nbt.contains("Pos2")) nbt.getIntArray("Pos2").ifPresent(p -> { if(p.length==3) pos2 = new BlockPos(p[0], p[1], p[2]); });

        if (nbt.contains("Shape")) try { currentShape = OctantItem.SelectionShape.valueOf(nbt.getStringOr("Shape", OctantItem.SelectionShape.CUBOID.name())); } catch (Exception ignored) {}

        int orientIdx = nbt.getIntOr("Orientation", 1);
        currentOrientation = OrientationMode.fromNbtIndex(orientIdx);

        isHollow = nbt.getBooleanOr("Hollow", false);
        isLayerMode = nbt.getBooleanOr("LayerMode", false);
        try { currentOrder = OctantItem.FillOrder.valueOf(nbt.getStringOr("FillOrder", OctantItem.FillOrder.DEFAULT.name())); } catch (Exception ignored) {}

        isLocked = nbt.getBooleanOr("Locked", false);
    }

    @Override
    protected void init() {
        // Vanilla-Masse: Knoepfe und Eingabefelder 20 hoch, Zeilen im 24er-Raster, 4 px Abstand.
        this.panelWidth = CONTENT_WIDTH + PANEL_PADDING * 2;
        this.columnCenterX = width - (panelWidth / 2) - 20;
        this.panelX = columnCenterX - (panelWidth / 2);
        int tallest = TITLE_HEIGHT + ROW_STEP * 4 + SUMMARY_HEIGHT + WIDGET_HEIGHT + PANEL_PADDING * 2;
        this.panelY = Math.max(4, (height - tallest) / 2);
        this.startY = panelY + TITLE_HEIGHT;
        this.rowStartX = panelX + PANEL_PADDING + LABEL_WIDTH;

        int contentLeft = panelX + PANEL_PADDING;
        int contentTop = startY;

        // 1. POS 1
        createRow(contentTop, pos1.getX(), pos1.getY(), pos1.getZ(), f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);
        // 2. POS 2
        createRow(contentTop + ROW_STEP, pos2.getX(), pos2.getY(), pos2.getZ(), f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);
        // 3. SIZE
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(contentTop + ROW_STEP * 2, w, h, d);

        int yControls = contentTop + ROW_STEP * 3;

        // Shape & Orientation
        int orientationWidth = 60;
        shapeButton = Button.builder(getShapeText(), b -> cycleShape())
            .bounds(contentLeft, yControls, CONTENT_WIDTH - orientationWidth - GAP, WIDGET_HEIGHT).build();
        addPageOneWidget(shapeButton);

        orientationButton = Button.builder(getOrientationText(), b -> cycleOrientation())
            .bounds(contentLeft + CONTENT_WIDTH - orientationWidth, yControls, orientationWidth, WIDGET_HEIGHT).build();
        addPageOneWidget(orientationButton);

        int half = (CONTENT_WIDTH - GAP) / 2;
        int yFill = contentTop;
        hollowButton = Button.builder(getHollowText(), b -> { isHollow = !isHollow; b.setMessage(getHollowText()); updateLocalAndSend(); })
            .bounds(contentLeft, yFill, half, WIDGET_HEIGHT).build();
        addPageTwoWidget(hollowButton);

        layerModeButton = Button.builder(getLayerText(), b -> { isLayerMode = !isLayerMode; b.setMessage(getLayerText()); updateLocalAndSend(); })
            .bounds(contentLeft + CONTENT_WIDTH - half, yFill, half, WIDGET_HEIGHT).build();
        addPageTwoWidget(layerModeButton);

        int yOrder = yFill + ROW_STEP;
        fillOrderButton = Button.builder(getOrderText(), b -> cycleOrder())
            .bounds(contentLeft, yOrder, CONTENT_WIDTH, WIDGET_HEIGHT).build();
        addPageTwoWidget(fillOrderButton);

        int yFigure = yOrder + ROW_STEP;
        figureToggleButton = Button.builder(getFigureText(), b -> toggleFigure())
            .bounds(contentLeft, yFigure, CONTENT_WIDTH, WIDGET_HEIGHT).build();
        addPageTwoWidget(figureToggleButton);

        this.hudSummaryY = yControls + ROW_STEP;

        this.yDoneSelection = hudSummaryY + SUMMARY_HEIGHT;
        this.yDoneSettings = yFigure + ROW_STEP;
        int yDone = yDoneSelection;

        int pageWidth = (CONTENT_WIDTH - WIDGET_HEIGHT - GAP * 2) / 2;
        int doneWidth = CONTENT_WIDTH - WIDGET_HEIGHT - GAP * 2 - pageWidth;

        lockButton = Button.builder(getLockIcon(), b -> { isLocked = !isLocked; b.setMessage(getLockIcon()); updateLocalAndSend(); })
            .bounds(contentLeft, yDone, WIDGET_HEIGHT, WIDGET_HEIGHT).build();
        addRenderableWidget(lockButton);

        pageButton = Button.builder(getPageButtonText(), b -> togglePage())
            .bounds(contentLeft + WIDGET_HEIGHT + GAP, yDone, pageWidth, WIDGET_HEIGHT).build();
        addRenderableWidget(pageButton);

        doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(contentLeft + CONTENT_WIDTH - doneWidth, yDone, doneWidth, WIDGET_HEIGHT).build());

        updateFooterLayout();

        updatePageVisibility();
    }

    private void createRow(int y, int v1, int v2, int v3, Consumer<EditBox> a1, Consumer<EditBox> a2, Consumer<EditBox> a3) {
        createControlGroup(rowStartX, y, v1, a1, false);
        createControlGroup(rowStartX + GROUP_WIDTH + GAP, y, v2, a2, false);
        createControlGroup(rowStartX + (GROUP_WIDTH + GAP) * 2, y, v3, a3, false);
    }
    private void createSizeRow(int y, int w, int h, int d) {
        createControlGroup(rowStartX, y, w, f -> wField = f, true);
        createControlGroup(rowStartX + GROUP_WIDTH + GAP, y, h, f -> hField = f, true);
        createControlGroup(rowStartX + (GROUP_WIDTH + GAP) * 2, y, d, f -> dField = f, true);
    }
    /** [-][Feld][+] in voller Vanilla-Hoehe statt der frueheren 10x7-Knoepfe uebereinander. */
    private void createControlGroup(int x, int y, int val, Consumer<EditBox> assigner, boolean isSize) {
        EditBox field = new EditBox(font, x + STEP_BUTTON_WIDTH + 1, y, FIELD_WIDTH, WIDGET_HEIGHT, Component.empty());
        addPageOneWidget(Button.builder(Component.literal("-"), b -> adjustField(field, -1, isSize))
                .bounds(x, y, STEP_BUTTON_WIDTH, WIDGET_HEIGHT).build());
        field.setValue(String.valueOf(val));
        field.setResponder(s -> { if (!isUpdating) { if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } });
        assigner.accept(field);
        addPageOneWidget(field);
        addPageOneWidget(Button.builder(Component.literal("+"), b -> adjustField(field, 1, isSize))
                .bounds(x + STEP_BUTTON_WIDTH + 1 + FIELD_WIDTH + 1, y, STEP_BUTTON_WIDTH, WIDGET_HEIGHT).build());
    }
    private void adjustField(EditBox field, int delta, boolean isSize) {
        try { int val = Integer.parseInt(field.getValue()) + delta; if (isSize && val < 1) val = 1; field.setValue(String.valueOf(val)); if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } catch (Exception e) { field.setValue("0"); }
    }

    private Component getLockIcon() { return isLocked ? Component.translatable("simplebuilding.gui.locked") : Component.translatable("simplebuilding.gui.unlocked"); }
    private Component getShapeText() { return currentShape.getText(); }
    private Component getOrientationText() { return Component.translatable("simplebuilding.gui.orientation", Component.translatable(currentOrientation.labelKey)); }
    private Component getHollowText() { return Component.translatable("simplebuilding.gui.hollow", isHollow ? "ON" : "OFF"); }
    private Component getLayerText() { return Component.translatable("simplebuilding.gui.layer", isLayerMode ? "ON" : "OFF"); }
    private Component getOrderText() { return Component.translatable("simplebuilding.gui.order", currentOrder.getText()); }
    private Component getFigureText() { return Component.translatable("simplebuilding.gui.figure", Component.translatable(ClientState.showOctantFigure ? "simplebuilding.gui.on" : "simplebuilding.gui.off")); }
    private Component getPageButtonText() { return currentPage == MenuPage.SELECTION ? Component.translatable("simplebuilding.gui.page.settings") : Component.translatable("simplebuilding.gui.page.selection"); }

    private void cycleShape() { currentShape = OctantItem.SelectionShape.values()[(currentShape.ordinal() + 1) % OctantItem.SelectionShape.values().length]; shapeButton.setMessage(getShapeText()); updateLocalAndSend(); }
    private void cycleOrientation() { currentOrientation = currentOrientation.next(); orientationButton.setMessage(getOrientationText()); updateLocalAndSend(); }
    private void cycleOrder() { currentOrder = OctantItem.FillOrder.values()[(currentOrder.ordinal() + 1) % OctantItem.FillOrder.values().length]; fillOrderButton.setMessage(getOrderText()); updateLocalAndSend(); }
    private void toggleFigure() { ClientState.showOctantFigure = !ClientState.showOctantFigure; figureToggleButton.setMessage(getFigureText()); }
    private void togglePage() {
        currentPage = currentPage == MenuPage.SELECTION ? MenuPage.SETTINGS : MenuPage.SELECTION;
        pageButton.setMessage(getPageButtonText());
        updatePageVisibility();
        updateFooterLayout();
        setFocused(null);
    }

    private void updateFooterLayout() {
        int footerY = currentPage == MenuPage.SELECTION ? yDoneSelection : yDoneSettings;
        lockButton.setY(footerY);
        pageButton.setY(footerY);
        doneButton.setY(footerY);
        panelHeight = (footerY + WIDGET_HEIGHT + PANEL_PADDING) - panelY;
    }

    private void updatePageVisibility() {
        boolean isSelection = currentPage == MenuPage.SELECTION;
        for (AbstractWidget widget : pageOneWidgets) {
            widget.visible = isSelection;
            widget.active = isSelection;
        }
        for (AbstractWidget widget : pageTwoWidgets) {
            widget.visible = !isSelection;
            widget.active = !isSelection;
        }
    }

    private <T extends AbstractWidget> T addPageOneWidget(T widget) {
        pageOneWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private <T extends AbstractWidget> T addPageTwoWidget(T widget) {
        pageTwoWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private void updatePos2FromSize() {
        if (isUpdating || x1Field == null) return;
        isUpdating = true;
        try {
            int x1 = parse(x1Field), y1 = parse(y1Field), z1 = parse(z1Field);
            int w = Math.max(1, parse(wField)), h = Math.max(1, parse(hField)), d = Math.max(1, parse(dField));
            x2Field.setValue(String.valueOf(x1 + w - 1)); y2Field.setValue(String.valueOf(y1 + h - 1)); z2Field.setValue(String.valueOf(z1 + d - 1));
            isUpdating = false; updateLocalAndSend();
        } catch (Exception e) { isUpdating = false; }
    }
    private void updateLocalAndSend() {
        if (x1Field == null) return;
        try {
            BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
            BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));
            CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtData.copyTag();
            nbt.putIntArray("Pos1", new int[]{p1.getX(), p1.getY(), p1.getZ()});
            nbt.putIntArray("Pos2", new int[]{p2.getX(), p2.getY(), p2.getZ()});
            nbt.putString("Shape", currentShape.name());
                nbt.putInt("Orientation", currentOrientation.nbtIndex);
            nbt.putBoolean("Locked", isLocked);
            nbt.putBoolean("Hollow", isHollow);
            nbt.putBoolean("LayerMode", isLayerMode);
            nbt.putString("FillOrder", currentOrder.name());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

            ClientNetworking.send(new OctantConfigurePayload(Optional.of(p1), Optional.of(p2), currentShape.name(), isLocked,
                    currentOrientation.nbtIndex,
                    isHollow, isLayerMode, currentOrder.name()));
        } catch (Exception ignored) {}
    }
    private int parse(EditBox f) { try { return Integer.parseInt(f.getValue()); } catch (Exception e) { return 0; } }

    // --- Input & Rendering ---

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (setMovementKeyPressed(input, true)) {
            return false;
        }

        // Hole den KeyCode für den Vergleich mit E und ESC
        int keyCode = input.key();

        // KORREKTUR: Übergib das 'input' Objekt direkt an matchesKey
        if (ClientState.settingsKey.matches(input)
                || keyCode == InputConstants.KEY_E
                || keyCode == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (setMovementKeyPressed(input, false)) {
            return false;
        }
        return super.keyReleased(input);
    }

    private boolean setMovementKeyPressed(KeyEvent input, boolean pressed) {
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        if (minecraft.options.keyUp.matches(input)) {
            minecraft.options.keyUp.setDown(pressed);
            return true;
        }
        if (minecraft.options.keyDown.matches(input)) {
            minecraft.options.keyDown.setDown(pressed);
            return true;
        }
        if (minecraft.options.keyLeft.matches(input)) {
            minecraft.options.keyLeft.setDown(pressed);
            return true;
        }
        if (minecraft.options.keyRight.matches(input)) {
            minecraft.options.keyRight.setDown(pressed);
            return true;
        }
        if (minecraft.options.keyJump.matches(input)) {
            minecraft.options.keyJump.setDown(pressed);
            return true;
        }
        if (minecraft.options.keyShift.matches(input)) {
            minecraft.options.keyShift.setDown(pressed);
            return true;
        }
        if (minecraft.options.keySprint.matches(input)) {
            minecraft.options.keySprint.setDown(pressed);
            return true;
        }

        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Die Welt bleibt sichtbar (man kann sich mit offenem Fenster bewegen) - keine Unschaerfe.
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Vanillas Popup-Hintergrund (nine-slice, wie PopupScreen) statt des lila Tooltip-Nachbaus.
        context.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL_SPRITE, panelX, panelY, panelWidth, panelHeight);
        // Titel wie bei Container-Bildschirmen: links, dunkelgrau, ohne Schatten.
        context.drawString(font, this.title, panelX + PANEL_PADDING, panelY + 7, LABEL_COLOR, false);

        if (currentPage == MenuPage.SELECTION) {
            drawInputLabels(context);
            drawInlineHudSummary(context);
        }

        super.render(context, mouseX, mouseY, delta);
        if (lockButton.isMouseOver(mouseX, mouseY)) context.setTooltipForNextFrame(font, Component.translatable("simplebuilding.gui.lock_tooltip"), mouseX, mouseY);
    }

    private void drawInputLabels(GuiGraphics context) {
        int textDy = (WIDGET_HEIGHT - 8) / 2;
        int labelX = panelX + PANEL_PADDING;
        context.drawString(font, Component.translatable("simplebuilding.gui.label.pos1"), labelX, startY + textDy, LABEL_COLOR, false);
        context.drawString(font, Component.translatable("simplebuilding.gui.label.pos2"), labelX, startY + ROW_STEP + textDy, LABEL_COLOR, false);
        context.drawString(font, Component.translatable("simplebuilding.gui.label.size"), labelX, startY + ROW_STEP * 2 + textDy, LABEL_COLOR, false);
    }

    private void drawInlineHudSummary(GuiGraphics context) {
        int boxX = panelX + PANEL_PADDING;
        int boxY = hudSummaryY;

        BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
        BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));
        int dx = Math.abs(p1.getX() - p2.getX()) + 1;
        int dy = Math.abs(p1.getY() - p2.getY()) + 1;
        int dz = Math.abs(p1.getZ() - p2.getZ()) + 1;

        context.drawString(font, Component.translatable("simplebuilding.gui.summary.pos1", p1.getX(), p1.getY(), p1.getZ()),
                boxX, boxY, LABEL_COLOR, false);
        context.drawString(font, Component.translatable("simplebuilding.gui.summary.pos2", p2.getX(), p2.getY(), p2.getZ()),
                boxX, boxY + 10, LABEL_COLOR, false);

        Component metric;
        Component dims = null;
        if (dy == 1 && (dx == 1 || dz == 1)) {
            metric = Component.translatable("simplebuilding.gui.summary.distance", Math.max(dx, dz));
        } else if (dy == 1) {
            metric = Component.translatable("simplebuilding.gui.summary.area", dx * dz);
            dims = Component.literal("(" + dx + " x " + dz + ")");
        } else {
            metric = Component.translatable("simplebuilding.gui.summary.volume", dx * dy * dz);
            dims = Component.literal("(" + dx + " x " + dy + " x " + dz + ")");
        }

        context.drawString(font, metric, boxX, boxY + 20, LABEL_COLOR, false);
        if (dims != null) {
            context.drawString(font, dims, boxX, boxY + 30, SECONDARY_COLOR, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
