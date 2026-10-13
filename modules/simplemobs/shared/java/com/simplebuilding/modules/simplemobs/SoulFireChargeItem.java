package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Soulfire Charge: Vanilla's fire charge (lights campfires and candles, places fire, shot by dispensers) whose
 * dispensed projectile is a {@link SoulFireball} - the Soulfire Blaze's own ball.
 */
public class SoulFireChargeItem extends FireChargeItem {
    public SoulFireChargeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        var random = level.getRandom();
        double spread = 0.11485000000000001;
        Vec3 aim = new Vec3(random.triangle(direction.getStepX(), spread), random.triangle(direction.getStepY(), spread),
                random.triangle(direction.getStepZ(), spread));
        SoulFireball ball = new SoulFireball(level, pos.x(), pos.y(), pos.z(), aim.normalize());
        ball.setItem(stack.copyWithCount(1));
        return ball;
    }
}
