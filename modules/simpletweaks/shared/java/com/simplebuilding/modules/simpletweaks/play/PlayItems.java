package com.simplebuilding.modules.simpletweaks.play;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Gives a stack into the inventory; what does not fit lands at the player's feet. */
public final class PlayItems {
    public static void give(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return;
        var rest = stack.copy();
        p.getInventory().add(rest);
        if (!rest.isEmpty()) {
            var e = new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), rest);
            e.setNoPickUpDelay();
            p.level().addFreshEntity(e);
        }
        p.containerMenu.broadcastChanges();
    }
    private PlayItems() {}
}
