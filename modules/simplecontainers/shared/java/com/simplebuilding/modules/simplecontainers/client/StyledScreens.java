package com.simplebuilding.modules.simplecontainers.client;

import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout.Layout;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StyleContext;
import com.simplebuilding.modules.simplecontainers.style.StationStyles;
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

    private StyledScreens() {}

    /** The style that applies to {@code screen} right now, or {@code null} (Vanilla). */
    public static @Nullable ScreenStyle style(AbstractContainerScreen<?> screen) {
        ScreenStyle style;
        try {
            style = ContainerStyles.find(screen.getMenu().getType(), screen.getClass().getName());
        } catch (UnsupportedOperationException e) {
            style = ContainerStyles.findMenuless(screen.getClass().getName()); // the player inventory menu has no type
        }
        return style != null && ContainersClient.config().isOn(style.id()) ? style : null;
    }

    /** Box layout of {@code screen} from its active slots, or {@code null}. */
    public static @Nullable Layout layout(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) slots.add(new BoxLayout.Slot(slot.x, slot.y, StationScreens.playerSlot(slot)));
        }
        return BoxLayout.compute(slots, imageWidth, imageHeight, titleY);
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
        MenuType<?> menu;
        try {
            menu = screen.getMenu().getType();
        } catch (UnsupportedOperationException e) {
            menu = null; // the player inventory menu has no type
        }
        return new StyleContext(menu, key, block);
    }

    /**
     * Draws the styled background at {@code left, top}; returns {@code false} (and draws nothing) when the screen
     * stays Vanilla.
     */
    public static boolean drawBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top,
            int imageWidth, int imageHeight, int titleY) {
        ScreenStyle style = style(screen);
        if (style == null) return false;
        Layout layout = layout(screen, imageWidth, imageHeight, titleY);
        if (layout == null) return false;
        UiPalette block = palette(screen, style);
        drawBoxes(g, left, top, layout, block);
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : block);
        }
        return true;
    }

    /**
     * Labels of a styled screen (coordinates relative to the image): the title in the box's label colour; the
     * inventory label is left out - in Vanilla's geometry it would sit on the divider between the boxes (image 3 has
     * none either). Returns {@code false} for Vanilla screens.
     */
    public static boolean drawLabels(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, Font font, Component title,
            int titleX, int titleY, int imageWidth, int imageHeight) {
        ScreenStyle style = style(screen);
        if (style == null || layout(screen, imageWidth, imageHeight, titleY) == null) return false;
        // G3 screens: the title at (8, 6) as in the W0-B preview (Vanilla moves it for the anvil, smithing table ...).
        boolean station = StationStyles.STYLES.contains(style);
        g.text(font, title, station ? 8 : titleX, station ? 6 : titleY, palette(screen, style).label(), false);
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

    /**
     * Tint for an empty-slot icon on {@code screen}: {@code -1} = Vanilla (screen not styled), {@code 0} = leave it out
     * (the brewing stand's blaze powder slot shows its fuel level instead), else the colour for
     * {@code blitSprite(..., color)}. Vanilla's icons are one grey (104, lapis 85); the tint scales it to the slot's
     * top-line colour at 80 % so it reads as an engraved silhouette.
     */
    public static int slotIconColor(AbstractContainerScreen<?> screen, Slot slot, net.minecraft.resources.Identifier icon,
            int imageWidth, int imageHeight, int titleY) {
        ScreenStyle style = style(screen);
        if (style == null || layout(screen, imageWidth, imageHeight, titleY) == null) return -1;
        String path = icon.getPath();
        if (path.equals("container/slot/brewing_fuel")) return 0;
        UiPalette p = StationScreens.playerSlot(slot) ? UiPalette.INVENTORY : palette(screen, style);
        int grey = path.equals("container/slot/lapis_lazuli") ? 85 : 104;
        int top = p.slotTop(), color = 0xCC000000;
        for (int shift = 16; shift >= 0; shift -= 8) color |= Math.min(255, ((top >> shift) & 255) * 255 / grey) << shift;
        return color;
    }
}
