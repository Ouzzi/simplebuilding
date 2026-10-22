package com.simplehammers.mixin.client;

import com.simplehammers.engine.UpgradeHints;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The upgrade hint ({@link UpgradeHints}) under every block item the hammer upgrades in the world (Queue 738). */
@Mixin(Item.class)
public class HammerTooltipMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void simplehammers$upgradeHints(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> out, TooltipFlag flag, CallbackInfo ci) {
        UpgradeHints.lines(stack).forEach(out);
    }
}
