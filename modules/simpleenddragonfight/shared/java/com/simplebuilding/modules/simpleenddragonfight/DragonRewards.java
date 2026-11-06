package com.simplebuilding.modules.simpleenddragonfight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * WI26-6: from the {@code rewardFromDragon}-th defeated dragon on, every fight participant gets one reward item (default
 * the Ender Heart of Simple Mobs) once per player. It is placed in a ring around the End portal, picked up by its owner
 * only and never despawns. The item is looked up by id, so this module needs no Simple Mobs at compile or run time.
 */
public final class DragonRewards {
    public static final double RING = 4.0;

    private DragonRewards() {}

    /** The configured reward item, or null if the id is unknown (mod not loaded): then nothing is given or marked. */
    public static Item item() {
        Identifier id = Identifier.tryParse(DragonFightConfig.rewardItem);
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    /** Participants who get the reward for this kill: kill count reached and not rewarded before. */
    public static List<UUID> due(DragonKills data, Collection<UUID> participants) {
        List<UUID> out = new ArrayList<>();
        if (!DragonFightConfig.rewardEnabled || data.kills() < DragonFightConfig.rewardFromDragon) return out;
        for (UUID p : participants) if (!data.hasReward(p)) out.add(p);
        return out;
    }

    /** A dragon was defeated: count it and place the rewards for the online participants around the portal. Returns the number placed. */
    public static int onDefeated(ServerLevel level, UUID dragon, BlockPos center, Collection<? extends Player> participants) {
        DragonKills data = DragonKills.get(level.getServer());
        data.defeat(dragon);
        Item item = item();
        if (item == null) return 0;
        List<UUID> due = due(data, participants.stream().map(Player::getUUID).toList());
        int n = 0;
        for (Player player : participants) {
            UUID id = player.getUUID();
            if (!due.contains(id)) continue;
            double a = Math.PI * 2 * n / due.size();
            ItemEntity e = new ItemEntity(level, center.getX() + 0.5 + Math.cos(a) * RING, center.getY() + 2.0, center.getZ() + 0.5 + Math.sin(a) * RING, new ItemStack(item));
            e.setTarget(id);
            e.setUnlimitedLifetime();
            e.setNoGravity(true);
            e.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(e);
            data.markReward(id);
            player.sendSystemMessage(Component.translatable("message.simpleenddragonfight.reward", new ItemStack(item).getHoverName()));
            n++;
        }
        return n;
    }
}
