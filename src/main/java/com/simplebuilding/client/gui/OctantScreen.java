package com.simplebuilding.client.gui;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantConfigurePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private final ItemStack stack;

    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget wField, hField, dField;

    private ButtonWidget shapeButton;
    private ButtonWidget lockButton;
    private ButtonWidget orientationButton;

    // Fill Settings Buttons
    private ButtonWidget hollowButton;
    private ButtonWidget layerModeButton;
    private ButtonWidget fillOrderButton;

    private OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
    private Direction.Axis currentOrientation = Direction.Axis.Y;
    private boolean isHollow = false;
    private boolean isLayerMode = false;
    private OctantItem.FillOrder currentOrder = OctantItem.FillOrder.DEFAULT;

    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private boolean isLocked = false;
    private boolean isUpdating = false;

    private int columnCenterX;
    private int startY;
    private final int rowSpacing = 28;
    private final int fieldOffsetY = 10;

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

        int orientIdx = nbt.getInt("Orientation", 1); // 0=X, 1=Y, 2=Z
        if (orientIdx == 0) currentOrientation = Direction.Axis.X;
        else if (orientIdx == 2) currentOrientation = Direction.Axis.Z;
        else currentOrientation = Direction.Axis.Y;

        isHollow = nbt.getBoolean("Hollow", false);
        isLayerMode = nbt.getBoolean("LayerMode", false);
        try { currentOrder = OctantItem.FillOrder.valueOf(nbt.getString("FillOrder", OctantItem.FillOrder.DEFAULT.name())); } catch (Exception ignored) {}

        isLocked = nbt.getBoolean("Locked", false);
    }

    @Override
    protected void init() {
        int menuWidth = 160;
        this.columnCenterX = width - (menuWidth / 2) - 20;
        // Taller menu now
        int totalHeight = (rowSpacing * 3) + 25 + 25 + 25 + 25;
        this.startY = Math.max(10, (height - totalHeight) / 2);

        // 1. POS 1
        createRow(columnCenterX, startY + fieldOffsetY, pos1.getX(), pos1.getY(), pos1.getZ(), f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);
        // 2. POS 2
        createRow(columnCenterX, startY + fieldOffsetY + rowSpacing, pos2.getX(), pos2.getY(), pos2.getZ(), f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);
        // 3. SIZE
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(columnCenterX, startY + fieldOffsetY + rowSpacing * 2, w, h, d);

        int yControls = startY + fieldOffsetY + rowSpacing * 3 + 5;

        // Shape & Orientation
        shapeButton = ButtonWidget.builder(getShapeText(), b -> cycleShape())
                .dimensions(columnCenterX - 100, yControls, 125, 20).build();
        addDrawableChild(shapeButton);

        orientationButton = ButtonWidget.builder(getOrientationText(), b -> cycleOrientation())
                .dimensions(columnCenterX + 30, yControls, 50, 20).build();
        addDrawableChild(orientationButton);

        // Fill Settingse
        int yFill = yControls + 25;
        hollowButton = ButtonWidget.builder(getHollowText(), b -> { isHollow = !isHollow; b.setMessage(getHollowText()); updateLocalAndSend(); })
                .dimensions(columnCenterX - 100, yFill, 75, 20).build();
        addDrawableChild(hollowButton);

        layerModeButton = ButtonWidget.builder(getLayerText(), b -> { isLayerMode = !isLayerMode; b.setMessage(getLayerText()); updateLocalAndSend(); })
                .dimensions(columnCenterX - 20, yFill, 100, 20).build();
        addDrawableChild(layerModeButton);

        int yOrder = yFill + 25;
        fillOrderButton = ButtonWidget.builder(getOrderText(), b -> cycleOrder())
                .dimensions(columnCenterX - 100, yOrder, 180, 20).build();
        addDrawableChild(fillOrderButton);

        int yDone = yOrder + 25;

        lockButton = ButtonWidget.builder(getLockIcon(), b -> { isLocked = !isLocked; b.setMessage(getLockIcon()); updateLocalAndSend(); })
                .dimensions(columnCenterX - 100, yDone, 25, 20).build();
        addDrawableChild(lockButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("simplebuilding.gui.close"), b -> close())
                .dimensions(columnCenterX - 70, yDone, 150, 20).build());
    }

    private void createRow(int cX, int y, int v1, int v2, int v3, Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
        int groupWidth = 50;
        int startX = cX - (int)(groupWidth * 1.5) - 5;
        createControlGroup(startX, y, v1, a1, false);
        createControlGroup(startX + groupWidth + 5, y, v2, a2, false);
        createControlGroup(startX + (groupWidth + 5) * 2, y, v3, a3, false);
    }
    private void createSizeRow(int cX, int y, int w, int h, int d) {
        int groupWidth = 50;
        int startX = cX - (int)(groupWidth * 1.5) - 5;
        createControlGroup(startX, y, w, f -> wField = f, true);
        createControlGroup(startX + groupWidth + 5, y, h, f -> hField = f, true);
        createControlGroup(startX + (groupWidth + 5) * 2, y, d, f -> dField = f, true);
    }
    private void createControlGroup(int x, int y, int val, Consumer<TextFieldWidget> assigner, boolean isSize) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 30, 16, Text.empty());
        field.setText(String.valueOf(val));
        field.setChangedListener(s -> { if (!isUpdating) { if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } });
        assigner.accept(field); addDrawableChild(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> adjustField(field, 1, isSize)).dimensions(x + 31, y, 12, 8).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> adjustField(field, -1, isSize)).dimensions(x + 31, y + 8, 12, 8).build());
    }
    private void adjustField(TextFieldWidget field, int delta, boolean isSize) {
        try { int val = Integer.parseInt(field.getText()) + delta; if (isSize && val < 1) val = 1; field.setText(String.valueOf(val)); if (isSize) updatePos2FromSize(); else updateLocalAndSend(); } catch (Exception e) { field.setText("0"); }
    }

    private Text getLockIcon() { return isLocked ? Text.translatable("simplebuilding.gui.locked") : Text.translatable("simplebuilding.gui.unlocked"); }
    private Text getShapeText() { return currentShape.getText(); }
    private Text getOrientationText() { return Text.translatable("simplebuilding.gui.orientation", currentOrientation.name()); }
    private Text getHollowText() { return Text.translatable("simplebuilding.gui.hollow", isHollow ? "ON" : "OFF"); }
    private Text getLayerText() { return Text.translatable("simplebuilding.gui.layer", isLayerMode ? "ON" : "OFF"); }
    private Text getOrderText() { return Text.translatable("simplebuilding.gui.order", currentOrder.getText()); }

    private void cycleShape() { currentShape = OctantItem.SelectionShape.values()[(currentShape.ordinal() + 1) % OctantItem.SelectionShape.values().length]; shapeButton.setMessage(getShapeText()); updateLocalAndSend(); }
    private void cycleOrientation() { currentOrientation = (currentOrientation == Direction.Axis.X) ? Direction.Axis.Y : (currentOrientation == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.X); orientationButton.setMessage(getOrientationText()); updateLocalAndSend(); }
    private void cycleOrder() { currentOrder = OctantItem.FillOrder.values()[(currentOrder.ordinal() + 1) % OctantItem.FillOrder.values().length]; fillOrderButton.setMessage(getOrderText()); updateLocalAndSend(); }

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
            nbt.putInt("Orientation", currentOrientation == Direction.Axis.X ? 0 : (currentOrientation == Direction.Axis.Y ? 1 : 2));
            nbt.putBoolean("Locked", isLocked);
            nbt.putBoolean("Hollow", isHollow);
            nbt.putBoolean("LayerMode", isLayerMode);
            nbt.putString("FillOrder", currentOrder.name());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            ClientPlayNetworking.send(new OctantConfigurePayload(Optional.of(p1), Optional.of(p2), currentShape.name(), isLocked,
                    currentOrientation == Direction.Axis.X ? 0 : (currentOrientation == Direction.Axis.Y ? 1 : 2),
                    isHollow, isLayerMode, currentOrder.name()));
        } catch (Exception ignored) {}
    }
    private int parse(TextFieldWidget f) { try { return Integer.parseInt(f.getText()); } catch (Exception e) { return 0; } }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x15000000);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.title"), columnCenterX, startY - 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.pos1"), columnCenterX, startY, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.pos2"), columnCenterX, startY + rowSpacing, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.size"), columnCenterX, startY + rowSpacing * 2, 0x55FFFF);
        if (lockButton.isMouseOver(mouseX, mouseY)) context.drawTooltip(textRenderer, Text.translatable("simplebuilding.gui.lock_tooltip"), mouseX, mouseY);
    }
}
