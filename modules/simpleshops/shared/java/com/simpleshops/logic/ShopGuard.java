package com.simpleshops.logic;

import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlock;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.block.SalesBlockEntity;
import com.simplebuilding.framework.api.Protection;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Who may break a shop block and how the owner does it. Rules (owner answers 15.10.): only the owner and operators
 * (permission level 2) remove a shop block, the owner always (also in a foreign claim or after claim rights changed);
 * a block without data (damaged world) only by operators; everything runs on the server.
 */
public final class ShopGuard {
    public static final String PROVIDER = "simpleshops";

    public static boolean isShop(BlockState state) {
        return state.getBlock() instanceof SalesBlock || state.getBlock() instanceof DisplayCaseBlock;
    }

    /** Operator in the sense of the other modules: permission level 2 (gamemaster). */
    public static boolean isAdmin(Player player) {
        if (!(player instanceof ServerPlayer sp)) return player != null && Commands.LEVEL_GAMEMASTERS.check(player.permissions());
        // Fresh from the operator list (a level set at login is not enough after /op or /deop).
        MinecraftServer server = sp.level().getServer();
        var entry = server.getPlayerList().getOps().get(sp.nameAndId());
        if (entry != null) return Commands.LEVEL_GAMEMASTERS.check(entry.permissions());
        return server.getPlayerList().isOp(sp.nameAndId()) && Commands.LEVEL_GAMEMASTERS.check(server.getProfilePermissions(sp.nameAndId()));
    }

    public static @Nullable UUID ownerAt(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SalesBlockEntity s) return s.owner();
        if (be instanceof DisplayCaseBlockEntity c) return c.owner();
        return null;
    }

    /** Owner or operator; the data-less block belongs to nobody. */
    public static boolean mayBreak(Player player, Level level, BlockPos pos) {
        UUID owner = ownerAt(level, pos);
        if (owner != null && owner.equals(player.getUUID())) return true;
        return isAdmin(player);
    }

    /**
     * The break of a shop block by a player who passed {@link #mayBreak}: Vanilla's destroyBlock sequence without the claim
     * check (the owner may always remove). Contents drop in the block entity's pre-remove hook, the block item from the
     * loot table unless the player is in creative mode.
     */
    public static boolean ownerBreak(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isShop(state)) return false;
        if (player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return false;
        BlockEntity be = level.getBlockEntity(pos);
        BlockState adjusted = state.getBlock().playerWillDestroy(level, pos, state, player);
        boolean removed = level.removeBlock(pos, false);
        if (removed) state.getBlock().destroy(level, pos, adjusted);
        if (removed && !player.preventsBlockDrops()) {
            state.getBlock().playerDestroy(level, player, pos, adjusted, be, ItemStack.EMPTY);
        }
        return true;
    }

    // --- Protection provider (framework contract: hammer, vein, strip, laser, pistons, hoppers ... ask it) --------

    public static void register(MinecraftServer server) {
        Protection.unregister(server, PROVIDER);
        Protection.register(server, PROVIDER, new Protection.Check() {
            @Override
            public boolean allows(Protection.Target target) {
                return allows(target, Protection.Action.ANY);
            }

            @Override
            public boolean allows(Protection.Target target, Protection.Action action) {
                if (action == Protection.Action.USE || action == Protection.Action.CONTAINER || action == Protection.Action.REDSTONE
                        || action == Protection.Action.ENTITY) return true;
                Found shop = shopAt(server, target);
                if (shop == null) return true;
                if (target.administrator()) return true;
                return shop.owner() != null && shop.owner().equals(target.actor());
            }

            @Override
            public boolean allowsAutomation(Protection.Target source, Protection.Target target) {
                return shopAt(server, source) == null && shopAt(server, target) == null;
            }
        });
    }

    public static void unregister(MinecraftServer server) {
        Protection.unregister(server, PROVIDER);
    }

    private record Found(@Nullable UUID owner) {}

    /** The shop at a target in an already loaded chunk (owner {@code null} = damaged block without data), else {@code null} (unloaded: not assumed). */
    private static @Nullable Found shopAt(MinecraftServer server, Protection.Target t) {
        Identifier id = Identifier.tryParse(t.dimension());
        if (id == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) return null;
        BlockPos pos = new BlockPos(t.x(), t.y(), t.z());
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!isShop(state)) return null;
        return new Found(ownerAt(level, pos));
    }

    private ShopGuard() {}
}
