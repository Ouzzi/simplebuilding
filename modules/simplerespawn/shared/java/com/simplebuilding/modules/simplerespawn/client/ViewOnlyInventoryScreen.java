package com.simplebuilding.modules.simplerespawn.client;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * The Vanilla inventory, only to look at: clicks, drags, scrolling and keys are swallowed (no slot packet ever leaves
 * the client; the server refuses them anyway), tooltips still show. Esc or the inventory key go back to the downed
 * screen.
 */
public final class ViewOnlyInventoryScreen extends InventoryScreen {
    public ViewOnlyInventoryScreen(Player player) {
        super(player);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || this.minecraft.options.keyInventory.matches(event)) {
            this.minecraft.gui.setScreen(new DownedScreen());
        }
        return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
