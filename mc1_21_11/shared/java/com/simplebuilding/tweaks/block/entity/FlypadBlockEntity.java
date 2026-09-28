package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.LegacyFlypadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.simplebuilding.util.Feedback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Flypad, aus Simple Tweaks: im Bereich Kreativflug + Leuchten; wer den Bereich verlaesst,
 * verliert den Flug wieder (ausser Kreativ/Zuschauer). Seit 2026-09-27 drei Stufen aus Enderit,
 * jede mit dem Sicherheitsnetz: wer fliegend hinausfliegt, bekommt 10 s Sanfter Fall. Alte Flypads
 * (netherite_flypad, enderite_flypad) werden beim ersten Tick zu ihrer neuen Stufe
 * ({@link LegacyFlypadBlock}).
 * Neu gegenueber Simple Tweaks: wird das Pad abgebaut oder abgeschaltet, verlieren auch die
 * Spieler im Bereich den Flug (vorher behielten sie ihn fuer immer).
 *
 * <p>Seit dem Audit 2026-09-26 (#32): ein Pad nimmt nur Flug zurueck, den ein Flypad gegeben hat
 * (Spieler-Tag {@link #FLIGHT_TAG}, ueberlebt Neustarts), und nicht, solange ein anderes Flypad den
 * Spieler noch abdeckt - vorher holte das eine Pad ihn aus der Luft, bis das andere ihn fuenf Ticks
 * spaeter wieder fliegen liess.
 */
public class FlypadBlockEntity extends OwnedBlockEntity implements PadSignalSource {
    public static final int SAFETY_NET_TICKS = 200;
    /** Merkt am Spieler, dass sein Flug von einem Flypad stammt (nicht Kreativ, nicht ein anderer Mod). */
    public static final String FLIGHT_TAG = "simplebuilding.flypad_flight";

    /**
     * Rand-Warnung (Immersion 2026-09-28): wer fliegend naeher als so viele Bloecke an den Rand
     * (Seiten oder Decke) kommt, hoert ein Warnsignal, nur er selbst, und sieht Funken an der
     * Feldgrenze - je naeher, desto hoeher der Ton.
     */
    public static final double WARNING_MARGIN = 1.5;
    /** Hoechstens so oft (Ticks) eine Warnung je Spieler. */
    public static final int WARNING_INTERVAL = 10;

    private final Set<UUID> flyingPlayers = new HashSet<>();
    /** Spielzeit der letzten Rand-Warnung je Spieler (nur zur Laufzeit). */
    private final Map<UUID, Long> lastWarning = new HashMap<>();
    /** Spieler im Bereich beim letzten Durchlauf (Komparator-Signal, hoechstens 15). */
    private int served;

    public FlypadBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.FLYPAD, pos, state);
    }

    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof FlypadBlock pad ? pad.getTier() : 1;
    }

    public Set<UUID> flyingPlayers() {
        return flyingPlayers;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FlypadBlockEntity be) {
        if (state.getBlock() instanceof LegacyFlypadBlock legacy) {
            legacy.migrate(level, pos, be);
            return;
        }
        if (level.getGameTime() % 5 == 0) {
            update(level, pos, state, be);
        }
    }

    /** Ein Durchlauf: Flug geben, Flug nehmen (der Tick macht das alle 5 Ticks). */
    public static void update(Level level, BlockPos pos, BlockState state, FlypadBlockEntity be) {
        int tier = tierOf(state);
        if (!SimpleTweaks.config().pads.enableFlypads || com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, pos)) {
            // Abgeschaltet (Config oder Redstone-Signal, Besitzer 2026-09-28): niemand fliegt mehr ueber dieses Pad.
            be.revokeAll(level, tier);
            setActive(level, pos, state, false);
            be.setServed(level, pos, state, 0);
            return;
        }

        AABB range = areaOf(level, pos, state);
        List<ServerPlayer> players = PlayerScan.playersIn(level, range, ServerPlayer.class);
        Set<UUID> current = new HashSet<>();
        for (ServerPlayer player : players) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                player.addTag(FLIGHT_TAG);
                com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.FLYPAD);
                // Flug erteilt: ein heller Leuchtfeuer-Ton nur fuer ihn, Funken um die Fuesse.
                Feedback.playTo(player, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5f, 1.6f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.1, player.getZ(),
                            10, 0.3, 0.05, 0.3, 0.02);
                }
            }
            // Nur Flug, den ein Flypad gab, wird verfolgt (und spaeter zurueckgenommen).
            if (player.getTags().contains(FLIGHT_TAG)) {
                current.add(player.getUUID());
                if (player.getAbilities().flying) {
                    be.warnNearEdge(level, player, range);
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, true, false, false));
        }

        Iterator<UUID> it = be.flyingPlayers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (!current.contains(id)) {
                be.release(level, id, tier);
                it.remove();
            }
        }
        be.flyingPlayers.addAll(current);
        be.lastWarning.keySet().retainAll(current);
        setActive(level, pos, state, !players.isEmpty());
        be.setServed(level, pos, state, players.size());
    }

    /** Stellt den sichtbaren Zustand ({@link FlypadBlock#ACTIVE}) ein; derselbe Block, die Block-Entity bleibt. */
    private static void setActive(Level level, BlockPos pos, BlockState state, boolean active) {
        if (state.hasProperty(FlypadBlock.ACTIVE) && state.getValue(FlypadBlock.ACTIVE) != active
                && level.getBlockState(pos) == state) {
            level.setBlock(pos, state.setValue(FlypadBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /**
     * Abstand eines Punkts zum naechsten Rand des Felds (vier Seiten und Decke; der Boden ist das Pad
     * selbst). Negativ = ausserhalb.
     */
    public static double edgeMargin(AABB area, double x, double y, double z) {
        return Math.min(Math.min(x - area.minX, area.maxX - x), Math.min(Math.min(z - area.minZ, area.maxZ - z), area.maxY - y));
    }

    /** Tonhoehe der Rand-Warnung: 0,6 am Beginn des Warnstreifens bis 1,2 direkt am Rand. */
    public static float warningPitch(double margin) {
        double closeness = 1.0 - Math.max(0.0, Math.min(WARNING_MARGIN, margin)) / WARNING_MARGIN;
        return (float) (0.6 + 0.6 * closeness);
    }

    /** Warnt einen fliegenden Spieler, der dem Rand nahe kommt (hoechstens alle {@link #WARNING_INTERVAL} Ticks). */
    private void warnNearEdge(Level level, ServerPlayer player, AABB area) {
        double margin = edgeMargin(area, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ());
        if (margin >= WARNING_MARGIN || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = level.getGameTime();
        Long last = lastWarning.get(player.getUUID());
        if (last != null && now - last < WARNING_INTERVAL) {
            return;
        }
        lastWarning.put(player.getUUID(), now);
        Feedback.playTo(player, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6f, warningPitch(margin));
        // Funken auf der naechsten Feldwand, auf Hoehe des Spielers - nur er sieht sie.
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();
        double[] distances = {x - area.minX, area.maxX - x, z - area.minZ, area.maxZ - z, area.maxY - y};
        int nearest = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[nearest]) {
                nearest = i;
            }
        }
        double px = nearest == 0 ? area.minX : nearest == 1 ? area.maxX : x;
        double pz = nearest == 2 ? area.minZ : nearest == 3 ? area.maxZ : z;
        double py = nearest == 4 ? area.maxY : y;
        double sx = nearest <= 1 ? 0.0 : 0.6;
        double sz = nearest == 2 || nearest == 3 ? 0.0 : 0.6;
        double sy = nearest == 4 ? 0.0 : 0.6;
        serverLevel.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, false, false, px, py, pz, 8, sx, sy, sz, 0.0);
    }

    /** Zahl der Spieler im Bereich (0..15) fuer den Komparator. */
    @Override
    public int comparatorSignal() {
        return Math.min(15, served);
    }

    private void setServed(Level level, BlockPos pos, BlockState state, int count) {
        int before = comparatorSignal();
        this.served = count;
        if (comparatorSignal() != before) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Bereich dieses gesetzten Pads; die letzte Easter-Stufe ({@link EasterEggs}) ist doppelt so breit und hoch. */
    public static AABB areaOf(Level level, BlockPos pos, BlockState state) {
        return PadTiers.flyArea(pos, tierOf(state), EasterEggs.isBoosted(level, pos));
    }

    private void revokeAll(Level level, int tier) {
        for (UUID id : flyingPlayers) {
            release(level, id, tier);
        }
        flyingPlayers.clear();
    }

    /** Dieses Pad laesst den Spieler los; der Flug endet nur, wenn kein anderes Flypad ihn traegt. */
    private void release(Level level, UUID id, int tier) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
        if (player == null || !player.getTags().contains(FLIGHT_TAG)) {
            return;
        }
        if (player.level() == level && anotherPadCovers(serverLevel, player)) {
            return;
        }
        revoke(player, tier);
    }

    /** Ob ein anderes eingeschaltetes Flypad den Spieler abdeckt (Suche ueber die Chunks in Reichweite des groessten Pads). */
    private boolean anotherPadCovers(ServerLevel level, ServerPlayer player) {
        if (!SimpleTweaks.config().pads.enableFlypads) {
            return false;
        }
        // Groesster Bereich: die letzte Easter-Stufe, doppelt so breit wie Stufe III.
        int reach = (int) Math.ceil(PadTiers.flyWidth(PadTiers.FLYPAD_MAX) / 16.0) + 1;
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity other : chunk.getBlockEntities().values()) {
                    if (other != this && !other.isRemoved() && other instanceof FlypadBlockEntity pad
                            && !com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, pad.getBlockPos())
                            && areaOf(level, pad.getBlockPos(), pad.getBlockState()).intersects(player.getBoundingBox())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Nimmt einem Spieler den Pad-Flug (nicht im Kreativ-/Zuschauermodus); mit Sicherheitsnetz (alle Stufen sind aus Enderit). */
    public static void revoke(ServerPlayer player, int tier) {
        // Kreativ = instabuild (Simple Tweaks fragte isCreative(); fuer echte Spieler dasselbe).
        if (player.getAbilities().instabuild || player.isSpectator()) {
            // Der Flug gehoert jetzt dem Spielmodus: den Tag trotzdem loesen, sonst nimmt ein Pad
            // spaeter Flug zurueck, den ein anderer Mod gab (Nach-Audit N16).
            player.removeTag(FLIGHT_TAG);
            return;
        }
        player.removeTag(FLIGHT_TAG);
        boolean wasFlying = player.getAbilities().flying;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        // Flug vorbei: das Abschalten eines Leuchtfeuers, nur fuer ihn.
        Feedback.playTo(player, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.5f);
        if (wasFlying && PadTiers.flypadHasSafetyNet(tier)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SAFETY_NET_TICKS, 0, false, true, true));
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            revokeAll(level, tierOf(getBlockState()));
        }
        super.setRemoved();
    }
}
