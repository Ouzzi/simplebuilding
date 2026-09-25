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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Flypad, 1:1 aus Simple Tweaks: im Bereich Kreativflug + Leuchten; wer den Bereich verlaesst,
 * verliert den Flug wieder (ausser Kreativ/Zuschauer). Ab Stufe IV (Enderit) faengt ein
 * Sicherheitsnetz den Absturz ab: wer fliegend hinausfliegt, bekommt 10 s Sanfter Fall.
 * Neu gegenueber Simple Tweaks: wird das Pad abgebaut oder abgeschaltet, verlieren auch die
 * Spieler im Bereich den Flug (vorher behielten sie ihn fuer immer).
 */
public class FlypadBlockEntity extends OwnedBlockEntity {
    public static final int SAFETY_NET_TICKS = 200;

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
            current.add(player.getUUID());
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, true, false, false));
        }

        Iterator<UUID> it = be.flyingPlayers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (!current.contains(id)) {
                revoke(level, id, tier);
                it.remove();
            }
        }
        be.flyingPlayers.addAll(current);
    }

    private void revokeAll(Level level, int tier) {
        for (UUID id : flyingPlayers) {
            revoke(level, id, tier);
        }
        flyingPlayers.clear();
    }

    private static void revoke(Level level, UUID id, int tier) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
        if (player != null) {
            revoke(player, tier);
        }
    }

    /** Nimmt einem Spieler den Pad-Flug (nicht im Kreativ-/Zuschauermodus); ab Enderit mit Sicherheitsnetz. */
    public static void revoke(ServerPlayer player, int tier) {
        // Kreativ = instabuild (Simple Tweaks fragte isCreative(); fuer echte Spieler dasselbe).
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
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
