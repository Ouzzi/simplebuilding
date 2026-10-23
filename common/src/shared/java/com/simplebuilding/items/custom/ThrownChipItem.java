package com.simplebuilding.items.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Throwable shards (B23-M4, owner 23.10.): stone pebble, flint/fire/ice chips and the obsidian chips fly like a snowball on
 * a right click into the air - held, one every four ticks like snowballs (no cooldown) - and hit for {@value #DAMAGE}
 * (half a heart). Laying them down (sneak + right click on a block, {@code PlacedSmallParts}) comes first and stays.
 * Nuggets and lumps are not thrown.
 */
public class ThrownChipItem extends Item {
    /** Damage of one hit (half a heart), for every target. */
    public static final float DAMAGE = 1.0F;

    public ThrownChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation(Chip::new, server, stack, player, 0.0F, 1.5F, 1.0F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * The flying chip: a snowball-type entity (Vanilla's {@code minecraft:snowball}, so the client draws it with the chip's
     * item and its hit particles without an own entity type or renderer), but it hurts for {@link #DAMAGE}. Saved and
     * loaded again it becomes a plain snowball carrying the chip, which only matters for a chip in flight.
     */
    public static class Chip extends ThrowableItemProjectile {
        public Chip(ServerLevel level, LivingEntity owner, ItemStack stack) {
            super(EntityTypes.SNOWBALL, owner, level, stack);
        }

        @Override
        protected Item getDefaultItem() {
            return Items.SNOWBALL;
        }

        @Override
        protected void onHitEntity(EntityHitResult hit) {
            super.onHitEntity(hit);
            hit.getEntity().hurt(damageSources().thrown(this, getOwner()), DAMAGE);
        }

        @Override
        protected void onHit(HitResult hit) {
            super.onHit(hit);
            if (!level().isClientSide()) {
                level().broadcastEntityEvent(this, (byte) 3);
                discard();
            }
        }
    }
}
