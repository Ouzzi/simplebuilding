package com.simplemusic.mixin;

import com.simplemusic.sheet.SheetCopy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sheet music copies at the cartography table (owner 21.10., rule in {@link SheetCopy}). Priority 1200: applied after
 * Simple Maps (1000) and SimpleBuilding's blueprint table (1100), so it wraps their slots instead of being replaced.
 */
@Mixin(value = CartographyTableMenu.class, priority = 1200)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private ResultContainer resultContainer;

    protected CartographyTableMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void simplemusic$wrapSlots(int id, Inventory inventory, ContainerLevelAccess levelAccess, CallbackInfo ci) {
        SheetCopy.wrapSlots(this, this.slots, levelAccess);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simplemusic$copySheet(ItemStack map, ItemStack additional, ItemStack current, CallbackInfo ci) {
        if (!com.simplemusic.SimpleMusic.on() || !SheetCopy.involves(map, additional)) return;
        ci.cancel();
        access.execute((level, pos) -> {
            ItemStack result = SheetCopy.copy(map, additional);
            if (!ItemStack.matches(result, resultContainer.getItem(SheetCopy.RESULT_SLOT))) resultContainer.setItem(SheetCopy.RESULT_SLOT, result);
            broadcastChanges();
        });
    }

    /** Shift-click puts sheets into the table's slots instead of the hotbar. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simplemusic$quickMoveSheet(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index <= SheetCopy.RESULT_SLOT || index >= this.slots.size()) return;
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        int target = SheetCopy.quickMoveTarget(stack);
        if (target < 0) return;
        ItemStack before = stack.copy();
        if (!this.moveItemStackTo(stack, target, target + 1, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        cir.setReturnValue(before);
    }
}
