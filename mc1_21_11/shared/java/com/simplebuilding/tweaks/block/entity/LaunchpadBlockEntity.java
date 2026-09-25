package com.simplebuilding.tweaks.block.entity;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Launchpad, 1:1 aus Simple Tweaks: Ladungen, 3 s Countdown mit Spirale, Start je nach Ladung. */
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
        if (charges < max) {
            charges++;
            chargeTimer = 0;
            setChanged();
            sync();
        }
    }

    /** Startstaerke wie in Simple Tweaks: 1,5 + 0,4 je Ladung. */
    public static double strengthFor(int charges) {
        return 1.5 + charges * 0.4;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchpadBlockEntity be) {
        boolean client = level.isClientSide();
        if (!SimpleTweaks.config().pads.enableLaunchpads) {
            be.chargeTimer = 0;
            return;
        }
        AABB detection = new AABB(pos).move(0, 0.1, 0).inflate(0.0, 0.5, 0.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, detection, p -> true);

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
                player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.empty").withStyle(ChatFormatting.RED), true);
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
                player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.countdown",
                        be.charges, 3 - be.chargeTimer / 20).withStyle(ChatFormatting.AQUA), true);
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
