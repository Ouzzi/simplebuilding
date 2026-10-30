package com.simpledinos.entity;

import com.simpledinos.DinoVariants;
import com.simpledinos.config.DinosConfig;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base of the dinosaurs (concept 3.5): a horse-family mount without breeding. It comes only from an egg ({@link #hatch}), is
 * tame from its first second and belongs to the player who sat longest on the egg. The hatchling grows from 25 % to 100 % of
 * its size over {@code growMinutes} (the baby age counts down as for every animal; the synchronized {@link #GROWTH} percent
 * gives the smooth size). Saddle, armor, inventory and riding are the Vanilla horse code; the AI is a handful of Vanilla goals.
 */
public abstract class Dino extends AbstractHorse {
    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.INT);
    public static final float MIN_SCALE = 0.25F;

    protected Dino(EntityType<? extends Dino> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 100);
        builder.define(COAT, 0);
    }

    /** Coat variant, an index into {@link DinoVariants#COATS} (the rearing biome, see there); every dinosaur kind has it. */
    public int coat() {
        return this.entityData.get(COAT);
    }

    public void setCoat(int coat) {
        this.entityData.set(COAT, DinoVariants.clamp(coat));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CoatVariant", coat());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCoat(input.getIntOr("CoatVariant", 0));
    }

    /** Spawned by command, spawner or egg: the coat of the biome where it stands (an egg sets the coat itself before it hatches). */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setCoat(DinoVariants.pick(level.getBiome(blockPosition()), this.random));
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    /** Size factor for a growth percentage (0 = just hatched, 100 = grown up). */
    public static float scaleFor(int percent) {
        return MIN_SCALE + (1.0F - MIN_SCALE) * Math.max(0, Math.min(100, percent)) / 100.0F;
    }

    /** Growth percentage for a baby age (negative ticks left to grow) over {@code total} ticks. */
    public static int growthFor(int age, int total) {
        if (age >= 0) return 100;
        return (int) Math.max(0L, Math.min(100L, 100L - (long) (-age) * 100L / Math.max(1, total)));
    }

    public int growth() {
        return this.entityData.get(GROWTH);
    }

    @Override
    public float getAgeScale() {
        return isBaby() ? scaleFor(growth()) : 1.0F;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (GROWTH.equals(accessor)) refreshDimensions();
    }

    /** Called by the egg: tame, owned, a hatchling that needs {@code growMinutes} to grow up. */
    public void hatch(Player owner) {
        setTamed(true);
        setOwner(owner);
        setPersistenceRequired();
        setAge(-DinosConfig.growTicks());
        this.entityData.set(GROWTH, 0);
        setHealth(getMaxHealth());
    }

    /** Server: copies the baby age into the synchronized growth percentage (smooth size on the client). */
    public void syncGrowth() {
        int growth = isBaby() ? growthFor(getAge(), DinosConfig.growTicks()) : 100;
        if (growth != growth()) this.entityData.set(GROWTH, growth);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (this.tickCount % 20 == 0)) syncGrowth();
    }

    // ------------------------------------------------------------------------------------------------ behaviour

    @Override
    protected boolean canPerformRearing() {
        return false;
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return Dino.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return !Dino.this.isBaby() && !Dino.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new FollowOwnerLiteGoal(this, 1.1, 10.0, 3.0));
        // Defends the owner: only monsters that have the owner or the dinosaur as their target, scanned every 10 ticks.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false,
                (target, level) -> target instanceof Monster m && (m.getTarget() == getOwner() || m.getTarget() == this)) {
            @Override
            public boolean canUse() {
                return Dino.this.isTamed() && !Dino.this.isBaby() && !Dino.this.isVehicle() && super.canUse();
            }
        });
    }

    /** Walks toward the owner when farther than {@code start} blocks until within {@code stop} blocks (Vanilla goals need a TamableAnimal). */
    private static final class FollowOwnerLiteGoal extends Goal {
        private final Dino dino;
        private final double speed, start, stop;
        private int recalc;

        FollowOwnerLiteGoal(Dino dino, double speed, double start, double stop) {
            this.dino = dino;
            this.speed = speed;
            this.start = start;
            this.stop = stop;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = dino.getOwner();
            return dino.isTamed() && !dino.isVehicle() && owner != null && !owner.isSpectator() && dino.distanceToSqr(owner) > start * start;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = dino.getOwner();
            return !dino.isVehicle() && owner != null && dino.distanceToSqr(owner) > stop * stop;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            dino.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = dino.getOwner();
            if (owner == null) return;
            dino.getLookControl().setLookAt(owner, 10.0F, dino.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = adjustedTickDelay(10);
                dino.getNavigation().moveTo(owner, speed);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ interaction

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.MEAT);
    }

    @Override
    public boolean canMate(Animal partner) {
        return false; // dinosaurs come from eggs only
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBaby() && !stack.isEmpty() && isFood(stack)) {
            if (getHealth() < getMaxHealth()) {
                if (!level().isClientSide()) {
                    heal(8.0F);
                    stack.consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (!isBaby() && !isTamed() && !isVehicle()) {
            makeMad(); // a dinosaur without owner (spawned by command) cannot be ridden
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.BODY) return isAlive() && !isBaby();
        return super.canUseSlot(slot);
    }

    // ------------------------------------------------------------------------------------------------ sounds (Vanilla until the owner picks the recordings)

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) playSound(SoundEvents.RAVAGER_STEP, 0.5F, 0.7F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.3F : 0.65F);
    }
}
