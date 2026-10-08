package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.ModScreenLayout.Box;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackSlot;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.util.DyedStorage;
import com.simplebuilding.util.HopperFilterMode;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSymbols;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 26.3: the mod screens in the container style (owner images 3/4; previews {@code g4-*.png} of the simplecontainers
 * plan, group G4) drawn with SimpleLib's style blocks ({@code com.simplelib.api.client.ui}, principle 6a) - without
 * simplecontainers. Only the picture changes: menus, slots and the hopper filter behave as before. The 26.2 twin in
 * common/src/mc26_2/java answers {@code false} everywhere, the screens then draw their old look.
 */
public final class ModScreenStyle {
    /** Whether this line draws the style (26.2: false). */
    public static final boolean ACTIVE = true;

    /** Leather backpack (W0-B palette table "backpack"). */
    static final UiPalette LEATHER = UiPalette.derived(0xFF8E6440);
    static final UiPalette AUTO_SMITHER = UiPalette.derived(0xFF4F5560);
    static final UiPalette FLETCHING = UiPalette.derived(0xFFC5B485);
    private static final int REDSTONE_ON = 0xFFD8261E;
    private static final int ERROR = 0xFFC9503E;

    private ModScreenStyle() {}

    // ------------------------------------------------------------------ palettes

    static UiPalette tier(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    static UiMotifs.Kind motif(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiMotifs.Kind.METAL;
            case NETHERITE -> UiMotifs.Kind.NETHER;
            case ENDERITE -> UiMotifs.Kind.ENDER;
        };
    }

    /** The hopper's tier from its block (Netherite when unknown). */
    static ChestTier hopperTier(NetheriteHopperScreenHandler menu) {
        ModHopperBlockEntity be = menu.getBlockEntity();
        Block block = be == null ? null : be.getBlockState().getBlock();
        if (block == ModBlocks.REINFORCED_HOPPER) return ChestTier.REINFORCED;
        if (block == ModBlocks.ENDERITE_HOPPER) return ChestTier.ENDERITE;
        return ChestTier.NETHERITE;
    }

    /** Backpack colours: a dyed backpack in its dye (muted like a shulker box), Enderite violet, else leather. */
    static UiPalette backpack(BackpackTier tier, int dye) {
        if (dye != DyedStorage.UNDYED) return UiPalette.derived(UiPalette.mix(0xFF000000 | dye, 0xFF808080, 0.4));
        return tier == BackpackTier.ENDERITE ? UiPalette.ENDERITE : LEATHER;
    }

    // ------------------------------------------------------------------ tiered chests

    public static boolean tieredChest(GuiGraphicsExtractor g, TieredChestMenu menu, Font font, Component title, int left, int top) {
        UiPalette p = tier(menu.tier());
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(menu.imageWidth(), inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        if (menu.tier().stackMultiplier() > 1) {
            int w = font.width(bonus(menu.tier()));
            avoid.add(new int[] {left + menu.imageWidth() - 8 - w - 12, top + 4, left + menu.imageWidth() - 4, top + 15});
        }
        motif(g, left, top, container, p, motif(menu.tier()), avoid, 3);
        slots(g, menu, left, top, p);
        return true;
    }

    /** Title in the box's label colour, the stack factor as a symbol + "xN" on its right; no inventory label. */
    public static boolean tieredChestLabels(GuiGraphicsExtractor g, Font font, TieredChestMenu menu, Component title, int titleX, int titleY) {
        UiPalette p = tier(menu.tier());
        g.text(font, title, titleX, titleY, p.label(), false);
        if (menu.tier().stackMultiplier() > 1) {
            String bonus = bonus(menu.tier());
            int x = menu.imageWidth() - 8 - font.width(bonus);
            g.text(font, bonus, x, titleY, p.label(), false);
            UiSymbols.draw(g, UiSymbols.STACK, x - 10, titleY, p.label(), 0);
        }
        return true;
    }

    private static String bonus(ChestTier tier) {
        return "x" + tier.stackMultiplier();
    }

    // ------------------------------------------------------------------ hoppers

    /** Filter key: 4 px right of the slots, 18x18, the same place and action as before. */
    public static Button hopperFilterButton(int x, int y, Button.OnPress onPress, NetheriteHopperScreenHandler menu) {
        return new FilterButton(x, y, onPress, menu);
    }

    public static boolean hopper(GuiGraphicsExtractor g, NetheriteHopperScreenHandler menu, Font font, Component title, int left, int top,
            int imageWidth) {
        ChestTier tier = hopperTier(menu);
        UiPalette p = tier(tier);
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {left + 138, top + 19, left + 157, top + 38});
        motif(g, left, top, container, p, motif(tier), avoid, 7);
        slots(g, menu, left, top, p);
        // Filter ghosts (only while a filter is on): the item faint behind a veil, like the crucible's reserved slots.
        if (menu.getBlockEntity() instanceof ModHopperBlockEntity be && menu.getSyncedFilterMode() != HopperFilterMode.NONE) {
            g.nextStratum();
            for (int i = 0; i < 5; i++) {
                Slot slot = menu.slots.get(i);
                ItemStack ghost = be.getGhostItem(i);
                if (!ghost.isEmpty() && slot.getItem().isEmpty()) g.fakeItem(ghost, left + slot.x, top + slot.y);
            }
            g.nextStratum();
            for (int i = 0; i < 5; i++) {
                Slot slot = menu.slots.get(i);
                if (!be.getGhostItem(i).isEmpty() && slot.getItem().isEmpty()) UiBoxes.veil(g, left + slot.x, top + slot.y, p);
            }
        }
        return true;
    }

    public static boolean hopperLabels(GuiGraphicsExtractor g, Font font, NetheriteHopperScreenHandler menu, Component title, int x, int y) {
        g.text(font, title, x, y, tier(hopperTier(menu)).label(), false);
        return true;
    }

    /** The filter key: raised, an engraved funnel, the mode as a badge (off: red slash, exact: green check, kind: three squares). */
    private static final class FilterButton extends Button {
        private final NetheriteHopperScreenHandler menu;

        FilterButton(int x, int y, Button.OnPress onPress, NetheriteHopperScreenHandler menu) {
            super(x, y, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
            this.menu = menu;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            UiPalette p = tier(hopperTier(this.menu));
            int x = this.getX(), y = this.getY();
            UiBoxes.raised(g, x, y, 18, 18, UiPalette.mix(p.fill(), 0xFFFFFFFF, this.isHoveredOrFocused() ? 0.32 : 0.18));
            UiSymbols.draw(g, UiSymbols.FUNNEL, x + 3, y + 3, p.slotTop(), 0);
            switch (this.menu.getSyncedFilterMode()) {
                case NONE -> {
                    for (int k = 0; k < 14; k++) g.fill(x + 2 + k, y + 15 - k, x + 4 + k, y + 16 - k, 0xFFD8402F);
                }
                case WHITELIST -> UiSymbols.draw(g, UiSymbols.CHECK, x + 10, y + 11, 0xFF55FF55, 0);
                case TYPE -> {
                    for (int k = 0; k < 3; k++) g.fill(x + 9 + k * 3, y + 13, x + 11 + k * 3, y + 15, 0xFFFFE055);
                }
            }
        }
    }

    // ------------------------------------------------------------------ auto smither

    public static boolean autoSmither(GuiGraphicsExtractor g, AutoSmitherMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        UiPalette p = AUTO_SMITHER;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(AutoSmitherMenu.RESULT_SLOT);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {left + 98, top + 22, left + 110, top + 34});
        avoid.add(new int[] {left + 99, top + 36, left + 121, top + 51});
        avoid.add(bigRect(result, left, top));
        motif(g, left, top, container, p, UiMotifs.Kind.REDSTONE, avoid, 4);
        slots(g, menu, left, top, p, result);
        if (menu.isPowered()) UiSymbols.draw(g, UiSymbols.REDSTONE, left + 98, top + 22, REDSTONE_ON, p.light());
        else UiSymbols.engraved(g, UiSymbols.REDSTONE, left + 98, top + 22, p);
        if (menu.hasRecipeError()) UiSymbols.draw(g, UiSymbols.ARROW, left + 99, top + 36, ERROR, p.light());
        else UiSymbols.engraved(g, UiSymbols.ARROW, left + 99, top + 36, p);
        return true;
    }

    public static boolean autoSmitherLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, AUTO_SMITHER.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ fletching table

    public static boolean fletching(GuiGraphicsExtractor g, FletchingMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        UiPalette p = FLETCHING;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(FletchingMenu.RESULT_SLOT);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(bigRect(result, left, top));
        avoid.add(new int[] {left + 92, top + 36, left + 114, top + 51});
        avoid.add(new int[] {left + 30, top + 17, left + 87, top + 70});
        motif(g, left, top, container, p, UiMotifs.Kind.WOOD, avoid, 6);
        // The three part slots sit on a diagonal: an engraved arrow shaft runs through them.
        List<int[]> parts = new ArrayList<>();
        for (int i : new int[] {FletchingMenu.TIP_SLOT, FletchingMenu.SHAFT_SLOT, FletchingMenu.FLETCHING_SLOT}) {
            Slot s = menu.getSlot(i);
            parts.add(new int[] {s.x, s.y, s.x + 17, s.y + 17});
        }
        for (int k = -6; k < 50; k++) {
            int x = 36 + k, y = 59 - k;
            if (inside(x, y, parts)) continue;
            g.fill(left + x, top + y + 1, left + x + 1, top + y + 2, p.light());
            g.fill(left + x, top + y, left + x + 2, top + y + 1, p.slot());
        }
        slots(g, menu, left, top, p, result);
        UiSymbols.engraved(g, UiSymbols.ARROW, left + 92, top + 36, p);
        return true;
    }

    public static boolean fletchingLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, FLETCHING.label(), false);
        return true;
    }

    private static boolean inside(int x, int y, List<int[]> rects) {
        for (int[] r : rects) {
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ backpack

    /**
     * The backpack in one box (W0-B "ein Kasten": only 5 free rows above the backpack rows): armour, player preview,
     * offhand and 2x2 crafting on the block colour, the backpack rows and the extra columns on a tinted strip of the
     * light inventory panel behind a seam.
     */
    /** Slot colour of the styled backpack's own slots (client tests check the drawn slots against it). */
    public static int backpackSlotColor(BackpackMenu menu) {
        return backpack(menu.tier(), menu.openData().dyeColor()).slot();
    }

    public static boolean backpack(GuiGraphicsExtractor g, BackpackMenu menu, BackpackLayout layout, int left, int top) {
        UiPalette p = backpack(menu.tier(), menu.openData().dyeColor());
        UiMotifs.Kind kind = menu.openData().dyeColor() == DyedStorage.UNDYED && menu.tier() == BackpackTier.ENDERITE
                ? UiMotifs.Kind.ENDER : UiMotifs.Kind.LEATHER;
        Box all = ModScreenLayout.backpack(layout);
        int vx = layout.vanillaX();
        box(g, left, top, all, p);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.isActive() && slot.y < BackpackLayout.FIRST_ROW_Y) avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + vx + 26, top + 8, left + vx + 76, top + 78});
        avoid.add(new int[] {left + vx + 135, top + 29, left + vx + 151, top + 40});
        int seam = ModScreenLayout.BACKPACK_SEAM_Y;
        UiMotifs.draw(g, left + ModScreenLayout.FRAME, top + ModScreenLayout.FRAME, all.width() - 2 * ModScreenLayout.FRAME,
                seam - ModScreenLayout.FRAME, p, kind, avoid, 7);
        UiBoxes.seam(g, left + ModScreenLayout.FRAME, top + seam, all.width() - 2 * ModScreenLayout.FRAME,
                all.height() - ModScreenLayout.FRAME_BOTTOM - seam, p);
        UiBoxes.inset(g, left + vx + 26, top + 8, 49, 70, p);
        UiSymbols.engraved(g, UiSymbols.ARROW_SMALL, left + vx + 135, top + 29, p);
        int tint = UiPalette.mix(UiPalette.INVENTORY.fill(), p.fill(), 0.28);
        int[] strip = ModScreenLayout.backpackStrip(layout);
        if (menu.tier().rows() > 0) g.fill(left + strip[0], top + strip[1], left + strip[2], top + strip[3], tint);
        for (Slot slot : menu.slots) {
            if (slot instanceof BackpackSlot b && b.isExtraColumn()) {
                int y = slot.y == BackpackLayout.FIRST_ROW_Y ? slot.y - 2 : slot.y;
                g.fill(left + slot.x - 2, top + y, left + slot.x + 18, top + slot.y + 18, tint);
            }
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            boolean own = slot instanceof BackpackSlot || slot.y < BackpackLayout.FIRST_ROW_Y;
            UiBoxes.slot(g, left + slot.x, top + slot.y, own ? p : UiPalette.INVENTORY);
        }
        return true;
    }

    // ------------------------------------------------------------------ shared pieces

    private static void box(GuiGraphicsExtractor g, int left, int top, Box b, UiPalette p) {
        UiBoxes.box(g, left + b.x(), top + b.y(), b.width(), b.height(), p);
    }

    private static void motif(GuiGraphicsExtractor g, int left, int top, Box box, UiPalette p, UiMotifs.Kind kind, List<int[]> avoid, int seed) {
        Box in = box.inner();
        UiMotifs.draw(g, left + in.x(), top + in.y(), in.width(), in.height(), p, kind, avoid, seed);
    }

    /** Title rectangle the motifs keep off (the preview's rule). */
    private static int[] titleRect(Font font, Component title, int left, int top) {
        return new int[] {left + 4, top + 4, left + 12 + font.width(title), top + 15};
    }

    /** The 24x24 result slot's rectangle (with light edge). */
    private static int[] bigRect(Slot slot, int left, int top) {
        return new int[] {left + slot.x - 4, top + slot.y - 4, left + slot.x + 21, top + slot.y + 21};
    }

    /** Container-side slot rectangles (with light edge) in screen coordinates. */
    private static List<int[]> slotRects(AbstractContainerMenu menu, int left, int top) {
        List<int[]> rects = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            rects.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        return rects;
    }

    private static void slots(GuiGraphicsExtractor g, AbstractContainerMenu menu, int left, int top, UiPalette p) {
        slots(g, menu, left, top, p, null);
    }

    /** Every active slot: player slots light, the container's in {@code p}; {@code big} as the 24x24 result slot. */
    private static void slots(GuiGraphicsExtractor g, AbstractContainerMenu menu, int left, int top, UiPalette p, Slot big) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == big) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, p);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : p);
        }
    }
}
