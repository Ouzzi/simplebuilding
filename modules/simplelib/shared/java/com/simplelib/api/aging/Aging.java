package com.simplelib.api.aging;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The behaviour of every {@link AgingChain}, run for any block class through SimpleLib's mixins on the block state
 * ({@code AgingStateMixin}: random tick and item use, so it works for Vanilla base blocks and for classes that override
 * {@code useItemOn}; {@code AgingDispenserMixin}: honeycomb from a dispenser). Ported 1:1 from Vanilla
 * {@code ChangeOverTimeBlock}/{@code HoneycombItem}/the axe's {@code BlockTransformer}: same neighbour rule, particles
 * (level events 3003/3004/3005), sounds and advancement trigger.
 */
public final class Aging {
    /** Where a block sits in its chain. */
    public record Entry(AgingChain chain, int stage, boolean waxed) {
        public boolean ages() {
            return !waxed && stage < chain.endStage();
        }

        public Block block(int stage, boolean waxed) {
            return waxed ? chain.waxed().get(stage) : chain.stages().get(stage);
        }
    }

    private static final Map<Block, Entry> ENTRIES = new IdentityHashMap<>();
    private static final List<AgingChain> CHAINS = new ArrayList<>();
    /** Manhattan distance of the neighbour scan (Vanilla {@code ChangeOverTimeBlock.SCAN_DISTANCE}). */
    public static final int SCAN_DISTANCE = 4;

    private Aging() {}

    static synchronized void register(AgingChain chain) {
        CHAINS.add(chain);
        for (int i = 0; i < chain.stages().size(); i++) {
            Block block = chain.stages().get(i);
            if (ENTRIES.containsKey(block)) throw new IllegalStateException(block + " is already in an aging chain");
            ENTRIES.put(block, new Entry(chain, i, false));
        }
        for (int i = 0; i < chain.waxed().size(); i++) ENTRIES.put(chain.waxed().get(i), new Entry(chain, i, true));
        // Vanilla base blocks (e.g. the pumpkin) do not random-tick: their state caches were built before any mod, so the
        // flag is set on the cached states. Own stages carry randomTicks() in their properties.
        for (BlockState state : chain.base().getStateDefinition().getPossibleStates()) {
            if (!state.isRandomlyTicking()) ((com.simplelib.mixin.AgingTickAccessor) (Object) state).simplelib$setRandomlyTicking(true);
        }
    }

    public static List<AgingChain> chains() {
        return List.copyOf(CHAINS);
    }

    public static @Nullable Entry entry(Block block) {
        return ENTRIES.isEmpty() ? null : ENTRIES.get(block);
    }

    // --- aging --------------------------------------------------------------------------------------------------------

    /** Random tick (mixin, before the block's own): true when the block aged (the block's own tick is then skipped). */
    public static boolean randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Entry e = entry(state.getBlock());
        if (e == null || !e.ages() || !e.chain().condition().test(level, pos, state)) return false;
        if (random.nextFloat() >= e.chain().chance()) return false;
        Optional<BlockState> next = nextState(e, state, level, pos, random);
        next.ifPresent(s -> level.setBlockAndUpdate(pos, s));
        return next.isPresent();
    }

    /**
     * Vanilla's neighbour rule: a younger unwaxed block of the same group within 4 blocks holds this one back; otherwise
     * the chance is ((older + 1) / (older + same + 1))².
     */
    static Optional<BlockState> nextState(Entry e, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int older = 0, same = 0;
        for (BlockPos p : BlockPos.withinManhattan(pos, SCAN_DISTANCE)) {
            if (p.distManhattan(pos) > SCAN_DISTANCE || p.equals(pos)) continue;
            Entry n = entry(level.getBlockState(p).getBlock());
            if (n == null || n.waxed() || !n.chain().group().equals(e.chain().group())) continue;
            if (n.stage() < e.stage()) return Optional.empty();
            if (n.stage() > e.stage()) older++;
            else same++;
        }
        float chance = (float) (older + 1) / (older + same + 1);
        return random.nextFloat() < chance * chance ? Optional.of(next(e, state)) : Optional.empty();
    }

    /** The next stage of an unwaxed block (properties kept); the state itself at the end stage or when waxed. */
    public static BlockState next(Entry e, BlockState state) {
        return e.ages() ? e.block(e.stage() + 1, false).withPropertiesOf(state) : state;
    }

    // --- honeycomb and axe --------------------------------------------------------------------------------------------

    /** Waxed copy of {@code state}, if it can be waxed. */
    public static Optional<BlockState> waxed(BlockState state) {
        Entry e = entry(state.getBlock());
        return e == null || e.waxed() || e.stage() >= e.chain().waxed().size() ? Optional.empty()
                : Optional.of(e.block(e.stage(), true).withPropertiesOf(state));
    }

    /** What an axe does: wax off, else one stage back; empty for an unwaxed base block. */
    public static Optional<BlockState> scraped(BlockState state) {
        Entry e = entry(state.getBlock());
        if (e == null) return Optional.empty();
        if (e.waxed()) return Optional.of(e.block(e.stage(), false).withPropertiesOf(state));
        return e.stage() > 0 ? Optional.of(e.block(e.stage() - 1, false).withPropertiesOf(state)) : Optional.empty();
    }

    /**
     * Same stage and wax in the chain of {@code target}'s block (for overrides that turn one chain block into another,
     * e.g. shears carving a faded pumpkin into a faded carved pumpkin). {@code target} when that block has no chain.
     */
    public static BlockState sameAge(BlockState from, BlockState target) {
        Entry e = entry(from.getBlock()), t = entry(target.getBlock());
        if (e == null || t == null) return target;
        AgingChain chain = t.chain();
        if (e.stage() > chain.endStage() || (e.waxed() && e.stage() >= chain.waxed().size())) return target;
        return (e.waxed() ? chain.waxed().get(e.stage()) : chain.stages().get(e.stage())).withPropertiesOf(target);
    }

    /** Item use on a chain block (mixin, before the block's own): null = not ours, let the block and the item act. */
    public static @Nullable InteractionResult useItemOn(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand,
                                                        BlockHitResult hit) {
        if (entry(state.getBlock()) == null) return null;
        boolean honeycomb = stack.is(Items.HONEYCOMB);
        boolean axe = !honeycomb && stack.typeHolder().is(ItemTags.AXES);
        if (!honeycomb && !axe) return null;
        Optional<BlockState> target = honeycomb ? waxed(state) : scraped(state);
        BlockPos pos = hit.getBlockPos();
        if (target.isEmpty() || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return null;
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            boolean waxOff = !honeycomb && entry(state.getBlock()).waxed();
            level.setBlock(pos, target.get(), Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, target.get()));
            if (honeycomb) {
                effects(level, pos, 0);
                stack.consume(1, player);
            } else {
                effects(level, pos, waxOff ? 1 : 2);
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Honeycomb from a dispenser in front of {@code pos}: true when it waxed (Vanilla dispenser behaviour). */
    public static boolean dispenseWax(ServerLevel level, BlockPos pos) {
        Optional<BlockState> target = waxed(level.getBlockState(pos));
        if (target.isEmpty()) return false;
        level.setBlockAndUpdate(pos, target.get());
        effects(level, pos, 0);
        return true;
    }

    private static final Map<net.minecraft.core.dispenser.DispenseItemBehavior, net.minecraft.core.dispenser.DispenseItemBehavior> WRAPPED =
            new IdentityHashMap<>();

    /**
     * The dispenser's honeycomb behaviour with chain blocks added (AgingDispenserMixin): waxes a chain block in front,
     * otherwise Vanilla's behaviour (copper) unchanged. Without chains the original comes back.
     */
    public static synchronized net.minecraft.core.dispenser.DispenseItemBehavior dispenseHoneycomb(net.minecraft.core.dispenser.DispenseItemBehavior original) {
        if (CHAINS.isEmpty() || original == null) return original;
        return WRAPPED.computeIfAbsent(original, o -> (source, stack) -> {
            BlockPos front = source.pos().relative(source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING));
            if (!dispenseWax(source.level(), front)) return o.dispense(source, stack);
            stack.shrink(1);
            source.level().levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, source.pos(), 0);
            source.level().levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, source.pos(),
                    source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING).get3DDataValue());
            return stack;
        });
    }

    /** 0 wax on, 1 wax off, 2 scrape: Vanilla particles (level event) and sound, heard by everyone. */
    private static void effects(Level level, BlockPos pos, int kind) {
        switch (kind) {
            case 0 -> {
                level.levelEvent(null, LevelEvent.PARTICLES_WAX_ON, pos, 0);
                level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            case 1 -> {
                level.levelEvent(null, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            default -> {
                level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, pos, 0);
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
}
