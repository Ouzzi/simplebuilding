package com.simplevisuals.client;

import com.simplevisuals.Visuals;
import com.simplevisuals.effects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded cosmetic work. Uses loaded client data only and never sends gameplay packets. */
public final class Immersion {
    public static final ParticleBudget BUDGET = new ParticleBudget();
    private static int tick;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static final java.util.Map<Integer, Float> health = new java.util.HashMap<>();
    public static int lastEmitted;

    public static void tick(Minecraft client) {
        if (client.level != level) { health.clear(); tick = 0; level = client.level; }
        lastEmitted = 0;
        if (client.level == null || client.player == null || client.isPaused()) return;
        tick++;
        ConfigSafety.normalizePeriodically(tick);
        BUDGET.begin(client.options.particles().get().ordinal());
        int players = 0;
        // Rotate effect priority each tick; ambient effects cannot permanently starve reactions.
        for (var player : client.level.players()) {
            if (players++ >= ParticleBudget.MAX_TRACKED_PLAYERS) break;
            if (player.isSpectator() || player.isInvisible() || player.distanceToSqr(client.player) > 256) continue;
            for (int index = 0; index < EffectRegistry.ALL.size(); index++) {
                var effect = EffectRegistry.ALL.get((index + tick) % EffectRegistry.ALL.size());
                if (effect.category().equals("reactive") || tick % effect.interval() != 0) continue;
                var pos = player.blockPosition();
                boolean active = switch (effect.id()) {
                    case "footstep_dust" -> player.onGround() && !player.isInWater() && player.getDeltaMovement().horizontalDistanceSqr() > .015;
                    case "cold_breath" -> !player.isUnderWater() && client.level.getBiome(pos).value().coldEnoughToSnow(pos, client.level.getSeaLevel());
                    case "fireflies" -> client.level.getOverworldClockTime() % 24000 >= 13000 && client.level.getOverworldClockTime() % 24000 <= 23000 && client.level.getBlockState(pos.below()).is(BlockTags.DIRT);
                    case "pollen" -> client.level.getOverworldClockTime() % 24000 < 12000 && nearby(client, pos, BlockTags.FLOWERS, null);
                    case "fire_sparks" -> nearby(client, pos, null, Blocks.FIRE) || nearby(client, pos, null, Blocks.LAVA) || nearby(client, pos, null, Blocks.CAMPFIRE);
                    case "water_ripples" -> player.isInWater() && !player.isUnderWater() && player.getDeltaMovement().horizontalDistanceSqr() > .005;
                    case "water_droplets" -> player.isInWater() && player.getDeltaMovement().lengthSqr() > .01;
                    case "leaf_fall" -> nearby(client, pos.above(2), BlockTags.LEAVES, null);
                    case "enchanted_items" -> player.getMainHandItem().isEnchanted() || player.getOffhandItem().isEnchanted();
                    case "beacon_aura" -> nearby(client, pos, null, Blocks.BEACON);
                    default -> false;
                };
                if (active) emit(client, effect, player.getId(), origin(player, effect.id()));
            }
        }
        int inspected = 0, tracked = 0;
        var keep = new java.util.HashSet<Integer>();
        for (var entity : client.level.entitiesForRendering()) {
            if (++inspected > 128 || tracked >= 32) break;
            if (!(entity instanceof LivingEntity living) || living.isInvisible() || living.distanceToSqr(client.player) > 256) continue;
            tracked++; keep.add(entity.getId());
            float current = living.getHealth();
            Float previous = health.put(entity.getId(), current);
            if (previous != null && Float.isFinite(current) && Math.abs(previous - current) > .01f) {
                String id = current < previous ? "damage_feedback" : "healing_feedback";
                var effect = EffectRegistry.ALL.stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
                emit(client, effect, client.player.getId(), living.position().add(0, living.getBbHeight() / 2, 0));
                if (current < previous) VisualsHud.damage(living, previous - current);
            }
        }
        health.keySet().retainAll(keep);
        lastEmitted = BUDGET.used();
    }
    private static Vec3 origin(LivingEntity player, String effect) {
        return switch (effect) {
            case "footstep_dust", "water_ripples" -> player.position().add(0, .05, 0);
            case "cold_breath" -> player.getEyePosition().add(player.getLookAngle().scale(.4));
            case "leaf_fall" -> player.position().add(0, 3, 0);
            default -> player.position().add(0, 1, 0);
        };
    }
    private static boolean nearby(Minecraft client, BlockPos pos, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag, net.minecraft.world.level.block.Block block) {
        // Six loaded block reads, never a radius/chunk scan or forced chunk load.
        for (var direction : net.minecraft.core.Direction.values()) {
            var sample = pos.relative(direction);
            if (!client.level.hasChunkAt(sample)) continue;
            var state = client.level.getBlockState(sample);
            if (tag != null ? state.is(tag) : state.is(block)) return true;
        }
        return false;
    }
    public static void emit(Minecraft client, EffectRegistry.Effect effect, int player, Vec3 pos) {
        if (client.level == null || client.player == null || client.player.distanceToSqr(pos) > 256) return;
        var type = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(effect.particle()));
        if (!(type instanceof SimpleParticleType particle)) return;
        int count = BUDGET.claim(player, EffectRegistry.level(Visuals.CONFIG, effect.id()).count());
        for (int i = 0; i < count; i++) {
            double x = (client.level.getRandom().nextDouble() - .5) * .35;
            double z = (client.level.getRandom().nextDouble() - .5) * .35;
            client.level.addParticle(particle, pos.x + x, pos.y, pos.z + z, 0, .015, 0);
        }
    }
    private static final class ConfigSafety {
        static void normalizePeriodically(int tick) { if (tick % 20 == 0) com.simplevisuals.ConfigOptions.normalize(Visuals.CONFIG); }
    }
}
