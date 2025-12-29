package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SledgehammerItem extends Item {

    public static final int STONE_ATTACK_DAMAGE = 4;
    public static final int COPPER_ATTACK_DAMAGE = 4;
    public static final int IRON_ATTACK_DAMAGE = 6;
    public static final int GOLD_ATTACK_DAMAGE = 5;
    public static final int DIAMOND_ATTACK_DAMAGE = 7;
    public static final int NETHERITE_ATTACK_DAMAGE = 8;

    public static final float ATTACK_SPEED_OFFSET = 0.4f;

    public static final float STONE_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float COPPER_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float IRON_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float GOLD_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float DIAMOND_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float NETHERITE_ATTACK_SPEED = -2.6f - ATTACK_SPEED_OFFSET;

    public static final int DURABILITY_MULTIPLIER_SLEDGEHAMMER = 4;

    public static final int DURABILITY_STONE_SLEDGEHAMMER = 190 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;
    public static final int DURABILITY_COPPER_SLEDGEHAMMER = 190 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;
    public static final int DURABILITY_IRON_SLEDGEHAMMER = 250 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;
    public static final int DURABILITY_GOLD_SLEDGEHAMMER = 32 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;
    public static final int DURABILITY_DIAMOND_SLEDGEHAMMER = 1561 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;
    public static final int DURABILITY_NETHERITE_SLEDGEHAMMER = 2031 * DURABILITY_MULTIPLIER_SLEDGEHAMMER;

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed));
    }

    // =============================================================
    // 1. Abbau-Geschwindigkeit (Logik angepasst)
    // =============================================================
    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getMiningSpeed(stack, state);
        if (baseSpeed > 1.0F) {
            ItemEnchantmentsComponent enchantments = stack.getEnchantments();

            boolean hasRadius = false;
            boolean hasBreakThrough = false;

            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(ModEnchantments.RADIUS)) {hasRadius = true;}
                if (entry.getKey().matchesKey(ModEnchantments.BREAK_THROUGH)) {hasBreakThrough = true;}
            }

            float divisor = 6.0F; // Default: 3x3 (9 Blöcke) -> 1/6 Speed
            if (hasRadius && hasBreakThrough) { divisor = 33.0F; /* 5x5x2 = 50 Blöcke (Berechnung: 50 * 2/3 = 33.3) */
            } else if (hasRadius) { divisor = 16.0F; /* 5x5 = 25 Blöcke -> 16.0F (wie gewünscht) */
            } else if (hasBreakThrough) { divisor = 11.0F; /* 3x3x2 = 18 Blöcke -> 11.0F (wie gewünscht) */ }

            return baseSpeed / divisor;
        }
        return baseSpeed;
    }

    // --- Core Logic ---
    public static List<BlockPos> getBlocksToBeDestroyed(int baseRange, BlockPos initialPos, PlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>();
        World world = player.getEntityWorld();
        ItemStack stack = player.getMainHandStack();
        BlockState initialState = world.getBlockState(initialPos);

        if (!stack.getItem().isCorrectForDrops(stack, initialState)) {return positions;}

        Direction sideHit = getHitSideFromPlayer(player);

        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);

        var radiusKey = enchantLookup.getOptional(ModEnchantments.RADIUS);
        int range = baseRange;
        if (radiusKey.isPresent() && EnchantmentHelper.getLevel(radiusKey.get(), stack) > 0) {range += 1;}

        var breakThroughKey = enchantLookup.getOptional(ModEnchantments.BREAK_THROUGH);
        int depth = 0;
        if (breakThroughKey.isPresent() && EnchantmentHelper.getLevel(breakThroughKey.get(), stack) > 0) {depth = 1;}

        // Positionen berechnen
        for(int x = -range; x <= range; x++) {
            for(int y = -range; y <= range; y++) {
                for(int z = 0; z <= depth; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        positions.add(initialPos);
                        continue;
                    }

                    BlockPos targetPos = null;

                    if (sideHit == Direction.DOWN || sideHit == Direction.UP) {
                        int depthOffset = (sideHit == Direction.UP) ? -z : z;
                        targetPos = initialPos.add(x, depthOffset, y);
                    }
                    else if (sideHit == Direction.NORTH || sideHit == Direction.SOUTH) {
                        int depthOffset = (sideHit == Direction.NORTH) ? z : -z;
                        targetPos = initialPos.add(x, y, depthOffset);
                    }
                    else if (sideHit == Direction.EAST || sideHit == Direction.WEST) {
                        int depthOffset = (sideHit == Direction.WEST) ? z : -z;
                        targetPos = initialPos.add(depthOffset, y, x);
                    }

                    if (targetPos != null) {
                        positions.add(targetPos);
                    }
                }
            }
        }
        return positions;
    }

    private static Direction getHitSideFromPlayer(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch < -60) return Direction.DOWN;
        if (pitch > 60) return Direction.UP;
        return player.getHorizontalFacing().getOpposite();
    }
}