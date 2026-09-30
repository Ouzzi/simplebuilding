package com.simplefun.event;

import com.simplefun.SimplefunCommon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Shared (loader-agnostic) handler: on a PvP kill, the victim drops their head with their skin.
 * Each loader wires this to its own "entity died" event.
 */
public class PlayerHeadDrop {

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!SimplefunCommon.getConfig().fun.playerHeadDrops) return;

        if (entity instanceof ServerPlayer victim && source.getEntity() instanceof Player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victim.getGameProfile()));
            victim.drop(head, true, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
}
