package com.simplebuilding.modules.simpleend;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Spoiled Drift Fruit: what End trees grown outside the End bear (fruit only ripens in the End, owner 2026-10-13).
 * Eating it gives {@link #DRIFT_TICKS} of Drifting (a drunk stagger, {@link DriftingEffect}) and a longer Slow Falling, so the
 * drift never ends in a deadly fall.
 */
public class SpoiledDriftFruitItem extends Item {
    public static final int DRIFT_TICKS = 100;
    public static final int SLOW_FALL_TICKS = 160;

    public SpoiledDriftFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && SimpleEnd.on() && EndRegistry.drifting() != null) {
            entity.addEffect(new MobEffectInstance(EndRegistry.drifting(), DRIFT_TICKS, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0));
        }
        return result;
    }
}
