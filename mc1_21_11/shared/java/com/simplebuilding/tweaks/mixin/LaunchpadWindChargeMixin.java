package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.block.LaunchpadBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Schleichen + Rechtsklick mit Windkugeln auf ein Launchpad laedt alle Windkugeln der Hand auf einmal.
 * Beim Schleichen mit einem Item in der Hand fragt Vanilla den Block nicht ({@code useItemOn} wird
 * unterdrueckt), sondern nur {@code Item#useOn} - WindChargeItem ueberschreibt das nicht, darum legt
 * dieses Mixin die Methode dort an (wie {@code TweaksItemStackMixin}). Client und Server laufen beide
 * hier durch; so wirft der Client beim Laden keine Windkugel.
 */
@Mixin(WindChargeItem.class)
public abstract class LaunchpadWindChargeMixin extends Item {

    private LaunchpadWindChargeMixin(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult bulk = LaunchpadBlock.bulkDeposit(context);
        return bulk != null ? bulk : super.useOn(context);
    }
}
