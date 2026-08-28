package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.simplebuilding.util.EnchantmentHelper.getOverrideLevel;
import static com.simplebuilding.util.EnchantmentHelper.hasConstructorsTouch;

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

    public SledgehammerItem(ToolMaterial material, float attackDamage, float attackSpeed, int durability, Properties settings) {
        super(settings.pickaxe(material, attackDamage, attackSpeed).durability(durability));
        this.material = material;
    }

    public ToolMaterial getMaterial() {
        return this.material;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Standard-Verhalten (Pickaxe)
        if (super.isCorrectToolForDrops(stack, state)) {
            return true;
        }

        // Wenn Override Level >= 2, erlaube auch Axt, Schaufel und Hacke
        if (getOverrideLevel(stack) >= 2) {
            if (state.is(BlockTags.MINEABLE_WITH_AXE) ||
                state.is(BlockTags.MINEABLE_WITH_SHOVEL) ||
                state.is(BlockTags.MINEABLE_WITH_HOE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);

        // 2. NEU: Wenn Speed langsam ist (1.0f), aber Override II aktiv ist -> Setze vollen Speed
        if (baseSpeed <= 1.0F && getOverrideLevel(stack) >= 2) {
             if (state.is(BlockTags.MINEABLE_WITH_AXE) ||
                 state.is(BlockTags.MINEABLE_WITH_SHOVEL) ||
                 state.is(BlockTags.MINEABLE_WITH_HOE)) {
                 // Setze die Geschwindigkeit auf die des Materials (z.B. Diamant-Speed)
                 baseSpeed = this.material.speed();
            }
        }

        // 3. Deine existierende Multiplier-Logik (unverändert, nutzt jetzt aber den korrigierten baseSpeed)
        if (baseSpeed > 1.0F) {
            int blockCount = getBlockCountForSpeed(stack);
            float cappedCount = Math.min(blockCount, 25);
            float speedMultiplier = 1.25F + ((cappedCount - 1) / 24.0F) * 0.6F;
            return baseSpeed * speedMultiplier;
        }
        return baseSpeed;
    }

    public static int getBlockCountForSpeed(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 9;

        boolean hasRadius = false;
        boolean hasBreakThrough = false;

        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(ModEnchantments.RADIUS)) {hasRadius = true;}
            if (entry.getKey().is(ModEnchantments.BREAK_THROUGH)) {hasBreakThrough = true;}
        }
        int blockCount = 9;
        if (hasRadius) blockCount = 25;
        if (hasBreakThrough) blockCount *= 2;
        return blockCount;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        BlockState state = world.getBlockState(pos);

        if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }

        // Relativer Hit Vector
        Vec3 relativeHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));

        // FIX: pos übergeben
        BlockState transformState = getTransformationState(state, pos, context.getClickedFace(), relativeHit, player, stack);

        if (transformState != null) {
            if (player != null) {
                player.startUsingItem(context.getHand());
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        return false; // Nichts tun, wenn vorzeitig abgebrochen
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!(user instanceof Player player)) return stack;

        var hitResult = player.pick(5.0, 0.0f, false);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos pos = ((net.minecraft.world.phys.BlockHitResult)hitResult).getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction side = ((net.minecraft.world.phys.BlockHitResult)hitResult).getDirection();

            // Relativer Hit Vector berechnen
            Vec3 relativeHit = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(pos));

            // Transformation abrufen (FIX: pos übergeben)
            BlockState newState = getTransformationState(state, pos, side, relativeHit, player, stack);

            if (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)) {
                if (!world.isClientSide()) {
                    crushDiamondBlock((ServerLevel) world, pos, player, stack);
                }
                return stack;
            }

            if (newState != null) {
                if (!world.isClientSide()) {
                    world.setBlockAndUpdate(pos, newState);

                    // Sound: Verwende den Break-Sound des Blocks, klingt natürlicher
                    world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 0.8f);

                    ((ServerLevel) world).sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            20, 0.25, 0.25, 0.25, 0.05
                    );

                    if (!player.isCreative()) {
                        // Prüfen ob Reverse Action (teurer)
                        boolean isReverse = player.isShiftKeyDown() && hasConstructorsTouch(stack, world);
                        int damage = isReverse ? 2 : 1;
                        stack.hurtAndBreak(damage, player, EquipmentSlot.MAINHAND);
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        float baseTime = 20.0f;
        int efficiencyLevel = 0;
        var registry = user.registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isPresent()) {
            var efficiencyEntry = registry.get().get(Enchantments.EFFICIENCY);
             if (efficiencyEntry.isPresent()) {
                 efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEntry.get(), stack);
             }
        }

        float speed = this.getMaterial().speed();
        float factor = speed + (efficiencyLevel * 5.0f);
        int time = (int) (baseTime * 10.0f / factor);
        return Math.clamp(time, 4, 40);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    public BlockState getTransformationState(BlockState state, BlockPos pos, Direction side, Vec3 hit, Player player, ItemStack stack) {
        Block block = state.getBlock();
        Level world = player.level();

        // STRIKTE TRENNUNG:
        if (player.isShiftKeyDown()) {
            // === SNEAKING = REVERSE ===

            // 1. Voraussetzung: Constructor's Touch
            if (!hasConstructorsTouch(stack, world)) {
                return null; // Keine Reparatur ohne Enchantment -> Keine Animation
            }

            // 2. Slab -> Stairs
            if (block instanceof SlabBlock) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                String baseName = id.replace("_slab", "");
                Optional<Block> stairs = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), baseName + "_stairs")
                );
                if (stairs.isPresent()) {
                    BlockState stairState = stairs.get().defaultBlockState();
                    return ChiselItem.applyIntuitiveOrientation(stairState, side, hit, player);
                }
            }

            // 3. Stairs -> Block
            if (block instanceof StairBlock) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                String baseName = id.replace("_stairs", "");
                Optional<Block> fullBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), baseName)
                );
                // Fallbacks prüfen (plural 's' oder '_planks')
                if (fullBlock.isEmpty()) fullBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), baseName + "s"));
                if (fullBlock.isEmpty()) fullBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), baseName + "_planks"));

                if (fullBlock.isPresent()) {
                    return fullBlock.get().defaultBlockState();
                }
            }

        } else {
            // === NICHT SNEAKING = FORWARD ===

            // 1. Block -> Stairs
            // FIX: world und pos an isFullCube übergeben, statt null
            if (state.isCollisionShapeFullBlock(world, pos)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                Optional<Block> stairs = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), id + "_stairs")
                );
                if (stairs.isPresent()) {
                    BlockState stairState = stairs.get().defaultBlockState();
                    return ChiselItem.applyIntuitiveOrientation(stairState, side, hit, player);
                }
            }

            // 2. Stairs -> Slab
            if (block instanceof StairBlock) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                String baseName = id.replace("_stairs", "");
                Optional<Block> slab = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace(), baseName + "_slab")
                );
                if (slab.isPresent()) {
                    BlockState slabState = slab.get().defaultBlockState();
                    return ChiselItem.applyIntuitiveOrientation(slabState, side, hit, player);
                }
            }
        }

        return null;
    }

    public static List<BlockPos> getBlocksToBeDestroyed(int baseRange, BlockPos initialPos, Player player) {
        List<BlockPos> positions = new ArrayList<>();
        Level world = player.level();
        ItemStack stack = player.getMainHandItem();
        BlockState initialState = world.getBlockState(initialPos);

        if (!(stack.getItem() instanceof SledgehammerItem)) {
            return positions;
        }
        if (initialState.isAir() || initialState.getDestroySpeed(world, initialPos) < 0.0F) {
            return positions;
        }
        if (!SledgehammerUtils.canMineOrigin(world, initialPos, stack)) {
            return positions;
        }

        boolean isPlayerSneaking = player.isShiftKeyDown();
        Direction sideHit = getHitSideFromPlayer(player);

        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);

        var radiusKey = enchantLookup.get(ModEnchantments.RADIUS);
        int range = baseRange + ((!isPlayerSneaking && radiusKey.isPresent()) ? EnchantmentHelper.getItemEnchantmentLevel(radiusKey.get(), stack) : 0);

        var breakThroughKey = enchantLookup.get(ModEnchantments.BREAK_THROUGH);
        int depth = ((!isPlayerSneaking && breakThroughKey.isPresent()) ? EnchantmentHelper.getItemEnchantmentLevel(breakThroughKey.get(), stack) : 0);

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
                        targetPos = initialPos.offset(x, depthOffset, y);
                    }
                    else if (sideHit == Direction.NORTH || sideHit == Direction.SOUTH) {
                        int depthOffset = (sideHit == Direction.NORTH) ? z : -z;
                        targetPos = initialPos.offset(x, y, depthOffset);
                    }
                    else if (sideHit == Direction.EAST || sideHit == Direction.WEST) {
                        int depthOffset = (sideHit == Direction.WEST) ? z : -z;
                        targetPos = initialPos.offset(depthOffset, y, x);
                    }

                    if (targetPos != null) {
                        positions.add(targetPos);
                    }
                }
            }
        }
        return positions;
    }

    private static void crushDiamondBlock(ServerLevel world, BlockPos pos, Player player, ItemStack stack) {
        if (!world.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) {
            return;
        }

        world.destroyBlock(pos, false, player);

        int totalPebbles = 81;
        while (totalPebbles > 0) {
            int batch = Math.min(totalPebbles, 64);
            ItemEntity itemEntity = new ItemEntity(
                    world,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    new ItemStack(ModItems.DIAMOND_PEBBLE, batch)
            );
            world.addFreshEntity(itemEntity);
            totalPebbles -= batch;
        }

        world.playSound(null, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (!player.isCreative()) {
            stack.hurtAndBreak(1, world, (ServerPlayer) player,
                    item -> player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
        }
    }

    private static Direction getHitSideFromPlayer(Player player) {
        float pitch = player.getXRot();
        if (pitch < -60) return Direction.DOWN;
        if (pitch > 60) return Direction.UP;
        return player.getDirection().getOpposite();
    }


}