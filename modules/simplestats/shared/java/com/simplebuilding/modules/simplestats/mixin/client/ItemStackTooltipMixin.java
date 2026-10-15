package com.simplebuilding.modules.simplestats.mixin.client;

import com.simplebuilding.modules.simplestats.client.StatsTooltip;
import java.util.function.Consumer;
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

/** Client only: the statistics lines go last, after the lines of the item and of other mods. */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void simplestats$tooltip(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag,
                                     Consumer<Component> builder, CallbackInfo ci) {
        StatsTooltip.append((ItemStack) (Object) this, builder);
    }
}
