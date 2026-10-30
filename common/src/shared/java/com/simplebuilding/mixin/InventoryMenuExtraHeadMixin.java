package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.TrimStatsLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The extra head slot (owner N49, {@link ExtraHead}) in the player inventory: added after the off-hand slot (index 46,
 * Vanilla's indices stay), above the off-hand slot, level with the helmet slot. Shift-click puts a head there first (IG31-10:
 * also with an empty helmet slot; a carved pumpkin still goes to the helmet slot first); out of it, Vanilla's last branch
 * moves it into the inventory.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuExtraHeadMixin extends AbstractContainerMenu {
    protected InventoryMenuExtraHeadMixin(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplebuilding$addExtraHeadSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        this.addSlot(new ExtraHead.HeadSlot(owner, TrimStatsLayout.HEAD_SLOT_X, TrimStatsLayout.HEAD_SLOT_Y));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$quickMoveIntoExtraHead(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex < InventoryMenu.INV_SLOT_START || slotIndex >= InventoryMenu.USE_ROW_SLOT_END || slotIndex >= this.slots.size()) {
            return;
        }
        Slot from = this.slots.get(slotIndex);
        Slot head = extraHeadSlot();
        // IG31-10: a head goes into the extra head slot first, even with the helmet slot empty (not Vanilla's helmet slot);
        // a carved pumpkin keeps Vanilla's way (helmet slot first)
        if (head == null || head.hasItem() || !from.hasItem() || !ExtraHead.fits(from.getItem())
                || (from.getItem().is(net.minecraft.world.item.Items.CARVED_PUMPKIN) && !this.slots.get(InventoryMenu.ARMOR_SLOT_START).hasItem())) {
            return;
        }
        ItemStack stack = from.getItem();
        ItemStack clicked = stack.copy();
        if (!this.moveItemStackTo(stack, head.index, head.index + 1, false)) {
            return;
        }
        if (stack.isEmpty()) {
            from.setByPlayer(ItemStack.EMPTY, clicked);
        } else {
            from.setChanged();
        }
        from.onTake(player, stack);
        cir.setReturnValue(ItemStack.EMPTY);
    }

    @org.spongepowered.asm.mixin.Unique
    private @Nullable Slot extraHeadSlot() {
        for (int i = this.slots.size() - 1; i > InventoryMenu.SHIELD_SLOT; i--) {
            if (this.slots.get(i) instanceof ExtraHead.HeadSlot slot) {
                return slot;
            }
        }
        return null;
    }
}
