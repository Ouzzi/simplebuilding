package com.simpleores.item;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

/** A food whose tooltip lists every status effect it gives, like a potion (B02, 02.11.). */
public class FoodEffectsItem extends Item {
    public FoodEffectsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) {
            return;
        }
        boolean header = false;
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
                for (MobEffectInstance instance : apply.effects()) {
                    if (!header) {
                        out.accept(Component.translatable("tooltip.simpleores.when_eaten").withStyle(net.minecraft.ChatFormatting.GRAY));
                        header = true;
                    }
                    MutableComponent name = Component.translatable(instance.getDescriptionId());
                    if (instance.getAmplifier() > 0) {
                        name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + instance.getAmplifier()));
                    }
                    int total = instance.getDuration() / 20;
                    name = Component.translatable("potion.withDuration", name, Component.literal(total / 60 + ":" + (total % 60 < 10 ? "0" : "") + total % 60));
                    out.accept(Component.literal(" ").append(name).withStyle(instance.getEffect().value().getCategory().getTooltipFormatting()));
                }
            }
        }
    }
}
