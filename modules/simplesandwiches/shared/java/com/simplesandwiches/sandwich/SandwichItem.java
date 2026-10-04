package com.simplesandwiches.sandwich;

import com.simplesandwiches.registry.ModComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

/**
 * The sandwich. Eating, effects and sounds are plain Vanilla (FOOD + CONSUMABLE written by
 * {@link SandwichFormula}); this class only names it after its ingredients and lists them.
 */
public class SandwichItem extends Item {
    public SandwichItem(Properties properties) {
        super(properties);
    }

    public static SandwichContents contents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SANDWICH_CONTENTS, SandwichContents.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        List<Holder<Item>> in = contents(stack).ingredients();
        String base = "item.simplesandwiches.sandwich";
        return switch (in.size()) {
            case 0 -> contents(stack).buttered() ? Component.translatable(base + ".buttered") : super.getName(stack);
            case 1 -> Component.translatable(base + ".one", name(in.get(0)));
            case 2 -> Component.translatable(base + ".two", name(in.get(0)), name(in.get(1)));
            default -> Component.translatable(base + ".more", name(in.get(0)), name(in.get(1)), in.size() - 2);
        };
    }

    private static Component name(Holder<Item> item) {
        return item.value().getName(item.value().getDefaultInstance());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> out, TooltipFlag flag) {
        SandwichContents c = contents(stack);
        if (c.buttered()) out.accept(Component.translatable("tooltip.simplesandwiches.buttered").withStyle(ChatFormatting.GOLD));
        for (Holder<Item> ingredient : c.ingredients()) {
            out.accept(Component.translatable("tooltip.simplesandwiches.layer", name(ingredient)).withStyle(ChatFormatting.GRAY));
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            out.accept(Component.translatable("tooltip.simplesandwiches.food", food.nutrition(),
                    String.format(java.util.Locale.ROOT, "%.1f", food.saturation())).withStyle(ChatFormatting.DARK_GREEN));
        }
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return;
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect apply)) continue;
            for (MobEffectInstance inst : apply.effects()) {
                Component effectName = Component.translatable(inst.getDescriptionId());
                if (inst.getAmplifier() > 0) {
                    effectName = Component.translatable("potion.withAmplifier", effectName,
                            Component.translatable("potion.potency." + inst.getAmplifier()));
                }
                int seconds = Math.max(1, inst.getDuration() / 20);
                out.accept(Component.translatable("tooltip.simplesandwiches.effect", effectName,
                                String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60),
                                Math.round(apply.probability() * 100))
                        .withStyle(inst.getEffect().value().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
        }
    }
}
