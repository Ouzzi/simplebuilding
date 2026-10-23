package com.simplemaps.mixin;

import com.simplemaps.MapsConfig;
import com.simplemaps.MegaWayfinderMapItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owner B23-K2: mega wayfinder map + the item of a dimension at the anvil unlocks that dimension. Only that pair is
 * taken over (computed on the server, like Simple Models' anvil requests); everything else stays Vanilla's.
 */
@Mixin(AnvilMenu.class)
public abstract class MegaAnvilMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;

    protected MegaAnvilMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(type, id, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplemaps$megaUnlock(CallbackInfo ci) {
        if (!(player instanceof ServerPlayer sp)) return;
        ItemStack out = MegaWayfinderMapItem.anvilResult(inputSlots.getItem(0), inputSlots.getItem(1), sp.level());
        if (out.isEmpty()) return;
        ci.cancel();
        resultSlots.setItem(0, out);
        cost.set(MapsConfig.megaUnlockLevels);
        repairItemCountCost = 1;
        broadcastChanges();
    }
}
