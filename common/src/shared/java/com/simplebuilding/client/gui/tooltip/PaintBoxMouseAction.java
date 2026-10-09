package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.component.PaintBoxContents;
import com.simplebuilding.items.custom.PaintBoxItem;
import com.simplebuilding.networking.ReinforcedBundleSelectionPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

/**
 * Mouse wheel over a paint box: brings the next held colour to the front ({@link PaintBoxItem#nextSelection}), on the
 * client stack and through the bundle selection packet on the server. Unlike a bundle the choice stays when the mouse
 * leaves the box, so the next right-click still takes that colour.
 */
public class PaintBoxMouseAction implements ItemSlotMouseAction {
    private final Minecraft client;
    private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();

    public PaintBoxMouseAction(Minecraft client) {
        this.client = client;
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.hasItem() && slot.getItem().getItem() instanceof PaintBoxItem;
    }

    @Override
    public boolean onMouseScrolled(double horizontal, double vertical, int slotId, ItemStack stack) {
        PaintBoxContents contents = PaintBoxItem.contents(stack);
        if (contents.isEmpty()) return false;
        Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(horizontal, vertical);
        int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
        if (wheel != 0) {
            int current = contents.selected() == PaintBoxContents.NONE ? PaintBoxItem.frontColor(contents) : contents.selected();
            int updated = PaintBoxItem.nextSelection(contents, current, -wheel);
            if (updated != contents.selected() && this.client.getConnection() != null) {
                PaintBoxItem.select(stack, updated);
                ClientNetworking.send(new ReinforcedBundleSelectionPayload(slotId, updated));
            }
        }
        return true;
    }

    @Override
    public void onSlotClicked(Slot slot, ContainerInput actionType) {
    }

    @Override
    public void onStopHovering(Slot slot) {
    }
}
