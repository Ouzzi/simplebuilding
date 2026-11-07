package com.simplelib.mixin.client;

import com.simplelib.api.client.SlotAlert;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@link SlotAlert} in the hotbar and the offhand slot (WH26-10, principle A14: show the effect where it comes from). */
@Mixin(Hud.class)
public abstract class HotbarAlertMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void simplelib$alert(GuiGraphicsExtractor g, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        if (SlotAlert.active(stack)) SlotAlert.draw(g, x, y);
    }
}
