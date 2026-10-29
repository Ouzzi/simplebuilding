package com.simpleanimals.entity;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.config.AnimalsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Zebra (concept WP-18, owner choice ABN29b-3): a horse of the savanna with its own looks (stripes, stiff mane) and its own
 * texture on the Vanilla horse model. Everything else is the Vanilla horse: tamed by riding, saddle, horse armor, same
 * stats, same food, breeds with its own kind.
 *
 * <p>Zebra x horse (R16) gives the {@link Zorse}, infertile like the mule: the zebra is the parent that looks for a horse
 * partner ({@link #canMate}); the Vanilla horse is not changed. The config key {@code zorse} switches the cross off.
 */
public class Zebra extends AbstractHorse {
    public Zebra(EntityType<? extends Zebra> type, Level level) {
        super(type, level);
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(random::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(random::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean openInventory = !isBaby() && isTamed() && player.isSecondaryUseActive();
        if (!isVehicle() && !openInventory) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                if (isFood(stack)) return fedFood(player, stack);
                if (!isTamed()) {
                    makeMad();
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    /** A zebra mates with a zebra, and with a Vanilla horse when the zorse is on. */
    @Override
    public boolean canMate(Animal partner) {
        if (partner == this) return false;
        boolean kind = partner instanceof Zebra || (AnimalsConfig.zorse && partner instanceof Horse);
        return kind && readyToBreed(this) && readyToBreed((AbstractHorse) partner);
    }

    /** The conditions of Vanilla's protected {@code canParent}, from public methods. */
    static boolean readyToBreed(AbstractHorse horse) {
        return !horse.isVehicle() && !horse.isPassenger() && horse.isTamed() && !horse.isBaby()
                && horse.getHealth() >= horse.getMaxHealth() && horse.isInLove();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        EntityType<? extends AbstractHorse> babyType = partner instanceof Horse ? AnimalsRegistry.ZORSE : AnimalsRegistry.ZEBRA;
        AbstractHorse baby = babyType.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) this.setOffspringAttributes(partner, baby);
        return baby;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    @Override
    protected void hurtArmor(DamageSource source, float damage) {
        this.doHurtEquipment(source, damage, EquipmentSlot.BODY);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (AnimalsSpawn.natural(reason)) {
            return AnimalsSpawn.allowed(AnimalsConfig.zebra, this.random) && AnimalsSpawn.onFarmGround(level, blockPosition());
        }
        return super.checkSpawnRules(level, reason);
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
