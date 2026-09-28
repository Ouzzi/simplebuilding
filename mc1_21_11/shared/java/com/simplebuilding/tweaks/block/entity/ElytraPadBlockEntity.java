package com.simplebuilding.tweaks.block.entity;

import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.ElytraPadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Elytra-Pad, 1:1 aus Simple Tweaks; ab Stufe IV (Enderit) laden Boosts im ganzen Bereich. */
public class ElytraPadBlockEntity extends OwnedBlockEntity {

    public ElytraPadBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.ELYTRA_PAD, pos, state);
    }

    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof ElytraPadBlock pad ? pad.getTier() : 1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElytraPadBlockEntity be) {
        if (level.getGameTime() % 10 == 0) {
            applyArea(level, pos, state);
        }
    }

    /** Ein Durchlauf ueber alle Spieler im Bereich (der Tick macht das alle halbe Sekunde). */
    public static void applyArea(Level level, BlockPos pos, BlockState state) {
        if (!SimpleTweaks.config().pads.enableElytraPads) {
            return;
        }
        int tier = tierOf(state);
        AABB range = areaOf(level, pos, state);
        List<ServerPlayer> players = PlayerScan.playersIn(level, range, ServerPlayer.class);
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;

        for (ServerPlayer player : players) {
            applyTo(level, pos, tier, player, config);
        }
    }

    /** Bereich dieses gesetzten Pads; die letzte Easter-Stufe ({@link EasterEggs}) ist doppelt so breit und hoch. */
    public static AABB areaOf(Level level, BlockPos pos, BlockState state) {
        return PadTiers.elytraArea(pos, tierOf(state), EasterEggs.isBoosted(level, pos));
    }

    /** Was ein Spieler im Bereich bekommt; auch fuer die Spieltests einzeln aufrufbar. */
    public static void applyTo(Level level, BlockPos pos, int tier, ServerPlayer player, TweaksConfig.Spawn config) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            ItemStack elytra = new ItemStack(TweaksItems.SPAWN_ELYTRA);
            SpawnElytra.recharge(elytra, config);
            // Pad-Elytren schuetzen NICHT vor Fall-/Kinetikschaden, nur Spawn-Elytren.
            elytra.set(TweaksComponents.IS_SAFE_ELYTRA, false);
            elytra.set(TweaksComponents.LAST_PAD_TICK, level.getGameTime());
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            // Angelegt: Vanillas Elytra-Anlegeklang statt einer Meldung (keine Bildschirmtexte).
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            // ... und eine Wolke weisser Federn (Wolkenpartikel) um die Schultern.
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.35, 0.2, 0.35, 0.01);
            }
            com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.ELYTRA_PAD);
        } else if (chest.is(TweaksItems.SPAWN_ELYTRA)) {
            chest.set(TweaksComponents.LAST_PAD_TICK, level.getGameTime());
            chest.set(TweaksComponents.FLIGHT_TIME, config.flightTicks());
            if (PadTiers.hasEnderiteBonus(tier) || isInBoostColumn(player, pos)) {
                Float before = chest.get(TweaksComponents.BOOST_LEVEL);
                chest.set(TweaksComponents.BOOST_LEVEL, 1.0f);
                if (before != null && before < 1.0f) {
                    // Boost wieder voll: ein Feuerwerks-Knistern nur fuer ihn, Funken am Ruecken.
                    com.simplebuilding.util.Feedback.playTo(player, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.5f, 1.4f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.25, 0.25, 0.25, 0.02);
                    }
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, true, false, false));
        }
    }

    /** 3x3-Saeule direkt ueber dem Pad, 4 Bloecke hoch. */
    public static boolean isInBoostColumn(ServerPlayer player, BlockPos pos) {
        AABB boost = new AABB(pos).inflate(1.5, 0, 1.5).expandTowards(0, 4.0, 0);
        return boost.intersects(player.getBoundingBox());
    }
}
