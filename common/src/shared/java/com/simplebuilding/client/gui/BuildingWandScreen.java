package com.simplebuilding.client.gui;

import com.mojang.blaze3d.platform.InputConstants;

import com.simplebuilding.client.ClientState;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.networking.BuildingWandConfigurePayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class BuildingWandScreen extends Screen {
    private final ItemStack stack;

    private int currentRadius = 0;
    private int maxRadiusForTier = 1;
    private int axisMode = 0; // 0 = Face (Default), 1 = X, 2 = Y, 3 = Z

    private int columnCenterX;
    private int startY;
    /** N43 panel (26.3, style on): top-left corner and fill; {@code styled} false = the old floating buttons. */
    private boolean styled;
    private int panelX, panelY;
    private final int fill;

    public BuildingWandScreen(ItemStack stack) {
        super(Component.translatable("simplebuilding.gui.wand_title"));
        this.stack = stack;
        this.fill = ModScreenLayout.wandFill(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
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
        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();

        // Lade Radius (Default ist maxRadius, wenn noch nicht gesetzt)
        if (nbt.contains("SettingsRadius")) {
            currentRadius = nbt.getIntOr("SettingsRadius", 1);
        } else {
            currentRadius = maxRadiusForTier;
        }
        // Clamp Radius
        if (currentRadius > maxRadiusForTier) currentRadius = maxRadiusForTier;
        if (currentRadius < 0) currentRadius = 0;

        // Lade Axis
        axisMode = nbt.getIntOr("SettingsAxis", 0);
    }

    @Override
    protected void init() {
        this.columnCenterX = width / 2;
        this.startY = height / 2 - 40;
        this.styled = ModScreenStyle.panels();
        this.panelX = (width - ModScreenLayout.WAND_W) / 2;
        this.panelY = (height - ModScreenLayout.WAND_H) / 2;
        int bx = styled ? panelX + ModScreenLayout.WAND_BUTTON_X : columnCenterX - 100;
        int radiusY = styled ? panelY + ModScreenLayout.WAND_RADIUS_Y : startY;
        int axisY = styled ? panelY + ModScreenLayout.WAND_AXIS_Y : startY + 25;
        int closeY = styled ? panelY + ModScreenLayout.WAND_CLOSE_Y : startY + 60;

        // --- Radius Button ---
        addRenderableWidget(Button.builder(getRadiusText(), b -> {
                    // Zyklisch erhöhen: 0 -> 1 -> ... -> Max -> 0
                    currentRadius++;
                    if (currentRadius > maxRadiusForTier) currentRadius = 0;
                    b.setMessage(getRadiusText());
                    updateLocalAndSend();
                })
                .bounds(bx, radiusY, 200, 20)
                .build());

        // --- Axis Button ---
        addRenderableWidget(Button.builder(getAxisText(), b -> {
                    // 0 -> 1 -> 2 -> 3 -> 0
                    axisMode = (axisMode + 1) % 4;
                    b.setMessage(getAxisText());
                    updateLocalAndSend();
                })
                .bounds(bx, axisY, 200, 20)
                .build());

        // Close Button
        addRenderableWidget(Button.builder(Component.translatable("simplebuilding.gui.close"), b -> onClose())
                .bounds(columnCenterX - 50, closeY, 100, 20).build());
    }

    private Component getRadiusText() {
        // Zeigt an: "Radius: 1 / 3" (als Beispiel)
        return Component.translatable("simplebuilding.gui.radius")
                .append(": " + currentRadius + " / " + maxRadiusForTier);
    }

    private Component getAxisText() {
        String modeString = switch (axisMode) {
            case 1 -> "X";
            case 2 -> "Y";
            case 3 -> "Z";
            default -> "Face (Auto)";
        };
        ChatFormatting color = axisMode == 0 ? ChatFormatting.GRAY : ChatFormatting.YELLOW;
        return Component.translatable("simplebuilding.gui.axis").append(": ").append(Component.literal(modeString).withStyle(color));
    }

    private void updateLocalAndSend() {
        try {
            CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtData.copyTag();
            nbt.putInt("SettingsRadius", currentRadius);
            nbt.putInt("SettingsAxis", axisMode);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

            // Sende Paket an Server
            ClientNetworking.send(new BuildingWandConfigurePayload(currentRadius, axisMode));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // Hole den KeyCode für den Vergleich mit E und ESC
        int keyCode = input.key();

        // KORREKTUR: Übergib das 'input' Objekt direkt an matchesKey
        if (ClientState.settingsKey.matches(input)
                || this.minecraft.options.keyInventory.matches(input)
                || keyCode == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x15000000);
        if (styled && ModScreenStyle.panel(context, fill, panelX, panelY, ModScreenLayout.WAND_W, ModScreenLayout.WAND_H)) {
            // Title like a container's: top left, label colour of the panel, no shadow.
            context.text(font, Component.translatable("simplebuilding.gui.wand_settings"), panelX + ModScreenLayout.SIDE,
                    panelY + ModScreenLayout.WAND_TITLE_Y, ModScreenStyle.label(fill, 0xFF404040), false);
            super.extractRenderState(context, mouseX, mouseY, delta);
            return;
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, Component.translatable("simplebuilding.gui.wand_settings"), columnCenterX, startY - 20, 0xFFFFFFFF);
    }
}