package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Launchpad, aus Simple Tweaks: Ladungen, 3 s Countdown mit Spirale, Start je nach Ladung. Seit den
 * drei Stufen (2026-09-27) zaehlt eine Ladung doppelt; Launchpads aus aelteren Welten, die mehr
 * geladen haben, als ihre Stufe jetzt fasst, werfen den Ueberschuss beim ersten Tick aus.
 */
public class LaunchpadBlockEntity extends OwnedBlockEntity {
    public static final int LAUNCH_TICKS = 60;

    private int charges;
    private int chargeTimer;

    public LaunchpadBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.LAUNCHPAD, pos, state);
    }

    public int getCharges() {
        return charges;
    }

    public void addCharge(int max) {
        addCharges(1, max);
    }

    /** Laedt bis zu {@code count} Ladungen, hoechstens bis {@code max}; liefert, wie viele es waren. */
    public int addCharges(int count, int max) {
        int added = Math.max(0, Math.min(count, max - charges));
        if (added > 0) {
            charges += added;
            chargeTimer = 0;
            setChanged();
            sync();
        }
        return added;
    }

    /** Grundschub ohne Ladung (Simple Tweaks). */
    public static final double BASE_STRENGTH = 1.5;
    /** Schub je Ladung: doppelt so viel wie in Simple Tweaks (0,4), seit die Stufen nur noch 4/8/16 fassen. */
    public static final double STRENGTH_PER_CHARGE = 0.8;

    /** Startstaerke: 1,5 + 0,8 je Ladung - 16 Ladungen = die alten 32 (1,5 + 0,4 je Ladung). */
    public static double strengthFor(int charges) {
        // Config tweaks.padTuning.launchpadStrengthMultiplier (Standard 1).
        return (BASE_STRENGTH + charges * STRENGTH_PER_CHARGE)
                * com.simplebuilding.tweaks.SimpleTweaks.config().padTuning.launchpadStrengthFactor();
    }

    /**
     * Welt-Upgrade: ein Launchpad aus der Zeit vor den Stufen (Enderit fasste 32, das normale 16) kann
     * mehr Ladungen tragen, als seine Stufe jetzt fasst. Der Ueberschuss faellt als Windkugeln heraus,
     * nichts geht verloren. Liefert die Zahl der ausgeworfenen Windkugeln.
     */
    public int clampToCapacity(Level level, BlockPos pos, BlockState state) {
        int max = state.getBlock() instanceof LaunchpadBlock pad ? pad.capacityAt(level, pos) : LaunchpadBlock.maxCharges(LaunchpadBlock.ENDERITE_TIER);
        int excess = charges - max;
        if (excess <= 0) {
            return 0;
        }
        charges = max;
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.WIND_CHARGE, excess));
        setChanged();
        sync();
        return excess;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchpadBlockEntity be) {
        boolean client = level.isClientSide();
        if (!client && be.charges > 0) {
            be.clampToCapacity(level, pos, state);
        }
        if (!SimpleTweaks.config().pads.enableLaunchpads) {
            be.chargeTimer = 0;
            return;
        }
        AABB detection = new AABB(pos).move(0, 0.1, 0).inflate(0.0, 0.5, 0.0);
        List<Player> players = PlayerScan.playersIn(level, detection, Player.class);

        if (players.isEmpty() && be.charges > 0 && client && level.getRandom().nextInt(30) == 0) {
            level.addParticle(ParticleTypes.SMALL_GUST,
                    pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4, pos.getY() + 0.2,
                    pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }

        if (players.isEmpty()) {
            be.chargeTimer = 0;
            return;
        }
        Player player = players.get(0);
        if (!player.isAlive()) {
            return;
        }
        if (be.charges <= 0) {
            if (!client && level.getGameTime() % 40 == 0) {
                player.sendOverlayMessage(Component.translatable("message.simplebuilding.launchpad.empty").withStyle(ChatFormatting.RED));
            }
            return;
        }

        be.chargeTimer++;
        if (be.chargeTimer < LAUNCH_TICKS) {
            if (client) {
                int frequency = Math.max(1, 10 - (be.charges / 2));
                if (be.chargeTimer % frequency == 0) {
                    double angle = be.chargeTimer * (0.2 + be.charges * 0.02);
                    double px = pos.getX() + 0.5 + Math.cos(angle) * 0.6;
                    double pz = pos.getZ() + 0.5 + Math.sin(angle) * 0.6;
                    level.addParticle(ParticleTypes.GUST, px, pos.getY() + 0.2 + be.chargeTimer * 0.01, pz, 0, 0.05, 0);
                }
            } else if (be.chargeTimer % 20 == 0) {
                float pitch = 0.8f + be.charges / 40f + be.chargeTimer / 60f;
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.5f, pitch);
                player.sendOverlayMessage(Component.translatable("message.simplebuilding.launchpad.countdown",
                        be.charges, 3 - be.chargeTimer / 20).withStyle(ChatFormatting.AQUA));
            }
            return;
        }

        if (!client && player instanceof ServerPlayer serverPlayer) {
            launch(serverPlayer, strengthFor(be.charges), state.getBlock() instanceof LaunchpadBlock pad && pad.isEnderite());
            be.charges = 0;
            be.chargeTimer = 0;
            be.setChanged();
            be.sync();
        }
    }

    public static void launch(ServerPlayer player, double strength, boolean fallProtection) {
        player.setDeltaMovement(player.getDeltaMovement().add(0, strength, 0));
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.LAUNCHPAD);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), (int) (strength * 5), 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 2.0f, 1.0f);
        if (fallProtection) {
            LaunchSafety.protect(player);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Beim Abbau fallen die geladenen Windkugeln heraus (Audit #51: vorher waren sie weg). Wie bei
     * Truhen nur beim normalen Entfernen, nicht bei {@code /setblock} mit Flag 256.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide() && charges > 0) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.WIND_CHARGE, charges));
            charges = 0;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Charges", charges);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("Charges", 0);
    }
}
