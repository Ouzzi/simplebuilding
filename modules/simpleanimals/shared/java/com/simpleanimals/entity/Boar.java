package com.simpleanimals.entity;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.config.AnimalsConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Wild boar (concept WP-18, owner choice ABN29b-3): neutral, like the polar bear.
 * <ul>
 * <li>An adult attacks a player who comes too close to one of its piglets ({@link #PIGLET_RANGE} blocks, a piglet within
 * {@link #HERD_RANGE} blocks of the adult), in sight, as the polar bear does with its cub.</li>
 * <li>Without piglets it only reacts to provocation: it is hurt, it is shoved (a player pushing against it), or a player
 * stays within {@link #CLOSE_RANGE} blocks for longer than the configured time (sneaking players do not count).</li>
 * <li>It rams: a fast charge, a short knockback, 4/6/8 damage by difficulty, with the Vanilla attack cooldown.</li>
 * </ul>
 * Piglets flee when hurt and alert the adults. Drops porkchop. Carrots, potatoes, beetroots (the Vanilla pig food) breed it.
 */
public class Boar extends Animal {
    /** A player this close to a piglet provokes its adults. */
    public static final double PIGLET_RANGE = 5.0;
    /** Adults this close to the piglet join in. */
    public static final double HERD_RANGE = 16.0;
    public static final double CLOSE_RANGE = 3.0;
    /** Ticks a shove keeps the boar provoked. */
    public static final int SHOVE_TICKS = 100;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Boar.class, EntityDataSerializers.INT);

    /** Server: ticks a player has stood within {@link #CLOSE_RANGE}. */
    private int closeTicks;
    private int shoveTicks;

    public Boar(EntityType<? extends Boar> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ATTACK_KNOCKBACK, 0.8).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6, boar -> boar.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> AnimalsConfig.boarAggression && provokedBy(target)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(VARIANT, 0);
    }

    /** 0 = dark coat, 1 = light coat. */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v == 1 ? 1 : 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setVariant(this.random.nextInt(3) == 0 ? 1 : 0);
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.PIG_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Boar baby = AnimalsRegistry.BOAR.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) baby.setVariant(this.random.nextBoolean() || !(partner instanceof Boar other) ? variant() : other.variant());
        return baby;
    }

    /** True when this adult boar has a reason to attack {@code target} now (see class comment). */
    public boolean provokedBy(LivingEntity target) {
        if (isBaby()) return false;
        if (shoveTicks > 0 || closeTicks >= AnimalsConfig.boarCloseSeconds * 20) return true;
        return pigletNear(target);
    }

    /** A piglet of the herd within {@link #PIGLET_RANGE} blocks of the target, in sight, and this adult within {@link #HERD_RANGE} of it. */
    public boolean pigletNear(LivingEntity target) {
        List<Boar> piglets = level().getEntitiesOfClass(Boar.class, target.getBoundingBox().inflate(PIGLET_RANGE), Boar::isBaby);
        for (Boar piglet : piglets) {
            if (distanceToSqr(piglet) <= HERD_RANGE * HERD_RANGE && piglet.hasLineOfSight(target)) return true;
        }
        return false;
    }

    public int closeTicks() {
        return closeTicks;
    }

    /** The shove of a player pushing against the boar: it is provoked for {@link #SHOVE_TICKS} ticks. */
    public void shove() {
        shoveTicks = SHOVE_TICKS;
    }

    /** Adds time to the "too long too near" counter (the aiStep does this every tick a player stands within {@link #CLOSE_RANGE}). */
    public void addCloseTicks(int ticks) {
        closeTicks += ticks;
    }

    public int shoveTicks() {
        return shoveTicks;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && isAlive() && !isBaby()) {
            Player near = level().getNearestPlayer(this, CLOSE_RANGE);
            if (near != null && !near.isShiftKeyDown() && !near.isSpectator() && !near.isCreative()) closeTicks++;
            else closeTicks = Math.max(0, closeTicks - 2);
            if (shoveTicks > 0) shoveTicks--;
        }
    }

    /** A player pushing against an adult boar provokes it (the shove). */
    @Override
    public void push(Entity entity) {
        if (entity instanceof Player && !isBaby() && !level().isClientSide()) shove();
        super.push(entity);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isBaby() && source.getEntity() instanceof LivingEntity attacker) alertAdults(level, attacker);
        return hurt;
    }

    /** A hurt piglet calls every adult of the herd on its attacker. */
    private void alertAdults(ServerLevel level, LivingEntity attacker) {
        for (Boar adult : level.getEntitiesOfClass(Boar.class, getBoundingBox().inflate(HERD_RANGE), b -> !b.isBaby())) {
            if (adult.getTarget() == null && adult.canAttack(attacker)) adult.setTarget(attacker);
        }
    }

    /** The ram hits for 4 / 6 / 8 by difficulty (the attribute holds the normal value). */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            Difficulty difficulty = level().getDifficulty();
            attack.setBaseValue(difficulty == Difficulty.HARD ? 8.0 : difficulty == Difficulty.EASY ? 4.0 : 6.0);
        }
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (AnimalsSpawn.natural(reason)) {
            return AnimalsSpawn.allowed(AnimalsConfig.boar, this.random) && AnimalsSpawn.onFarmGround(level, blockPosition());
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.HOGLIN_STEP, 0.15F, 1.0F);
    }
}
