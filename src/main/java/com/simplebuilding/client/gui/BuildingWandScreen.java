package com.simplebuilding.client.gui;

import com.simplebuilding.items.custom.BuildingWandItem;
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

    private int currentRadius = 0;
    private int maxRadiusForTier = 1;
    private int axisMode = 0; // 0 = Face (Default), 1 = X, 2 = Y, 3 = Z

    private int columnCenterX;
    private int startY;

    public BuildingWandScreen(ItemStack stack) {
        super(Text.translatable("simplebuilding.gui.wand_title"));
        this.stack = stack;
        determineMaxRadius();
        loadDataFromStack();
    }


    private void determineMaxRadius() {
        if (stack.getItem() instanceof BuildingWandItem wand) {
            // Hole den maximalen Durchmesser vom Item und rechne in Radius um ( (d-1)/2 )
            // Copper (3) -> Radius 1
            // Iron (5) -> Radius 2
            // Gold/Diamond (7) -> Radius 3
            // Netherite (9) -> Radius 4
            int diameter = wand.getWandSquareDiameter();
            this.maxRadiusForTier = (diameter - 1) / 2;
        } else {
            this.maxRadiusForTier = 1;
        }
    }

    private void loadDataFromStack() {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        // Lade Radius (Default ist maxRadius, wenn noch nicht gesetzt)
        if (nbt.contains("SettingsRadius")) {
            currentRadius = nbt.getInt("SettingsRadius", 1);
        } else {
            currentRadius = maxRadiusForTier;
        }
        // Clamp Radius
        if (currentRadius > maxRadiusForTier) currentRadius = maxRadiusForTier;
        if (currentRadius < 0) currentRadius = 0;

        // Lade Axis
        axisMode = nbt.getInt("SettingsAxis", 0);
    }

    @Override
    protected void init() {
        this.columnCenterX = width / 2;
        this.startY = height / 2 - 40;

        // --- Radius Button ---
        addDrawableChild(ButtonWidget.builder(getRadiusText(), b -> {
                    // Zyklisch erhöhen: 0 -> 1 -> ... -> Max -> 0
                    currentRadius++;
                    if (currentRadius > maxRadiusForTier) currentRadius = 0;
                    b.setMessage(getRadiusText());
                    updateLocalAndSend();
                })
                .dimensions(columnCenterX - 100, startY, 200, 20)
                .build());

        // --- Axis Button ---
        addDrawableChild(ButtonWidget.builder(getAxisText(), b -> {
                    // 0 -> 1 -> 2 -> 3 -> 0
                    axisMode = (axisMode + 1) % 4;
                    b.setMessage(getAxisText());
                    updateLocalAndSend();
                })
                .dimensions(columnCenterX - 100, startY + 25, 200, 20)
                .build());

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.translatable("simplebuilding.gui.close"), b -> close())
                .dimensions(columnCenterX - 50, startY + 60, 100, 20).build());
    }

    private Text getRadiusText() {
        // Zeigt an: "Radius: 1 / 3" (als Beispiel)
        return Text.translatable("simplebuilding.gui.radius")
                .append(": " + currentRadius + " / " + maxRadiusForTier);
    }

    private Text getAxisText() {
        String modeString = switch (axisMode) {
            case 1 -> "X";
            case 2 -> "Y";
            case 3 -> "Z";
            default -> "Face (Auto)";
        };
        Formatting color = axisMode == 0 ? Formatting.GRAY : Formatting.YELLOW;
        return Text.translatable("simplebuilding.gui.axis").append(": ").append(Text.literal(modeString).formatted(color));
    }

    private void updateLocalAndSend() {
        try {
            NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound nbt = nbtData.copyNbt();
            nbt.putInt("SettingsRadius", currentRadius);
            nbt.putInt("SettingsAxis", axisMode);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            // Sende Paket an Server
            ClientPlayNetworking.send(new BuildingWandConfigurePayload(currentRadius, axisMode));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean keyPressed(KeyInput input) {
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x15000000);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("simplebuilding.gui.wand_settings"), columnCenterX, startY - 20, 0xFFFFFF);
    }
}