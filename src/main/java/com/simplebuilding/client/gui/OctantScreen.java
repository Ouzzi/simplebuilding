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

    // Layout Variablen
    private int guiLeft;
    private int guiTop;

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
        isLocked = nbt.getBoolean("Locked").orElse(false);
    }

    @Override
    protected void init() {
        // --- LAYOUT BERECHNUNG ---
        // Wir berechnen die absolute Höhe des Inhalts, um ihn vertikal zu zentrieren.
        // Inhalt: Title(15) + Pos1(35) + Pos2(35) + Size(35) + Shape(25) + Done(20) = ~165px
        int contentHeight = 175;
        this.guiLeft = width / 2;
        this.guiTop = Math.max(10, (height - contentHeight) / 2); // Mindestens 10px von oben

        int rowSpacing = 40; // Abstand zwischen den Startpunkten der Gruppen (Label + Felder)
        int fieldYOffset = 12; // Abstand von Label zu Feld

        // 1. LOCK BUTTON (Oben rechts vom Menü)
        lockButton = ButtonWidget.builder(getLockText(), button -> {
            isLocked = !isLocked;
            button.setMessage(getLockText());
            updateLocalAndSend();
        }).dimensions(guiLeft + 60, guiTop - 5, 60, 16).build();
        addDrawableChild(lockButton);

        // 2. POS 1 (Start)
        int y1 = guiTop + 15;
        createRow(guiLeft, y1 + fieldYOffset, pos1.getX(), pos1.getY(), pos1.getZ(),
                f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);

        // 3. POS 2 (Ende)
        int y2 = y1 + rowSpacing;
        createRow(guiLeft, y2 + fieldYOffset, pos2.getX(), pos2.getY(), pos2.getZ(),
                f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);

        // 4. SIZE (Größe)
        int y3 = y2 + rowSpacing;
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(guiLeft, y3 + fieldYOffset, w, h, d);

        // 5. SHAPE BUTTON (Form) - Jetzt ÜBER dem Fertig Button
        int yShape = y3 + rowSpacing + 5;
        shapeButton = ButtonWidget.builder(getShapeText(), button -> cycleShape())
                .dimensions(guiLeft - 60, yShape, 120, 20).build();
        addDrawableChild(shapeButton);

        // 6. FERTIG BUTTON (Ganz unten)
        int yDone = yShape + 24; // Knopf darunter
        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), button -> close())
                .dimensions(guiLeft - 60, yDone, 120, 20).build());
    }

    private void createRow(int cX, int y, int v1, int v2, int v3,
                           Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
        // Kompakte Reihe: [Input][Input][Input] (Buttons integriert? Nein, daneben wird zu breit)
        // Wir machen:  [Input][+][-]   [Input][+][-]   [Input][+][-]

        int groupWidth = 55; // Breite eines Blocks (Feld + Buttons)
        int startX = cX - (int)(groupWidth * 1.5) - 5;

        createControlGroup(startX, y, v1, a1, false);
        createControlGroup(startX + groupWidth + 5, y, v2, a2, false);
        createControlGroup(startX + (groupWidth + 5) * 2, y, v3, a3, false);
    }

    private void createSizeRow(int cX, int y, int w, int h, int d) {
        int groupWidth = 55;
        int startX = cX - (int)(groupWidth * 1.5) - 5;

        createControlGroup(startX, y, w, f -> wField = f, true);
        createControlGroup(startX + groupWidth + 5, y, h, f -> hField = f, true);
        createControlGroup(startX + (groupWidth + 5) * 2, y, d, f -> dField = f, true);
    }

    private void createControlGroup(int x, int y, int val, Consumer<TextFieldWidget> assigner, boolean isSize) {
        // Feld
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 32, 16, Text.empty());
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

        // Buttons (gestapelt, ganz klein rechts daneben)
        // + Button
        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> adjustField(field, 1, isSize))
                .dimensions(x + 33, y, 12, 8).build();
        addDrawableChild(plus);

        // - Button
        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> adjustField(field, -1, isSize))
                .dimensions(x + 33, y + 8, 12, 8).build();
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

    private Text getLockText() {
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
        // HINTERGRUND: Sehr transparent (nur 15% Deckkraft), KEIN Blur
        context.fill(0, 0, width, height, 0x25000000);

        super.render(context, mouseX, mouseY, delta);

        // TEXTE RENDERN
        // Titel
        context.drawCenteredTextWithShadow(textRenderer, "Octant Konfiguration", guiLeft, guiTop, 0xFFFFFF);

        // Gruppen Überschriften (direkt über den Feldern)
        // Position passend zu init() Logik
        int rowSpacing = 40;
        int y1 = guiTop + 15;

        // "Start Position"
        context.drawCenteredTextWithShadow(textRenderer, "Start Position (1)", guiLeft, y1, 0xAAAAAA);

        // "End Position"
        context.drawCenteredTextWithShadow(textRenderer, "End Position (2)", guiLeft, y1 + rowSpacing, 0xAAAAAA);

        // "Größe"
        context.drawCenteredTextWithShadow(textRenderer, "Größe (B x H x T)", guiLeft, y1 + rowSpacing * 2, 0x55FFFF);

        // Lock Tooltip
        if (lockButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.literal(isLocked ? "Entsperren (Erlaubt Klicken)" : "Sperren (Verhindert Klicken)"), mouseX, mouseY);
        }
    }

    // Scrollen Support
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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