package com.simplebuilding.modules.simplecontainers.client;

import com.simplebuilding.framework.api.ContainerStyleHints;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.BoxMotifs;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout.Layout;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StyleContext;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the Simple style behind a Vanilla container screen (called from the background and label mixins). A screen
 * is styled when a {@link ScreenStyle} matches its exact class and menu type, the config has it on and
 * {@link BoxLayout} finds room for both boxes; otherwise nothing changes. The colours are picked once per screen
 * (title key + the block looked at when it opened).
 */
public final class StyledScreens {
    /** Picked palette per open screen; weak, so closed screens drop out. */
    private static final Map<AbstractContainerScreen<?>, UiPalette> PALETTES = new WeakHashMap<>();
    /** Extra elements of a screen (set by its background mixin each frame). */
    private static final Map<AbstractContainerScreen<?>, Decor> DECOR = new WeakHashMap<>();
    /** Whether the last background of a screen was drawn in the style. */
    private static final Map<AbstractContainerScreen<?>, Boolean> STYLED = new WeakHashMap<>();

    static {
        // optional coupling (framework): other mods skip decorations made for the Vanilla look, e.g. SimpleBuilding's
        // astral vault tint (the style draws the astral rows itself, StorageDecors.astralVault)
        ContainerStyleHints.register("simplecontainers", screen -> screen instanceof AbstractContainerScreen<?> s && isStyled(s));
    }

    private StyledScreens() {}

    /**
     * Screen-specific parts of the style beyond boxes and slots (the crafter's redstone, a mount's preview field ...).
     * Coordinates relative to the screen image.
     */
    public interface Decor {
        Decor NONE = new Decor() {};

        /** Container elements besides the slots (incl. light edges): they count for the box layout, marks avoid them. */
        default List<BoxLayout.Rect> elements() {
            return List.of();
        }

        /** Whether {@code slot} is drawn as the big result slot (24x24, image 4). */
        default boolean bigSlot(Slot slot) {
            return false;
        }

        /** Colours of a container slot ({@code block} = the container box's). */
        default UiPalette slotPalette(Slot slot, UiPalette block) {
            return block;
        }

        /** Draws the elements on the box, after the slots. */
        default void draw(GuiGraphicsExtractor g, int left, int top, UiPalette block) {}
    }

    /** The style that applies to {@code screen} right now, or {@code null} (Vanilla). */
    public static @Nullable ScreenStyle style(AbstractContainerScreen<?> screen) {
        MenuType<?> type;
        try {
            type = screen.getMenu().getType();
        } catch (UnsupportedOperationException e) {
            type = null; // mount inventories and the player inventory have no menu type
        }
        ScreenStyle style = ContainerStyles.find(type, screen.getClass().getName());
        return style != null && ContainersClient.config().isOn(style.id()) ? style : null;
    }

    /** Whether the last frame of {@code screen} drew its background in the style. */
    public static boolean isStyled(AbstractContainerScreen<?> screen) {
        return Boolean.TRUE.equals(STYLED.get(screen));
    }

    /** Box layout of {@code screen} from its active slots (and its decor's elements), or {@code null}. */
    public static @Nullable Layout layout(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) slots.add(new BoxLayout.Slot(slot.x, slot.y, slot.container instanceof Inventory));
        }
        return BoxLayout.compute(slots, DECOR.getOrDefault(screen, Decor.NONE).elements(), imageWidth, imageHeight, titleY);
    }

    /** The container box colours of {@code screen} (picked on first use). */
    public static UiPalette palette(AbstractContainerScreen<?> screen, ScreenStyle style) {
        return PALETTES.computeIfAbsent(screen, s -> style.palette().apply(context(s)));
    }

    private static StyleContext context(AbstractContainerScreen<?> screen) {
        String key = screen.getTitle().getContents() instanceof TranslatableContents t ? t.getKey() : null;
        String block = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            block = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(hit.getBlockPos()).getBlock()).toString();
        }
        return new StyleContext(screen.getMenu().getType(), key, block);
    }

    /**
     * Draws the styled background at {@code left, top}; returns {@code false} (and draws nothing) when the screen
     * stays Vanilla.
     */
    public static boolean drawBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top,
            int imageWidth, int imageHeight, int titleY) {
        return drawBackground(screen, g, left, top, imageWidth, imageHeight, titleY, Decor.NONE);
    }

    /** {@link #drawBackground(AbstractContainerScreen, GuiGraphicsExtractor, int, int, int, int, int)} with screen-specific elements. */
    public static boolean drawBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top,
            int imageWidth, int imageHeight, int titleY, Decor decor) {
        DECOR.put(screen, decor);
        ScreenStyle style = style(screen);
        Layout layout = style == null ? null : layout(screen, imageWidth, imageHeight, titleY);
        STYLED.put(screen, layout != null);
        if (layout == null) return false;
        UiPalette block = palette(screen, style);
        drawBoxes(g, left, top, layout, block);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            int x = left + slot.x, y = top + slot.y;
            avoid.add(decor.bigSlot(slot) ? new int[] {x - 4, y - 4, x + 21, y + 21} : new int[] {x, y, x + 17, y + 17});
        }
        for (BoxLayout.Rect r : decor.elements()) avoid.add(new int[] {left + r.x(), top + r.y(), left + r.x() + r.width(), top + r.y() + r.height()});
        if (layout.container() != null) {
            int tx = left + layout.container().x() + TITLE_X, ty = top + titleY;
            avoid.add(new int[] {tx - 4, ty - 2, tx + 4 + Minecraft.getInstance().font.width(screen.getTitle()), ty + 9});
            motif(g, left, top, layout, block, avoid);
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            UiPalette p = slot.container instanceof Inventory ? UiPalette.INVENTORY : decor.slotPalette(slot, block);
            if (decor.bigSlot(slot)) {
                UiBoxes.bigSlot(g, left + slot.x, top + slot.y, p);
            } else {
                UiBoxes.slot(g, left + slot.x, top + slot.y, p);
            }
        }
        decor.draw(g, left, top, block);
        return true;
    }

    /** Title position in a styled screen: 8 px right of the container box's left edge (README W0-B, "(8, 6)"). */
    public static final int TITLE_X = 8;

    /** Image 4's faint marks in the container box's fill area. */
    private static void motif(GuiGraphicsExtractor g, int left, int top, Layout layout, UiPalette block, List<int[]> avoid) {
        BoxMotifs.Entry motif = BoxMotifs.of(block);
        BoxLayout.Rect c = layout.container();
        int bottom = switch (layout.variant()) {
            case TWO_BOXES -> c.bottom() - UiBoxes.FRAME_BOTTOM;
            case NO_SHADOW -> c.bottom() - UiBoxes.FRAME;
            case SEAM -> layout.inventory().y();
        };
        int y = c.y() + UiBoxes.FRAME;
        UiBoxes.motif(g, left + c.x() + UiBoxes.FRAME, top + y, c.width() - 2 * UiBoxes.FRAME, bottom - y, block, motif.motif(), avoid, motif.seed());
    }

    /**
     * Colour (ARGB multiplier) for the empty-slot sprite of {@code slot} (saddle, armor, potion ...) on a styled screen:
     * an engraved silhouette in the slot's top-line colour at 80 % (README W0-B, point 5); {@code -1} = Vanilla.
     * Vanilla's sprites are flat #7C7C7C, so the multiplier is the target colour scaled by 255/124.
     */
    public static int iconTint(AbstractContainerScreen<?> screen, Slot slot) {
        ScreenStyle style = isStyled(screen) ? style(screen) : null;
        if (style == null) return -1;
        UiPalette p = slot.container instanceof Inventory ? UiPalette.INVENTORY
                : DECOR.getOrDefault(screen, Decor.NONE).slotPalette(slot, palette(screen, style));
        int t = p.slotTop();
        int r = Math.min(255, ((t >> 16) & 255) * 255 / 124), gr = Math.min(255, ((t >> 8) & 255) * 255 / 124), b = Math.min(255, (t & 255) * 255 / 124);
        return 0xCC000000 | r << 16 | gr << 8 | b;
    }

    /**
     * Labels of a styled screen (coordinates relative to the image): the title in the box's label colour; the
     * inventory label is left out - in Vanilla's geometry it would sit on the divider between the boxes (image 3 has
     * none either). Returns {@code false} for Vanilla screens.
     */
    public static boolean drawLabels(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, Font font, Component title,
            int titleX, int titleY, int imageWidth, int imageHeight) {
        ScreenStyle style = style(screen);
        Layout layout = style == null ? null : layout(screen, imageWidth, imageHeight, titleY);
        if (layout == null) return false;
        int x = layout.container() != null ? layout.container().x() + TITLE_X : titleX;
        g.text(font, title, x, titleY, palette(screen, style).label(), false);
        return true;
    }

    /** The boxes of {@code layout} at {@code left, top}: container box in {@code block}, inventory box or seam panel. */
    public static void drawBoxes(GuiGraphicsExtractor g, int left, int top, Layout layout, UiPalette block) {
        BoxLayout.Rect c = layout.container(), i = layout.inventory();
        if (layout.variant() == BoxLayout.Variant.SEAM) {
            UiBoxes.box(g, left + c.x(), top + c.y(), c.width(), c.height(), block);
            UiBoxes.seam(g, left + i.x(), top + i.y(), i.width(), i.height(), block);
            return;
        }
        if (c != null) UiBoxes.box(g, left + c.x(), top + c.y(), c.width(), c.height(), block, layout.variant() == BoxLayout.Variant.TWO_BOXES);
        UiBoxes.box(g, left + i.x(), top + i.y(), i.width(), i.height(), UiPalette.INVENTORY);
    }
}
