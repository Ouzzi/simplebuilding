package com.simplebuilding.modules.simplevault;

import com.simplebuilding.modules.simplevault.SimpleVault;
import com.simplebuilding.modules.simplevault.block.ChimeCrystalBlock;
import com.simplebuilding.modules.simplevault.block.PathTileBlock;
import com.simplebuilding.modules.simplevault.block.SealPart;
import com.simplebuilding.modules.simplevault.block.SealPedestalBlock;
import com.simplebuilding.modules.simplevault.block.SealSocketBlock;
import com.simplebuilding.modules.simplevault.block.VaultLampBlock;
import com.simplebuilding.modules.simplevault.gen.VaultLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Bedrock Vault riddles (UNDER21, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md). Positions and solutions come from the pure
 * {@link VaultLayout} of the vault; the state lives in the blocks (levers, lamps, lit tiles, pedestals, sockets) - only the
 * running chime sequence is kept in memory. Every try answers at once in the action bar (A14), nothing punishes, everything can
 * be tried again. Solving a chamber readies its pedestal; the three seal parts in the altar open the ascent.
 */
public final class VaultPuzzles {
    public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> REWARD =
            ResourceKey.create(Registries.LOOT_TABLE, SimpleVault.id("vault/ascent"));
    private static final float[] PITCH = {0.71F, 0.89F, 1.06F, 1.33F};
    private static final Map<Long, Integer> CHIME = new HashMap<>();

    private VaultPuzzles() {}

    /** The layout of the vault around {@code pos}, or null outside the vault dimension. */
    public static @Nullable VaultLayout layout(ServerLevel level, BlockPos pos) {
        if (!VaultRegistry.inVault(level)) return null;
        return VaultLayout.at(VaultTravel.seed(level), pos.getX(), pos.getZ());
    }

    private static BlockPos at(int[] p) {
        return new BlockPos(p[0], p[1], p[2]);
    }

    private static void tell(@Nullable Player player, String key, Object... args) {
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable(key, args));
    }

    // ------------------------------------------------------------------------------------------------------------------- light

    /** Lever bits as they stand now (bit i = lever i on). */
    public static int leverBits(ServerLevel level, VaultLayout l) {
        int bits = 0;
        for (int i = 0; i < VaultLayout.LEVERS; i++) {
            BlockState s = level.getBlockState(at(l.levers[i]));
            if (s.hasProperty(LeverBlock.POWERED) && s.getValue(LeverBlock.POWERED)) bits |= 1 << i;
        }
        return bits;
    }

    /** After a lever moved: lamps follow the levers (lever i switches lamps i and i + 1), the count is told, all five right solves. */
    public static void leverPulled(ServerLevel level, BlockPos pos, @Nullable Player player) {
        VaultLayout l = layout(level, pos);
        if (l == null || l.levers[0] == null) return;
        boolean ours = false;
        for (int[] lv : l.levers) ours |= at(lv).equals(pos);
        if (!ours) return;
        int lamps = VaultLayout.lampsFor(leverBits(level, l));
        for (int i = 0; i < VaultLayout.LEVERS; i++) {
            BlockPos lp = at(l.lamps[i]);
            BlockState s = level.getBlockState(lp);
            if (s.is(VaultRegistry.VAULT_LAMP)) level.setBlock(lp, s.setValue(VaultLampBlock.LIT, (lamps >> i & 1) == 1), 3);
        }
        int right = VaultLayout.LEVERS - Integer.bitCount((lamps ^ l.target) & 0b11111);
        if (right == VaultLayout.LEVERS) solve(level, l, SealPart.LIGHT, player);
        else tell(player, "message.simplevault.vault.lamps", right, VaultLayout.LEVERS);
    }

    // ------------------------------------------------------------------------------------------------------------------- sound

    /** A crystal struck: it rings; the next tone of the frieze moves on, a wrong one lets the sequence fade (starting over). */
    public static void strike(ServerLevel level, BlockPos pos, int tone, @Nullable Player player) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.9F, PITCH[tone & 3]);
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, tone / 4.0);
        VaultLayout l = layout(level, pos);
        if (l == null || l.crystals[0] == null || ready(level, l, SealPart.SOUND)) return;
        long key = VaultTravel.key(l);
        int at = CHIME.getOrDefault(key, 0);
        if (l.sequence[at] == tone) {
            at++;
            if (at == VaultLayout.SEQUENCE) {
                CHIME.remove(key);
                solve(level, l, SealPart.SOUND, player);
                return;
            }
            CHIME.put(key, at);
            tell(player, "message.simplevault.vault.chime", at, VaultLayout.SEQUENCE);
        } else {
            int restart = l.sequence[0] == tone ? 1 : 0;
            CHIME.put(key, restart);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.5F, 0.5F);
            tell(player, "message.simplevault.vault.chime_fade");
        }
    }

    // -------------------------------------------------------------------------------------------------------------------- path

    /** Cells lit now (one block per cell tells). */
    static boolean lit(ServerLevel level, VaultLayout l, int cell) {
        BlockState s = level.getBlockState(at(l.pathBlock(cell, 0, 0)));
        return s.is(VaultRegistry.PATH_TILE) && s.getValue(PathTileBlock.LIT);
    }

    static void light(ServerLevel level, VaultLayout l, int cell, boolean on) {
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
            BlockPos p = at(l.pathBlock(cell, i, j));
            BlockState s = level.getBlockState(p);
            if (s.is(VaultRegistry.PATH_TILE) && s.getValue(PathTileBlock.LIT) != on) level.setBlock(p, s.setValue(PathTileBlock.LIT, on), 3);
        }
    }

    /**
     * A player on a path tile. The lit cells always form the start or the end of the path (it can be walked from either side);
     * the next right cell lights, a wrong one puts the whole path out (quietly). The full path solves.
     */
    public static void step(ServerLevel level, BlockPos pos, @Nullable Player player) {
        VaultLayout l = layout(level, pos);
        if (l == null || l.pathOrigin == null || ready(level, l, SealPart.PATH)) return;
        int cell = l.pathCell(pos.getX(), pos.getY(), pos.getZ());
        if (cell < 0 || lit(level, l, cell)) return;
        List<Integer> path = l.path;
        int n = path.size(), k = 0;
        for (int c : path) if (lit(level, l, c)) k++;
        boolean prefix = true, suffix = true;
        for (int i = 0; i < k; i++) {
            prefix &= lit(level, l, path.get(i));
            suffix &= lit(level, l, path.get(n - 1 - i));
        }
        boolean right = k == 0 ? cell == path.get(0) || cell == path.get(n - 1)
                : prefix && cell == path.get(k) || suffix && cell == path.get(n - 1 - k);
        if (right) {
            light(level, l, cell, true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.8F + 0.6F * (k + 1) / n);
            if (k + 1 == n) solve(level, l, SealPart.PATH, player);
            return;
        }
        if (k == 0) {
            tell(player, "message.simplevault.vault.path_start");
            return;
        }
        for (int c : path) light(level, l, c, false);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.7F, 0.6F);
        tell(player, "message.simplevault.vault.path_fade");
    }

    // ------------------------------------------------------------------------------------------------------- pedestals, altar

    static boolean ready(ServerLevel level, VaultLayout l, SealPart part) {
        BlockState s = level.getBlockState(at(l.pedestals[part.index()]));
        return s.is(VaultRegistry.SEAL_PEDESTAL) && s.getValue(SealPedestalBlock.READY);
    }

    static boolean filled(ServerLevel level, VaultLayout l, SealPart part) {
        BlockState s = level.getBlockState(at(l.sockets[part.index()]));
        return s.is(VaultRegistry.SEAL_SOCKET) && s.getValue(SealSocketBlock.FILLED);
    }

    static void solve(ServerLevel level, VaultLayout l, SealPart part, @Nullable Player player) {
        BlockPos p = at(l.pedestals[part.index()]);
        BlockState s = level.getBlockState(p);
        if (s.is(VaultRegistry.SEAL_PEDESTAL) && !s.getValue(SealPedestalBlock.READY)) level.setBlock(p, s.setValue(SealPedestalBlock.READY, true), 3);
        level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.3, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        tell(player, "message.simplevault.vault.solved." + part.name);
    }

    /** Pedestal used: locked = what the room asks; ready = the seal part (again, as long as the altar lacks it). */
    public static @Nullable ItemStack takeFragment(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
        VaultLayout l = layout(level, pos);
        SealPart part = state.getValue(SealPedestalBlock.PART);
        if (l == null) return null;
        if (!state.getValue(SealPedestalBlock.READY)) {
            tell(player, "message.simplevault.vault.hint." + part.name);
            return null;
        }
        if (filled(level, l, part)) {
            tell(player, "message.simplevault.vault.already_set");
            return null;
        }
        if (player != null) {
            for (ItemStack s : player.getInventory()) {
                if (SealFragmentItem.fits(s, part, l.rx, l.rz)) {
                    tell(player, "message.simplevault.vault.carrying");
                    return null;
                }
            }
        }
        ItemStack stack = SealFragmentItem.bound(part, l.rx, l.rz);
        if (player != null && !player.getInventory().add(stack.copy())) level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy()));
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        tell(player, "message.simplevault.vault.took." + part.name);
        return stack;
    }

    /** Socket used: the matching seal part of this vault goes in; three set open the ascent. */
    public static boolean insert(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player, ItemStack stack) {
        VaultLayout l = layout(level, pos);
        SealPart part = state.getValue(SealSocketBlock.PART);
        if (l == null) return false;
        if (state.getValue(SealSocketBlock.FILLED)) {
            altarStatus(level, pos, player);
            return false;
        }
        if (!SealFragmentItem.fits(stack, part, l.rx, l.rz)) {
            tell(player, stack.getItem() instanceof SealFragmentItem ? "message.simplevault.vault.wrong_part" : "message.simplevault.vault.socket." + part.name);
            return false;
        }
        if (player == null || !player.isCreative()) stack.shrink(1);
        level.setBlock(pos, state.setValue(SealSocketBlock.FILLED, true), 3);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
        int set = 0;
        for (SealPart p : SealPart.values()) if (filled(level, l, p)) set++;
        if (set == SealPart.values().length) openAscent(level, l);
        else tell(player, "message.simplevault.vault.altar", set, SealPart.values().length);
        return true;
    }

    public static void altarStatus(ServerLevel level, BlockPos pos, @Nullable Player player) {
        VaultLayout l = layout(level, pos);
        if (l == null) return;
        int set = 0;
        for (SealPart p : SealPart.values()) if (filled(level, l, p)) set++;
        tell(player, set == SealPart.values().length ? "message.simplevault.vault.ascent_open" : "message.simplevault.vault.altar", set, SealPart.values().length);
    }

    /** The seal in the dome gives way; a stream rises from the altar through the shaft; a one-time reward drops on the altar. */
    public static void openAscent(ServerLevel level, VaultLayout l) {
        for (int[] s : l.seal) level.setBlock(at(s), Blocks.AIR.defaultBlockState(), 3);
        for (int y = l.altar[1] + 1; y <= VaultLayout.SHAFT_TOP; y++) {
            BlockPos p = new BlockPos(l.altar[0], y, l.altar[2]);
            if (level.getBlockState(p).isAir()) level.setBlock(p, VaultRegistry.ASCENT_STREAM.defaultBlockState(), 3);
        }
        BlockPos top = at(l.altar).above();
        level.playSound(null, top, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(REWARD);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(top)).create(LootContextParamSets.CHEST);
        for (ItemStack s : table.getRandomItems(params)) {
            ItemEntity item = new ItemEntity(level, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 1.5, s);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        for (ServerPlayer p : level.players()) {
            if (l.inComplex(p.getBlockX(), p.getBlockY(), p.getBlockZ())) p.sendOverlayMessage(Component.translatable("message.simplevault.vault.ascent_open"));
        }
    }

    /** Whether the vault's ascent is open (its seal gone). */
    public static boolean ascentOpen(ServerLevel level, VaultLayout l) {
        return !level.getBlockState(at(l.seal.get(0))).is(VaultRegistry.ASCENT_SEAL);
    }

    public static void forgetChimes() {
        CHIME.clear();
    }

    /** Test hook: the tone a crystal block plays. */
    public static int tone(BlockState state) {
        return state.getValue(ChimeCrystalBlock.TONE);
    }
}
