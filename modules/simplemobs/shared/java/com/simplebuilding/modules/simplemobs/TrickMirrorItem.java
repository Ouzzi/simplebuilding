package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Trick mirror (Deceiver stage 2, balanced 12.10. so it only eases a fight): held up for 3 s it shows what lies in a
 * 20 degree cone up to 12 blocks: the nearest {@link #MAX_FOUND} fakes, disguised Deceivers or players glow for 2 s,
 * their disguises fall. 60 s cooldown, 64 uses.
 */
public class TrickMirrorItem extends Item {
    public static final int USE_TICKS = 60, COOLDOWN_TICKS = 1200, GLOW_TICKS = 40, MAX_FOUND = 3;
    public static final double RANGE = 12.0, CONE_COS = Math.cos(Math.toRadians(20.0));

    public TrickMirrorItem(Properties properties) { super(properties); }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return USE_TICKS; }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.SPYGLASS; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.3f);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level instanceof ServerLevel server && user instanceof Player player) {
            int found = reveal(server, player);
            player.sendOverlayMessage(Component.translatable("message.simplemobs.mirror.found", found));
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, player, player.getUsedItemHand());
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        return stack;
    }

    /** Unmasks the nearest {@link #MAX_FOUND} fakes, disguised Deceivers and disguised players in the viewer's cone; returns how many. */
    public static int reveal(ServerLevel level, Player viewer) {
        Vec3 eye = viewer.getEyePosition(), view = viewer.getViewVector(1.0f).normalize();
        int found = 0;
        var candidates = new java.util.ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(RANGE), e -> e != viewer));
        candidates.sort(java.util.Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(eye)));
        for (LivingEntity e : candidates) {
            if (found >= MAX_FOUND) break;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > RANGE || d < 1.0e-3 || view.dot(to.scale(1.0 / d)) < CONE_COS) continue;
            boolean hit = false;
            if (e instanceof DeceiverEntity deceiver && deceiver.look() != null) {
                deceiver.disguiseAs(null);
                deceiver.playAction(DeceiverEntity.ACT_UNMASK, 16);
                hit = true;
            } else if (e.entityTags().contains(DeceiverEntity.TAG_FAKE)) {
                hit = true;
            } else if (e instanceof Player p && Disguise.disguised(p)) {
                Disguise.revert(p);
                hit = true;
            }
            if (!hit) continue;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false, true));
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            found++;
        }
        return found;
    }
}
