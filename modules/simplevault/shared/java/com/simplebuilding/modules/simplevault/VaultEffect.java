package com.simplebuilding.modules.simplevault;

import com.simplebuilding.modules.simplevault.SimpleVault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Vault-bound" (UNDER21): every player in the Bedrock Vault carries it; its icon says why the bedrock and the riddle's friezes
 * do not give way (A14) - the way out is the altar. The cladding in front of the bedrock stays mineable (owner: the bedrock shows
 * only once the cladding is mined). Refreshed every {@link #PERIOD} ticks for {@link #DURATION}: outside it runs out quickly.
 */
public final class VaultEffect extends MobEffect {
    public static final int COLOR = 0x6A7C96, PERIOD = 10, DURATION = 40;
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(Registries.MOB_EFFECT, SimpleVault.id("vault_bound"));
    private static Holder<MobEffect> holder;

    private VaultEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    public static void register() {
        holder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, KEY, new VaultEffect());
    }

    public static Holder<MobEffect> holder() {
        return holder;
    }

    public static boolean due(Player player) {
        return VaultRegistry.on() && !player.isSpectator() && VaultRegistry.inVault(player.level());
    }

    /** A block of the vault the player cannot mine here: bedrock or a frieze/marker block, in the vault, with the effect, not creative. */
    public static boolean held(BlockState state, Player player) {
        return state.is(VaultRegistry.VAULT_HELD) && !player.isCreative() && holder != null && player.hasEffect(holder) && VaultRegistry.inVault(player.level());
    }

    /** Pistons leave the vault's held blocks where they are. */
    public static boolean heldInPlace(BlockState state, Level level) {
        return VaultRegistry.inVault(level) && state.is(VaultRegistry.VAULT_HELD);
    }

    /** A14: hitting the bedrock (or a held block) says why it holds and where the way out is. */
    public static void hintOnAttack(Player player, Level level, BlockPos pos) {
        if (!level.isClientSide() && VaultRegistry.inVault(level) && !player.isCreative() && level.getBlockState(pos).is(VaultRegistry.VAULT_HELD)) {
            player.sendOverlayMessage(Component.translatable("message.simplevault.vault.held"));
        }
    }

    public static void tick(MinecraftServer server) {
        if (holder == null || server.getTickCount() % PERIOD != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (due(player)) player.addEffect(new MobEffectInstance(holder, DURATION, 0, true, false, true));
        }
    }
}
