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

import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private final ItemStack stack;

    // UI Komponenten
    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget wField, hField, dField;
    private ButtonWidget shapeButton;
    private ButtonWidget lockButton;

    // Daten
    private OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private boolean isLocked = false;
    private boolean isUpdating = false;

    // Layout
    private int columnCenterX;
    private int startY;
    private final int rowSpacing = 32; // Sehr kompakt
    private final int fieldOffsetY = 10; // Abstand Text zu Feld

    public OctantScreen(ItemStack stack) {
        super(Text.translatable("simplebuilding.gui.title"));
        this.stack = stack;
        loadDataFromStack();
    }

    private void loadDataFromStack() {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        if (nbt.contains("Pos1")) {
            nbt.getIntArray("Pos1").ifPresent(p -> { if(p.length==3) pos1 = new BlockPos(p[0], p[1], p[2]); });
        }
        if (nbt.contains("Pos2")) {
            nbt.getIntArray("Pos2").ifPresent(p -> { if(p.length==3) pos2 = new BlockPos(p[0], p[1], p[2]); });
        }
        if (nbt.contains("Shape")) {
            try { currentShape = OctantItem.SelectionShape.valueOf(nbt.getString("Shape", "CUBOID")); } catch (Exception ignored) {}
        }
        isLocked = nbt.getBoolean("Locked", false);
    }

    @Override
    protected void init() {
        // --- LAYOUT RECHTS ---
        // Wir nutzen das rechte Drittel.
        // Breite des Menü-Bereichs ca. 150px
        int menuWidth = 160;
        this.columnCenterX = width - (menuWidth / 2) - 20; // 20px Abstand vom rechten Rand

        // Vertikal zentrieren
        int totalHeight = (rowSpacing * 3) + 25 + 25; // 3 Gruppen + ShapeZeile + FertigButton
        this.startY = (height - totalHeight) / 2;

        // 1. POS 1 (Start)
        int y1 = startY + fieldOffsetY;
        createRow(columnCenterX, y1, pos1.getX(), pos1.getY(), pos1.getZ(),
                f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);

        // 2. POS 2 (Ende)
        int y2 = y1 + rowSpacing;
        createRow(columnCenterX, y2, pos2.getX(), pos2.getY(), pos2.getZ(),
                f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);

        // 3. SIZE (Größe)
        int y3 = y2 + rowSpacing;
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(columnCenterX, y3, w, h, d);

        // 4. SHAPE & LOCK (Nebeneinander)
        int yShape = y3 + rowSpacing + 5;

        // Shape Button (Links in der Gruppe)
        shapeButton = ButtonWidget.builder(getShapeText(), button -> cycleShape())
                .dimensions(columnCenterX - 60, yShape, 95, 20).build();
        addDrawableChild(shapeButton);

        // Lock Button (Rechts daneben, kleines Icon)
        lockButton = ButtonWidget.builder(getLockIcon(), button -> {
            isLocked = !isLocked;
            button.setMessage(getLockIcon());
            updateLocalAndSend();
        }).dimensions(columnCenterX + 40, yShape, 20, 20).build();
        addDrawableChild(lockButton);

        // 5. FERTIG BUTTON (Ganz unten)
        int yDone = yShape + 25;
        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), button -> close())
                .dimensions(columnCenterX - 60, yDone, 120, 20).build());
    }

    private void createRow(int cX, int y, int v1, int v2, int v3,
                           Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
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
        // Feld
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 30, 16, Text.empty());
        field.setText(String.valueOf(val));
        field.setTextPredicate(s -> s.matches("-?\\d*"));
        field.setChangedListener(s -> {
            if (!isUpdating) {
                if (isSize) updatePos2FromSize();
                else updateLocalAndSend();
            }
        });
        assigner.accept(field);
        addDrawableChild(field);

        // Mini Buttons rechts daneben gestapelt
        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> adjustField(field, 1, isSize))
                .dimensions(x + 31, y, 12, 8).build();
        addDrawableChild(plus);

        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> adjustField(field, -1, isSize))
                .dimensions(x + 31, y + 8, 12, 8).build();
        addDrawableChild(minus);
    }

    private void adjustField(TextFieldWidget field, int delta, boolean isSize) {
        try {
            int val = Integer.parseInt(field.getText());
            val += delta;
            if (isSize && val < 1) val = 1;
            field.setText(String.valueOf(val));
            if (isSize) updatePos2FromSize();
            else updateLocalAndSend();
        } catch (NumberFormatException ignored) {
            field.setText("0");
        }
    }

    private Text getLockIcon() {
        // Einfaches Schloss Icon als Text. Rot = Zu, Grün = Offen
        return isLocked ? Text.literal("🔒").formatted(Formatting.RED) : Text.literal("🔓").formatted(Formatting.GREEN);
    }

    private Text getShapeText() {
        return Text.literal("Form: " + currentShape.getName()).formatted(Formatting.AQUA);
    }

    private void updatePos2FromSize() {
        if (isUpdating || x1Field == null) return;
        isUpdating = true;
        try {
            int x1 = parse(x1Field); int y1 = parse(y1Field); int z1 = parse(z1Field);
            int w = Math.max(1, parse(wField));
            int h = Math.max(1, parse(hField));
            int d = Math.max(1, parse(dField));

            x2Field.setText(String.valueOf(x1 + w - 1));
            y2Field.setText(String.valueOf(y1 + h - 1));
            z2Field.setText(String.valueOf(z1 + d - 1));

            isUpdating = false;
            updateLocalAndSend();
        } catch (Exception e) { isUpdating = false; }
    }

    private void updateLocalAndSend() {
        if (x1Field == null || wField == null) return;
        try {
            BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
            BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));

            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putIntArray("Pos1", new int[]{p1.getX(), p1.getY(), p1.getZ()});
            nbt.putIntArray("Pos2", new int[]{p2.getX(), p2.getY(), p2.getZ()});
            nbt.putString("Shape", currentShape.name());
            nbt.putBoolean("Locked", isLocked);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            ClientPlayNetworking.send(new OctantConfigurePayload(
                    Optional.of(p1), Optional.of(p2), currentShape.name(), isLocked
            ));
        } catch (Exception ignored) {}
    }

    private int parse(TextFieldWidget f) {
        try { return Integer.parseInt(f.getText()); } catch (Exception e) { return 0; }
    }

    private void cycleShape() {
        OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
        int index = (currentShape.ordinal() + 1) % values.length;
        currentShape = values[index];
        shapeButton.setMessage(getShapeText());
        updateLocalAndSend();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // HINTERGRUND: Fast unsichtbar (0x10... ist sehr transparent)
        // Wir füllen den ganzen Screen, damit der Hintergrund einheitlich etwas abgedunkelt ist,
        // aber so schwach, dass man fast alles sieht.
        context.fill(0, 0, width, height, 0x15000000);

        // Optional: Einen etwas dunkleren Streifen nur rechts hinter dem Menü?
        // context.fill(width - 180, 0, width, height, 0x20000000); // Wenn gewünscht, einkommentieren.

        super.render(context, mouseX, mouseY, delta);

        // TEXTE RENDERN (Überschriften)
        // Wir rendern sie NACH super.render, damit sie sicher oben drauf sind.

        // Titel
        context.drawCenteredTextWithShadow(textRenderer, "Octant", columnCenterX, startY - 15, 0xFFFFFF);

        // Labels direkt über den Gruppen
        // Koordinaten müssen exakt zu init() passen
        int y1 = startY;
        context.drawCenteredTextWithShadow(textRenderer, "Start (1)", columnCenterX, y1, 0xAAAAAA);

        int y2 = y1 + rowSpacing;
        context.drawCenteredTextWithShadow(textRenderer, "Ende (2)", columnCenterX, y2, 0xAAAAAA);

        int y3 = y2 + rowSpacing;
        context.drawCenteredTextWithShadow(textRenderer, "Größe", columnCenterX, y3, 0x55FFFF);

        // Tooltip für Lock
        if (lockButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.literal(isLocked ? "Entsperren" : "Sperren"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Scroll-Support für alle Felder
        if (handleScroll(x1Field, mouseX, mouseY, verticalAmount)) return true;
        if (handleScroll(y1Field, mouseX, mouseY, verticalAmount)) return true;
        if (handleScroll(z1Field, mouseX, mouseY, verticalAmount)) return true;
        if (handleScroll(x2Field, mouseX, mouseY, verticalAmount)) return true;
        if (handleScroll(y2Field, mouseX, mouseY, verticalAmount)) return true;
        if (handleScroll(z2Field, mouseX, mouseY, verticalAmount)) return true;

        if (handleScroll(wField, mouseX, mouseY, verticalAmount)) { updatePos2FromSize(); return true; }
        if (handleScroll(hField, mouseX, mouseY, verticalAmount)) { updatePos2FromSize(); return true; }
        if (handleScroll(dField, mouseX, mouseY, verticalAmount)) { updatePos2FromSize(); return true; }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean handleScroll(TextFieldWidget f, double mx, double my, double amount) {
        if (f != null && f.isMouseOver(mx, my)) {
            boolean isSize = (f == wField || f == hField || f == dField);
            adjustField(f, (int)amount, isSize);
            return true;
        }
        return false;
    }
}