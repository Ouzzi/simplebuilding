package com.simplemaps.mixin;

import com.simplemaps.Cartography;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wayfinder maps at the cartography table (owner F5): wider input slots and our own results. */
@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final public Container container;
    @Shadow @Final private ResultContainer resultContainer;

    protected CartographyTableMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void simplemaps$wideSlots(int id, Inventory inventory, ContainerLevelAccess levelAccess, CallbackInfo ci) {
        replace(0, Cartography.mapSlot(container));
        replace(1, Cartography.additionalSlot(container));
    }

    private void replace(int index, Slot slot) {
        slot.index = index;
        this.slots.set(index, slot);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simplemaps$wayfinderResult(ItemStack map, ItemStack additional, ItemStack current, CallbackInfo ci) {
        if (!Cartography.involves(map, additional)) return;
        ci.cancel();
        access.execute((level, pos) -> {
            if (!(level instanceof ServerLevel server)) return;
            ItemStack result = Cartography.result(map, additional, server);
            if (result.isEmpty()) {
                resultContainer.removeItemNoUpdate(2);
            } else if (!ItemStack.matches(result, current)) {
                resultContainer.setItem(2, result);
            }
            broadcastChanges();
        });
    }
}
