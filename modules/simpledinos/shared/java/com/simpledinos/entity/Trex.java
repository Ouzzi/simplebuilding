package com.simpledinos.entity;

import com.simpledinos.DinosRegistry;
import com.simpledinos.config.DinosConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tyrannosaurus rex (concept 3.4): the combat mount. Life size (12 m long, about 3.7 m at the hips) with a core hitbox of
 * 3.0 x 4.0 blocks (the tail and the head reach beyond it, R4) and a large culling box on the client. Bite 10 damage (config),
 * bites only monsters that attack its owner or itself; carries one rider with a large saddle.
 */
public class Trex extends Dino {
    public Trex(EntityType<? extends Trex> type, Level level) {
        super(type, level);
        var bite = getAttribute(Attributes.ATTACK_DAMAGE);
        if (bite != null) bite.setBaseValue(DinosConfig.trexBiteDamage);
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

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.TREX_ARMOR_DIAMOND)) return 3;
        return 0;
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }
}
