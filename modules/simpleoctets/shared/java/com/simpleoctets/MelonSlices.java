package com.simpleoctets;

import com.simpleoctets.block.OctetCells;
import com.simpleoctets.registry.OctetRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

/** The melon octet (Queue Nachtrag 24): a melon slice placed with sneak + right-click. */
public final class MelonSlices {
    /**
     * Sneak + right-click with a melon slice on a block places it as a melon octet; without sneaking, or where no octet
     * fits, the slice is eaten as usual (null = not handled). Off with the super-mod switch.
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        if (OctetRegistry.MELON_OCTET == null || player == null || !player.isSecondaryUseActive()
                || !context.getItemInHand().is(Items.MELON_SLICE) || !SimpleOctets.on()) {
            return null;
        }
        InteractionResult result = OctetCells.place(context, OctetRegistry.MELON_OCTET);
        return result.consumesAction() ? result : null;
    }

    private MelonSlices() {}
}
