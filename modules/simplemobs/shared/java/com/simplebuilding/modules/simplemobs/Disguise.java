package com.simplebuilding.modules.simplemobs;

import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Cloak of deception on players (owner 11.10.): worn, mobs notice you only within {@link CloakLogic#DETECT_RADIUS};
 * right-click a mob or player with an empty hand to take its shape; hitting or breaking a block ends the disguise and
 * reveals you to the attacked mob and the 5x5 area around it. Rules live in {@link CloakLogic}.
 */
public final class Disguise {
    public static final EquipmentSlot[] GEAR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private Disguise() {}

    private static DisguiseHolder holder(Player p) { return (DisguiseHolder) p; }

    public static boolean wearsCloak(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.CHEST).is(DisguiseRegistry.CLOAK);
    }

    /** The synced disguise spec ("" when none; safe during construction). */
    public static String spec(Player p) {
        if (p.getEntityData() == null) return "";
        return holder(p).simplemobs$spec();
    }

    public static boolean disguised(Player p) { return !spec(p).isEmpty(); }

    /** The mob type a player looks like, or null (none or a player disguise). */
    public static @Nullable EntityType<?> morphType(Player p) {
        String s = spec(p);
        if (s.isEmpty() || CloakLogic.isPlayerSpec(s)) return null;
        Identifier id = Identifier.tryParse(s);
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    public static ItemStack gear(Player p, int slot) { return holder(p).simplemobs$gear(slot); }

    public static boolean seesThrough(EntityType<?> type) {
        return type.builtInRegistryHolder().is(DisguiseRegistry.SEES_THROUGH);
    }

    /** Whether a right-click on {@code target} may turn the player into it. */
    public static boolean canImitate(Entity target) {
        return (target instanceof Mob || target instanceof Player) && target.isAlive() && !seesThrough(target.getType());
    }

    // ---- server side ------------------------------------------------------------------

    /** Takes the shape of {@code target}; false (with a hint) when it is a boss or the shape has no room here. */
    public static boolean morph(Player p, LivingEntity target) {
        if (!(p.level() instanceof ServerLevel level) || !wearsCloak(p)) return false;
        if (seesThrough(target.getType()) || !(target instanceof Mob || target instanceof Player)) {
            p.sendOverlayMessage(Component.translatable("message.simplemobs.disguise.refused", target.getType().getDescription()));
            return false;
        }
        String spec;
        if (target instanceof Player other) {
            spec = CloakLogic.playerSpec(other.getUUID(), other.getGameProfile().name());
            for (int i = 0; i < GEAR_SLOTS.length; i++) holder(p).simplemobs$setGear(i, other.getItemBySlot(GEAR_SLOTS[i]).copy());
        } else {
            EntityType<?> type = target.getType();
            if (!level.noCollision(p, type.getDimensions().makeBoundingBox(p.position()))) {
                p.sendOverlayMessage(Component.translatable("message.simplemobs.disguise.no_room"));
                return false;
            }
            spec = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            for (int i = 0; i < GEAR_SLOTS.length; i++) holder(p).simplemobs$setGear(i, ItemStack.EMPTY);
            // Its own kind stops hunting the new "kin".
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32.0), m -> m.getType() == type)) {
                if (m.getTarget() == p) m.setTarget(null);
                if (m.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
        holder(p).simplemobs$setSpec(spec);
        p.refreshDimensions();
        p.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(CloakLogic.MORPH_COST, p, EquipmentSlot.CHEST);
        level.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + p.getBbHeight() / 2, p.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        p.sendOverlayMessage(Component.translatable("message.simplemobs.disguise.morph", target.getDisplayName()));
        return true;
    }

    /** Back to the own shape (no-op when not disguised). */
    public static void revert(Player p) {
        if (!disguised(p)) return;
        holder(p).simplemobs$setSpec("");
        for (int i = 0; i < GEAR_SLOTS.length; i++) holder(p).simplemobs$setGear(i, ItemStack.EMPTY);
        p.refreshDimensions();
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + p.getBbHeight() / 2, p.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    /** A cloaked player hurt {@code victim}: shape falls, the victim and the 5x5 around it know him, others see farther. */
    public static void reveal(Player p, LivingEntity victim) {
        revert(p);
        if (!(p.level() instanceof ServerLevel level) || !wearsCloak(p)) return;
        long now = level.getGameTime();
        Map<UUID, Long> spotted = holder(p).simplemobs$spotted();
        for (Mob m : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(CloakLogic.SPOT_HALF + 1.0),
                m -> CloakLogic.inSpotArea(m.getX() - victim.getX(), m.getY() - victim.getY(), m.getZ() - victim.getZ()))) {
            spotted.put(m.getUUID(), now + CloakLogic.SPOTTED_TICKS);
        }
        spotted.put(victim.getUUID(), now + CloakLogic.SPOTTED_TICKS);
        if (holder(p).simplemobs$revealedUntil() < now) {
            p.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(CloakLogic.REVEAL_COST, p, EquipmentSlot.CHEST);
            p.sendOverlayMessage(Component.translatable("message.simplemobs.disguise.revealed"));
        }
        holder(p).simplemobs$setRevealedUntil(now + CloakLogic.REVEAL_TICKS);
    }

    public static boolean revealed(Player p) {
        return holder(p).simplemobs$revealedUntil() >= p.level().getGameTime();
    }

    public static boolean spottedBy(Player p, Entity mob) {
        Long until = holder(p).simplemobs$spotted().get(mob.getUUID());
        return until != null && until >= p.level().getGameTime();
    }

    /** Whether {@code targeter} fails to notice the cloaked player (combat targeting only). */
    public static boolean hides(LivingEntity targeter, Player p) {
        if (!wearsCloak(p) || seesThrough(targeter.getType())) return false;
        EntityType<?> look = morphType(p);
        return CloakLogic.hidden(look == targeter.getType(), spottedBy(p, targeter), revealed(p), targeter.distanceToSqr(p));
    }

    /** Server tick: the disguise ends with the cloak; old marks are dropped. */
    public static void tick(Player p) {
        if (p.level().isClientSide()) return;
        if (disguised(p) && (!wearsCloak(p) || p.isSpectator())) revert(p);
        if (p.tickCount % 100 == 0) {
            long now = p.level().getGameTime();
            holder(p).simplemobs$spotted().values().removeIf(t -> t < now);
        }
    }
}
