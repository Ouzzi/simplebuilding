package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.networking.ReinforcedBundleSelectionPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.joml.Vector2i;

/**
 * The mouse-wheel submenu of the mod's bundles and quivers, a copy of vanilla's
 * {@code BundleMouseActions} (which only matches {@code minecraft:bundles}, a tag the mod's
 * bundles are deliberately not in).
 *
 * <p>Every change of the selection is written to the <em>client</em> stack and sent to the
 * server, never just one of the two: {@code BundleContents} neither compares nor transmits its
 * selected index, so a container sync can never carry it back. Leaving the slot, closing the
 * screen ({@code AbstractContainerScreen.onClose} ends the hover) and shift-click / number-key
 * swaps therefore close the bundle on both sides, exactly as vanilla - before 2026-09-28 only the
 * packet went out, and the client bundle stayed open until the next restart.
 */
public class ReinforcedBundleTooltipSubmenuHandler implements ItemSlotMouseAction {
    private final Minecraft client;
    private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();

    public ReinforcedBundleTooltipSubmenuHandler(Minecraft client) {
        this.client = client;
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.hasItem() && slot.getItem().getItem() instanceof ReinforcedBundleItem;
    }

    @Override
    public boolean onMouseScrolled(double horizontal, double vertical, int slotId, ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        int shown = contents == null ? 0 : contents.getNumberOfItemsToShow();
        if (shown == 0) return false;

        Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(horizontal, vertical);
        int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
        if (wheel != 0) {
            int selected = contents.getSelectedItem();
            int updated = ReinforcedBundleItem.nextScrollSelection(wheel, selected, shown);
            if (selected != updated) {
                select(stack, slotId, updated);
            }
        }
        return true;
    }

    @Override
    public void onSlotClicked(Slot slot, ClickType actionType) {
        if (actionType == ClickType.QUICK_MOVE || actionType == ClickType.SWAP) {
            select(slot.getItem(), slot.index, BundleContents.NO_SELECTED_ITEM_INDEX);
        }
    }

    @Override
    public void onStopHovering(Slot slot) {
        select(slot.getItem(), slot.index, BundleContents.NO_SELECTED_ITEM_INDEX);
    }

    private void select(ItemStack stack, int slotId, int index) {
        if (this.client.getConnection() == null) return;
        if (index == BundleContents.NO_SELECTED_ITEM_INDEX) {
            ReinforcedBundleItem.clearBundleSelection(stack);
        } else {
            ReinforcedBundleItem.setBundleSelectedItem(stack, index);
        }
        ClientNetworking.send(new ReinforcedBundleSelectionPayload(slotId, index));
    }
}
