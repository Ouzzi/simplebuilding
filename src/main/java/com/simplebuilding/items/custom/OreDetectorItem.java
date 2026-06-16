package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class OreDetectorItem extends Item {

    // Balancing: "Energie Budget" für die Suche.
    private static final double BUDGET_COMMON = 24.0;    // Eisen, Kupfer
    private static final double BUDGET_MEDIUM = 18.0;    // Gold, Redstone
    private static final double BUDGET_RARE = 16.0;      // Diamant
    private static final double BUDGET_VERY_RARE = 10.0; // Netherite

    private static final int SCAN_INTERVAL = 20;   // Ping alle 1 Sekunde

    private enum DetectMode {
        IRON(ChatFormatting.GRAY, "Iron", BUDGET_COMMON),
        GOLD(ChatFormatting.GOLD, "Gold", BUDGET_MEDIUM),
        DIAMOND(ChatFormatting.AQUA, "Diamond", BUDGET_RARE),
        NETHERITE(ChatFormatting.DARK_PURPLE, "Netherite", BUDGET_VERY_RARE),
        ALL(ChatFormatting.WHITE, "All Ores", BUDGET_COMMON),
        CUSTOM(ChatFormatting.YELLOW, "Custom", BUDGET_COMMON);

        final ChatFormatting color;
        final String name;
        final double budget;

        DetectMode(ChatFormatting color, String name, double budget) {
            this.color = color;
            this.name = name;
            this.budget = budget;
        }
    }

    public OreDetectorItem(Properties settings) {
        super(settings.stacksTo(1).durability(1024));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        if (!isHeldInHands(slot)) return;

        if (world.getGameTime() % SCAN_INTERVAL != 0) return;

        DetectMode mode = getMode(stack);

        BlockPos playerPos = BlockPos.containing(player.getEyePosition());

        double costMultiplier = hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH) ? 1.0 : 2.0;

        // 2. Suche
        BlockPos targetPos = findNearestOreWithRaycast(world, player.getEyePosition(), mode, stack, costMultiplier);

        if (targetPos != null) {
            BlockState targetState = world.getBlockState(targetPos);
            double distance = Math.sqrt(playerPos.distSqr(targetPos));
            float pitch = getPingPitch(distance);

            world.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, pitch);
            world.playSound(null, targetPos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.6f, 2.0f);
            SoundEvent blockSound = targetState.getSoundType().getBreakSound();
            world.playSound(null, targetPos, blockSound, SoundSource.BLOCKS, 0.55f, pitch);

            spawnSonarBeam(world, player.getEyePosition(), targetPos, targetState);
        }
    }

    private static boolean isHeldInHands(@Nullable EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    private static float getPingPitch(double distance) {
        float pitch = (float) (1.8f - (distance / 32.0f));
        return Math.max(0.6f, Math.min(2.0f, pitch));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                ItemStack stack = user.getItemInHand(hand);
                cycleMode(stack, user);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level world = context.getLevel();
            if (!world.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                BlockState state = world.getBlockState(context.getClickedPos());

                setMode(stack, DetectMode.CUSTOM);
                setCustomBlock(stack, state);

                context.getPlayer().sendSystemMessage(Component.literal("Calibrated to: ").withStyle(ChatFormatting.GREEN)
                        .append(state.getBlock().getName().copy().withStyle(ChatFormatting.WHITE)));

                world.playSound(null, context.getClickedPos(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    private BlockPos findNearestOreWithRaycast(Level world, Vec3 eyesPos, DetectMode mode, ItemStack stack, double costMultiplier) {
        BlockPos origin = BlockPos.containing(eyesPos);
        BlockState customTarget = (mode == DetectMode.CUSTOM) ? getCustomBlock(stack, world.registryAccess()) : null;

        int scanRadius = (int) Math.ceil(mode.budget);
        double maxScanDistanceSq = scanRadius * scanRadius;
        BlockPos bestTarget = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = origin.offset(x, y, z);

                    double distanceSq = origin.distSqr(checkPos);
                    if (distanceSq > maxScanDistanceSq || distanceSq >= bestDistanceSq) continue;

                    BlockState state = world.getBlockState(checkPos);
                    if (!isTarget(state, mode, customTarget)) continue;

                    if (canReach(world, eyesPos, checkPos, mode.budget, costMultiplier)) {
                        bestTarget = checkPos;
                        bestDistanceSq = distanceSq;
                    }
                }
            }
        }

        return bestTarget;
    }

    private boolean canReach(Level world, Vec3 start, BlockPos target, double budget, double costMultiplier) {
        Vec3 end = target.getCenter();
        Vec3 vector = end.subtract(start);
        double distance = vector.length();
        Vec3 direction = vector.normalize();

        double accumulatedCost = 0;
        double stepSize = 0.5;

        for (double d = 0; d < distance; d += stepSize) {
            Vec3 currentPos = start.add(direction.scale(d));
            BlockPos bPos = BlockPos.containing(currentPos);

            if (bPos.equals(target)) break;

            BlockState state = world.getBlockState(bPos);

            double blockDensity = getBlockDensity(state);

            accumulatedCost += (stepSize * blockDensity * costMultiplier);

            if (accumulatedCost > budget) return false;
        }

        return accumulatedCost <= budget;
    }

    private double getBlockDensity(BlockState state) {
        if (state.isAir() || !state.canOcclude()) {
            return 1.0;
        }

        if (state.is(Blocks.NETHERRACK)) {
            return 1.5;
        }

        if (state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.POLISHED_BASALT)
                || state.is(Blocks.BLACKSTONE)) {
            return 6.0;
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return 3.0;
        }

        return 3.0;
    }

    private boolean isTarget(BlockState state, DetectMode mode, BlockState customTarget) {
        return switch (mode) {
            case IRON -> state.is(BlockTags.IRON_ORES);
            case GOLD -> state.is(BlockTags.GOLD_ORES);
            case DIAMOND -> state.is(BlockTags.DIAMOND_ORES);
            case NETHERITE -> state.is(Blocks.ANCIENT_DEBRIS);
            case ALL -> state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) ||
                    state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES) ||
                    state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES) ||
                    state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES) ||
                    state.is(Blocks.ANCIENT_DEBRIS) || state.is(Blocks.NETHER_QUARTZ_ORE);
            case CUSTOM -> customTarget != null && state.is(customTarget.getBlock());
        };
    }

    private void spawnSonarBeam(ServerLevel world, Vec3 startPos, BlockPos endPos, BlockState targetState) {
        Vec3 targetCenter = endPos.getCenter();
        Vec3 direction = targetCenter.subtract(startPos).normalize();
        double distance = startPos.distanceTo(targetCenter);

        double stepSize = 0.4;
        for (double d = 0.5; d < distance; d += stepSize) {
            Vec3 p = startPos.add(direction.scale(d));
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, targetState),
                    p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.sendParticles(ParticleTypes.END_ROD, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0.1, 0.1, 0.1, 0.02);
        world.sendParticles(ParticleTypes.WAX_ON, targetCenter.x, targetCenter.y, targetCenter.z, 5, 0.3, 0.3, 0.3, 0.05);

    }

    private void cycleMode(ItemStack stack, Player player) {
        DetectMode current = getMode(stack);
        DetectMode[] modes = DetectMode.values();
        DetectMode next = modes[(current.ordinal() + 1) % modes.length];
        setMode(stack, next);

        player.sendSystemMessage(Component.literal("Detector Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(next.name).withStyle(next.color)));

        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);

        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    private DetectMode getMode(ItemStack stack) {
        CompoundTag nbt = getCustomData(stack);
        int modeIndex = 0;

        if (nbt.contains("Mode")) {
            modeIndex = nbt.getIntOr("Mode", 0);
        }

        modeIndex = Math.max(0, Math.min(DetectMode.values().length - 1, modeIndex));

        return DetectMode.values()[modeIndex];
    }

    private void setMode(ItemStack stack, DetectMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putInt("Mode", mode.ordinal()));
    }

    private void setCustomBlock(ItemStack stack, BlockState state) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt ->
                nbt.put("CustomBlock", NbtUtils.writeBlockState(state))
        );
    }

    private BlockState getCustomBlock(ItemStack stack, HolderLookup.Provider registryLookup) {
        if (registryLookup == null) return null;

        CompoundTag nbt = getCustomData(stack);
        if (nbt.contains("CustomBlock")) {
            var blockRegistry = registryLookup.lookupOrThrow(Registries.BLOCK);
            try {
                return NbtUtils.readBlockState(blockRegistry, nbt.getCompoundOrEmpty("CustomBlock"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        DetectMode mode = getMode(stack);
        textConsumer.accept(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(mode.name).withStyle(mode.color)));

        if (mode == DetectMode.CUSTOM) {
            BlockState custom = getCustomBlock(stack, context.registries());
            if (custom != null) {
                textConsumer.accept(Component.literal("Target: ").withStyle(ChatFormatting.GRAY)
                        .append(custom.getBlock().getName().copy().withStyle(ChatFormatting.GREEN)));
            } else {
                textConsumer.accept(Component.literal("Target: None (Sneak-Use on block)").withStyle(ChatFormatting.RED));
            }
        } else {
            textConsumer.accept(Component.literal("Sneak + Use to cycle modes").withStyle(ChatFormatting.DARK_GRAY));
        }

        textConsumer.accept(Component.empty());
        textConsumer.accept(Component.literal("Power: " + (int)mode.budget).withStyle(ChatFormatting.DARK_AQUA));
        textConsumer.accept(Component.literal("Penetrates dense blocks slower.").withStyle(ChatFormatting.GRAY));
    }
}