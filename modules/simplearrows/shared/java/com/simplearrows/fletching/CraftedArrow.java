package com.simplearrows.fletching;

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
    /** WE25-9: the potion colour for the particles (-1 = no potion), synced like Vanilla's {@code Arrow}. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> POTION_COLOR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(CraftedArrow.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** Like Vanilla's tipped arrow: after this many ticks in the ground the potion has dried off. */
    private static final int EXPOSED_POTION_DECAY_TIME = 600;
    private double simplearrows$baseDamage = 2.0;

    public CraftedArrow(EntityType<? extends CraftedArrow> type, Level level) {
        super(type, level);
    }

    public CraftedArrow(Level level, LivingEntity owner, ItemStack pickup, ItemStack weapon) {
        super(Fletchings.CRAFTED_ARROW_ENTITY, owner, level, pickup, weapon);
        applyParts();
    }

    public CraftedArrow(Level level, double x, double y, double z, ItemStack pickup, ItemStack weapon) {
        super(Fletchings.CRAFTED_ARROW_ENTITY, x, y, z, level, pickup, weapon);
        applyParts();
    }

    private void applyParts() {
        updatePotionColor();
        int extra = parts().extraPierce();
        if (extra > 0) {
            ((com.simplearrows.mixin.ArrowPierceAccessor) this).simplearrows$setPierceLevel(
                    (byte) (this.getPierceLevel() + extra));
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POTION_COLOR, -1);
    }

    /** WE25-9: the potion of the Fletching Table's potion slot (empty = none). */
    public net.minecraft.world.item.alchemy.PotionContents potion() {
        return this.getPickupItemStackOrigin().getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY);
    }

    private void updatePotionColor() {
        net.minecraft.world.item.alchemy.PotionContents potion = potion();
        this.entityData.set(POTION_COLOR, potion.equals(net.minecraft.world.item.alchemy.PotionContents.EMPTY) ? -1 : potion.getColor());
    }

    @Override
    protected void setPickupItemStack(ItemStack stack) {
        super.setPickupItemStack(stack);
        updatePotionColor();
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
        this.simplearrows$baseDamage = damage;
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
        int color = this.entityData.get(POTION_COLOR);
        if (this.level().isClientSide()) {
            if (color != -1 && (!this.isInGround() || this.inGroundTime % 5 == 0)) {
                this.level().addParticle(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color),
                        this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0);
            }
        } else if (color != -1 && this.isInGround() && this.inGroundTime >= EXPOSED_POTION_DECAY_TIME) {
            // Vanilla tipped arrow: picked up after half a minute in the ground it is a plain arrow again (same parts, no potion).
            ItemStack dry = this.getPickupItemStackOrigin().copy();
            dry.remove(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            dry.remove(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE);
            this.setPickupItemStack(dry);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (parts().tip() == ArrowParts.Tip.AMETHYST) {
            // It shatters on this hit (afterHit): never hand it back - SimpleBuilding's arrow recovery only takes ALLOWED arrows.
            this.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
        double bonus = parts().bonusAgainst(result.getEntity());
        double base = this.simplearrows$baseDamage;
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
        // WE25-9: the potion works like on Vanilla's tipped arrow (duration scale from the stack, 1/8 from the table).
        float scale = this.getPickupItemStackOrigin().getOrDefault(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE, 1.0F);
        net.minecraft.world.entity.Entity source = this.getEffectSource();
        potion().forEachEffect(effect -> target.addEffect(effect, source), scale);
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

    /** Ob ein Stapel dieses Pfeils ist. */
    public static boolean isCraftedArrow(ItemStack stack) {
        return stack.is(Fletchings.CRAFTED_ARROW);
    }

    /** Tests: the hit effects (potion, blaze fire) on {@code target} without a flight. */
    public void applyHitEffects(LivingEntity target) {
        this.doPostHurtEffects(target);
    }

    /** Tests: a hit on an entity without a flight. */
    public void hitForTest(EntityHitResult hit) {
        this.onHitEntity(hit);
    }
}
