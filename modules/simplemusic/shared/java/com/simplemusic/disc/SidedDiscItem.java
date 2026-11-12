package com.simplemusic.disc;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A music disc with an A- and a B-side (a-shopmusic37b, docs/ai/PLAN-A-SHOPMUSIC37B-2026-11-12.md): a right click with it in hand
 * (into the air, or at a block that does nothing with it) turns it over, A to B and back - like flipping a record before putting it
 * on. No text on the screen: the hand swings, a soft turning click, the label colours swap, and the Vanilla tooltip names the
 * side's song. A short cooldown keeps a held button from flipping it back and forth. A jukebox or a shop takes the disc as it is:
 * take it out, turn it over, put it back. Everything else about the disc is Vanilla ({@code jukebox_playable}).
 */
public final class SidedDiscItem extends Item {
    /** Ticks before the disc can be turned again (a held use button repeats every 4 ticks). */
    public static final int TURN_COOLDOWN = 10;

    public SidedDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack turned = MusicDiscs.turnOver(player.getItemInHand(hand));
        if (turned.isEmpty()) return InteractionResult.PASS;
        player.getCooldowns().addCooldown(turned, TURN_COOLDOWN);
        // the player hears it at once on the client, the others from the server
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.PLAYERS, 0.6F, 1.2F);
        return InteractionResult.SUCCESS.heldItemTransformedTo(turned);
    }
}
