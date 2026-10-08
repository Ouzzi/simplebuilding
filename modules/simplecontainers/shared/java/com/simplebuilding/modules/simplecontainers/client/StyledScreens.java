package com.simplebuilding.modules.simplecontainers.client;

import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
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
            return null; // the player inventory menu has no type
        }
        return style != null && ContainersClient.config().isOn(style.id()) ? style : null;
    }

    /** Box layout of {@code screen} from its active slots, or {@code null}. */
    public static @Nullable Layout layout(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) slots.add(new BoxLayout.Slot(slot.x, slot.y, slot.container instanceof Inventory));
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
        return new StyleContext(screen.getMenu().getType(), key, block);
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
        if (layout.container() != null) box(g, left, top, layout.container(), block);
        box(g, left, top, layout.inventory(), UiPalette.INVENTORY);
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
        g.text(font, title, titleX, titleY, palette(screen, style).label(), false);
        return true;
    }

    private static void box(GuiGraphicsExtractor g, int left, int top, BoxLayout.Rect r, UiPalette p) {
        UiBoxes.box(g, left + r.x(), top + r.y(), r.width(), r.height(), p);
    }
}
