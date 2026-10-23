package com.simpletrims.mixin.client;

import com.simpletrims.TrimGlow;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upgrade state of an armor piece (nowhere else visible, AGENTS.md rule 14): Radiance level, Glowing, Pulsating, Inverted. */
@Mixin(Item.class)
public class ItemTooltipMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void simpletrims$upgradeLines(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                          Consumer<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        int emission = TrimGlow.getEmissionLevel(stack);
        int glow = TrimGlow.getGlowLevel(stack);
        if (emission > 0) lines.accept(Component.translatable("tooltip.simpletrims.radiance_level", emission).withStyle(ChatFormatting.GOLD));
        if (glow > 0) lines.accept(Component.translatable("tooltip.simpletrims.glow_level").withStyle(ChatFormatting.AQUA));
        if (TrimGlow.isInverted(stack)) {
            lines.accept(Component.translatable("tooltip.simpletrims.inverted").withStyle(ChatFormatting.DARK_GRAY));
            lines.accept(Component.translatable("tooltip.simpletrims.inverted.desc").withStyle(ChatFormatting.GRAY));
        }
        if (TrimGlow.isPulsating(stack)) {
            lines.accept(Component.translatable("tooltip.simpletrims.pulsating").withStyle(ChatFormatting.DARK_AQUA));
            lines.accept(Component.translatable(glow > 0 ? "tooltip.simpletrims.pulsating.glow" : "tooltip.simpletrims.pulsating.color")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
