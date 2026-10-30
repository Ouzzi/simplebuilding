package com.simpleores.entity;

import com.simpleores.OresRegistry;
import com.simpleores.config.OresConfig;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The silver arrow (concept ORES31 section 4): a Vanilla arrow with a silver head that hits {@code #minecraft:undead} harder by
 * {@code silverArrowUndeadBonus} (config, 0 = Vanilla damage). Built like the Vanilla spectral arrow, which also extends
 * {@link AbstractArrow}: Vanilla's {@code Arrow} is tied to its own entity type.
 */
public class SilverArrow extends AbstractArrow {
    /** Vanilla's default base damage, tracked here because the field of the parent is private. */
    private double base = 2.0;

    public SilverArrow(EntityType<? extends SilverArrow> type, Level level) {
        super(type, level);
    }

    public SilverArrow(Level level, LivingEntity owner, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(OresRegistry.SILVER_ARROW_ENTITY, owner, level, pickupItemStack, firedFromWeapon);
    }

    public SilverArrow(Level level, double x, double y, double z, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(OresRegistry.SILVER_ARROW_ENTITY, x, y, z, level, pickupItemStack, firedFromWeapon);
    }

    /** Extra damage this arrow does to the target now (0 for a living non-undead target and when the bonus is switched off). */
    public static int bonusAgainst(Entity target) {
        return OresConfig.silverArrowUndeadBonus > 0 && target.is(EntityTypeTags.UNDEAD) ? OresConfig.silverArrowUndeadBonus : 0;
    }

    @Override
    public void setBaseDamage(double baseDamage) {
        this.base = baseDamage;
        super.setBaseDamage(baseDamage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.base = input.getDoubleOr("damage", 2.0);
    }

    /** Public so the GameTest can hit a target without flying: damage = ceil(speed * baseDamage), the bonus is added to it. */
    @Override
    public void onHitEntity(EntityHitResult hitResult) {
        int bonus = bonusAgainst(hitResult.getEntity());
        double speed = this.getDeltaMovement().length();
        if (bonus <= 0 || speed < 1.0E-3) {
            super.onHitEntity(hitResult);
            return;
        }
        super.setBaseDamage(this.base + bonus / speed);
        try {
            super.onHitEntity(hitResult);
        } finally {
            super.setBaseDamage(this.base);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(OresRegistry.item("silver_arrow"));
    }
}
