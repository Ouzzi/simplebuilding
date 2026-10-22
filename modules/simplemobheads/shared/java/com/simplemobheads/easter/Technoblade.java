package com.simplemobheads.easter;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.ability.HeadAbilities;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Tribute to Technoblade (EH21 N-A, TO-1, F-A; docs/ai/PLAN-B-EH21-2026-10-22.md). Only his symbol, never his face or a player
 * profile: an adult pig named exactly "Technoblade" (like Vanilla's "Dinnerbone") wears a golden crown - the Crowned Pig Head in
 * its head slot, so Vanilla saves it and shows it to every client (the crown layer of the pig renderer draws it). While it wears
 * the crown it never dies: a deadly hit leaves half a heart and the totem's flash. When a player struck that hit and is not
 * waiting, the crown falls off as the head; it grows back after {@code technobladeCooldownTicks}, and that player waits as long.
 * Without a player the crown stays - the pig never dies. {@code /kill} and the void ({@code bypasses_invulnerability}) still
 * work. Lightning never turns Technoblade into a zombified piglin.
 */
public final class Technoblade {
    public static final String NAME = "Technoblade";
    public static final String HEAD = "crowned_pig";
    /** Entity tag (pig): game time when the crown grows back. */
    public static final String CROWN_READY = "simplemobheads.crown_ready";
    /** Entity tag (player): game time when that player may get the next crowned head. */
    public static final String TRIBUTE_READY = "simplemobheads.tribute_ready";
    /** F-A: pigs within this range turn their heads to the wearer. */
    public static final double LOOK_RANGE = 8.0;
    /** F-A: the crown glints every two seconds. */
    public static final int GLINT_TICKS = 40;

    public enum Saved { NOT_CROWNED, KEPT_CROWN, CROWN_FELL }

    private Technoblade() {
    }

    private static @Nullable Item crown() {
        return MobHeads.item(HEAD);
    }

    public static boolean named(LivingEntity entity) {
        Component name = entity.getCustomName();
        return name != null && NAME.equals(name.getString());
    }

    /** Whether the pig wears the crown (the crowned pig head in its head slot). */
    public static boolean crowned(LivingEntity entity) {
        Item crown = crown();
        return entity instanceof Pig && crown != null && entity.getItemBySlot(EquipmentSlot.HEAD).is(crown);
    }

    private static boolean enabled() {
        return SimpleMobHeads.on() && SimpleMobHeads.config().technoblade;
    }

    /** Every second of a pig ({@code AnimalAiMixin}): the crown comes when it should, and goes with the name or the switch. */
    public static void tick(ServerLevel level, Pig pig) {
        if (pig.tickCount % 20 == 0) {
            update(level, pig);
        }
    }

    /** Puts the crown on or takes it off; returns whether the pig wears it afterwards. */
    public static boolean update(ServerLevel level, Pig pig) {
        Item crown = crown();
        if (crown == null) {
            return false;
        }
        boolean wanted = enabled() && named(pig) && !pig.isBaby() && EntityTimes.get(pig, CROWN_READY, Long.MIN_VALUE) <= level.getGameTime();
        boolean worn = crowned(pig);
        if (wanted && !worn && pig.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(crown));
            pig.setDropChance(EquipmentSlot.HEAD, 0.0F);
            EntityTimes.clear(pig, CROWN_READY);
            level.sendParticles(ParticleTypes.WAX_ON, pig.getX(), pig.getEyeY() + 0.4, pig.getZ(), 6, 0.25, 0.15, 0.25, 0.0);
            return true;
        }
        if (!wanted && worn) {
            // Renamed, a baby again or switched off: the crown simply goes (no drop, nothing to farm).
            pig.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            return false;
        }
        return worn;
    }

    /**
     * {@code LivingEntity#checkTotemDeathProtection} ({@code EasterDeathMixin}): a crowned pig never dies. Returns what happened;
     * {@link Saved#NOT_CROWNED} lets Vanilla go on (totem or death).
     */
    public static Saved neverDies(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Pig pig) || !(pig.level() instanceof ServerLevel level) || !crowned(pig) || !enabled()
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return Saved.NOT_CROWNED;
        }
        pig.setHealth(1.0F);
        level.broadcastEntityEvent(pig, (byte) 35);
        ServerPlayer player = responsible(pig, source);
        long now = level.getGameTime();
        if (player == null || EntityTimes.get(player, TRIBUTE_READY, Long.MIN_VALUE) > now) {
            return Saved.KEPT_CROWN;
        }
        int cooldown = SimpleMobHeads.config().technobladeCooldownTicks;
        pig.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        EntityTimes.set(pig, CROWN_READY, now + cooldown);
        EntityTimes.set(player, TRIBUTE_READY, now + cooldown);
        pig.spawnAtLocation(level, new ItemStack(crown()), pig.getBbHeight());
        return Saved.CROWN_FELL;
    }

    /** The player behind the deadly hit: the attacker, else the last player who hurt the pig (Vanilla's 5 seconds). */
    static @Nullable ServerPlayer responsible(Pig pig, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return pig.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
    }

    /** {@code Pig#thunderHit} ({@code PigThunderMixin}): lightning leaves Technoblade a pig (crowned or waiting for the crown). */
    public static boolean resistsLightning(Pig pig) {
        return (crowned(pig) || named(pig)) && enabled();
    }

    /**
     * Wearing the crowned head (F-A, ability {@code crowned_pig}): pigs within {@link #LOOK_RANGE} turn their heads to the wearer,
     * and every {@link #GLINT_TICKS} the crown glints. Purely cosmetic; returns the pigs that looked.
     */
    public static List<Pig> charm(ServerLevel level, ServerPlayer player, long now) {
        if (!HeadAbilities.active(player, HEAD)) {
            return List.of();
        }
        List<Pig> pigs = level.getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(LOOK_RANGE), Pig::isAlive);
        for (Pig pig : pigs) {
            pig.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
        if (now % GLINT_TICKS == 0) {
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getEyeY() + 0.55, player.getZ(), 2, 0.18, 0.05, 0.18, 0.0);
        }
        return pigs;
    }
}
