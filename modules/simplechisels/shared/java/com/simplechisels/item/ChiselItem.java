package com.simplechisels.item;

import com.simplechisels.ChiselTier;
import com.simplechisels.SimpleChisels;
import com.simplechisels.config.ChiselConfig;
import com.simplechisels.data.ChiselTables;
import com.simplechisels.registry.ChiselRegistry;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A chisel (moved from SimpleBuilding's {@code ChiselItem}, same behaviour): a right click moves the block one step along
 * its family in the chisel's tier table ({@link ChiselTables}), sneaking one step back at twice the durability; with
 * SimpleBuilding's Constructor's Touch the larger {@code touch} table applies. Stairs, pillars and rods turn towards the
 * clicked edge. Fast Chiseling shortens the cooldown and speeds up mining. A partner's way runs first
 * (SimpleLib {@code ChiselActions.use}: SimpleBuilding's wood carving with a pottery sherd).
 */
public class ChiselItem extends Item {
    /** Durability per step forward. */
    public static final int TRANSFORM_DAMAGE = 1;
    /** Durability per step back (sneaking). */
    public static final int REVERSE_TRANSFORM_DAMAGE = 2;
    public static final ResourceKey<Enchantment> FAST_CHISELING = ResourceKey.create(Registries.ENCHANTMENT,
            SimpleChisels.id(SimpleChisels.FAST_CHISELING));
    /** SimpleBuilding's Constructor's Touch (chisels, wands, hammer): read by id, without SimpleBuilding no item has it. */
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath("simplebuilding", "constructors_touch"));

    private final ChiselTier tier;

    public ChiselItem(ChiselTier tier, Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public ChiselTier tier() {
        return tier;
    }

    /** The cooldown that applies now: {@code cooldownTicks.<tier>} of the server config. */
    public int effectiveCooldownTicks() {
        return ChiselConfig.cooldownTicks(tier);
    }

    public Map<Block, Block> getForwardMap() { return ChiselTables.tier(tier.table).forward(); }
    public Map<Block, Block> getBackwardMap() { return ChiselTables.tier(tier.table).backward(); }
    public Map<Block, Block> getTouchForwardMap() { return ChiselTables.tier(tier.table).touchForward(); }
    public Map<Block, Block> getTouchBackwardMap() { return ChiselTables.tier(tier.table).touchBackward(); }

    // Mines pickaxe, axe and shovel blocks with drops, at half the material speed (plus Fast Chiseling).
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isCorrectToolForDrops(stack, state)) return 1.0f;
        int fastChiseling = level(stack, null, FAST_CHISELING);
        float bonus = fastChiseling == 1 ? 5.0f : fastChiseling >= 2 ? 17.0f : 0.0f; // Efficiency II / IV equivalent
        return (tier.material.speed() + bonus) * 0.5f;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && state.getDestroySpeed(world, pos) != 0.0F) {
            stack.hurtAndBreak(2, miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !SimpleChisels.on()) {
            return InteractionResult.PASS;
        }
        // The rights of placing a block (adventure mode, spawn protection, claims).
        if (!ChiselProtection.mayTransform(context.getLevel(), player, context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        InteractionResult partner = com.simplelib.api.ChiselActions.use(context, effectiveCooldownTicks(), TRANSFORM_DAMAGE);
        if (partner != null) {
            return partner;
        }
        if (context.getLevel().isClientSide()) {
            // The client decides with the same table as the server: only then the click is used here and not passed on.
            return canChisel(context.getLevel(), context.getClickedPos(), context.getItemInHand(), player)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        Vec3 relativeHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
        return tryChiselBlock(context.getLevel(), player, context.getHand(), context.getClickedPos(), context.getItemInHand(),
                context.getClickedFace(), relativeHit) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    private boolean tryChiselBlock(Level world, Player player, InteractionHand hand, BlockPos pos, ItemStack stack, Direction side, Vec3 relativeHit) {
        if (player.getCooldowns().isOnCooldown(stack)) return false;
        BlockState oldState = world.getBlockState(pos);
        Map<Block, Block> map = activeMap(world, stack, player);
        Block newBlock = map.get(oldState.getBlock());
        if (newBlock == null) return false;

        BlockState newState = newBlock.defaultBlockState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.hasProperty(prop)) newState = copyProperty(oldState, newState, prop);
        }
        newState = applyIntuitiveOrientation(newState, side, relativeHit, player);
        world.setBlockAndUpdate(pos, newState);
        com.simplelib.api.ChiselActions.fire(com.simplelib.api.ChiselActions.CHISEL, player);

        int cooldown = effectiveCooldownTicks();
        int fastChiseling = level(stack, world, FAST_CHISELING);
        if (fastChiseling > 0) cooldown = Math.max(1, (int) (cooldown * (1.0f - fastChiseling * 0.3f)));
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(stack, cooldown);
            int damage = player.isShiftKeyDown() ? REVERSE_TRANSFORM_DAMAGE : TRANSFORM_DAMAGE;
            stack.hurtAndBreak(damage, (ServerLevel) world, (ServerPlayer) player,
                    item -> player.onEquippedItemBroken(item, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
        }
        float pitch = 1.0F + (world.getRandom().nextFloat() * 0.4F - 0.2F);
        world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.5f, pitch);
        ((ServerLevel) world).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, oldState),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.1);
        stack.set(ChiselRegistry.LAST_TARGET, pos);
        return true;
    }

    private <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    /** The table of this click: forward, sneaking backward; with Constructor's Touch the larger one. */
    private Map<Block, Block> activeMap(Level world, ItemStack stack, Player player) {
        boolean touch = level(stack, world, CONSTRUCTORS_TOUCH) > 0;
        if (!player.isShiftKeyDown()) return touch ? getTouchForwardMap() : getForwardMap();
        return touch ? getTouchBackwardMap() : getBackwardMap();
    }

    /** Whether a click now would reshape the block (click prediction, hand hint). */
    public boolean canChisel(Level world, BlockPos pos, ItemStack stack, Player player) {
        if (!SimpleChisels.on() || player.getCooldowns().isOnCooldown(stack)) return false;
        return activeMap(world, stack, player).containsKey(world.getBlockState(pos).getBlock());
    }

    /** The level of {@code key} on {@code stack}; with a level through its registry, without one from the stack's component. */
    public static int level(ItemStack stack, @Nullable Level level, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;
        if (level != null) {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return registry.get(key).map(e -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e, stack)).orElse(0);
        }
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) return entry.getIntValue();
        }
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag type) {
        BlockPos p = stack.get(ChiselRegistry.LAST_TARGET);
        if (p != null) {
            lines.accept(Component.translatable("tooltip.simplechisels.chisel.last_target", p.getX(), p.getY(), p.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, lines, type);
    }

    /** Turns stairs, pillars and rods towards the clicked edge (a 25 % margin of the face), else towards the clicked face. */
    public static BlockState applyIntuitiveOrientation(BlockState state, Direction side, Vec3 hit, Player player) {
        double margin = 0.25;
        double x = hit.x;
        double y = hit.y;
        double z = hit.z;
        Direction orientation = side;
        boolean isEdge = false;
        if (side.getAxis() == Direction.Axis.Y) {
            if (x < margin) { orientation = Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = Direction.EAST; isEdge = true; }
            else if (z < margin) { orientation = Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = Direction.SOUTH; isEdge = true; }
        } else if (side.getAxis() == Direction.Axis.X) {
            if (y < margin) { orientation = Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = Direction.UP; isEdge = true; }
            else if (z < margin) { orientation = Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = Direction.SOUTH; isEdge = true; }
        } else if (side.getAxis() == Direction.Axis.Z) {
            if (y < margin) { orientation = Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = Direction.UP; isEdge = true; }
            else if (x < margin) { orientation = Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = Direction.EAST; isEdge = true; }
        }
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.setValue(RotatedPillarBlock.AXIS, isEdge ? orientation.getAxis() : side.getAxis());
        }
        if (state.hasProperty(StairBlock.FACING)) {
            Direction facing = isEdge && orientation.getAxis().isHorizontal() ? orientation.getOpposite() : player.getDirection();
            state = state.setValue(StairBlock.FACING, facing);
            Half half;
            if ((side == Direction.UP && !isEdge) || (y < 0.5 && !isEdge)) half = Half.BOTTOM;
            else if ((side == Direction.DOWN && !isEdge) || (y > 0.5 && !isEdge)) half = Half.TOP;
            else half = y > 0.5 ? Half.TOP : Half.BOTTOM;
            if (orientation == Direction.UP) half = Half.BOTTOM;
            if (orientation == Direction.DOWN) half = Half.TOP;
            return state.setValue(StairBlock.HALF, half);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, isEdge ? orientation : side);
        }
        return state;
    }
}
