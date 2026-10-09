package com.simplebuilding.items.custom;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

/**
 * Paint palette (owner 2026-10-09): a bundle that only takes dyes (64 in all, the bundle weight). For the
 * {@link ColorBrushItem} it is ink like a loose dye, found in the same bow order; each stroke uses a random one of its
 * colours that differs from the block's colour and takes exactly that dye out.
 */
public final class PaintPaletteItem extends BundleItem {
    public PaintPaletteItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.paint_palette")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    private static boolean acceptable(ItemStack stack) {
        return stack.isEmpty() || ColorBrushItem.dyeColor(stack) != null;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack palette, Slot slot, ClickAction action, Player player) {
        return acceptable(slot.getItem()) && super.overrideStackedOnOther(palette, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack palette, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess carried) {
        return acceptable(other) && super.overrideOtherStackedOnMe(palette, other, slot, action, player, carried);
    }

    private static List<ItemStackTemplate> contents(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? List.of() : contents.items();
    }

    /** A palette holding at least one dye. */
    public static boolean hasDyes(ItemStack stack) {
        if (!(stack.getItem() instanceof PaintPaletteItem)) return false;
        for (ItemStackTemplate entry : contents(stack)) {
            if (entry.count() > 0 && ColorBrushItem.dyeColor(entry.create()) != null) return true;
        }
        return false;
    }

    /** Colour of the entry at {@code index}. */
    public static DyeColor colorAt(ItemStack palette, int index) {
        return ColorBrushItem.dyeColor(contents(palette).get(index).create());
    }

    /** A random entry whose colour differs from {@code avoid}, uniform over the colours held; -1 if none. */
    public static int pickIndex(ItemStack palette, DyeColor avoid, RandomSource random) {
        List<ItemStackTemplate> items = contents(palette);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            DyeColor color = ColorBrushItem.dyeColor(items.get(i).create());
            if (items.get(i).count() > 0 && color != null && color != avoid) candidates.add(i);
        }
        return candidates.isEmpty() ? -1 : candidates.get(random.nextInt(candidates.size()));
    }

    /** Takes one dye out of the entry at {@code index}. */
    public static void removeOne(ItemStack palette, int index) {
        List<ItemStackTemplate> items = new ArrayList<>(contents(palette));
        ItemStackTemplate entry = items.get(index);
        if (entry.count() <= 1) items.remove(index);
        else items.set(index, entry.withCount(entry.count() - 1));
        palette.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
    }
}
