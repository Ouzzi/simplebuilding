package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.PartRig;
import com.simpledinos.entity.part.TrexBones;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Tyrannosaurus rex (concept 3.4): the combat mount. Drawn at three quarters of life size ({@link #MODEL_SCALE}, TREX32 T1) with a
 * core hitbox of 3.6 x 3.5 blocks; head and tail reach beyond it and have part boxes ({@link #RIG}, a-dino36b) that turn away from walls
 * and living things. Bite 10 damage (config), bites only monsters that attack its owner or itself; carries one rider with a large saddle.
 */
public class Trex extends Dino {
    /** TREX32 T1 (owner 01.11.): the model is drawn at 75 % so the turning head stays near the 3.6 x 3.5 hitbox. */
    public static final float MODEL_SCALE = 0.75F;
    /**
     * Part boxes (a-dino36b): neck and head share the look yaw (0.4 each), the head takes half the pitch; pulled in, the neck leans back
     * 1.2 rad and the head turns 1.2 rad down again, so it lies level over the chest (front 1.63 < 1.8, top 4.03 blocks); each tail
     * segment bends 0.3 rad per step.
     */
    public static final PartRig RIG = new PartRig(TrexBones.SKELETON, MODEL_SCALE, "neck", "head", new String[] {"neck", "head", "jaw"},
            new String[] {"tail1", "tail2", "tail3"}, 0.4F, 0.4F, 0.5F, -1.2F, 1.2F, 0.3F);

    public Trex(EntityType<? extends Trex> type, Level level) {
        super(type, level);
        applyBite();
    }

    @Override
    public PartRig rig() {
        return RIG;
    }

    @Override
    public DinoKind kind() {
        return DinoKind.TREX;
    }

    /**
     * The T. rex is the only fighter (concept DINO36, a-dinorest1 moved these goals here from Dino): it bites monsters that hunt its
     * owner or itself, from the reach of its head, never players.
     */
    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return !Trex.this.isBaby() && !Trex.this.isVehicle() && super.canUse();
            }

            @Override
            public void tick() {
                super.tick();
                // a-dino36b: in bite reach it stands and bites from its head instead of pushing its head into the target
                LivingEntity target = Trex.this.getTarget();
                if (target != null && Trex.this.isWithinMeleeAttackRange(target)) Trex.this.getNavigation().stop();
            }
        });
        // Defends the owner: only monsters that have the owner or the dinosaur as their target, scanned every 10 ticks.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false,
                (target, level) -> target instanceof Monster m && defends(m)) {
            @Override
            public boolean canUse() {
                return Trex.this.isTamed() && !Trex.this.isBaby() && !Trex.this.isVehicle() && super.canUse();
            }
        });
    }

    /**
     * Whether {@code monster} is a reason to bite: it hunts the owner or this dinosaur. An idle monster is none - also while the
     * owner is offline or in another dimension, where {@link #getOwner()} is null and a plain {@code getTarget() == getOwner()}
     * would be true for every monster without a target.
     */
    public boolean defends(Monster monster) {
        LivingEntity hunted = monster.getTarget();
        return hunted != null && (hunted == getOwner() || hunted == this);
    }

    /**
     * The bite follows the server config (server authority): the attribute is saved with the entity, so without this a T. rex would
     * keep the value of its last save for ever and {@code trexBiteDamage} would only reach dinosaurs hatched afterwards.
     */
    public void applyBite() {
        var bite = getAttribute(Attributes.ATTACK_DAMAGE);
        if (bite != null && bite.getBaseValue() != DinosConfig.trexBiteDamage) bite.setBaseValue(DinosConfig.trexBiteDamage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        applyBite();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && this.tickCount % 20 == 0) applyBite();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.JUMP_STRENGTH, 0.45)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.ATTACK_DAMAGE, DinosConfig.BITE_DEFAULT)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.4);
    }

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond, 4..9 the metal tiers of DinosRegistry.ARMOR_TIER_NAMES (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_DIAMOND)) return 3;
        return DinosRegistry.metalArmorTier(bodyArmor, "trex");   // N89A: the metal tiers (codes 4..9)
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }
}
