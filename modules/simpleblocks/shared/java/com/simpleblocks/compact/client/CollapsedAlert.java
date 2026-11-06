package com.simpleblocks.compact.client;

import com.simpleblocks.compact.CollapsedEffect;
import com.simplelib.api.client.SlotAlert;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** SlotAlert rule of the Collapsed blocks: red beam only while the Slowness from the carried stacks really acts on the player. */
public final class CollapsedAlert {
    private CollapsedAlert() {}

    public static void register() {
        SlotAlert.register((player, stack) -> {
            if (!CollapsedEffect.isCollapsed(stack)) return false;
            int amp = CollapsedEffect.amplifier(CollapsedEffect.stacks(player.getInventory()));
            MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
            return amp >= 0 && slow != null && slow.getAmplifier() >= amp;
        });
    }
}
