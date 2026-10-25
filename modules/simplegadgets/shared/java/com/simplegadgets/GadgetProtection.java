package com.simplegadgets;

import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.HammerActions;
import com.simplelib.api.InteractionGuard;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Who may change a block with a gadget: framework {@link Protection} (claims of any Simple mod) plus every permission a partner
 * registered with SimpleLib {@link HammerActions#registerPermission} (SimpleBuilding: its WorldPermissions). Asked on the server
 * only; on the client everything is allowed, the server has the last word. Same rules as Simple Hammers' {@code HammerProtection}.
 */
public final class GadgetProtection {
    /**
     * The loader's side of "may this player break or build here": fires the loader's own block break (and place) events with the
     * real player, the way claim and protection mods expect to be asked. Set by the loader entry; without one everything is allowed.
     */
    public interface BuildGuard {
        boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);

        boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
    }

    private static volatile BuildGuard buildGuard = new BuildGuard() {
        @Override
        public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
            return true;
        }
    };

    private GadgetProtection() {}

    public static void setBuildGuard(BuildGuard guard) {
        buildGuard = guard;
    }

    /** The loader's guard (game tests swap it for a refusing one briefly). */
    public static BuildGuard buildGuard() {
        return buildGuard;
    }

    public static boolean mayAct(Level level, Player player, BlockPos pos) {
        return mayAct(level, player, pos, Protection.Action.ANY);
    }

    public static boolean mayAct(Level level, Player player, BlockPos pos, Protection.Action action) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (Protection.active(server.getServer())) {
            boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(serverPlayer.createCommandSourceStack());
            var target = new Protection.Target(serverPlayer.getUUID(), server.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), admin);
            if (!Protection.allows(server.getServer(), target, action)) return false;
        }
        return HammerActions.mayChange(level, player, pos);
    }

    /**
     * Whether {@code player} may change the clicked block at {@code pos} with {@code stack}: the question a block placement asks
     * (spectator, adventure mode, spawn protection) and the claims above.
     */
    public static boolean mayTransform(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        return !player.isSpectator() && player.mayBuild() && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, face, stack) && mayAct(level, player, pos);
    }

    /**
     * Whether {@code player} may break (or melt, dry, light, prime) the block at {@code pos}: the world border, spawn protection,
     * claims, then the loader's break event. While the event is answered SimpleLib's {@link InteractionGuard#probing} is raised, so
     * area breakers know it is no real break.
     */
    public static boolean mayBreak(Level level, Player player, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                || !mayAct(level, player, pos, Protection.Action.DESTROY)) {
            return false;
        }
        InteractionGuard.probing++;
        try {
            return buildGuard.mayBreak(serverLevel, serverPlayer, pos.immutable(), state);
        } finally {
            InteractionGuard.probing--;
        }
    }

    /** Whether {@code player} may place {@code state} at {@code pos} (fire): like {@link #mayBreak} with the build action. */
    public static boolean mayPlace(Level level, Player player, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                || !mayAct(level, player, pos, Protection.Action.BUILD)) {
            return false;
        }
        InteractionGuard.probing++;
        try {
            return buildGuard.mayPlace(serverLevel, serverPlayer, pos.immutable(), state);
        } finally {
            InteractionGuard.probing--;
        }
    }

    /** Whether {@code player} may affect {@code target}: the claims at the corners and the middle of its footprint (entity action). */
    public static boolean mayAffectEntity(Player player, Entity target) {
        Level level = target.level();
        if (!(level instanceof ServerLevel server) || !Protection.active(server.getServer())) return true;
        AABB box = target.getBoundingBox();
        int y = net.minecraft.util.Mth.floor(box.minY);
        int x0 = net.minecraft.util.Mth.floor(box.minX);
        int x1 = net.minecraft.util.Mth.floor(Math.nextDown(box.maxX));
        int z0 = net.minecraft.util.Mth.floor(box.minZ);
        int z1 = net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ));
        int[][] points = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1},
                {net.minecraft.util.Mth.floor(box.getCenter().x), net.minecraft.util.Mth.floor(box.getCenter().z)}};
        for (int[] point : points) {
            if (!mayAct(level, player, new BlockPos(point[0], y, point[1]), Protection.Action.ENTITY)) return false;
        }
        return true;
    }
}
