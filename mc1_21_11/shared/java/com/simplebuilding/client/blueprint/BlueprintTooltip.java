package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintMaterials;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.items.tooltip.BlueprintTooltipData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * Tooltip-Bild der Blaupause: das Bauwerk als langsam kreisende 3D-Miniatur und darunter, mit
 * gedrueckter Umschalttaste, die Materialliste (Icon, Menge, Name; groesste Menge zuerst).
 * Fuer alle Loader dieselbe Fabrik ({@link #create}).
 */
public final class BlueprintTooltip implements ClientTooltipComponent {
    private static final int VIEW = 88;
    private static final int ROW = 17;
    private static final int MAX_ROWS = 8;
    /** Eine Umdrehung in so vielen Millisekunden. */
    private static final float PERIOD_MS = 9000f;

    private final BlueprintModel model;
    private final List<BlueprintMaterials.Entry> materials;

    private BlueprintTooltip(BlueprintModel model) {
        this.model = model;
        this.materials = BlueprintMaterials.list(model);
    }

    public static ClientTooltipComponent create(BlueprintTooltipData data) {
        return new BlueprintTooltip(BlueprintCode.parseCached(data.code()).model());
    }

    private static boolean showMaterials() {
        return Minecraft.getInstance().hasShiftDown();
    }

    private int rows() {
        return Math.min(MAX_ROWS, materials.size());
    }

    @Override
    public int getHeight(Font font) {
        int h = VIEW + 4;
        if (showMaterials()) {
            h += rows() * ROW + (materials.size() > MAX_ROWS ? 10 : 0);
        } else {
            h += 10;
        }
        return h;
    }

    @Override
    public int getWidth(Font font) {
        int w = VIEW;
        if (showMaterials()) {
            for (int i = 0; i < rows(); i++) {
                w = Math.max(w, 20 + font.width(rowText(materials.get(i))));
            }
        } else {
            w = Math.max(w, font.width(Component.translatable("simplebuilding.blueprint.tooltip.shift")));
        }
        return w;
    }

    private static Component rowText(BlueprintMaterials.Entry e) {
        if (e.block() != null) {
            return Component.literal(e.count() + "× ").append(e.block().getName())
                    .append(Component.translatable("simplebuilding.blueprint.materials.creative_only").withStyle(ChatFormatting.DARK_RED));
        }
        return Component.literal(e.count() + "× ").append(new ItemStack(e.item()).getHoverName());
    }

    @Override
    public void renderImage(Font font, int x, int y, int w, int h, GuiGraphics graphics) {
        int viewX = x + (w - VIEW) / 2;
        graphics.fill(viewX, y, viewX + VIEW, y + VIEW, 0x40203050);
        BlueprintView.Mesh mesh = BlueprintView.mesh(model);
        float angle = (Util.getMillis() % (long) PERIOD_MS) / PERIOD_MS * (float) (Math.PI * 2);
        Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(28)).rotateY(angle);
        BlueprintView.render(graphics, mesh, viewX + 2, y + 2, VIEW - 4, VIEW - 4, rotation, 0.95f);
        int ty = y + VIEW + 4;
        if (!showMaterials()) {
            graphics.drawString(font, Component.translatable("simplebuilding.blueprint.tooltip.shift").withStyle(ChatFormatting.DARK_GRAY), x, ty, 0xFFFFFFFF);
            return;
        }
        for (int i = 0; i < rows(); i++) {
            BlueprintMaterials.Entry e = materials.get(i);
            if (e.block() == null) {
                graphics.renderItem(new ItemStack(e.item()), x, ty + i * ROW);
            }
            graphics.drawString(font, rowText(e), x + 20, ty + i * ROW + 4, 0xFFFFFFFF);
        }
        if (materials.size() > MAX_ROWS) {
            graphics.drawString(font, Component.translatable("simplebuilding.blueprint.materials.more", materials.size() - MAX_ROWS)
                    .withStyle(ChatFormatting.GRAY), x, ty + rows() * ROW + 1, 0xFFFFFFFF);
        }
    }
}
