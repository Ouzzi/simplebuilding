package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
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

/** Combining anything with Soulbound on the right costs the full {@code soulboundAnvilCost} (35 levels, owner 2026-10-12). */
@Mixin(value = AnvilMenu.class, priority = 1500)
public abstract class AnvilSoulboundMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;

    protected AnvilSoulboundMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
                                  ItemCombinerMenuSlotDefinition slots) {
        super(type, containerId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void simpletools$soulboundCost(CallbackInfo ci) {
        ItemStack result = this.resultSlots.getItem(0);
        if (result.isEmpty() || !Soulbound.carries(this.inputSlots.getItem(1)) || !Soulbound.has(result) && !Soulbound.carries(result)) return;
        this.cost.set(SimpleTools.config().soulboundAnvilCost);
    }
}
