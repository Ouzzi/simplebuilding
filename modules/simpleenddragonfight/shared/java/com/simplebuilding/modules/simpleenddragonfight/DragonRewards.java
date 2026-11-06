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
    public static Item item() { return lookup(DragonFightConfig.rewardItem); }

    /** The configured jewel (from {@code rewardJewelFromDragon} on; default the Ender Gem of Simple Mobs), or null. */
    public static Item jewel() { return lookup(DragonFightConfig.rewardJewelItem); }

    private static Item lookup(String name) {
        Identifier id = Identifier.tryParse(name);
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    /** Participants who get the reward for this kill: kill count reached and not rewarded before. */
    public static List<UUID> due(DragonKills data, Collection<UUID> participants) {
        List<UUID> out = new ArrayList<>();
        if (!DragonFightConfig.rewardEnabled || data.kills() < DragonFightConfig.rewardFromDragon) return out;
        for (UUID p : participants) if (!data.hasReward(p)) out.add(p);
        return out;
    }

    /** Participants who get the jewel for this kill ({@code rewardJewelFromDragon} 0 = off). */
    public static List<UUID> dueJewel(DragonKills data, Collection<UUID> participants) {
        List<UUID> out = new ArrayList<>();
        if (!DragonFightConfig.rewardEnabled || DragonFightConfig.rewardJewelFromDragon <= 0 || data.kills() < DragonFightConfig.rewardJewelFromDragon) return out;
        for (UUID p : participants) if (!data.hasJewel(p)) out.add(p);
        return out;
    }

    /** A dragon was defeated: count it and place the rewards for the online participants around the portal. Returns the number placed. */
    public static int onDefeated(ServerLevel level, UUID dragon, BlockPos center, Collection<? extends Player> participants) {
        DragonKills data = DragonKills.get(level.getServer());
        data.defeat(dragon);
        List<UUID> ids = participants.stream().map(Player::getUUID).toList();
        Item heart = item(), jewel = jewel();
        List<UUID> dueHeart = heart == null ? List.of() : due(data, ids), dueJewel = jewel == null ? List.of() : dueJewel(data, ids);
        int total = dueHeart.size() + dueJewel.size(), n = 0;
        for (Player player : participants) {
            UUID id = player.getUUID();
            if (dueHeart.contains(id)) { place(level, center, player, heart, n++, total); data.markReward(id); }
            if (dueJewel.contains(id)) { place(level, center, player, jewel, n++, total); data.markJewel(id); }
        }
        return n;
    }

    private static void place(ServerLevel level, BlockPos center, Player player, Item item, int n, int total) {
        double a = Math.PI * 2 * n / Math.max(1, total);
        ItemEntity e = new ItemEntity(level, center.getX() + 0.5 + Math.cos(a) * RING, center.getY() + 2.0, center.getZ() + 0.5 + Math.sin(a) * RING, new ItemStack(item));
        e.setTarget(player.getUUID());
        e.setUnlimitedLifetime();
        e.setNoGravity(true);
        e.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(e);
        player.sendSystemMessage(Component.translatable("message.simpleenddragonfight.reward", new ItemStack(item).getHoverName()));
    }
}
