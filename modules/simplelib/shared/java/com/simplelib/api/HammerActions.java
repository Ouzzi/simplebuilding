package com.simplelib.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The hammer contract (split stage 7, docs/ai/PLAN-S7-HAMMERS-2026-10-18.md): a hammer mod (Simple Hammers) asks these
 * hooks, every other mod registers its own hammer ways here - nobody imports the hammer mod. Without a hammer mod
 * nothing calls them (the axe ways of the mods stay on, principle 5a). Content-free: only Vanilla types.
 *
 * <ul>
 *   <li>{@link #registerUse}: right clicks with a hammer on a block, before ({@link Stage#FIRST}) or after
 *       ({@link Stage#PERMITTED}) the hammer's own permission check.</li>
 *   <li>{@link #registerKeep}: blocks the hammer must not break right now (a placed template in the hammer stance).</li>
 *   <li>{@link #registerArea}: replaces the hammer's 3x3 area (SimpleBuilding's octant selection).</li>
 *   <li>{@link #registerPermission}: may the player change this block (claims, protections of other mods);
 *       {@link #registerAreaVeto}: no area break right now (permission probes).</li>
 *   <li>{@link #listen}/{@link #fire}: what the hammer did (advancements, counters of other mods).</li>
 *   <li>{@link #registerStaged}: in-world upgrades that go strike by strike with their own material units (crucibles).</li>
 *   <li>{@link #registerParts}: upgrades that change several blocks at once (a double chest).</li>
 *   <li>{@link #registerUpgradedState}: adjusts the block state after a plain upgrade (a fresh breaker piston).</li>
 *   <li>{@link #upgradeTakesClick}: the hammer mod's answer to "does a hammer upgrade take this click instead of the
 *       menu" (crucibles, chests of other mods ask it).</li>
 *   <li>{@link #areaBreaking}: true while the hammer breaks the extra blocks of an area (for anti-cheat checks).</li>
 * </ul>
 */
public final class HammerActions {
    /** The hammer reshaped a block (stairs, slab, carved octet). */
    public static final String RESHAPE = "reshape";
    /** The hammer crushed a diamond block (or cracked diamond block) into pebbles. */
    public static final String DIAMOND_CRUSH = "diamond_crush";
    /** The hammer crushed Stone / Cobblestone / Gravel one stage further (HS29-3). */
    public static final String CRUSH = "crush";
    /** One extra block of an area break. */
    public static final String AREA_BLOCK = "area_block";
    /** An in-world upgrade to the Netherite / Enderite tier finished. */
    public static final String UPGRADE_NETHERITE = "upgrade_netherite";
    public static final String UPGRADE_ENDERITE = "upgrade_enderite";

    /** When a {@link Use} is asked. */
    public enum Stage {
        /** Right after the hammer's in-world upgrade check, before its permission check (the hook checks itself). */
        FIRST,
        /** After the hammer checked that the player may change the clicked block. */
        PERMITTED
    }

    /** A right click with a hammer; {@code null} = not this hook's block, the hammer goes on. */
    @FunctionalInterface
    public interface Use {
        @Nullable InteractionResult use(UseOnContext context);
    }

    @FunctionalInterface
    public interface Keep {
        boolean keeps(Player player, BlockState state, Level level, BlockPos pos);
    }

    @FunctionalInterface
    public interface Area {
        @Nullable List<BlockPos> positions(Player player, BlockPos origin);
    }

    @FunctionalInterface
    public interface Permission {
        boolean mayChange(Level level, Player player, BlockPos pos);
    }

    /** An upgrade that goes strike by strike and takes its material in units (crucibles, owner N51). */
    public interface Staged {
        /** Strikes of the whole upgrade. */
        int strikes();

        /** Strikes per material unit. */
        int strikesPerUnit();

        /** Units already struck into the block at {@code state}. */
        int units(BlockState state);

        /** Strike {@code done} of the upgrade into {@code to}; the last one upgrades in place. */
        void strike(ServerLevel level, BlockPos pos, Block to, int done, ItemStack material);
    }

    @FunctionalInterface
    public interface Parts {
        List<BlockPos> parts(Level level, BlockPos pos, BlockState state, Block to);
    }

    @FunctionalInterface
    public interface TakesClick {
        boolean takes(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);
    }

    private record Registered<T>(Predicate<Block> blocks, T hook) {}

    private static final Map<Stage, List<Use>> USES = new ConcurrentHashMap<>();
    private static final List<Keep> KEEPS = new CopyOnWriteArrayList<>();
    private static final List<Area> AREAS = new CopyOnWriteArrayList<>();
    private static final List<Permission> PERMISSIONS = new CopyOnWriteArrayList<>();
    private static final List<java.util.function.BooleanSupplier> VETOES = new CopyOnWriteArrayList<>();
    private static final Map<String, List<Consumer<ServerPlayer>>> LISTENERS = new ConcurrentHashMap<>();
    private static final List<Registered<Staged>> STAGED = new CopyOnWriteArrayList<>();
    private static final List<Registered<Parts>> PARTS = new CopyOnWriteArrayList<>();
    private static final List<UnaryOperator<BlockState>> UPGRADED = new CopyOnWriteArrayList<>();
    private static volatile @Nullable TakesClick takesClick;
    private static final ThreadLocal<Boolean> AREA_BREAKING = ThreadLocal.withInitial(() -> false);

    private HammerActions() {}

    public static void registerUse(Stage stage, Use use) {
        USES.computeIfAbsent(stage, s -> new CopyOnWriteArrayList<>()).add(use);
    }

    /** The first registered hook of {@code stage} that handles the click, or {@code null}. */
    public static @Nullable InteractionResult use(Stage stage, UseOnContext context) {
        for (Use use : USES.getOrDefault(stage, List.of())) {
            InteractionResult result = use.use(context);
            if (result != null) return result;
        }
        return null;
    }

    public static void registerKeep(Keep keep) {
        KEEPS.add(keep);
    }

    public static boolean keeps(Player player, BlockState state, Level level, BlockPos pos) {
        for (Keep keep : KEEPS) {
            if (keep.keeps(player, state, level, pos)) return true;
        }
        return false;
    }

    public static void registerArea(Area area) {
        AREAS.add(area);
    }

    /** The replacing area of the first hook that has one (origin first), or {@code null} for the hammer's own. */
    public static @Nullable List<BlockPos> area(Player player, BlockPos origin) {
        for (Area area : AREAS) {
            List<BlockPos> positions = area.positions(player, origin);
            if (positions != null) return positions;
        }
        return null;
    }

    /** While {@code veto} is true no area break starts (SimpleBuilding: its claim probes fire break events that are no real breaks). */
    public static void registerAreaVeto(java.util.function.BooleanSupplier veto) {
        VETOES.add(veto);
    }

    public static boolean areaVetoed() {
        for (java.util.function.BooleanSupplier veto : VETOES) {
            if (veto.getAsBoolean()) return true;
        }
        return false;
    }

    public static void registerPermission(Permission permission) {
        PERMISSIONS.add(permission);
    }

    /** Whether every registered permission lets {@code player} change the block at {@code pos}. */
    public static boolean mayChange(Level level, Player player, BlockPos pos) {
        for (Permission permission : PERMISSIONS) {
            if (!permission.mayChange(level, player, pos)) return false;
        }
        return true;
    }

    public static void listen(String event, Consumer<ServerPlayer> listener) {
        LISTENERS.computeIfAbsent(event, e -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Tells the listeners of {@code event}; only server players count. */
    public static void fire(String event, @Nullable Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        for (Consumer<ServerPlayer> listener : LISTENERS.getOrDefault(event, List.of())) listener.accept(server);
    }

    public static void registerStaged(Predicate<Block> from, Staged staged) {
        STAGED.add(0, new Registered<>(from, staged));
    }

    /** The staged way of upgrades starting at {@code from}, or {@code null} for a plain upgrade. */
    public static @Nullable Staged staged(Block from) {
        for (Registered<Staged> registered : STAGED) {
            if (registered.blocks().test(from)) return registered.hook();
        }
        return null;
    }

    public static void registerParts(Predicate<Block> to, Parts parts) {
        PARTS.add(0, new Registered<>(to, parts));
    }

    /** The blocks an upgrade into {@code to} changes at once (at least {@code pos}). */
    public static List<BlockPos> parts(Level level, BlockPos pos, BlockState state, Block to) {
        for (Registered<Parts> registered : PARTS) {
            if (registered.blocks().test(to)) {
                List<BlockPos> parts = registered.hook().parts(level, pos, state, to);
                if (parts != null && !parts.isEmpty()) return parts;
            }
        }
        List<BlockPos> single = new ArrayList<>();
        single.add(pos);
        return single;
    }

    public static void registerUpgradedState(UnaryOperator<BlockState> adjust) {
        UPGRADED.add(adjust);
    }

    public static BlockState upgradedState(BlockState state) {
        for (UnaryOperator<BlockState> adjust : UPGRADED) state = adjust.apply(state);
        return state;
    }

    /** Set by the hammer mod: whether its in-world upgrade takes this click (instead of the block's menu). */
    public static void setUpgradeTakesClick(@Nullable TakesClick check) {
        takesClick = check;
    }

    public static boolean upgradeTakesClick(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        TakesClick check = takesClick;
        return check != null && check.takes(state, level, pos, player, hand);
    }

    /** True on this thread while a hammer breaks the extra blocks of an area (server side). */
    public static boolean areaBreaking() {
        return AREA_BREAKING.get();
    }

    /** Set by the hammer mod around the extra breaks of an area. */
    public static void areaBreaking(boolean on) {
        AREA_BREAKING.set(on);
    }
}
