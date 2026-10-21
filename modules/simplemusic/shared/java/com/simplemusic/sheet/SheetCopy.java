package com.simplemusic.sheet;

import com.simplemusic.registry.MusicComponents;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Copying sheets of music at the cartography table like a blueprint (owner 21.10.): a signed sheet on top, an empty
 * unsigned one below -> a copy; only the lower sheet is used up, the original stays. A composition gives an unsigned,
 * editable copy (code and title, no author) as the blueprint does; an upload sheet gives an exact copy (track, title,
 * uploader - there is nothing to edit). {@code CartographyTableMenuMixin} wraps the table's slots (never replaces
 * them, so SimpleBuilding's blueprint and Simple Maps' wayfinder slots keep working) and fills the result.
 */
public final class SheetCopy {
    public static final int MAP_SLOT = 0, ADDITIONAL_SLOT = 1, RESULT_SLOT = 2;

    private SheetCopy() {}

    /** Top slot: a signed sheet (composition or upload). */
    public static boolean isOriginal(ItemStack stack) {
        return stack.getItem() instanceof SheetMusicItem && SheetMusicItem.content(stack).signed();
    }

    /** Lower slot: an empty sheet - no code, no track, not signed. */
    public static boolean isBlank(ItemStack stack) {
        return stack.getItem() instanceof SheetMusicItem && SheetMusicItem.content(stack).isBlank()
                && !SheetMusicItem.content(stack).signed() && SheetMusicItem.track(stack) == null;
    }

    /** Whether the table holds a sheet on top, so the result is ours (and never Vanilla's or another mod's). */
    public static boolean involves(ItemStack map, ItemStack additional) {
        return map.getItem() instanceof SheetMusicItem;
    }

    /** The copy, or empty. */
    public static ItemStack copy(ItemStack original, ItemStack blank) {
        if (!isOriginal(original) || !isBlank(blank)) return ItemStack.EMPTY;
        if (SheetMusicItem.track(original) != null) return original.copyWithCount(1);
        SheetContent source = SheetMusicItem.content(original);
        ItemStack copy = blank.copyWithCount(1);
        copy.set(MusicComponents.SHEET, new SheetContent(source.code(), source.title(), "", false));
        return copy;
    }

    /** Wraps the three table slots: inputs also take sheets, taking a copy uses up only the lower sheet. */
    public static void wrapSlots(AbstractContainerMenu menu, List<Slot> slots, ContainerLevelAccess access) {
        slots.set(MAP_SLOT, new InputSlot(slots.get(MAP_SLOT), SheetCopy::isOriginal));
        slots.set(ADDITIONAL_SLOT, new InputSlot(slots.get(ADDITIONAL_SLOT), s -> s.getItem() instanceof SheetMusicItem));
        slots.set(RESULT_SLOT, new ResultSlot(slots.get(RESULT_SLOT), menu, access));
    }

    /** Shift-click target for a sheet from the inventory: signed ones on top, others below; -1 = not ours. */
    public static int quickMoveTarget(ItemStack stack) {
        if (!(stack.getItem() instanceof SheetMusicItem)) return -1;
        return isOriginal(stack) ? MAP_SLOT : ADDITIONAL_SLOT;
    }

    static final class InputSlot extends Slot {
        private final Slot original;
        private final Predicate<ItemStack> extra;

        InputSlot(Slot original, Predicate<ItemStack> extra) {
            super(original.container, original.getContainerSlot(), original.x, original.y);
            this.index = original.index;
            this.original = original;
            this.extra = extra;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return original.mayPlace(stack) || extra.test(stack);
        }
    }

    static final class ResultSlot extends Slot {
        private final Slot original;
        private final AbstractContainerMenu menu;
        private final ContainerLevelAccess access;

        ResultSlot(Slot original, AbstractContainerMenu menu, ContainerLevelAccess access) {
            super(original.container, original.getContainerSlot(), original.x, original.y);
            this.index = original.index;
            this.original = original;
            this.menu = menu;
            this.access = access;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            if (!(menu.getSlot(MAP_SLOT).getItem().getItem() instanceof SheetMusicItem)) {
                original.onTake(player, carried);
                return;
            }
            menu.getSlot(ADDITIONAL_SLOT).remove(1);
            access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F));
            setChanged();
        }

        @Override
        public ItemStack safeClone(Player player) {
            return original.safeClone(player);
        }
    }
}
