package com.simpleanimals.entity;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.config.AnimalsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Deer (concept WP-18, owner choice ABN29b-3): shy herd animal of forests and taiga. Flees from players that run, stand
 * close or come with empty hands (sneaking players and players with food are tolerated), drops leather and beef like a
 * cow, breeds with wheat or carrots. Bucks (about half of the adults) carry antlers; two coat variants.
 * Vanilla goals only (A12: gentle, no block damage).
 */
public class Deer extends Animal {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Deer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANTLERS = SynchedEntityData.defineId(Deer.class, EntityDataSerializers.BOOLEAN);

    public Deer(EntityType<? extends Deer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        // Shy of players unless they sneak or hold food (so a deer can be tempted and bred).
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class,
                p -> !p.isShiftKeyDown() && !isFood(p.getMainHandItem()) && !isFood(p.getOffhandItem()),
                8.0F, 1.0, 1.5, EntitySelector.NO_CREATIVE_OR_SPECTATOR));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(VARIANT, 0);
        data.define(ANTLERS, false);
    }

    /** 0 = red deer, 1 = fallow deer. */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, AnimalVariants.clamp(AnimalVariants.DEER, v));
    }

    public boolean hasAntlers() {
        return this.entityData.get(ANTLERS) && !isBaby();
    }

    public void setAntlers(boolean antlers) {
        this.entityData.set(ANTLERS, antlers);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        output.putBoolean("Antlers", this.entityData.get(ANTLERS));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        setAntlers(input.getBooleanOr("Antlers", false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setVariant(AnimalVariants.pick(AnimalVariants.DEER, level.getBiome(blockPosition()), this.random));
        setAntlers(this.random.nextBoolean());
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CARROT);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Deer baby = AnimalsRegistry.DEER.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.random.nextBoolean() || !(partner instanceof Deer other) ? variant() : other.variant());
            baby.setAntlers(this.random.nextBoolean());
        }
        return baby;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (AnimalsSpawn.natural(reason)) {
            return AnimalsSpawn.allowed(AnimalsConfig.deer, this.random) && AnimalsSpawn.onFarmGround(level, blockPosition());
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.LLAMA_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.LLAMA_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.LLAMA_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.LLAMA_STEP, 0.15F, 1.1F);
    }

}
