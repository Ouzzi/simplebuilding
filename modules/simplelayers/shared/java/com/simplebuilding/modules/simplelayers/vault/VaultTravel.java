package com.simplebuilding.modules.simplelayers.vault;

import com.simplebuilding.modules.simplelayers.vault.block.VaultGateBlock;
import com.simplebuilding.modules.simplelayers.vault.gen.VaultLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Into and out of the Bedrock Vault (UNDER21). The Overworld gate (bottom of a bedrock gap) leads to the Entrance Hall of the vault
 * of the same region (x/z ÷ 128). The inner gate stays open {@code vault.closeSeconds} after the first entry - stepping back in
 * leads out again - then seals while anyone is inside; an empty vault opens again ({@link #tick}, state in memory + gate blocks,
 * so a restart loses nothing that matters: entering an empty vault opens it). While sealed and occupied the Overworld gate
 * refuses (A14 message). The only other way out: the solved altar's ascent stream ({@link #ascend}). Both lead to the Overworld
 * surface above the entry point.
 */
public final class VaultTravel {
    public static final int COOLDOWN = 100;
    private static final Map<Long, Long> CLOSE_AT = new HashMap<>();
    private static Set<Long> occupiedLast = new HashSet<>();

    private VaultTravel() {}

    public static @Nullable ServerLevel vault(MinecraftServer server) {
        return server.getLevel(VaultRegistry.VAULT);
    }

    public static long seed(ServerLevel vault) {
        return VaultChunkGenerator.seed(vault.getChunkSource().randomState());
    }

    static long key(VaultLayout l) {
        return ((long) l.rx << 32) ^ (l.rz & 0xFFFFFFFFL);
    }

    /** Gate contact: in the Overworld enter, in the vault (gate still open) back out. Players only, once per cooldown. */
    public static void gateContact(ServerLevel level, Entity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer player) || player.isSpectator() || player.isOnPortalCooldown() || player.isPassenger() || !VaultRegistry.on()) return;
        if (VaultRegistry.inVault(level)) leave(player);
        else enter(player);
    }

    /** Sends the player into the vault of its region; false when the vault is sealed (someone inside) or not loaded. */
    public static boolean enter(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel vault = vault(server);
        if (vault == null) return false;
        VaultLayout l = VaultLayout.at(seed(vault), player.getBlockX(), player.getBlockZ());
        if (sealed(vault, l)) {
            player.setPortalCooldown(40);
            player.sendOverlayMessage(Component.translatable("message.simplelayers.vault.sealed"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 0.6F, 0.7F);
            return false;
        }
        if (!occupied(vault, l)) setGate(vault, l, true);
        VaultEntries.get(server).put(player.getUUID(), player.blockPosition());
        int[] a = l.arrival;
        float yaw = switch (l.entranceSide) { // face into the hall
            case 0 -> 0.0F;
            case 1 -> 90.0F;
            case 2 -> 180.0F;
            default -> -90.0F;
        };
        Entity moved = player.teleport(new TeleportTransition(vault, new Vec3(a[0] + 0.5, a[1], a[2] + 0.5), Vec3.ZERO, yaw, 0.0F, TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved == null) return false;
        moved.setPortalCooldown(COOLDOWN);
        moved.resetFallDistance();
        vault.playSound(null, moved.blockPosition(), SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 0.6F, 0.6F);
        if (moved instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.simplelayers.vault.arrive", VaultConfig.closeSeconds));
        CLOSE_AT.putIfAbsent(key(l), vault.getGameTime() + VaultConfig.closeSeconds * 20L);
        return true;
    }

    /** Through the still open inner gate: back to the Overworld surface above the entry. */
    public static boolean leave(ServerPlayer player) {
        return out(player, "message.simplelayers.vault.left");
    }

    /** The ascent stream: lifts, and at the top of the shaft hands players to the Overworld. */
    public static void ascend(ServerLevel level, Entity entity, BlockPos pos) {
        if (entity instanceof ServerPlayer player && !player.isSpectator() && handsOver(pos)) out(player, "message.simplelayers.vault.ascended");
    }

    static boolean out(ServerPlayer player, String message) {
        MinecraftServer server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        BlockPos entry = VaultEntries.get(server).get(player.getUUID());
        if (entry == null) entry = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
        overworld.getChunk(entry.getX() >> 4, entry.getZ() >> 4);
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entry.getX(), entry.getZ());
        Vec3 to = new Vec3(entry.getX() + 0.5, Math.max(overworld.getMinY() + 1, y), entry.getZ() + 0.5);
        ServerPlayer moved = (ServerPlayer) player.teleport(new TeleportTransition(overworld, to, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved == null) return false;
        VaultEntries.get(server).remove(moved.getUUID());
        moved.setPortalCooldown(COOLDOWN);
        moved.resetFallDistance();
        overworld.playSound(null, moved.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.8F);
        moved.sendOverlayMessage(Component.translatable(message));
        return true;
    }

    /** Players (not spectating) inside the complex of {@code l}. */
    public static boolean occupied(ServerLevel vault, VaultLayout l) {
        for (ServerPlayer p : vault.players()) {
            if (!p.isSpectator() && l.inComplex(p.getBlockX(), p.getBlockY(), p.getBlockZ())) return true;
        }
        return false;
    }

    public static boolean gateOpen(ServerLevel vault, VaultLayout l) {
        int[] g = l.gate.get(0);
        BlockState s = vault.getBlockState(new BlockPos(g[0], g[1], g[2]));
        return !s.is(VaultRegistry.VAULT_GATE) || s.getValue(VaultGateBlock.OPEN);
    }

    /** Sealed against newcomers: someone is inside and the gate has closed. */
    public static boolean sealed(ServerLevel vault, VaultLayout l) {
        return occupied(vault, l) && !gateOpen(vault, l);
    }

    public static void setGate(ServerLevel vault, VaultLayout l, boolean open) {
        boolean changed = false;
        for (int[] g : l.gate) {
            BlockPos p = new BlockPos(g[0], g[1], g[2]);
            BlockState s = vault.getBlockState(p);
            if (s.is(VaultRegistry.VAULT_GATE) && s.getValue(VaultGateBlock.OPEN) != open) {
                vault.setBlock(p, s.setValue(VaultGateBlock.OPEN, open), 3);
                changed = true;
            }
        }
        if (changed) {
            int[] g = l.gate.get(0);
            vault.playSound(null, new BlockPos(g[0], g[1], g[2]), open ? SoundEvents.VAULT_OPEN_SHUTTER : SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    /** Once a second: seal gates whose time is up while occupied, open the gates of vaults that became empty. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel vault = vault(server);
        if (vault == null) return;
        long seed = seed(vault), now = vault.getGameTime();
        Map<Long, VaultLayout> occupied = new HashMap<>();
        for (ServerPlayer p : vault.players()) {
            if (p.isSpectator()) continue;
            VaultLayout l = VaultLayout.at(seed, p.getBlockX(), p.getBlockZ());
            if (l.inComplex(p.getBlockX(), p.getBlockY(), p.getBlockZ())) occupied.put(key(l), l);
        }
        for (VaultLayout l : occupied.values()) update(vault, l, true, now);
        for (long k : occupiedLast) {
            if (!occupied.containsKey(k)) update(vault, VaultLayout.of(seed, (int) (k >> 32), (int) k), false, now);
        }
        occupiedLast = new HashSet<>(occupied.keySet());
    }

    /**
     * The gate rule for one vault: occupied and open → seal once {@code vault.closeSeconds} have passed since the first moment it
     * was seen occupied; empty → open (and forget the timer). Returns whether the gate is open afterwards.
     */
    public static boolean update(ServerLevel vault, VaultLayout l, boolean occupied, long now) {
        long k = key(l);
        if (!occupied) {
            CLOSE_AT.remove(k);
            setGate(vault, l, true);
            return true;
        }
        if (!gateOpen(vault, l)) return false;
        long at = CLOSE_AT.computeIfAbsent(k, x -> now + VaultConfig.closeSeconds * 20L);
        if (now < at) return true;
        setGate(vault, l, false);
        for (ServerPlayer p : vault.players()) {
            if (l.inComplex(p.getBlockX(), p.getBlockY(), p.getBlockZ())) p.sendOverlayMessage(Component.translatable("message.simplelayers.vault.closed"));
        }
        return false;
    }

    /** The ascent stream hands players over at the top of the shaft. */
    public static boolean handsOver(BlockPos streamPos) {
        return streamPos.getY() >= VaultLayout.SHAFT_TOP - 2;
    }

    /** Test hook: forget the timers (a test runs its own clock). */
    public static void reset() {
        CLOSE_AT.clear();
        occupiedLast = new HashSet<>();
    }
}
