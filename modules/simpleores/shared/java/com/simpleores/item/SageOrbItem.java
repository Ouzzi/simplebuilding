package com.simpleores.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Sage Orb (owner 2026-10-01, moved from SimpleBuilding in IG31-13): rare drop of Sage Ore. A right click charges
 * {@link #CHARGE_TICKS} ticks (0.3 s) with rising sparks, then gives {@link #MIN_XP}-{@link #MAX_XP} experience points and
 * is used up; the server rolls the amount. Feedback by sound and particles only. The hand motion like the building cores
 * comes from a partner ({@code CoreHooks.startHeldMotion(..., "orb")}, SimpleBuilding).
 */
public class SageOrbItem extends Item {
    /** 0.3 s animation (owner 2026-10-02). */
    public static final int CHARGE_TICKS = 6;
    public static final int MIN_XP = 50;
    public static final int MAX_XP = 100;

    public SageOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (level.isClientSide()) {
            com.simplelib.api.CoreHooks.startHeldMotion(player, hand, "orb");
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CHARGE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (level instanceof ServerLevel server && remainingUseTicks % 2 == 0) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getY() + 1.0, user.getZ(), 2, 0.35, 0.4, 0.35, 0.02);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player) || !(level instanceof ServerLevel server)) {
            return stack;
        }
        int xp = MIN_XP + server.getRandom().nextInt(MAX_XP - MIN_XP + 1);
        player.giveExperiencePoints(xp);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.4, 0.5, 0.4, 0.15);
        stack.consume(1, player);
        return stack;
    }
}
