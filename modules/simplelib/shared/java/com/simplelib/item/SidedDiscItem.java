package com.simplelib.item;

import com.simplelib.api.DiscSides;
import java.util.function.UnaryOperator;
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
        return DiscSides.turnInHand(level, player, hand, turner);
    }
}
