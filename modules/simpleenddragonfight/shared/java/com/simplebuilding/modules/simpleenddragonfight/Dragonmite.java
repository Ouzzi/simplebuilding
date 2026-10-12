package com.simplebuilding.modules.simpleenddragonfight;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Dragonmite: a tougher Endermite kin that only the dragon fight calls (concept section 3). Not an Endermite subclass,
 * so Endermen ignore it. A mite bound to a dragon dissolves in smoke (no drop, no XP) as soon as that dragon is gone,
 * dying, or the module is switched off; an unbound one (spawn egg) behaves like a normal monster.
 */
public class Dragonmite extends Monster {
    public static final double MAX_HEALTH = 14.0, DAMAGE = 3.0, SPEED = 0.32, ARMOR = 2.0, FOLLOW = 32.0, KNOCKBACK_RESISTANCE = 0.3;
    /** Chance per hit to slow the target ("sticky chitin"), Slowness I for 2 s. */
    public static final float SLOW_CHANCE = 0.10f;
    public static final int SLOW_TICKS = 40;

    private @Nullable UUID boundDragon;

    public Dragonmite(EntityType<? extends Dragonmite> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.FOLLOW_RANGE, FOLLOW)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void bindTo(@Nullable UUID dragon) { this.boundDragon = dragon; }

    public @Nullable UUID boundDragon() { return boundDragon; }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && this.random.nextFloat() < SLOW_CHANCE) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0), this);
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) { return false; }

    @Override
    protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.EVENTS; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENDERMITE_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMITE_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENDERMITE_DEATH; }

    @Override
    public float getVoicePitch() { return super.getVoicePitch() * 0.8F; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) { this.playSound(SoundEvents.ENDERMITE_STEP, 0.15F, 0.8F); }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide()) this.playSound(SoundEvents.ENDER_DRAGON_HURT, 0.25F, 1.6F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.boundDragon = input.read("BoundDragon", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (boundDragon != null) output.store("BoundDragon", UUIDUtil.CODEC, boundDragon);
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
        if (this.level() instanceof ServerLevel level && this.tickCount % 20 == 0) checkBound(level);
    }

    /** Dissolves this mite if it is bound to a dragon that is gone (the once-a-second check of {@link #tick()}). */
    public void checkBound(ServerLevel level) {
        if (boundDragon != null && !dragonAlive(level, boundDragon)) dissolve(level);
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        this.setYRot(yBodyRot);
        super.setYBodyRot(yBodyRot);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5),
                    (this.random.nextDouble() - 0.5) * 0.5, -this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.5);
        }
    }

    /** Whether the bound dragon still fights: loaded, alive, not dying, and the module on. */
    static boolean dragonAlive(ServerLevel level, UUID dragon) {
        if (!DragonFightRegistry.on() || !DragonFightConfig.mitesEnabled) return false;
        Entity e = level.getEntity(dragon);
        return e instanceof EnderDragon d && d.isAlive() && d.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.DYING;
    }

    /** Vanishes in smoke: no drop, no experience. */
    public void dissolve(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.25, 0.15, 0.25, 0.01);
        this.discard();
    }
}
