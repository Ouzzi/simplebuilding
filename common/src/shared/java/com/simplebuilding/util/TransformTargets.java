package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.items.custom.*;
import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Read-only gameplay predicates for the hand hint (26.3: the held item tilts when a right click
 * would transform the aimed block) and the server tests. Never simulates item use.
 *
 * <p><b>Same predicate as the action.</b> Every transformation asks one shared method that its real
 * action asks too ({@link ChiselItem#canChisel}, {@link NetheriteBreakerPistonBlock#canRepairWith},
 * {@link CopperPressurePlateBlock#transformWith}, {@link ShearsWoolInteraction#canShear},
 * {@link OctantCauldronWash#isWashable}, {@link SledgehammerItem#strikesDiamondBlock}, ...), and
 * {@link #mayTransform} is the permission check of every mod action that changes the clicked block.
 *
 * <p><b>Vanilla click order.</b> Main hand first: the block's own item interaction
 * ({@code useItemOn}: cauldron, pumpkin, sign, piston repair, copper plate), then - unless sneaking
 * with an item - its empty-hand action ({@code useWithoutItem}: menus, doors, buttons, repeaters,
 * signs), then the item ({@code useOn}). The off hand only gets the click when the main hand did
 * nothing, so it does not hint when the main hand would act ({@link #mainHandTakesClick}). Rotator,
 * chisel, sledgehammer, shears on wool, honeycomb and the vanilla tool transforms run in
 * {@code useOn} and therefore do not hint where the block takes the click first
 * ({@link #blockTakesClick}). Hints of other mods arrive through {@code ModuleTransformHints}.
 *
 * <p><b>Not hinted:</b> the building core's ore chance (owner backlog Q4: a 1-in-2000..10000 roll
 * per click is nothing the click promises, so it gets no hint at all, not even the partial one).
 * <b>Limits:</b> on a client {@code WorldPermissions} has no provider (claims live on the server),
 * so a claim on a remote server does not hide the hint - the server still refuses. An item whose own
 * {@code use} would claim a main-hand click (food, ender pearls) is not predicted.
 */
public final class TransformTargets {
    private TransformTargets() {}

    /**
     * Whether {@code player} may change the clicked block at {@code pos} with {@code stack}: the
     * question a block placement asks (spectator, adventure mode, spawn protection, claims through
     * {@code WorldPermissions}). Rotator, chisel, sledgehammer, Constructor's Touch, piston repair
     * and the copper pressure plate ask it before acting; the hint asks it too.
     */
    public static boolean mayTransform(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        return !player.isSpectator() && player.mayBuild() && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, face, stack) && com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos);
    }

    /** Item, cooldown and permission checks shared by the full and the partial hint. */
    private static boolean mayHint(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        if (level == null || player == null) return false;
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty() && !player.getCooldowns().isOnCooldown(stack)
                && mayTransform(level, player, hit.getBlockPos(), hit.getDirection(), stack);
    }

    public static boolean canTransformTarget(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        if (!mayHint(level, hit, player, hand)) return false;
        // Hammer in the main hand + material in the off hand: both tilt together (the machine upgrade
        // and the placed-template upgrade bypass the block's menu by design).
        if (hammerPairing(level, hit.getBlockPos(), player)) return true;
        if (hand == InteractionHand.OFF_HAND && mainHandTakesClick(level, hit, player)) return false;
        return handTransforms(level, hit, player, hand);
    }

    /** A machine or placed-template upgrade can start with this hammer and this material now. */
    private static boolean hammerPairing(Level level, BlockPos pos, Player player) {
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof SledgehammerItem) || player.getCooldowns().isOnCooldown(main)) return false;
        return SledgehammerUpgrades.showsUpgradeHint(level, pos, player) || PlacedTemplates.isHammerTarget(level, pos, player);
    }

    /**
     * Whether the main hand uses this click before the off hand gets it: the block's empty-hand
     * action, a transformation by the main-hand item, or an item that always acts on a block
     * (placing a block, the building core's show).
     */
    public static boolean mainHandTakesClick(Level level, BlockHitResult hit, Player player) {
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (blockTakesClick(level, hit.getBlockPos(), state, player)) return true;
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty() || player.getCooldowns().isOnCooldown(main)) return false;
        if (main.getItem() instanceof BlockItem || main.getItem() instanceof BuildingCoreItem) return true;
        return mayHint(level, hit, player, InteractionHand.MAIN_HAND) && handTransforms(level, hit, player, InteractionHand.MAIN_HAND);
    }

    /** What the item in {@code hand} alone transforms with this click (permissions already checked). */
    private static boolean handTransforms(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Item item = stack.getItem();

        // Interaction callbacks that run before the block: other mods, Constructor's Touch.
        if (com.simplebuilding.api.ModuleTransformHints.wouldTransform(level, player, hit, hand)) return true;
        if (stack.is(Items.STICK)) return ConstructorsTouchInteraction.canTransformTarget(player, level, hand, hit);

        // The block's own item interaction (BlockBehaviour#useItemOn), before its empty-hand action.
        if (NetheriteBreakerPistonBlock.canRepairWith(state, stack)) return true;
        if (state.getBlock() instanceof CopperPressurePlateBlock plate) return plate.transformWith(state, stack).isPresent();
        if (state.is(Blocks.WATER_CAULDRON) && (OctantCauldronWash.isWashable(stack) || TieredShulkerBoxes.isWashable(stack))) return true;
        if (stack.is(Items.SHEARS) && state.getBlock() instanceof PumpkinBlock) return true;
        if (item instanceof SignApplicator && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity)
            return com.simplebuilding.version.McVersion.canVanillaTransform(level, hit, player, hand);

        // Item#useOn: only reached when the block does not take the click first.
        if (blockTakesClick(level, pos, state, player)) return false;
        if (item instanceof SledgehammerItem hammer) {
            return SledgehammerItem.strikesDiamondBlock(state, item)
                    || hammer.getTransformationState(state, pos, hit.getDirection(),
                            hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)), player, stack) != null;
        }
        if (item instanceof RotatorItem rotator)
            return rotator.canTransformTarget(new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
        if (item instanceof ChiselItem chisel) return chisel.canChisel(level, pos, stack, player);
        if (stack.is(Items.SHEARS) && ShearsWoolInteraction.canShear(state)) return true;
        if (stack.is(Items.HONEYCOMB) && HoneycombItem.WAXABLES.get().containsKey(state.getBlock())) return true;
        return com.simplebuilding.version.McVersion.canVanillaTransform(level, hit, player, hand);
    }

    /**
     * Whether the block's empty-hand action takes this click before any item {@code useOn}: vanilla
     * calls it unless the player sneaks with something in a hand. Doors and trapdoors only when
     * they open by hand (iron ones do not); otherwise every block class that overrides
     * {@code useWithoutItem} (menus, buttons, levers, repeaters, beds, signs, ...).
     */
    public static boolean blockTakesClick(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof DoorBlock door) return door.type().canOpenByHand();
        if (block instanceof TrapDoorBlock) return !state.is(Blocks.IRON_TRAPDOOR);
        return EMPTY_HAND_ACTION.get(block.getClass());
    }

    /** Block classes that override {@code useWithoutItem}; computed once per class. */
    private static final ClassValue<Boolean> EMPTY_HAND_ACTION = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != Block.class && c != BlockBehaviour.class; c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    if (method.getName().equals("useWithoutItem") && method.getParameterCount() == 5) return true;
                }
            }
            return false;
        }
    };

    /**
     * Teil-Hinweis (Besitzer 2026-10-01): das Item in dieser Hand passt zu einer Hammer-Aufwertung am
     * Ziel, aber das Gegenstueck fehlt - Hammer ohne Material oder Material ohne Hammer. Die Hand neigt
     * sich dann nur halb so stark wie bei {@link #canTransformTarget}. Maschinen fragen dieselben
     * Bedingungen wie die echte Aufwertung ({@link SledgehammerUpgrades#missingCounterpart}).
     */
    public static boolean partialTransformTarget(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        if (!mayHint(level, hit, player, hand)) return false;
        BlockPos pos = hit.getBlockPos();
        if (SledgehammerUpgrades.missingCounterpart(level, pos, player, hand)) return true;
        if (!(level.getBlockState(pos).getBlock() instanceof PlacedTemplateBlock) || !PlacedTemplates.isUpgradable(level, pos)) return false;
        ItemStack stack = player.getItemInHand(hand);
        var catalysts = SledgehammerEntityInteraction.trimUpgrades();
        if (stack.getItem() instanceof SledgehammerItem)
            return hand == InteractionHand.MAIN_HAND && !catalysts.containsKey(player.getOffhandItem().getItem());
        return catalysts.containsKey(stack.getItem()) && !(player.getMainHandItem().getItem() instanceof SledgehammerItem);
    }
}
