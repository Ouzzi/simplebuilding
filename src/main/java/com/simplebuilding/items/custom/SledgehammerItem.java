package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SledgehammerItem extends Item {

    public static final int STONE_ATTACK_DAMAGE = 3;
    public static final int COPPER_ATTACK_DAMAGE = 4;
    public static final int IRON_ATTACK_DAMAGE = 5;
    public static final int GOLD_ATTACK_DAMAGE = 4;
    public static final int DIAMOND_ATTACK_DAMAGE = 6;
    public static final int NETHERITE_ATTACK_DAMAGE = 7;

    public static final float STONE_ATTACK_SPEED = -4.0f;
    public static final float COPPER_ATTACK_SPEED = -3.8f;
    public static final float IRON_ATTACK_SPEED = -3.6f;
    public static final float GOLD_ATTACK_SPEED = -3.4f;
    public static final float DIAMOND_ATTACK_SPEED = -3.2f;
    public static final float NETHERITE_ATTACK_SPEED = -2.0f;

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed));
    }

    // =============================================================
    // 1. Abbau-Geschwindigkeit // TODO
    // =============================================================


    // =============================================================
    // 2. Die Abbau-Logik (postMine)
    // =============================================================
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        // Standard Abnutzung für den Hauptblock
        if (!world.isClient() && state.getHardness(world, pos) != 0.0F) {
            stack.damage(1, miner, EquipmentSlot.MAINHAND);
        }

        if (miner instanceof ServerPlayerEntity player && !world.isClient()) {
            // Nur wenn das Werkzeug korrekt ist (z.B. Stein vs Holz)
            if (isCorrectForDrops(stack, state)) {
                breakConnectedArea(world, player, pos, state, stack);
            }
        }

        return true;
    }

    private void breakConnectedArea(World world, ServerPlayerEntity player, BlockPos centerPos, BlockState centerState, ItemStack stack) {
        // 1. Raycast: Welche Seite des Blocks schauen wir an? (Nötig für 3x3 Ausrichtung)
        Direction sideHit = raycastForSide(player, world);

        // 2. Enchantment & Sneak Check
        boolean hasBreakThrough = false;
        boolean isSneaking = player.isSneaking();

        // Enchantments sicher abrufen (1.21 Style)
        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var breakThroughKey = enchantLookup.getOptional(ModEnchantments.BREAK_THROUGH);

        if (breakThroughKey.isPresent()) {
            ItemEnchantmentsComponent enchantments = stack.getEnchantments();
            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(breakThroughKey.get().getKey().get())) {
                    hasBreakThrough = true;
                    break;
                }
            }
        }

        // 3. Tiefe berechnen
        // Wenn Sneak + Enchant -> Tiefe 1 (dieser Block + 1 dahinter), sonst 0
        int depth = (hasBreakThrough && isSneaking) ? 1 : 0;

        // 4. Flood Fill Algorithmus starten
        Set<BlockPos> blocksToBreak = findConnectedBlocks(world, centerPos, sideHit, centerState.getBlock(), depth);

        // 5. Gefundene Blöcke abbauen
        for (BlockPos targetPos : blocksToBreak) {
            if (targetPos.equals(centerPos)) continue; // Den Hauptblock nicht nochmal abbauen

            BlockState targetState = world.getBlockState(targetPos);
            
            // Sicherheits-Check: Keine Blöcke mit völlig anderer Härte (z.B. Bedrock)
            if (targetState.getHardness(world, targetPos) < 0) continue; 

            // tryBreakBlock nutzt den Spieler für XP, Drops, Fortune, etc.
            if (player.interactionManager.tryBreakBlock(targetPos)) {
                 // Optional: Extra Haltbarkeit pro Block abziehen?
                 // stack.damage(1, player, EquipmentSlot.MAINHAND); 
            }
        }
    }

    // =============================================================
    // 3. Flood Fill Algorithmus (Nur angrenzende gleiche Blöcke)
    // =============================================================
    private Set<BlockPos> findConnectedBlocks(World world, BlockPos center, Direction sideHit, Block targetBlock, int extraDepth) {
        Set<BlockPos> result = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(center);
        visited.add(center);

        // Wir definieren die erlaubte Box relativ zum Zentrum
        // Aber der Flood-Fill stellt sicher, dass wir nur verbundene Blöcke finden.

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            result.add(current);

            // Alle 6 Nachbarn prüfen
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);

                if (visited.contains(neighbor)) continue;

                // A. Ist der Nachbar geometrisch innerhalb der erlaubten Mining-Box?
                if (!isInBox(center, neighbor, sideHit, extraDepth)) continue;

                // B. Ist es der gleiche Block?
                BlockState neighborState = world.getBlockState(neighbor);
                if (neighborState.getBlock() == targetBlock) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    /**
     * Prüft, ob ein Block (check) relativ zum Zentrum (center) innerhalb der erlaubten 3x3 Box liegt.
     * Die Orientierung der Box hängt davon ab, welche Seite (sideHit) geschlagen wurde.
     */
    private boolean isInBox(BlockPos center, BlockPos check, Direction sideHit, int extraDepth) {
        int xDiff = check.getX() - center.getX();
        int yDiff = check.getY() - center.getY();
        int zDiff = check.getZ() - center.getZ();

        // Logik:
        // Radius 1 in den Achsen senkrecht zum Schlag (für 3x3)
        // Tiefe in Schlagrichtung (0 bis extraDepth)

        switch (sideHit) {
            case UP:   // Schlag von Oben -> Tiefe geht ins Negative Y
                if (Math.abs(xDiff) > 1 || Math.abs(zDiff) > 1) return false;
                // yDiff muss zwischen 0 und -depth liegen (z.B. 0 und -1)
                return yDiff <= 0 && yDiff >= -extraDepth;
                
            case DOWN: // Schlag von Unten -> Tiefe geht ins Positive Y
                if (Math.abs(xDiff) > 1 || Math.abs(zDiff) > 1) return false;
                return yDiff >= 0 && yDiff <= extraDepth;

            case NORTH: // Schlag auf Nord-Seite (-Z) -> Tiefe geht ins Positive Z (in den Block rein)
                if (Math.abs(xDiff) > 1 || Math.abs(yDiff) > 1) return false;
                return zDiff >= 0 && zDiff <= extraDepth;

            case SOUTH: // Schlag auf Süd-Seite (+Z) -> Tiefe geht ins Negative Z
                if (Math.abs(xDiff) > 1 || Math.abs(yDiff) > 1) return false;
                return zDiff <= 0 && zDiff >= -extraDepth;

            case WEST: // Schlag auf West-Seite (-X) -> Tiefe geht ins Positive X
                if (Math.abs(yDiff) > 1 || Math.abs(zDiff) > 1) return false;
                return xDiff >= 0 && xDiff <= extraDepth;

            case EAST: // Schlag auf Ost-Seite (+X) -> Tiefe geht ins Negative X
                if (Math.abs(yDiff) > 1 || Math.abs(zDiff) > 1) return false;
                return xDiff <= 0 && xDiff >= -extraDepth;
        }
        return false;
    }

    // =============================================================
    // Helper: Raycast
    // =============================================================
    private Direction raycastForSide(PlayerEntity player, World world) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d rotation = player.getRotationVec(1.0F);
        Vec3d end = start.add(rotation.x * 5.0, rotation.y * 5.0, rotation.z * 5.0);
        
        BlockHitResult result = world.raycast(new RaycastContext(
                start, end, 
                RaycastContext.ShapeType.OUTLINE, 
                RaycastContext.FluidHandling.NONE, 
                player
        ));

        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getSide();
        }
        return Direction.UP;
    }
}
