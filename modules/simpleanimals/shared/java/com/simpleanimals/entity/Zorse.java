package com.simpleanimals.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Zorse (zebra x horse, concept WP-18 R16): infertile like the Vanilla mule (it never mates and has no offspring), a
 * rideable horse with a brown coat and zebra stripes on legs and rump. Only the cross produces it; there is no spawn egg and
 * no natural spawn. Same stats as the parents (mixed at birth), no chest.
 */
public class Zorse extends AbstractHorse {
    public Zorse(EntityType<? extends Zorse> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    @Override
    public void hurtArmor(DamageSource source, float damage) {
        this.doHurtEquipment(source, damage, EquipmentSlot.BODY);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isBaby() ? SoundEvents.HORSE_AMBIENT_BABY : SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return isBaby() ? SoundEvents.HORSE_DEATH_BABY : SoundEvents.HORSE_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getEatingSound() {
        return isBaby() ? SoundEvents.HORSE_EAT_BABY : SoundEvents.HORSE_EAT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return isBaby() ? SoundEvents.HORSE_HURT_BABY : SoundEvents.HORSE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return isBaby() ? SoundEvents.HORSE_ANGRY_BABY : SoundEvents.HORSE_ANGRY;
    }
}
