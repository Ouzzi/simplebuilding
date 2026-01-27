package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SledgehammerItem extends Item {

    public static final int STONE_ATTACK_DAMAGE = 4;
    public static final int COPPER_ATTACK_DAMAGE = 4;
    public static final int IRON_ATTACK_DAMAGE = 6;
    public static final int GOLD_ATTACK_DAMAGE = 5;
    public static final int DIAMOND_ATTACK_DAMAGE = 7;
    public static final int NETHERITE_ATTACK_DAMAGE = 8;
    public static final int ENDERITE_ATTACK_DAMAGE = 9;

    public static final float ATTACK_SPEED_OFFSET = 0.4f;

    public static final float STONE_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float COPPER_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float IRON_ATTACK_SPEED = -3.0f - ATTACK_SPEED_OFFSET;
    public static final float GOLD_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float DIAMOND_ATTACK_SPEED = -2.8f - ATTACK_SPEED_OFFSET;
    public static final float NETHERITE_ATTACK_SPEED = -2.6f - ATTACK_SPEED_OFFSET;
    public static final float ENDERITE_ATTACK_SPEED = -2.4f - ATTACK_SPEED_OFFSET;

    public static final int BASE_DURABILITY_MULTIPLIER = 4;
    public static final int DURABILITY_STONE_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_COPPER_SLEDGEHAMMER = 190 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_IRON_SLEDGEHAMMER = 250 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_GOLD_SLEDGEHAMMER = 32 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_DIAMOND_SLEDGEHAMMER = 1561 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_NETHERITE_SLEDGEHAMMER = 2031 * BASE_DURABILITY_MULTIPLIER;
    public static final int DURABILITY_ENDERITE_SLEDGEHAMMER = 2500 * BASE_DURABILITY_MULTIPLIER;

    private final ToolMaterial material;

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, int durability, Settings settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed).maxDamage(durability));
        this.material = material;
    }

    // Getter für das Material
    public ToolMaterial getMaterial() {
        return this.material;
    }

    // =============================================================
    // 1. Abbau-Geschwindigkeit
    // =============================================================
    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getMiningSpeed(stack, state);
        if (baseSpeed > 1.0F) {
            int blockCount = getBlockCountForSpeed(stack);
            float cappedCount = Math.min(blockCount, 25);
            float speedMultiplier = 1.25F + ((cappedCount - 1) / 24.0F) * 0.6F;
            return baseSpeed * speedMultiplier;
        }
        return baseSpeed;
    }

    public static int getBlockCountForSpeed(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 9;

        boolean hasRadius = false;
        boolean hasBreakThrough = false;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(ModEnchantments.RADIUS)) {hasRadius = true;}
            if (entry.getKey().matchesKey(ModEnchantments.BREAK_THROUGH)) {hasBreakThrough = true;}
        }
        int blockCount = 9;
        if (hasRadius) blockCount = 25;
        if (hasBreakThrough) blockCount *= 2;
        return blockCount;
    }

    // =============================================================
    // NEU: Transformations-Logik (Block -> Stairs -> Slab)
    // =============================================================

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        BlockState state = world.getBlockState(pos);

        // Relativer Hit Vector
        Vec3d relativeHit = context.getHitPos().subtract(Vec3d.of(pos));

        // FIX: pos übergeben
        BlockState transformState = getTransformationState(state, pos, context.getSide(), relativeHit, player, stack);

        if (transformState != null) {
            if (player != null) {
                player.setCurrentHand(context.getHand());
            }
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        return false; // Nichts tun, wenn vorzeitig abgebrochen
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!(user instanceof PlayerEntity player)) return stack;

        var hitResult = player.raycast(5.0, 0.0f, false);
        if (hitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockPos pos = ((net.minecraft.util.hit.BlockHitResult)hitResult).getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction side = ((net.minecraft.util.hit.BlockHitResult)hitResult).getSide();

            // Relativer Hit Vector berechnen
            Vec3d relativeHit = hitResult.getPos().subtract(Vec3d.of(pos));

            // Transformation abrufen (FIX: pos übergeben)
            BlockState newState = getTransformationState(state, pos, side, relativeHit, player, stack);

            if (newState != null) {
                if (!world.isClient()) {
                    world.setBlockState(pos, newState);

                    // Sound: Verwende den Break-Sound des Blocks, klingt natürlicher
                    world.playSound(null, pos, state.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 0.8f);

                    ((ServerWorld) world).spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            20, 0.25, 0.25, 0.25, 0.05
                    );

                    if (!player.isCreative()) {
                        // Prüfen ob Reverse Action (teurer)
                        boolean isReverse = player.isSneaking() && hasConstructorsTouch(stack, world);
                        int damage = isReverse ? 2 : 1;
                        stack.damage(damage, player, EquipmentSlot.MAINHAND);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        float baseTime = 20.0f;
        int efficiencyLevel = 0;
        var registry = user.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (registry.isPresent()) {
            var efficiencyEntry = registry.get().getOptional(Enchantments.EFFICIENCY);
             if (efficiencyEntry.isPresent()) {
                 efficiencyLevel = EnchantmentHelper.getLevel(efficiencyEntry.get(), stack);
             }
        }

        float speed = this.getMaterial().speed();
        float factor = speed + (efficiencyLevel * 5.0f);
        int time = (int) (baseTime * 10.0f / factor);
        return Math.clamp(time, 4, 40);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    // --- Helper: Hat das Item Constructor's Touch? ---
    private boolean hasConstructorsTouch(ItemStack stack, World world) {
        var registry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (registry.isPresent()) {
            var entry = registry.get().getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);
            if (entry.isPresent()) {
                return EnchantmentHelper.getLevel(entry.get(), stack) > 0;
            }
        }
        return false;
    }

    // WICHTIG: Diese Methode muss PUBLIC sein für das Mixin!
    // FIX: Parameter BlockPos pos hinzugefügt
    public BlockState getTransformationState(BlockState state, BlockPos pos, Direction side, Vec3d hit, PlayerEntity player, ItemStack stack) {
        Block block = state.getBlock();
        World world = player.getEntityWorld();

        if (world == null) {
            return null;
        }

        // STRIKTE TRENNUNG:
        if (player.isSneaking()) {
            // === SNEAKING = REVERSE ===

            // 1. Voraussetzung: Constructor's Touch
            if (!hasConstructorsTouch(stack, world)) {
                return null; // Keine Reparatur ohne Enchantment -> Keine Animation
            }

            // 2. Slab -> Stairs
            if (block instanceof SlabBlock) {
                String id = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
                String baseName = id.replace("_slab", "");
                Optional<Block> stairs = net.minecraft.registry.Registries.BLOCK.getOptionalValue(
                        net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), baseName + "_stairs")
                );
                if (stairs.isPresent()) {
                    BlockState stairState = stairs.get().getDefaultState();
                    return ChiselItem.applyIntuitiveOrientation(stairState, side, hit, player);
                }
            }

            // 3. Stairs -> Block
            if (block instanceof StairsBlock) {
                String id = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
                String baseName = id.replace("_stairs", "");
                Optional<Block> fullBlock = net.minecraft.registry.Registries.BLOCK.getOptionalValue(
                        net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), baseName)
                );
                // Fallbacks prüfen (plural 's' oder '_planks')
                if (fullBlock.isEmpty()) fullBlock = net.minecraft.registry.Registries.BLOCK.getOptionalValue(net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), baseName + "s"));
                if (fullBlock.isEmpty()) fullBlock = net.minecraft.registry.Registries.BLOCK.getOptionalValue(net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), baseName + "_planks"));

                if (fullBlock.isPresent()) {
                    return fullBlock.get().getDefaultState();
                }
            }

        } else {
            // === NICHT SNEAKING = FORWARD ===

            // 1. Block -> Stairs
            // FIX: world und pos an isFullCube übergeben, statt null
            if (state.isFullCube(world, pos)) {
                String id = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
                Optional<Block> stairs = net.minecraft.registry.Registries.BLOCK.getOptionalValue(
                        net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), id + "_stairs")
                );
                if (stairs.isPresent()) {
                    BlockState stairState = stairs.get().getDefaultState();
                    return ChiselItem.applyIntuitiveOrientation(stairState, side, hit, player);
                }
            }

            // 2. Stairs -> Slab
            if (block instanceof StairsBlock) {
                String id = net.minecraft.registry.Registries.BLOCK.getId(block).getPath();
                String baseName = id.replace("_stairs", "");
                Optional<Block> slab = net.minecraft.registry.Registries.BLOCK.getOptionalValue(
                        net.minecraft.util.Identifier.of(net.minecraft.registry.Registries.BLOCK.getId(block).getNamespace(), baseName + "_slab")
                );
                if (slab.isPresent()) {
                    BlockState slabState = slab.get().getDefaultState();
                    return ChiselItem.applyIntuitiveOrientation(slabState, side, hit, player);
                }
            }
        }

        return null;
    }

    // --- Core Logic (Bestehend) ---
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
        int depth = ((!isPlayerSneaking && breakThroughKey.isPresent()) ? EnchantmentHelper.getLevel(breakThroughKey.get(), stack) : 0);

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