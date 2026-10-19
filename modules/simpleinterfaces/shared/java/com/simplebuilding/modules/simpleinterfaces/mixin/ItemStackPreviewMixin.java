package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.ContainerPreviewTooltip;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client: shulker / ender chest tooltip image; the Vanilla shulker content lines stay out while the grid shows. */
@Mixin(ItemStack.class)
public abstract class ItemStackPreviewMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$preview(CallbackInfoReturnable<Optional<TooltipComponent>> ci) {
        var preview = ContainerPreviewTooltip.of((ItemStack) (Object) this);
        if (preview != null) ci.setReturnValue(Optional.of(preview));
    }

    @Inject(method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/component/TooltipProvider$Getter;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$hideList(DataComponentType<?> type, TooltipProvider.Getter<?> getter, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
        if (type != DataComponents.CONTAINER) return;
        var preview = ContainerPreviewTooltip.of((ItemStack) (Object) this);
        if (preview != null && preview.grid() && preview.replacesList()) ci.cancel();
    }
}
