package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.ElytraPadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
        AABB range = PadTiers.elytraArea(pos, tier);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, range, p -> true);
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;

        for (ServerPlayer player : players) {
            applyTo(level, pos, tier, player, config);
        }
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
            player.displayClientMessage(Component.translatable("message.simplebuilding.elytra_pad.equipped").withStyle(ChatFormatting.GREEN), true);
        } else if (chest.is(TweaksItems.SPAWN_ELYTRA)) {
            chest.set(TweaksComponents.LAST_PAD_TICK, level.getGameTime());
            chest.set(TweaksComponents.FLIGHT_TIME, config.flightTimeSeconds * 20);
            if (PadTiers.hasEnderiteBonus(tier) || isInBoostColumn(player, pos)) {
                chest.set(TweaksComponents.BOOST_LEVEL, 1.0f);
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
