package com.simplebuilding.client;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.util.EnchantmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Shared client-side air-jump (double jump) logic with a level-dependent cooldown.
 *
 * <p>Called once per client tick by each loader (Fabric + NeoForge) so the logic
 * is not duplicated. After an air-jump the ability goes on cooldown and cannot be
 * used again until it recharges — the cooldown keeps running even after landing,
 * which prevents spamming the air-jump to repeatedly cancel fall damage.
 *
 * <p>The cooldown length scales with the DOUBLE_JUMP enchantment level: level 1 gets
 * the full {@code airJumpCooldownTicks}, level 2+ gets half (still notable). The
 * remaining cooldown is exposed for the HUD ({@link com.simplebuilding.client.gui.DoubleJumpHudOverlay}).
 */
public final class DoubleJumpController {
    private static boolean jumpKeyPressed = false;
    private static boolean wasOnGround = true;
    private static int cooldownRemaining = 0; // ticks until the air-jump recharges
    private static int cooldownMax = 0;        // cooldown of the most recent air-jump (for the HUD bar)

    private DoubleJumpController() {
    }

    /** Run the air-jump + cooldown logic for this client tick. Safe to call every tick. */
    public static void tick(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return;
        }
        if (!Simplebuilding.getConfig().enableDoubleJump) {
            jumpKeyPressed = false;
            cooldownRemaining = 0;
            cooldownMax = 0;
            return;
        }

        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }

        boolean grounded = player.onGround() || player.onClimbable() || player.isInWater();
        boolean jumping = client.options.keyJump.isDown();

        if (!grounded && jumping && !jumpKeyPressed && !wasOnGround) {
            int level = getDoubleJumpLevel(player);
            if (level > 0 && cooldownRemaining <= 0 && !player.getAbilities().flying) {
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(velocity.x, 0.5, velocity.z);
                player.fallDistance = 0;
                ClientNetworking.send(new DoubleJumpPayload());
                cooldownMax = cooldownTicksForLevel(level);
                cooldownRemaining = cooldownMax;
            }
        }

        jumpKeyPressed = jumping;
        wasOnGround = grounded;
    }

    /** Level 1 = full cooldown; level 2+ = half (still notable). */
    private static int cooldownTicksForLevel(int level) {
        int base = Math.max(0, Simplebuilding.getConfig().airJumpCooldownTicks);
        if (level >= 2) {
            return Math.max(1, base / 2);
        }
        return base;
    }

    public static int getDoubleJumpLevel(Player player) {
        int maxLevel = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = player.getItemBySlot(slot);
            int level = EnchantmentHelper.getEnchantmentLevel(equipped, player.level(), ModEnchantments.DOUBLE_JUMP);
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }

    public static boolean isOnCooldown() {
        return cooldownRemaining > 0;
    }

    public static int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public static int getCooldownMax() {
        return cooldownMax;
    }
}
