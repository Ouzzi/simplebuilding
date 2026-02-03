package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel;
import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class OreDetectorItem extends Item {

    // Balancing: "Energie Budget" für die Suche.
    private static final double BUDGET_COMMON = 24.0;    // Eisen, Kupfer
    private static final double BUDGET_MEDIUM = 18.0;    // Gold, Redstone
    private static final double BUDGET_RARE = 16.0;      // Diamant
    private static final double BUDGET_VERY_RARE = 10.0; // Netherite

    private static final int SCAN_INTERVAL = 20;   // Ping alle 1 Sekunde

    private enum DetectMode {
        IRON(Formatting.GRAY, "Iron", BlockTags.IRON_ORES, BUDGET_COMMON),
        GOLD(Formatting.GOLD, "Gold", BlockTags.GOLD_ORES, BUDGET_MEDIUM),
        DIAMOND(Formatting.AQUA, "Diamond", BlockTags.DIAMOND_ORES, BUDGET_RARE),
        NETHERITE(Formatting.DARK_PURPLE, "Netherite", null, BUDGET_VERY_RARE),
        ALL(Formatting.WHITE, "All Ores", null, BUDGET_COMMON),
        CUSTOM(Formatting.YELLOW, "Custom", null, BUDGET_COMMON);

        final Formatting color;
        final String name;
        final TagKey<Block> tag;
        final double budget;

        DetectMode(Formatting color, String name, TagKey<Block> tag, double budget) {
            this.color = color;
            this.name = name;
            this.tag = tag;
            this.budget = budget;
        }
    }

    public OreDetectorItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(1024));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof PlayerEntity player)) return;

        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!isHeld) return;

        if (world.getTime() % SCAN_INTERVAL != 0) return;

        DetectMode mode = getMode(stack);

        BlockPos playerPos = BlockPos.ofFloored(player.getEyePos());

        double costMultiplier = 2.0;
        boolean hasConstructorsTouch = hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);

        if (hasConstructorsTouch) {
            costMultiplier = 1.0;
        }

        // 2. Suche
        BlockPos targetPos = findNearestOreWithRaycast(world, player.getEyePos(), mode, stack, costMultiplier);

        if (targetPos != null) {
            BlockState targetState = world.getBlockState(targetPos);
            double distance = Math.sqrt(playerPos.getSquaredDistance(targetPos));

            float pitch = (float) (1.8f - (distance / 32.0f));
            pitch = Math.max(0.6f, Math.min(2.0f, pitch));

            world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.9f, pitch);
            world.playSound(null, targetPos, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.BLOCKS, 0.6f, 2.0f);
            SoundEvent blockSound = targetState.getSoundGroup().getBreakSound();
            world.playSound(null, targetPos, blockSound, SoundCategory.BLOCKS, 0.55f, pitch);

            spawnSonarBeam(world, player.getEyePos(), targetPos, targetState);
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            if (!world.isClient()) {
                ItemStack stack = user.getStackInHand(hand);
                cycleMode(stack, user);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getPlayer().isSneaking()) {
            World world = context.getWorld();
            if (!world.isClient()) {
                ItemStack stack = context.getStack();
                BlockState state = world.getBlockState(context.getBlockPos());

                setMode(stack, DetectMode.CUSTOM);
                setCustomBlock(stack, state);

                context.getPlayer().sendMessage(Text.literal("Calibrated to: ").formatted(Formatting.GREEN)
                        .append(state.getBlock().getName().copy().formatted(Formatting.WHITE)), true);

                world.playSound(null, context.getBlockPos(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock(context);
    }

    private BlockPos findNearestOreWithRaycast(World world, Vec3d eyesPos, DetectMode mode, ItemStack stack, double costMultiplier) {
        BlockPos origin = BlockPos.ofFloored(eyesPos);
        BlockState customTarget = (mode == DetectMode.CUSTOM) ? getCustomBlock(stack, world.getRegistryManager()) : null;

        int scanRadius = (int) Math.ceil(mode.budget);
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = origin.add(x, y, z);

                    if (origin.getSquaredDistance(checkPos) > scanRadius * scanRadius) continue;

                    BlockState state = world.getBlockState(checkPos);
                    if (isTarget(state, mode, customTarget)) {
                        candidates.add(checkPos);
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));

        for (BlockPos candidate : candidates) {
            if (canReach(world, eyesPos, candidate, mode.budget, costMultiplier)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean canReach(World world, Vec3d start, BlockPos target, double budget, double costMultiplier) {
        Vec3d end = target.toCenterPos();
        Vec3d vector = end.subtract(start);
        double distance = vector.length();
        Vec3d direction = vector.normalize();

        double accumulatedCost = 0;
        double stepSize = 0.5;

        for (double d = 0; d < distance; d += stepSize) {
            Vec3d currentPos = start.add(direction.multiply(d));
            BlockPos bPos = BlockPos.ofFloored(currentPos);

            if (bPos.equals(target)) break;

            BlockState state = world.getBlockState(bPos);

            double blockDensity = getBlockDensity(state);

            accumulatedCost += (stepSize * blockDensity * costMultiplier);

            if (accumulatedCost > budget) return false;
        }

        return accumulatedCost <= budget;
    }

    private double getBlockDensity(BlockState state) {
        if (state.isAir() || !state.isOpaqueFullCube()) {
            return 1.0;
        }

        if (state.isOf(Blocks.NETHERRACK)) {
            return 1.5;
        }

        if (state.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.isOf(Blocks.BASALT)
                || state.isOf(Blocks.POLISHED_BASALT)
                || state.isOf(Blocks.BLACKSTONE)) {
            return 6.0;
        }

        if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
            return 3.0;
        }

        return 3.0;
    }

    private boolean isTarget(BlockState state, DetectMode mode, BlockState customTarget) {
        return switch (mode) {
            case IRON -> state.isIn(BlockTags.IRON_ORES);
            case GOLD -> state.isIn(BlockTags.GOLD_ORES);
            case DIAMOND -> state.isIn(BlockTags.DIAMOND_ORES);
            case NETHERITE -> state.isOf(Blocks.ANCIENT_DEBRIS);
            case ALL -> state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) ||
                    state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.GOLD_ORES) ||
                    state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES) ||
                    state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES) ||
                    state.isOf(Blocks.ANCIENT_DEBRIS) || state.isOf(Blocks.NETHER_QUARTZ_ORE);
            case CUSTOM -> customTarget != null && state.isOf(customTarget.getBlock());
        };
    }

    private void spawnSonarBeam(ServerWorld world, Vec3d startPos, BlockPos endPos, BlockState targetState) {
        Vec3d targetCenter = endPos.toCenterPos();
        Vec3d direction = targetCenter.subtract(startPos).normalize();
        double distance = startPos.distanceTo(targetCenter);

        double stepSize = 0.4;
        for (double d = 0.5; d < distance; d += stepSize) {
            Vec3d p = startPos.add(direction.multiply(d));
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, targetState),
                    p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.spawnParticles(ParticleTypes.END_ROD, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0.1, 0.1, 0.1, 0.02);
        world.spawnParticles(ParticleTypes.WAX_ON, targetCenter.x, targetCenter.y, targetCenter.z, 5, 0.3, 0.3, 0.3, 0.05);

    }

    private void cycleMode(ItemStack stack, PlayerEntity player) {
        DetectMode current = getMode(stack);
        DetectMode[] modes = DetectMode.values();
        DetectMode next = modes[(current.ordinal() + 1) % modes.length];
        setMode(stack, next);

        player.sendMessage(Text.literal("Detector Mode: ").formatted(Formatting.GRAY)
                .append(Text.literal(next.name).formatted(next.color)), true);

        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);

        if (!player.isCreative()) {
            stack.damage(1, player, EquipmentSlot.MAINHAND);
        }
    }

    private DetectMode getMode(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        int modeIndex = 0;

        if (nbt.contains("Mode")) {
            modeIndex = nbt.getInt("Mode", 0);
        }

        modeIndex = Math.max(0, Math.min(DetectMode.values().length - 1, modeIndex));

        return DetectMode.values()[modeIndex];
    }

    private void setMode(ItemStack stack, DetectMode mode) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt("Mode", mode.ordinal()));
    }

    private void setCustomBlock(ItemStack stack, BlockState state) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt ->
                nbt.put("CustomBlock", NbtHelper.fromBlockState(state))
        );
    }

    private BlockState getCustomBlock(ItemStack stack, RegistryWrapper.WrapperLookup registryLookup) {
    if (registryLookup == null) return null;
    NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    if (nbt.contains("CustomBlock")) {
        var blockRegistry = registryLookup.getOrThrow(RegistryKeys.BLOCK);
        try {
            return NbtHelper.toBlockState(blockRegistry, nbt.getCompoundOrEmpty("CustomBlock"));
        } catch (Exception e) {
            return null;
        }
    }
    return null;
}

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        DetectMode mode = getMode(stack);
        textConsumer.accept(Text.literal("Mode: ").formatted(Formatting.GRAY)
                .append(Text.literal(mode.name).formatted(mode.color)));

        if (mode == DetectMode.CUSTOM) {
            BlockState custom = getCustomBlock(stack, context.getRegistryLookup());
            if (custom != null) {
                textConsumer.accept(Text.literal("Target: ").formatted(Formatting.GRAY)
                        .append(custom.getBlock().getName().copy().formatted(Formatting.GREEN)));
            } else {
                textConsumer.accept(Text.literal("Target: None (Sneak-Use on block)").formatted(Formatting.RED));
            }
        } else {
            textConsumer.accept(Text.literal("Sneak + Use to cycle modes").formatted(Formatting.DARK_GRAY));
        }

        textConsumer.accept(Text.empty());
        textConsumer.accept(Text.literal("Power: " + (int)mode.budget).formatted(Formatting.DARK_AQUA));
        textConsumer.accept(Text.literal("Penetrates dense blocks slower.").formatted(Formatting.GRAY));
    }
}