package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.guiDrawHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.ArrayList;
import java.util.List;

public class RangefinderHudOverlay {

    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (client.gui.screen() instanceof OctantScreen) return;

        ItemStack stack = client.player.getMainHandItem();
        boolean hasOctant = stack.getItem() instanceof OctantItem;

        if (!hasOctant) {
            stack = client.player.getOffhandItem();
            if (stack.getItem() instanceof OctantItem) {
                hasOctant = true;
            }
        }

        if (!hasOctant) return;

        OctantItem octantItem = (OctantItem) stack.getItem();
        DyeColor dyeColor = octantItem.getColor();

        guiDrawHelper.ColorTheme theme = guiDrawHelper.getColorTheme(dyeColor);

        ItemStack main = client.player.getMainHandItem();
        ItemStack off = client.player.getOffhandItem();
        boolean hasSpeedometer = main.is(ModItems.VELOCITY_GAUGE) || off.is(ModItems.VELOCITY_GAUGE);

        CustomData nbtData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtData.copyTag();
        List<Component> lines = new ArrayList<>();

        ChatFormatting titleColor = stack.isEnchanted() ? ChatFormatting.AQUA : ChatFormatting.WHITE;

        lines.add(stack.getHoverName().copy().withStyle(titleColor));

        BlockPos pos1 = null;
        BlockPos pos2 = null;

        if (nbt.contains("Pos1")) {
            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
            if (p1.length == 3) {
                pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                lines.add(Component.literal("Pos 1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ())
                        .setStyle(Style.EMPTY.withColor(theme.pos1())));
            }
        } else {
            lines.add(Component.literal("Right-Click block to set Pos 1").withStyle(ChatFormatting.GRAY));
        }

        if (nbt.contains("Pos2")) {
            int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
            if (p2.length == 3) {
                pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                lines.add(Component.literal("Pos 2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ())
                        .setStyle(Style.EMPTY.withColor(theme.pos2())));
            }
        } else if (pos1 != null) {
            lines.add(Component.literal("Sneak + R-Click to set Pos 2").withStyle(ChatFormatting.GRAY));
        }

        if (pos1 != null && pos2 != null) {
            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;
            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;
            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;

            // Resultat nutzt Pos1 Farbe (hell)
            int resultColor = theme.pos1();

            if (dy == 1 && (dx == 1 || dz == 1)) {
                lines.add(Component.literal("Distance: " + Math.max(dx, dz) + " blocks").setStyle(Style.EMPTY.withColor(resultColor)));
            } else if (dy == 1) {
                lines.add(Component.literal("Area: " + (dx * dz) + " blocks²").setStyle(Style.EMPTY.withColor(resultColor)));
                lines.add(Component.literal("(" + dx + " x " + dz + ")").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal("Volume: " + (dx * dy * dz) + " blocks³").setStyle(Style.EMPTY.withColor(resultColor)));
                lines.add(Component.literal("(" + dx + " x " + dy + " x " + dz + ")").withStyle(ChatFormatting.GRAY));
            }
        }

        if (lines.isEmpty()) return;

        Font textRenderer = client.font;
        int screenHeight = context.guiHeight();

        int actualTextWidth = 0;
        for (Component line : lines) {
            int w = textRenderer.width(line);
            if (w > actualTextWidth) actualTextWidth = w;
        }
        String sampleString = "Pos 2: -8888, 888, -8888";
        int minSafeWidth = textRenderer.width(sampleString);
        int finalContentWidth = Math.max(actualTextWidth, minSafeWidth);

        int paddingX = 6;
        int paddingY = 6;
        int lineSpacing = 2;
        int titleSpacing = 4;

        int totalTextHeight = (lines.size() * textRenderer.lineHeight) + ((lines.size() - 1) * lineSpacing) + titleSpacing * (lines.size() > 3 ? 2 : 1);
        int boxWidth = finalContentWidth + (paddingX * 2);
        int boxHeight = totalTextHeight + (paddingY * 2);

        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);

        if (hasSpeedometer) {
            y -= 35;
        }

        // --- BOX RAHMEN ---
        int bg = theme.background();

        // Haupt-Hintergrund
        context.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, bg);
        context.fill(x + 1, y, x + boxWidth - 1, y + 1, bg);

        // Hintergrund "Schatten" für den Rahmen
        context.fill(x + 1, y - 1, x + boxWidth - 1, y, bg);
        context.fill(x + 1, y + boxHeight, x + boxWidth - 1, y + boxHeight + 1, bg);
        context.fill(x - 1, y + 1, x, y + boxHeight - 1, bg);
        context.fill(x + boxWidth, y + 1, x + boxWidth + 1, y + boxHeight - 1, bg);

        // Ecken
        context.fillGradient(x + boxWidth - 1, y, x + boxWidth, y + 1, bg, bg);
        context.fillGradient(x, y, x + 1, y + 1, bg, bg);
        context.fillGradient(x + boxWidth - 1, y + boxHeight - 1, x + boxWidth, y + boxHeight, bg, bg);
        context.fillGradient(x, y + boxHeight - 1, x + 1, y + boxHeight, bg, bg);

        // 2. Farbiger Rahmen (Lines)
        // Nutzt theme.borderStart und theme.borderEnd
        context.fillGradient(x + 1, y, x + boxWidth - 1, y + 1, theme.borderStart(), theme.borderStart());
        context.fillGradient(x + 1, y + boxHeight - 1, x + boxWidth - 1, y + boxHeight, theme.borderEnd(), theme.borderEnd());
        context.fillGradient(x, y + 1, x + 1, y + boxHeight - 1, theme.borderStart(), theme.borderEnd());
        context.fillGradient(x + boxWidth - 1, y + 1, x + boxWidth, y + boxHeight - 1, theme.borderStart(), theme.borderEnd());

        // --- TEXT ---
        int textY = y + paddingY;

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            context.text(textRenderer, line, x + paddingX, textY, 0xFFFFFFFF, true);
            textY += textRenderer.lineHeight + lineSpacing;
            if (i == 0 || i == 2) textY += titleSpacing;
        }
    }

}