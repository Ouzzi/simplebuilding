package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.net.CellInfoPayload;
import com.simplelib.api.client.ui.UiBoxes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The line under the crosshair when the player looks at a locked cell next to the free area (S3): what it costs and whether the
 * player can pay it. White on a dark plate = 15:1, green #7CFC7C = 11:1 and red #FF8888 = 6.4:1 on the plate #202020.
 */
public final class ChallengeHud {
    static final int PLATE = 0xD0202020, TEXT = 0xFFFFFFFF, OK = 0xFF7CFC7C, NO = 0xFFFF8888;
    private static final int[] FRAME = {0xD01E1F23, 0xD0555555};

    private ChallengeHud() {}

    /** The price as text: "16 x Dirt", "3 credits", "2 levels". */
    public static Component priceText(CellInfoPayload p) {
        return switch (p.currency()) {
            case "credits" -> Component.translatable("simplechallenges.price.credits", p.count());
            case "xp" -> Component.translatable("simplechallenges.price.xp", p.count());
            default -> Component.literal(p.count() + " x ").append(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(p.item()))).getHoverName());
        };
    }

    public static void render(GuiGraphicsExtractor g) {
        CellInfoPayload p = ChallengeClient.looked();
        Minecraft mc = Minecraft.getInstance();
        if (!p.show() || mc.player == null || mc.gui.hud.isHidden()) return;
        var font = mc.font;
        Component title = Component.translatable("simplechallenges.hud.locked");
        Component price = priceText(p);
        Component have = Component.translatable("simplechallenges.hud.have", p.have());
        Component verdict = Component.translatable(p.canPay() ? "simplechallenges.hud.can" : "simplechallenges.hud.cannot");
        int w = Math.max(Math.max(font.width(title), font.width(price) + 4 + font.width(have)), font.width(verdict)) + 14;
        int h = 4 * 11 + 8;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 14;
        UiBoxes.stairFrame(g, x, y, w, h, FRAME, PLATE);
        g.text(font, title, x + 7, y + 5, TEXT, false);
        g.text(font, price, x + 7, y + 16, TEXT, false);
        g.text(font, have, x + w - 7 - font.width(have), y + 16, p.canPay() ? OK : NO, false);
        g.text(font, verdict, x + 7, y + 27, p.canPay() ? OK : NO, false);
        g.text(font, Component.translatable("simplechallenges.hud.at", p.blockX(), p.blockZ()), x + 7, y + 38, 0xFFB8B8B8, false);
    }
}
