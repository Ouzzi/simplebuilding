package com.simplebuilding.blocks.custom;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.framework.api.Protection;
import com.simplebuilding.util.NihilVaultStorage;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Nihil-Gewoelbe (Besitzer 2026-10-04): wie das Astralgewoelbe eine Vanilla-Endertruhe (Deckel, Sounds, Partikel,
 * Wasser, Oeffnerzaehler), aber alle Gewoelbe der Welt zeigen denselben Inhalt ({@link NihilVaultStorage}); sechs Reihen wie das
 * Astral-Gewoelbe (Besitzer N16).
 *
 * <p>simplebuilding-7 (PLAN40, Empfehlung A): in einer geteilten Welt (Dedicated Server oder fuer LAN geoeffnet) mit aktivem
 * Schutz-Anbieter (framework {@link Protection}, z. B. die Claims von Simple Tweaks) oeffnet es nur auf geschuetztem Land, auf
 * dem der Spieler Behaelter benutzen darf - sonst stellte ein Fremder sein eigenes Gewoelbe in die Wildnis und laese alles.
 * Einzelspieler und Server ohne Schutz-Anbieter wie bisher; Schalter {@code server.features.nihilVaultClaimOnly}.
 */
public class NihilVaultBlock extends EnderChestBlock {
    public static final String TITLE = "block.simplebuilding.nihil_vault";
    /** Aktionsleiste, wenn das Gewoelbe hier nicht oeffnet (A14: der Spieler sieht, warum). */
    public static final String CLAIM_ONLY = "block.simplebuilding.nihil_vault.claim_only";

    public NihilVaultBlock(Properties properties) { super(properties); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!ServerTuning.get().features.nihilVault) return InteractionResult.PASS;
        if (level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above())) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof EnderChestBlockEntity chest) {
            if (player instanceof ServerPlayer serverPlayer && !mayOpen(server, serverPlayer, pos)) {
                serverPlayer.sendOverlayMessage(Component.translatable(CLAIM_ONLY));
                server.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            View view = new View(NihilVaultStorage.get(server.getServer()).items(), chest);
            player.openMenu(new SimpleMenuProvider((id, inv, user) -> ChestMenu.sixRows(id, inv, view),
                    Component.translatable(TITLE)));
            player.awardStat(net.minecraft.stats.Stats.OPEN_ENDERCHEST);
            net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(server, player, true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether {@code player} may open the shared vault at {@code pos} (see the class comment). */
    public static boolean mayOpen(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!ServerTuning.get().features.nihilVaultClaimOnly) return true;
        MinecraftServer server = level.getServer();
        if (!server.isDedicatedServer() && !server.isPublished()) return true; // single player: nobody else can reach it
        if (!Protection.active(server)) return true; // no claims on this server: shared like before (handbook warns)
        boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack());
        return decide(target -> Protection.allows(server, target, Protection.Action.CONTAINER),
                level.dimension().identifier().toString(), pos, player.getUUID(), admin);
    }

    /**
     * The rule alone (testable without registering a provider on the shared test server): the land must be protected -
     * nobody (no actor) may use containers there - and the player must be allowed to.
     */
    public static boolean decide(Predicate<Protection.Target> allows, String dimension, BlockPos pos, UUID player, boolean admin) {
        if (allows.test(new Protection.Target(null, dimension, pos.getX(), pos.getY(), pos.getZ(), false))) return false;
        return allows.test(new Protection.Target(player, dimension, pos.getX(), pos.getY(), pos.getZ(), admin));
    }

    /**
     * Ein geoeffnetes Gewoelbe: alle Slots gehen an den geteilten Container (eine Instanz fuer alle Menues - kein
     * Dupe, Vanilla-Synchronisation wie an einer Truhe). Oeffnen/Schliessen bindet die Truhe als aktive Endertruhe
     * des Spielers, damit der Vanilla-Oeffnerzaehler (Deckel, Sounds) ihn mitzaehlt.
     */
    public static final class View implements Container {
        private final SimpleContainer shared;
        private final EnderChestBlockEntity chest;

        public View(SimpleContainer shared, EnderChestBlockEntity chest) {
            this.shared = shared;
            this.chest = chest;
        }

        public SimpleContainer shared() { return shared; }

        @Override public int getContainerSize() { return shared.getContainerSize(); }
        @Override public boolean isEmpty() { return shared.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return shared.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int count) { return shared.removeItem(slot, count); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return shared.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { shared.setItem(slot, stack); }
        @Override public void setChanged() { shared.setChanged(); }
        @Override public void clearContent() { shared.clearContent(); }

        @Override public boolean stillValid(Player player) {
            return ServerTuning.get().features.nihilVault && chest.stillValid(player);
        }

        @Override public void startOpen(ContainerUser user) {
            if (user.getLivingEntity() instanceof Player player) player.getEnderChestInventory().setActiveChest(chest);
            chest.startOpen(user);
        }

        @Override public void stopOpen(ContainerUser user) {
            chest.stopOpen(user);
            if (user.getLivingEntity() instanceof Player player && player.getEnderChestInventory().isActiveChest(chest)) {
                player.getEnderChestInventory().setActiveChest(null);
            }
        }
    }
}
