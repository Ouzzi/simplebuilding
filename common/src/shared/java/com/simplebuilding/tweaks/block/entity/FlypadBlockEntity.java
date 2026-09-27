package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Flypad, 1:1 aus Simple Tweaks: im Bereich Kreativflug + Leuchten; wer den Bereich verlaesst,
 * verliert den Flug wieder (ausser Kreativ/Zuschauer). Ab Stufe IV (Enderit) faengt ein
 * Sicherheitsnetz den Absturz ab: wer fliegend hinausfliegt, bekommt 10 s Sanfter Fall.
 * Neu gegenueber Simple Tweaks: wird das Pad abgebaut oder abgeschaltet, verlieren auch die
 * Spieler im Bereich den Flug (vorher behielten sie ihn fuer immer).
 *
 * <p>Seit dem Audit 2026-09-26 (#32): ein Pad nimmt nur Flug zurueck, den ein Flypad gegeben hat
 * (Spieler-Tag {@link #FLIGHT_TAG}, ueberlebt Neustarts), und nicht, solange ein anderes Flypad den
 * Spieler noch abdeckt - vorher holte das eine Pad ihn aus der Luft, bis das andere ihn fuenf Ticks
 * spaeter wieder fliegen liess.
 */
public class FlypadBlockEntity extends OwnedBlockEntity {
    public static final int SAFETY_NET_TICKS = 200;
    /** Merkt am Spieler, dass sein Flug von einem Flypad stammt (nicht Kreativ, nicht ein anderer Mod). */
    public static final String FLIGHT_TAG = "simplebuilding.flypad_flight";

    private final Set<UUID> flyingPlayers = new HashSet<>();

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
        if (level.getGameTime() % 5 == 0) {
            update(level, pos, state, be);
        }
    }

    /** Ein Durchlauf: Flug geben, Flug nehmen (der Tick macht das alle 5 Ticks). */
    public static void update(Level level, BlockPos pos, BlockState state, FlypadBlockEntity be) {
        int tier = tierOf(state);
        if (!SimpleTweaks.config().pads.enableFlypads) {
            be.revokeAll(level, tier);
            return;
        }

        AABB range = PadTiers.flyArea(pos, tier);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, range, p -> true);
        Set<UUID> current = new HashSet<>();
        for (ServerPlayer player : players) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                player.addTag(FLIGHT_TAG);
            }
            // Nur Flug, den ein Flypad gab, wird verfolgt (und spaeter zurueckgenommen).
            if (player.entityTags().contains(FLIGHT_TAG)) {
                current.add(player.getUUID());
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
        if (player == null || !player.entityTags().contains(FLIGHT_TAG)) {
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
        int reach = (int) Math.ceil(PadTiers.halfWidth(PadTiers.MAX) / 16.0) + 1;
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
                            && PadTiers.flyArea(pad.getBlockPos(), tierOf(pad.getBlockState())).intersects(player.getBoundingBox())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Nimmt einem Spieler den Pad-Flug (nicht im Kreativ-/Zuschauermodus); ab Enderit mit Sicherheitsnetz. */
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
        if (wasFlying && PadTiers.hasEnderiteBonus(tier)) {
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
