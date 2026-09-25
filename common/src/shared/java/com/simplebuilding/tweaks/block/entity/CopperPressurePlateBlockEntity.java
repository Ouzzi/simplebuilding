package com.simplebuilding.tweaks.block.entity;

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

/** Stehzeit-Logik der Kupfer-Druckplatte, 1:1 aus Simple Tweaks. */
public class CopperPressurePlateBlockEntity extends OwnedBlockEntity {
    private int ticksActive;

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
        List<Player> players = level.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator());

        if (!players.isEmpty()) {
            int required = state.getBlock() instanceof CopperPressurePlateBlock plate
                    ? CopperPressurePlateBlock.requiredTicks(plate.getAge()) : 20;
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
            if (powered) {
                setPowered(level, pos, state, false);
                level.playSound(null, pos, SoundEvents.COPPER_STEP, SoundSource.BLOCKS, 0.7f, 0.8f);
            }
        }
    }

    /** Wie Vanilla-Druckplatten auch den Block darunter benachrichtigen (er wird stark versorgt). */
    private static void setPowered(Level level, BlockPos pos, BlockState state, boolean powered) {
        level.setBlock(pos, state.setValue(CopperPressurePlateBlock.POWERED, powered), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos.below(), state.getBlock());
    }
}
