package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.ClaimRights;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Claim deeds at the cartography table (SP22-8 §5): an empty deed on top and nothing below gives a deed bound to the
 * player at the table (always possible: the way back to one's own deed); a bound original on top and an empty deed below
 * gives a copy, but only for the owner or a manager (M) of the owner's land. Another person's deed or a copy gives
 * nothing. {@code CartographyDeedMixin} wraps the table's slots (never replaces them, so Simple Maps, the blueprint and
 * sheet music keep working: whatever is not a deed goes on to the wrapped slot).
 */
public final class DeedCartography {
    public static final int MAP_SLOT = 0, ADDITIONAL_SLOT = 1, RESULT_SLOT = 2;

    private DeedCartography() {}

    public static boolean isDeed(ItemStack stack) { return !stack.isEmpty() && stack.is(ClaimDeedItem.item()); }
    public static boolean isBlank(ItemStack stack) { return isDeed(stack) && !stack.has(DeedData.TYPE); }

    /** Whether a deed lies in the table, so the result is ours (and never Vanilla's or another mod's). */
    public static boolean involves(ItemStack map, ItemStack additional) { return isDeed(map) || isDeed(additional); }

    /** May {@code actor} copy the deed of {@code owner}: the owner, or a manager (M) of the owner's land (C8). */
    static boolean mayCopy(MinecraftServer server, UUID owner, UUID actor) {
        if (owner.equals(actor)) return true;
        var claims = server == null ? null : Claims.get(server);
        var land = claims == null ? null : claims.lands().get(owner);
        return land != null && (land.top().of(actor) & ClaimRights.MANAGE) != 0;
    }

    /** The result for {@code player}, or empty. */
    public static ItemStack result(Player player, MinecraftServer server, ItemStack map, ItemStack additional) {
        if (player == null || server == null || !Claims.enabled(server)) return ItemStack.EMPTY;
        if (isBlank(map) && additional.isEmpty()) {
            String name = player.getPlainTextName();
            if (name.length() > DeedData.MAX_NAME) name = name.substring(0, DeedData.MAX_NAME);
            ItemStack own = map.copyWithCount(1);
            own.set(DeedData.TYPE, new DeedData(player.getUUID(), name, 0));
            return own;
        }
        DeedData data = isDeed(map) ? map.get(DeedData.TYPE) : null;
        if (data == null || !data.copyable() || !isBlank(additional) || !mayCopy(server, data.owner(), player.getUUID())) return ItemStack.EMPTY;
        ItemStack copy = map.copyWithCount(1);
        copy.set(DeedData.TYPE, data.copy());
        return copy;
    }

    /** Wraps the three table slots: inputs also take deeds, taking a result uses up the empty deed. */
    public static void wrapSlots(AbstractContainerMenu menu, List<Slot> slots, ContainerLevelAccess access) {
        slots.set(MAP_SLOT, new InputSlot(slots.get(MAP_SLOT), DeedCartography::isDeed));
        slots.set(ADDITIONAL_SLOT, new InputSlot(slots.get(ADDITIONAL_SLOT), DeedCartography::isBlank));
        slots.set(RESULT_SLOT, new ResultSlot(slots.get(RESULT_SLOT), menu, access));
    }

    /** Shift-click target for a deed: bound ones and empty ones on top, an empty one below when the top is taken; -1 = not ours. */
    public static int quickMoveTarget(ItemStack stack, ItemStack top) {
        if (!Claims.on() || !isDeed(stack)) return -1;
        return isBlank(stack) && !top.isEmpty() ? ADDITIONAL_SLOT : MAP_SLOT;
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
            return original.mayPlace(stack) || Claims.on() && extra.test(stack);
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
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public void onTake(Player player, ItemStack carried) {
            ItemStack top = menu.getSlot(MAP_SLOT).getItem();
            if (!involves(top, menu.getSlot(ADDITIONAL_SLOT).getItem())) {
                original.onTake(player, carried);
                return;
            }
            // Own deed: the empty deed on top becomes it. Copy: only the empty deed below is used, the original stays.
            menu.getSlot(isBlank(top) ? MAP_SLOT : ADDITIONAL_SLOT).remove(1);
            access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F));
            setChanged();
        }

        @Override
        public ItemStack safeClone(Player player) { return original.safeClone(player); }
    }
}
