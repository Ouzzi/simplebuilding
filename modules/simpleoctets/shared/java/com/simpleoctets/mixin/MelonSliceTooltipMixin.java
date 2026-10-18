package com.simpleoctets.mixin;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The melon slice tells how it becomes an octet (sneak + right-click; AGENTS rule 14 (a): not obvious). */
@Mixin(Item.class)
public abstract class MelonSliceTooltipMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void simpleoctets$melonSliceHint(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> out, TooltipFlag flag, CallbackInfo ci) {
        if ((Object) this == Items.MELON_SLICE && com.simpleoctets.registry.OctetRegistry.MELON_OCTET != null
                && com.simpleoctets.SimpleOctets.on()) {
            out.accept(Component.translatable("tooltip.simpleoctets.melon_slice.place").withStyle(ChatFormatting.GRAY));
        }
    }
}
