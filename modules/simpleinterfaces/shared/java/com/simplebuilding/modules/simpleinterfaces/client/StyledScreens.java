package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import com.simplebuilding.framework.api.ContainerStyleHints;
import com.simplebuilding.modules.simpleinterfaces.mixin.SlotPositionAccessor;
import com.simplebuilding.modules.simpleinterfaces.style.BoxLayout;
import com.simplebuilding.modules.simpleinterfaces.style.BoxMotifs;
import com.simplebuilding.modules.simpleinterfaces.style.BoxLayout.Layout;
import com.simplebuilding.modules.simpleinterfaces.style.ContainerStyles;
import com.simplebuilding.modules.simpleinterfaces.style.ScreenStyle;
import com.simplebuilding.modules.simpleinterfaces.style.StationStyles;
import com.simplebuilding.modules.simpleinterfaces.style.StyleContext;
import com.simplebuilding.modules.simpleinterfaces.style.WorkStyles;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
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
    /** Whether the last background of a screen was drawn in the style. */
    private static final Map<AbstractContainerScreen<?>, Boolean> STYLED = new WeakHashMap<>();

    static {
        // optional coupling (framework): other mods skip decorations made for the Vanilla look, e.g. SimpleBuilding's
        // astral vault tint (the style draws the astral rows itself, StorageDecors.astralVault)
        ContainerStyleHints.register("simpleinterfaces", screen -> screen instanceof AbstractContainerScreen<?> s && isStyled(s));
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
        if (!ContainersConfig.on() || !UiStyleToggle.isEnabled()) return null;
        ScreenStyle style = ContainerStyles.find(menuType(screen), screen.getClass().getName());
        return style != null && ContainersClient.config().isOn(style.id()) ? style : null;
    }

    /** IG31-30: whether the Vanilla hopper is drawn in the style right now (its screen then moves the player's slots down). */
    public static boolean hopperStyleOn() {
        return ContainersConfig.on() && UiStyleToggle.isEnabled() && ContainersClient.config().isOn("hopper");
    }

    /** The menu type of {@code screen}, or {@code null} (mount inventories and the player inventory have none). */
    static @Nullable MenuType<?> menuType(AbstractContainerScreen<?> screen) {
        try {
            return screen.getMenu().getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /** Whether the last frame of {@code screen} drew its background in the style. */
    public static boolean isStyled(AbstractContainerScreen<?> screen) {
        return Boolean.TRUE.equals(STYLED.get(screen));
    }

    /**
     * Records whether {@code screen}'s background was drawn in the style this frame, for screens drawn outside
     * {@link #drawBackground} (the G3 screens, {@link StationScreens#draw}); {@link #isStyled} and the framework's
     * restyling hint read it (owner 2026-10-21: SimpleBuilding's extra head slot frame).
     */
    static void markStyled(AbstractContainerScreen<?> screen, boolean styled) {
        STYLED.put(screen, styled);
    }

    /**
     * Box layout of {@code screen} from its active slots and its static container elements ({@link #elements}), or {@code null}.
     * N64: every styled screen keeps the fixed container margin ({@link BoxLayout#margin}) except villager trading, whose offer
     * list beside the inventory leaves no room for it at Vanilla's places ({@link BoxLayout#compute}, owner question).
     */
    public static @Nullable Layout layout(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (measured(slot, imageWidth)) slots.add(new BoxLayout.Slot(LinkedPanel.homeX(slot), LinkedPanel.homeY(slot), playerSlot(slot)));
        }
        ScreenStyle style = style(screen);
        List<BoxLayout.Rect> elements = elements(screen, style);
        return marginStyle(style) ? BoxLayout.margin(slots, elements, imageWidth, imageHeight, titleY)
                : BoxLayout.compute(slots, elements, imageWidth, imageHeight, titleY);
    }

    /** N64: whether {@code style} uses the container margin (all but villager trading). */
    public static boolean marginStyle(@Nullable ScreenStyle style) {
        return style != null && !style.id().equals("merchant");
    }

    /**
     * Container elements besides the slots by style id (N64: static, so {@link #applyMargin} before {@code init} and the drawing
     * count the same): the stations' fields and symbols ({@link StationStyles#ELEMENTS}), the work screens' ({@link WorkStyles#ELEMENTS}),
     * the big result slot of either ({@code RESULT_SLOT}, 24 x 24 + light edge), the crafter's and the mounts' decor.
     */
    public static List<BoxLayout.Rect> elements(AbstractContainerScreen<?> screen, @Nullable ScreenStyle style) {
        if (style == null) return List.of();
        String id = style.id();
        List<BoxLayout.Rect> out = new ArrayList<>(StationStyles.ELEMENTS.getOrDefault(id, WorkStyles.ELEMENTS.getOrDefault(id, List.of())));
        out.addAll(StorageDecors.elements(id));
        Integer result = StationStyles.RESULT_SLOT.getOrDefault(id, WorkStyles.RESULT_SLOT.get(id));
        if (result != null && result < screen.getMenu().slots.size()) {
            Slot s = screen.getMenu().getSlot(result);
            out.add(new BoxLayout.Rect(s.x - 4, s.y - 4, 25, 25));
        }
        return out;
    }

    /** Vanilla image height per open screen (recorded by {@link #applyMargin}). */
    private static final Map<AbstractContainerScreen<?>, Integer> VANILLA_HEIGHT = new WeakHashMap<>();

    /**
     * N64: a recipe book button that Vanilla places relative to the screen's middle ({@code height / 2 - k}, made for its own
     * image height) kept at its Vanilla place in the image when the margin made the image taller; the crafting table's 3 px
     * further right (Vanilla x 5 touches the box's light line). {@code pos} unchanged when the screen is not styled.
     */
    public static net.minecraft.client.gui.navigation.ScreenPosition recipeButton(AbstractContainerScreen<?> screen,
            net.minecraft.client.gui.navigation.ScreenPosition pos, int screenHeight, int imageHeight, int topPos) {
        Integer vanilla = VANILLA_HEIGHT.get(screen);
        if (vanilla == null || !marginStyle(style(screen))) return pos;
        int dx = screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen ? UiBoxes.MARGIN : 0;
        int vanillaTop = (screenHeight - vanilla) / 2;
        return new net.minecraft.client.gui.navigation.ScreenPosition(pos.x() + dx, topPos + pos.y() - vanillaTop);
    }

    /** Vanilla item y of each player slot moved by {@link #applyMargin} (weak: menus that close drop out). */
    private static final Map<Slot, Integer> VANILLA_Y = new WeakHashMap<>();

    /**
     * N62/N64: at every {@code init}, before it centres the screen - puts the stations' moved slots in place
     * ({@code StationScreens.moveSlots}), then the player's slots of this (client) menu at their Vanilla y plus
     * {@link Layout#shift} (style on) or back at their Vanilla y (style off), and returns the image height the margin needs
     * ({@code vanillaHeight} otherwise). Idempotent: the player inventory's menu lives as long as the player, its slots must
     * never move twice; the Vanilla y is remembered per slot.
     */
    public static int applyMargin(AbstractContainerScreen<?> screen, int imageWidth, int vanillaHeight, int titleY) {
        VANILLA_HEIGHT.put(screen, vanillaHeight);
        StationScreens.moveSlots(screen);
        var slots = screen.getMenu().slots;
        for (Slot slot : slots) {
            Integer y = VANILLA_Y.get(slot);
            if (y != null && slot.y != y) ((SlotPositionAccessor) slot).qol$setY(y);
        }
        ScreenStyle style = style(screen);
        Layout layout = marginStyle(style) ? layout(screen, imageWidth, vanillaHeight, titleY) : null;
        if (layout == null) return vanillaHeight;
        if (layout.shift() > 0) {
            for (Slot slot : slots) {
                if (!playerSlot(slot) || !inImage(slot, imageWidth)) continue;
                VANILLA_Y.putIfAbsent(slot, slot.y);
                ((SlotPositionAccessor) slot).qol$setY(slot.y + layout.shift());
            }
        }
        return layout.height();
    }

    /**
     * Whether {@code slot} counts for the box layout: {@link #inImage} at the place its screen put it (N65: the linked panel
     * moves the player's slots into its hotbar strip and inventory tab - the style still measures them at home), the marked
     * containers' own slots never.
     */
    public static boolean measured(Slot slot, int imageWidth) {
        if (slot instanceof com.simplebuilding.modules.simpleinterfaces.linked.LinkedSlot || !slot.isActive()) return false;
        int x = LinkedPanel.homeX(slot);
        return x >= 0 && x + 16 <= imageWidth;
    }

    /**
     * Whether {@code slot} is active and inside the screen image horizontally. Slots outside belong to side panels
     * other mods attach (simpleriding's hoof panel at x -20, linked panels): they keep their own look and do not count
     * for the boxes.
     */
    public static boolean inImage(Slot slot, int imageWidth) {
        return slot.isActive() && slot.x >= 0 && slot.x + 16 <= imageWidth;
    }

    /**
     * The player's own inventory and hotbar ({@link Inventory} index &lt; 36): they make up the inventory box. Armour,
     * shield and other containers' slots belong to the container box (player inventory screen, G3).
     */
    public static boolean playerSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
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
        return new StyleContext(menuType(screen), key, block);
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
        ScreenStyle style = style(screen);
        Layout layout = style == null ? null : layout(screen, imageWidth, imageHeight, titleY);
        STYLED.put(screen, layout != null);
        if (layout == null) return false;
        UiPalette block = palette(screen, style);
        drawBoxes(g, left, top, layout, block);
        if (layout.container() != null) motif(screen, g, left, top, layout, block, decor);
        for (Slot slot : screen.getMenu().slots) {
            if (!inImage(slot, imageWidth)) continue;
            UiPalette p = playerSlot(slot) ? UiPalette.INVENTORY : decor.slotPalette(slot, block);
            if (decor.bigSlot(slot)) {
                UiBoxes.bigSlot(g, left + slot.x, top + slot.y, p);
            } else {
                UiBoxes.slot(g, left + slot.x, top + slot.y, p);
            }
        }
        decor.draw(g, left, top, block);
        return true;
    }

    /** Title position in a styled screen (image coordinates): the preview draws every title at (8, 6) (README W0-B). */
    public static final int TITLE_X = 8, TITLE_Y = 6;

    /**
     * Image 4's faint marks ({@link UiMotifs}, kind and seed from {@link BoxMotifs}) in the container box's fill area,
     * clear of the container slots, the decor's elements and the title (image coordinates).
     */
    private static void motif(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, Layout layout,
            UiPalette block, Decor decor) {
        BoxMotifs.Entry motif = BoxMotifs.of(block);
        if (motif.motif() == UiMotifs.Kind.NONE) return;
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || playerSlot(slot)) continue;
            avoid.add(decor.bigSlot(slot) ? new int[] {slot.x - 4, slot.y - 4, slot.x + 21, slot.y + 21}
                    : new int[] {slot.x, slot.y, slot.x + 17, slot.y + 17});
        }
        for (BoxLayout.Rect r : decor.elements()) avoid.add(new int[] {r.x(), r.y(), r.x() + r.width(), r.y() + r.height()});
        int tx = TITLE_X;
        avoid.add(new int[] {tx - 4, TITLE_Y - 2, tx + 4 + Minecraft.getInstance().font.width(screen.getTitle()), TITLE_Y + 9});
        BoxLayout.Rect c = layout.container();
        int bottom = switch (layout.variant()) {
            case TWO_BOXES -> c.bottom() - UiBoxes.FRAME_BOTTOM;
            case NO_SHADOW -> c.bottom() - UiBoxes.FRAME;
            case SEAM -> layout.inventory().y();
        };
        int y = c.y() + UiBoxes.FRAME;
        UiMotifs.draw(g, left, top, motif.motif(), c.x() + UiBoxes.FRAME, y, c.width() - 2 * UiBoxes.FRAME, bottom - y, block, avoid, motif.seed());
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
        // every styled screen: the title at (8, 6) as in the W0-B preview (Vanilla centres or moves it for the dispenser,
        // crafter, furnaces, anvil, smithing table ...)
        // (VIS24-8 lifted the cartography title to Vanilla's y 4; since WD25-19 its map field sits lower, the title is at 6)
        g.text(font, title, TITLE_X, TITLE_Y, palette(screen, style).label(), false);
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
     * {@code blitSprite(..., color)}: the slot palette's icon colour (SimpleLib {@link com.simplelib.api.client.ui.UiPalette#slotIcon},
     * owner N37: the same contrast to the slot as Vanilla's icons in every theme).
     */
    public static int slotIconColor(AbstractContainerScreen<?> screen, Slot slot, net.minecraft.resources.Identifier icon,
            int imageWidth, int imageHeight, int titleY) {
        ScreenStyle style = style(screen);
        if (style == null || layout(screen, imageWidth, imageHeight, titleY) == null) return -1;
        if (icon.getPath().equals("container/slot/brewing_fuel")) return 0;
        UiPalette p = playerSlot(slot) ? UiPalette.INVENTORY : palette(screen, style);
        return com.simplelib.api.client.ui.UiSlotIcons.tint(p, icon);
    }
}
