package com.simplebuilding.util;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A spear in a dispenser (owner 2026-10-08, queue N24 "Speer im Spender: bei Aktivierung wie Stachelfalle"): the
 * dispenser does not throw the spear out but thrusts it forward like a spike trap. Every activation hurts all living
 * entities in the {@link #REACH} blocks in front of the opening with the spear's attack damage
 * ({@link #thrustDamage}), pushes them back a little and costs the spear one durability point if it hit anything; a worn
 * out spear breaks like a tool. The spear stays in the dispenser. Sound: the spear's attack sound, crit sparks along
 * the thrust. All vanilla spears and the Enderite spear.
 */
public final class SpearDispensing {
    /** How far the thrust reaches, in blocks in front of the opening. */
    public static final int REACH = 2;
    /** Knockback of a thrust (horizontal speed added away from the dispenser). */
    public static final double KNOCKBACK = 0.4;

    private SpearDispensing() {
    }

    /** The spears a dispenser thrusts. */
    public static List<Item> spears() {
        return List.of(Items.WOODEN_SPEAR, Items.STONE_SPEAR, Items.COPPER_SPEAR, Items.IRON_SPEAR, Items.GOLDEN_SPEAR,
                Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR, com.simplebuilding.items.ModItems.ENDERITE_SPEAR);
    }

    /** Registers the thrust for every spear (each loader calls this once, after the items exist). */
    public static void register() {
        for (Item spear : spears()) {
            if (spear != null) {
                DispenserBlock.registerBehavior(spear, BEHAVIOR);
            }
        }
    }

    /** The spear's attack damage as a player would deal it (1 + its main-hand attack damage modifiers). */
    public static float thrustDamage(ItemStack spear) {
        ItemAttributeModifiers modifiers = spear.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        return (float) modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
    }

    /** The space the thrust reaches: {@link #REACH} blocks in front of the dispenser. */
    public static AABB reach(BlockPos dispenser, Direction facing) {
        BlockPos first = dispenser.relative(facing);
        BlockPos last = dispenser.relative(facing, REACH);
        return new AABB(Vec3.atLowerCornerOf(first), Vec3.atLowerCornerOf(first).add(1, 1, 1))
                .minmax(new AABB(Vec3.atLowerCornerOf(last), Vec3.atLowerCornerOf(last).add(1, 1, 1)));
    }

    /**
     * One thrust: hurts every living entity in reach and wears the spear by one point when it hit. Returns how many
     * entities were hit. The stack is changed in place (it may become empty when the spear breaks).
     */
    public static int thrust(ServerLevel level, BlockPos dispenser, Direction facing, ItemStack spear) {
        AABB box = reach(dispenser, facing);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator());
        float damage = thrustDamage(spear);
        int hit = 0;
        for (LivingEntity target : targets) {
            if (target.hurtServer(level, level.damageSources().generic(), damage)) {
                hit++;
                target.push(facing.getStepX() * KNOCKBACK, 0.1, facing.getStepZ() * KNOCKBACK);
            }
        }
        Vec3 tip = Vec3.atCenterOf(dispenser);
        for (int i = 1; i <= REACH * 3; i++) {
            Vec3 p = tip.add(facing.getStepX() * i / 3.0, facing.getStepY() * i / 3.0, facing.getStepZ() * i / 3.0);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (hit > 0) {
            spear.hurtAndBreak(1, level, null, item -> level.playSound(null, dispenser,
                    net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.8F, 1.0F));
        }
        return hit;
    }

    private static final DefaultDispenseItemBehavior BEHAVIOR = new DefaultDispenseItemBehavior() {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack dispensed) {
            Direction facing = source.state().getValue(DispenserBlock.FACING);
            thrust(source.level(), source.pos(), facing, dispensed);
            return dispensed;
        }

        @Override
        protected void playSound(BlockSource source) {
            source.level().playSound(null, source.pos(), net.minecraft.sounds.SoundEvents.SPEAR_ATTACK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        protected void playAnimation(BlockSource source, Direction direction) {
        }
    };
}
