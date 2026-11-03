package com.simplemachines.upgrade;

import com.simplemachines.config.MachinesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

/**
 * The shared rule of the piston upgrades that push or break "unbreakable" blocks (Reinforced (Sticky), Netherite, Enderite).
 *
 * <p><b>Breachable</b> ({@link #isBreachable}): a block with a destroy speed below 0 from the {@code minecraft} namespace, or a
 * block in {@code simplemachines:piston_breachable_extra} (Vanilla: reinforced deepslate), that is neither in
 * {@code simplemachines:piston_breach_immune} nor has a block entity. Unbreakables of other mods only with
 * {@code pistons.breachModdedUnbreakables} (default off, audit #24); the End portal frame only with
 * {@code pistons.breachEndPortalFrames} (default off).
 *
 * <p><b>The client asks too</b> (audit N16): replaying a Reinforced Piston it builds its own {@code PistonStructureResolver}; the
 * server sends both options at login ({@code PistonConfigPayload}), a client level reads only those ({@link #setClientRules}).
 *
 * <p><b>Who pays</b> ({@link #findFuel}): a redstone block right next to the piston, the one behind first, then the four sides
 * across the facing. Never the front.
 */
public final class PistonBreach {
    public static final TagKey<Block> PISTON_BREACH_IMMUNE = TagKey.create(Registries.BLOCK,
            com.simplemachines.SimpleMachines.id("piston_breach_immune"));
    public static final TagKey<Block> PISTON_BREACHABLE_EXTRA = TagKey.create(Registries.BLOCK,
            com.simplemachines.SimpleMachines.id("piston_breachable_extra"));

    private PistonBreach() {}

    /** {@link #isBreachable(BlockState, BlockGetter, BlockPos)} for the block standing there. */
    public static boolean isBreachable(BlockGetter level, BlockPos pos) {
        return isBreachable(level.getBlockState(pos), level, pos);
    }

    /** Whether a piston upgrade may push or breach this block as "unbreakable". */
    public static boolean isBreachable(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir() || state.is(PISTON_BREACH_IMMUNE) || state.hasBlockEntity()) {
            return false;
        }
        if (state.is(PISTON_BREACHABLE_EXTRA)) {
            return true;
        }
        if (state.is(Blocks.END_PORTAL_FRAME) && !rulesFor(level).endPortalFrames()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) < 0.0F && unbreakableCounts(state.getBlock(), level);
    }

    /** Whether a block with negative destroy speed counts at all: {@code minecraft} always, other mods only with the option. */
    public static boolean unbreakableCounts(Block block, @Nullable BlockGetter level) {
        if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft")) {
            return true;
        }
        return rulesFor(level).moddedUnbreakables();
    }

    /** The class of blocks this is about, without the exceptions (the moving-block explosion shield asks only this). */
    public static boolean isUnbreakableClass(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) < 0.0F || state.is(PISTON_BREACHABLE_EXTRA);
    }

    /** The redstone block that pays a breach: right behind the piston first, then the four sides across the facing. Never the front. */
    public static @Nullable BlockPos findFuel(Level level, BlockPos piston, Direction facing) {
        BlockPos behind = piston.relative(facing.getOpposite());
        if (level.getBlockState(behind).is(Blocks.REDSTONE_BLOCK)) {
            return behind;
        }
        for (Direction side : Direction.values()) {
            if (side.getAxis() != facing.getAxis()) {
                BlockPos candidate = piston.relative(side);
                if (level.getBlockState(candidate).is(Blocks.REDSTONE_BLOCK)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Vanilla's {@code PistonBaseBlock#isPushable} without the two rules that hold a breachable block (destroy speed -1 and the
     * name list). World border, build height, push reaction and the block entity ban still hold. Only the handler mixin calls
     * this, for the one block right in front of a paid Reinforced Piston.
     */
    public static boolean isPushableAsBreach(BlockState state, Level level, BlockPos pos, Direction direction,
                                             boolean allowDestroyable, Direction connectionDirection) {
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (direction == Direction.DOWN && pos.getY() == level.getMinY()) {
            return false;
        }
        if (direction == Direction.UP && pos.getY() == level.getMaxY()) {
            return false;
        }
        PushReaction reaction = pushReaction(state);
        if (reaction == PushReaction.IMMOVEABLE) {
            return false;
        }
        if (reaction == PushReaction.POPPED) {
            return allowDestroyable;
        }
        if (reaction == PushReaction.PUSH) {
            return direction == connectionDirection;
        }
        return !state.hasBlockEntity();
    }

    /**
     * The push reaction with 26.2's meaning (SimpleBuilding's {@code McVersion#pushReaction}): 26.3 moved Vanilla's piston name
     * list (obsidian, crying obsidian, respawn anchor, reinforced deepslate) into the push reaction; those count as normal blocks
     * here, their own rules (hardness, the breach tags) decide.
     */
    public static PushReaction pushReaction(BlockState state) {
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.REINFORCED_DEEPSLATE)) {
            return PushReaction.PUSH_PULL;
        }
        return state.getPistonPushReaction();
    }

    // ---- options: the server reads its file, a client level the server's values ----

    private static volatile @Nullable ClientRules clientRules;

    /** The two options a client needs when it replays a piston. */
    public record ClientRules(boolean endPortalFrames, boolean moddedUnbreakables) {
    }

    /** Client side: take over the server's values (null = the own file again). */
    public static void setClientRules(@Nullable ClientRules rules) {
        clientRules = rules;
    }

    /** What the server sends: its own values. */
    public static ClientRules serverRules() {
        return new ClientRules(MachinesConfig.breachEndPortalFrames, MachinesConfig.breachModdedUnbreakables);
    }

    private static ClientRules rulesFor(@Nullable BlockGetter level) {
        ClientRules synced = clientRules;
        if (synced != null && level instanceof Level world && world.isClientSide()) {
            return synced;
        }
        return serverRules();
    }

    /** Whether the loader guard fires block break events with a fake player ({@code pistons.fireBreakEvents}). */
    public static boolean fireBreakEvents() {
        return MachinesConfig.fireBreakEvents;
    }
}
