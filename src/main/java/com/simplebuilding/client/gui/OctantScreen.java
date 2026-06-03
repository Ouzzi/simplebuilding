package com.simplebuilding.client.gui;

import com.simplebuilding.SimplebuildingClient;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantConfigurePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private static final int PANEL_BG = 0xE0100010;
    private static final int PANEL_BORDER_START = 0xF03f0073;
    private static final int PANEL_BORDER_END = 0xF0250061;

    private final ItemStack stack;

    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget wField, hField, dField;

    private ButtonWidget shapeButton;
    private ButtonWidget lockButton;
    private ButtonWidget orientationButton;
    private ButtonWidget figureToggleButton;
    private ButtonWidget pageButton;
    private ButtonWidget doneButton;

    // Fill Settings Buttons
    private ButtonWidget hollowButton;
    private ButtonWidget layerModeButton;
    private ButtonWidget fillOrderButton;

    private final List<ClickableWidget> pageOneWidgets = new ArrayList<>();
    private final List<ClickableWidget> pageTwoWidgets = new ArrayList<>();

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

    private static final int GROUP_WIDTH = 44;
    private static final int GROUP_GAP = 5;
    private static final int ROW_SPACING = 18;
    private static final int FIELD_OFFSET_Y = 9;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_STEP = 19;
    private static final int LOCK_BUTTON_WIDTH = 25;
    private static final int PAGE_BUTTON_WIDTH = 70;
    private static final int DONE_BUTTON_WIDTH = 75;

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
        super(Text.translatable("simplebuilding.gui.title"));
        this.stack = stack;
        loadDataFromStack();
    }

    private void loadDataFromStack() {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        if (nbt.contains("Pos1")) nbt.getIntArray("Pos1").ifPresent(p -> { if(p.length==3) pos1 = new BlockPos(p[0], p[1], p[2]); });
        if (nbt.contains("Pos2")) nbt.getIntArray("Pos2").ifPresent(p -> { if(p.length==3) pos2 = new BlockPos(p[0], p[1], p[2]); });

        if (nbt.contains("Shape")) try { currentShape = OctantItem.SelectionShape.valueOf(nbt.getString("Shape", OctantItem.SelectionShape.CUBOID.name())); } catch (Exception ignored) {}

        int orientIdx = nbt.getInt("Orientation", 1);
        currentOrientation = OrientationMode.fromNbtIndex(orientIdx);

        isHollow = nbt.getBoolean("Hollow", false);
        isLayerMode = nbt.getBoolean("LayerMode", false);
        try { currentOrder = OctantItem.FillOrder.valueOf(nbt.getString("FillOrder", OctantItem.FillOrder.DEFAULT.name())); } catch (Exception ignored) {}

        isLocked = nbt.getBoolean("Locked", false);
    }

    @Override
    protected void init() {
        int panelPaddingX = 8;
        int labelColumnWidth = 20;
        int selectionRowWidth = labelColumnWidth + (GROUP_WIDTH * 3) + (GROUP_GAP * 2);
        int splitButtonRowWidth = (88 * 2) + 4;
        int bottomRowWidth = LOCK_BUTTON_WIDTH + 5 + PAGE_BUTTON_WIDTH + 5 + DONE_BUTTON_WIDTH;
        int contentWidth = Math.max(Math.max(selectionRowWidth, 180), Math.max(splitButtonRowWidth, bottomRowWidth));

        this.panelWidth = contentWidth + (panelPaddingX * 2);
        this.columnCenterX = width - (panelWidth / 2) - 20;

        int totalHeight = 190;
        this.startY = Math.max(16, (height - totalHeight) / 2) + 10;
        this.panelX = columnCenterX - (panelWidth / 2);
        this.panelY = startY - 20;
        this.rowStartX = panelX + panelPaddingX + labelColumnWidth;

        int contentLeft = panelX + panelPaddingX;
        int contentTop = startY + FIELD_OFFSET_Y;

        // 1. POS 1
        createRow(contentTop, pos1.getX(), pos1.getY(), pos1.getZ(), f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);
        // 2. POS 2
        createRow(contentTop + ROW_SPACING, pos2.getX(), pos2.getY(), pos2.getZ(), f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);
        // 3. SIZE
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(contentTop + ROW_SPACING * 2, w, h, d);

        int yControls = contentTop + ROW_SPACING * 3 + 2;

        // Shape & Orientation
        shapeButton = ButtonWidget.builder(getShapeText(), b -> cycleShape())
            .dimensions(contentLeft, yControls, 125, BUTTON_HEIGHT).build();
        addPageOneWidget(shapeButton);

        orientationButton = ButtonWidget.builder(getOrientationText(), b -> cycleOrientation())
            .dimensions(contentLeft + 130, yControls, 50, BUTTON_HEIGHT).build();
        addPageOneWidget(orientationButton);

        int yFill = contentTop;
        hollowButton = ButtonWidget.builder(getHollowText(), b -> { isHollow = !isHollow; b.setMessage(getHollowText()); updateLocalAndSend(); })
            .dimensions(contentLeft, yFill, 88, BUTTON_HEIGHT).build();
        addPageTwoWidget(hollowButton);

        layerModeButton = ButtonWidget.builder(getLayerText(), b -> { isLayerMode = !isLayerMode; b.setMessage(getLayerText()); updateLocalAndSend(); })
            .dimensions(contentLeft + 92, yFill, 88, BUTTON_HEIGHT).build();
        addPageTwoWidget(layerModeButton);

        int yOrder = yFill + BUTTON_STEP;
        fillOrderButton = ButtonWidget.builder(getOrderText(), b -> cycleOrder())
            .dimensions(contentLeft, yOrder, 180, BUTTON_HEIGHT).build();
        addPageTwoWidget(fillOrderButton);

        int yFigure = yOrder + BUTTON_STEP;
        figureToggleButton = ButtonWidget.builder(getFigureText(), b -> toggleFigure())
            .dimensions(contentLeft, yFigure, 180, BUTTON_HEIGHT).build();
        addPageTwoWidget(figureToggleButton);

        this.hudSummaryY = yControls + BUTTON_STEP + 2;

        int pageOneBottom = hudSummaryY + 30;
        int pageTwoBottom = yFigure + BUTTON_HEIGHT;
        this.yDoneSelection = pageOneBottom + 8;
        this.yDoneSettings = pageTwoBottom + 8;
        int yDone = yDoneSelection;

        int bottomX = contentLeft;

        lockButton = ButtonWidget.builder(getLockIcon(), b -> { isLocked = !isLocked; b.setMessage(getLockIcon()); updateLocalAndSend(); })
            .dimensions(bottomX, yDone, LOCK_BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addDrawableChild(lockButton);

        pageButton = ButtonWidget.builder(getPageButtonText(), b -> togglePage())
            .dimensions(bottomX + LOCK_BUTTON_WIDTH + 5, yDone, PAGE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addDrawableChild(pageButton);

        doneButton = addDrawableChild(ButtonWidget.builder(Text.translatable("simplebuilding.gui.close"), b -> close())
            .dimensions(bottomX + LOCK_BUTTON_WIDTH + 5 + PAGE_BUTTON_WIDTH + 5, yDone, DONE_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        updateFooterLayout();

        updatePageVisibility();
    }

        private void createRow(int y, int v1, int v2, int v3, Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
        createControlGroup(rowStartX, y, v1, a1, false);
        createControlGroup(rowStartX + GROUP_WIDTH + GROUP_GAP, y, v2, a2, false);
        createControlGroup(rowStartX + (GROUP_WIDTH + GROUP_GAP) * 2, y, v3, a3, false);
    }
        private void createSizeRow(int y, int w, int h, int d) {
        createControlGroup(rowStartX, y, w, f -> wField = f, true);
        createControlGroup(rowStartX + GROUP_WIDTH + GROUP_GAP, y, h, f -> hField = f, true);
        createControlGroup(rowStartX + (GROUP_WIDTH + GROUP_GAP) * 2, y, d, f -> dField = f, true);
    }
    private void createControlGroup(int x, int y, int val, Consumer<TextFieldWidget> assigner, boolean isSize) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 24, 14, Text.empty());
        field.setText(String.valueOf(val));
        field.setChangedListener(s -> { if (!isUpdating) { if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } });
        assigner.accept(field);
        addPageOneWidget(field);
        addPageOneWidget(ButtonWidget.builder(Text.literal("+"), b -> adjustField(field, 1, isSize)).dimensions(x + 25, y, 10, 7).build());
        addPageOneWidget(ButtonWidget.builder(Text.literal("-"), b -> adjustField(field, -1, isSize)).dimensions(x + 25, y + 7, 10, 7).build());
    }
    private void adjustField(TextFieldWidget field, int delta, boolean isSize) {
        try { int val = Integer.parseInt(field.getText()) + delta; if (isSize && val < 1) val = 1; field.setText(String.valueOf(val)); if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } catch (Exception e) { field.setText("0"); }
    }

    private Text getLockIcon() { return isLocked ? Text.translatable("simplebuilding.gui.locked") : Text.translatable("simplebuilding.gui.unlocked"); }
    private Text getShapeText() { return currentShape.getText(); }
    private Text getOrientationText() { return Text.translatable("simplebuilding.gui.orientation", Text.translatable(currentOrientation.labelKey)); }
    private Text getHollowText() { return Text.translatable("simplebuilding.gui.hollow", isHollow ? "ON" : "OFF"); }
    private Text getLayerText() { return Text.translatable("simplebuilding.gui.layer", isLayerMode ? "ON" : "OFF"); }
    private Text getOrderText() { return Text.translatable("simplebuilding.gui.order", currentOrder.getText()); }
    private Text getFigureText() { return Text.translatable("simplebuilding.gui.figure", Text.translatable(SimplebuildingClient.showHighlights ? "simplebuilding.gui.on" : "simplebuilding.gui.off")); }
    private Text getPageButtonText() { return currentPage == MenuPage.SELECTION ? Text.literal(">> Settings") : Text.literal("<< Selection"); }
    private Text getPageLabelText() { return currentPage == MenuPage.SELECTION ? Text.literal("Page 1/2: Selection") : Text.literal("Page 2/2: Fill Settings"); }

    private void cycleShape() { currentShape = OctantItem.SelectionShape.values()[(currentShape.ordinal() + 1) % OctantItem.SelectionShape.values().length]; shapeButton.setMessage(getShapeText()); updateLocalAndSend(); }
    private void cycleOrientation() { currentOrientation = currentOrientation.next(); orientationButton.setMessage(getOrientationText()); updateLocalAndSend(); }
    private void cycleOrder() { currentOrder = OctantItem.FillOrder.values()[(currentOrder.ordinal() + 1) % OctantItem.FillOrder.values().length]; fillOrderButton.setMessage(getOrderText()); updateLocalAndSend(); }
    private void toggleFigure() { SimplebuildingClient.showHighlights = !SimplebuildingClient.showHighlights; figureToggleButton.setMessage(getFigureText()); }
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
        panelHeight = (footerY + BUTTON_HEIGHT + 8) - panelY;
    }

    private void updatePageVisibility() {
        boolean isSelection = currentPage == MenuPage.SELECTION;
        for (ClickableWidget widget : pageOneWidgets) {
            widget.visible = isSelection;
            widget.active = isSelection;
        }
        for (ClickableWidget widget : pageTwoWidgets) {
            widget.visible = !isSelection;
            widget.active = !isSelection;
        }
    }

    private <T extends ClickableWidget> T addPageOneWidget(T widget) {
        pageOneWidgets.add(widget);
        return addDrawableChild(widget);
    }

    private <T extends ClickableWidget> T addPageTwoWidget(T widget) {
        pageTwoWidgets.add(widget);
        return addDrawableChild(widget);
    }

    private void updatePos2FromSize() {
        if (isUpdating || x1Field == null) return;
        isUpdating = true;
        try {
            int x1 = parse(x1Field), y1 = parse(y1Field), z1 = parse(z1Field);
            int w = Math.max(1, parse(wField)), h = Math.max(1, parse(hField)), d = Math.max(1, parse(dField));
            x2Field.setText(String.valueOf(x1 + w - 1)); y2Field.setText(String.valueOf(y1 + h - 1)); z2Field.setText(String.valueOf(z1 + d - 1));
            isUpdating = false; updateLocalAndSend();
        } catch (Exception e) { isUpdating = false; }
    }
    private void updateLocalAndSend() {
        if (x1Field == null) return;
        try {
            BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
            BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));
            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putIntArray("Pos1", new int[]{p1.getX(), p1.getY(), p1.getZ()});
            nbt.putIntArray("Pos2", new int[]{p2.getX(), p2.getY(), p2.getZ()});
            nbt.putString("Shape", currentShape.name());
                nbt.putInt("Orientation", currentOrientation.nbtIndex);
            nbt.putBoolean("Locked", isLocked);
            nbt.putBoolean("Hollow", isHollow);
            nbt.putBoolean("LayerMode", isLayerMode);
            nbt.putString("FillOrder", currentOrder.name());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            ClientPlayNetworking.send(new OctantConfigurePayload(Optional.of(p1), Optional.of(p2), currentShape.name(), isLocked,
                    currentOrientation.nbtIndex,
                    isHollow, isLayerMode, currentOrder.name()));
        } catch (Exception ignored) {}
    }
    private int parse(TextFieldWidget f) { try { return Integer.parseInt(f.getText()); } catch (Exception e) { return 0; } }

    // --- Input & Rendering ---

    @Override
    public boolean keyPressed(KeyInput input) {
        if (setMovementKeyPressed(input, true)) {
            return false;
        }

        // Hole den KeyCode für den Vergleich mit E und ESC
        int keyCode = input.key();

        // KORREKTUR: Übergib das 'input' Objekt direkt an matchesKey
        if (com.simplebuilding.SimplebuildingClient.settingsKey.matchesKey(input)
                || keyCode == GLFW.GLFW_KEY_E
                || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (setMovementKeyPressed(input, false)) {
            return false;
        }
        return super.keyReleased(input);
    }

    private boolean setMovementKeyPressed(KeyInput input, boolean pressed) {
        if (client == null || client.options == null) {
            return false;
        }

        if (client.options.forwardKey.matchesKey(input)) {
            client.options.forwardKey.setPressed(pressed);
            return true;
        }
        if (client.options.backKey.matchesKey(input)) {
            client.options.backKey.setPressed(pressed);
            return true;
        }
        if (client.options.leftKey.matchesKey(input)) {
            client.options.leftKey.setPressed(pressed);
            return true;
        }
        if (client.options.rightKey.matchesKey(input)) {
            client.options.rightKey.setPressed(pressed);
            return true;
        }
        if (client.options.jumpKey.matchesKey(input)) {
            client.options.jumpKey.setPressed(pressed);
            return true;
        }
        if (client.options.sneakKey.matchesKey(input)) {
            client.options.sneakKey.setPressed(pressed);
            return true;
        }
        if (client.options.sprintKey.matchesKey(input)) {
            client.options.sprintKey.setPressed(pressed);
            return true;
        }

        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep transparent world background.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x15000000);

        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, PANEL_BG);
        context.fill(panelX + 1, panelY, panelX + panelWidth - 1, panelY + 1, PANEL_BG);
        context.fill(panelX + 1, panelY - 1, panelX + panelWidth - 1, panelY, PANEL_BG);
        context.fill(panelX + 1, panelY + panelHeight, panelX + panelWidth - 1, panelY + panelHeight + 1, PANEL_BG);
        context.fill(panelX - 1, panelY + 1, panelX, panelY + panelHeight - 1, PANEL_BG);
        context.fill(panelX + panelWidth, panelY + 1, panelX + panelWidth + 1, panelY + panelHeight - 1, PANEL_BG);
        context.fillGradient(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + 1, PANEL_BG, PANEL_BG);
        context.fillGradient(panelX, panelY, panelX + 1, panelY + 1, PANEL_BG, PANEL_BG);
        context.fillGradient(panelX + panelWidth - 1, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, PANEL_BG, PANEL_BG);
        context.fillGradient(panelX, panelY + panelHeight - 1, panelX + 1, panelY + panelHeight, PANEL_BG, PANEL_BG);

        context.fillGradient(panelX + 1, panelY, panelX + panelWidth - 1, panelY + 1, PANEL_BORDER_START, PANEL_BORDER_START);
        context.fillGradient(panelX + 1, panelY + panelHeight - 1, panelX + panelWidth - 1, panelY + panelHeight, PANEL_BORDER_END, PANEL_BORDER_END);
        context.fillGradient(panelX, panelY + 1, panelX + 1, panelY + panelHeight - 1, PANEL_BORDER_START, PANEL_BORDER_END);
        context.fillGradient(panelX + panelWidth - 1, panelY + 1, panelX + panelWidth, panelY + panelHeight - 1, PANEL_BORDER_START, PANEL_BORDER_END);

        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.title"), columnCenterX, panelY + 6, 0xFF66FFFF);
        // Requested: hide page subtitle line for a cleaner header.

        if (currentPage == MenuPage.SELECTION) {
            drawInputLabels(context);
            drawInlineHudSummary(context);
        }

        super.render(context, mouseX, mouseY, delta);
        if (lockButton.isMouseOver(mouseX, mouseY)) context.drawTooltip(textRenderer, Text.translatable("simplebuilding.gui.lock_tooltip"), mouseX, mouseY);
    }

    private void drawInputLabels(DrawContext context) {
        int row1Y = startY + FIELD_OFFSET_Y;
        int row2Y = row1Y + ROW_SPACING;
        int row3Y = row2Y + ROW_SPACING;

        context.drawTextWithShadow(textRenderer, Text.literal("P1"), panelX + 8, row1Y + 3, 0xFFFFD15A);
        context.drawTextWithShadow(textRenderer, Text.literal("P2"), panelX + 8, row2Y + 3, 0xFFD4E56A);
        context.drawTextWithShadow(textRenderer, Text.literal("SZ"), panelX + 8, row3Y + 3, 0xFF55FFFF);
    }

    private void drawInlineHudSummary(DrawContext context) {
        int boxX = panelX + 8;
        int boxY = hudSummaryY;
        int boxW = panelWidth - 16;
        int boxH = 40;

        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x30000000);

        BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
        BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));
        int dx = Math.abs(p1.getX() - p2.getX()) + 1;
        int dy = Math.abs(p1.getY() - p2.getY()) + 1;
        int dz = Math.abs(p1.getZ() - p2.getZ()) + 1;

        context.drawTextWithShadow(textRenderer,
                Text.literal("Pos 1: " + p1.getX() + ", " + p1.getY() + ", " + p1.getZ()),
            boxX + 4, boxY + 3, 0xFFFFD15A);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Pos 2: " + p2.getX() + ", " + p2.getY() + ", " + p2.getZ()),
            boxX + 4, boxY + 12, 0xFFD4E56A);

        String metric;
        String dims = null;
        if (dy == 1 && (dx == 1 || dz == 1)) {
            metric = "Distance: " + Math.max(dx, dz) + " blocks";
        } else if (dy == 1) {
            metric = "Area: " + (dx * dz) + " blocks^2";
            dims = "(" + dx + " x " + dz + ")";
        } else {
            metric = "Volume: " + (dx * dy * dz) + " blocks^3";
            dims = "(" + dx + " x " + dy + " x " + dz + ")";
        }

        context.drawTextWithShadow(textRenderer, Text.literal(metric), boxX + 4, boxY + 21, 0xFFFFD15A);
        if (dims != null) {
            context.drawTextWithShadow(textRenderer, Text.literal(dims), boxX + 4, boxY + 30, 0xFFB0B0B0);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
