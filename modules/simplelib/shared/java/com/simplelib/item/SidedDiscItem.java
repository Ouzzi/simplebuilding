package com.simplelib.item;

import com.simplelib.api.DiscSides;
import java.util.function.UnaryOperator;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A music disc with an A- and a B-side (N47 M6, formerly Simple Music's own class): a right click with it in hand turns it over
 * to the other side ({@link DiscSides}). No text on the screen: the hand swings, a soft turning click, a short cooldown
 * ({@link DiscSides#TURN_COOLDOWN}) keeps a held button from flipping back and forth. The owning mod passes its own turner
 * (stack to turned stack, empty = nothing to turn, e.g. module switched off). Everything else about the disc is Vanilla.
 */
public class SidedDiscItem extends Item {
    private final UnaryOperator<ItemStack> turner;

    public SidedDiscItem(Properties properties, UnaryOperator<ItemStack> turner) {
        super(properties);
        this.turner = turner;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack turned = turner.apply(player.getItemInHand(hand));
        if (turned.isEmpty()) return InteractionResult.PASS;
        player.getCooldowns().addCooldown(turned, DiscSides.TURN_COOLDOWN);
        // the player hears it at once on the client, the others from the server
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.PLAYERS, 0.6F, 1.2F);
        return InteractionResult.SUCCESS.heldItemTransformedTo(turned);
    }
}
