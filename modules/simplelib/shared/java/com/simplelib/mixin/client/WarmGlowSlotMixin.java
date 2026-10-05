package com.simplelib.mixin.client;

import com.simplelib.warm.Warm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Warm glow in every container screen (owner: a glow around the items, strength follows the remaining
 * warmth; answer 41: four steps). Drawn behind the item as a soft orange halo. Hand and dropped items
 * follow in a later step (plan section 12).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class WarmGlowSlotMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void simplelib$glow(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = slot.getItem();
        var level = Minecraft.getInstance().level;
        if (stack.isEmpty() || level == null) return;
        float warmth = Warm.warmth(stack, level.getGameTime());
        if (warmth <= 0) return;
        int step = Math.min(4, 1 + (int) (warmth * 4));
        int alpha = 0x18 + step * 0x14;
        int outer = (alpha / 2) << 24 | 0xFF9A3C;
        int inner = alpha << 24 | 0xFFB347;
        int x = slot.x, y = slot.y;
        g.fill(x - 1, y - 1, x + 17, y + 17, outer);
        g.fill(x + 1, y + 1, x + 15, y + 15, inner);
    }
}
