package com.simplelib.mixin;

import com.simplelib.api.LegendaryMoments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Legendary moments (SP22-2): taking an anvil result compares it with the left input (still in slot 0 at this point), so
 * only a newly reached maximum level of a legendary enchantment counts.
 */
@Mixin(AnvilMenu.class)
public abstract class LegendaryAnvilMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void simplelib$legendary(Player player, ItemStack carried, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            LegendaryMoments.enchanted(serverPlayer, ((AnvilMenu) (Object) this).getSlot(0).getItem(), carried);
        }
    }
}
