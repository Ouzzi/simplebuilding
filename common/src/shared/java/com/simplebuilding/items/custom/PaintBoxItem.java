package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.component.PaintBoxContents;
import com.simplebuilding.items.tooltip.PaintBoxTooltipData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Paint box (owner 2026-10-09, round 3): keeps dyes like a bundle, but only dyes and at most one stack of each colour
 * times the tier factor ({@link #capacity}: basic 64, reinforced 128, netherite 256, enderite 512 - the factor of
 * {@code StackLimits.max}). Clicks as on a bundle: left-click puts the dyes in, right-click takes up to a stack of the
 * colour in front (scroll wheel over the box, {@link #nextSelection}) or else of the first colour held. For the
 * {@link ColorBrushItem} a filled box is ink like a loose dye; each stroke takes a random colour of it (not the block's).
 */
public final class PaintBoxItem extends Item {
    private final int multiplier;

    public PaintBoxItem(Properties properties, int multiplier) {
        super(properties.stacksTo(1).component(ModDataComponentTypes.PAINT_BOX, PaintBoxContents.EMPTY));
        this.multiplier = multiplier;
    }

    /** Dyes of one colour this box holds: 64 times the tier factor. */
    public int capacity() {
        return 64 * this.multiplier;
    }

    public static PaintBoxContents contents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.PAINT_BOX, PaintBoxContents.EMPTY);
    }

    /** The vanilla dye item of a colour. */
    public static Item dyeItem(DyeColor color) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_dye"));
    }

    /** A box holding at least one dye. */
    public static boolean hasDyes(ItemStack stack) {
        return stack.getItem() instanceof PaintBoxItem && !contents(stack).isEmpty();
    }

    /**
     * Puts as many of {@code dyes} into {@code box} as fit (shrinking {@code dyes}); returns how many went in. Anything
     * that is not a vanilla dye stays out.
     */
    public static int insert(ItemStack box, ItemStack dyes) {
        DyeColor color = ColorBrushItem.dyeColor(dyes);
        if (color == null || !(box.getItem() instanceof PaintBoxItem item)) return 0;
        PaintBoxContents contents = contents(box);
        int moved = Math.min(dyes.getCount(), item.capacity() - contents.count(color.getId()));
        if (moved <= 0) return 0;
        box.set(ModDataComponentTypes.PAINT_BOX, contents.with(color.getId(), contents.count(color.getId()) + moved));
        dyes.shrink(moved);
        return moved;
    }

    /** The colour a click takes out: the one in front if the box still holds it, else the first held; -1 if empty. */
    public static int frontColor(PaintBoxContents contents) {
        if (contents.selected() != PaintBoxContents.NONE && contents.count(contents.selected()) > 0) return contents.selected();
        for (int c = 0; c < PaintBoxContents.COLORS; c++) if (contents.count(c) > 0) return c;
        return -1;
    }

    /** Takes up to a stack (64) of the front colour out; empty if the box is empty. */
    public static ItemStack takeFront(ItemStack box) {
        PaintBoxContents contents = contents(box);
        int color = frontColor(contents);
        if (color < 0) return ItemStack.EMPTY;
        Item dye = dyeItem(DyeColor.byId(color));
        int taken = Math.min(contents.count(color), dye.getDefaultMaxStackSize());
        box.set(ModDataComponentTypes.PAINT_BOX, contents.with(color, contents.count(color) - taken));
        return new ItemStack(dye, taken);
    }

    /**
     * Scroll wheel: the next held colour after {@code selected} in wheel direction ({@code wheel} > 0 forwards), wrapping
     * around; {@code selected} when nothing else is held, {@link PaintBoxContents#NONE} when the box is empty.
     */
    public static int nextSelection(PaintBoxContents contents, int selected, int wheel) {
        int start = selected == PaintBoxContents.NONE ? frontColor(contents) : selected;
        if (start < 0) return PaintBoxContents.NONE;
        if (selected == PaintBoxContents.NONE || wheel == 0) return start;
        int step = wheel > 0 ? 1 : -1;
        for (int i = 1; i <= PaintBoxContents.COLORS; i++) {
            int c = Math.floorMod(start + step * i, PaintBoxContents.COLORS);
            if (contents.count(c) > 0) return c;
        }
        return start;
    }

    /** Sets the colour in front (the scroll wheel; the client and the bundle selection packet both call this). */
    public static void select(ItemStack box, int color) {
        if (box.getItem() instanceof PaintBoxItem) {
            box.set(ModDataComponentTypes.PAINT_BOX, contents(box).withSelected(color));
        }
    }

    /** A random held colour that differs from {@code avoid}, uniform over the colours held; null if none. */
    public static DyeColor pickColor(ItemStack box, DyeColor avoid, RandomSource random) {
        PaintBoxContents contents = contents(box);
        List<Integer> candidates = new ArrayList<>();
        for (int c = 0; c < PaintBoxContents.COLORS; c++) {
            if (contents.count(c) > 0 && (avoid == null || avoid.getId() != c)) candidates.add(c);
        }
        return candidates.isEmpty() ? null : DyeColor.byId(candidates.get(random.nextInt(candidates.size())));
    }

    /** Takes one dye of {@code color} out. */
    public static void removeOne(ItemStack box, DyeColor color) {
        PaintBoxContents contents = contents(box);
        box.set(ModDataComponentTypes.PAINT_BOX, contents.with(color.getId(), contents.count(color.getId()) - 1));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack box, Slot slot, ClickAction action, Player player) {
        if (box.getCount() != 1) return false;
        ItemStack other = slot.getItem();
        if (action == ClickAction.PRIMARY && !other.isEmpty()) {
            if (ColorBrushItem.dyeColor(other) == null || !slot.allowModification(player)) {
                playInsertFail(player);
                return true;
            }
            ItemStack taken = slot.safeTake(other.getCount(), Integer.MAX_VALUE, player);
            insert(box, taken);
            if (!taken.isEmpty()) slot.safeInsert(taken);
            if (taken.isEmpty()) playInsert(player); else playInsertFail(player);
            changed(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack out = takeFront(box);
            if (!out.isEmpty()) {
                ItemStack rest = slot.safeInsert(out);
                if (!rest.isEmpty()) insert(box, rest);
                else playRemove(player);
            }
            changed(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack box, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess carried) {
        if (box.getCount() != 1) return false;
        if (action == ClickAction.PRIMARY && !other.isEmpty()) {
            if (slot.allowModification(player) && insert(box, other) > 0) playInsert(player);
            else playInsertFail(player);
            changed(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            if (slot.allowModification(player)) {
                ItemStack out = takeFront(box);
                if (!out.isEmpty()) {
                    playRemove(player);
                    carried.set(out);
                }
            }
            changed(player);
            return true;
        }
        return false;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new PaintBoxTooltipData(contents(stack), capacity()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.paint_box", capacity())
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.paint_box.scroll")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13f * contents(stack).total() / (PaintBoxContents.COLORS * capacity())));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return net.minecraft.util.ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F); // vanilla bundle bar colour
    }

    private static void changed(Player player) {
        if (player.containerMenu != null) player.containerMenu.slotsChanged(player.getInventory());
    }

    private static void playInsert(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playRemove(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFail(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }
}
