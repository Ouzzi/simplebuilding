package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
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

    public static final int BASE_DURABILITY_MULTIPLIER = 4;
    public static final int DURABILITY_STONE_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_COPPER_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_IRON_SLEDGEHAMMER = 250 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_GOLD_SLEDGEHAMMER = 32 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_DIAMOND_SLEDGEHAMMER = 1561 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_NETHERITE_SLEDGEHAMMER = 2031 * BASE_DURABILITY_MULTIPLIER;

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, int durability, Settings settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed).maxDamage(durability));
    }

    // =============================================================
    // 1. Abbau-Geschwindigkeit
    // =============================================================
    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getMiningSpeed(stack, state);

        // Wenn das Werkzeug effektiv ist, wenden wir nur den "Massen-Bonus" an,
        // teilen aber NICHT durch die Anzahl der Blöcke. Das macht jetzt das PlayerEntityMixin.
        // Dadurch bleibt der Basis-Wert hoch genug (> 1.0), damit Minecraft den Efficiency-Zauber anwendet.
        if (baseSpeed > 1.0F) {
            int blockCount = getBlockCountForSpeed(stack);

            // Speed Buff Logik (125% - 200%) um das Gefühl von Wucht zu geben
            float cappedCount = Math.min(blockCount, 25);
            float speedMultiplier = 1.25F + ((cappedCount - 1) / 24.0F) * 0.75F;

            return baseSpeed * speedMultiplier;
        }
        return baseSpeed;
    }

    // Hilfsmethode für das Mixin, um zu wissen durch wie viel geteilt werden muss
    public static int getBlockCountForSpeed(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 9; // Standard 3x3

        boolean hasRadius = false;
        boolean hasBreakThrough = false;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(ModEnchantments.RADIUS)) {hasRadius = true;}
            if (entry.getKey().matchesKey(ModEnchantments.BREAK_THROUGH)) {hasBreakThrough = true;}
        }

        int blockCount = 9; // Standard 3x3
        if (hasRadius) {
            blockCount = 25; // 5x5
        }
        if (hasBreakThrough) {
            blockCount *= 2; // Doppelte Tiefe
        }
        return blockCount;
    }

    // --- Core Logic ---
    public static List<BlockPos> getBlocksToBeDestroyed(int baseRange, BlockPos initialPos, PlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>();
        World world = player.getEntityWorld();
        ItemStack stack = player.getMainHandStack();
        BlockState initialState = world.getBlockState(initialPos);

        if (!stack.getItem().isCorrectForDrops(stack, initialState)) {return positions;}

        boolean isPlayerSneaking = player.isSneaking();
        Direction sideHit = getHitSideFromPlayer(player);

        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);

        var radiusKey = enchantLookup.getOptional(ModEnchantments.RADIUS);
        int range = baseRange + ((!isPlayerSneaking && radiusKey.isPresent()) ? EnchantmentHelper.getLevel(radiusKey.get(), stack) : 0);

        var breakThroughKey = enchantLookup.getOptional(ModEnchantments.BREAK_THROUGH);
        int depth = 1 + ((!isPlayerSneaking && breakThroughKey.isPresent()) ? EnchantmentHelper.getLevel(breakThroughKey.get(), stack) : 0);

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