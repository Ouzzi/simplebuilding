package com.simplelib.api.client;

import com.simplelib.SimpleLib;
import com.simplelib.api.ModpackGuide;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The top bookmarks of the Simple Modpack guide, one per section (super-mod), standing up from the top edge of the book
 * (b-guide01, owner 03.11.). SimpleLib's own {@link ModpackGuideScreen} and every section's own book screen (SimpleBuilding's)
 * call {@link #render} before they draw the cover - the lower part of each bookmark hides under it, like the side bookmarks -
 * and {@link #click} first in their mouse handling. The sprite is the side bookmark of the book sheet turned by 90 degrees.
 */
public final class ModpackGuideBar {
    public static final Identifier TEXTURE = SimpleLib.id("textures/gui/modpack_guide/book.png");
    public static final int TEX_W = 512, TEX_H = 256, BOOK_W = 292, BOOK_H = 180;
    /** First bookmark's left edge from the book's, distance between two, visible size above the cover. */
    static final int FIRST_X = 18, STEP = 22, WIDTH = 20, HEIGHT = 22, LIFT = 3;
    /** A section's own screen reports its "More Simple mods" bookmark here (GUI pixels; null: none), for the test probe. */
    private static volatile int @Nullable [] sideLink;

    private static List<String> lastIds = List.of();
    private static List<int[]> lastRects = List.of();

    private ModpackGuideBar() {}

    /** Where the book stands on a screen of this size (the same for every guide screen, so switching never jumps). */
    public static int bookX(int width) {
        return (width - BOOK_W) / 2;
    }

    public static int bookY(int height) {
        return Math.max(HEIGHT + LIFT, (height - BOOK_H) / 2);
    }

    /** Draws the bookmarks and returns the hovered one's tooltip (null: none hovered). {@code current} = the open section. */
    public static @Nullable Component render(GuiGraphicsExtractor g, int bx, int by, @Nullable String current, int mouseX, int mouseY) {
        List<ModpackGuide.Section> sections = ModpackGuideScreens.sections();
        Component hovered = null;
        List<String> ids = new ArrayList<>();
        List<int[]> rects = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            ModpackGuide.Section section = sections.get(i);
            boolean selected = section.id().equals(current);
            int x = bx + FIRST_X + i * STEP, y = by - HEIGHT - (selected ? LIFT : 0);
            // The side bookmark (30x20, attached at its left end) turned upright: attached end at the bottom, under the cover.
            g.pose().pushMatrix();
            g.pose().translate(x, y + 30);
            g.pose().rotate((float) (-Math.PI / 2));
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, selected ? 32 : 0, 184, 30, 20, TEX_W, TEX_H);
            g.pose().popMatrix();
            g.item(icon(section.icon()), x + 2, y + 3);
            int[] rect = {x, y, WIDTH, HEIGHT + (selected ? LIFT : 0)};
            ids.add(section.id());
            rects.add(rect);
            if (in(mouseX, mouseY, rect)) hovered = Component.translatable(section.name());
        }
        lastIds = List.copyOf(ids);
        lastRects = List.copyOf(rects);
        return hovered;
    }

    /** A click on a bookmark opens that section (its own screen if it has one); true when the click hit a bookmark. */
    public static boolean click(double mouseX, double mouseY, @Nullable String current, @Nullable InteractionHand hand) {
        for (int i = 0; i < lastRects.size(); i++) {
            if (!in(mouseX, mouseY, lastRects.get(i))) continue;
            String id = lastIds.get(i);
            // The open section's bookmark leads back to its main view (its own screen or the contents).
            ModpackGuideScreens.open(id, hand);
            return true;
        }
        return false;
    }

    /** The bookmarks of the last frame: section ids and rectangles (GUI pixels), for the test probe. */
    public static List<String> ids() {
        return lastIds;
    }

    public static List<int[]> rects() {
        return lastRects;
    }

    public static void reportSideLink(int @Nullable [] rect) {
        sideLink = rect;
    }

    public static int @Nullable [] sideLink() {
        return sideLink;
    }

    /** The item of an icon id, empty when it is not registered. */
    public static ItemStack icon(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public static boolean in(double mx, double my, int[] r) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
