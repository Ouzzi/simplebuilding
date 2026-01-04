package com.simplebuilding.client.gui;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.OctantConfigurePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW; // Wichtig für Key-Codes

import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private final ItemStack stack;

    // UI Widgets
    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget wField, hField, dField;

    private ButtonWidget shapeButton;
    private ButtonWidget modeButton; // NEU: 2D/3D
    private ButtonWidget lockButton;
    private ButtonWidget doneButton;

    // Daten
    private OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
    private OctantItem.SelectionMode currentMode = OctantItem.SelectionMode.MODE_3D;
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private boolean isLocked = false;
    private boolean isUpdating = false;

    // Layout Konstanten
    private int panelX;
    private int panelY;
    private final int ROW_HEIGHT = 24;
    private final int LABEL_WIDTH = 40;

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

        // Shape laden
        try { currentShape = OctantItem.SelectionShape.valueOf(nbt.getString("Shape", "")); }
        catch (Exception e) { currentShape = OctantItem.SelectionShape.CUBOID; }

        // Mode laden (2D/3D)
        try { currentMode = OctantItem.SelectionMode.valueOf(nbt.getString("Mode", "")); }
        catch (Exception e) { currentMode = OctantItem.SelectionMode.MODE_3D; }

        isLocked = nbt.getBoolean("Locked", false);
    }

    @Override
    protected void init() {
        // Menü auf der rechten Seite, etwas breiter
        int menuWidth = 190;
        this.panelX = width - menuWidth - 10;

        // Vertikale Zentrierung
        int contentHeight = (ROW_HEIGHT * 6) + 40; // 3 Zeilen Koords + 2 Zeilen Buttons + Padding
        this.panelY = (height - contentHeight) / 2;

        int currentY = panelY;

        // --- ZEILE 1: Pos 1 ---
        // Labels werden in render() gezeichnet, hier nur Widgets
        createCoordRow(panelX + LABEL_WIDTH, currentY, pos1.getX(), pos1.getY(), pos1.getZ(),
                f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);

        currentY += ROW_HEIGHT + 5;

        // --- ZEILE 2: Pos 2 ---
        createCoordRow(panelX + LABEL_WIDTH, currentY, pos2.getX(), pos2.getY(), pos2.getZ(),
                f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);

        currentY += ROW_HEIGHT + 5;

        // --- ZEILE 3: Größe ---
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        createSizeRow(panelX + LABEL_WIDTH, currentY, w, h, d);

        currentY += ROW_HEIGHT + 15; // Etwas mehr Abstand zu den Buttons

        // --- ZEILE 4: Shape & Mode (Nebeneinander) ---
        int buttonWidth = 70;

        // Shape Button (Links)
        shapeButton = ButtonWidget.builder(getShapeText(), b -> cycleShape())
                .dimensions(panelX + 10, currentY, buttonWidth + 10, 20)
                .build();
        addDrawableChild(shapeButton);

        // Mode Button (Rechts)
        modeButton = ButtonWidget.builder(getModeText(), b -> cycleMode())
                .dimensions(panelX + 10 + buttonWidth + 15, currentY, buttonWidth - 5, 20)
                .build();
        addDrawableChild(modeButton);

        currentY += 25; // Nächste Zeile

        // --- ZEILE 5: Lock & Fertig ---
        // Lock Button (Kleines Icon links)
        lockButton = ButtonWidget.builder(getLockIcon(), b -> {
            isLocked = !isLocked;
            b.setMessage(getLockIcon());
            updateLocalAndSend();
        }).dimensions(panelX + 10, currentY, 20, 20).build();
        addDrawableChild(lockButton);

        // Fertig Button (Daneben, breit)
        doneButton = ButtonWidget.builder(Text.literal("Fertig"), b -> close())
                .dimensions(panelX + 35, currentY, 135, 20) // Mehr Platz und Padding
                .build();
        addDrawableChild(doneButton);
    }

    // --- Helper für Textfelder ---
    private void createCoordRow(int x, int y, int v1, int v2, int v3,
                                Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
        int spacing = 48; // Abstand zwischen den Feldern
        createControlGroup(x, y, v1, a1, false);
        createControlGroup(x + spacing, y, v2, a2, false);
        createControlGroup(x + spacing * 2, y, v3, a3, false);
    }

    private void createSizeRow(int x, int y, int w, int h, int d) {
        int spacing = 48;
        createControlGroup(x, y, w, f -> wField = f, true);
        createControlGroup(x + spacing, y, h, f -> hField = f, true);
        createControlGroup(x + spacing * 2, y, d, f -> dField = f, true);
    }

    private void createControlGroup(int x, int y, int val, Consumer<TextFieldWidget> assigner, boolean isSize) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 32, 16, Text.empty());
        field.setText(String.valueOf(val));
        field.setTextPredicate(s -> s.matches("-?\\d*"));
        field.setChangedListener(s -> {
            if (!isUpdating) {
                if (isSize) updatePos2FromSize(); else updateLocalAndSend();
            }
        });
        assigner.accept(field);
        addDrawableChild(field);

        // Kleine +/- Buttons
        // Vertikal gestapelt rechts neben dem Feld
        int btnX = x + 33;
        addDrawableChild(ButtonWidget.builder(Text.literal("▴"), b -> adjustField(field, 1, isSize))
                .dimensions(btnX, y - 1, 12, 9).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▾"), b -> adjustField(field, -1, isSize))
                .dimensions(btnX, y + 8, 12, 9).build());
    }

    // --- Logik ---

    private void cycleShape() {
        OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
        int index = (currentShape.ordinal() + 1) % values.length;
        currentShape = values[index];
        shapeButton.setMessage(getShapeText());
        updateLocalAndSend();
    }

    private void cycleMode() {
        OctantItem.SelectionMode[] values = OctantItem.SelectionMode.values();
        int index = (currentMode.ordinal() + 1) % values.length;
        currentMode = values[index];
        modeButton.setMessage(getModeText());
        updateLocalAndSend();
    }

    private Text getShapeText() { return Text.literal(currentShape.getName()); }
    private Text getModeText() { return Text.literal(currentMode.getName()); }

    private Text getLockIcon() {
        return isLocked ? Text.literal("🔒").formatted(Formatting.RED) : Text.literal("🔓").formatted(Formatting.GREEN);
    }

    private void adjustField(TextFieldWidget field, int delta, boolean isSize) {
        try {
            int val = Integer.parseInt(field.getText());
            val += delta;
            if (isSize && val < 1) val = 1;
            field.setText(String.valueOf(val));
            if (isSize) updatePos2FromSize(); else updateLocalAndSend();
        } catch (NumberFormatException ignored) { field.setText(isSize ? "1" : "0"); }
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
        if (x1Field == null) return;
        try {
            BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
            BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));

            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putIntArray("Pos1", new int[]{p1.getX(), p1.getY(), p1.getZ()});
            nbt.putIntArray("Pos2", new int[]{p2.getX(), p2.getY(), p2.getZ()});
            nbt.putString("Shape", currentShape.name());
            nbt.putString("Mode", currentMode.name()); // Neu: Mode speichern
            nbt.putBoolean("Locked", isLocked);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            // HINWEIS: Payload Klasse muss ggf. angepasst werden um 'Mode' zu senden,
            // oder du packst es auch dort rein. Hier wird es zumindest lokal im Item gespeichert.
            ClientPlayNetworking.send(new OctantConfigurePayload(
                    Optional.of(p1), Optional.of(p2), currentShape.name(), isLocked
            ));
        } catch (Exception ignored) {}
    }

    private int parse(TextFieldWidget f) {
        try { return Integer.parseInt(f.getText()); } catch (Exception e) { return 0; }
    }

    // --- Input & Rendering ---

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key(); // Key Code aus dem Wrapper holen

        // Schließen mit R oder ESC
        if (keyCode == GLFW.GLFW_KEY_R || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // HINTERGRUND: Sehr transparent (0x10...), damit man die Welt sieht
        context.fill(0, 0, width, height, 0x10000000);

        // Optional: Dunkler Hintergrund NUR hinter dem Menü für Lesbarkeit
        int bgPad = 10;
        int menuWidth = 190;
        context.fill(panelX - bgPad, panelY - 20, panelX + menuWidth, panelY + (ROW_HEIGHT * 6) + 40, 0x50000000);

        super.render(context, mouseX, mouseY, delta); // Zeichnet Widgets

        // LABELS: Nach super.render() zeichnen, damit sie nicht überdeckt werden
        int labelColor = 0xFFFFFF; // Weiß
        int y = panelY + 4; // Text etwas zentrieren relativ zur TextBox

        // Zeile 1: Pos 1
        context.drawTextWithShadow(textRenderer, "Start", panelX, y, labelColor);
        drawXYZLabels(context, panelX + LABEL_WIDTH, y);

        y += ROW_HEIGHT + 5;

        // Zeile 2: Pos 2
        context.drawTextWithShadow(textRenderer, "Ende", panelX, y, labelColor);
        drawXYZLabels(context, panelX + LABEL_WIDTH, y);

        y += ROW_HEIGHT + 5;

        // Zeile 3: Größe
        context.drawTextWithShadow(textRenderer, "Größe", panelX, y, 0x55FFFF); // Cyan für Größe
        drawWHDLabels(context, panelX + LABEL_WIDTH, y);
    }

    private void drawXYZLabels(DrawContext context, int startX, int y) {
        int spacing = 48;
        // Kleine Labels über oder neben den Feldern? Hier einfach davor, da Platz ist.
        // Falls in den Feldern kein Platz ist, zeichnen wir sie knapp drüber
        int labelY = y - 10;
        context.drawTextWithShadow(textRenderer, "X", startX, labelY, 0xFF5555);
        context.drawTextWithShadow(textRenderer, "Y", startX + spacing, labelY, 0x55FF55);
        context.drawTextWithShadow(textRenderer, "Z", startX + spacing * 2, labelY, 0x5555FF);
    }

    private void drawWHDLabels(DrawContext context, int startX, int y) {
        int spacing = 48;
        int labelY = y - 10;
        context.drawTextWithShadow(textRenderer, "W", startX, labelY, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "H", startX + spacing, labelY, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "T", startX + spacing * 2, labelY, 0xAAAAAA);
    }
}