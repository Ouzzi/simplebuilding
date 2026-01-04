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
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Consumer;

public class OctantScreen extends Screen {
    private final ItemStack stack;

    private TextFieldWidget x1Field, y1Field, z1Field;
    private TextFieldWidget x2Field, y2Field, z2Field;
    private TextFieldWidget wField, hField, dField;
    private ButtonWidget shapeButton;
    private ButtonWidget lockButton;

    private OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private boolean isLocked = false;
    private boolean isUpdating = false; // Verhindert Rekursion bei Updates

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
        int cX = width / 2;
        // Kompaktes Layout: Wir nutzen weniger vertikalen Platz
        int startY = 25;
        int rowH = 18;
        int groupGap = 22;

        // --- LOCK BUTTON (Oben Rechts vom Titel, oder Zentriert oben) ---
        lockButton = ButtonWidget.builder(getLockText(), button -> {
            isLocked = !isLocked;
            button.setMessage(getLockText());
            updateLocalAndSend();
        }).dimensions(cX + 60, 4, 90, 16).build(); // Kleiner und oben
        addDrawableChild(lockButton);

        // --- POS 1 ---
        // Labels rendern wir in der render() Methode, hier nur die Inputs
        createRow(cX, startY, pos1.getX(), pos1.getY(), pos1.getZ(),
                f -> x1Field=f, f -> y1Field=f, f -> z1Field=f);

        // --- POS 2 ---
        int pos2Y = startY + rowH + groupGap;
        createRow(cX, pos2Y, pos2.getX(), pos2.getY(), pos2.getZ(),
                f -> x2Field=f, f -> y2Field=f, f -> z2Field=f);

        // --- SIZE ---
        int sizeY = pos2Y + rowH + groupGap;
        int w = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int h = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int d = Math.abs(pos2.getZ() - pos1.getZ()) + 1;

        createSizeRow(cX, sizeY, w, h, d);

        // --- SHAPE & CLOSE ---
        int bottomY = height - 25;
        shapeButton = ButtonWidget.builder(Text.translatable("simplebuilding.gui.shape", currentShape.getName()), button -> cycleShape())
                .dimensions(cX - 105, bottomY, 100, 20).build();
        addDrawableChild(shapeButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("simplebuilding.gui.close"), button -> close())
                .dimensions(cX + 5, bottomY, 100, 20).build());
    }

    // Erstellt eine Zeile mit 3 Eingabefeldern (X, Y, Z) und +/- Buttons
    private void createRow(int cX, int y, int v1, int v2, int v3,
                           Consumer<TextFieldWidget> a1, Consumer<TextFieldWidget> a2, Consumer<TextFieldWidget> a3) {
        // Wir platzieren X, Y, Z nebeneinander, aber zentriert
        // Layout: [Label] [Input] [+/-]  |  [Label] [Input] [+/-] ... zu breit.
        // Kompakt: [Input][+/-]  [Input][+/-]  [Input][+/-]

        int fieldW = 35;
        int btnW = 12;
        int spacing = 5;
        int groupW = fieldW + btnW * 2 + 10; // Breite einer Gruppe (z.B. X)

        // Start X Position für das erste Element (X), damit alles zentriert ist
        int startX = cX - (int)(groupW * 1.5) - spacing;

        createSingleControl(startX, y, v1, a1, false);
        createSingleControl(startX + groupW, y, v2, a2, false);
        createSingleControl(startX + groupW * 2, y, v3, a3, false);
    }

    private void createSizeRow(int cX, int y, int w, int h, int d) {
        int fieldW = 35;
        int btnW = 12;
        int groupW = fieldW + btnW * 2 + 10;
        int startX = cX - (int)(groupW * 1.5) - spacing();

        createSingleControl(startX, y, w, f -> wField = f, true);
        createSingleControl(startX + groupW, y, h, f -> hField = f, true);
        createSingleControl(startX + groupW * 2, y, d, f -> dField = f, true);
    }

    private int spacing() { return 5; }

    private void createSingleControl(int x, int y, int val, Consumer<TextFieldWidget> assigner, boolean isSize) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 35, 16, Text.empty());
        field.setText(String.valueOf(val));
        field.setTextPredicate(s -> s.matches("-?\\d*"));

        // WICHTIG: Real-Time Update beim Tippen
        field.setChangedListener(s -> {
            if (!isUpdating) {
                if (isSize) updatePos2FromSize();
                else updateLocalAndSend();
            }
        });

        assigner.accept(field);
        addDrawableChild(field);

        // Buttons stapeln wir klein übereinander oder nebeneinander?
        // Nebeneinander ist einfacher zu klicken.
        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> adjustField(field, -1, isSize))
                .dimensions(x + 37, y, 12, 16).build(); // Klein
        addDrawableChild(minus);

        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> adjustField(field, 1, isSize))
                .dimensions(x + 50, y, 12, 16).build(); // Klein
        addDrawableChild(plus);
    }

    private void adjustField(TextFieldWidget field, int delta, boolean isSize) {
        try {
            int val = Integer.parseInt(field.getText());
            val += delta;
            if (isSize && val < 1) val = 1; // Größe mind. 1
            field.setText(String.valueOf(val));
            // Trigger update manuell, da setText den Listener nicht immer feuert (je nach MC Version)
            if (isSize) updatePos2FromSize();
            else updateLocalAndSend();
        } catch (NumberFormatException ignored) {
            field.setText("0");
        }
    }

    private Text getLockText() {
        return isLocked ? Text.translatable("simplebuilding.gui.locked") : Text.translatable("simplebuilding.gui.unlocked");
    }

    // Berechnet Pos2 basierend auf Pos1 und Size neu
    private void updatePos2FromSize() {
        if (isUpdating) return;
        isUpdating = true; // Sperre Loop
        try {
            int x1 = parse(x1Field); int y1 = parse(y1Field); int z1 = parse(z1Field);
            int w = Math.max(1, parse(wField));
            int h = Math.max(1, parse(hField));
            int d = Math.max(1, parse(dField));

            x2Field.setText(String.valueOf(x1 + w - 1));
            y2Field.setText(String.valueOf(y1 + h - 1));
            z2Field.setText(String.valueOf(z1 + d - 1));

            // Jetzt senden wir das Update
            isUpdating = false; // Sperre aufheben vor Send
            updateLocalAndSend();
        } catch (Exception e) {
            isUpdating = false;
        }
    }

    // WICHTIGSTE METHODE:
    // 1. Schreibt NBT lokal in das Item in der Hand (Renderer sieht es sofort)
    // 2. Sendet Paket an Server (für dauerhaftes Speichern)
    private void updateLocalAndSend() {
        if (x1Field == null || wField == null) return;
        try {
            BlockPos p1 = new BlockPos(parse(x1Field), parse(y1Field), parse(z1Field));
            BlockPos p2 = new BlockPos(parse(x2Field), parse(y2Field), parse(z2Field));

            // 1. Lokales Update (Client Side Render Update)
            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putIntArray("Pos1", new int[]{p1.getX(), p1.getY(), p1.getZ()});
            nbt.putIntArray("Pos2", new int[]{p2.getX(), p2.getY(), p2.getZ()});
            nbt.putString("Shape", currentShape.name());
            nbt.putBoolean("Locked", isLocked);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            // 2. Server Update
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
        shapeButton.setMessage(Text.translatable("simplebuilding.gui.shape", currentShape.getName()));
        updateLocalAndSend();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // LEICHTER HINTERGRUND (weniger dunkel, kein Blur)
        context.fill(0, 0, width, height, 0x45000000);

        super.render(context, mouseX, mouseY, delta);

        int cX = width / 2;

        // Titel
        context.drawCenteredTextWithShadow(textRenderer, this.title, cX, 8, 0xFFFFFF);

        // Labels über den Gruppen
        // Koordinaten der Labels müssen zu createRow passen
        int startY = 25;
        int rowH = 18;
        int groupGap = 22;

        // Helper für zentrierten Text über Input-Reihe
        drawLabel(context, "simplebuilding.gui.pos1", cX, startY - 10, 0xAAAAAA);
        drawLabel(context, "simplebuilding.gui.pos2", cX, startY + rowH + groupGap - 10, 0xAAAAAA);
        drawLabel(context, "simplebuilding.gui.size", cX, startY + (rowH + groupGap)*2 - 10, 0x55FFFF);

        // Optional: Kleine Labels X Y Z über den Feldern zeichnen?
        // Das könnte überladen wirken. Wir lassen es bei den Gruppen-Headern.

        if (lockButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("simplebuilding.gui.lock_tooltip"), mouseX, mouseY);
        }
    }

    private void drawLabel(DrawContext context, String key, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable(key), x, y, color);
    }

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