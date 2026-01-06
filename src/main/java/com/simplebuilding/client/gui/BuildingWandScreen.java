package com.simplebuilding.client.gui;

import com.simplebuilding.networking.BuildingWandConfigurePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class BuildingWandScreen extends Screen {
    private final ItemStack stack;
    
    private ButtonWidget useFullInventoryButton;
    private boolean useFullInventory = true; // Standardwert

    private int columnCenterX;
    private int startY;

    public BuildingWandScreen(ItemStack stack) {
        super(Text.translatable("simplebuilding.gui.wand_title"));
        this.stack = stack;
        loadDataFromStack();
    }

    private void loadDataFromStack() {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();
        // Lade den Status, default true (wenn Master Builder vorhanden ist, will man es meist nutzen)
        useFullInventory = nbt.getBoolean("UseFullInventory", true);
    }

    @Override
    protected void init() {
        this.columnCenterX = width / 2;
        this.startY = height / 2 - 50;

        // Toggle für Master Builder Inventory Scan
        useFullInventoryButton = ButtonWidget.builder(getInventoryText(), b -> {
            useFullInventory = !useFullInventory;
            b.setMessage(getInventoryText());
            updateLocalAndSend();
        })
        .dimensions(columnCenterX - 100, startY, 200, 20)
        .build();
        
        addDrawableChild(useFullInventoryButton);

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.translatable("simplebuilding.gui.close"), b -> close())
                .dimensions(columnCenterX - 50, startY + 30, 100, 20).build());
    }

    private Text getInventoryText() {
        Text status = useFullInventory ? Text.translatable("simplebuilding.gui.on").formatted(Formatting.GREEN) : Text.translatable("simplebuilding.gui.off").formatted(Formatting.RED);
        return Text.translatable("simplebuilding.gui.use_full_inventory").append(": ").append(status);
    }

    private void updateLocalAndSend() {
        try {
            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putBoolean("UseFullInventory", useFullInventory);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            // Sende Paket an Server
            ClientPlayNetworking.send(new BuildingWandConfigurePayload(useFullInventory));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_E || input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x15000000); // Leichter Hintergrund
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.wand_settings"), columnCenterX, startY - 20, 0xFFFFFF);
    }
}