package com.simplebuilding.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leuchtende (emittierende, "Radiance") Ruestung setzt einen unsichtbaren Lichtblock. Traeger
 * sind Spieler (Position in einer Tabelle, wandert alle 2 Ticks mit), Mobs, Ruestungsstaender und
 * Gegenstandsrahmen (die ihren Lichtblock ueber {@link OwnedLightHolder} mit der Entity speichern).
 */
public class DynamicLightHandler {
    private static final Map<UUID, BlockPos> lightSources = new HashMap<>();

    /** Ruhende Traeger pruefen nur alle so viele Ticks (versetzt nach Entity-ID). */
    public static final int HOLDER_INTERVAL = 10;

    /** Ein Aufwertungslevel bringt 3 Lichtpunkte -> 5 Level = 15 (Max). */
    public static int lightLevelFor(int emissionPoints) {
        return Math.min(15, Math.max(0, emissionPoints) * 3);
    }

    /** Summe der Emissionslevel aller getragenen Ruestungsteile. */
    public static int wornEmission(LivingEntity entity) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                total += GlowingTrimUtils.getEmissionLevel(entity.getItemBySlot(slot));
            }
        }
        return total;
    }

    public static void tick(Player player) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer)) return;

        Level world = player.level();
        UUID uuid = player.getUUID();
        BlockPos currentPos = player.blockPosition().above(); // Kopfhöhe für bessere Ausleuchtung

        // 1. Licht-Level NUR aus der Emission (nicht Visual Glow)
        int lightLevel = lightLevelFor(wornEmission(player));

        BlockPos oldPos = lightSources.get(uuid);

        // 2. Aufräumen (Wenn bewegt oder Licht aus)
        if (oldPos != null && !oldPos.equals(currentPos)) {
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }

        // 3. Neues Licht setzen
        if (lightLevel > 0) {
            if (placeOrUpdate(world, currentPos, lightLevel)) {
                lightSources.put(uuid, currentPos);
            }
        } else if (oldPos != null) {
            // Wenn Lichtlevel auf 0 gefallen ist, altes Licht entfernen
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }
    }

    /** Wandernde Traeger (Mobs) pruefen oefter als ruhende, damit das Licht ihnen folgt. */
    public static final int MOB_INTERVAL = 4;

    /**
     * Ruestungsstaender und Mobs: Licht auf Kopfhoehe wie beim Spieler, mit Position im Traeger
     * gespeichert. Aufruf jeden Tick aus dem LivingEntityMixin; Spieler laufen ueber {@link #tick}.
     */
    public static void tickWearer(LivingEntity wearer) {
        if (wearer instanceof Player || !(wearer instanceof OwnedLightHolder holder)) return;
        int interval = wearer instanceof ArmorStand ? HOLDER_INTERVAL : MOB_INTERVAL;
        if (!isHolderTick(wearer, interval)) return;
        tickHolder(wearer.level(), holder, wearer.blockPosition().above(), wornEmission(wearer));
    }

    /** Gegenstandsrahmen: Licht im Block des Rahmens, gespeist vom eingelegten Gegenstand. */
    public static void tickItemFrame(ItemFrame frame) {
        if (!(frame instanceof OwnedLightHolder holder) || !isHolderTick(frame, HOLDER_INTERVAL)) return;
        tickHolder(frame.level(), holder, frame.getPos(), GlowingTrimUtils.getEmissionLevel(frame.getItem()));
    }

    private static boolean isHolderTick(Entity entity, int interval) {
        return !entity.level().isClientSide() && !entity.isRemoved()
                && Math.floorMod(entity.level().getGameTime() + entity.getId(), interval) == 0;
    }

    static void tickHolder(Level level, OwnedLightHolder holder, BlockPos target, int emissionPoints) {
        int lightLevel = lightLevelFor(emissionPoints);
        BlockPos owned = holder.simplebuilding$getOwnedLight();
        if (owned != null && (lightLevel == 0 || !owned.equals(target))) {
            removeLight(level, owned);
            holder.simplebuilding$setOwnedLight(null);
        }
        if (lightLevel > 0 && placeOrUpdate(level, target, lightLevel)) {
            holder.simplebuilding$setOwnedLight(target);
        }
    }

    /**
     * Setzt oder aktualisiert den Lichtblock - nur in Luft, einem schon stehenden Lichtblock
     * oder einer Wasserquelle (um nichts zu zerstören).
     * Blocks.LIGHT ist replaceable, aber NICHT air, und ein trockener Lichtblock meldet eine
     * leere Fluidstate - ohne den is(Blocks.LIGHT)-Zweig bliebe ein schon stehender Lichtblock
     * unangetastet und die Helligkeit aenderte sich erst beim naechsten Schritt. isWater stammt
     * aus dem aktuellen Zustand und ist fuer einen waterlogged Lichtblock true, das Wasser
     * bleibt beim Aktualisieren also erhalten.
     */
    private static boolean placeOrUpdate(Level world, BlockPos pos, int lightLevel) {
        BlockState currentState = world.getBlockState(pos);
        boolean isWater = currentState.getFluidState().is(FluidTags.WATER);
        if (!(currentState.isAir() || currentState.is(Blocks.LIGHT)
                || (isWater && currentState.getFluidState().isSource()))) {
            return false;
        }
        // Nur schreiben, wenn sich das Level aendert
        if (!currentState.is(Blocks.LIGHT) || currentState.getValue(LightBlock.LEVEL) != lightLevel) {
            world.setBlock(pos, Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, lightLevel)
                    .setValue(LightBlock.WATERLOGGED, isWater), 3);
        }
        return true;
    }

    private static void removeLight(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            if (state.getValue(LightBlock.WATERLOGGED)) {
                world.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        BlockPos pos = lightSources.remove(player.getUUID());
        if (pos != null) {
            removeLight(player.level(), pos);
        }
    }

    /**
     * Aus Entity.setRemoved (HEAD) - die Entity steht also noch in ihrer alten Welt.
     * Ruhende Traeger raeumen nur auf, wenn sie wirklich zerstoert werden; beim Entladen des
     * Chunks bleibt ihr Lichtblock samt gespeicherter Position erhalten. Ein Spieler raeumt bei
     * jedem Grund auf, auch beim Dimensionswechsel (sonst bliebe das Licht in der alten Dimension
     * stehen, weil sein naechster Tick schon in der neuen Welt arbeitet).
     */
    public static void onEntityRemoved(Entity entity, Entity.RemovalReason reason) {
        Level level = entity.level();
        if (level == null || level.isClientSide()) return;
        // Spieler zuerst: auch sie tragen (als LivingEntity) das Holder-Feld, nutzen es aber nicht.
        if (entity instanceof ServerPlayer) {
            BlockPos pos = lightSources.remove(entity.getUUID());
            if (pos != null) {
                removeLight(level, pos);
            }
        } else if (entity instanceof OwnedLightHolder holder) {
            BlockPos owned = holder.simplebuilding$getOwnedLight();
            if (owned != null && reason.shouldDestroy()) {
                removeLight(level, owned);
                holder.simplebuilding$setOwnedLight(null);
            }
        }
    }

    // --- PARTIKEL (Client) ---

    /**
     * Leuchtende Teile sehen sonst genauso aus wie normale: feine, warme Schimmer-Partikel
     * (Wachs-Glanz: kurzlebig, selbstleuchtend, goldgelb wie der Glowstone-Staub der Aufwertung).
     * Die Chance je Tick waechst mit der Emission und ist gedeckelt, damit volle Ausruestung
     * nicht qualmt. Unsichtbare Traeger zeigen nichts. Nur clientseitig wirksam.
     */
    public static void tickGlowMotes(Entity entity, IntSupplier emissionPoints) {
        Level level = entity.level();
        if (!level.isClientSide() || (entity.isInvisible() && !(entity instanceof ItemFrame))) return;
        RandomSource random = entity.getRandom();
        // Erst wuerfeln, dann die Emission lesen: die meisten Ticks kosten so keinen NBT-Zugriff.
        float roll = random.nextFloat();
        if (roll >= MAX_MOTE_CHANCE) return;
        if (roll >= moteChance(emissionPoints.getAsInt())) return;
        double x;
        double y;
        double z;
        if (entity instanceof ItemFrame frame) {
            Direction face = frame.getDirection();
            x = frame.getX() + face.getStepX() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
            y = frame.getY() + face.getStepY() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
            z = frame.getZ() + face.getStepZ() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
        } else {
            x = entity.getRandomX(0.6);
            y = entity.getRandomY();
            z = entity.getRandomZ(0.6);
        }
        level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0.0, 0.01, 0.0);
    }

    public static final float MAX_MOTE_CHANCE = 0.12f;

    /** 1 Level: ein Funke etwa alle 2,5 s; ab 6 Level gedeckelt bei gut 2 pro Sekunde. */
    public static float moteChance(int emissionPoints) {
        return Math.min(MAX_MOTE_CHANCE, 0.02f * Math.max(0, emissionPoints));
    }
}
