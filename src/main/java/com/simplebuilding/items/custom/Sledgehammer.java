package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
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

public class SledgehammerItem extends PickaxeItem {

    public SledgehammerItem(ToolMaterial material, Settings settings) {
        // Langsamerer Attack Speed (-3.2F), höherer Schaden als normale Pickaxe
        super(material, settings.attributeModifiers(PickaxeItem.createAttributeModifiers(material, 6.0F, -3.2F)));
    }

    // =============================================================
    // 1. Abbau-Geschwindigkeit (Balance)
    // =============================================================
    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        float baseSpeed = super.getMiningSpeedMultiplier(stack, state);
        
        // HINWEIS: In dieser Methode haben wir standardmäßig keinen Zugriff auf den Player,
        // um "isSneaking" zu prüfen. Für eine exakte 1/5 Logik beim Sneaken bräuchte man
        // ein Mixin in PlayerEntity. Hier setzen wir den Basis-Malus auf 1/3.
        return baseSpeed / 3.0f;
    }

    // =============================================================
    // 2. Die Abbau-Logik (postMine)
    // =============================================================
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        // Standard Abnutzung für den ersten Block
        if (!world.isClient && state.getHardness(world, pos) != 0.0F) {
            stack.damage(1, miner, EquipmentSlot.MAINHAND);
        }

        if (miner instanceof ServerPlayerEntity player && !world.isClient) {
            // Nur wenn das Werkzeug korrekt für den Block ist (z.B. Stein vs Holz)
            if (isCorrectForDrops(stack, state)) {
                breakConnectedArea(world, player, pos, state, stack);
            }
        }

        return true;
    }

    private void breakConnectedArea(World world, ServerPlayerEntity player, BlockPos centerPos, BlockState centerState, ItemStack stack) {
        // 1. Raycast: Welche Seite des Blocks schauen wir an? (Nötig für 3x3 Ausrichtung)
        Direction sideHit = raycastForSide(player, world);

        // 2. Enchantment Check: Break Through
        boolean hasBreakThrough = false;
        boolean isSneaking = player.isSneaking();

        // Enchantments abrufen (1.21 Component Style)
        ItemEnchantmentsComponent enchantments = stack.getEnchantments();
        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var breakThroughKey = enchantLookup.getOptional(ModEnchantments.BREAK_THROUGH);

        if (breakThroughKey.isPresent()) {
            // Prüfen ob das Enchantment auf dem Item ist
            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(breakThroughKey.get().getKey().get())) {
                    hasBreakThrough = true;
                    break;
                }
            }
        }

        // 3. Logik anwenden
        // Wenn Sneak + Enchant -> Tiefe 1 (also 2 Layer total), sonst Tiefe 0
        int depth = (hasBreakThrough && isSneaking) ? 1 : 0;

        // 4. Flood Fill Algorithmus starten
        Set<BlockPos> blocksToBreak = findConnectedBlocks(world, centerPos, sideHit, centerState.getBlock(), depth);

        // 5. Gefundene Blöcke abbauen
        for (BlockPos targetPos : blocksToBreak) {
            if (targetPos.equals(centerPos)) continue; // Den Hauptblock nicht nochmal abbauen

            BlockState targetState = world.getBlockState(targetPos);
            
            // Härte-Check: Wir wollen kein Bedrock abbauen, wenn wir Stone minen
            float hardnessDiff = Math.abs(targetState.getHardness(world, targetPos) - centerState.getHardness(world, centerPos));
            
            // Abbauen simulieren (Nutzt den Player, damit XP, Drops und Enchants wirken)
            if (player.interactionManager.tryBreakBlock(targetPos)) {
                 // Extra Durability Damage pro abgebautem Block (optional, hier deaktiviert für "Magic Feel" oder aktiviert für Balance)
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

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            result.add(current);

            // Alle 6 Nachbarn prüfen
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);

                if (visited.contains(neighbor)) continue;

                // A. Ist der Nachbar innerhalb der geometrischen Box (3x3 bzw 3x3x2)?
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
     * Prüft, ob ein Block (check) relativ zum Zentrum (center) innerhalb der erlaubten Mining-Box liegt.
     * Die Box orientiert sich an der Schlag-Richtung (sideHit).
     */
    private boolean isInBox(BlockPos center, BlockPos check, Direction sideHit, int extraDepth) {
        int xDiff = check.getX() - center.getX();
        int yDiff = check.getY() - center.getY();
        int zDiff = check.getZ() - center.getZ();

        // Logik:
        // Radius 1 in den Achsen senkrecht zum Schlag (für 3x3)
        // Tiefe in Schlagrichtung (0 bis extraDepth)

        switch (sideHit) {
            case UP:   // Schlag von Oben -> Wir graben nach UNTEN (negatives Y)
                if (Math.abs(xDiff) > 1 || Math.abs(zDiff) > 1) return false;
                if (yDiff > 0 || yDiff < -extraDepth) return false; 
                return true;
                
            case DOWN: // Schlag von Unten -> Wir graben nach OBEN (positives Y)
                if (Math.abs(xDiff) > 1 || Math.abs(zDiff) > 1) return false;
                if (yDiff < 0 || yDiff > extraDepth) return false;
                return true;

            case NORTH: // Schlag auf Nord-Seite -> Wir graben nach SÜDEN (+Z)
                if (Math.abs(xDiff) > 1 || Math.abs(yDiff) > 1) return false;
                if (zDiff < 0 || zDiff > extraDepth) return false;
                return true;

            case SOUTH: // Schlag auf Süd-Seite -> Wir graben nach NORDEN (-Z)
                if (Math.abs(xDiff) > 1 || Math.abs(yDiff) > 1) return false;
                if (zDiff > 0 || zDiff < -extraDepth) return false;
                return true;

            case WEST: // Schlag auf West-Seite -> Wir graben nach OSTEN (+X)
                if (Math.abs(yDiff) > 1 || Math.abs(zDiff) > 1) return false;
                if (xDiff < 0 || xDiff > extraDepth) return false;
                return true;

            case EAST: // Schlag auf Ost-Seite -> Wir graben nach WESTEN (-X)
                if (Math.abs(yDiff) > 1 || Math.abs(zDiff) > 1) return false;
                if (xDiff > 0 || xDiff < -extraDepth) return false;
                return true;
        }
        return false;
    }

    // =============================================================
    // Helper: Raycast um die getroffene Seite zu finden
    // =============================================================
    private Direction raycastForSide(PlayerEntity player, World world) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d rotation = player.getRotationVec(1.0F);
        // Reichweite 5 Blöcke
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
        return Direction.UP; // Fallback
    }
}
