package com.simplebuilding.fluid;

import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The crucible buckets (plan section 10, owner round 3 answers 32-36):
 * <ul>
 *   <li><b>Copper</b>: water and lava, never soul lava. Pouring oxidizes it one stage (0-3, looks only) unless
 *       waxed; poured water never makes a source (one flowing block that runs off); pouring lava breaks it.
 *       Main hand bucket + axe in the off hand scrapes one stage (or the wax) off, + honeycomb waxes it.</li>
 *   <li><b>Enderite</b>: water, lava and soul lava, never breaks.</li>
 *   <li><b>Iron</b> (only the soul lava bucket here - Vanilla's bucket scoops soul lava itself): breaks when
 *       the soul lava is poured.</li>
 * </ul>
 */
public class ModBucketItem extends BucketItem {
    public enum Kind { COPPER, ENDERITE, IRON }

    private final Kind kind;

    public ModBucketItem(Kind kind, Fluid content, Properties properties) {
        super(content, properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    // ------------------------------------------------------------ variants

    /** The empty bucket of this kind (null for iron: Vanilla's bucket). */
    public static @Nullable Item empty(Kind kind) {
        return switch (kind) {
            case COPPER -> ModFluids.COPPER_BUCKET;
            case ENDERITE -> ModFluids.ENDERITE_BUCKET;
            case IRON -> Items.BUCKET;
        };
    }

    /** The bucket of {@code kind} filled with {@code fluid} (source), or null when this kind cannot hold it. */
    public static @Nullable Item filled(Kind kind, Fluid fluid) {
        if (fluid.isSame(Fluids.WATER)) {
            return switch (kind) {
                case COPPER -> ModFluids.COPPER_WATER_BUCKET;
                case ENDERITE -> ModFluids.ENDERITE_WATER_BUCKET;
                case IRON -> Items.WATER_BUCKET;
            };
        }
        if (fluid.isSame(Fluids.LAVA)) {
            return switch (kind) {
                case COPPER -> ModFluids.COPPER_LAVA_BUCKET;
                case ENDERITE -> ModFluids.ENDERITE_LAVA_BUCKET;
                case IRON -> Items.LAVA_BUCKET;
            };
        }
        if (fluid.isSame(ModFluids.SOUL_LAVA)) {
            return switch (kind) {
                case COPPER -> null; // owner: the copper bucket cannot take soul lava
                case ENDERITE -> ModFluids.ENDERITE_SOUL_LAVA_BUCKET;
                case IRON -> ModFluids.SOUL_LAVA_BUCKET;
            };
        }
        return null;
    }

    /** Whether pouring this bucket's content breaks it (copper + lava, iron + soul lava; Enderite never). */
    public boolean breaksOnPour() {
        if (kind == Kind.ENDERITE || getContent() == Fluids.EMPTY) return false;
        if (kind == Kind.COPPER) return getContent().is(FluidTags.LAVA);
        return getContent().isSame(ModFluids.SOUL_LAVA);
    }

    /** What stays in the hand after pouring {@code stack} (survival): the empty bucket, or nothing when it breaks. */
    public ItemStack afterPour(ItemStack stack) {
        if (breaksOnPour()) return ItemStack.EMPTY;
        Item empty = empty(kind);
        ItemStack out = stack.transmuteCopy(empty == null ? Items.BUCKET : empty, 1);
        if (kind == Kind.COPPER) oxidize(out);
        return out;
    }

    /** A filled copy of an empty bucket of this kind, keeping its components (oxidation, wax). */
    public static ItemStack fill(ItemStack empty, Item filled) {
        return empty.transmuteCopy(filled, 1);
    }

    // ------------------------------------------------------------ copper oxidation

    public static int oxidation(ItemStack stack) {
        Integer value = stack.get(ModDataComponentTypes.OXIDATION);
        return value == null ? 0 : value;
    }

    public static boolean waxed(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponentTypes.WAXED));
    }

    /** One oxidation stage more (owner 32: on pouring), unless waxed. */
    public static void oxidize(ItemStack stack) {
        if (!waxed(stack)) stack.set(ModDataComponentTypes.OXIDATION, Math.min(3, oxidation(stack) + 1));
    }

    /** Axe: takes the wax off first, otherwise one oxidation stage. True when something changed. */
    public static boolean scrape(ItemStack stack) {
        if (waxed(stack)) {
            stack.remove(ModDataComponentTypes.WAXED);
            return true;
        }
        int stage = oxidation(stack);
        if (stage <= 0) return false;
        if (stage == 1) stack.remove(ModDataComponentTypes.OXIDATION);
        else stack.set(ModDataComponentTypes.OXIDATION, stage - 1);
        return true;
    }

    // ------------------------------------------------------------ use

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.COPPER && hand == InteractionHand.MAIN_HAND) {
            InteractionResult care = copperCare(level, player, stack);
            if (care != null) return care;
        }
        if (getContent() == Fluids.EMPTY) return scoop(level, player, hand, stack);
        InteractionResult result = super.use(level, player, hand);
        if (result instanceof InteractionResult.Success success && !player.hasInfiniteMaterials()) {
            ItemStack after = afterPour(stack);
            if (after.isEmpty()) {
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                return InteractionResult.SUCCESS.heldItemTransformedTo(ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(after);
        }
        return result;
    }

    /** Copper bucket in the main hand: axe in the off hand scrapes, honeycomb waxes (null: neither). */
    private @Nullable InteractionResult copperCare(Level level, Player player, ItemStack stack) {
        ItemStack other = player.getOffhandItem();
        if (other.is(ItemTags.AXES)) {
            if (!scrape(stack)) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                other.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        if (other.is(Items.HONEYCOMB)) {
            if (waxed(stack)) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                stack.set(ModDataComponentTypes.WAXED, true);
                level.playSound(null, player.blockPosition(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.0F);
                other.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    /** An empty copper/enderite bucket takes a fluid source it can hold. */
    private InteractionResult scoop(Level level, Player player, InteractionHand hand, ItemStack stack) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(direction), direction, stack)) return InteractionResult.FAIL;
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return InteractionResult.FAIL;
        Item target = filled(kind, fluid.getType());
        if (target == null) {
            if (!level.isClientSide()) level.playSound(null, pos, SoundEvents.METAL_HIT, SoundSource.BLOCKS, 0.6F, 0.6F);
            return InteractionResult.FAIL;
        }
        ItemStack taken = pickup.pickupBlock(player, level, pos, state);
        if (taken.isEmpty()) return InteractionResult.FAIL;
        player.awardStat(Stats.ITEM_USED.get(this));
        pickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        ItemStack result = ItemUtils.createFilledResult(stack, player, fill(stack, target));
        return InteractionResult.SUCCESS.heldItemTransformedTo(result);
    }

    /** Copper water never makes a source (owner 32): one flowing water block that runs off. */
    @Override
    public boolean emptyContents(@Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult hit) {
        if (kind == Kind.COPPER && getContent().isSame(Fluids.WATER)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced(getContent())) {
                return hit != null && emptyContents(user, level, hit.getBlockPos().relative(hit.getDirection()), null);
            }
            if (level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos)) {
                level.playSound(user, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
                return true;
            }
            if (!level.isClientSide() && !state.isAir() && !state.liquid()) level.destroyBlock(pos, true);
            level.setBlock(pos, Fluids.WATER.getFlowing(7, false).createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
            playEmptySound(user, level, pos);
            return true;
        }
        return super.emptyContents(user, level, pos, hit);
    }

    /** Server-side pour for tests and dispensers: places the content and returns what stays (survival rules). */
    public ItemStack pourAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!emptyContents(null, level, pos, null)) return stack;
        return afterPour(stack);
    }
}
