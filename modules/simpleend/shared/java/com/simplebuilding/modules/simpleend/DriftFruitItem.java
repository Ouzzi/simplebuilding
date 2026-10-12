package com.simplebuilding.modules.simpleend;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Schwebfrucht (Drift Fruit): eating it gives 4 s Slow Falling (consume effect) and a small upward hop - a short glide. */
public class DriftFruitItem extends Item {
    public static final int SLOW_FALL_TICKS = 80;
    public static final double HOP = 0.55;

    public DriftFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && SimpleEnd.on()) hop(entity);
        return result;
    }

    public static void hop(LivingEntity entity) {
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x, Math.max(v.y, 0) + HOP, v.z);
        entity.needsSync = true;
        entity.resetFallDistance();
    }
}
