package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.tooltip.GuideTooltipData;
import com.simplebuilding.guide.GuideBooks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Each inserted chapter has its own miniature book and localized name. */
public record GuideTooltip(GuideTooltipData data) implements ClientTooltipComponent {
    public static ClientTooltipComponent create(GuideTooltipData data) { return new GuideTooltip(data); }
    @Override public int getHeight(Font font) { return data.books().size() * 12; }
    @Override public int getWidth(Font font) {
        return data.books().stream().mapToInt(b -> font.width(Component.translatable(b.key() + ".title")) + 14).max().orElse(0);
    }
    @Override public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
        for (GuideBooks.Book book : data.books()) {
            String name = book.isHub() ? book.itemName() : "guide_book_" + (book == GuideBooks.Book.PADS ? "tweaks" : book.id());
            Identifier texture = Identifier.fromNamespaceAndPath("simplebuilding", "textures/item/" + name + ".png");
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(0.625f, 0.625f);
            g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 16, 16, 16, 16);
            g.pose().popMatrix();
            g.text(font, Component.translatable(book.key() + ".title"), x + 14, y + 1, 0xFFAAAAAA, false);
            y += 12;
        }
    }
}
