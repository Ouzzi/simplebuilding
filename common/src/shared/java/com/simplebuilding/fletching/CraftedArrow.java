package com.simplebuilding.fletching;

import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pfeil vom Befiederungstisch (Besitzer 2026-10-01). Die Teile liest er aus seinem Aufhebe-Stapel
 * ({@link ArrowParts#of}); Bogen-Verzauberungen (Staerke, Flamme, Schlag, Durchschlag, Mehrfachschuss) wirken wie
 * bei Vanilla, die Teile wirken zusaetzlich. Unendlichkeit gilt nur fuer {@code minecraft:arrow} (Vanilla-Regel).
 */
public class CraftedArrow extends AbstractArrow {
    private double simplebuilding$baseDamage = 2.0;

    public CraftedArrow(EntityType<? extends CraftedArrow> type, Level level) {
        super(type, level);
    }

    public CraftedArrow(Level level, LivingEntity owner, ItemStack pickup, ItemStack weapon) {
        super(ModEntities.CRAFTED_ARROW, owner, level, pickup, weapon);
        applyParts();
    }

    public CraftedArrow(Level level, double x, double y, double z, ItemStack pickup, ItemStack weapon) {
        super(ModEntities.CRAFTED_ARROW, x, y, z, level, pickup, weapon);
        applyParts();
    }

    private void applyParts() {
        int extra = parts().extraPierce();
        if (extra > 0) {
            ((com.simplebuilding.mixin.AbstractArrowAccessor) this).simplebuilding$setPierceLevel(
                    (byte) (this.getPierceLevel() + extra));
        }
    }

    public ArrowParts.Parts parts() {
        return ArrowParts.of(this.getPickupItemStackOrigin());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ArrowParts.stack(ArrowParts.Parts.VANILLA, 1);
    }

    @Override
    public void setBaseDamage(double damage) {
        this.simplebuilding$baseDamage = damage;
        super.setBaseDamage(damage);
    }

    @Override
    protected double getDefaultGravity() {
        ArrowParts.Parts parts = parts();
        if (parts.tip() == ArrowParts.Tip.ENDERITE && this.tickCount < ArrowParts.ENDERITE_WEIGHTLESS_TICKS) {
            return 0.0;
        }
        return super.getDefaultGravity() * parts.gravityFactor();
    }

    @Override
    public float getWaterInertia() {
        return parts().tip() == ArrowParts.Tip.PRISMARINE ? ArrowParts.PRISMARINE_WATER_INERTIA : super.getWaterInertia();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && !this.isInGround() && parts().shaft() == ArrowParts.Shaft.END_ROD) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        double bonus = parts().bonusAgainst(result.getEntity());
        double base = this.simplebuilding$baseDamage;
        super.setBaseDamage(base + bonus);
        super.onHitEntity(result);
        super.setBaseDamage(base);
        afterHit(result.getLocation(), result.getEntity());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        afterHit(result.getLocation(), null);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (parts().shaft() == ArrowParts.Shaft.BLAZE_ROD) {
            target.setRemainingFireTicks(target.getRemainingFireTicks() + ArrowParts.BLAZE_EXTRA_FIRE_TICKS);
        }
    }

    private void afterHit(Vec3 at, net.minecraft.world.entity.Entity directTarget) {
        if (!(this.level() instanceof ServerLevel server) || this.isRemoved()) {
            return;
        }
        ArrowParts.Parts parts = parts();
        if (parts.shaft() == ArrowParts.Shaft.BREEZE_ROD) {
            server.explode(this, null, AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR, at.x, at.y, at.z, 1.2F, false,
                    Level.ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                    WeightedList.of(), SoundEvents.WIND_CHARGE_BURST);
        }
        if (parts.tip() == ArrowParts.Tip.AMETHYST) {
            var source = server.damageSources().arrow(this, this.getOwner());
            for (LivingEntity near : server.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(at, at).inflate(ArrowParts.AMETHYST_SPLASH_RADIUS))) {
                if (near != directTarget && near != this.getOwner() && near.position().distanceTo(at) <= ArrowParts.AMETHYST_SPLASH_RADIUS + near.getBbWidth()) {
                    near.hurtServer(server, source, ArrowParts.AMETHYST_SPLASH_DAMAGE);
                }
            }
            server.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.AMETHYST_SHARD), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.08);
            this.discard();
        }
    }

    /** Nur fuer {@link ModItems}: ob ein Stapel dieses Pfeils ist. */
    public static boolean isCraftedArrow(ItemStack stack) {
        return stack.is(ModItems.CRAFTED_ARROW);
    }
}
