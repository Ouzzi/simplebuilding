package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * Besitzer-Regeln der Platten aus Simple Tweaks: wer setzt, ist Besitzer; der Besitzer baut in
 * wenigen Sekunden ab, alle anderen brauchen sehr lange; Kreativ bleibt Vanilla.
 */
public final class PadOwnership {

    /** Pads, Teleporter, Launchpads: Besitzer 2 s, Fremde 60 s. */
    public static final float OWNER_PAD = 1.0f / 40.0f;
    public static final float STRANGER_PAD = 1.0f / 1200.0f;
    /** Chunk-Loader, Kupfer- und Filterplatten: Besitzer 1,5 s, Fremde 10 s. */
    public static final float OWNER_PLATE = 1.0f / 30.0f;
    public static final float STRANGER_PLATE = 1.0f / 200.0f;

    private PadOwnership() {
    }

    public static void onPlaced(Level level, BlockPos pos, LivingEntity placer) {
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(player.getUUID());
        }
    }

    /**
     * Abbaufortschritt pro Tick; {@code vanilla} ist der Wert der Oberklasse und gilt im
     * Kreativmodus oder wenn keine Block-Entity da ist.
     */
    public static float destroyProgress(Player player, BlockGetter level, BlockPos pos, float owner, float stranger, float vanilla) {
        if (player.isCreative()) {
            return vanilla;
        }
        if (level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            return owned.isOwner(player) ? owner : stranger;
        }
        return vanilla;
    }
}
