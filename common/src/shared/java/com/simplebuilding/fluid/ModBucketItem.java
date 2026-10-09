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
 *   <li><b>Enderite</b>: water, lava and soul lava, never breaks. Owner N21/N28: it holds two buckets of one fluid
 *       (half and full items, {@link ModFluids#fullEnderite}). Use only scoops while it has room (empty and half);
 *       a full one pours one bucket on use; sneak + use pours one bucket from a half or full one.</li>
 *   <li><b>Iron</b> (only the soul lava bucket here - Vanilla's bucket scoops soul lava itself): breaks when
 *       the soul lava is poured.</li>
 *   <li><b>Ceramic</b> (owner addition 11): fired clay, water and (owner N12) lava - real crucibles are ceramic -,
 *       no soul lava, no milk. Owner N12b: it wears like copper oxidizes - in {@link #CERAMIC_STAGES} stages (as many
 *       as the copper bucket's oxidation stages), each its own item for every filling: intact, chipped, cracked,
 *       brittle. Every pour is one of {@link #CERAMIC_USES} uses (counted in a hidden component, no durability bar);
 *       every {@link #CERAMIC_USES_PER_STAGE} pours it moves one stage on and keeps its filling. Scooping does not
 *       wear it; the last pour breaks the brittle bucket. As furnace fuel a ceramic lava bucket burns up.</li>
 * </ul>
 * A fully oxidized copper bucket (stage 3) scoops nothing any more (owner addition 11); pouring one that is
 * already full still works, and the axe/honeycomb care keeps working.
 */
public class ModBucketItem extends BucketItem {
    public enum Kind { COPPER, ENDERITE, IRON, CERAMIC }

    private final Kind kind;
    /** Wear stage (ceramic only, 0 = intact). */
    private final int stage;

    public ModBucketItem(Kind kind, Fluid content, Properties properties) {
        this(kind, content, 0, properties);
    }

    public ModBucketItem(Kind kind, Fluid content, int stage, Properties properties) {
        super(content, properties);
        this.kind = kind;
        this.stage = stage;
    }

    public Kind kind() {
        return kind;
    }

    /** Wear stage of a ceramic bucket (0 intact, 1 chipped, 2 cracked, 3 brittle); 0 for every other kind. */
    public int stage() {
        return stage;
    }

    // ------------------------------------------------------------ variants

    /** The empty bucket of this kind (null for iron: Vanilla's bucket). */
    public static @Nullable Item empty(Kind kind) {
        return switch (kind) {
            case COPPER -> ModFluids.COPPER_BUCKET;
            case ENDERITE -> ModFluids.ENDERITE_BUCKET;
            case IRON -> Items.BUCKET;
            case CERAMIC -> ModFluids.CERAMIC_BUCKET;
        };
    }

    /** The bucket of {@code kind} filled with {@code fluid} (source), or null when this kind cannot hold it. */
    public static @Nullable Item filled(Kind kind, Fluid fluid) {
        if (fluid.isSame(Fluids.WATER)) {
            return switch (kind) {
                case COPPER -> ModFluids.COPPER_WATER_BUCKET;
                case ENDERITE -> ModFluids.ENDERITE_WATER_BUCKET;
                case IRON -> Items.WATER_BUCKET;
                case CERAMIC -> ModFluids.CERAMIC_WATER_BUCKET;
            };
        }
        if (fluid.isSame(Fluids.LAVA)) {
            return switch (kind) {
                case COPPER -> ModFluids.COPPER_LAVA_BUCKET;
                case ENDERITE -> ModFluids.ENDERITE_LAVA_BUCKET;
                case IRON -> Items.LAVA_BUCKET;
                case CERAMIC -> ModFluids.CERAMIC_LAVA_BUCKET; // owner N12: fired clay holds lava
            };
        }
        if (fluid.isSame(ModFluids.SOUL_LAVA)) {
            return switch (kind) {
                case COPPER -> null; // owner: the copper bucket cannot take soul lava
                case ENDERITE -> ModFluids.ENDERITE_SOUL_LAVA_BUCKET;
                case IRON -> ModFluids.SOUL_LAVA_BUCKET;
                case CERAMIC -> null;
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

    /** Buckets of fluid this item holds: 0 empty, 2 a full Enderite bucket, otherwise 1. */
    public int amount() {
        if (getContent() == Fluids.EMPTY) return 0;
        return ModFluids.halfEnderite(this) != null ? 2 : 1;
    }

    /**
     * What stays in the hand after pouring {@code stack} (survival): the empty bucket, or nothing when it breaks; a full
     * Enderite bucket pours one of its two buckets and becomes the half one.
     */
    public ItemStack afterPour(ItemStack stack) {
        if (breaksOnPour()) return ItemStack.EMPTY;
        Item half = ModFluids.halfEnderite(this);
        if (half != null) return stack.transmuteCopy(half, 1);
        Item empty = kind == Kind.CERAMIC ? ModFluids.ceramic(Fluids.EMPTY, stage) : empty(kind);
        ItemStack out = stack.transmuteCopy(empty == null ? Items.BUCKET : empty, 1);
        if (kind == Kind.COPPER) oxidize(out);
        return wear(out);
    }

    /**
     * A filled copy of an empty bucket of this kind, keeping its components (oxidation, wax, uses). Scooping a ceramic
     * bucket does not wear it.
     */
    public static ItemStack fill(ItemStack empty, Item filled) {
        if (empty.getItem() instanceof ModBucketItem bucket && bucket.kind == Kind.CERAMIC && filled instanceof ModBucketItem target) {
            Item staged = ModFluids.ceramic(target.getContent(), bucket.stage);
            if (staged != null) filled = staged;
        }
        return empty.transmuteCopy(filled, 1);
    }

    /** Successful pours of a ceramic bucket before it breaks (owner rule: four pours). */
    public static final int CERAMIC_USES = 4;
    /** Wear stages of the ceramic bucket: as many as the copper bucket's oxidation stages (0..3). */
    public static final int CERAMIC_STAGES = 4;
    public static final int CERAMIC_USES_PER_STAGE = CERAMIC_USES / CERAMIC_STAGES;

    /** Uses of a ceramic bucket so far: the counter, at least the start of the item's stage. */
    public static int ceramicUses(ItemStack stack) {
        int stage = stack.getItem() instanceof ModBucketItem bucket ? bucket.stage : 0;
        Integer used = stack.get(ModDataComponentTypes.CERAMIC_USES);
        return Math.max(used == null ? 0 : used, stage * CERAMIC_USES_PER_STAGE);
    }

    /**
     * Ceramic buckets: one pour more; every {@link #CERAMIC_USES_PER_STAGE} pours the next stage's item with the same
     * filling; at {@link #CERAMIC_USES} the bucket is gone (empty stack). Other kinds unchanged.
     */
    public static ItemStack wear(ItemStack stack) {
        if (!(stack.getItem() instanceof ModBucketItem bucket) || bucket.kind != Kind.CERAMIC) return stack;
        int used = ceramicUses(stack) + 1;
        if (used >= CERAMIC_USES) return ItemStack.EMPTY;
        int stage = used / CERAMIC_USES_PER_STAGE;
        ItemStack out = stage == bucket.stage ? stack : stack.transmuteCopy(ModFluids.ceramic(bucket.getContent(), stage), stack.getCount());
        out.set(ModDataComponentTypes.CERAMIC_USES, used);
        return out;
    }

    /** Whether {@code stack} (an empty bucket of a mod kind) may scoop: not a fully oxidized copper bucket. */
    public static boolean canScoop(ItemStack stack) {
        return !(stack.getItem() instanceof ModBucketItem bucket && bucket.kind == Kind.COPPER && oxidation(stack) >= 3);
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
        // Owner N21: a half Enderite bucket only scoops on use; sneak + use pours one bucket.
        if (ModFluids.fullEnderite(this) != null && !player.isSecondaryUseActive()) return scoop(level, player, hand, stack);
        // super.use consumes the held stack (createFilledResult), so the follow-up is built from a copy.
        ItemStack poured = stack.copy();
        InteractionResult result = super.use(level, player, hand);
        if (result instanceof InteractionResult.Success success && !player.hasInfiniteMaterials()) {
            ItemStack after = afterPour(poured);
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

    /** An empty copper/enderite bucket takes a fluid source it can hold; a half Enderite bucket one more of its fluid. */
    private InteractionResult scoop(Level level, Player player, InteractionHand hand, ItemStack stack) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(direction), direction, stack)) return InteractionResult.FAIL;
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return InteractionResult.FAIL;
        Item target = getContent() == Fluids.EMPTY ? (canScoop(stack) ? filled(kind, fluid.getType()) : null)
                : fluid.getType().isSame(getContent()) ? ModFluids.fullEnderite(this) : null;
        if (target == null) {
            if (!level.isClientSide()) level.playSound(null, pos, SoundEvents.METAL_HIT, SoundSource.BLOCKS, 0.6F, 0.6F);
            return InteractionResult.FAIL;
        }
        ItemStack taken = pickup.pickupBlock(player, level, pos, state);
        if (taken.isEmpty()) return InteractionResult.FAIL;
        player.awardStat(Stats.ITEM_USED.get(this));
        pickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        ItemStack filledStack = player.hasInfiniteMaterials() ? stack.transmuteCopy(target, 1) : fill(stack, target);
        ItemStack result = ItemUtils.createFilledResult(stack, player, filledStack);
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

    /**
     * NeoForge pours through this overload (its 4-argument version and {@code use} both call it), which skipped the
     * copper rule above. Without {@code @Override} it overrides only where the loader has it; elsewhere it is unused.
     */
    public boolean emptyContents(@Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult hit, @Nullable ItemStack container) {
        if (kind == Kind.COPPER && getContent().isSame(Fluids.WATER)) return emptyContents(user, level, pos, hit);
        try {
            return (boolean) loaderEmptyContents().invoke(this, user, level, pos, hit, container);
        } catch (Throwable e) {
            throw new IllegalStateException("bucket pour failed", e);
        }
    }

    private static java.lang.invoke.MethodHandle loaderEmptyContents;

    /** The loader's own 5-argument {@code BucketItem.emptyContents}, called like {@code super}. */
    private static java.lang.invoke.MethodHandle loaderEmptyContents() throws ReflectiveOperationException {
        if (loaderEmptyContents == null) {
            loaderEmptyContents = java.lang.invoke.MethodHandles.lookup().findSpecial(BucketItem.class, "emptyContents",
                    java.lang.invoke.MethodType.methodType(boolean.class, LivingEntity.class, Level.class, BlockPos.class, BlockHitResult.class, ItemStack.class),
                    ModBucketItem.class);
        }
        return loaderEmptyContents;
    }

    /** Copper: hint when fully oxidized (unusable until scraped) and when waxed; Enderite: fill 1/2 or 2/2; ceramic: what it holds. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        if (kind == Kind.COPPER) {
            if (oxidation(stack) >= 3) {
                lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.copper_bucket.oxidized")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
            if (waxed(stack)) {
                lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.copper_bucket.waxed")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        } else if (kind == Kind.ENDERITE && getContent() != Fluids.EMPTY) {
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.enderite_bucket.fill", amount(), 2)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.accept(net.minecraft.network.chat.Component.translatable(amount() < 2
                    ? "tooltip.simplebuilding.enderite_bucket.half" : "tooltip.simplebuilding.enderite_bucket.full")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        } else if (kind == Kind.CERAMIC) {
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.ceramic_bucket")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.ceramic_bucket.uses",
                    CERAMIC_USES - ceramicUses(stack), CERAMIC_USES).withStyle(stage >= CERAMIC_STAGES - 1
                    ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Server-side pour for tests and dispensers: places the content and returns what stays (survival rules). */
    public ItemStack pourAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!emptyContents(null, level, pos, null)) return stack;
        return afterPour(stack);
    }
}
