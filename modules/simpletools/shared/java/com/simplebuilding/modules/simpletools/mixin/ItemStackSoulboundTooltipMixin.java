package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nachtrag 60: an item with Soulbound says so in its tooltip when the world is Hardcore, where it does nothing. */
@Mixin(ItemStack.class)
public abstract class ItemStackSoulboundTooltipMixin {
    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void simpletools$hardcoreHint(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player,
                                          TooltipFlag flag, Consumer<Component> builder, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (Soulbound.hardcore(player) && Soulbound.carries(self)) {
            builder.accept(Component.translatable("tooltip.simpletools.soulbound.hardcore").withStyle(ChatFormatting.GRAY));
        }
    }
}
