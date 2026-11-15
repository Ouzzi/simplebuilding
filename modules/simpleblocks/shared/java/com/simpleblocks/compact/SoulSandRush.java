package com.simpleblocks.compact;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;

/**
 * N57-2 (owner 14.11.): Dense and Collapsed soul sand slow more than soul sand ({@link CompactBlocks#soulSandSpeed}); with Soul
 * Speed boots they are faster than soul sand by the same steps. All compact soul sands are in {@code #minecraft:soul_speed_blocks},
 * so Vanilla's Soul Speed already lifts their slowdown and gives its own bonus (0.0405 + 0.0105 per level above the first, from
 * Vanilla's {@code soul_speed.json}); on top of that a player on Dense soul sand gets half that bonus again, on Collapsed the whole
 * bonus again (1.5x / 2x Vanilla Soul Speed) as a transient movement speed modifier, set and removed every {@link #INTERVAL}
 * ticks. Vanilla soul sand is not touched (A12: only own blocks; weak). Switch: {@link CollapsedConfig#soulSandRush}.
 */
public final class SoulSandRush {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("simpleblocks", "dense_soul_speed");
    public static final int INTERVAL = 4;
    /** Extra share of the Vanilla Soul Speed bonus on Dense / Collapsed soul sand. */
    public static final double DENSE_EXTRA = 0.5, COLLAPSED_EXTRA = 1.0;

    private SoulSandRush() {}

    /** Vanilla's Soul Speed movement bonus at {@code level} (0 for none). */
    public static double vanillaBonus(int level) {
        return level <= 0 ? 0.0 : 0.0405 + 0.0105 * (level - 1);
    }

    /** The extra share for the block, 0 for every block but Dense and Collapsed soul sand. */
    public static double extraShare(Block block) {
        if (block == CompactBlocks.BLOCKS.get("dense_soul_sand")) return DENSE_EXTRA;
        if (block == CompactBlocks.BLOCKS.get("collapsed_soul_sand")) return COLLAPSED_EXTRA;
        return 0.0;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) apply(player);
    }

    /** Sets or removes the modifier for where the player stands now. */
    public static void apply(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = amount(player);
        if (amount <= 0.0) {
            speed.removeModifier(ID);
            return;
        }
        AttributeModifier now = speed.getModifier(ID);
        if (now == null || now.amount() != amount) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** The modifier the player should have (0: none). */
    public static double amount(ServerPlayer player) {
        if (!CollapsedConfig.soulSandRush || player.isSpectator() || player.getAbilities().flying || player.isPassenger() || !player.onGround()) return 0.0;
        double share = extraShare(player.level().getBlockState(player.getOnPos()).getBlock());
        if (share <= 0.0) return 0.0;
        var soulSpeed = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SOUL_SPEED);
        if (soulSpeed.isEmpty()) return 0.0;
        int level = EnchantmentHelper.getItemEnchantmentLevel(soulSpeed.get(), player.getItemBySlot(EquipmentSlot.FEET));
        return vanillaBonus(level) * share;
    }
}
