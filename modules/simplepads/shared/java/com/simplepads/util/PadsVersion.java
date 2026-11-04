package com.simplepads.util;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

/**
 * The few 26.3 values of SimpleBuilding's {@code McVersion} the pad code reads (Simple Pads is a 26.3 Fabric module; the
 * SimpleBuilding 26.2 line keeps its own classes, b-pads01).
 */
public final class PadsVersion {
    /** SimpleBuilding's brewing effects (Shivering) exist on 26.3. */
    public static final boolean BREWING_EFFECTS = true;
    /** Gadget rework (OW24) is live on 26.3. */
    public static final boolean GADGET_REWORK = true;
    public static final PushReaction PUSH_BLOCKED = PushReaction.IMMOVEABLE;
    public static final PushReaction PUSH_DESTROYS = PushReaction.POPPED;

    private PadsVersion() {
    }

    public static @Nullable ItemEntity drop(Player player, ItemStack stack, boolean thrownFromHand, boolean predictedByClient) {
        return player.drop(stack, thrownFromHand, predictedByClient ? net.minecraft.util.Prediction.PREDICTED
                : net.minecraft.util.Prediction.SERVER_ONLY);
    }

    public static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties properties) {
        return properties.isViewBlocking((state, level, pos, nearPlane) -> false);
    }

    /** Vanilla wax-on particles and sound (26.3: level event 3003 is particles only, the sound is separate). */
    public static void waxOnEffects(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        level.levelEvent(null, net.minecraft.world.level.block.LevelEvent.PARTICLES_WAX_ON, pos, 0);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.HONEYCOMB_WAX_ON, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
