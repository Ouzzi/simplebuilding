package com.simplebuilding.util;

import com.simplebuilding.items.custom.*;
import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Read-only gameplay predicates shared by hand hints and server tests. Never simulates item use. */
public final class TransformTargets {
    private TransformTargets() {}
    public static boolean canTransformTarget(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        if (level == null || player == null || player.isSpectator() || !player.mayBuild()) return false;
        ItemStack stack = player.getItemInHand(hand);
        var pos = hit.getBlockPos();
        if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return false;
        var state = level.getBlockState(pos);
        Item item = stack.getItem();
        boolean upgrade = SledgehammerUpgrades.showsUpgradeHint(level, pos, player);
        boolean template = PlacedTemplates.isHammerTarget(level, pos, player);
        if (item instanceof SledgehammerItem hammer) {
            return (hand == InteractionHand.MAIN_HAND && (upgrade || template))
                    || (state.is(Blocks.DIAMOND_BLOCK) && SledgehammerItem.canCrushDiamondBlock(item))
                    || hammer.getTransformationState(state, pos, hit.getDirection(),
                            hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)), player, stack) != null;
        }
        if (hand == InteractionHand.OFF_HAND && (upgrade || template)) return true;
        if (stack.is(Items.STICK)) return ConstructorsTouchInteraction.canTransformTarget(player, level, hand, hit);
        if (item instanceof RotatorItem rotator)
            return rotator.canTransformTarget(new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
        if (item instanceof ChiselItem chisel) return chisel.canChisel(level, pos, stack, player);
        if (stack.is(Items.SHEARS) && (state.is(BlockTags.WOOL) || state.getBlock() instanceof PumpkinBlock)) return true;
        if (item instanceof OctantItem && item != com.simplebuilding.items.ModItems.OCTANT)
            return state.is(Blocks.WATER_CAULDRON);
        if (item instanceof BuildingCoreItem) return CoreOreTransmutation.hostOf(state).isPresent();
        if (state.getBlock() instanceof CopperPressurePlateBlock plate) {
            if (stack.is(Items.HONEYCOMB)) return plate.getWaxedState(state).isPresent();
            if (stack.typeHolder().is(ItemTags.AXES))
                return plate.getUnwaxedState(state).isPresent() || plate.getPreviousState(state).isPresent();
        }
        if (stack.is(Items.HONEYCOMB) && HoneycombItem.WAXABLES.get().containsKey(state.getBlock())) return true;
        return com.simplebuilding.version.McVersion.canVanillaTransform(level, hit, player, hand);
    }

    /**
     * Teil-Hinweis (Besitzer 2026-10-01): das Item in dieser Hand passt zu einer Hammer-Aufwertung am
     * Ziel, aber das Gegenstueck fehlt - Hammer ohne Material oder Material ohne Hammer. Die Hand neigt
     * sich dann nur halb so stark wie bei {@link #canTransformTarget}.
     */
    public static boolean partialTransformTarget(Level level, BlockHitResult hit, Player player, InteractionHand hand) {
        if (level == null || player == null || player.isSpectator() || !player.mayBuild()) return false;
        ItemStack stack = player.getItemInHand(hand);
        var pos = hit.getBlockPos();
        if (stack.isEmpty() || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        boolean machine = SledgehammerUpgrades.upgradeOf(state.getBlock()) != null;
        boolean template = PlacedTemplates.isUpgradable(level, pos);
        if (!machine && !template) return false;
        boolean hammerInMain = player.getMainHandItem().getItem() instanceof SledgehammerItem;
        ItemStack off = player.getOffhandItem();
        boolean material = machine && SledgehammerUpgrades.isUpgradeNugget(off)
                || template && SledgehammerEntityInteraction.trimUpgrades().containsKey(off.getItem());
        if (stack.getItem() instanceof SledgehammerItem) return hand == InteractionHand.MAIN_HAND && !material;
        boolean fits = machine && SledgehammerUpgrades.isUpgradeNugget(stack)
                || template && SledgehammerEntityInteraction.trimUpgrades().containsKey(stack.getItem());
        return fits && !hammerInMain;
    }
}
