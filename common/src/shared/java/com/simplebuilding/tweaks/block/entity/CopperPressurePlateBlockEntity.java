package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Stehzeit-Logik der Kupfer-Druckplatte aus Simple Tweaks; neu: das Loslassen wartet genauso lange
 * wie das Ausloesen.
 */
public class CopperPressurePlateBlockEntity extends OwnedBlockEntity {
    private int ticksActive;
    private int ticksInactive;

    public CopperPressurePlateBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.COPPER_PRESSURE_PLATE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CopperPressurePlateBlockEntity be) {
        boolean powered = state.getValue(CopperPressurePlateBlock.POWERED);
        if (!SimpleTweaks.config().pads.enableTimedCopperPlates) {
            be.ticksActive = 0;
            if (powered) {
                setPowered(level, pos, state, false);
            }
            return;
        }
        AABB box = new AABB(pos).inflate(0.0, 0.5, 0.0);
        List<Player> players = PlayerScan.playersIn(level, box, Player.class, p -> !p.isSpectator());
        int required = state.getBlock() instanceof CopperPressurePlateBlock plate
                ? CopperPressurePlateBlock.requiredTicks(plate.getAge()) : 20;

        if (!players.isEmpty()) {
            be.ticksInactive = 0;
            if (!powered) {
                be.ticksActive++;
                if (be.ticksActive % 5 == 0) {
                    float pitch = 0.5f + (float) be.ticksActive / required;
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.2f, pitch);
                }
                if (be.ticksActive >= required) {
                    setPowered(level, pos, state, true);
                    level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.0f, 1.2f);
                    be.ticksActive = 0;
                }
            }
        } else {
            be.ticksActive = 0;
            // Loslassen dauert so lange wie Ausloesen (Besitzer 2026-09-27): erst nach derselben
            // Wartezeit ohne Spieler geht das Signal aus; wer zurueckkommt, haelt es an.
            if (powered) {
                be.ticksInactive++;
                if (be.ticksInactive >= required) {
                    setPowered(level, pos, state, false);
                    level.playSound(null, pos, SoundEvents.COPPER_STEP, SoundSource.BLOCKS, 0.7f, 0.8f);
                    be.ticksInactive = 0;
                }
            } else {
                be.ticksInactive = 0;
            }
        }
    }

    /** Wie Vanilla-Druckplatten auch den Block darunter benachrichtigen (er wird stark versorgt). */
    private static void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        level.setBlock(pos, state.setValue(CopperPressurePlateBlock.POWERED, powered), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos.below(), state.getBlock());
    }
}
